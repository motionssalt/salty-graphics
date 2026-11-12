package com.motionssalt.saltygraphics.uikit.controls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.Job
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Universal scroll-wheel (ruler) slider.
 * Renders continuous tick marks with a center needle indicator.
 * Per V5 requirements: strictly NO haptic feedback.
 */
@Composable
fun SaltyRulerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueCommit: (Float) -> Unit = onValueChange,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = -1000f..1000f,
    step: Float = 1f,
    unit: String = "",
    label: String = "",
    defaultValue: Float = 0f,
    onValueClick: (() -> Unit)? = null,
    onValueCancel: () -> Unit = {}
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes
    val coroutineScope = rememberCoroutineScope()

    var internalValue by remember { mutableFloatStateOf(value) }
    var dragging by remember { mutableStateOf(false) }
    val liveValue by rememberUpdatedState(value)
    val changeCallback by rememberUpdatedState(onValueChange)
    val commitCallback by rememberUpdatedState(onValueCommit)
    val cancelCallback by rememberUpdatedState(onValueCancel)
    val liveRange by rememberUpdatedState(range)
    val liveStep by rememberUpdatedState(step)
    var fling by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(value) { if (!dragging) internalValue = value }
    DisposableEffect(Unit) { onDispose { fling?.cancel() } }
    val decayAnim = remember { Animatable(0f) }

    val tickSpacingPx = 16f
    val stepsPerTick = step.coerceAtLeast(0.01f)
    val pxPerUnit = tickSpacingPx / stepsPerTick


    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)
    ) {
        // Label and clickable value pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = SaltyTokens.typography.labelMedium,
                    color = colors.textSecondary
                )
            }

            // Clickable value pill
            Box(
                modifier = Modifier
                    .background(colors.surfaceVariant, shape = shapes.pill)
                    .clickable { onValueClick?.invoke() }
                    .padding(horizontal = SaltyTokens.spacing.sm, vertical = SaltyTokens.spacing.xxs)
            ) {
                val formatted = if (step >= 1f) {
                    internalValue.roundToInt().toString()
                } else {
                    String.format("%.1f", internalValue)
                }
                Text(
                    text = if (unit.isNotEmpty()) "$formatted $unit" else formatted,
                    style = SaltyTokens.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.primary
                )
            }
        }

        // The ruler track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SaltyTokens.spacing.rulerHeight)
                .background(colors.surfaceVariant.copy(alpha = 0.5f), shape = shapes.sm)
                .testTag("ruler_track")
                .pointerInput(Unit) {
                    val velocity = VelocityTracker()
                    detectDragGestures(
                        onDragStart = { fling?.cancel(); velocity.resetTracking(); internalValue = liveValue; dragging = true },
                        onDragEnd = {
                            val unitsPerSecond = -velocity.calculateVelocity().x * liveStep.coerceAtLeast(.01f) / tickSpacingPx
                            fling = coroutineScope.launch {
                                try {
                                    decayAnim.updateBounds(liveRange.start, liveRange.endInclusive)
                                    decayAnim.snapTo(internalValue)
                                    decayAnim.animateDecay(unitsPerSecond, exponentialDecay()) {
                                        internalValue = this.value.coerceIn(liveRange.start, liveRange.endInclusive)
                                        changeCallback(internalValue)
                                    }
                                    dragging = false
                                    commitCallback(internalValue)
                                } catch (_: CancellationException) { /* next gesture owns the preview */ }
                            }
                        },
                        onDragCancel = { dragging = false; internalValue = liveValue; cancelCallback() }
                    ) { change, amount ->
                        velocity.addPosition(change.uptimeMillis, change.position)
                        change.consume()
                        internalValue = (internalValue - amount.x * liveStep.coerceAtLeast(.01f) / tickSpacingPx)
                            .coerceIn(liveRange.start, liveRange.endInclusive)
                        changeCallback(internalValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val centerX = width / 2f

                val visibleUnitsLeft = centerX / pxPerUnit
                val minVisible = internalValue - visibleUnitsLeft
                val maxVisible = internalValue + visibleUnitsLeft

                val firstTickIndex = (minVisible / stepsPerTick).toInt() - 1
                val lastTickIndex = (maxVisible / stepsPerTick).toInt() + 1

                for (i in firstTickIndex..lastTickIndex) {
                    val tickVal = i * stepsPerTick
                    if (tickVal !in range.start..range.endInclusive) continue

                    val x = centerX + (tickVal - internalValue) * pxPerUnit
                    if (x < 0f || x > width) continue

                    val isMajor = (i % 5 == 0)
                    val tickHeight = if (isMajor) height * 0.55f else height * 0.35f
                    val yStart = height - tickHeight
                    val yEnd = height - 4f

                    drawLine(
                        color = if (isMajor) colors.outline else colors.outlineVariant,
                        start = Offset(x, yStart),
                        end = Offset(x, yEnd),
                        strokeWidth = if (isMajor) 2f else 1.2f,
                        cap = StrokeCap.Round
                    )
                }

                // Center needle indicator
                drawLine(
                    color = colors.primary,
                    start = Offset(centerX, 2f),
                    end = Offset(centerX, height - 2f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
