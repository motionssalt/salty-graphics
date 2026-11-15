package com.motionssalt.saltygraphics.engine.model

import android.content.Context
import android.graphics.Bitmap
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import com.motionssalt.saltygraphics.media.MediaCache
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object LayerRasterizer {

    private fun estimateLayerLocalSpan(layer: Layer): Pair<Float, Float> {
        val strokePad = if (layer.effectiveStroke.isEnabled) layer.effectiveStroke.width * 2f else 20f
        val shadowPad = if (layer.effectiveShadow.isEnabled) {
            abs(layer.effectiveShadow.offsetX) + abs(layer.effectiveShadow.offsetY) + layer.effectiveShadow.blurRadius * 2f
        } else 0f
        val extraPad = strokePad + shadowPad + 40f
        return when (layer.type) {
            LayerType.SHAPE -> {
                val s = layer.shapeData ?: ShapeData()
                (max(s.width, 50f) + extraPad) to (max(s.height, 50f) + extraPad)
            }
            LayerType.TEXT -> {
                val t = layer.textData ?: TextLayerData()
                (max(t.boxWidth, 200f) + extraPad) to (max(t.boxHeight, 150f) + extraPad)
            }
            LayerType.MEDIA -> {
                val mw = max(layer.mediaWidth ?: 300f, 50f)
                val mh = max(layer.mediaHeight ?: 300f, 50f)
                (mw + extraPad) to (mh + extraPad)
            }
            LayerType.VECTOR -> {
                var maxDim = 400f
                layer.editablePaths.forEach { p ->
                    val xs = p.points.map { it.x }
                    val ys = p.points.map { it.y }
                    val pathW = if (xs.isEmpty()) 50f else max(50f, xs.maxOrNull()!! - xs.minOrNull()!!)
                    val pathH = if (ys.isEmpty()) 50f else max(50f, ys.maxOrNull()!! - ys.minOrNull()!!)
                    maxDim = max(maxDim, max(pathW, pathH))
                }
                (maxDim + extraPad) to (maxDim + extraPad)
            }
            LayerType.GROUP -> {
                var maxW = 400f
                var maxH = 400f
                layer.children.forEach { child ->
                    val (cw, ch) = estimateLayerLocalSpan(child)
                    val spanX = cw * abs(child.transform.scaleX) + abs(child.transform.translationX) * 2f
                    val spanY = ch * abs(child.transform.scaleY) + abs(child.transform.translationY) * 2f
                    maxW = max(maxW, spanX)
                    maxH = max(maxH, spanY)
                }
                maxW to maxH
            }
            else -> 400f to 400f
        }
    }

    private fun computeExpandedBufferSize(document: ProjectDocument, targets: List<Layer>): Pair<Int, Int> {
        val canvasW = document.canvasSize.width
        val canvasH = document.canvasSize.height
        var reqW = max(canvasW * 2, 512)
        var reqH = max(canvasH * 2, 512)
        targets.forEach { layer ->
            val (localW, localH) = estimateLayerLocalSpan(layer)
            val w = (localW * max(1f, abs(layer.transform.scaleX)) * 2f + abs(layer.transform.translationX) * 2f).roundToInt()
            val h = (localH * max(1f, abs(layer.transform.scaleY)) * 2f + abs(layer.transform.translationY) * 2f).roundToInt()
            reqW = max(reqW, w)
            reqH = max(reqH, h)
        }
        return reqW.coerceAtMost(4096) to reqH.coerceAtMost(4096)
    }

    private fun cropAndPersistBitmap(
        context: Context,
        document: ProjectDocument,
        fullBmp: Bitmap,
        defaultCenterX: Float,
        defaultCenterY: Float
    ): Triple<Bitmap, Float, Float> {
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

        val bufferCenterX = w / 2f
        val bufferCenterY = h / 2f
        val croppedBmp = if (maxX >= minX && maxY >= minY) {
            val cropW = maxX - minX + 1
            val cropH = maxY - minY + 1
            Bitmap.createBitmap(fullBmp, minX, minY, cropW, cropH)
        } else {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val resolvedCenterX = if (maxX >= minX && maxY >= minY) {
            val cropW = maxX - minX + 1
            val cx = minX + cropW / 2f
            cx - bufferCenterX
        } else defaultCenterX

        val resolvedCenterY = if (maxX >= minX && maxY >= minY) {
            val cropH = maxY - minY + 1
            val cy = minY + cropH / 2f
            cy - bufferCenterY
        } else defaultCenterY

        return Triple(croppedBmp, resolvedCenterX, resolvedCenterY)
    }

    private fun saveRasterizedAsset(context: Context, documentId: String, bitmap: Bitmap): String {
        val assetKey = "rasterized_${UUID.randomUUID()}.png"
        MediaCache.put("$documentId:$assetKey", bitmap)
        val assetsDir = File(File(context.filesDir, "projects"), "${documentId}_assets")
        assetsDir.mkdirs()
        File(assetsDir, assetKey).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return assetKey
    }

    /**
     * Rasterizes a single vector, text, or shape layer into a pixel-based MEDIA layer at project resolution.
     * Preserves layer blendMode, opacity, and mask as properties of the rasterized layer.
     */
    fun rasterizeLayer(
        context: Context,
        document: ProjectDocument,
        layerId: String,
        duplicateOriginal: Boolean
    ): ProjectDocument {
        val layer = document.findLayerById(layerId) ?: return document
        if (layer.type == LayerType.GROUP) {
            return rasterizeSelection(context, document, setOf(layerId), duplicateOriginal)
        }
        if (layer.type !in listOf(LayerType.TEXT, LayerType.VECTOR, LayerType.SHAPE)) return document

        val (bufferW, bufferH) = computeExpandedBufferSize(document, listOf(layer))
        val mediaResolver: (String) -> Bitmap? = { key -> MediaCache.getOrRequest(context, document.id, key) }
        val fullBmp = LayerCompositor.renderLayerContent(
            layer = layer,
            width = bufferW,
            height = bufferH,
            document = document,
            mediaResolver = mediaResolver,
            forceVisible = true
        )

        val (croppedBmp, resolvedCenterX, resolvedCenterY) = cropAndPersistBitmap(
            context,
            document,
            fullBmp,
            layer.transform.translationX,
            layer.transform.translationY
        )
        fullBmp.recycle()

        val assetKey = saveRasterizedAsset(context, document.id, croppedBmp)

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
            effects = emptyList()
        )

        fun replaceInList(list: List<Layer>): List<Layer> {
            val idx = list.indexOfFirst { it.id == layerId }
            if (idx < 0) {
                return list.map { if (it.type == LayerType.GROUP) it.copy(children = replaceInList(it.children)) else it }
            }
            val mutable = list.toMutableList()
            if (duplicateOriginal) {
                mutable[idx] = rasterized
                mutable.add(idx + 1, layer.copy(isVisible = false))
            } else {
                mutable[idx] = rasterized
            }
            return mutable
        }

        return document.copy(
            layers = replaceInList(document.layers),
            selectedLayerIds = setOf(rasterized.id),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * V40: Flattens multiple selected layers or a selected group into one bitmap MEDIA layer
     * placed at the top-most selected position in the stack, respecting visibility, opacity,
     * blend modes, masks, effects, and transforms.
     */
    fun rasterizeSelection(
        context: Context,
        document: ProjectDocument,
        layerIds: Set<String>,
        duplicateOriginal: Boolean
    ): ProjectDocument {
        if (layerIds.isEmpty()) return document

        if (layerIds.size == 1) {
            val single = document.findLayerById(layerIds.first()) ?: return document
            if (single.type in listOf(LayerType.TEXT, LayerType.VECTOR, LayerType.SHAPE)) {
                return rasterizeLayer(context, document, single.id, duplicateOriginal)
            }
        }

        fun findContainingList(list: List<Layer>): List<Layer>? {
            if (list.any { it.id in layerIds }) return list
            list.forEach { if (it.type == LayerType.GROUP) findContainingList(it.children)?.let { found -> return found } }
            return null
        }

        val siblingList = findContainingList(document.layers) ?: return document
        val selectedTopFirst = siblingList.filter { it.id in layerIds }
        if (selectedTopFirst.isEmpty()) return document

        val (bufferW, bufferH) = computeExpandedBufferSize(document, selectedTopFirst)
        val mediaResolver: (String) -> Bitmap? = { key -> MediaCache.getOrRequest(context, document.id, key) }

        val singleGroup = selectedTopFirst.singleOrNull()?.takeIf { it.type == LayerType.GROUP }
        val layersToComposite = if (singleGroup != null) {
            listOf(singleGroup.copy(blendMode = BlendMode.NORMAL, isVisible = true))
        } else {
            selectedTopFirst
        }

        val pixels = LayerCompositor.compositeLayerListToPixels(
            layersTopFirst = layersToComposite,
            document = document,
            width = bufferW,
            height = bufferH,
            mediaResolver = mediaResolver,
            forceVisible = false
        )
        val fullBmp = Bitmap.createBitmap(bufferW, bufferH, Bitmap.Config.ARGB_8888)
        fullBmp.setPixels(pixels, 0, bufferW, 0, 0, bufferW, bufferH)

        val fallbackLayer = selectedTopFirst.first()
        val (croppedBmp, resolvedCenterX, resolvedCenterY) = cropAndPersistBitmap(
            context,
            document,
            fullBmp,
            fallbackLayer.transform.translationX,
            fallbackLayer.transform.translationY
        )
        fullBmp.recycle()

        val assetKey = saveRasterizedAsset(context, document.id, croppedBmp)

        val rasterizedName = if (singleGroup != null) {
            "${singleGroup.name} (Rasterized)"
        } else {
            "${fallbackLayer.name} (Merged)"
        }

        val rasterized = Layer(
            id = UUID.randomUUID().toString(),
            name = rasterizedName,
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
            blendMode = singleGroup?.blendMode ?: BlendMode.NORMAL,
            opacity = 1f,
            maskType = MaskType.NONE,
            maskSourceLayerId = null,
            isMaskInverted = false,
            effects = emptyList()
        )

        fun replaceSelectionInList(list: List<Layer>): List<Layer> {
            val topIndex = list.indexOfFirst { it.id in layerIds }
            if (topIndex < 0) {
                return list.map { if (it.type == LayerType.GROUP) it.copy(children = replaceSelectionInList(it.children)) else it }
            }
            val result = mutableListOf<Layer>()
            var inserted = false
            list.forEachIndexed { idx, item ->
                if (idx == topIndex && !inserted) {
                    result.add(rasterized)
                    inserted = true
                }
                if (item.id in layerIds) {
                    if (duplicateOriginal) {
                        result.add(item.copy(isVisible = false))
                    }
                } else {
                    result.add(item)
                }
            }
            if (!inserted) result.add(0, rasterized)
            return result
        }

        return document.copy(
            layers = replaceSelectionInList(document.layers),
            selectedLayerIds = setOf(rasterized.id),
            updatedAt = System.currentTimeMillis()
        )
    }
}
