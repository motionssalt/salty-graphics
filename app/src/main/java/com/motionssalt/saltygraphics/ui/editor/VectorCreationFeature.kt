package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.motionssalt.saltygraphics.uikit.registry.FeatureCatalog
import com.motionssalt.saltygraphics.uikit.registry.FeatureDefinition
import com.motionssalt.saltygraphics.uikit.registry.ToolPlacement

/** Vector creation reuses the same registered chooser as shapes; no feature-specific UI. */
object VectorCreationFeature {
    val catalog = FeatureCatalog(requireTypedBindings = true).apply {
        listOf(
            FeatureDefinition("draw", "Draw with points", Icons.Default.Draw, ToolPlacement.ALWAYS),
            FeatureDefinition("rectangle", "Rectangle", Icons.Default.CropSquare, ToolPlacement.ALWAYS),
            FeatureDefinition("ellipse", "Ellipse", Icons.Default.Circle, ToolPlacement.ALWAYS),
            FeatureDefinition("polygon", "Polygon", Icons.Default.Hexagon, ToolPlacement.ALWAYS),
            FeatureDefinition("star", "Star", Icons.Default.Star, ToolPlacement.ALWAYS),
            FeatureDefinition("import_svg", "Import SVG", Icons.Default.FileOpen, ToolPlacement.ALWAYS)
        ).forEach(::register)
    }
    val availableActions = catalog.getAllFeatures().map { it.id }.toSet()
    fun actionLabel(id: String): String? = catalog.getFeature(id)?.title
}
