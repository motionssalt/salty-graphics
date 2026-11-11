package com.motionssalt.saltygraphics.uikit.structure

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

/**
 * Standard section container inside panel pages.
 */
@Composable
fun SaltySection(
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SaltyTokens.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.rowSpacing)
    ) {
        if (title != null || action != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = SaltyTokens.typography.labelLarge,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (action != null) {
                    action()
                }
            }
        }

        content()
    }
}

/**
 * Standard horizontal row placing a label on the left and a control on the right.
 */
@Composable
fun SaltyRow(
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    control: @Composable () -> Unit
) {
    val colors = SaltyTokens.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SaltyTokens.spacing.controlHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = SaltyTokens.typography.bodyMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = SaltyTokens.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(SaltyTokens.spacing.md))

        control()
    }
}
