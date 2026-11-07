package com.motionssalt.saltygraphics.engine.render

import android.graphics.*
import com.motionssalt.saltygraphics.engine.model.*
import kotlin.math.*

/**
 * CPU Reference and Android Canvas renderer for Universal Fill (Solid, Gradient, Media),
 * Media Fill modes (Fill, Fit, Stretch), and Unified Stroke & Shadow.
 */
object FillRenderer {

    /**
     * Creates an Android Shader corresponding to GradientData over a target rectangle.
     */
    fun createGradientShader(gradient: GradientData, bounds: RectF): Shader {
        val w = bounds.width().coerceAtLeast(1f)
        val h = bounds.height().coerceAtLeast(1f)

        return when (gradient.type) {
            GradientType.LINEAR -> {
                val rad = Math.toRadians(gradient.angleDegrees.toDouble())
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()
                val cx = bounds.centerX()
                val cy = bounds.centerY()
                val halfDiag = sqrt(w * w + h * h) / 2f
                val x0 = cx - cosA * halfDiag
                val y0 = cy - sinA * halfDiag
                val x1 = cx + cosA * halfDiag
                val y1 = cy + sinA * halfDiag

                val sorted = gradient.sortedStops()
                val colors = IntArray(sorted.size) { sorted[it].effectiveArgb.toInt() }
                val positions = FloatArray(sorted.size) { sorted[it].position.coerceIn(0f, 1f) }

                LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP)
            }
            GradientType.RADIAL -> {
                val cx = bounds.left + gradient.centerX.coerceIn(0f, 1f) * w
                val cy = bounds.top + gradient.centerY.coerceIn(0f, 1f) * h
                val maxDim = max(w, h)
                val radius = (gradient.radius.coerceAtLeast(0.01f) * maxDim)

                val sorted = gradient.sortedStops()
                val colors = IntArray(sorted.size) { sorted[it].effectiveArgb.toInt() }
                val positions = FloatArray(sorted.size) { sorted[it].position.coerceIn(0f, 1f) }

                RadialGradient(cx, cy, radius, colors, positions, Shader.TileMode.CLAMP)
            }
            GradientType.FREEFORM -> {
                // Approximate freeform mesh by rendering a smooth interpolated bitmap shader
                createFreeformShader(gradient, bounds)
            }
        }
    }

    /**
     * Renders a smooth multi-point freeform gradient into a bitmap and wraps in BitmapShader.
     */
    fun createFreeformShader(gradient: GradientData, bounds: RectF): Shader {
        val sampleW = 64
        val sampleH = 64
        val bmp = Bitmap.createBitmap(sampleW, sampleH, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(sampleW * sampleH)

        val points = gradient.freeformPoints
        if (points.isEmpty()) {
            pixels.fill(0xFF00E5FF.toInt())
        } else {
            for (y in 0 until sampleH) {
                val ny = y.toFloat() / (sampleH - 1).coerceAtLeast(1)
                for (x in 0 until sampleW) {
                    val nx = x.toFloat() / (sampleW - 1).coerceAtLeast(1)

                    var totalWeight = 0f
                    var accA = 0f
                    var accR = 0f
                    var accG = 0f
                    var accB = 0f

                    for (pt in points) {
                        val dx = nx - pt.x
                        val dy = ny - pt.y
                        val dist = sqrt(dx * dx + dy * dy)
                        val r = pt.radius.coerceAtLeast(0.01f)
                        // Inverse quadratic falloff
                        val weight = 1f / (1f + (dist / r).pow(3))
                        totalWeight += weight

                        val color = pt.effectiveArgb
                        val a = ((color ushr 24) and 0xFF) / 255f
                        val cr = ((color ushr 16) and 0xFF) / 255f
                        val cg = ((color ushr 8) and 0xFF) / 255f
                        val cb = (color and 0xFF) / 255f

                        accA += a * weight
                        accR += cr * weight
                        accG += cg * weight
                        accB += cb * weight
                    }

                    val finalA = (accA / totalWeight * 255f).roundToInt().coerceIn(0, 255)
                    val finalR = (accR / totalWeight * 255f).roundToInt().coerceIn(0, 255)
                    val finalG = (accG / totalWeight * 255f).roundToInt().coerceIn(0, 255)
                    val finalB = (accB / totalWeight * 255f).roundToInt().coerceIn(0, 255)

                    pixels[y * sampleW + x] = (finalA shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
                }
            }
        }

        bmp.setPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
        val matrix = Matrix()
        matrix.postScale(bounds.width() / sampleW, bounds.height() / sampleH)
        matrix.postTranslate(bounds.left, bounds.top)

        val shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(matrix)
        return shader
    }

    /**
     * Computes target source and destination rectangles for Media fill modes (Fill, Fit, Stretch).
     */
    fun computeMediaPlacement(
        mediaWidth: Float,
        mediaHeight: Float,
        layerBounds: RectF,
        mode: MediaScaleMode,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ): Pair<RectF, RectF> {
        val srcRect = RectF(0f, 0f, mediaWidth.coerceAtLeast(1f), mediaHeight.coerceAtLeast(1f))
        val dstRect = RectF(layerBounds)

        when (mode) {
            MediaScaleMode.STRETCH -> {
                return Pair(srcRect, dstRect)
            }
            MediaScaleMode.FIT -> {
                // Whole media fits inside layer bounds, letterboxing / keeping transparent margins
                val scale = min(layerBounds.width() / mediaWidth, layerBounds.height() / mediaHeight)
                val fitW = mediaWidth * scale
                val fitH = mediaHeight * scale
                val cx = layerBounds.centerX() + offsetX * (layerBounds.width() - fitW) / 2f
                val cy = layerBounds.centerY() + offsetY * (layerBounds.height() - fitH) / 2f
                val fitDst = RectF(cx - fitW / 2f, cy - fitH / 2f, cx + fitW / 2f, cy + fitH / 2f)
                return Pair(srcRect, fitDst)
            }
            MediaScaleMode.FILL -> {
                // Cover the whole layer area and crop overflow
                val scale = max(layerBounds.width() / mediaWidth, layerBounds.height() / mediaHeight)
                val coverW = mediaWidth * scale
                val coverH = mediaHeight * scale
                val cx = layerBounds.centerX() + offsetX * (layerBounds.width() - coverW) / 2f
                val cy = layerBounds.centerY() + offsetY * (layerBounds.height() - coverH) / 2f
                val fillDst = RectF(cx - coverW / 2f, cy - coverH / 2f, cx + coverW / 2f, cy + coverH / 2f)
                return Pair(srcRect, fillDst)
            }
        }
    }

    /**
     * Prepares a Paint for drawing FillData across any layer geometry.
     */
    fun configureFillPaint(paint: Paint, fill: FillData, bounds: RectF): Boolean {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL

        return when (fill.type) {
            FillType.SOLID -> {
                if (fill.solidOpacity <= 0f) return false
                paint.color = fill.effectiveSolidArgb.toInt()
                true
            }
            FillType.GRADIENT -> {
                paint.shader = createGradientShader(fill.gradient, bounds)
                true
            }
            FillType.MEDIA -> {
                // Media fill is drawn via drawBitmap clipped to shape path
                false
            }
        }
    }

    /**
     * Prepares a Paint for drawing StrokeData across any layer geometry.
     */
    fun configureStrokePaint(paint: Paint, stroke: StrokeData): Boolean {
        if (!stroke.isEnabled || stroke.width <= 0f || stroke.opacity <= 0f) return false
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = stroke.effectiveArgb.toInt()
        paint.strokeWidth = stroke.width

        paint.strokeCap = when (stroke.cap) {
            VectorCap.ROUND -> Paint.Cap.ROUND
            VectorCap.SQUARE -> Paint.Cap.SQUARE
            VectorCap.BUTT -> Paint.Cap.BUTT
        }

        paint.strokeJoin = when (stroke.join) {
            VectorJoin.ROUND -> Paint.Join.ROUND
            VectorJoin.BEVEL -> Paint.Join.BEVEL
            VectorJoin.MITER -> Paint.Join.MITER
        }

        if (stroke.isDashed && stroke.dashLength > 0f) {
            paint.pathEffect = DashPathEffect(floatArrayOf(stroke.dashLength, stroke.dashGap), 0f)
        } else {
            paint.pathEffect = null
        }
        return true
    }

    /**
     * Prepares a Paint for rendering a non-destructive drop shadow.
     */
    fun configureShadowPaint(paint: Paint, shadow: ShadowData): Boolean {
        if (!shadow.isEnabled || shadow.opacity <= 0f) return false
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = shadow.effectiveArgb.toInt()
        if (shadow.blurRadius > 0f) {
            paint.maskFilter = BlurMaskFilter(shadow.blurRadius, BlurMaskFilter.Blur.NORMAL)
        } else {
            paint.maskFilter = null
        }
        return true
    }
}
