package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.FillData
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.uikit.overlays.SaltyGradientLayerOverlay

@Composable
fun GradientCanvasOverlay(
    layer: Layer,
    canvasSize: CanvasSize,
    zoom: Float,
    pan: Offset,
    onPreview: (FillData) -> Unit,
    onCommit: (FillData) -> Unit
) {
    SaltyGradientLayerOverlay(
        layer = layer,
        canvasSize = canvasSize,
        zoom = zoom,
        pan = pan,
        onPreview = onPreview,
        onCommit = onCommit
    )
}
