package com.motionssalt.saltygraphics.uikit.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens
import kotlin.math.roundToInt

/**
 * Progress slider with track, thumb, and clickable value pill.
 * Strictly NO haptic feedback per V5 requirements.
 */
@Composable
fun SaltyProgressSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueCommit: (Float) -> Unit = onValueChange,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    step: Float = 1f,
    unit: String = "",
    label: String = "",
    defaultValue: Float = 0f,
    onValueClick: (() -> Unit)? = null,
    onValueCancel: () -> Unit = {}
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes

    val liveValue by rememberUpdatedState(value)
    val changeCallback by rememberUpdatedState(onValueChange)
    val commitCallback by rememberUpdatedState(onValueCommit)
    val cancelCallback by rememberUpdatedState(onValueCancel)
    val liveRange by rememberUpdatedState(range)
    val liveStep by rememberUpdatedState(step)
    var dragValue by remember { mutableFloatStateOf(value) }
    fun valueAt(fraction: Float): Float {
        val raw = liveRange.start + fraction * (liveRange.endInclusive - liveRange.start)
        return if (liveStep > 0f) (liveRange.start +
            Math.round((raw - liveRange.start) / liveStep) * liveStep).coerceIn(liveRange.start, liveRange.endInclusive) else raw
    }
    val fraction = if (range.start == range.endInclusive) 0f else ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)
    ) {
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

            Box(
                modifier = Modifier
                    .background(colors.surfaceVariant, shape = shapes.pill)
                    .clickable { onValueClick?.invoke() }
                    .padding(horizontal = SaltyTokens.spacing.sm, vertical = SaltyTokens.spacing.xxs)
            ) {
                val formatted = if (step >= 1f) {
                    value.roundToInt().toString()
                } else {
                    String.format("%.1f", value)
                }
                Text(
                    text = if (unit.isNotEmpty()) "$formatted $unit" else formatted,
                    style = SaltyTokens.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.primary
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(SaltyTokens.spacing.progressHeight)
                .clip(shapes.pill)
                .background(colors.surfaceVariant)
                .testTag("progress_track")
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val next = valueAt((offset.x / size.width).coerceIn(0f, 1f))
                        changeCallback(next)
                        commitCallback(next)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragValue = liveValue },
                        onDragEnd = { commitCallback(dragValue) },
                        onDragCancel = { cancelCallback() }
                    ) { change, _ ->
                        change.consume()
                        dragValue = valueAt((change.position.x / size.width).coerceIn(0f, 1f))
                        changeCallback(dragValue)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidth = maxWidth

            // Filled progress track
            Box(
                modifier = Modifier
                    .width(totalWidth * fraction)
                    .fillMaxHeight()
                    .background(colors.primary.copy(alpha = 0.85f))
            )

            // Thumb indicator knob
            Box(
                modifier = Modifier
                    .padding(start = (totalWidth * fraction - 12.dp).coerceAtLeast(0.dp))
                    .size(24.dp)
                    .clip(shapes.pill)
                    .background(colors.thumbOn)
            )
        }
    }
}
