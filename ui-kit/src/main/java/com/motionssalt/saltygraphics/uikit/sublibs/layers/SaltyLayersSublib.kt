package com.motionssalt.saltygraphics.uikit.sublibs.layers

import android.graphics.Bitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.motionssalt.saltygraphics.engine.action.PasteMode
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.uikit.structure.SaltyBitmapImage
import com.motionssalt.saltygraphics.uikit.structure.SaltyIcon
import com.motionssalt.saltygraphics.uikit.structure.SaltyIconSize
import com.motionssalt.saltygraphics.uikit.structure.SaltyLayerRow
import com.motionssalt.saltygraphics.uikit.structure.SaltyLayersPanel
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextTone
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxSize

fun layerFallbackIcon(type: LayerType): ImageVector = when (type) {
    LayerType.MEDIA -> Icons.Default.Image
    LayerType.TEXT -> Icons.Default.TextFields
    LayerType.ADJUSTMENT -> Icons.Default.Tune
    LayerType.SHAPE -> Icons.Default.Category
    LayerType.VECTOR -> Icons.Default.Draw
    LayerType.GROUP -> Icons.Default.Folder
    LayerType.QUARANTINED -> Icons.Default.Warning
}

@Composable
fun SaltyLayersSublib(
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
    onUngroupSelected: (() -> Unit)? = null,
    onRasterizeSelected: ((Set<String>) -> Unit)? = null,
    thumbnailContent: @Composable (Layer, Boolean) -> Unit
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
        onRasterizeSelected = onRasterizeSelected,
        thumbnailContent = thumbnailContent
    )
}

@Composable
fun SaltyLayerRowSublib(
    layer: Layer,
    isSelected: Boolean,
    thumbnail: @Composable () -> Unit,
    onVisibilityToggle: () -> Unit,
    onLockToggle: () -> Unit,
    onEnterGroup: () -> Unit,
    onRowClick: () -> Unit,
    onRowLongClick: () -> Unit
) {
    SaltyLayerRow(
        id = layer.id,
        name = layer.name,
        subtitle = "${layer.type.displayName} • ${layer.blendMode.displayName}",
        isSelected = isSelected,
        isVisible = layer.isVisible,
        isLocked = layer.isLocked,
        showGroupButton = layer.type == LayerType.GROUP,
        fallbackIcon = layerFallbackIcon(layer.type),
        thumbnail = thumbnail,
        onVisibilityToggle = onVisibilityToggle,
        onLockToggle = onLockToggle,
        onEnterGroup = onEnterGroup,
        onRowClick = onRowClick,
        onRowLongClick = onRowLongClick
    )
}

@Composable
fun SaltyLayerThumbnailSlot(
    layerName: String,
    layerType: LayerType,
    isSelected: Boolean,
    bitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    if (bitmap != null) {
        SaltyBitmapImage(
            bitmap = bitmap,
            contentDescription = "$layerName thumbnail",
            modifier = modifier.saltyFillMaxSize()
        )
    } else {
        SaltyIcon(
            icon = layerFallbackIcon(layerType),
            tone = if (isSelected) SaltyTextTone.ACCENT else SaltyTextTone.SECONDARY,
            size = SaltyIconSize.MD
        )
    }
}
