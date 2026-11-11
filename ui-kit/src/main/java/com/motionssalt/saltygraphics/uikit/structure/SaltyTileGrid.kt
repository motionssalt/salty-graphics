package com.motionssalt.saltygraphics.uikit.structure

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

data class TileItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val isSelected: Boolean = false,
    val isEnabled: Boolean = true
)

/**
 * Tool grid rendering tool tiles cleanly in horizontal/wrapping flow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaltyTileGrid(
    tiles: List<TileItem>,
    onTileClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.toolGridSpacing),
        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.toolGridSpacing),
        maxItemsInEachRow = 5
    ) {
        tiles.forEach { tile ->
            SaltyTile(
                title = tile.title,
                icon = tile.icon,
                isSelected = tile.isSelected,
                isEnabled = tile.isEnabled,
                onClick = { onTileClick(tile.id) }
            )
        }
    }
}

/**
 * Individual tool tile.
 */
@Composable
fun SaltyTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isEnabled: Boolean = true
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes

    val bg = if (isSelected) colors.primary.copy(alpha = 0.2f) else colors.surfaceVariant
    val borderColor = if (isSelected) colors.primary else colors.outlineVariant
    val iconColor = if (isSelected) colors.primary else if (isEnabled) colors.textPrimary else colors.textDisabled
    val textColor = if (isSelected) colors.primary else if (isEnabled) colors.textSecondary else colors.textDisabled

    Column(
        modifier = modifier
            .width(64.dp)
            .height(64.dp)
            .clip(shapes.tile)
            .background(bg)
            .border(1.dp, borderColor, shapes.tile)
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(SaltyTokens.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(SaltyTokens.spacing.xxs))
        Text(
            text = title,
            style = SaltyTokens.typography.labelSmall,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
