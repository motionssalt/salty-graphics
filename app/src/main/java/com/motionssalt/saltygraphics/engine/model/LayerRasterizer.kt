package com.motionssalt.saltygraphics.engine.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.motionssalt.saltygraphics.media.MediaCache
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import java.io.File
import java.util.UUID
import kotlin.math.*

object LayerRasterizer {

    /**
     * Rasterizes a vector, text, or shape layer into a pixel-based MEDIA layer at project resolution.
     * Tight bounds are computed to content (including stroke/shadow), content outside canvas is preserved (not clipped).
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

        // Determine estimated local bounds of the layer's unclipped content
        val (localW, localH) = when (layer.type) {
            LayerType.SHAPE -> {
                val s = layer.shapeData ?: ShapeData()
                val sw = max(s.width, 50f)
                val sh = max(s.height, 50f)
                val strokePad = if (layer.effectiveStroke.isEnabled) layer.effectiveStroke.width * 2f else 20f
                val shadowPad = if (layer.effectiveShadow.isEnabled) (abs(layer.effectiveShadow.offsetX) + abs(layer.effectiveShadow.offsetY) + layer.effectiveShadow.blurRadius * 2f) else 0f
                Pair(sw + strokePad + shadowPad + 40f, sh + strokePad + shadowPad + 40f)
            }
            LayerType.TEXT -> {
                val t = layer.textData ?: TextLayerData()
                val tw = max(t.boxWidth, 200f)
                val th = max(t.boxHeight, 150f)
                val strokePad = if (layer.effectiveStroke.isEnabled) layer.effectiveStroke.width * 2f else 20f
                val shadowPad = if (layer.effectiveShadow.isEnabled) (abs(layer.effectiveShadow.offsetX) + abs(layer.effectiveShadow.offsetY) + layer.effectiveShadow.blurRadius * 2f) else 0f
                Pair(tw + strokePad + shadowPad + 40f, th + strokePad + shadowPad + 40f)
            }
            LayerType.VECTOR -> {
                var maxDim = 400f
                layer.editablePaths.forEach { p ->
                    val pathW = max(50f, p.points.map { it.x }.let { if (it.isEmpty()) 50f else (it.maxOrNull()!! - it.minOrNull()!!) })
                    val pathH = max(50f, p.points.map { it.y }.let { if (it.isEmpty()) 50f else (it.maxOrNull()!! - it.minOrNull()!!) })
                    maxDim = max(maxDim, max(pathW, pathH))
                }
                val strokePad = if (layer.effectiveStroke.isEnabled) layer.effectiveStroke.width * 2f else 20f
                val shadowPad = if (layer.effectiveShadow.isEnabled) (abs(layer.effectiveShadow.offsetX) + abs(layer.effectiveShadow.offsetY) + layer.effectiveShadow.blurRadius * 2f) else 0f
                Pair(maxDim + strokePad + shadowPad + 40f, maxDim + strokePad + shadowPad + 40f)
            }
            else -> Pair(400f, 400f)
        }

        // Expanded buffer size ensuring content outside the canvas is preserved
        val bufferW = max(canvasW * 2, max(512, (localW * max(1f, layer.transform.scaleX) * 2f).roundToInt())).coerceAtMost(4096)
        val bufferH = max(canvasH * 2, max(512, (localH * max(1f, layer.transform.scaleY) * 2f).roundToInt())).coerceAtMost(4096)

        val fullBmp = LayerCompositor.renderLayerContent(layer, bufferW, bufferH, document, forceVisible = true)
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

        val bufferCenterX = bufferW / 2f
        val bufferCenterY = bufferH / 2f

        val (croppedBmp, newCenterX, newCenterY) = if (maxX >= minX && maxY >= minY) {
            val cropW = maxX - minX + 1
            val cropH = maxY - minY + 1
            val cropped = Bitmap.createBitmap(fullBmp, minX, minY, cropW, cropH)
            val cx = minX + cropW / 2f
            val cy = minY + cropH / 2f
            // Map offset from buffer center directly to document coordinate center
            Triple(cropped, (cx - bufferCenterX) + layer.transform.translationX - (cx - bufferCenterX), (cy - bufferCenterY) + layer.transform.translationY - (cy - bufferCenterY))
        } else {
            val fallback = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            Triple(fallback, layer.transform.translationX, layer.transform.translationY)
        }

        // Accurate translation calculation:
        // In renderLayerContent with bufferW/bufferH, the layer is translated to bufferCenter + layer.transform.translation
        // So the cropped center cx in buffer coordinates corresponds to document offset:
        val resolvedCenterX = if (maxX >= minX && maxY >= minY) {
            val cropW = maxX - minX + 1
            val cx = minX + cropW / 2f
            cx - bufferCenterX
        } else layer.transform.translationX

        val resolvedCenterY = if (maxX >= minX && maxY >= minY) {
            val cropH = maxY - minY + 1
            val cy = minY + cropH / 2f
            cy - bufferCenterY
        } else layer.transform.translationY

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
                translationX = resolvedCenterX,
                translationY = resolvedCenterY,
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
