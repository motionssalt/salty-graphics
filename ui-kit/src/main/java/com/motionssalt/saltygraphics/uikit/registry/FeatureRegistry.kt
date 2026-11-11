package com.motionssalt.saltygraphics.uikit.registry

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.controls.*
import com.motionssalt.saltygraphics.uikit.structure.*
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

/**
 * The Central Scaffold Feature Registry.
 * Features register their definitions declaratively here.
 * The scaffold automatically renders them in the bottom panel and wires
 * control changes to the history system with structured action labels.
 */
open class FeatureCatalog {
    // Snapshot-backed and ordered: runtime registration must appear immediately,
    // without losing the declaration order when metadata is replaced.
    private val features = mutableStateListOf<FeatureDefinition>()

    fun register(feature: FeatureDefinition) {
        require(feature.id.isNotBlank()) { "Feature id is required" }
        require(feature.title.isNotBlank()) { "Feature title is required: ${feature.id}" }
        require(feature.pages.map { it.id }.distinct().size == feature.pages.size) { "Duplicate page id: ${feature.id}" }
        require(feature.defaultPageId == null || feature.pages.any { it.id == feature.defaultPageId }) {
            "Default page does not exist: ${feature.id}"
        }
        for (page in feature.pages) {
            require(page.id.isNotBlank() && page.title.isNotBlank()) { "Page metadata is required: ${feature.id}" }
            require(page.sections.map { it.id }.distinct().size == page.sections.size) { "Duplicate section id: ${page.id}" }
            val controls = page.sections.flatMap { it.controls }
            require(controls.map { it.id }.distinct().size == controls.size) { "Duplicate control id: ${page.id}" }
            for (section in page.sections) require(section.id.isNotBlank()) { "Section id is required" }
            for (control in controls) {
                require(control.id.isNotBlank() && control.label.isNotBlank()) { "Control metadata is required" }
                require(control.defaultValue != null) { "Default value is required: ${control.id}" }
                control.range?.let { range ->
                    require(range.start.isFinite() && range.endInclusive.isFinite() && range.start <= range.endInclusive) {
                        "Invalid range: ${control.id}"
                    }
                }
                control.step?.let { require(it.isFinite() && it > 0f) { "Invalid step: ${control.id}" } }
                val validType = when (control.type) {
                    BoundControlType.SWITCH -> control.defaultValue is Boolean
                    BoundControlType.RULER_SLIDER, BoundControlType.PROGRESS_SLIDER -> control.defaultValue is Float
                    BoundControlType.COLOR_PICKER -> control.defaultValue is Long
                    BoundControlType.TEXT_FIELD -> control.defaultValue is String
                    BoundControlType.INLINE_DROPDOWN -> !control.options.isNullOrEmpty()
                    BoundControlType.SEGMENTED_CHIPS -> !control.segmentOptions.isNullOrEmpty()
                    BoundControlType.CUSTOM -> control.customRenderer != null
                }
                require(validType) { "Default/options do not match control type: ${control.id}" }
            }
        }
        val existing = features.indexOfFirst { it.id == feature.id }
        if (existing < 0) features.add(feature) else features[existing] = feature
    }

    fun unregister(featureId: String) {
        features.removeAll { it.id == featureId }
    }

    fun getAllFeatures(): List<FeatureDefinition> = features.toList()

    fun getFeature(id: String): FeatureDefinition? = features.firstOrNull { it.id == id }

    /**
     * Queries features applicable for the current tool grid (no-selection or layer-selected).
     */
    fun queryTools(
        document: ProjectDocument,
        selectedLayerIds: Set<String>
    ): List<FeatureDefinition> {
        val selected = selectedLayerIds.mapNotNull(document::findLayerById)
        val hasSelection = selected.isNotEmpty()
        val validIds = selected.map { it.id }.toSet()

        return features.filter { feat ->
            val placementMatches = when (feat.placement) {
                ToolPlacement.ALWAYS -> true
                ToolPlacement.NO_SELECTION -> !hasSelection
                ToolPlacement.LAYER_SELECTED -> hasSelection
            }
            val layerTypeMatches = if (feat.supportedLayerTypes.isEmpty()) {
                true
            } else {
                selected.isNotEmpty() && selected.all { it.type in feat.supportedLayerTypes }
            }
            placementMatches && layerTypeMatches && feat.visibleCondition(document, validIds)
        }
    }
}

object FeatureRegistry : FeatureCatalog()

/**
 * Declarative Bottom Panel renderer powered by FeatureRegistry.
 * Generates panel pages and control bindings automatically using shared components.
 */
@Composable
fun ScaffoldFeaturePage(
    feature: FeatureDefinition,
    pageId: String,
    document: ProjectDocument,
    layerId: String?,
    onDocUpdate: (ProjectDocument, historyLabel: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pageDef = feature.pages.firstOrNull { it.id == pageId }
        ?: feature.pages.firstOrNull()
        ?: return

    if (pageDef.customPageContent != null) {
        pageDef.customPageContent.invoke(document, layerId, onDocUpdate, onBack)
        return
    }

    SaltyPanelPage(
        title = pageDef.title,
        onBack = onBack,
        modifier = modifier
    ) {
        pageDef.sections.forEach { sec ->
            SaltySection(
                title = sec.title
            ) {
                sec.controls.forEach { ctrl ->
                    val isVis = ctrl.isVisible?.invoke(document, layerId) ?: true
                    if (isVis) {
                        RenderBoundControl(
                            control = ctrl,
                            document = document,
                            layerId = layerId,
                            onUpdate = { updated, label -> onDocUpdate(updated, label) }
                        )
                    }
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
private fun <T> RenderBoundControl(
    control: FeatureControlDef<T>,
    document: ProjectDocument,
    layerId: String?,
    onUpdate: (ProjectDocument, String) -> Unit
) {
    val currentVal = control.binding.getter(document, layerId)

    if (control.customRenderer != null) {
        control.customRenderer.invoke(currentVal) { newVal ->
            val (updated, label) = control.binding.setter(document, layerId, newVal)
            onUpdate(updated, label)
        }
        return
    }

    when (control.type) {
        BoundControlType.SWITCH -> {
            val boolVal = currentVal as? Boolean ?: false
            SaltyRow(label = control.label) {
                SaltySwitch(
                    checked = boolVal,
                    onCheckedChange = { checked ->
                        val (updated, label) = (control.binding as PropertyBinding<Boolean>).setter(document, layerId, checked)
                        onUpdate(updated, label)
                    }
                )
            }
        }
        BoundControlType.RULER_SLIDER -> {
            val floatVal = (currentVal as? Number)?.toFloat() ?: 0f
            val range = control.range ?: (-1000f..1000f)
            val step = control.step ?: 1f
            val unit = control.unit ?: ""

            SaltyRulerSlider(
                value = floatVal,
                label = control.label,
                range = range,
                step = step,
                unit = unit,
                onValueChange = { newVal ->
                    val (updated, label) = (control.binding as PropertyBinding<Float>).setter(document, layerId, newVal)
                    onUpdate(updated, label)
                }
            )
        }
        BoundControlType.PROGRESS_SLIDER -> {
            val floatVal = (currentVal as? Number)?.toFloat() ?: 0f
            val range = control.range ?: (0f..100f)
            val step = control.step ?: 1f
            val unit = control.unit ?: ""

            SaltyProgressSlider(
                value = floatVal,
                label = control.label,
                range = range,
                step = step,
                unit = unit,
                onValueChange = { newVal ->
                    val (updated, label) = (control.binding as PropertyBinding<Float>).setter(document, layerId, newVal)
                    onUpdate(updated, label)
                }
            )
        }
        BoundControlType.INLINE_DROPDOWN -> {
            val opts = control.options ?: emptyList()
            SaltyInlineDropdown(
                selected = currentVal,
                options = opts,
                label = control.label,
                onSelect = { selectedVal ->
                    val (updated, label) = control.binding.setter(document, layerId, selectedVal)
                    onUpdate(updated, label)
                }
            )
        }
        BoundControlType.SEGMENTED_CHIPS -> {
            val opts = control.segmentOptions ?: emptyList()
            SaltySegmentedChips(
                selected = currentVal,
                options = opts,
                label = control.label,
                onSelect = { selectedVal ->
                    val (updated, label) = control.binding.setter(document, layerId, selectedVal)
                    onUpdate(updated, label)
                }
            )
        }
        BoundControlType.COLOR_PICKER -> {
            val colorLong = (currentVal as? Long) ?: ((currentVal as? Number)?.toLong() ?: 0xFFFFFFFFL)
            SaltyColorPicker(
                colorArgb = colorLong,
                onColorChange = { newArgb ->
                    val (updated, label) = (control.binding as PropertyBinding<Long>).setter(document, layerId, newArgb)
                    onUpdate(updated, label)
                }
            )
        }
        BoundControlType.TEXT_FIELD -> {
            val strVal = currentVal as? String ?: ""
            SaltyTextField(
                value = strVal,
                label = control.label,
                onValueChange = { newStr ->
                    val (updated, label) = (control.binding as PropertyBinding<String>).setter(document, layerId, newStr)
                    onUpdate(updated, label)
                }
            )
        }
        BoundControlType.CUSTOM -> {}
    }
}
