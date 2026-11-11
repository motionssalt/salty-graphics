package com.motionssalt.saltygraphics.engine.model

import kotlin.math.*

/** Engine-only geometry. All distances are document units; UI touch sizes never enter here. */
data class SelectionPoint(val x: Float, val y: Float) {
    operator fun plus(p: SelectionPoint) = SelectionPoint(x + p.x, y + p.y)
    operator fun minus(p: SelectionPoint) = SelectionPoint(x - p.x, y - p.y)
    fun rotated(degrees: Float): SelectionPoint {
        val r = Math.toRadians(degrees.toDouble())
        return SelectionPoint((x*cos(r)-y*sin(r)).toFloat(), (x*sin(r)+y*cos(r)).toFloat())
    }
    val finite get() = x.isFinite() && y.isFinite()
}

data class SelectionBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = max(right-left, .001f)
    val height get() = max(bottom-top, .001f)
    val center get() = SelectionPoint((left+right)/2f, (top+bottom)/2f)
    val corners get() = listOf(SelectionPoint(left,top),SelectionPoint(right,top),SelectionPoint(right,bottom),SelectionPoint(left,bottom))
    companion object {
        fun enclosing(points: List<SelectionPoint>): SelectionBounds {
            val finite = points.filter { it.finite }
            if (finite.isEmpty()) return SelectionBounds(-.5f,-.5f,.5f,.5f)
            return SelectionBounds(finite.minOf{it.x},finite.minOf{it.y},finite.maxOf{it.x},finite.maxOf{it.y})
        }
    }
}

enum class SelectionHandle(val x: Int, val y: Int) {
    NONE(0,0), BODY(0,0), ROTATE(0,0),
    CORNER_TOP_LEFT(-1,-1), CORNER_TOP_RIGHT(1,-1), CORNER_BOTTOM_RIGHT(1,1), CORNER_BOTTOM_LEFT(-1,1),
    EDGE_TOP(0,-1), EDGE_BOTTOM(0,1), EDGE_LEFT(-1,0), EDGE_RIGHT(1,0)
}

object SelectionGeometry {
    const val MIN_SCALE = .0001f
    fun valid(t: Transform2D): Boolean = t.translationX.isFinite() && t.translationY.isFinite() &&
        t.rotationDegrees.isFinite() && t.scaleX.isFinite() && t.scaleY.isFinite() &&
        t.scaleX >= MIN_SCALE && t.scaleY >= MIN_SCALE && t.moveSensitivity.isFinite()

    fun world(p: SelectionPoint, t: Transform2D): SelectionPoint =
        SelectionPoint(p.x*t.scaleX,p.y*t.scaleY).rotated(t.rotationDegrees) + SelectionPoint(t.translationX,t.translationY)

    /** A transform command may only alter a live unlocked target's transform, never its layer list. */
    fun apply(document: ProjectDocument, id: String, t: Transform2D): ProjectDocument {
        val layer = document.findLayerById(id) ?: return document
        if (layer.isLocked || layer.type == LayerType.ADJUSTMENT || !valid(t) || layer.transform == t) return document
        return document.updateLayer(id) { it.copy(transform = t) }
    }

    /** Absolute drag from pointer-down; opposite edge/corner remains fixed, including rotated layers. */
    fun drag(start: Transform2D, bounds: SelectionBounds, handle: SelectionHandle,
             down: SelectionPoint, pointer: SelectionPoint): Transform2D {
        if (!valid(start) || !down.finite || !pointer.finite || down == pointer) return start
        val delta = (pointer-down).rotated(-start.rotationDegrees)
        val result = when(handle) {
            SelectionHandle.NONE -> start
            SelectionHandle.BODY -> start.copy(translationX=start.translationX+pointer.x-down.x,translationY=start.translationY+pointer.y-down.y)
            SelectionHandle.ROTATE -> {
                val center=world(bounds.center,start)
                val a=atan2((down.y-center.y).toDouble(),(down.x-center.x).toDouble())
                val b=atan2((pointer.y-center.y).toDouble(),(pointer.x-center.x).toDouble())
                var angle=((start.rotationDegrees+Math.toDegrees(b-a))%360+360)%360
                val snap=round(angle/45)*45
                if (abs(angle-snap)<=3) angle=snap%360
                // Rotate about visible center, not an offset vector/text origin.
                val offset=SelectionPoint(bounds.center.x*start.scaleX,bounds.center.y*start.scaleY).rotated(angle.toFloat())
                start.copy(rotationDegrees=angle.toFloat(),translationX=center.x-offset.x,translationY=center.y-offset.y)
            }
            else -> {
                val center = world(bounds.center, start)
                val w = bounds.width.toDouble() * start.scaleX
                val h = bounds.height.toDouble() * start.scaleY
                var sx = start.scaleX.toDouble()
                var sy = start.scaleY.toDouble()
                if (handle.x != 0 && handle.y != 0 && start.isScaleLinked) {
                    val ratio = 1 + 2.0 * (handle.x * w * delta.x + handle.y * h * delta.y) / (w * w + h * h)
                    val safe = max(ratio, max(MIN_SCALE / start.scaleX, MIN_SCALE / start.scaleY).toDouble())
                    sx *= safe
                    sy *= safe
                } else {
                    if (handle.x != 0) sx = max(MIN_SCALE.toDouble(), sx + 2.0 * handle.x * delta.x / bounds.width)
                    if (handle.y != 0) sy = max(MIN_SCALE.toDouble(), sy + 2.0 * handle.y * delta.y / bounds.height)
                }
                val offset = SelectionPoint((bounds.center.x * sx).toFloat(), (bounds.center.y * sy).toFloat()).rotated(start.rotationDegrees)
                start.copy(scaleX = sx.toFloat(), scaleY = sy.toFloat(), translationX = center.x - offset.x, translationY = center.y - offset.y)
            }
        }
        return if(valid(result) && bounds.corners.all { world(it,result).finite }) result else start
    }
}
