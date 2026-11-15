package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderStyle
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.uikit.controls.SegmentChipItem
import com.motionssalt.saltygraphics.uikit.registry.*

/** Appearance declarations only. Every event writes one property of the live layer. */
object BorderShadowFeature {
    private fun exists(doc: ProjectDocument, id: String?) =
        doc.findLayerById(id.orEmpty())?.type?.let { it != LayerType.ADJUSTMENT } == true
    private fun strokeVisible(doc: ProjectDocument, id: String?) =
        doc.findLayerById(id.orEmpty())?.effectiveStroke?.isEnabled == true
    private fun shadowVisible(doc: ProjectDocument, id: String?) =
        doc.findLayerById(id.orEmpty())?.effectiveShadow?.isEnabled == true

    // Retain legacy text/shape/vector mirrors without overwriting any unrelated geometry or paint.
    private fun Layer.withStroke(stroke: StrokeData): Layer {
        val base = copy(strokeData = stroke)
        return when (type) {
            LayerType.SHAPE -> base.copy(shapeData = (shapeData ?: ShapeData()).copy(
                hasStroke = stroke.isEnabled, strokeColor = stroke.color, strokeWidth = stroke.width))
            LayerType.TEXT -> base.copy(textData = (textData ?: TextLayerData()).copy(
                hasOutline = stroke.isEnabled, outlineColor = stroke.color, outlineWidth = stroke.width))
            LayerType.VECTOR -> base.copy(vectorPathData = vectorPathData?.copy(
                hasStroke = stroke.isEnabled, strokeColor = stroke.color, strokeWidth = stroke.width,
                strokeCap = stroke.cap, strokeJoin = stroke.join,
                dashLength = if (stroke.isDashed) stroke.dashLength else 0f,
                dashGap = if (stroke.isDashed) stroke.dashGap else 0f))
            else -> base
        }
    }

    private fun <T> strokeBinding(property: String, default: T, read: (StrokeData) -> T,
                                  write: (StrokeData, T) -> StrokeData) = PropertyBinding(DocumentPropertyPath(
        key = "layer.stroke.$property", target = "Stroke", property = property,
        read = { doc, id -> doc.findLayerById(id.orEmpty())?.effectiveStroke?.let(read) ?: default },
        write = { live, id, value -> live.updateLayer(id.orEmpty()) { it.withStroke(write(it.effectiveStroke, value)) } },
        exists = ::exists
    ))
    private fun <T> shadowBinding(property: String, default: T, read: (ShadowData) -> T,
                                  write: (ShadowData, T) -> ShadowData) = PropertyBinding(DocumentPropertyPath(
        key = "layer.shadow.$property", target = "Shadow", property = property,
        read = { doc, id -> doc.findLayerById(id.orEmpty())?.effectiveShadow?.let(read) ?: default },
        write = { live, id, value -> live.updateLayer(id.orEmpty()) { it.copy(shadowData = write(it.effectiveShadow, value)) } },
        exists = ::exists
    ))
    private fun strokeNumber(id: String, label: String, property: String, range: ClosedFloatingPointRange<Float>,
                             default: Float, step: Float = 1f, unit: String = "px",
                             read: (StrokeData) -> Float, write: (StrokeData, Float) -> StrokeData,
                             visible: (ProjectDocument, String?) -> Boolean = ::strokeVisible) = FeatureControlDef(
        id, label, BoundControlType.RULER_SLIDER, strokeBinding(property, default, read, write),
        range = range, step = step, unit = unit, defaultValue = default, isVisible = visible)
    private fun shadowNumber(id: String, label: String, property: String, range: ClosedFloatingPointRange<Float>,
                             default: Float, unit: String = "px",
                             read: (ShadowData) -> Float, write: (ShadowData, Float) -> ShadowData) = FeatureControlDef(
        id, label, BoundControlType.RULER_SLIDER, shadowBinding(property, default, read, write),
        range = range, step = 1f, unit = unit, defaultValue = default, isVisible = ::shadowVisible)
    private fun <T> strokeChoice(id: String, label: String, property: String, default: T,
                                choices: List<SegmentChipItem<T>>, read: (StrokeData) -> T,
                                write: (StrokeData, T) -> StrokeData) = FeatureControlDef(
        id, label, BoundControlType.SEGMENTED_CHIPS, strokeBinding(property, default, read, write),
        segmentOptions = choices, defaultValue = default, isVisible = ::strokeVisible)

    val strokeEnabled = FeatureControlDef("stroke_enabled", "Stroke / Outline", BoundControlType.SWITCH,
        strokeBinding("enabled", false, { it.isEnabled }, { s, v -> s.copy(isEnabled = v) }), defaultValue = false)
    val strokeWidth = strokeNumber("stroke_width", "Stroke Width", "width", 0f..100f, 4f, step = .5f,
        read = { it.width }, write = { s, v -> s.copy(width = v) })
    val strokeOpacity = strokeNumber("stroke_opacity", "Stroke Opacity", "opacity", 0f..100f, 100f, unit = "%",
        read = { it.opacity * 100f }, write = { s, v -> s.copy(opacity = v / 100f) })
    // Color alpha and the separate opacity are independent. Never fold opacity into the picker.
    val strokeColor = FeatureControlDef("stroke_color", "Stroke Color", BoundControlType.COLOR_PICKER,
        strokeBinding("color", 0xFFFFFFFFL, { it.color }, { s, v -> s.copy(color = v) }),
        defaultValue = 0xFFFFFFFFL, isVisible = ::strokeVisible)
    val strokeDashed = FeatureControlDef("stroke_dashed", "Dashed Stroke", BoundControlType.SWITCH,
        strokeBinding("dashed", false, { it.isDashed }, { s, v -> s.copy(isDashed = v) }),
        defaultValue = false, isVisible = ::strokeVisible)
    val alignment = strokeChoice("stroke_alignment", "Alignment", "alignment", StrokeAlignment.CENTER,
        StrokeAlignment.entries.map { SegmentChipItem(it, it.displayName) }, { it.alignment }, { s, v -> s.copy(alignment = v) })
    val cap = strokeChoice("stroke_cap", "Line Cap", "cap", VectorCap.ROUND,
        VectorCap.entries.map { SegmentChipItem(it, it.name) }, { it.cap }, { s, v -> s.copy(cap = v) })
    val join = strokeChoice("stroke_join", "Line Join", "join", VectorJoin.ROUND,
        VectorJoin.entries.map { SegmentChipItem(it, it.name) }, { it.join }, { s, v -> s.copy(join = v) })
    private fun dashedVisible(doc: ProjectDocument, id: String?) = strokeVisible(doc, id) &&
        doc.findLayerById(id.orEmpty())?.effectiveStroke?.isDashed == true
    val dashLength = strokeNumber("stroke_dash_length", "Dash Length", "dashLength", 1f..100f, 12f,
        read = { it.dashLength }, write = { s, v -> s.copy(dashLength = v) }, visible = ::dashedVisible)
    val dashGap = strokeNumber("stroke_dash_gap", "Dash Gap", "dashGap", 1f..100f, 8f,
        read = { it.dashGap }, write = { s, v -> s.copy(dashGap = v) }, visible = ::dashedVisible)

    val shadowEnabled = FeatureControlDef("shadow_enabled", "Drop Shadow", BoundControlType.SWITCH,
        shadowBinding("enabled", false, { it.isEnabled }, { s, v -> s.copy(isEnabled = v) }), defaultValue = false)
    val shadowColor = FeatureControlDef("shadow_color", "Shadow Color", BoundControlType.COLOR_PICKER,
        shadowBinding("color", 0xFF000000L, { it.color }, { s, v -> s.copy(color = v) }),
        defaultValue = 0xFF000000L, isVisible = ::shadowVisible)
    val shadowBlend = FeatureControlDef("shadow_blend", "Shadow Blend", BoundControlType.SEGMENTED_CHIPS,
        shadowBinding("blendMode", BlendMode.NORMAL, { it.blendMode }, { s, v -> s.copy(blendMode = v) }),
        segmentOptions = listOf(BlendMode.NORMAL, BlendMode.MULTIPLY, BlendMode.DARKEN, BlendMode.LIGHTEN,
            BlendMode.SCREEN, BlendMode.OVERLAY).map { SegmentChipItem(it, it.displayName) },
        defaultValue = BlendMode.NORMAL, isVisible = ::shadowVisible)
    val shadowOpacity = shadowNumber("shadow_opacity", "Shadow Opacity", "opacity", 0f..100f, 50f, unit = "%",
        read = { it.opacity * 100f }, write = { s, v -> s.copy(opacity = v / 100f) })
    val angle = shadowNumber("shadow_angle", "Light Angle", "lightAngleDegrees", 0f..360f, 45f, unit = "°",
        read = { it.lightAngleDegrees }, write = { s, v -> s.copy(lightAngleDegrees = v) })
    val distance = shadowNumber("shadow_distance", "Distance", "distance", 0f..100f, 10f,
        read = { it.distance }, write = { s, v -> s.copy(distance = v) })
    val blur = shadowNumber("shadow_blur", "Blur / Softness", "blurRadius", 0f..100f, 16f,
        read = { it.blurRadius }, write = { s, v -> s.copy(blurRadius = v) })
    val spread = shadowNumber("shadow_spread", "Spread", "spread", 0f..50f, 0f,
        read = { it.spread }, write = { s, v -> s.copy(spread = v) })

    val definition = FeatureDefinition("border_shadow_settings", "Border & Shadow", Icons.Default.BorderStyle,
        ToolPlacement.LAYER_SELECTED, supportedLayerTypes = LayerType.entries.toSet() - LayerType.ADJUSTMENT,
        defaultPageId = "appearance", pages = listOf(FeaturePageDef("appearance", "Border & Shadow", sections = listOf(
            FeatureSectionDef("stroke", controls = listOf(strokeEnabled, alignment, strokeWidth, strokeOpacity,
                strokeColor, strokeDashed, dashLength, dashGap, cap, join)),
            FeatureSectionDef("shadow", controls = listOf(shadowEnabled, shadowColor, shadowBlend, shadowOpacity,
                angle, distance, blur, spread))
        ))))
    val catalog = FeatureCatalog(requireTypedBindings = true).apply { register(definition) }
}
