package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.model.FillData
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.registry.GradientOverlayBindings
import com.motionssalt.saltygraphics.uikit.registry.OverlayRenderContext
import com.motionssalt.saltygraphics.uikit.registry.ScaffoldCanvasOverlay

/** Binding-only facade: shared overlay registration owns rendering and interaction. */
@Composable
fun GradientCanvasOverlay(
    document: ProjectDocument,
    layer: Layer,
    zoom: Float,
    pan: Offset,
    onPreview: (FillData) -> Unit,
    onCommit: (ActionIdentity, FillData) -> Unit,
    onCancel: () -> Unit
) {
    ScaffoldCanvasOverlay(
        id = "gradient_handles",
        context = OverlayRenderContext(
            document = document,
            layer = layer,
            canvasSize = document.canvasSize,
            zoom = zoom,
            pan = pan,
            gradientEditing = GradientOverlayBindings(onPreview, onCommit, onCancel)
        )
    )
}
