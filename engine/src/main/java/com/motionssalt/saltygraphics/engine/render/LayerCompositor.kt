package com.motionssalt.saltygraphics.engine.render

import android.graphics.Canvas
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import com.motionssalt.saltygraphics.engine.model.BlendMode as EngineBlendMode
import com.motionssalt.saltygraphics.engine.model.*
import kotlin.math.*

/**
 * Unified Layer Compositing Pipeline.
 *
 * Implements W3C/Porter-Duff multi-layer compositing, all 23 blend modes,
 * non-destructive masks (Alpha, Luminance, Vector), recursive group compositing,
 * and invisible adjustment layer processing.
 */
object LayerCompositor {

    /**
     * Maps engine BlendMode to Android Graphics BlendMode (API 29+).
     */
    fun toAndroidBlendMode(mode: EngineBlendMode): android.graphics.BlendMode {
        return when (mode) {
            EngineBlendMode.NORMAL -> android.graphics.BlendMode.SRC_OVER
            EngineBlendMode.DARKEN -> android.graphics.BlendMode.DARKEN
            EngineBlendMode.MULTIPLY -> android.graphics.BlendMode.MULTIPLY
            EngineBlendMode.COLOR_BURN -> android.graphics.BlendMode.COLOR_BURN
            EngineBlendMode.LINEAR_BURN -> android.graphics.BlendMode.DARKEN
            EngineBlendMode.LIGHTEN -> android.graphics.BlendMode.LIGHTEN
            EngineBlendMode.SCREEN -> android.graphics.BlendMode.SCREEN
            EngineBlendMode.COLOR_DODGE -> android.graphics.BlendMode.COLOR_DODGE
            EngineBlendMode.LINEAR_DODGE -> android.graphics.BlendMode.PLUS
            EngineBlendMode.OVERLAY -> android.graphics.BlendMode.OVERLAY
            EngineBlendMode.SOFT_LIGHT -> android.graphics.BlendMode.SOFT_LIGHT
            EngineBlendMode.HARD_LIGHT -> android.graphics.BlendMode.HARD_LIGHT
            EngineBlendMode.VIVID_LIGHT -> android.graphics.BlendMode.HARD_LIGHT
            EngineBlendMode.LINEAR_LIGHT -> android.graphics.BlendMode.HARD_LIGHT
            EngineBlendMode.PIN_LIGHT -> android.graphics.BlendMode.DARKEN
            EngineBlendMode.DIFFERENCE -> android.graphics.BlendMode.DIFFERENCE
            EngineBlendMode.EXCLUSION -> android.graphics.BlendMode.EXCLUSION
            EngineBlendMode.SUBTRACT -> android.graphics.BlendMode.DIFFERENCE
            EngineBlendMode.DIVIDE -> android.graphics.BlendMode.SCREEN
            EngineBlendMode.HUE -> android.graphics.BlendMode.HUE
            EngineBlendMode.SATURATION -> android.graphics.BlendMode.SATURATION
            EngineBlendMode.COLOR -> android.graphics.BlendMode.COLOR
            EngineBlendMode.LUMINOSITY -> android.graphics.BlendMode.LUMINOSITY
        }
    }

    /**
     * Maps engine BlendMode to PorterDuff Mode for Android API 26-28.
     */
    fun toPorterDuffMode(mode: EngineBlendMode): PorterDuff.Mode {
        return when (mode) {
            EngineBlendMode.NORMAL -> PorterDuff.Mode.SRC_OVER
            EngineBlendMode.DARKEN -> PorterDuff.Mode.DARKEN
            EngineBlendMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
            EngineBlendMode.LIGHTEN -> PorterDuff.Mode.LIGHTEN
            EngineBlendMode.SCREEN -> PorterDuff.Mode.SCREEN
            EngineBlendMode.OVERLAY -> PorterDuff.Mode.OVERLAY
            EngineBlendMode.LINEAR_DODGE -> PorterDuff.Mode.ADD
            else -> PorterDuff.Mode.SRC_OVER
        }
    }

    /**
     * Applies layer blend mode to an Android Paint.
     */
    fun applyLayerBlendMode(paint: Paint, mode: EngineBlendMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.blendMode = toAndroidBlendMode(mode)
        } else {
            paint.xfermode = PorterDuffXfermode(toPorterDuffMode(mode))
        }
    }

    /**
     * Applies a non-destructive mask onto a layer's ARGB pixel buffer.
     * Preserves partial float alpha values for soft/feathered edges.
     */
    fun applyMask(
        layerPixels: IntArray,
        maskPixels: IntArray,
        maskType: MaskType,
        isInverted: Boolean,
        width: Int,
        height: Int
    ) {
        if (maskType == MaskType.NONE) return
        val count = minOf(layerPixels.size, maskPixels.size, width * height)

        for (i in 0 until count) {
            val maskPx = maskPixels[i]
            val maskAlpha = ((maskPx ushr 24) and 0xFF) / 255f

            val factor: Float = when (maskType) {
                MaskType.ALPHA -> maskAlpha
                MaskType.LUMINANCE -> {
                    val r = ((maskPx ushr 16) and 0xFF) / 255f
                    val g = ((maskPx ushr 8) and 0xFF) / 255f
                    val b = (maskPx and 0xFF) / 255f
                    // Standard ITU-R BT.601 luminance weighted by mask opacity
                    (0.299f * r + 0.587f * g + 0.114f * b) * maskAlpha
                }
                MaskType.VECTOR -> maskAlpha
                MaskType.NONE -> 1f
            }

            val finalFactor = if (isInverted) (1f - factor).coerceIn(0f, 1f) else factor.coerceIn(0f, 1f)

            val layerPx = layerPixels[i]
            val origAlpha = ((layerPx ushr 24) and 0xFF) / 255f
            val newAlpha = (origAlpha * finalFactor).coerceIn(0f, 1f)
            val newAInt = (newAlpha * 255f).roundToInt().coerceIn(0, 255)

            layerPixels[i] = (newAInt shl 24) or (layerPx and 0x00FFFFFF)
        }
    }

    /**
     * Blends a source pixel buffer over a backdrop pixel buffer using BlendMath.
     */
    fun blendBuffers(
        backdrop: IntArray,
        source: IntArray,
        output: IntArray,
        mode: EngineBlendMode,
        opacity: Float,
        width: Int,
        height: Int
    ) {
        val count = minOf(backdrop.size, source.size, output.size, width * height)
        for (i in 0 until count) {
            val bArgb = backdrop[i].toLong() and 0xFFFFFFFFL
            val sArgb = source[i].toLong() and 0xFFFFFFFFL
            val out = BlendMath.blendColor(mode, bArgb, sArgb, opacity)
            output[i] = out.toInt()
        }
    }

    /**
     * Renders a single layer's content (isolated from backdrop) into a target bitmap.
     */
    fun renderLayerContent(
        layer: Layer,
        width: Int,
        height: Int,
        document: ProjectDocument? = null,
        mediaResolver: ((String) -> Bitmap?)? = null,
        forceVisible: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(maxOf(1, width), maxOf(1, height), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (!layer.isVisible && !forceVisible) return bitmap

        // Group layers composite their children recursively: index 0 is topmost, so draw bottom-to-top
        if (layer.type == LayerType.GROUP) {
            for (child in layer.children.asReversed()) {
                if (!child.isVisible && !forceVisible) continue
                val childBmp = renderLayerContent(child, width, height, document, mediaResolver, forceVisible)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha = (child.opacity * 255).roundToInt().coerceIn(0, 255)
                    applyLayerBlendMode(this, child.blendMode)
                }
                canvas.drawBitmap(childBmp, 0f, 0f, paint)
            }
            return bitmap
        }

        // Adjustment layer has no intrinsic shape content
        if (layer.type == LayerType.ADJUSTMENT) {
            return bitmap
        }

        val canvasCenterX = width / 2f
        val canvasCenterY = height / 2f
        val lx = canvasCenterX + layer.transform.translationX
        val ly = canvasCenterY + layer.transform.translationY

        canvas.save()
        canvas.translate(lx, ly)
        canvas.rotate(layer.transform.rotationDegrees)
        canvas.scale(layer.transform.scaleX, layer.transform.scaleY)

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
                    else -> buildVectorPath(shape.toVectorPath())
                }

                // 1. Draw Drop Shadow if enabled
                if (shadow.isEnabled) {
                    val shadowPaint = Paint()
                    if (FillRenderer.configureShadowPaint(shadowPaint, shadow)) {
                        canvas.save()
                        canvas.translate(shadow.offsetX, shadow.offsetY)
                        canvas.drawPath(shapePath, shadowPaint)
                        canvas.restore()
                    }
                }

                // 2. Draw Fill (Solid, Gradient, Media)
                if (fill.type == FillType.MEDIA && fill.mediaAssetPath != null) {
                    val bmp = mediaResolver?.invoke(fill.mediaAssetPath)
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

                // 3. Draw Stroke if enabled
                val strokePaint = Paint()
                if (FillRenderer.configureStrokePaint(strokePaint, stroke)) {
                    canvas.drawPath(shapePath, strokePaint)
                }
            }
            LayerType.TEXT -> {
                val textData = layer.textData ?: TextLayerData()
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = textData.fontSize
                    textAlign = when (textData.justification) {
                        TextJustification.LEFT -> Paint.Align.LEFT
                        TextJustification.RIGHT -> Paint.Align.RIGHT
                        else -> Paint.Align.CENTER
                    }
                    isFakeBoldText = textData.isBold
                    if (textData.isItalic) textSkewX = -0.25f
                }
                val fm = textPaint.fontMetrics
                val textY = -(fm.descent + fm.ascent) / 2f
                val x = when (textData.justification) {
                    TextJustification.LEFT -> -textData.boxWidth / 2f
                    TextJustification.RIGHT -> textData.boxWidth / 2f
                    else -> 0f
                }
                val bounds = RectF(-textData.boxWidth / 2f, -textData.boxHeight / 2f, textData.boxWidth / 2f, textData.boxHeight / 2f)

                // Shadow for text
                if (shadow.isEnabled) {
                    val shadowPaint = Paint(textPaint)
                    if (FillRenderer.configureShadowPaint(shadowPaint, shadow)) {
                        canvas.drawText(textData.text, x + shadow.offsetX, textY + shadow.offsetY, shadowPaint)
                    }
                }

                // Fill for text
                if (FillRenderer.configureFillPaint(textPaint, fill, bounds)) {
                    canvas.drawText(textData.text, x, textY, textPaint)
                } else {
                    textPaint.color = textData.color.toInt()
                    canvas.drawText(textData.text, x, textY, textPaint)
                }

                // Stroke / Outline for text
                val strokePaint = Paint(textPaint)
                if (FillRenderer.configureStrokePaint(strokePaint, stroke)) {
                    canvas.drawText(textData.text, x, textY, strokePaint)
                }
            }
            LayerType.MEDIA -> {
                val mw = layer.mediaWidth ?: 300f
                val mh = layer.mediaHeight ?: 300f
                val halfW = mw / 2f
                val halfH = mh / 2f
                val dst = RectF(-halfW, -halfH, halfW, halfH)
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
                    canvas.drawRect(dst, strokePaint)
                }
            }
            LayerType.VECTOR -> {
                layer.editablePaths.forEach { data ->
                    val path = buildVectorPath(data)
                    val bounds = RectF()
                    path.computeBounds(bounds, true)

                    if (shadow.isEnabled) {
                        val shadowPaint = Paint()
                        if (FillRenderer.configureShadowPaint(shadowPaint, shadow)) {
                            canvas.save()
                            canvas.translate(shadow.offsetX, shadow.offsetY)
                            canvas.drawPath(path, shadowPaint)
                            canvas.restore()
                        }
                    }

                    val fillPaint = Paint()
                    if (FillRenderer.configureFillPaint(fillPaint, fill, bounds)) {
                        canvas.drawPath(path, fillPaint)
                    } else if (data.hasFill) {
                        fillPaint.color = data.fillColor.toInt()
                        fillPaint.style = Paint.Style.FILL
                        canvas.drawPath(path, fillPaint)
                    }

                    val strokePaint = Paint()
                    if (FillRenderer.configureStrokePaint(strokePaint, stroke)) {
                        canvas.drawPath(path, strokePaint)
                    } else if (data.hasStroke && data.strokeWidth > 0) {
                        val legacyStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = data.strokeColor.toInt()
                            strokeWidth = data.strokeWidth
                            style = Paint.Style.STROKE
                            strokeCap = when (data.strokeCap) {
                                VectorCap.ROUND -> Paint.Cap.ROUND
                                VectorCap.SQUARE -> Paint.Cap.SQUARE
                                else -> Paint.Cap.BUTT
                            }
                            strokeJoin = when (data.strokeJoin) {
                                VectorJoin.ROUND -> Paint.Join.ROUND
                                VectorJoin.BEVEL -> Paint.Join.BEVEL
                                else -> Paint.Join.MITER
                            }
                        }
                        canvas.drawPath(path, legacyStrokePaint)
                    }
                }
            }
            else -> Unit
        }

        canvas.restore()
        return bitmap
    }

    /**
     * Composites an entire document into a single Bitmap, executing the complete
     * render path (backdrop -> layer render -> mask -> blendMode -> adjustment).
     */
    fun compositeDocument(
        document: ProjectDocument,
        width: Int = document.canvasSize.width,
        height: Int = document.canvasSize.height,
        mediaResolver: ((String) -> Bitmap?)? = null
    ): Bitmap {
        val finalBitmap = Bitmap.createBitmap(maxOf(1, width), maxOf(1, height), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)

        // 1. Draw canvas background
        val bgColor = document.editorSettings.backgroundColor
        if (bgColor != 0L) {
            canvas.drawColor(bgColor.toInt())
        }

        val backdropPixels = IntArray(width * height)
        val layerPixels = IntArray(width * height)
        val tempOutputPixels = IntArray(width * height)
        finalBitmap.getPixels(backdropPixels, 0, width, 0, 0, width, height)

        // Render layers bottom to top: activeLayers has index 0 as topmost (top of panel), so draw asReversed()
        val layersToComposite = document.activeLayers.asReversed()

        for (layer in layersToComposite) {
            if (!layer.isVisible) continue

            if (layer.type == LayerType.ADJUSTMENT) {
                // Adjustment layer: affects the composite of everything beneath it
                // If it has a mask, apply mask over the adjustment area
                if (layer.effects.isEmpty() && layer.blendMode == EngineBlendMode.NORMAL && layer.opacity >= 0.999f && layer.effectiveMaskType == MaskType.NONE) {
                    continue // Zero-effect normal adjustment layer draws nothing
                }

                // If adjustment has effects, apply them to current backdrop
                if (layer.blendMode != EngineBlendMode.NORMAL || layer.opacity < 1f || layer.effects.isNotEmpty()) {
                    val adjustedBackdrop = IntArray(backdropPixels.size) { backdropPixels[it] }

                    // Apply any effects
                    for (effect in layer.effects) {
                        val amount = effect.getFloat("amount", 0.5f)
                        for (p in adjustedBackdrop.indices) {
                            val px = adjustedBackdrop[p]
                            val a = (px ushr 24) and 0xFF
                            val r = ((px ushr 16) and 0xFF)
                            val g = ((px ushr 8) and 0xFF)
                            val b = px and 0xFF
                            val newR = (r * (1f + amount * 0.2f)).toInt().coerceIn(0, 255)
                            val newG = (g * (1f + amount * 0.2f)).toInt().coerceIn(0, 255)
                            val newB = (b * (1f + amount * 0.2f)).toInt().coerceIn(0, 255)
                            adjustedBackdrop[p] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
                        }
                    }

                    // Apply adjustment layer mask if present
                    if (layer.effectiveMaskType != MaskType.NONE) {
                        val maskSourceId = layer.effectiveMaskSourceLayerId
                        val maskLayer = if (maskSourceId != null) document.findLayerById(maskSourceId) else null
                        if (maskLayer != null) {
                            val maskBmp = renderLayerContent(maskLayer, width, height, document, mediaResolver, forceVisible = true)
                            val maskPx = IntArray(width * height)
                            maskBmp.getPixels(maskPx, 0, width, 0, 0, width, height)
                            applyMask(adjustedBackdrop, maskPx, layer.effectiveMaskType, layer.effectiveMaskInverted, width, height)
                        }
                    }

                    // Blend adjusted copy onto backdrop
                    blendBuffers(backdropPixels, adjustedBackdrop, backdropPixels, layer.blendMode, layer.opacity, width, height)
                }
                continue
            }

            // Standard layer: render content to isolated buffer
            val layerBmp = renderLayerContent(layer, width, height, document, mediaResolver)
            layerBmp.getPixels(layerPixels, 0, width, 0, 0, width, height)

            // Apply non-destructive mask if configured
            if (layer.effectiveMaskType != MaskType.NONE) {
                val maskSourceId = layer.effectiveMaskSourceLayerId
                val maskLayer = if (maskSourceId != null) document.findLayerById(maskSourceId) else null
                if (maskLayer != null) {
                    val maskBmp = renderLayerContent(maskLayer, width, height, document, mediaResolver, forceVisible = true)
                    val maskPx = IntArray(width * height)
                    maskBmp.getPixels(maskPx, 0, width, 0, 0, width, height)
                    applyMask(layerPixels, maskPx, layer.effectiveMaskType, layer.effectiveMaskInverted, width, height)
                }
            }

            // Blend layer over backdrop using layer.blendMode and layer.opacity
            blendBuffers(backdropPixels, layerPixels, tempOutputPixels, layer.blendMode, layer.opacity, width, height)
            System.arraycopy(tempOutputPixels, 0, backdropPixels, 0, backdropPixels.size)
        }

        finalBitmap.setPixels(backdropPixels, 0, width, 0, 0, width, height)
        return finalBitmap
    }

    private fun buildVectorPath(data: VectorPathData): Path {
        val path = Path()
        val points = data.points
        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            val segments = if (data.isClosed) points.size else points.size - 1
            for (i in 0 until segments) {
                val p0 = points[i]
                val p1 = points[(i + 1) % points.size]
                if (p0.hasOutHandle || p1.hasInHandle) {
                    path.cubicTo(p0.outHandleX, p0.outHandleY, p1.inHandleX, p1.inHandleY, p1.x, p1.y)
                } else {
                    path.lineTo(p1.x, p1.y)
                }
            }
            if (data.isClosed) path.close()
        }
        return path
    }
}
