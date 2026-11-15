package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object SwitchContrastHelper {
    private fun linearize(channel: Float): Float {
        return if (channel <= 0.04045f) channel / 12.92f
        else ((channel + 0.055f) / 1.055f).pow(2.4f)
    }

    fun luminance(color: Color): Float {
        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    fun contrastRatio(c1: Color, c2: Color): Float {
        val l1 = luminance(c1)
        val l2 = luminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}

@Composable
fun SaltySwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    com.motionssalt.saltygraphics.uikit.controls.SaltySwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled
    )
}
