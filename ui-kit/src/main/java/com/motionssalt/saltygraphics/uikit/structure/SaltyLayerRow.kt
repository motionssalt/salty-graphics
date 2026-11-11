package com.motionssalt.saltygraphics.uikit.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

/**
 * Standard Layer Panel Row component.
 * Features:
 * - One dedicated grab handle for long-press & drag reorder (V9).
 * - Layer content thumbnail with fallback type icon (V10).
 * - Multi-select checkbox.
 * - Visibility & lock toggles.
 */
@Composable
fun SaltyLayerRow(
    id: String,
    name: String,
    isSelected: Boolean,
    isVisible: Boolean,
    isLocked: Boolean,
    onSelectToggle: (Boolean) -> Unit,
    onVisibilityToggle: () -> Unit,
    onLockToggle: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: (@Composable () -> Unit)? = null,
    fallbackIcon: ImageVector? = null,
    onRowClick: (() -> Unit)? = null
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes

    val bg = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant
    val borderColor = if (isSelected) colors.primary else colors.outlineVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shapes.sm)
            .background(bg)
            .border(1.dp, borderColor, shapes.sm)
            .then(if (onRowClick != null) Modifier.clickable(onClick = onRowClick) else Modifier)
            .padding(horizontal = SaltyTokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Drag handle (reorder V9), Checkbox, Thumbnail, Layer Name
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)
        ) {
            // Dedicated grab handle for long-press reorder (V9)
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder handle",
                tint = colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )

            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.primary,
                    uncheckedColor = colors.outline
                ),
                modifier = Modifier.size(24.dp)
            )

            // Layer thumbnail with fallback type icon (V10)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(shapes.xs)
                    .background(colors.surface)
                    .border(1.dp, colors.outlineVariant, shapes.xs),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    thumbnail()
                } else if (fallbackIcon != null) {
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = name,
                style = SaltyTokens.typography.bodyMedium,
                color = colors.textPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right side: Visibility & Lock icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xxs)
        ) {
            IconButton(onClick = onVisibilityToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (isVisible) "Hide layer" else "Show layer",
                    tint = if (isVisible) colors.textPrimary else colors.textDisabled,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onLockToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isLocked) "Unlock layer" else "Lock layer",
                    tint = if (isLocked) colors.secondary else colors.textDisabled,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
