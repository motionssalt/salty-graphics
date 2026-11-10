package com.motionssalt.saltygraphics.engine.render

import android.graphics.*
import android.os.Build
import com.motionssalt.saltygraphics.engine.model.*
import kotlin.math.*

/**
 * CPU Reference and Android Canvas renderer for Universal Fill (Solid, Gradient, Media),
 * Media Fill modes (Fill, Fit, Stretch), and Unified Stroke & Shadow.
 */
object FillRenderer {

    private fun toTileMode(mode: GradientRepeatMode): Shader.TileMode = when (mode) {
        GradientRepeatMode.CLAMP -> Shader.TileMode.CLAMP
        GradientRepeatMode.REPEAT -> Shader.TileMode.REPEAT
        GradientRepeatMode.MIRROR -> Shader.TileMode.MIRROR
    }

    /**
     * Expands stops to take midpoint biasing into account by inserting midpoint colors.
     */
    fun computeExpandedStops(gradient: GradientData): Pair<IntArray, FloatArray> {
        val sorted = gradient.sortedStops()
        if (sorted.isEmpty()) return intArrayOf(0,0) to floatArrayOf(0f,1f)
        if (sorted.size == 1) return intArrayOf(sorted[0].effectiveArgb.toInt(),sorted[0].effectiveArgb.toInt()) to floatArrayOf(0f,1f)
        val colors = mutableListOf<Int>()
        val positions = mutableListOf<Float>()
        for (i in 0 until sorted.lastIndex) {
            val a=sorted[i]; val b=sorted[i+1]
            // Native shaders are piecewise linear. Sample the same midpoint/smooth
            // curve used by CPU references so every gradient type obeys it.
            for (j in 0 until 32) {
                val t=j/32f
                var u=t.toDouble().pow(ln(.5)/ln(a.midpoint.coerceIn(.01f,.99f).toDouble())).toFloat()
                if (gradient.interpolation==GradientInterpolation.SMOOTH) u=u*u*(3f-2f*u)
                positions.add((a.position+(b.position-a.position)*t).coerceIn(0f,1f))
                colors.add(interpolateColors(a.effectiveArgb,b.effectiveArgb,u).toInt())
            }
        }
        positions.add(sorted.last().position.coerceIn(0f,1f));colors.add(sorted.last().effectiveArgb.toInt())
        return colors.toIntArray() to positions.toFloatArray()
    }

    private fun interpolateColors(c0: Long, c1: Long, t: Float): Long {
        val a0 = (c0 ushr 24 and 0xFF) / 255f
        val r0 = (c0 ushr 16 and 0xFF) / 255f
        val g0 = (c0 ushr 8 and 0xFF) / 255f
        val b0 = (c0 and 0xFF) / 255f

        val a1 = (c1 ushr 24 and 0xFF) / 255f
        val r1 = (c1 ushr 16 and 0xFF) / 255f
        val g1 = (c1 ushr 8 and 0xFF) / 255f
        val b1 = (c1 and 0xFF) / 255f

        val a = ((a0 + (a1 - a0) * t) * 255f).roundToInt().coerceIn(0, 255)
        val r = ((r0 + (r1 - r0) * t) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((g0 + (g1 - g0) * t) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((b0 + (b1 - b0) * t) * 255f).roundToInt().coerceIn(0, 255)

        return ((a.toLong() and 0xFF) shl 24) or
            ((r.toLong() and 0xFF) shl 16) or
            ((g.toLong() and 0xFF) shl 8) or
            (b.toLong() and 0xFF)
    }

    /** Scalar reference shared by angular repeat and CPU regression checks. */
    fun sampleGradient(gradient: GradientData, position: Float): Long {
        val x=when(gradient.repeatMode) {
            GradientRepeatMode.CLAMP -> position.coerceIn(0f,1f)
            GradientRepeatMode.REPEAT -> ((position%1f)+1f)%1f
            GradientRepeatMode.MIRROR -> {val t=((position%2f)+2f)%2f;if(t>1f) 2f-t else t}
        }
        val stops=gradient.sortedStops()
        if(stops.isEmpty()) return 0L
        if(x<=stops.first().position) return stops.first().effectiveArgb
        for(i in 0 until stops.lastIndex) {
            val a=stops[i];val b=stops[i+1]
            if(x<=b.position) {
                var t=((x-a.position)/(b.position-a.position).coerceAtLeast(.000001f)).coerceIn(0f,1f)
                t=t.toDouble().pow(ln(.5)/ln(a.midpoint.coerceIn(.01f,.99f).toDouble())).toFloat()
                if(gradient.interpolation==GradientInterpolation.SMOOTH) t=t*t*(3f-2f*t)
                return interpolateColors(a.effectiveArgb,b.effectiveArgb,t)
            }
        }
        return stops.last().effectiveArgb
    }

    /**
     * Creates an Android Shader corresponding to GradientData over a target rectangle.
     */
    fun createGradientShader(gradient: GradientData, bounds: RectF): Shader {
        val w = bounds.width().coerceAtLeast(1f)
        val h = bounds.height().coerceAtLeast(1f)
        val tileMode = toTileMode(gradient.repeatMode)
        val (colors, positions) = computeExpandedStops(gradient)

        return when (gradient.type) {
            GradientType.LINEAR -> {
                val rad = Math.toRadians(gradient.angleDegrees.toDouble())
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()
                val cx = bounds.left + gradient.centerX.coerceIn(0f, 1f) * w
                val cy = bounds.top + gradient.centerY.coerceIn(0f, 1f) * h
                val halfDiag = sqrt(w * w + h * h) / 2f * (gradient.radius.coerceAtLeast(0.01f) / 0.5f)
                val x0 = cx - cosA * halfDiag
                val y0 = cy - sinA * halfDiag
                val x1 = cx + cosA * halfDiag
                val y1 = cy + sinA * halfDiag

                LinearGradient(x0, y0, x1, y1, colors, positions, tileMode)
            }
            GradientType.RADIAL -> {
                val cx = bounds.left + gradient.centerX.coerceIn(0f, 1f) * w
                val cy = bounds.top + gradient.centerY.coerceIn(0f, 1f) * h
                val maxDim = max(w, h)
                val radius = (gradient.radius.coerceAtLeast(0.01f) * maxDim)

                val shader = RadialGradient(cx, cy, radius, colors, positions, tileMode)
                val matrix = Matrix()
                matrix.postScale(1f, 1f / gradient.aspect.coerceAtLeast(0.05f), cx, cy)
                matrix.postRotate(gradient.angleDegrees,cx,cy)
                shader.setLocalMatrix(matrix)
                shader
            }
            GradientType.ANGULAR -> {
                val cx=bounds.left+gradient.centerX.coerceIn(0f,1f)*w
                val cy=bounds.top+gradient.centerY.coerceIn(0f,1f)*h
                val samples=1024
                val period=(2f*gradient.radius).coerceAtLeast(.02f)
                val sampled=IntArray(samples+1){i->sampleGradient(gradient,i.toFloat()/samples/period).toInt()}
                SweepGradient(cx,cy,sampled,null).apply {
                    setLocalMatrix(Matrix().apply {postRotate(gradient.angleDegrees,cx,cy)})
                }
            }
            GradientType.REFLECTED -> {
                val rad=Math.toRadians(gradient.angleDegrees.toDouble())
                val cx=bounds.left+gradient.centerX.coerceIn(0f,1f)*w
                val cy=bounds.top+gradient.centerY.coerceIn(0f,1f)*h
                val length=sqrt(w*w+h*h)*gradient.radius.coerceAtLeast(.01f)
                val dx=cos(rad).toFloat()*length;val dy=sin(rad).toFloat()*length
                val reflectedColors=colors.reversedArray()+colors
                val reflectedPositions=positions.reversedArray().map{(1f-it)*.5f}.toFloatArray()+positions.map{.5f+it*.5f}.toFloatArray()
                LinearGradient(cx-dx,cy-dy,cx+dx,cy+dy,reflectedColors,reflectedPositions,tileMode)
            }
            GradientType.DIAMOND -> {
                createDiamondShader(gradient, bounds)
            }
            GradientType.FREEFORM -> {
                createFreeformShader(gradient, bounds)
            }
        }
    }

    /**
     * Renders a diamond gradient into a bitmap and wraps in BitmapShader.
     */
    fun createDiamondShader(gradient: GradientData, bounds: RectF): Shader {
        val sampleW = 64
        val sampleH = 64
        val bmp = Bitmap.createBitmap(sampleW, sampleH, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(sampleW * sampleH)

        val sorted = gradient.sortedStops()
        val cx = gradient.centerX.coerceIn(0f, 1f)
        val cy = gradient.centerY.coerceIn(0f, 1f)
        val r = gradient.radius.coerceAtLeast(0.01f)
        val aspect = gradient.aspect.coerceAtLeast(0.05f)

        for (y in 0 until sampleH) {
            val ny = y.toFloat() / (sampleH - 1).coerceAtLeast(1)
            for (x in 0 until sampleW) {
                val nx = x.toFloat() / (sampleW - 1).coerceAtLeast(1)
                val angle = Math.toRadians(gradient.angleDegrees.toDouble())
                val dx = abs(((nx-cx)*cos(angle)+(ny-cy)*sin(angle)).toFloat())
                val dy = abs((-(nx-cx)*sin(angle)+(ny-cy)*cos(angle)).toFloat()) / aspect
                var dist = (dx + dy) / r

                dist = when (gradient.repeatMode) {
                    GradientRepeatMode.CLAMP -> dist.coerceIn(0f, 1f)
                    GradientRepeatMode.REPEAT -> (dist % 1f + 1f) % 1f
                    GradientRepeatMode.MIRROR -> {
                        val m = (dist % 2f + 2f) % 2f
                        if (m > 1f) 2f - m else m
                    }
                }

                // Sample color from sorted stops
                var c = sorted.firstOrNull()?.effectiveArgb ?: 0xFF00E5FFL
                if (sorted.size > 1) {
                    if (dist <= sorted.first().position) {
                        c = sorted.first().effectiveArgb
                    } else if (dist >= sorted.last().position) {
                        c = sorted.last().effectiveArgb
                    } else {
                        for (i in 0 until sorted.size - 1) {
                            val s0 = sorted[i]
                            val s1 = sorted[i + 1]
                            if (dist in s0.position..s1.position) {
                                val tSpan = (s1.position - s0.position).coerceAtLeast(0.0001f)
                                var t = (dist - s0.position) / tSpan
                                val mid = s0.midpoint.coerceIn(0.01f, 0.99f)
                                if (abs(mid - 0.5f) > 0.01f) {
                                    val exponent = ln(0.5) / ln(mid.toDouble())
                                    t = t.toDouble().pow(exponent).toFloat()
                                }
                                if (gradient.interpolation == GradientInterpolation.SMOOTH) t=t*t*(3f-2f*t)
                                c = interpolateColors(s0.effectiveArgb, s1.effectiveArgb, t)
                                break
                            }
                        }
                    }
                }
                pixels[y * sampleW + x] = c.toInt()
            }
        }

        bmp.setPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
        val matrix = Matrix()
        matrix.postScale(bounds.width() / sampleW, bounds.height() / sampleH)
        matrix.postTranslate(bounds.left, bounds.top)

        val tileMode = toTileMode(gradient.repeatMode)
        val shader = BitmapShader(bmp, tileMode, tileMode)
        shader.setLocalMatrix(matrix)
        return shader
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
     * Computes target source and destination rectangles for Media fill modes (Fill, Fit, Stretch),
     * taking scale, offset, and rotation into account.
     */
    fun computeMediaPlacement(
        mediaWidth: Float,
        mediaHeight: Float,
        layerBounds: RectF,
        mode: MediaScaleMode,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        scaleMultiplier: Float = 1.0f
    ): Pair<RectF, RectF> {
        val srcRect = RectF(0f, 0f, mediaWidth.coerceAtLeast(1f), mediaHeight.coerceAtLeast(1f))
        val dstRect = RectF(layerBounds)
        val userScale = scaleMultiplier.coerceIn(0.05f, 20f)

        when (mode) {
            MediaScaleMode.STRETCH -> {
                val w = dstRect.width() * userScale
                val h = dstRect.height() * userScale
                val cx = layerBounds.centerX() + offsetX * layerBounds.width() / 2f
                val cy = layerBounds.centerY() + offsetY * layerBounds.height() / 2f
                val scaledDst = RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
                return Pair(srcRect, scaledDst)
            }
            MediaScaleMode.FIT -> {
                val scale = min(layerBounds.width() / mediaWidth, layerBounds.height() / mediaHeight) * userScale
                val fitW = mediaWidth * scale
                val fitH = mediaHeight * scale
                val cx = layerBounds.centerX() + offsetX * layerBounds.width() / 2f
                val cy = layerBounds.centerY() + offsetY * layerBounds.height() / 2f
                val fitDst = RectF(cx - fitW / 2f, cy - fitH / 2f, cx + fitW / 2f, cy + fitH / 2f)
                return Pair(srcRect, fitDst)
            }
            MediaScaleMode.FILL -> {
                val scale = max(layerBounds.width() / mediaWidth, layerBounds.height() / mediaHeight) * userScale
                val coverW = mediaWidth * scale
                val coverH = mediaHeight * scale
                val cx = layerBounds.centerX() + offsetX * layerBounds.width() / 2f
                val cy = layerBounds.centerY() + offsetY * layerBounds.height() / 2f
                val fillDst = RectF(cx - coverW / 2f, cy - coverH / 2f, cx + coverW / 2f, cy + coverH / 2f)
                return Pair(srcRect, fillDst)
            }
        }
    }

    /** Shared placement for text/vector shaders and clipped shape/media fills. */
    fun createMediaShader(bitmap: Bitmap, fill: FillData, bounds: RectF): Shader {
        // Materialize the clipped fill, including transparent FIT margins, so API 26
        // never stretches the bitmap's edge pixels into the letterbox area.
        val w=ceil(bounds.width().coerceAtLeast(1f)).toInt().coerceAtMost(4096)
        val h=ceil(bounds.height().coerceAtLeast(1f)).toInt().coerceAtMost(4096)
        val tile=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        val c=Canvas(tile)
        c.scale(w/bounds.width().coerceAtLeast(1f),h/bounds.height().coerceAtLeast(1f))
        c.translate(-bounds.left,-bounds.top)
        drawMediaFill(c,bitmap,fill,bounds)
        val matrix = Matrix().apply {
            setScale(bounds.width()/w,bounds.height()/h)
            postTranslate(bounds.left,bounds.top)
        }
        val shader = BitmapShader(tile, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(matrix)
        return shader
    }

    fun drawMediaFill(canvas: Canvas, bitmap: Bitmap, fill: FillData, bounds: RectF, path: Path? = null) {
        val (src,dst)=computeMediaPlacement(bitmap.width.toFloat(),bitmap.height.toFloat(),bounds,
            fill.mediaScaleMode,fill.mediaOffsetX,fill.mediaOffsetY,fill.mediaScale)
        canvas.save()
        if(path!=null) canvas.clipPath(path) else canvas.clipRect(bounds)
        canvas.rotate(fill.mediaRotation,bounds.centerX(),bounds.centerY())
        canvas.drawBitmap(bitmap,Rect(src.left.toInt(),src.top.toInt(),src.right.toInt(),src.bottom.toInt()),dst,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }

    /**
     * Prepares a Paint for drawing FillData across any layer geometry.
     */
    fun configureFillPaint(paint: Paint, fill: FillData, bounds: RectF, mediaResolver: ((String) -> Bitmap?)? = null): Boolean {
        // Do not reset a text paint: reset discards typeface, size and spacing.
        paint.shader = null
        paint.color = Color.TRANSPARENT
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL

        return when (fill.type) {
            FillType.SOLID -> {
                if (fill.solidOpacity <= 0f) return false
                paint.color = fill.effectiveSolidArgb.toInt()
                true
            }
            FillType.GRADIENT -> {
                paint.alpha = 255
                paint.shader = createGradientShader(fill.gradient, bounds)
                paint.isDither = fill.gradient.dither
                true
            }
            FillType.MEDIA -> {
                val bitmap=fill.mediaAssetPath?.let { mediaResolver?.invoke(it) } ?: return false
                paint.alpha=255
                paint.shader=createMediaShader(bitmap,fill,bounds)
                true
            }
        }
    }

    /**
     * Prepares a Paint for drawing StrokeData across any layer geometry.
     */
    fun configureStrokePaint(paint: Paint, stroke: StrokeData): Boolean {
        if (!stroke.isEnabled || stroke.width <= 0f || stroke.opacity <= 0f) return false
        paint.shader = null
        paint.pathEffect = null
        paint.maskFilter = null
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
        paint.shader = null
        paint.pathEffect = null
        paint.maskFilter = null
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

    /**
     * Renders a path with the specified StrokeData respecting stroke alignment (Center, Inside, Outside).
     */
    fun drawPathWithStroke(canvas: Canvas, path: Path, stroke: StrokeData, strokePaint: Paint) {
        if (!stroke.isEnabled || stroke.width <= 0f || stroke.opacity <= 0f) return
        when (stroke.alignment) {
            StrokeAlignment.CENTER -> {
                canvas.drawPath(path, strokePaint)
            }
            StrokeAlignment.INSIDE -> {
                canvas.save()
                canvas.clipPath(path)
                val origWidth = strokePaint.strokeWidth
                strokePaint.strokeWidth = origWidth * 2f
                canvas.drawPath(path, strokePaint)
                strokePaint.strokeWidth = origWidth
                canvas.restore()
            }
            StrokeAlignment.OUTSIDE -> {
                canvas.save()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    canvas.clipOutPath(path)
                } else {
                    @Suppress("DEPRECATION")
                    canvas.clipPath(path, Region.Op.DIFFERENCE)
                }
                val origWidth = strokePaint.strokeWidth
                strokePaint.strokeWidth = origWidth * 2f
                canvas.drawPath(path, strokePaint)
                strokePaint.strokeWidth = origWidth
                canvas.restore()
            }
        }
    }
}
