package com.motionssalt.saltygraphics.uikit.overlays

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens
import kotlin.math.sqrt

enum class GradientDragTarget {
    NONE,
    START_HANDLE,
    END_HANDLE,
    CENTER_HANDLE,
    LINE_BODY
}

/**
 * On-canvas gradient overlay with independent handles.
 * Per V0 & V7:
 * - Dragging start handle moves ONLY the start.
 * - Dragging end handle moves ONLY the end.
 * - Dragging center handle moves ONLY the center.
 * - Dragging the gradient line moves the whole gradient.
 * - Each drag action commits one clearly labelled history action.
 */
@Composable
fun SaltyGradientOverlay(
    startOffset: Offset,
    endOffset: Offset,
    centerOffset: Offset?,
    onStartChange: (Offset) -> Unit,
    onEndChange: (Offset) -> Unit,
    onCenterChange: ((Offset) -> Unit)? = null,
    onTranslateBoth: (dx: Float, dy: Float) -> Unit,
    onDragCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    isRadialOrAngular: Boolean = false
) {
    val colors = SaltyTokens.colors
    var activeTarget by remember { mutableStateOf(GradientDragTarget.NONE) }

    fun distance(p1: Offset, p2: Offset): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    // Distance from point P to line segment AB
    fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
        val l2 = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
        if (l2 == 0f) return distance(p, a)
        val t = (((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2).coerceIn(0f, 1f)
        val projection = Offset(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
        return distance(p, projection)
    }

    val hitRadiusPx = 28.dp.value * 2f // Generous touch target

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(startOffset, endOffset, centerOffset) {
                detectDragGestures(
                    onDragStart = { pos ->
                        // Hit test start, end, center, then line
                        activeTarget = when {
                            distance(pos, startOffset) <= hitRadiusPx -> GradientDragTarget.START_HANDLE
                            distance(pos, endOffset) <= hitRadiusPx -> GradientDragTarget.END_HANDLE
                            centerOffset != null && distance(pos, centerOffset) <= hitRadiusPx -> GradientDragTarget.CENTER_HANDLE
                            distanceToSegment(pos, startOffset, endOffset) <= hitRadiusPx -> GradientDragTarget.LINE_BODY
                            else -> GradientDragTarget.NONE
                        }
                    },
                    onDragEnd = {
                        val label = when (activeTarget) {
                            GradientDragTarget.START_HANDLE -> "Adjust Gradient Start"
                            GradientDragTarget.END_HANDLE -> "Adjust Gradient End"
                            GradientDragTarget.CENTER_HANDLE -> "Move Gradient Center"
                            GradientDragTarget.LINE_BODY -> "Move Gradient"
                            GradientDragTarget.NONE -> null
                        }
                        activeTarget = GradientDragTarget.NONE
                        if (label != null) onDragCommit(label)
                    },
                    onDragCancel = {
                        activeTarget = GradientDragTarget.NONE
                    }
                ) { change, dragAmount ->
                    if (activeTarget == GradientDragTarget.NONE) return@detectDragGestures
                    change.consume()

                    when (activeTarget) {
                        GradientDragTarget.START_HANDLE -> {
                            // Independent: moves ONLY start
                            onStartChange(startOffset + dragAmount)
                        }
                        GradientDragTarget.END_HANDLE -> {
                            // Independent: moves ONLY end
                            onEndChange(endOffset + dragAmount)
                        }
                        GradientDragTarget.CENTER_HANDLE -> {
                            // Independent: moves ONLY center
                            if (centerOffset != null && onCenterChange != null) {
                                onCenterChange(centerOffset + dragAmount)
                            }
                        }
                        GradientDragTarget.LINE_BODY -> {
                            // Moves whole gradient
                            onTranslateBoth(dragAmount.x, dragAmount.y)
                        }
                        GradientDragTarget.NONE -> {}
                    }
                }
            }
    ) {
        // Draw connection line
        drawLine(
            color = colors.primary.copy(alpha = 0.8f),
            start = startOffset,
            end = endOffset,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Draw center handle if radial/angular/diamond
        centerOffset?.let { c ->
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = c
            )
            drawCircle(
                color = colors.secondary,
                radius = 5.dp.toPx(),
                center = c
            )
        }

        // Draw start handle
        drawCircle(
            color = Color.White,
            radius = 9.dp.toPx(),
            center = startOffset
        )
        drawCircle(
            color = colors.primary,
            radius = 6.5.dp.toPx(),
            center = startOffset
        )

        // Draw end handle
        drawCircle(
            color = Color.White,
            radius = 9.dp.toPx(),
            center = endOffset
        )
        drawCircle(
            color = colors.primary,
            radius = 6.5.dp.toPx(),
            center = endOffset
        )
    }
}
