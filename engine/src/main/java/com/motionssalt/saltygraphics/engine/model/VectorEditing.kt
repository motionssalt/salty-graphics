package com.motionssalt.saltygraphics.engine.model

import kotlin.math.*

/** Document-space geometry shared by touch editing, numeric controls and SVG import. */
object VectorEditing {
    fun movePoint(p: BezierPoint, x: Float, y: Float): BezierPoint {
        require(x.isFinite() && y.isFinite())
        val dx = x - p.x; val dy = y - p.y
        return p.copy(x = x, y = y, inHandleX = if(p.hasInHandle) p.inHandleX + dx else x, inHandleY = if(p.hasInHandle) p.inHandleY + dy else y,
            outHandleX = if(p.hasOutHandle) p.outHandleX + dx else x, outHandleY = if(p.hasOutHandle) p.outHandleY + dy else y)
    }

    fun toggleSmooth(p: BezierPoint): BezierPoint = if (p.hasInHandle || p.hasOutHandle) {
        BezierPoint(p.x, p.y)
    } else p.copy(inHandleX = p.x - 40f, outHandleX = p.x + 40f)

    fun bend(p: BezierPoint, x: Float, y: Float): BezierPoint = p.copy(
        inHandleX = 2 * p.x - x, inHandleY = 2 * p.y - y, outHandleX = x, outHandleY = y)

    fun pointAt(a: BezierPoint, b: BezierPoint, t: Float): Pair<Float, Float> {
        val u = 1 - t
        return (u*u*u*a.x + 3*u*u*t*a.outHandleX + 3*u*t*t*b.inHandleX + t*t*t*b.x) to
            (u*u*u*a.y + 3*u*u*t*a.outHandleY + 3*u*t*t*b.inHandleY + t*t*t*b.y)
    }

    /** Split a cubic with de Casteljau: adding a point on a curve preserves its geometry. */
    fun split(path: VectorPathData, segment: Int, t: Float): Pair<VectorPathData, Int> {
        require(t in 0f..1f)
        val points = path.points.toMutableList()
        val next = (segment + 1) % points.size
        val a = points[segment]; val b = points[next]
        fun mix(x: Float, y: Float) = x + (y-x)*t
        val ax = mix(a.x, a.outHandleX); val ay = mix(a.y, a.outHandleY)
        val bx = mix(a.outHandleX, b.inHandleX); val by = mix(a.outHandleY, b.inHandleY)
        val cx = mix(b.inHandleX, b.x); val cy = mix(b.inHandleY, b.y)
        val dx = mix(ax,bx); val dy = mix(ay,by)
        val ex = mix(bx,cx); val ey = mix(by,cy)
        val px = mix(dx,ex); val py = mix(dy,ey)
        points[segment] = a.copy(outHandleX = ax, outHandleY = ay)
        points[next] = b.copy(inHandleX = cx, inHandleY = cy)
        val index = segment + 1
        points.add(index, BezierPoint(px, py, dx, dy, ex, ey))
        return path.copy(points = points) to index
    }

    fun addPoint(path: VectorPathData, x: Float, y: Float, tolerance: Float): Pair<VectorPathData, Int> {
        var bestDistance = tolerance * tolerance
        var bestSegment = -1; var bestT = 0f
        val count = if (path.isClosed) path.points.size else path.points.size - 1
        for (i in 0 until count) for (sample in 1 until 100) {
            val t = sample / 100f
            val (px,py) = pointAt(path.points[i],path.points[(i+1)%path.points.size],t)
            val d = (x-px).pow(2) + (y-py).pow(2)
            if (d < bestDistance) { bestDistance = d; bestSegment = i; bestT = t }
        }
        if (bestSegment >= 0) return split(path,bestSegment,bestT)
        return path.copy(points = path.points + BezierPoint(x,y)) to path.points.size
    }

    /** Inverse of the layer transform; supports rotated, mirrored and nonuniformly scaled layers. */
    fun toLocal(x: Float, y: Float, transform: Transform2D): Pair<Float, Float> {
        val angle = Math.toRadians(-transform.rotationDegrees.toDouble())
        val dx = x - transform.translationX; val dy = y - transform.translationY
        fun nonzero(v: Float) = if (abs(v) < .0001f) .0001f else v
        return ((dx*cos(angle)-dy*sin(angle))/nonzero(transform.scaleX)).toFloat() to
            ((dx*sin(angle)+dy*cos(angle))/nonzero(transform.scaleY)).toFloat()
    }
}
