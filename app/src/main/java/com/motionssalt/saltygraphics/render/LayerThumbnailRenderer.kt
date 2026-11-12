package com.motionssalt.saltygraphics.render

import android.graphics.*
import android.util.LruCache
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.FillRenderer
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import kotlin.math.*

/**
 * Renders small layer-content thumbnails for the Layers panel.
 * Uses the real rendering pipeline on a checkerboard background with caching and fallback.
 */
object LayerThumbnailRenderer {
    private val memoryCache = LruCache<String, Bitmap>(128)

    fun clearCache() {
        memoryCache.evictAll()
    }

    fun computeCacheKey(layer: Layer): String {
        return "${layer.id}_${layer.hashCode()}"
    }

    fun getCached(layer: Layer): Bitmap? {
        return memoryCache.get(computeCacheKey(layer))
    }

    fun putInCache(layer: Layer, bitmap: Bitmap) {
        memoryCache.put(computeCacheKey(layer), bitmap)
    }

    /**
     * Renders a layer's actual content to a small thumbnail bitmap.
     * Returns null for adjustment layers and empty layers/groups so the caller falls back to type icons.
     */
    fun renderThumbnail(
        layer: Layer,
        thumbSizePx: Int = 72,
        mediaResolver: ((String) -> Bitmap?)? = null
    ): Bitmap? {
        // Adjustment layers and quarantined layers show fallback icon
        if (layer.type == LayerType.ADJUSTMENT || layer.type == LayerType.QUARANTINED) {
            return null
        }
        val text = layer.textData
        // Empty text layer shows fallback icon
        if (layer.type == LayerType.TEXT && (text == null || text.text.isBlank())) {
            return null
        }
        // Empty vector layer shows fallback icon
        if (layer.type == LayerType.VECTOR && layer.editablePaths.all { it.points.isEmpty() }) {
            return null
        }
        // Empty group layer shows fallback icon
        if (layer.type == LayerType.GROUP && layer.children.isEmpty()) {
            return null
        }

        val cacheKey = computeCacheKey(layer)
        memoryCache.get(cacheKey)?.let { return it }

        try {
            val bitmap = Bitmap.createBitmap(thumbSizePx, thumbSizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Draw checkerboard for transparency
            drawCheckerboard(canvas, thumbSizePx, thumbSizePx)

            // Draw content
            if (layer.type == LayerType.GROUP) {
                drawGroupContent(canvas, layer, thumbSizePx, mediaResolver)
            } else {
                drawSingleLayerContent(canvas, layer, thumbSizePx, mediaResolver)
            }

            memoryCache.put(cacheKey, bitmap)
            return bitmap
        } catch (_: Throwable) {
            return null
        }
    }

    fun drawCheckerboard(canvas: Canvas, w: Int, h: Int, cellSize: Float = 8f) {
        val lightPaint = Paint().apply { color = 0xFF363638.toInt() }
        val darkPaint = Paint().apply { color = 0xFF242426.toInt() }
        val cols = ceil(w / cellSize).toInt()
        val rows = ceil(h / cellSize).toInt()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val p = if ((r + c) % 2 == 0) lightPaint else darkPaint
                canvas.drawRect(
                    c * cellSize,
                    r * cellSize,
                    minOf((c + 1) * cellSize, w.toFloat()),
                    minOf((r + 1) * cellSize, h.toFloat()),
                    p
                )
            }
        }
    }

    private fun drawGroupContent(
        canvas: Canvas,
        group: Layer,
        sizePx: Int,
        mediaResolver: ((String) -> Bitmap?)?
    ) {
        // Find children bounding box
        val children = group.children.filter { it.isVisible && it.type != LayerType.ADJUSTMENT }
        if (children.isEmpty()) return

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (child in children) {
            val cx = child.transform.translationX
            val cy = child.transform.translationY
            val hw = (child.localWidth * abs(child.transform.scaleX)) / 2f
            val hh = (child.localHeight * abs(child.transform.scaleY)) / 2f
            minX = minOf(minX, cx - hw)
            maxX = maxOf(maxX, cx + hw)
            minY = minOf(minY, cy - hh)
            maxY = maxOf(maxY, cy + hh)
        }

        val groupW = maxOf(10f, maxX - minX)
        val groupH = maxOf(10f, maxY - minY)
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f

        val maxDim = maxOf(groupW, groupH)
        val scale = (sizePx * 0.72f) / maxDim

        canvas.save()
        canvas.translate(sizePx / 2f, sizePx / 2f)
        canvas.scale(scale, scale)
        canvas.translate(-centerX, -centerY)

        for (child in children.asReversed()) {
            drawSingleLayerAtCanvasPosition(canvas, child, mediaResolver)
        }
        canvas.restore()
    }

    private fun drawSingleLayerContent(
        canvas: Canvas,
        layer: Layer,
        sizePx: Int,
        mediaResolver: ((String) -> Bitmap?)?
    ) {
        val maxDim = maxOf(
            10f,
            layer.localWidth * abs(layer.transform.scaleX),
            layer.localHeight * abs(layer.transform.scaleY)
        )
        val scale = (sizePx * 0.72f) / maxDim

        canvas.save()
        canvas.translate(sizePx / 2f, sizePx / 2f)
        canvas.scale(scale, scale)
        canvas.rotate(layer.transform.rotationDegrees)
        canvas.scale(layer.transform.scaleX, layer.transform.scaleY)

        drawLayerPrimitives(canvas, layer, mediaResolver)
        canvas.restore()
    }

    private fun drawSingleLayerAtCanvasPosition(
        canvas: Canvas,
        layer: Layer,
        mediaResolver: ((String) -> Bitmap?)?
    ) {
        canvas.save()
        canvas.translate(layer.transform.translationX, layer.transform.translationY)
        canvas.rotate(layer.transform.rotationDegrees)
        canvas.scale(layer.transform.scaleX, layer.transform.scaleY)

        drawLayerPrimitives(canvas, layer, mediaResolver)
        canvas.restore()
    }

    private fun drawLayerPrimitives(
        canvas: Canvas,
        layer: Layer,
        mediaResolver: ((String) -> Bitmap?)?
    ) {
        val fill = layer.effectiveFill
        val stroke = layer.effectiveStroke
        val shadow = layer.effectiveShadow

        when (layer.type) {
            LayerType.SHAPE -> {
                val shape = layer.shapeData ?: ShapeData()
                val halfW = shape.width / 2f
                val halfH = shape.height / 2f
                val rect = RectF(-halfW, -halfH, halfW, halfH)
                val shapePath = when (shape.type) {
                    ShapeType.RECTANGLE -> Path().apply { addRect(rect, Path.Direction.CW) }
                    ShapeType.ROUNDED_RECTANGLE -> Path().apply { addRoundRect(rect, shape.cornerRadius, shape.cornerRadius, Path.Direction.CW) }
                    ShapeType.CIRCLE -> Path().apply { addOval(rect, Path.Direction.CW) }
                    else -> LayerCompositor.buildVectorPath(shape.toVectorPath())
                }

                if (shadow.isEnabled) {
                    val shadowPaint = Paint()
                    if (FillRenderer.configureShadowPaint(shadowPaint, shadow)) {
                        FillRenderer.drawPathShadowWithStroke(canvas, shapePath, stroke, shadow, shadowPaint)
                    }
                }

                val mediaPath = fill.mediaAssetPath
                if (fill.type == FillType.MEDIA && mediaPath != null) {
                    val bmp = mediaResolver?.invoke(mediaPath)
                    if (bmp != null) {
                        canvas.save()
                        canvas.clipPath(shapePath)
                        val (srcR, dstR) = FillRenderer.computeMediaPlacement(
                            bmp.width.toFloat(), bmp.height.toFloat(), rect, fill.mediaScaleMode, fill.mediaOffsetX, fill.mediaOffsetY
                        )
                        canvas.drawBitmap(bmp, Rect(srcR.left.toInt(), srcR.top.toInt(), srcR.right.toInt(), srcR.bottom.toInt()), dstR, Paint(Paint.ANTI_ALIAS_FLAG))
                        canvas.restore()
                    }
                } else {
                    val fillPaint = Paint()
                    if (FillRenderer.configureFillPaint(fillPaint, fill, rect)) {
                        canvas.drawPath(shapePath, fillPaint)
                    }
                }

                val strokePaint = Paint()
                if (FillRenderer.configureStrokePaint(strokePaint, stroke)) {
                    FillRenderer.drawPathWithStroke(canvas, shapePath, stroke, strokePaint)
                }
            }

            LayerType.TEXT -> {
                val textData = layer.textData ?: TextLayerData()
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = textData.fontSize
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = textData.isBold
                    if (textData.isItalic) textSkewX = -0.25f
                    color = fill.effectiveSolidArgb.toInt()
                }
                val bounds = RectF(-textData.boxWidth / 2f, -textData.boxHeight / 2f, textData.boxWidth / 2f, textData.boxHeight / 2f)
                FillRenderer.configureFillPaint(textPaint, fill, bounds)

                var strokePaint: Paint? = null
                if (stroke.isEnabled && stroke.width > 0f) {
                    strokePaint = Paint(textPaint)
                    FillRenderer.configureStrokePaint(strokePaint, stroke)
                } else if (textData.hasOutline) {
                    strokePaint = Paint(textPaint).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = textData.outlineWidth
                        color = textData.outlineColor.toInt()
                    }
                }

                var shadowPaint: Paint? = null
                if (shadow.isEnabled) {
                    shadowPaint = Paint(textPaint)
                    FillRenderer.configureShadowPaint(shadowPaint, shadow)
                }

                com.motionssalt.saltygraphics.engine.render.TextLayoutEngine.drawTextLayout(
                    canvas = canvas,
                    textData = textData,
                    textPaint = textPaint,
                    strokePaint = strokePaint,
                    shadowPaint = shadowPaint
                )
            }

            LayerType.VECTOR -> {
                layer.editablePaths.forEach { data ->
                    val path = LayerCompositor.buildVectorPath(data)
                    val bounds = RectF()
                    path.computeBounds(bounds, true)

                    if (shadow.isEnabled) {
                        val shadowPaint = Paint()
                        if (FillRenderer.configureShadowPaint(shadowPaint, shadow)) {
                            FillRenderer.drawPathShadowWithStroke(canvas, path, stroke, shadow, shadowPaint)
                        }
                    }

                    val fillPaint = Paint()
                    if (FillRenderer.configureFillPaint(fillPaint, fill, bounds)) {
                        canvas.drawPath(path, fillPaint)
                    }

                    val strokePaint = Paint()
                    if (FillRenderer.configureStrokePaint(strokePaint, stroke)) {
                        FillRenderer.drawPathWithStroke(canvas, path, stroke, strokePaint)
                    }
                }
            }

            LayerType.MEDIA -> {
                val mw = layer.mediaWidth ?: 300f
                val mh = layer.mediaHeight ?: 300f
                val dst = RectF(-mw / 2f, -mh / 2f, mw / 2f, mh / 2f)
                val bmp = (fill.mediaAssetPath ?: layer.mediaAssetPath)?.let { mediaResolver?.invoke(it) }

                if (shadow.isEnabled) {
                    val shadowPaint = Paint()
                    if (FillRenderer.configureShadowPaint(shadowPaint, shadow)) {
                        canvas.save()
                        canvas.translate(shadow.offsetX, shadow.offsetY)
                        canvas.drawRect(dst, shadowPaint)
                        canvas.restore()
                    }
                }

                if (bmp != null) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    val (srcR, dstR) = FillRenderer.computeMediaPlacement(
                        bmp.width.toFloat(), bmp.height.toFloat(), dst, fill.mediaScaleMode, fill.mediaOffsetX, fill.mediaOffsetY
                    )
                    canvas.drawBitmap(bmp, Rect(srcR.left.toInt(), srcR.top.toInt(), srcR.right.toInt(), srcR.bottom.toInt()), dstR, paint)
                } else {
                    val fillPaint = Paint()
                    if (FillRenderer.configureFillPaint(fillPaint, fill, dst)) {
                        canvas.drawRect(dst, fillPaint)
                    } else {
                        val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = 0xFF888888.toInt()
                            style = Paint.Style.FILL
                        }
                        canvas.drawRect(dst, fallbackPaint)
                    }
                }

                val strokePaint = Paint()
                if (FillRenderer.configureStrokePaint(strokePaint, stroke)) {
                    val mediaPath = Path().apply { addRect(dst, Path.Direction.CW) }
                    FillRenderer.drawPathWithStroke(canvas, mediaPath, stroke, strokePaint)
                }
            }

            else -> {}
        }
    }
}
