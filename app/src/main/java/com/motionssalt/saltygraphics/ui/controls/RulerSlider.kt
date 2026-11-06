package com.motionssalt.saltygraphics.ui.controls

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()
    var isEditorOpen by remember { mutableStateOf(false) }

    // Internal continuous float state for 1:1 smooth dragging without losing sub-step deltas
    var internalValue by remember(value) { mutableFloatStateOf(value) }
    var lastHapticTime by remember { mutableLongStateOf(0L) }

    val formattedValue = remember(value, step, unit) {
        val numStr = if (step >= 1f && value % 1f == 0f) {
            value.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
        }
        if (unit.isNotBlank()) "$numStr $unit" else numStr
    }

    val tickPixelSpacing = 16f // pixels per step

    fun applyNewValue(rawVal: Float) {
        val clamped = rawVal.coerceIn(minValue, maxValue)
        internalValue = clamped
        val snapped = if (abs(clamped - defaultValue) < step * 0.35f) {
            defaultValue
        } else if (abs(clamped) < step * 0.35f && minValue <= 0f && maxValue >= 0f) {
            0f
        } else {
            (clamped / step).roundToInt() * step
        }.coerceIn(minValue, maxValue)

        if (snapped != value) {
            onValueChange(snapped)
            val now = System.currentTimeMillis()
            if (now - lastHapticTime > 45L) {
                lastHapticTime = now
                try {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                } catch (_: Throwable) {}
            }
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
                internalValue = defaultValue
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

        // Center Ruler Canvas with instant touch response & momentum fling
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.background)
                .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .pointerInput(minValue, maxValue, step) {
                    val velocityTracker = VelocityTracker()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        var previousX = down.position.x
                        var isDragging = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                // Gesture released: handle fling momentum
                                val velocity = velocityTracker.calculateVelocity().x
                                if (abs(velocity) > 200f) {
                                    coroutineScope.launch {
                                        val anim = Animatable(internalValue)
                                        val targetVelocity = -velocity / tickPixelSpacing * step
                                        anim.animateDecay(targetVelocity, exponentialDecay(frictionMultiplier = 2.0f)) {
                                            applyNewValue(this.value)
                                        }
                                    }
                                }
                                break
                            }

                            val currentX = change.position.x
                            val deltaX = currentX - previousX
                            previousX = currentX

                            if (!isDragging && abs(currentX - down.position.x) > 2f) {
                                isDragging = true
                            }

                            if (isDragging && deltaX != 0f) {
                                change.consume() // Claim horizontal drag so parent vertical scroll does not steal it
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                val deltaValue = (-deltaX / tickPixelSpacing) * step
                                applyNewValue(internalValue + deltaValue)
                            }
                        }
                    }
                },
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
                drawCircle(
                    color = colors.primary,
                    radius = 3.5f,
                    center = Offset(centerX, h * 0.12f)
                )
            }
        }

        // Tappable Value Pill
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
            onValueApplied = {
                internalValue = it
                onValueChange(it)
            },
            onDismiss = { isEditorOpen = false }
        )
    }
}
