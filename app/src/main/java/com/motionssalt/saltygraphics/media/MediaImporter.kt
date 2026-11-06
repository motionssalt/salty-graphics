package com.motionssalt.saltygraphics.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.Transform2D
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class MediaImportResult(
    val importedLayers: List<Layer>,
    val failureReasons: List<String>
)

object MediaImporter {
    private const val MAX_TEXTURE_DIMENSION = 4096

    fun importMediaFromUris(
        context: Context,
        projectId: String,
        uris: List<Uri>,
        canvasWidth: Int = 1080,
        canvasHeight: Int = 1920
    ): MediaImportResult {
        val imported = mutableListOf<Layer>()
        val failures = mutableListOf<String>()

        val assetsDir = File(File(context.filesDir, "projects"), "${projectId}_assets").apply { mkdirs() }

        for (uri in uris) {
            val fileName = queryDisplayName(context, uri) ?: "image_${UUID.randomUUID().toString().take(8)}"
            val tempFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}_${UUID.randomUUID()}")

            try {
                // 1. Copy to temp file safely to examine EXIF and decode bounds
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    failures.add("$fileName: Cannot open file stream")
                    continue
                }
                FileOutputStream(tempFile).use { fos ->
                    inputStream.copyTo(fos)
                }

                if (tempFile.length() < 12L) {
                    failures.add("$fileName: Corrupted or empty image file")
                    tempFile.delete()
                    continue
                }

                // 2. Decode bounds to inspect dimensions without allocating memory
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(tempFile.absolutePath, boundsOpts)
                if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
                    failures.add("$fileName: Invalid or unsupported image format")
                    tempFile.delete()
                    continue
                }

                // 3. Read EXIF Orientation
                val orientation = try {
                    val exif = ExifInterface(tempFile.absolutePath)
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } catch (_: Throwable) {
                    ExifInterface.ORIENTATION_NORMAL
                }

                val rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                val flipHorizontal = orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL

                // 4. Determine safe inSampleSize to never exceed GPU max texture size or cause OOM
                val rawW = boundsOpts.outWidth
                val rawH = boundsOpts.outHeight
                val maxDim = maxOf(rawW, rawH)
                var sampleSize = 1
                while (maxDim / sampleSize > MAX_TEXTURE_DIMENSION) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val rawBitmap = BitmapFactory.decodeFile(tempFile.absolutePath, decodeOpts)
                if (rawBitmap == null) {
                    failures.add("$fileName: Failed to decode image pixels")
                    tempFile.delete()
                    continue
                }

                // 5. Apply EXIF transformation (rotation / flip)
                val finalBitmap = if (rotationDegrees != 0f || flipHorizontal) {
                    val matrix = Matrix()
                    if (flipHorizontal) matrix.preScale(-1f, 1f)
                    if (rotationDegrees != 0f) matrix.postRotate(rotationDegrees)
                    val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    if (rotated != rawBitmap) rawBitmap.recycle()
                    rotated
                } else {
                    rawBitmap
                }

                // 6. Save normalized image to project assets
                val assetFileName = "media_${UUID.randomUUID()}.png"
                val destFile = File(assetsDir, assetFileName)
                FileOutputStream(destFile).use { fos ->
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
                }
                tempFile.delete()

                // Cache in memory for immediate canvas rendering
                val assetKey = "assets/$assetFileName"
                MediaCache.put("$projectId:$assetKey", finalBitmap)

                // 7. Calculate reasonable initial layer dimensions fitted to canvas
                val imgW = finalBitmap.width.toFloat()
                val imgH = finalBitmap.height.toFloat()
                val maxInitialSize = minOf(canvasWidth, canvasHeight) * 0.7f
                val fitScale = if (maxOf(imgW, imgH) > maxInitialSize) {
                    maxInitialSize / maxOf(imgW, imgH)
                } else 1f

                val layerW = imgW * fitScale
                val layerH = imgH * fitScale

                val layer = Layer(
                    id = UUID.randomUUID().toString(),
                    name = fileName.substringBeforeLast('.'),
                    type = LayerType.MEDIA,
                    mediaAssetPath = assetKey,
                    mediaWidth = layerW,
                    mediaHeight = layerH,
                    transform = Transform2D()
                )
                imported.add(layer)
            } catch (e: Exception) {
                failures.add("$fileName: ${e.message ?: "Import failed"}")
                try { tempFile.delete() } catch (_: Throwable) {}
            }
        }

        return MediaImportResult(imported, failures)
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        return cursor.getString(nameIndex)
                    }
                }
            } catch (_: Throwable) {}
        }
        return uri.path?.substringAfterLast('/')
    }
}
