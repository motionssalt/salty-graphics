package com.motionssalt.saltygraphics.render

import android.content.Context
import android.graphics.*
import android.util.LruCache
import com.motionssalt.saltygraphics.engine.model.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ProjectThumbnailRenderer {
    private val memoryCache = LruCache<String, Bitmap>(50)

    fun getCached(projectId: String): Bitmap? = memoryCache.get(projectId)

    fun getThumbnailsDir(context: Context): File =
        File(context.filesDir, "thumbnails").apply { mkdirs() }

    fun getThumbnailFile(context: Context, projectId: String): File =
        File(getThumbnailsDir(context), "$projectId.png")

    fun getOrLoad(context: Context, projectId: String): Bitmap? {
        memoryCache.get(projectId)?.let { return it }
        val file = getThumbnailFile(context, projectId)
        if (file.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    memoryCache.put(projectId, bitmap)
                    return bitmap
                }
            } catch (_: Throwable) {}
        }
        return null
    }

    /**
     * Renders a document canvas to a low-resolution proxy Bitmap (about 512px on the long edge).
     */
    fun renderThumbnail(context: Context, document: ProjectDocument): Bitmap {
        val maxEdge = 512f
        val canvasW = maxOf(1, document.canvasSize.width)
        val canvasH = maxOf(1, document.canvasSize.height)
        val longEdge = maxOf(canvasW, canvasH).toFloat()
        val scale = if (longEdge > maxEdge) maxEdge / longEdge else 1f

        val thumbW = maxOf(1, (canvasW * scale).toInt())
        val thumbH = maxOf(1, (canvasH * scale).toInt())

        val bitmap = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Canvas Background (checkerboard if transparent)
        val bgColor = document.editorSettings.backgroundColor
        val bgAlpha = (bgColor shr 24 and 0xFF).toInt()
        if (bgAlpha < 255) {
            val checkPaint1 = Paint().apply { color = Color.LTGRAY }
            val checkPaint2 = Paint().apply { color = Color.GRAY }
            val cellSize = maxOf(8f, 12f * scale)
            val cols = (thumbW / cellSize).toInt() + 1
            val rows = (thumbH / cellSize).toInt() + 1
            for (r in 0..rows) {
                for (c in 0..cols) {
                    val p = if ((r + c) % 2 == 0) checkPaint1 else checkPaint2
                    canvas.drawRect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize, p)
                }
            }
        }
        if (bgColor != 0L) {
            val bgPaint = Paint().apply { color = bgColor.toInt() }
            canvas.drawRect(0f, 0f, thumbW.toFloat(), thumbH.toFloat(), bgPaint)
        }

        // 2. Draw Layers in order (bottom to top)
        val canvasCenterX = thumbW / 2f
        val canvasCenterY = thumbH / 2f

        for (layer in document.layers.reversed()) {
            if (!layer.isVisible) continue
            if (layer.type == LayerType.ADJUSTMENT) continue

            val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                com.motionssalt.saltygraphics.engine.render.LayerCompositor.applyLayerBlendMode(this, layer.blendMode)
            }
            canvas.saveLayer(null, layerPaint)

            canvas.save()
            // Translate to layer center in canvas
            val lx = canvasCenterX + (layer.transform.translationX * scale)
            val ly = canvasCenterY + (layer.transform.translationY * scale)
            canvas.translate(lx, ly)
            canvas.rotate(layer.transform.rotationDegrees)
            canvas.scale(layer.transform.scaleX, layer.transform.scaleY)

            when (layer.type) {
                LayerType.SHAPE -> {
                    val shape = layer.shapeData ?: ShapeData()
                    val sw = shape.width * scale
                    val sh = shape.height * scale
                    val rect = RectF(-sw / 2f, -sh / 2f, sw / 2f, sh / 2f)

                    if (shape.hasFill) {
                        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = shape.fillColor.toInt()
                            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                            style = Paint.Style.FILL
                        }
                        when (shape.type) {
                            ShapeType.RECTANGLE -> canvas.drawRect(rect, fillPaint)
                            ShapeType.ROUNDED_RECTANGLE -> canvas.drawRoundRect(rect, shape.cornerRadius * scale, shape.cornerRadius * scale, fillPaint)
                            ShapeType.CIRCLE -> canvas.drawOval(rect, fillPaint)
                            else -> canvas.drawRect(rect, fillPaint)
                        }
                    }
                    if (shape.hasStroke && shape.strokeWidth > 0) {
                        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = shape.strokeColor.toInt()
                            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                            strokeWidth = maxOf(1f, shape.strokeWidth * scale)
                            style = Paint.Style.STROKE
                        }
                        when (shape.type) {
                            ShapeType.RECTANGLE -> canvas.drawRect(rect, strokePaint)
                            ShapeType.ROUNDED_RECTANGLE -> canvas.drawRoundRect(rect, shape.cornerRadius * scale, shape.cornerRadius * scale, strokePaint)
                            ShapeType.CIRCLE -> canvas.drawOval(rect, strokePaint)
                            else -> canvas.drawRect(rect, strokePaint)
                        }
                    }
                }
                LayerType.TEXT -> {
                    val textData = layer.textData ?: TextLayerData()
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = textData.color.toInt()
                        alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                        textSize = maxOf(6f, textData.fontSize * scale)
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = textData.isBold
                        if (textData.isItalic) textSkewX = -0.25f
                    }
                    val fm = textPaint.fontMetrics
                    val textY = -(fm.descent + fm.ascent) / 2f
                    canvas.drawText(textData.text, 0f, textY, textPaint)
                }
                LayerType.VECTOR -> {
                    val vectorPath = layer.vectorPathData
                    if (vectorPath != null && vectorPath.points.isNotEmpty()) {
                        val path = Path()
                        val points = vectorPath.points
                        path.moveTo(points[0].x * scale, points[0].y * scale)
                        val count = if (vectorPath.isClosed) points.size else points.size - 1
                        for (i in 0 until count) {
                            val p0 = points[i]
                            val p1 = points[(i + 1) % points.size]
                            if (p0.hasOutHandle || p1.hasInHandle) {
                                path.cubicTo(
                                    p0.outHandleX * scale, p0.outHandleY * scale,
                                    p1.inHandleX * scale, p1.inHandleY * scale,
                                    p1.x * scale, p1.y * scale
                                )
                            } else {
                                path.lineTo(p1.x * scale, p1.y * scale)
                            }
                        }
                        if (vectorPath.isClosed) path.close()

                        if (vectorPath.hasFill) {
                            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = vectorPath.fillColor.toInt()
                                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                                style = Paint.Style.FILL
                            }
                            canvas.drawPath(path, fillPaint)
                        }
                        if (vectorPath.hasStroke && vectorPath.strokeWidth > 0) {
                            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = vectorPath.strokeColor.toInt()
                                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                                strokeWidth = maxOf(1f, vectorPath.strokeWidth * scale)
                                style = Paint.Style.STROKE
                            }
                            canvas.drawPath(path, strokePaint)
                        }
                    }
                }
                LayerType.MEDIA -> {
                    val mediaPath = layer.mediaAssetPath
                    if (mediaPath != null) {
                        val file = if (mediaPath.startsWith("/")) File(mediaPath)
                        else File(File(context.filesDir, "projects"), "${document.id}_assets/${mediaPath.removePrefix("assets/")}")
                        if (file.exists()) {
                            try {
                                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                                val mediaBmp = BitmapFactory.decodeFile(file.absolutePath, opts)
                                if (mediaBmp != null) {
                                    val mw = mediaBmp.width.toFloat() * scale
                                    val mh = mediaBmp.height.toFloat() * scale
                                    val dst = RectF(-mw / 2f, -mh / 2f, mw / 2f, mh / 2f)
                                    val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                                    }
                                    canvas.drawBitmap(mediaBmp, null, dst, bmpPaint)
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                }
                else -> {}
            }
            canvas.restore()
            canvas.restore()
        }

        memoryCache.put(document.id, bitmap)
        return bitmap
    }

    fun renderToPng(context: Context, document: ProjectDocument): ByteArray {
        val bitmap = renderThumbnail(context, document)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        return stream.toByteArray()
    }

    fun saveThumbnail(context: Context, document: ProjectDocument): File {
        val bitmap = renderThumbnail(context, document)
        val file = getThumbnailFile(context, document.id)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
        }
        return file
    }

    fun duplicateThumbnail(context: Context, sourceId: String, newId: String) {
        val srcFile = getThumbnailFile(context, sourceId)
        val dstFile = getThumbnailFile(context, newId)
        if (srcFile.exists()) {
            try {
                srcFile.copyTo(dstFile, overwrite = true)
                memoryCache.get(sourceId)?.let { memoryCache.put(newId, it) }
            } catch (_: Throwable) {}
        }
    }

    fun deleteThumbnail(context: Context, projectId: String) {
        memoryCache.remove(projectId)
        try {
            getThumbnailFile(context, projectId).delete()
        } catch (_: Throwable) {}
    }
}
