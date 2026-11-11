package com.motionssalt.saltygraphics.uikit.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

data class SegmentChipItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null
)

/**
 * Segmented chips row for category and mode selection.
 */
@Composable
fun <T> SaltySegmentedChips(
    selected: T,
    options: List<SegmentChipItem<T>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = SaltyTokens.typography.labelMedium,
                color = colors.textSecondary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.pill)
                .background(colors.surfaceVariant)
                .border(1.dp, colors.outlineVariant, shapes.pill)
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEach { option ->
                val isSelected = option.value == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.pill)
                        .background(if (isSelected) colors.primary else colors.surfaceVariant.copy(alpha = 0f))
                        .clickable { onSelect(option.value) }
                        .padding(vertical = SaltyTokens.spacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)
                    ) {
                        if (option.icon != null) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (isSelected) colors.onPrimary else colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = option.label,
                            style = SaltyTokens.typography.labelMedium,
                            color = if (isSelected) colors.onPrimary else colors.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
