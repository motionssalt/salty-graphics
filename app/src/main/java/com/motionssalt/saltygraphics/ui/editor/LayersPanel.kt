package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.engine.action.PasteMode
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.render.LayerThumbnailRenderer
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxSize
import com.motionssalt.saltygraphics.uikit.sublibs.layers.SaltyLayerRowSublib
import com.motionssalt.saltygraphics.uikit.sublibs.layers.SaltyLayerThumbnailSlot
import com.motionssalt.saltygraphics.uikit.sublibs.layers.SaltyLayersSublib
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
    onUngroupSelected: (() -> Unit)? = null,
    onRasterizeSelected: ((Set<String>) -> Unit)? = null
) {
    SaltyLayersSublib(
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
    SaltyLayerRowSublib(
        layer = layer,
        isSelected = isSelected,
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
    SaltyLayerThumbnailSlot(
        layerName = layer.name,
        layerType = layer.type,
        isSelected = isSelected,
        bitmap = thumbnailBitmap,
        modifier = modifier
    )
}
