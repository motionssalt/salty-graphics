package com.motionssalt.saltygraphics.uikit.controls

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

/**
 * High-contrast switch with visible track outline in OFF and solid accent in ON with checkmark.
 * Guarantees high contrast across dark and light themes (>= 3:1).
 */
@Composable
fun SaltySwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes
    val interactionSource = remember { MutableInteractionSource() }

    val trackColor = if (checked) colors.trackOn else colors.trackOff
    val trackBorderColor = if (checked) colors.trackOn else colors.outline
    val thumbColor = if (checked) colors.thumbOn else colors.thumbOff
    val alpha = if (enabled) 1.0f else 0.38f

    val alignmentBias by animateFloatAsState(
        targetValue = if (checked) 1f else -1f,
        label = "switch_thumb_bias"
    )

    Box(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(shapes.pill)
            .background(trackColor.copy(alpha = alpha))
            .border(width = 1.5.dp, color = trackBorderColor.copy(alpha = alpha), shape = shapes.pill)
            .then(
                if (enabled && onCheckedChange != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Switch,
                        onClick = { onCheckedChange(!checked) }
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp),
            contentAlignment = BiasAlignment(alignmentBias, 0f)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(shapes.pill)
                    .background(thumbColor.copy(alpha = alpha)),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.trackOn,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
