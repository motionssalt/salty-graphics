package com.motionssalt.saltygraphics.uikit.sublibs.topbar

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import com.motionssalt.saltygraphics.uikit.structure.SaltyEditorTopBar

@Composable
fun SaltyTopBarSublib(
    projectName: String,
    groupBreadcrumbs: List<Pair<String, String>> = emptyList(),
    zoom: Float,
    previewQuality: PreviewQuality,
    canUndo: Boolean,
    canRedo: Boolean,
    showGrid: Boolean,
    gridSize: Int,
    isLayersPanelOpen: Boolean,
    isCanvasLocked: Boolean = false,
    onToggleCanvasLock: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onToggleLayers: () -> Unit,
    onPreviewQualityClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onOpenActionTree: () -> Unit,
    onExportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFitToScreen: () -> Unit,
    onReset100Percent: () -> Unit,
    onToggleGrid: () -> Unit,
    onGridSizeChange: (Int) -> Unit,
    onSaveSgClick: () -> Unit
) {
    SaltyEditorTopBar(
        projectName = projectName,
        groupBreadcrumbs = groupBreadcrumbs,
        zoom = zoom,
        previewQuality = previewQuality,
        canUndo = canUndo,
        canRedo = canRedo,
        showGrid = showGrid,
        gridSize = gridSize,
        isLayersPanelOpen = isLayersPanelOpen,
        isCanvasLocked = isCanvasLocked,
        onToggleCanvasLock = onToggleCanvasLock,
        onNavigateBack = onNavigateBack,
        onToggleLayers = onToggleLayers,
        onPreviewQualityClick = onPreviewQualityClick,
        onUndoClick = onUndoClick,
        onRedoClick = onRedoClick,
        onOpenActionTree = onOpenActionTree,
        onExportClick = onExportClick,
        onSettingsClick = onSettingsClick,
        onZoomIn = onZoomIn,
        onZoomOut = onZoomOut,
        onFitToScreen = onFitToScreen,
        onReset100Percent = onReset100Percent,
        onToggleGrid = onToggleGrid,
        onGridSizeChange = onGridSizeChange,
        onSaveSgClick = onSaveSgClick
    )
}
