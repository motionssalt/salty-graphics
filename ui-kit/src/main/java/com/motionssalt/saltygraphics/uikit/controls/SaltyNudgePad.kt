package com.motionssalt.saltygraphics.uikit.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Universal 4-way precision nudge pad with press-and-hold gentle acceleration and
 * a shared pixel step ruler slider (V43).
 */
@Composable
fun SaltyNudgePad(
    stepPx: Float,
    onStepChange: (Float) -> Unit,
    onNudgeDelta: (dx: Float, dy: Float) -> Unit,
    onNudgeCommit: (totalDx: Float, totalDy: Float) -> Unit,
    onNudgeCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
    minStepPx: Float = 1f,
    maxStepPx: Float = 200f
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes
    val hostView = LocalView.current
    val liveStep by rememberUpdatedState(stepPx.coerceIn(minStepPx, maxStepPx))
    val nudgeCb by rememberUpdatedState(onNudgeDelta)
    val commitCb by rememberUpdatedState(onNudgeCommit)
    val cancelCb by rememberUpdatedState(onNudgeCancel)
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxWidth().testTag("nudge_pad"),
        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        @Composable
        fun NudgeButton(
            dxSign: Float,
            dySign: Float,
            icon: ImageVector,
            label: String,
            tag: String
        ) {
            var pressed by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(shapes.md)
                    .background(if (pressed) colors.primary.copy(alpha = 0.2f) else colors.surfaceVariant)
                    .border(1.dp, if (pressed) colors.primary else colors.outline, shapes.md)
                    .semantics {
                        contentDescription = label
                        role = Role.Button
                    }
                    .testTag(tag)
                    .pointerInput(dxSign, dySign) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            pressed = true
                            SaltyHaptics.onKeyToggle(hostView)
                            var totalDx = dxSign * liveStep
                            var totalDy = dySign * liveStep
                            nudgeCb(dxSign * liveStep, dySign * liveStep)
                            val repeatJob = scope.launch {
                                delay(280L)
                                var interval = 130L
                                while (true) {
                                    val stepDx = dxSign * liveStep
                                    val stepDy = dySign * liveStep
                                    totalDx += stepDx
                                    totalDy += stepDy
                                    nudgeCb(stepDx, stepDy)
                                    delay(interval)
                                    interval = (interval * 0.88f).toLong().coerceAtLeast(45L)
                                }
                            }
                            val up = waitForUpOrCancellation()
                            repeatJob.cancel()
                            pressed = false
                            if (up != null) {
                                commitCb(totalDx, totalDy)
                            } else {
                                cancelCb()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (pressed) colors.primary else colors.textPrimary
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)
        ) {
            NudgeButton(0f, -1f, Icons.Default.KeyboardArrowUp, "Nudge Up", "nudge_up")
            Row(
                horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NudgeButton(-1f, 0f, Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Nudge Left", "nudge_left")
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(shapes.md)
                        .background(colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${liveStep.roundToInt()}px",
                        style = SaltyTokens.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
                NudgeButton(1f, 0f, Icons.AutoMirrored.Filled.KeyboardArrowRight, "Nudge Right", "nudge_right")
            }
            NudgeButton(0f, 1f, Icons.Default.KeyboardArrowDown, "Nudge Down", "nudge_down")
        }

        SaltyNumberControl(
            label = "Nudge step",
            value = liveStep,
            min = minStepPx,
            max = maxStepPx,
            defaultValue = 1f,
            step = 1f,
            unit = "px",
            onPreview = { onStepChange(it.coerceIn(minStepPx, maxStepPx)) },
            onCommit = { onStepChange(it.coerceIn(minStepPx, maxStepPx)) },
            onExactCommit = { onStepChange(it.coerceIn(minStepPx, maxStepPx)) }
        )
    }
}
