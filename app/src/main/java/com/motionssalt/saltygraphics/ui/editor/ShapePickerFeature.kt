package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.motionssalt.saltygraphics.engine.model.ShapeData
import com.motionssalt.saltygraphics.engine.model.ShapeType
import com.motionssalt.saltygraphics.uikit.registry.FeatureCatalog
import com.motionssalt.saltygraphics.uikit.registry.FeatureDefinition
import com.motionssalt.saltygraphics.uikit.registry.ToolPlacement

/** Creation choices are immutable data, not feature-owned layouts or stored editor callbacks. */
object ShapePickerFeature {
    data class Choice(val id: String, val name: String, val icon: ImageVector, val shape: ShapeData)

    val choices = listOf(
        Choice("rounded_rectangle", "Rounded Rect", Icons.Default.RoundedCorner,
            ShapeData(type = ShapeType.ROUNDED_RECTANGLE, width = 300f, height = 200f, cornerRadius = 32f)),
        Choice("rectangle", "Rectangle", Icons.Default.CropSquare,
            ShapeData(type = ShapeType.RECTANGLE, width = 300f, height = 200f)),
        Choice("ellipse", "Circle / Ellipse", Icons.Default.Circle,
            ShapeData(type = ShapeType.CIRCLE, width = 250f, height = 250f)),
        Choice("star", "Star", Icons.Default.Star,
            ShapeData(type = ShapeType.STAR, width = 280f, height = 280f, starPoints = 5, starInnerRadiusRatio = 0.5f)),
        Choice("polygon", "Polygon", Icons.Default.Hexagon,
            ShapeData(type = ShapeType.POLYGON, width = 280f, height = 280f, sides = 6)),
        Choice("line", "Line", Icons.Default.HorizontalRule,
            ShapeData(type = ShapeType.LINE, width = 300f, height = 4f, strokeWidth = 6f, hasStroke = true, hasFill = false)),
        Choice("arrow", "Arrow", Icons.Default.ArrowForward,
            ShapeData(type = ShapeType.ARROW, width = 300f, height = 32f, strokeWidth = 6f, hasStroke = true, hasFill = false))
    )
    val catalog = FeatureCatalog(requireTypedBindings = true).apply {
        choices.forEach { choice -> register(FeatureDefinition(
            id = choice.id, title = choice.name, icon = choice.icon, placement = ToolPlacement.ALWAYS
        )) }
    }
    val availableActions = choices.map { it.id }.toSet()
    fun shape(id: String): ShapeData? = choices.firstOrNull { it.id == id }?.shape
}
