package com.motionssalt.saltygraphics.history

import kotlin.math.max
import kotlin.math.min

/** Coordinates are dp, independent of device density. The same map drives hit/layout tests. */
data class HistoryViewport(val scale: Float = 1f, val panX: Float = 0f, val panY: Float = 0f) {
    fun pan(dx: Float, dy: Float) = copy(panX = panX + dx, panY = panY + dy)
    fun zoomAt(factor: Float, x: Float, y: Float): HistoryViewport {
        if (!factor.isFinite() || factor <= 0f) return this
        val next = (scale * factor).coerceIn(.25f, 2.5f)
        val ratio = next / scale
        return HistoryViewport(next, x - (x - panX) * ratio, y - (y - panY) * ratio)
    }
    fun centered(x: Float, y: Float, width: Float, height: Float) =
        copy(panX = width / 2f - x * scale, panY = height / 2f - y * scale)
    fun worldX(screenX: Float) = (screenX - panX) / scale
    fun worldY(screenY: Float) = (screenY - panY) / scale
    fun screenX(worldX: Float) = worldX * scale + panX
    fun screenY(worldY: Float) = worldY * scale + panY
}

object HistoryGeometry {
    const val NODE_WIDTH = 200f
    const val NODE_HEIGHT = 48f
    const val LANE_STEP = 248f
    const val ROW_STEP = 68f
    fun x(node: GraphNodeLayout) = node.lane * LANE_STEP + 24f
    fun y(node: GraphNodeLayout) = node.yIndex * ROW_STEP + 24f
    fun visible(node: GraphNodeLayout, viewport: HistoryViewport, width: Float, height: Float): Boolean {
        val x = viewport.screenX(x(node)); val y = viewport.screenY(y(node))
        return x + NODE_WIDTH * viewport.scale >= 0 && x <= width &&
            y + NODE_HEIGHT * viewport.scale >= 0 && y <= height
    }
    fun edgeVisible(edge: GraphEdgeLayout, viewport: HistoryViewport, width: Float, height: Float): Boolean {
        val x1=viewport.screenX(edge.fromLane*LANE_STEP+24f+NODE_WIDTH/2)
        val x2=viewport.screenX(edge.toLane*LANE_STEP+24f+NODE_WIDTH/2)
        val y1=viewport.screenY(edge.fromYIndex*ROW_STEP+24f+NODE_HEIGHT)
        val y2=viewport.screenY(edge.toYIndex*ROW_STEP+24f)
        return max(x1,x2)>=0 && min(x1,x2)<=width && max(y1,y2)>=0 && min(y1,y2)<=height
    }
}
