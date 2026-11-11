package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import com.motionssalt.saltygraphics.uikit.structure.SaltyEditorTopBar

@Composable
fun TopPanel(
    previewQuality: PreviewQuality,
    canUndo: Boolean,
    canRedo: Boolean,
    showGrid: Boolean,
    gridSize: Int,
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
    onSaveSgClick: () -> Unit = {},
    isCanvasLocked: Boolean = false,
    onToggleCanvasLock: () -> Unit = {}
) {
    SaltyEditorTopBar(
        previewQuality = previewQuality,
        canUndo = canUndo,
        canRedo = canRedo,
        showGrid = showGrid,
        gridSize = gridSize,
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
        onSaveSgClick = onSaveSgClick,
        isCanvasLocked = isCanvasLocked,
        onToggleCanvasLock = onToggleCanvasLock
    )
}
