package com.motionssalt.saltygraphics.uikit.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

data class DropdownOption<T>(
    val value: T,
    val label: String,
    val subtitle: String? = null
)

/**
 * Inline expanding dropdown list.
 * Follows the Standing UI Rule: strictly NO DropdownMenu popup or window overlay.
 * Expands directly inline within the bottom panel.
 */
@Composable
fun <T> SaltyInlineDropdown(
    selected: T,
    options: List<DropdownOption<T>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes
    var expanded by remember { mutableStateOf(false) }

    val currentOption = options.firstOrNull { it.value == selected }

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

        // Header button toggling inline expansion
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.sm)
                .background(colors.surfaceVariant)
                .border(1.dp, colors.outlineVariant, shapes.sm)
                .clickable { expanded = !expanded }
                .padding(horizontal = SaltyTokens.spacing.md, vertical = SaltyTokens.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentOption?.label ?: "Select",
                style = SaltyTokens.typography.bodyMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium
            )

            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = colors.textSecondary
            )
        }

        // Inline expanding options list
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.sm)
                    .background(colors.surfaceVariant.copy(alpha = 0.6f))
                    .border(1.dp, colors.outlineVariant, shapes.sm)
                    .padding(vertical = SaltyTokens.spacing.xxs)
            ) {
                options.forEach { option ->
                    val isSelected = option.value == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option.value)
                                expanded = false
                            }
                            .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant.copy(alpha = 0f))
                            .padding(horizontal = SaltyTokens.spacing.md, vertical = SaltyTokens.spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = option.label,
                                style = SaltyTokens.typography.bodyMedium,
                                color = if (isSelected) colors.primary else colors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (option.subtitle != null) {
                                Text(
                                    text = option.subtitle,
                                    style = SaltyTokens.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
