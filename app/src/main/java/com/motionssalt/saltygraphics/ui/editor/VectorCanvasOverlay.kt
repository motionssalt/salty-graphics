package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.uikit.overlays.SaltyVectorCanvasOverlay
import com.motionssalt.saltygraphics.uikit.overlays.SaltyVectorToolMode

@Composable
fun VectorCanvasOverlay(
    layer: Layer,
    canvasSize: CanvasSize,
    pathIndex: Int,
    selectedPoint: Int?,
    tool: VectorTool,
    zoom: Float,
    pan: Offset,
    onSelectPoint: (Int?) -> Unit,
    onPreview: (Layer?) -> Unit,
    onCommit: (Layer) -> Unit,
    onDone: () -> Unit
) {
    val mappedTool = when (tool) {
        VectorTool.SELECT -> SaltyVectorToolMode.SELECT
        VectorTool.ADD -> SaltyVectorToolMode.ADD
        VectorTool.REMOVE -> SaltyVectorToolMode.REMOVE
        VectorTool.SMOOTH -> SaltyVectorToolMode.SMOOTH
    }
    SaltyVectorCanvasOverlay(
        layer = layer,
        canvasSize = canvasSize,
        pathIndex = pathIndex,
        selectedPoint = selectedPoint,
        tool = mappedTool,
        zoom = zoom,
        pan = pan,
        onSelectPoint = onSelectPoint,
        onPreview = onPreview,
        onCommit = onCommit,
        onDone = onDone
    )
}
