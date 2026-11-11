package com.motionssalt.saltygraphics.uikit.registry
import androidx.compose.runtime.compositionLocalOf
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ProjectDocument

enum class IconToken { MEDIA, TEXT, ADJUSTMENT, SHAPE, VECTOR, EDIT, TRANSFORM, FILL, BORDER_SHADOW,
    EFFECTS, BLENDING, DUPLICATE, RASTERIZE, DELETE, GROUP, LAYERS, HISTORY, VIEW, GRID, SETTINGS, SAMPLE }
enum class FeaturePlacement { NO_SELECTION, LAYER_SELECTED, OPERATION, DEVELOPER }
enum class CanvasOverlayToken { SELECTION, GRADIENT, VECTOR, SNAPPING, GRID }
enum class ControlKind { SWITCH, RULER, PROGRESS, DROPDOWN, SEGMENTED, COLOR, TEXT, TRACKPAD,
    GRADIENT_EDITOR, MEDIA_GRID, LAYER_SELECTOR, CUSTOM }

/** A typed, round-trip-testable document/layer binding; no engine-to-UI dependency. */
data class PropertyPath<T>(val id: String, val read: (ProjectDocument, Layer?) -> T,
    val write: (ProjectDocument, Layer?, T) -> ProjectDocument)
sealed interface ControlBinding {
    val pathId: String
    fun validate(document: ProjectDocument, layer: Layer?): Boolean
}
data class SwitchBinding(val path: PropertyPath<Boolean>) : ControlBinding {
    override val pathId get() = path.id
    override fun validate(document: ProjectDocument, layer: Layer?) = roundTrip(path, document, layer)
}
data class NumberBinding(val path: PropertyPath<Float>, val range: ClosedFloatingPointRange<Float>,
    val step: Float = 1f, val unit: String = "", val defaultValue: Float = range.start) : ControlBinding {
    override val pathId get() = path.id
    override fun validate(document: ProjectDocument, layer: Layer?) =
        range.start < range.endInclusive && step > 0 && defaultValue in range && roundTrip(path, document, layer)
}
data class ChoiceBinding(val path: PropertyPath<String>, val options: List<String>) : ControlBinding {
    override val pathId get() = path.id
    override fun validate(document: ProjectDocument, layer: Layer?) =
        options.isNotEmpty() && options.toSet().size == options.size && roundTrip(path, document, layer)
}
data class ColorBinding(val path: PropertyPath<Long>, val transparentAllowed: Boolean = true) : ControlBinding {
    override val pathId get() = path.id
    override fun validate(document: ProjectDocument, layer: Layer?) = roundTrip(path, document, layer)
}
data class TextBinding(val path: PropertyPath<String>) : ControlBinding {
    override val pathId get() = path.id
    override fun validate(document: ProjectDocument, layer: Layer?) = roundTrip(path, document, layer)
}
data class CustomBinding(val componentId: String, val path: PropertyPath<String>? = null) : ControlBinding {
    override val pathId get() = path?.id ?: "component:$componentId"
    override fun validate(document: ProjectDocument, layer: Layer?) = componentId.isNotBlank() &&
        (path == null || roundTrip(path, document, layer))
}
private fun <T> roundTrip(path: PropertyPath<T>, document: ProjectDocument, layer: Layer?) = runCatching {
    path.write(document, layer, path.read(document, layer)); true
}.getOrDefault(false)

data class ControlSpec(val id: String, val label: String, val kind: ControlKind, val binding: ControlBinding,
    val visible: (ProjectDocument, Layer?) -> Boolean = { _, _ -> true },
    val enabled: (ProjectDocument, Layer?) -> Boolean = { _, _ -> true },
    val verb: String = "Change", val target: String = "Layer")
data class RowSpec(val id: String, val control: ControlSpec)
data class SectionSpec(val id: String, val title: String, val rows: List<RowSpec>)
data class PanelPageSpec(val id: String, val title: String, val sections: List<SectionSpec>, val parentId: String? = null)
data class FeatureSpec(val id: String, val title: String, val icon: IconToken, val placement: FeaturePlacement,
    val layerTypes: Set<LayerType>? = null,
    val condition: (ProjectDocument, Layer?) -> Boolean = { _, _ -> true },
    val pages: List<PanelPageSpec> = emptyList(), val overlays: Set<CanvasOverlayToken> = emptySet())
class FeatureRegistry(features: List<FeatureSpec>) {
    private val features = features.toList()
    init {
        require(features.map { it.id }.distinct().size == features.size) { "Feature ids must be unique" }
        features.forEach(::validate)
    }
    fun all() = features
    fun feature(id: String) = features.firstOrNull { it.id == id }
    fun toolsFor(document: ProjectDocument, layer: Layer?): List<FeatureSpec> = features.filter {
        val placementMatches = if (layer == null) it.placement == FeaturePlacement.NO_SELECTION
            else it.placement == FeaturePlacement.LAYER_SELECTED || it.placement == FeaturePlacement.DEVELOPER
        placementMatches && (it.layerTypes == null || layer == null || layer.type in it.layerTypes) && it.condition(document, layer)
    }
    fun validate(feature: FeatureSpec, document: ProjectDocument? = null, layer: Layer? = null) {
        require(feature.id.isNotBlank() && feature.title.isNotBlank())
        require(feature.pages.map { it.id }.distinct().size == feature.pages.size) { "Duplicate page in ${feature.id}" }
        feature.pages.forEach { page ->
            require(page.title.isNotBlank())
            require(page.parentId == null || feature.pages.any { it.id == page.parentId }) { "Missing parent page" }
            page.sections.forEach { section -> section.rows.forEach { row ->
                require(row.control.id.isNotBlank() && row.control.label.isNotBlank())
                require(row.control.binding.pathId.isNotBlank())
                if (document != null) require(row.control.binding.validate(document, layer)) { "Invalid binding ${row.control.id}" }
            } }
        }
    }
}
val LocalFeatureRegistry = compositionLocalOf { FeatureRegistry(emptyList()) }
fun ControlSpec.command() = ActionIdentity(verb, target, label)
