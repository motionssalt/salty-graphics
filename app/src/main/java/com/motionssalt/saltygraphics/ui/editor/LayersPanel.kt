package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.engine.action.PasteMode
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.render.LayerThumbnailRenderer
import com.motionssalt.saltygraphics.uikit.structure.SaltyBitmapImage
import com.motionssalt.saltygraphics.uikit.structure.SaltyIcon
import com.motionssalt.saltygraphics.uikit.structure.SaltyIconSize
import com.motionssalt.saltygraphics.uikit.structure.SaltyLayerRow
import com.motionssalt.saltygraphics.uikit.structure.SaltyLayersPanel
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextTone
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LayersPanel(
    layers: List<Layer>,
    selectedLayerIds: Set<String>,
    onSelectLayer: (String) -> Unit,
    onToggleSelectMulti: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit = {},
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDeleteLayers: (Set<String>) -> Unit,
    onRenameLayer: (String, String) -> Unit,
    onCopyLayer: (Layer) -> Unit,
    onPasteProperties: (PasteMode) -> Unit,
    onMoveLayer: (fromIndex: Int, toIndex: Int) -> Unit,
    onEnterGroup: (String) -> Unit,
    onClosePanel: () -> Unit,
    onDuplicateLayer: ((String) -> Unit)? = null,
    onGroupSelected: (() -> Unit)? = null,
    onUngroupSelected: (() -> Unit)? = null
) {
    SaltyLayersPanel(
        layers = layers,
        selectedLayerIds = selectedLayerIds,
        onSelectLayer = onSelectLayer,
        onToggleSelectMulti = onToggleSelectMulti,
        onSelectAll = onSelectAll,
        onSelectNone = onSelectNone,
        onToggleVisibility = onToggleVisibility,
        onToggleLock = onToggleLock,
        onDeleteLayers = onDeleteLayers,
        onRenameLayer = onRenameLayer,
        onCopyLayer = onCopyLayer,
        onPasteProperties = onPasteProperties,
        onMoveLayer = onMoveLayer,
        onEnterGroup = onEnterGroup,
        onClosePanel = onClosePanel,
        onDuplicateLayer = onDuplicateLayer,
        onGroupSelected = onGroupSelected,
        onUngroupSelected = onUngroupSelected,
        thumbnailContent = { layer, isSelected ->
            LayerThumbnailView(
                layer = layer,
                isSelected = isSelected,
                modifier = Modifier.saltyFillMaxSize()
            )
        }
    )
}

@Composable
fun LayerRow(
    layer: Layer,
    isSelected: Boolean,
    @Suppress("UNUSED_PARAMETER") isMultiSelectMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onEnterGroup: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onRenameClick: () -> Unit
) {
    val fallback = when (layer.type) {
        LayerType.MEDIA -> Icons.Default.Image
        LayerType.TEXT -> Icons.Default.TextFields
        LayerType.ADJUSTMENT -> Icons.Default.Tune
        LayerType.SHAPE -> Icons.Default.Category
        LayerType.VECTOR -> Icons.Default.Draw
        LayerType.GROUP -> Icons.Default.Folder
        LayerType.QUARANTINED -> Icons.Default.Warning
    }
    SaltyLayerRow(
        id = layer.id,
        name = layer.name,
        subtitle = "${layer.type.displayName} • ${layer.blendMode.displayName}",
        isSelected = isSelected,
        isVisible = layer.isVisible,
        isLocked = layer.isLocked,
        showGroupButton = layer.type == LayerType.GROUP,
        fallbackIcon = fallback,
        thumbnail = {
            LayerThumbnailView(
                layer = layer,
                isSelected = isSelected,
                modifier = Modifier.saltyFillMaxSize()
            )
        },
        onVisibilityToggle = onToggleVisibility,
        onLockToggle = onToggleLock,
        onEnterGroup = onEnterGroup,
        onRowClick = onClick,
        onRowLongClick = onLongClick
    )
}

@Composable
fun LayerThumbnailView(
    layer: Layer,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    var thumbnailBitmap by remember(layer.id, layer.hashCode()) {
        mutableStateOf(LayerThumbnailRenderer.getCached(layer))
    }
    LaunchedEffect(layer.id, layer.hashCode()) {
        if (thumbnailBitmap == null) {
            val bmp = withContext(Dispatchers.Default) {
                LayerThumbnailRenderer.renderThumbnail(layer)
            }
            thumbnailBitmap = bmp
        }
    }
    val currentBmp = thumbnailBitmap
    if (currentBmp != null) {
        SaltyBitmapImage(
            bitmap = currentBmp,
            contentDescription = "${layer.name} thumbnail",
            modifier = modifier.saltyFillMaxSize()
        )
    } else {
        SaltyIcon(
            icon = when (layer.type) {
                LayerType.MEDIA -> Icons.Default.Image
                LayerType.TEXT -> Icons.Default.TextFields
                LayerType.ADJUSTMENT -> Icons.Default.Tune
                LayerType.SHAPE -> Icons.Default.Category
                LayerType.VECTOR -> Icons.Default.Draw
                LayerType.GROUP -> Icons.Default.Folder
                LayerType.QUARANTINED -> Icons.Default.Warning
            },
            tone = if (isSelected) SaltyTextTone.ACCENT else SaltyTextTone.SECONDARY,
            size = SaltyIconSize.MD
        )
    }
}
