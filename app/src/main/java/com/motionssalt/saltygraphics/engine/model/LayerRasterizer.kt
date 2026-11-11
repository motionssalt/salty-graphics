package com.motionssalt.saltygraphics.engine.model

import android.content.Context
import android.graphics.Bitmap
import com.motionssalt.saltygraphics.media.MediaCache
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import java.io.File
import java.util.UUID

object LayerRasterizer {
    /**
     * Rasterizes a vector, text, or shape layer into a pixel-based MEDIA layer at project resolution.
     * Tight bounds are computed to content (including stroke/shadow), content outside canvas is preserved.
     * Layer's blendMode, opacity, mask, and effects remain as layer properties applied afterwards.
     */
    fun rasterizeLayer(
        context: Context,
        document: ProjectDocument,
        layerId: String,
        duplicateOriginal: Boolean
    ): ProjectDocument {
        val layer = document.findLayerById(layerId) ?: return document
        if (layer.type !in listOf(LayerType.TEXT, LayerType.VECTOR, LayerType.SHAPE)) return document

        val canvasW = document.canvasSize.width
        val canvasH = document.canvasSize.height

        // Render layer's content (content, fill, stroke, shadow) at full project resolution
        val fullBmp = LayerCompositor.renderLayerContent(layer, canvasW, canvasH, document, forceVisible = true)
        val w = fullBmp.width
        val h = fullBmp.height
        val pixels = IntArray(w * h)
        fullBmp.getPixels(pixels, 0, w, 0, 0, w, h)

        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        for (y in 0 until h) {
            for (x in 0 until w) {
                val a = (pixels[y * w + x] ushr 24) and 0xFF
                if (a > 0) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        val canvasCenterX = canvasW / 2f
        val canvasCenterY = canvasH / 2f

        val (croppedBmp, newCenterX, newCenterY) = if (maxX >= minX && maxY >= minY) {
            val cropW = maxX - minX + 1
            val cropH = maxY - minY + 1
            val cropped = Bitmap.createBitmap(fullBmp, minX, minY, cropW, cropH)
            val cx = minX + cropW / 2f
            val cy = minY + cropH / 2f
            Triple(cropped, cx - canvasCenterX, cy - canvasCenterY)
        } else {
            val fallback = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            Triple(fallback, layer.transform.translationX, layer.transform.translationY)
        }

        // Save asset to cache and storage
        val assetKey = "rasterized_${UUID.randomUUID()}.png"
        MediaCache.put("${document.id}:$assetKey", croppedBmp)
        val assetsDir = File(File(context.filesDir, "projects"), "${document.id}_assets")
        assetsDir.mkdirs()
        val file = File(assetsDir, assetKey)
        file.outputStream().use { croppedBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val rasterized = Layer(
            id = UUID.randomUUID().toString(),
            name = "${layer.name} (Rasterized)",
            type = LayerType.MEDIA,
            mediaAssetPath = assetKey,
            mediaWidth = croppedBmp.width.toFloat(),
            mediaHeight = croppedBmp.height.toFloat(),
            transform = Transform2D(
                translationX = newCenterX,
                translationY = newCenterY,
                scaleX = 1f,
                scaleY = 1f,
                rotationDegrees = 0f
            ),
            blendMode = layer.blendMode,
            opacity = layer.opacity,
            maskType = layer.maskType,
            maskSourceLayerId = layer.maskSourceLayerId,
            isMaskInverted = layer.isMaskInverted,
            effects = layer.effects
        )

        fun replaceInList(list: List<Layer>): List<Layer> {
            val idx = list.indexOfFirst { it.id == layerId }
            if (idx < 0) {
                return list.map { if (it.type == LayerType.GROUP) it.copy(children = replaceInList(it.children)) else it }
            }
            val mutable = list.toMutableList()
            if (duplicateOriginal) {
                // Keep original directly beneath rasterized layer, hidden
                mutable[idx] = rasterized
                mutable.add(idx + 1, layer.copy(isVisible = false))
            } else {
                mutable[idx] = rasterized
            }
            return mutable
        }

        val updatedLayers = replaceInList(document.layers)
        return document.copy(
            layers = updatedLayers,
            selectedLayerIds = setOf(rasterized.id),
            updatedAt = System.currentTimeMillis()
        )
    }
}
