package com.motionssalt.saltygraphics.uikit.structure

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

/**
 * Standard Layer Panel Row component.
 * Zero checkboxes (per V26 hold-to-select architecture):
 * - Tap selects a layer (or toggles in multi-select mode); long-press enters multi-select.
 * - One dedicated grab handle for long-press & drag reorder (V9), tagged layer_handle_<id>.
 * - Layer content thumbnail with fallback type icon (V10).
 * - Visibility, Lock, and optional Group enter action.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SaltyLayerRow(
    id: String,
    name: String,
    isSelected: Boolean,
    isVisible: Boolean,
    isLocked: Boolean,
    onVisibilityToggle: () -> Unit,
    onLockToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    fallbackIcon: ImageVector? = null,
    showGroupButton: Boolean = false,
    onEnterGroup: (() -> Unit)? = null,
    onRowClick: (() -> Unit)? = null,
    onRowLongClick: (() -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") onSelectToggle: (Boolean) -> Unit = {}
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes

    val bg = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant
    val borderColor = if (isSelected) colors.primary else colors.outlineVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(shapes.sm)
            .background(bg)
            .border(1.dp, borderColor, shapes.sm)
            .then(
                if (onRowClick != null || onRowLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onRowClick?.invoke() },
                        onLongClick = onRowLongClick
                    )
                } else Modifier
            )
            .padding(horizontal = SaltyTokens.spacing.sm, vertical = SaltyTokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Thumbnail, Name (+ optional subtitle)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm)
        ) {
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
                        tint = if (isSelected) colors.primary else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = SaltyTokens.typography.bodyMedium,
                    color = if (isVisible) colors.textPrimary else colors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = SaltyTokens.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right side: optional Group enter, Visibility, Lock, Drag Handle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xxs)
        ) {
            if (showGroupButton && onEnterGroup != null) {
                IconButton(onClick = onEnterGroup, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Enter Group",
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(onClick = onVisibilityToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Visibility",
                    tint = if (isVisible) colors.textPrimary else colors.textDisabled,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onLockToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Toggle Lock",
                    tint = if (isLocked) colors.secondary else colors.textDisabled,
                    modifier = Modifier.size(18.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Hold to reorder $name",
                tint = colors.textSecondary,
                modifier = Modifier
                    .size(32.dp)
                    .padding(SaltyTokens.spacing.xs)
                    .testTag("layer_handle_$id")
            )
        }
    }
}
