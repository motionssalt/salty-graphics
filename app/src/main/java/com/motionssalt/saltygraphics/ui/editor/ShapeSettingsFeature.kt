package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ShapeData
import com.motionssalt.saltygraphics.engine.model.ShapeType
import com.motionssalt.saltygraphics.uikit.registry.*

/** Shape geometry is declaration-only; each lens changes one field on the live target. */
object ShapeSettingsFeature {
    private fun geometry(
        id: String, label: String, property: String,
        range: ClosedFloatingPointRange<Float>, default: Float, unit: String = "px",
        shapeType: ShapeType? = null,
        read: (ShapeData) -> Float,
        write: (ShapeData, Float) -> ShapeData
    ) = FeatureControlDef(
        id = id, label = label, type = BoundControlType.RULER_SLIDER,
        binding = PropertyBinding(DocumentPropertyPath(
            key = "layer.shapeData.$property", target = "Shape", property = property,
            read = { doc, target -> doc.findLayerById(target.orEmpty())?.shapeData?.let(read) ?: default },
            write = { live, target, value ->
                live.updateLayer(target.orEmpty()) { layer ->
                    layer.shapeData?.let { layer.copy(shapeData = write(it, value)) } ?: layer
                }
            },
            exists = { doc, target ->
                val layer = doc.findLayerById(target.orEmpty())
                val shape = layer?.shapeData
                layer?.type == LayerType.SHAPE && shape != null &&
                    (shapeType == null || shape.type == shapeType)
            }
        )),
        range = range, step = 1f, unit = unit, defaultValue = default,
        isVisible = { doc, target ->
            val layer = doc.findLayerById(target.orEmpty())
            val shape = layer?.shapeData
            layer?.type == LayerType.SHAPE && shape != null &&
                (shapeType == null || shape.type == shapeType)
        }
    )

    // Keep the pre-migration ranges and reset values. Never rewrite fill/stroke on a size change.
    val width = geometry("shape_width", "Width", "width", 1f..50000f, 200f,
        read = { it.width }, write = { s, v -> s.copy(width = v) })
    val height = geometry("shape_height", "Height", "height", 1f..50000f, 200f,
        read = { it.height }, write = { s, v -> s.copy(height = v) })
    val cornerRadius = geometry("shape_corner_radius", "Corner Radius", "cornerRadius", 0f..50000f, 16f,
        shapeType = ShapeType.ROUNDED_RECTANGLE,
        read = { it.cornerRadius }, write = { s, v -> s.copy(cornerRadius = v) })
    val sides = geometry("shape_sides", "Sides", "sides", 3f..100f, 5f, unit = "",
        shapeType = ShapeType.POLYGON,
        read = { it.sides.toFloat() }, write = { s, v -> s.copy(sides = v.toInt()) })
    val starPoints = geometry("shape_star_points", "Star Points", "starPoints", 3f..100f, 5f, unit = "",
        shapeType = ShapeType.STAR,
        read = { it.starPoints.toFloat() }, write = { s, v -> s.copy(starPoints = v.toInt()) })
    val innerRadius = geometry("shape_inner_radius", "Inner Radius Ratio", "starInnerRadiusRatio", 10f..95f, 50f, unit = "%",
        shapeType = ShapeType.STAR,
        read = { it.starInnerRadiusRatio * 100f }, write = { s, v -> s.copy(starInnerRadiusRatio = v / 100f) })

    val definition = FeatureDefinition(
        id = "shape_settings", title = "Shape", icon = Icons.Default.Category,
        placement = ToolPlacement.LAYER_SELECTED, supportedLayerTypes = setOf(LayerType.SHAPE),
        defaultPageId = "shape_geometry",
        pages = listOf(FeaturePageDef("shape_geometry", "Shape", sections = listOf(
            FeatureSectionDef("geometry", controls = listOf(width, height, cornerRadius, sides, starPoints, innerRadius))
        )))
    )
    val catalog = FeatureCatalog(requireTypedBindings = true).apply { register(definition) }
}
