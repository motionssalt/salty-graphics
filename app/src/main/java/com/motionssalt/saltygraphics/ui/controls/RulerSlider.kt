package com.motionssalt.saltygraphics.ui.controls

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
import kotlin.math.ceil
import kotlin.math.floor
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
    onValueChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var isEditorOpen by remember { mutableStateOf(false) }

    // Internal continuous float state for 1:1 smooth dragging without losing sub-step deltas
    var internalValue by remember { mutableFloatStateOf(value) }
    var isInteracting by remember { mutableStateOf(false) }

    // Sync from external value when not actively dragging or animating
    LaunchedEffect(value) {
        if (!isInteracting) {
            internalValue = value
        }
    }

    val formattedValue = remember(value, step, unit) {
        val numStr = if (step >= 1f && value % 1f == 0f) {
            value.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
        }
        if (unit.isNotBlank()) "$numStr $unit" else numStr
    }

    val tickPixelSpacing = 16f // pixels per step

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnCommit by rememberUpdatedState(onValueChangeFinished)
    val currentMin by rememberUpdatedState(minValue)
    val currentMax by rememberUpdatedState(maxValue)
    val currentDefault by rememberUpdatedState(defaultValue)
    val currentStep by rememberUpdatedState(step)

    fun applyNewValue(rawVal: Float) {
        val clamped = rawVal.coerceIn(currentMin, currentMax)
        internalValue = clamped
        val s = currentStep
        val d = currentDefault
        val snapped = if (abs(clamped - d) < s * 0.35f) {
            d
        } else if (abs(clamped) < s * 0.35f && currentMin <= 0f && currentMax >= 0f) {
            0f
        } else {
            (clamped / s).roundToInt() * s
        }.coerceIn(currentMin, currentMax)

        if (snapped != value) {
            currentOnValueChange(snapped)
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
                internalValue = currentDefault
                currentOnValueChange(currentDefault)
                currentOnCommit?.invoke()
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
                .pointerInput(Unit) {
                    val velocityTracker = VelocityTracker()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        var previousX = down.position.x
                        var isDragging = false
                        var totalDx = 0f
                        var totalDy = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                // Gesture released: handle fling momentum or commit
                                val velocity = velocityTracker.calculateVelocity().x
                                if (isDragging && abs(velocity) > 150f) {
                                    isInteracting = true
                                    coroutineScope.launch {
                                        val anim = Animatable(internalValue)
                                        val targetVelocity = -velocity / tickPixelSpacing * currentStep
                                        anim.animateDecay(targetVelocity, exponentialDecay(frictionMultiplier = 2.0f)) {
                                            applyNewValue(this.value)
                                        }
                                        isInteracting = false
                                        currentOnCommit?.invoke()
                                    }
                                } else {
                                    isInteracting = false
                                    if (isDragging) {
                                        currentOnCommit?.invoke()
                                    }
                                }
                                break
                            }

                            val currentX = change.position.x
                            val currentY = change.position.y
                            val deltaX = currentX - previousX
                            previousX = currentX

                            totalDx += (currentX - down.position.x)
                            totalDy += (currentY - down.position.y)

                            if (!isDragging) {
                                val dx = abs(currentX - down.position.x)
                                val dy = abs(currentY - down.position.y)
                                // If vertical scroll is dominant, let parent scrolling container handle it
                                if (dy > 16f && dy > dx * 1.2f) {
                                    break
                                }
                                if (dx > 6f && dx > dy * 0.8f) {
                                    isDragging = true
                                    isInteracting = true
                                    change.consume()
                                }
                            }

                            if (isDragging && deltaX != 0f) {
                                change.consume() // Claim horizontal drag so parent vertical scroll does not steal it
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                val deltaValue = (-deltaX / tickPixelSpacing) * currentStep
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

                // Draw continuous tick marks relative to center based on continuous internalValue
                val currentVal = internalValue
                val s = currentStep
                val minV = currentMin
                val maxV = currentMax
                val defV = currentDefault

                val halfWidth = w / 2f
                val minVisibleVal = currentVal - ((halfWidth + tickPixelSpacing * 2f) / tickPixelSpacing) * s
                val maxVisibleVal = currentVal + ((halfWidth + tickPixelSpacing * 2f) / tickPixelSpacing) * s

                val firstTickIndex = floor(((minVisibleVal - minV) / s).toDouble()).toLong()
                val lastTickIndex = ceil(((maxVisibleVal - minV) / s).toDouble()).toLong()

                for (tickIndex in firstTickIndex..lastTickIndex) {
                    val tickVal = minV + (tickIndex * s)
                    if (tickVal < minV || tickVal > maxV) continue

                    // Continuous sub-pixel screen x coordinate: translates 1:1 with finger
                    val x = centerX + ((tickVal - currentVal) / s) * tickPixelSpacing
                    if (x < -tickPixelSpacing || x > w + tickPixelSpacing) continue

                    val isMajor = tickIndex % 5L == 0L
                    val isSuper = tickIndex % 10L == 0L || tickVal == defV || (tickVal == 0f && minV < 0f)

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
                onValueChangeFinished?.invoke()
            },
            onDismiss = { isEditorOpen = false }
        )
    }
}
