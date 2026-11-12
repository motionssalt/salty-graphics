package com.motionssalt.saltygraphics.uikit.registry

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
open class FeatureCatalog(private val requireTypedBindings: Boolean = false) {
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
            require(!requireTypedBindings || page.customPageContent == null) { "Typed pages cannot bypass the scaffold" }
            require(page.readouts.all { it.id.isNotBlank() } && page.readouts.map { it.id }.distinct().size == page.readouts.size) {
                "Invalid readout id: ${page.id}"
            }
            val controls = page.sections.flatMap { it.controls }
            require(controls.map { it.id }.distinct().size == controls.size) { "Duplicate control id: ${page.id}" }
            for (section in page.sections) require(section.id.isNotBlank()) { "Section id is required" }
            for (control in controls) {
                require(control.id.isNotBlank() && control.label.isNotBlank()) { "Control metadata is required" }
                require(!requireTypedBindings || control.binding.path != null) { "Typed property path required: ${control.id}" }
                require(!requireTypedBindings || control.customRenderer == null) { "Register custom components in the library, not feature layout" }
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
                if (control.defaultValue is Float) require(control.defaultValue.isFinite() &&
                    (control.range == null || control.defaultValue in control.range)) { "Default outside range: ${control.id}" }
                if (control.type == BoundControlType.INLINE_DROPDOWN) require(control.options.orEmpty().any { it.value == control.defaultValue })
                if (control.type == BoundControlType.SEGMENTED_CHIPS) require(control.segmentOptions.orEmpty().any { it.value == control.defaultValue })
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

/** All feature pages use property events; a feature never receives or submits a whole snapshot. */
@Composable
fun ScaffoldFeaturePage(
    feature: FeatureDefinition,
    pageId: String,
    document: ProjectDocument,
    layerId: String?,
    onCommit: (FeatureChange<*>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPreview: (FeatureChange<*>?) -> Unit = {}
) {
    val page = feature.pages.firstOrNull { it.id == pageId } ?: return
    var colorControlId by remember(feature.id, pageId, layerId) { mutableStateOf<String?>(null) }
    var numericControlId by remember(feature.id, pageId, layerId) { mutableStateOf<String?>(null) }
    var preview by remember(feature.id, pageId, layerId) { mutableStateOf<FeatureChange<*>?>(null) }
    val previewCallback by rememberUpdatedState(onPreview)
    val commitCallback by rememberUpdatedState(onCommit)
    fun cancel() { if (preview != null) { preview = null; previewCallback(null) } }
    fun commit(change: FeatureChange<*>) { preview = null; commitCallback(change) }
    fun update(change: FeatureChange<*>) { preview = change; previewCallback(change) }
    DisposableEffect(feature.id, pageId, layerId) { onDispose { cancel() } }

    val controls = page.sections.flatMap { it.controls }
    val color = controls.firstOrNull { it.id == colorControlId && it.type == BoundControlType.COLOR_PICKER }
    if (color != null) {
        @Suppress("UNCHECKED_CAST") val bound = color as FeatureControlDef<Long>
        SaltyPanelPage(bound.label, { cancel(); colorControlId = null }, modifier.testTag("scaffold_color_page")) {
            SaltyColorPicker(
                colorArgb = bound.binding.getter(document, layerId),
                onColorChange = { update(bound.change(document, layerId, it)) },
                onColorCommit = { commit(bound.change(document, layerId, it)) },
                onColorCancel = { cancel() }
            )
        }
        return
    }
    val numeric = controls.firstOrNull { it.id == numericControlId }
    SaltyPanelPage(page.title, { cancel(); onBack() }, modifier.testTag("scaffold_feature_page"), footer = {
        if (numeric != null) {
            @Suppress("UNCHECKED_CAST") val bound = numeric as FeatureControlDef<Float>
            SaltyValueEntryStrip(bound.label, bound.binding.getter(document, layerId), bound.range ?: (0f..100f), bound.unit.orEmpty(),
                onConfirm = { numericControlId = null; commit(bound.change(document, layerId, it)) },
                onDismiss = { numericControlId = null })
        }
    }) {
        page.sections.forEach { section ->
            SaltySection(title = section.title) {
                section.controls.forEach { control ->
                    if (control.isVisible?.invoke(document, layerId) != false) {
                        key(feature.id, pageId, layerId, control.id) {
                            val enabled = feature.enabledCondition(document, setOfNotNull(layerId)) &&
                                control.isEnabled(document, layerId) &&
                                (control.binding.path?.exists?.invoke(document, layerId) ?: true)
                            // Consume pointer input above disabled children and expose disabled semantics.
                            val stateModifier = if (enabled) Modifier else Modifier.alpha(.38f)
                                .semantics { disabled() }.pointerInput(Unit) {
                                    awaitPointerEventScope { while (true) {
                                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                    } }
                                }
                            Box(Modifier.testTag("control_${control.id}").then(stateModifier)) {
                                RenderBoundControl(control, document, layerId,
                                    onCommit = { if (enabled) commit(it) },
                                    onPreview = { if (enabled) update(it) },
                                    onCancel = { cancel() },
                                    onOpenColor = { if (enabled) colorControlId = control.id },
                                    onOpenValue = { if (enabled) { cancel(); numericControlId = control.id } })
                            }
                        }
                    }
                }
            }
        }
        page.readouts.forEach { readout ->
            Text(readout.text(document, layerId), style = SaltyTokens.typography.bodySmall,
                color = SaltyTokens.colors.textSecondary, modifier = Modifier.testTag("readout_${readout.id}"))
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
private fun <T> RenderBoundControl(
    control: FeatureControlDef<T>,
    document: ProjectDocument,
    layerId: String?,
    onCommit: (FeatureChange<*>) -> Unit,
    onPreview: (FeatureChange<*>) -> Unit,
    onCancel: () -> Unit,
    onOpenColor: () -> Unit,
    onOpenValue: () -> Unit
) {
    val current = control.binding.getter(document, layerId)
    fun commit(value: T) = onCommit(control.change(document, layerId, value))
    fun preview(value: T) = onPreview(control.change(document, layerId, value))
    when (control.type) {
        BoundControlType.SWITCH -> SaltyRow(label = control.label) {
            SaltySwitch(current as Boolean, { commit(it as T) })
        }
        BoundControlType.RULER_SLIDER, BoundControlType.PROGRESS_SLIDER -> {
            val range = control.range ?: (0f..100f)
            val onChange: (Float) -> Unit = { preview(it as T) }
            val onFinished: (Float) -> Unit = { commit(it as T) }
            Column {
                if (control.type == BoundControlType.RULER_SLIDER) {
                    SaltyRulerSlider(value = current as Float, label = control.label, range = range,
                        step = control.step ?: 1f, unit = control.unit.orEmpty(), defaultValue = control.defaultValue as Float,
                        onValueChange = onChange, onValueCommit = onFinished, onValueCancel = onCancel,
                        onValueClick = onOpenValue)
                } else {
                    SaltyProgressSlider(value = current as Float, label = control.label, range = range,
                        step = control.step ?: 1f, unit = control.unit.orEmpty(), defaultValue = control.defaultValue as Float,
                        onValueChange = onChange, onValueCommit = onFinished, onValueCancel = onCancel,
                        onValueClick = onOpenValue)
                }
            }
        }
        BoundControlType.INLINE_DROPDOWN -> SaltyInlineDropdown(current, control.options.orEmpty(), ::commit, label = control.label)
        BoundControlType.SEGMENTED_CHIPS -> SaltySegmentedChips(current, control.segmentOptions.orEmpty(), ::commit, label = control.label)
        BoundControlType.COLOR_PICKER -> SaltyRow(label = control.label) {
            TextButton(onClick = onOpenColor) {
                Text("#%08X".format(current as Long), color = SaltyTokens.colors.primary)
            }
        }
        BoundControlType.TEXT_FIELD -> SaltyTextField(current as String, { commit(it as T) }, label = control.label)
        BoundControlType.CUSTOM -> control.customRenderer?.invoke(current, ::commit)
    }
}
