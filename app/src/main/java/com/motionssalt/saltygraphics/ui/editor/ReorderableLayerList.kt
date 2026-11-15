package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.uikit.structure.SaltyReorderableLayerList

@Composable
internal fun ReorderableLayerList(
    layers: List<Layer>,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (Layer, Boolean) -> Unit
) {
    SaltyReorderableLayerList(
        layers = layers,
        onMove = onMove,
        modifier = modifier,
        row = row
    )
}
