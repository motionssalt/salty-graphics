package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Dimensions
    val trackWidth = 48.dp
    val trackHeight = 26.dp
    val thumbBaseSize = 20.dp
    val thumbPressedSize = 22.dp
    val currentThumbSize = if (isPressed) thumbPressedSize else thumbBaseSize

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 3.dp,
        animationSpec = tween(durationMillis = 200),
        label = "thumbOffset"
    )

    // Contrast-tuned colors for both light and dark themes
    val trackColor = when {
        !enabled -> if (checked) colors.primary.copy(alpha = 0.38f) else colors.surfaceVariant.copy(alpha = 0.38f)
        checked -> colors.primary
        else -> colors.surfaceVariant
    }

    val trackBorderColor = when {
        !enabled -> colors.outline.copy(alpha = 0.2f)
        checked -> colors.primary
        else -> colors.outline
    }

    val thumbColor = when {
        !enabled -> colors.onSurface.copy(alpha = 0.38f)
        checked -> colors.onPrimary
        else -> colors.onSurfaceVariant
    }

    // Touch target of at least 48dp
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .toggleable(
                value = checked,
                onValueChange = { onCheckedChange?.invoke(it) },
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Switch
            ),
        contentAlignment = Alignment.Center
    ) {
        // Track
        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(trackHeight)
                .clip(RoundedCornerShape(13.dp))
                .background(trackColor)
                .border(width = 1.5.dp, color = trackBorderColor, shape = RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            // Thumb
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(currentThumbSize)
                    .clip(CircleShape)
                    .background(thumbColor),
                contentAlignment = Alignment.Center
            ) {
                if (checked && enabled) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
