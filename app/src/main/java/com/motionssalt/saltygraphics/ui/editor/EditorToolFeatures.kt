package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.uikit.registry.*

/** Tool declarations only: layout and filtering are owned by :ui-kit. */
object EditorToolFeatures {
    private val visual = LayerType.entries.toSet() - LayerType.ADJUSTMENT
    private val rasterizable = setOf(LayerType.TEXT, LayerType.VECTOR, LayerType.SHAPE)
    private fun tool(id: String, title: String, icon: ImageVector, placement: ToolPlacement,
                     types: Set<LayerType> = emptySet()) =
        FeatureDefinition(id, title, icon, placement, supportedLayerTypes = types)

    val catalog = FeatureCatalog().apply {
        listOf(
            tool("media", "Media", Icons.Default.AddPhotoAlternate, ToolPlacement.NO_SELECTION),
            tool("text", "Text", Icons.Default.TextFields, ToolPlacement.NO_SELECTION),
            tool("adjustment", "Adjustment", Icons.Default.Tune, ToolPlacement.NO_SELECTION),
            tool("shape", "Shape", Icons.Default.Category, ToolPlacement.NO_SELECTION),
            tool("vector", "Vector", Icons.Default.Draw, ToolPlacement.NO_SELECTION),
            tool("edit", "Edit", Icons.Default.Edit, ToolPlacement.LAYER_SELECTED),
            tool("transform", "Transform", Icons.Default.OpenWith, ToolPlacement.LAYER_SELECTED),
            tool("fill", "Fill", Icons.Default.FormatColorFill, ToolPlacement.LAYER_SELECTED, visual),
            tool("border_shadow", "Border & Shadow", Icons.Default.BorderStyle, ToolPlacement.LAYER_SELECTED, visual),
            tool("effects", "Effects", Icons.Default.AutoAwesome, ToolPlacement.LAYER_SELECTED),
            tool("blending", "Blending & Mask", Icons.Default.Layers, ToolPlacement.LAYER_SELECTED),
            tool("duplicate", "Duplicate", Icons.Default.ControlPointDuplicate, ToolPlacement.LAYER_SELECTED),
            tool("rasterize", "Rasterize", Icons.Default.PhotoFilter, ToolPlacement.LAYER_SELECTED, rasterizable),
            tool("delete", "Delete", Icons.Default.Delete, ToolPlacement.LAYER_SELECTED)
        ).forEach(::register)
    }
}
