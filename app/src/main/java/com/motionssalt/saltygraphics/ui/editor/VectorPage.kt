package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.uikit.overlays.SaltyVectorToolMode
import com.motionssalt.saltygraphics.uikit.sublibs.vector.SaltyVectorPage

enum class VectorTool(val label: String, val hint: String) {
    SELECT("Select", "Tap point or handle to adjust"),
    ADD("Add point", "Tap on canvas to add point"),
    REMOVE("Remove point", "Tap point to delete"),
    SMOOTH("Smooth / Corner", "Tap point to toggle curve")
}

private fun VectorTool.toLibrary(): SaltyVectorToolMode = when (this) {
    VectorTool.SELECT -> SaltyVectorToolMode.SELECT
    VectorTool.ADD -> SaltyVectorToolMode.ADD
    VectorTool.REMOVE -> SaltyVectorToolMode.REMOVE
    VectorTool.SMOOTH -> SaltyVectorToolMode.SMOOTH
}

private fun SaltyVectorToolMode.toApp(): VectorTool = when (this) {
    SaltyVectorToolMode.SELECT -> VectorTool.SELECT
    SaltyVectorToolMode.ADD -> VectorTool.ADD
    SaltyVectorToolMode.REMOVE -> VectorTool.REMOVE
    SaltyVectorToolMode.SMOOTH -> VectorTool.SMOOTH
}

@Composable
fun VectorPage(
    layer: Layer,
    pathIndex: Int,
    selectedPoint: Int?,
    tool: VectorTool,
    onPathIndex: (Int) -> Unit,
    onPoint: (Int?) -> Unit,
    onTool: (VectorTool) -> Unit,
    onUpdate: (String, Layer) -> Unit,
    onBack: () -> Unit
) {
    SaltyVectorPage(
        layer = layer,
        pathIndex = pathIndex,
        selectedPoint = selectedPoint,
        tool = tool.toLibrary(),
        onPathIndex = onPathIndex,
        onPoint = onPoint,
        onTool = { onTool(it.toApp()) },
        onUpdate = onUpdate,
        onBack = onBack
    )
}
