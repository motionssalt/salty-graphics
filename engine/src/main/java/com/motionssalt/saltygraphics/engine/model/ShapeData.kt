package com.motionssalt.saltygraphics.engine.model

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Serializable
enum class ShapeType(val displayName: String) {
    RECTANGLE("Rectangle"),
    ROUNDED_RECTANGLE("Rounded Rectangle"),
    CIRCLE("Circle / Ellipse"),
    TRIANGLE("Triangle"),
    POLYGON("Polygon"),
    STAR("Star"),
    LINE("Line"),
    ARROW("Arrow")
}

@Serializable
data class ShapeData(
    val type: ShapeType = ShapeType.ROUNDED_RECTANGLE,
    val width: Float = 300f,
    val height: Float = 300f,
    val cornerRadius: Float = 32f, // Can reach min(width, height) / 2 to form circle/oval
    val sides: Int = 5,           // For polygon
    val starPoints: Int = 5,      // For star
    val starInnerRadiusRatio: Float = 0.5f,
    val fillColor: Long = 0xFF00E5FFL,   // ARGB
    val strokeColor: Long = 0xFFFFFFFFL,
    val strokeWidth: Float = 4f,
    val hasFill: Boolean = true,
    val hasStroke: Boolean = false
) {
    /**
     * Converts shape parameters into a unified VectorPathData for path-following text & vector rasterization.
     */
    fun toVectorPath(): VectorPathData {
        val points = mutableListOf<BezierPoint>()
        val halfW = width / 2f
        val halfH = height / 2f

        when (type) {
            ShapeType.RECTANGLE -> {
                points.add(BezierPoint(-halfW, -halfH))
                points.add(BezierPoint(halfW, -halfH))
                points.add(BezierPoint(halfW, halfH))
                points.add(BezierPoint(-halfW, halfH))
                return VectorPathData(points = points, isClosed = true)
            }
            ShapeType.ROUNDED_RECTANGLE -> {
                val maxR = minOf(halfW, halfH)
                val r = cornerRadius.coerceIn(0f, maxR)
                val k = 0.55228475f * r // Bezier circle approximation factor

                // Top-left corner to top-right
                points.add(BezierPoint(-halfW + r, -halfH, inHandleX = -halfW + r - k, inHandleY = -halfH, outHandleX = -halfW + r, outHandleY = -halfH))
                points.add(BezierPoint(halfW - r, -halfH, inHandleX = halfW - r, inHandleY = -halfH, outHandleX = halfW - r + k, outHandleY = -halfH))
                // Right edge
                points.add(BezierPoint(halfW, -halfH + r, inHandleX = halfW, inHandleY = -halfH + r - k, outHandleX = halfW, outHandleY = -halfH + r))
                points.add(BezierPoint(halfW, halfH - r, inHandleX = halfW, inHandleY = halfH - r, outHandleX = halfW, outHandleY = halfH - r + k))
                // Bottom edge
                points.add(BezierPoint(halfW - r, halfH, inHandleX = halfW - r + k, inHandleY = halfH, outHandleX = halfW - r, outHandleY = halfH))
                points.add(BezierPoint(-halfW + r, halfH, inHandleX = -halfW + r, inHandleY = halfH, outHandleX = -halfW + r - k, outHandleY = halfH))
                // Left edge
                points.add(BezierPoint(-halfW, halfH - r, inHandleX = -halfW, inHandleY = halfH - r + k, outHandleX = -halfW, outHandleY = halfH - r))
                points.add(BezierPoint(-halfW, -halfH + r, inHandleX = -halfW, inHandleY = -halfH + r, outHandleX = -halfW, outHandleY = -halfH + r - k))
                return VectorPathData(points = points, isClosed = true)
            }
            ShapeType.CIRCLE -> {
                val kx = 0.55228475f * halfW
                val ky = 0.55228475f * halfH
                points.add(BezierPoint(0f, -halfH, inHandleX = -kx, inHandleY = -halfH, outHandleX = kx, outHandleY = -halfH))
                points.add(BezierPoint(halfW, 0f, inHandleX = halfW, inHandleY = -ky, outHandleX = halfW, outHandleY = ky))
                points.add(BezierPoint(0f, halfH, inHandleX = kx, inHandleY = halfH, outHandleX = -kx, outHandleY = halfH))
                points.add(BezierPoint(-halfW, 0f, inHandleX = -halfW, inHandleY = ky, outHandleX = -halfW, outHandleY = -ky))
                return VectorPathData(points = points, isClosed = true)
            }
            ShapeType.TRIANGLE -> {
                points.add(BezierPoint(0f, -halfH))
                points.add(BezierPoint(halfW, halfH))
                points.add(BezierPoint(-halfW, halfH))
                return VectorPathData(points = points, isClosed = true)
            }
            ShapeType.POLYGON -> {
                val count = sides.coerceAtLeast(3)
                for (i in 0 until count) {
                    val angle = (i * 2.0 * PI / count) - (PI / 2.0)
                    val px = (halfW * cos(angle)).toFloat()
                    val py = (halfH * sin(angle)).toFloat()
                    points.add(BezierPoint(px, py))
                }
                return VectorPathData(points = points, isClosed = true)
            }
            ShapeType.STAR -> {
                val count = starPoints.coerceAtLeast(3)
                val totalPoints = count * 2
                val innerW = halfW * starInnerRadiusRatio
                val innerH = halfH * starInnerRadiusRatio
                for (i in 0 until totalPoints) {
                    val angle = (i * PI / count) - (PI / 2.0)
                    val isOuter = i % 2 == 0
                    val rx = if (isOuter) halfW else innerW
                    val ry = if (isOuter) halfH else innerH
                    val px = (rx * cos(angle)).toFloat()
                    val py = (ry * sin(angle)).toFloat()
                    points.add(BezierPoint(px, py))
                }
                return VectorPathData(points = points, isClosed = true)
            }
            ShapeType.LINE -> {
                points.add(BezierPoint(-halfW, 0f))
                points.add(BezierPoint(halfW, 0f))
                return VectorPathData(points = points, isClosed = false)
            }
            ShapeType.ARROW -> {
                points.add(BezierPoint(-halfW, 0f))
                points.add(BezierPoint(halfW, 0f))
                points.add(BezierPoint(halfW - 24f, -16f))
                points.add(BezierPoint(halfW, 0f))
                points.add(BezierPoint(halfW - 24f, 16f))
                return VectorPathData(points = points, isClosed = false)
            }
        }
    }
}

@Serializable
data class BezierPoint(
    val x: Float,
    val y: Float,
    val inHandleX: Float = x,
    val inHandleY: Float = y,
    val outHandleX: Float = x,
    val outHandleY: Float = y
) {
    val hasInHandle: Boolean
        get() = inHandleX != x || inHandleY != y

    val hasOutHandle: Boolean
        get() = outHandleX != x || outHandleY != y
}

@Serializable
data class VectorPathData(
    val points: List<BezierPoint> = emptyList(),
    val isClosed: Boolean = false,
    val strokeColor: Long = 0xFFFFFFFF,
    val strokeWidth: Float = 4f,
    val fillColor: Long = 0x00000000,
    val hasFill: Boolean = false,
    val hasStroke: Boolean = true
) {
    /**
     * Approximates path length and samples points along the path for text move-along-path.
     */
    fun samplePath(sampleCount: Int = 100): List<Pair<Float, Float>> {
        if (points.size < 2) return points.map { Pair(it.x, it.y) }
        val result = mutableListOf<Pair<Float, Float>>()
        val segments = if (isClosed) points.size else points.size - 1

        val samplesPerSegment = maxOf(2, sampleCount / maxOf(1, segments))
        for (i in 0 until segments) {
            val p0 = points[i]
            val p1 = points[(i + 1) % points.size]
            for (step in 0..samplesPerSegment) {
                val t = step.toFloat() / samplesPerSegment
                // Cubic Bezier interpolation
                val u = 1f - t
                val x = (u * u * u * p0.x) + (3f * u * u * t * p0.outHandleX) + (3f * u * t * t * p1.inHandleX) + (t * t * t * p1.x)
                val y = (u * u * u * p0.y) + (3f * u * u * t * p0.outHandleY) + (3f * u * t * t * p1.inHandleY) + (t * t * t * p1.y)
                result.add(Pair(x, y))
            }
        }
        return result
    }
}
