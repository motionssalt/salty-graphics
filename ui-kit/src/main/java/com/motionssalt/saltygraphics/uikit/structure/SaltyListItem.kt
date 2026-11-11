package com.motionssalt.saltygraphics.uikit.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

/**
 * Standard list item with thumbnail, title, subtitle, trailing action/handle.
 */
@Composable
fun SaltyListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingThumbnail: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes

    val bg = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant
    val borderColor = if (isSelected) colors.primary else colors.outlineVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.sm)
            .background(bg)
            .border(1.dp, borderColor, shapes.sm)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = SaltyTokens.spacing.md, vertical = SaltyTokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.md)
        ) {
            if (leadingThumbnail != null) {
                leadingThumbnail()
            }
            Column {
                Text(
                    text = title,
                    style = SaltyTokens.typography.bodyMedium,
                    color = colors.textPrimary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = SaltyTokens.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        if (trailingAction != null) {
            trailingAction()
        }
    }
}

/**
 * Empty state component with icon, message, and action button.
 */
@Composable
fun SaltyEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = SaltyTokens.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SaltyTokens.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(SaltyTokens.spacing.md))
        }

        Text(
            text = message,
            style = SaltyTokens.typography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(SaltyTokens.spacing.lg))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                )
            ) {
                Text(actionText)
            }
        }
    }
}
