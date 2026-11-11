package com.motionssalt.saltygraphics.uikit.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

/**
 * Precision 2D trackpad for smooth translation control.
 * Integrated with sensitivity multiplier and snapping engine (V15).
 */
@Composable
fun SaltyTrackpad(
    onDelta: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
    sensitivity: Float = 1.0f,
    label: String = "Precision Trackpad"
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes

    var touchOffset by remember { mutableStateOf<Offset?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)
    ) {
        if (label.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = SaltyTokens.typography.labelMedium,
                    color = colors.textSecondary
                )
                Text(
                    text = "Speed: ${String.format("%.1fx", sensitivity)}",
                    style = SaltyTokens.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(shapes.md)
                .background(colors.surfaceVariant)
                .border(1.dp, colors.outlineVariant, shapes.md)
                .pointerInput(sensitivity) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            touchOffset = offset
                        },
                        onDragEnd = {
                            touchOffset = null
                            onDragEnd()
                        },
                        onDragCancel = {
                            touchOffset = null
                            onDragEnd()
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        touchOffset = change.position
                        onDelta(dragAmount.x * sensitivity, dragAmount.y * sensitivity)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Fine crosshair reticle
                drawLine(
                    color = colors.outlineVariant,
                    start = Offset(cx, 0f),
                    end = Offset(cx, size.height),
                    strokeWidth = 1f
                )
                drawLine(
                    color = colors.outlineVariant,
                    start = Offset(0f, cy),
                    end = Offset(size.width, cy),
                    strokeWidth = 1f
                )

                // Active touch feedback point
                touchOffset?.let { pt ->
                    drawCircle(
                        color = colors.primary.copy(alpha = 0.4f),
                        radius = 24.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = colors.primary,
                        radius = 8.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}
