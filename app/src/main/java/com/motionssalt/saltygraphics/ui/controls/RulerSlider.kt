package com.motionssalt.saltygraphics.ui.controls

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun RulerSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    minValue: Float,
    maxValue: Float,
    defaultValue: Float = minValue,
    step: Float = 1f,
    unit: String = "",
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    var isEditorOpen by remember { mutableStateOf(false) }

    // Display formatted value
    val formattedValue = remember(value, step, unit) {
        val numStr = if (step >= 1f && value % 1f == 0f) {
            value.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
        }
        if (unit.isNotBlank()) "$numStr $unit" else numStr
    }

    // Touch dragging & momentum
    val range = maxValue - minValue
    val tickPixelSpacing = 16f // pixels per step

    var dragAccumulator by remember { mutableStateOf(0f) }

    val draggableState = rememberDraggableState { delta ->
        // Dragging left moves value up, dragging right moves value down (like a scroll wheel under an indicator)
        val deltaSteps = -delta / tickPixelSpacing
        val deltaValue = deltaSteps * step
        val newValue = (value + deltaValue).coerceIn(minValue, maxValue)

        // Light snap to default, zero, or whole numbers
        val snapped = if (abs(newValue - defaultValue) < step * 0.35f) {
            defaultValue
        } else if (abs(newValue) < step * 0.35f && minValue <= 0f && maxValue >= 0f) {
            0f
        } else {
            (newValue / step).roundToInt() * step
        }.coerceIn(minValue, maxValue)

        if (snapped != value) {
            onValueChange(snapped)
            // Haptic tick
            try {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            } catch (_: Throwable) {}
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label Button: Reset to default on tap
        Surface(
            onClick = {
                onValueChange(defaultValue)
                try {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                } catch (_: Throwable) {}
            },
            shape = RoundedCornerShape(6.dp),
            color = Color.Transparent,
            modifier = Modifier.widthIn(min = 64.dp, max = 96.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = if (value == defaultValue) colors.onSurfaceVariant else colors.primary,
                maxLines = 1
            )
        }

        // Center Ruler Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.background)
                .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f

                // Draw tick marks relative to center
                val visibleTicks = (w / tickPixelSpacing).toInt() + 4
                val currentStepIndex = ((value - minValue) / step).toInt()

                for (i in -visibleTicks / 2..visibleTicks / 2) {
                    val tickIndex = currentStepIndex + i
                    val tickVal = minValue + (tickIndex * step)
                    if (tickVal < minValue || tickVal > maxValue) continue

                    val x = centerX + (i * tickPixelSpacing)
                    if (x < 0 || x > w) continue

                    val isMajor = tickIndex % 5 == 0
                    val isSuper = tickIndex % 10 == 0 || tickVal == defaultValue || (tickVal == 0f && minValue < 0)

                    val tickHeight = when {
                        isSuper -> h * 0.65f
                        isMajor -> h * 0.45f
                        else -> h * 0.28f
                    }
                    val tickColor = when {
                        isSuper -> colors.onSurface.copy(alpha = 0.7f)
                        isMajor -> colors.onSurfaceVariant.copy(alpha = 0.5f)
                        else -> colors.outline.copy(alpha = 0.6f)
                    }

                    val yStart = (h - tickHeight) / 2f
                    drawLine(
                        color = tickColor,
                        start = Offset(x, yStart),
                        end = Offset(x, yStart + tickHeight),
                        strokeWidth = if (isSuper) 2f else 1.2f,
                        cap = StrokeCap.Round
                    )
                }

                // Fixed Center Indicator Needle
                drawLine(
                    color = colors.primary,
                    start = Offset(centerX, h * 0.12f),
                    end = Offset(centerX, h * 0.88f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                // Needle top triangle/pip
                drawCircle(
                    color = colors.primary,
                    radius = 3.5f,
                    center = Offset(centerX, h * 0.12f)
                )
            }
        }

        // Tappable Value Pill (Opens ValueEditorDialog)
        Surface(
            onClick = { isEditorOpen = true },
            shape = RoundedCornerShape(8.dp),
            color = colors.surfaceVariant,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.primary.copy(alpha = 0.4f))),
            modifier = Modifier.widthIn(min = 68.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formattedValue,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = colors.primary,
                    maxLines = 1
                )
            }
        }
    }

    if (isEditorOpen) {
        ValueEditorDialog(
            label = label,
            currentValue = value,
            minValue = minValue,
            maxValue = maxValue,
            step = step,
            unit = unit,
            onValueApplied = { onValueChange(it) },
            onDismiss = { isEditorOpen = false }
        )
    }
}
