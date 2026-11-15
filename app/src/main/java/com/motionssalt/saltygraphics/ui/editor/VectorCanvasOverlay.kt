package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.uikit.registry.OverlayRenderContext
import com.motionssalt.saltygraphics.uikit.registry.ScaffoldCanvasOverlay
import com.motionssalt.saltygraphics.uikit.registry.VectorOverlayBindings
import com.motionssalt.saltygraphics.uikit.overlays.SaltyVectorToolMode

@Composable
fun VectorCanvasOverlay(
    layer: Layer,
    document: ProjectDocument,
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
    ScaffoldCanvasOverlay(
        id = "vector_editor",
        context = OverlayRenderContext(
            document = document,
            layer = layer,
            canvasSize = document.canvasSize,
            zoom = zoom,
            pan = pan,
            vectorEditing = VectorOverlayBindings(
                pathIndex, selectedPoint, mappedTool, onSelectPoint, onPreview, onCommit, onDone
            )
        )
    )
}
