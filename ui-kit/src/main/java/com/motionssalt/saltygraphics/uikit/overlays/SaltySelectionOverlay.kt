package com.motionssalt.saltygraphics.uikit.overlays

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class SelectionHandleType {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    EDGE_TOP,
    EDGE_BOTTOM,
    EDGE_LEFT,
    EDGE_RIGHT,
    ROTATE_STEM,
    BODY
}

/**
 * On-canvas Selection Bounding Box overlay.
 * Follows V20: Scale bounding-box transforms around the object's center!
 * Supports 4 corner handles, 4 edge midpoint handles, top rotation stem with knob,
 * and 45-degree angle snapping.
 */
@Composable
fun SaltySelectionOverlay(
    bounds: Rect,
    rotationDeg: Float,
    onScaleAroundCenter: (scaleXDelta: Float, scaleYDelta: Float) -> Unit,
    onRotate: (newAngleDeg: Float) -> Unit,
    onTranslate: (dx: Float, dy: Float) -> Unit,
    onTransformCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false
) {
    val colors = SaltyTokens.colors
    var activeHandle by remember { mutableStateOf(SelectionHandleType.NONE) }

    val rad = Math.toRadians(rotationDeg.toDouble()).toFloat()
    val cosR = cos(rad)
    val sinR = sin(rad)

    val center = bounds.center

    // Helper to rotate local point around center to canvas coordinates
    fun toWorld(local: Offset): Offset {
        val dx = local.x - center.x
        val dy = local.y - center.y
        val rx = dx * cosR - dy * sinR
        val ry = dx * sinR + dy * cosR
        return Offset(center.x + rx, center.y + ry)
    }

    val tl = toWorld(Offset(bounds.left, bounds.top))
    val tr = toWorld(Offset(bounds.right, bounds.top))
    val bl = toWorld(Offset(bounds.left, bounds.bottom))
    val br = toWorld(Offset(bounds.right, bounds.bottom))

    val edgeTop = toWorld(Offset(center.x, bounds.top))
    val edgeBottom = toWorld(Offset(center.x, bounds.bottom))
    val edgeLeft = toWorld(Offset(bounds.left, center.y))
    val edgeRight = toWorld(Offset(bounds.right, center.y))

    val stemKnob = toWorld(Offset(center.x, bounds.top - 28.dp.value))

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(bounds, rotationDeg, isLocked) {
                if (isLocked) return@pointerInput

                detectDragGestures(
                    onDragStart = { pos ->
                        // Hit test handles
                        val hitDist = 24.dp.toPx()
                        fun hit(target: Offset) = (pos - target).getDistance() <= hitDist

                        activeHandle = when {
                            hit(stemKnob) -> SelectionHandleType.ROTATE_STEM
                            hit(tl) -> SelectionHandleType.TOP_LEFT
                            hit(tr) -> SelectionHandleType.TOP_RIGHT
                            hit(bl) -> SelectionHandleType.BOTTOM_LEFT
                            hit(br) -> SelectionHandleType.BOTTOM_RIGHT
                            hit(edgeTop) -> SelectionHandleType.EDGE_TOP
                            hit(edgeBottom) -> SelectionHandleType.EDGE_BOTTOM
                            hit(edgeLeft) -> SelectionHandleType.EDGE_LEFT
                            hit(edgeRight) -> SelectionHandleType.EDGE_RIGHT
                            bounds.contains(pos) -> SelectionHandleType.BODY
                            else -> SelectionHandleType.NONE
                        }
                    },
                    onDragEnd = {
                        val label = when (activeHandle) {
                            SelectionHandleType.ROTATE_STEM -> "Rotate Layer"
                            SelectionHandleType.BODY -> "Move Layer"
                            SelectionHandleType.NONE -> null
                            else -> "Scale Layer"
                        }
                        activeHandle = SelectionHandleType.NONE
                        if (label != null) onTransformCommit(label)
                    },
                    onDragCancel = {
                        activeHandle = SelectionHandleType.NONE
                    }
                ) { change, dragAmount ->
                    if (activeHandle == SelectionHandleType.NONE) return@detectDragGestures
                    change.consume()

                    when (activeHandle) {
                        SelectionHandleType.ROTATE_STEM -> {
                            val curAngle = Math.toDegrees(atan2((change.position.y - center.y).toDouble(), (change.position.x - center.x).toDouble())).toFloat()
                            val normalized = (curAngle + 90f + 360f) % 360f
                            // Snap to 45 deg within 3 deg
                            val snapped = (Math.round(normalized / 45f) * 45f)
                            val finalAngle = if (kotlin.math.abs(normalized - snapped) < 3.5f) snapped else normalized
                            onRotate(finalAngle)
                        }
                        SelectionHandleType.BODY -> {
                            onTranslate(dragAmount.x, dragAmount.y)
                        }
                        SelectionHandleType.TOP_LEFT, SelectionHandleType.BOTTOM_RIGHT -> {
                            // Uniform scale around center (V20)
                            val delta = (dragAmount.x + dragAmount.y) / 200f
                            onScaleAroundCenter(delta, delta)
                        }
                        SelectionHandleType.TOP_RIGHT, SelectionHandleType.BOTTOM_LEFT -> {
                            val delta = (-dragAmount.x + dragAmount.y) / 200f
                            onScaleAroundCenter(delta, delta)
                        }
                        SelectionHandleType.EDGE_LEFT, SelectionHandleType.EDGE_RIGHT -> {
                            // Scale X around center
                            val deltaX = dragAmount.x / 150f
                            onScaleAroundCenter(deltaX, 0f)
                        }
                        SelectionHandleType.EDGE_TOP, SelectionHandleType.EDGE_BOTTOM -> {
                            // Scale Y around center
                            val deltaY = dragAmount.y / 150f
                            onScaleAroundCenter(0f, deltaY)
                        }
                        SelectionHandleType.NONE -> {}
                    }
                }
            }
    ) {
        val strokeColor = if (isLocked) colors.outline else colors.selectionOutline
        val handleColor = colors.selectionHandle

        // Draw rotated bounding polygon
        val path = Path().apply {
            moveTo(tl.x, tl.y)
            lineTo(tr.x, tr.y)
            lineTo(br.x, br.y)
            lineTo(bl.x, bl.y)
            close()
        }
        drawPath(path, strokeColor, style = Stroke(width = 1.5.dp.toPx()))

        if (!isLocked) {
            // Draw rotation stem and knob
            drawLine(
                color = strokeColor,
                start = edgeTop,
                end = stemKnob,
                strokeWidth = 1.5.dp.toPx()
            )
            drawCircle(color = strokeColor, radius = 6.dp.toPx(), center = stemKnob)
            drawCircle(color = handleColor, radius = 4.dp.toPx(), center = stemKnob)

            // Draw corner handles
            listOf(tl, tr, bl, br).forEach { pt ->
                drawRect(
                    color = strokeColor,
                    topLeft = pt - Offset(5.dp.toPx(), 5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 10.dp.toPx())
                )
                drawRect(
                    color = handleColor,
                    topLeft = pt - Offset(3.5.dp.toPx(), 3.5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(7.dp.toPx(), 7.dp.toPx())
                )
            }

            // Draw edge midpoint handles
            listOf(edgeTop, edgeBottom, edgeLeft, edgeRight).forEach { pt ->
                drawCircle(color = strokeColor, radius = 4.5.dp.toPx(), center = pt)
                drawCircle(color = handleColor, radius = 3.dp.toPx(), center = pt)
            }
        }
    }
}
