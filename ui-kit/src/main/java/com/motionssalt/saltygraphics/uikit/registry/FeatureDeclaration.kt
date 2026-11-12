package com.motionssalt.saltygraphics.uikit.registry

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ProjectDocument

enum class ToolPlacement {
    NO_SELECTION,
    LAYER_SELECTED,
    ALWAYS
}

enum class BoundControlType {
    SWITCH,
    RULER_SLIDER,
    PROGRESS_SLIDER,
    INLINE_DROPDOWN,
    COLOR_PICKER,
    SEGMENTED_CHIPS,
    TEXT_FIELD,
    CUSTOM
}

data class PropertyBinding<T>(
    val getter: (ProjectDocument, String?) -> T,
    val setter: (ProjectDocument, String?, T) -> Pair<ProjectDocument, String>,
    val path: DocumentPropertyPath<T>? = null
) {
    constructor(path: DocumentPropertyPath<T>) : this(
        getter = path.read,
        setter = { live, layerId, value ->
            if (path.exists(live, layerId)) path.write(live, layerId, value) to path.identity(live, layerId, value).toLabel()
            else live to "No change"
        },
        path = path
    )
}

data class FeatureControlDef<T>(
    val id: String,
    val label: String,
    val type: BoundControlType,
    val binding: PropertyBinding<T>,
    val range: ClosedFloatingPointRange<Float>? = null,
    val step: Float? = null,
    val unit: String? = null,
    val options: List<com.motionssalt.saltygraphics.uikit.controls.DropdownOption<T>>? = null,
    val segmentOptions: List<com.motionssalt.saltygraphics.uikit.controls.SegmentChipItem<T>>? = null,
    val defaultValue: T,
    val isVisible: ((ProjectDocument, String?) -> Boolean)? = null,
    val customRenderer: (@Composable (current: T, onUpdate: (T) -> Unit) -> Unit)? = null,
    val isEnabled: (ProjectDocument, String?) -> Boolean = { _, _ -> true }
)

data class FeatureSectionDef(
    val id: String,
    val title: String? = null,
    val controls: List<FeatureControlDef<*>>
)

data class FeatureReadoutDef(val id: String, val text: (ProjectDocument, String?) -> String)

data class FeaturePageDef(
    val id: String,
    val title: String,
    val sections: List<FeatureSectionDef>,
    val customPageContent: (@Composable (doc: ProjectDocument, layerId: String?, onDocUpdate: (ProjectDocument, String) -> Unit, onBack: () -> Unit) -> Unit)? = null,
    val readouts: List<FeatureReadoutDef> = emptyList()
)

data class FeatureDefinition(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val placement: ToolPlacement,
    val supportedLayerTypes: Set<LayerType> = emptySet(), // empty = any
    val visibleCondition: (ProjectDocument, Set<String>) -> Boolean = { _, _ -> true },
    val enabledCondition: (ProjectDocument, Set<String>) -> Boolean = { _, _ -> true },
    val defaultPageId: String? = null,
    val pages: List<FeaturePageDef> = emptyList(),
    val onDirectAction: ((ProjectDocument, Set<String>) -> Pair<ProjectDocument, String>)? = null
)
