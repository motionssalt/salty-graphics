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
object FeatureRegistry {
    private val features = mutableMapOf<String, FeatureDefinition>()

    fun register(feature: FeatureDefinition) {
        features[feature.id] = feature
    }

    fun unregister(featureId: String) {
        features.remove(featureId)
    }

    fun getAllFeatures(): List<FeatureDefinition> = features.values.toList()

    fun getFeature(id: String): FeatureDefinition? = features[id]

    /**
     * Queries features applicable for the current tool grid (no-selection or layer-selected).
     */
    fun queryTools(
        document: ProjectDocument,
        selectedLayerIds: Set<String>
    ): List<FeatureDefinition> {
        val hasSelection = selectedLayerIds.isNotEmpty()
        val targetLayer = if (hasSelection) document.findLayerById(selectedLayerIds.first()) else null

        return features.values.filter { feat ->
            val placementMatches = when (feat.placement) {
                ToolPlacement.ALWAYS -> true
                ToolPlacement.NO_SELECTION -> !hasSelection
                ToolPlacement.LAYER_SELECTED -> hasSelection
            }
            val layerTypeMatches = if (feat.supportedLayerTypes.isEmpty() || targetLayer == null) {
                true
            } else {
                targetLayer.type in feat.supportedLayerTypes
            }
            placementMatches && layerTypeMatches && feat.visibleCondition(document, selectedLayerIds)
        }
    }
}

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
