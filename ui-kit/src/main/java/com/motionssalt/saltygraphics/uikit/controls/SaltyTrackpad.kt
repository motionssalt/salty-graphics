package com.motionssalt.saltygraphics.uikit.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
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
    // A registered session can replace its host callbacks without changing sensitivity.
    val currentDelta by rememberUpdatedState(onDelta)
    val currentDragEnd by rememberUpdatedState(onDragEnd)

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
                .testTag("trackpad_surface")
                .pointerInput(sensitivity) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            touchOffset = offset
                        },
                        onDragEnd = {
                            touchOffset = null
                            currentDragEnd()
                        },
                        onDragCancel = {
                            touchOffset = null
                            currentDragEnd()
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        touchOffset = change.position
                        currentDelta(dragAmount.x * sensitivity, dragAmount.y * sensitivity)
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

/**
 * Transform trackpad card with clickable X/Y coordinate pills and inline value entry (no popups).
 */
@Composable
fun SaltyPositionTrackpad(
    x: Float,
    y: Float,
    sensitivity: Float,
    onDelta: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    onCommitX: (Float) -> Unit,
    onCommitY: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = -100000f..100000f
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes
    var editingAxis by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)
    ) {
        SaltyTrackpad(
            onDelta = onDelta,
            onDragEnd = onDragEnd,
            sensitivity = sensitivity,
            label = "Drag here for precision translation"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(shapes.sm)
                    .background(colors.surfaceVariant)
                    .clickable { editingAxis = "X" }
                    .padding(horizontal = SaltyTokens.spacing.sm, vertical = SaltyTokens.spacing.xs)
            ) {
                Text(
                    text = "X: ${x.toInt()} px",
                    style = SaltyTokens.typography.labelSmall,
                    color = colors.primary
                )
            }
            Box(
                modifier = Modifier
                    .clip(shapes.sm)
                    .background(colors.surfaceVariant)
                    .clickable { editingAxis = "Y" }
                    .padding(horizontal = SaltyTokens.spacing.sm, vertical = SaltyTokens.spacing.xs)
            ) {
                Text(
                    text = "Y: ${y.toInt()} px",
                    style = SaltyTokens.typography.labelSmall,
                    color = colors.primary
                )
            }
        }
        if (editingAxis == "X") {
            SaltyValueEntryStrip(
                label = "Move X",
                currentValue = x,
                range = range,
                unit = "px",
                onConfirm = {
                    editingAxis = null
                    onCommitX(it)
                },
                onDismiss = { editingAxis = null }
            )
        } else if (editingAxis == "Y") {
            SaltyValueEntryStrip(
                label = "Move Y",
                currentValue = y,
                range = range,
                unit = "px",
                onConfirm = {
                    editingAxis = null
                    onCommitY(it)
                },
                onDismiss = { editingAxis = null }
            )
        }
    }
}
