package com.motionssalt.saltygraphics.uikit.registry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.controls.DropdownOption
import com.motionssalt.saltygraphics.uikit.controls.SegmentChipItem

/**
 * Example template feature registered into the scaffold as a working template.
 * Demonstrates:
 * - Declarative registration (zero custom layout code in features)
 * - Switch, Ruler slider, Progress slider, Dropdown, Segmented chips, and Color picker
 * - Automatic typed history labels
 */
object ExampleTemplateFeature {

    fun createDefinition(): FeatureDefinition {
        return FeatureDefinition(
            id = "template_example",
            title = "Template Demo",
            icon = Icons.Default.Tune,
            placement = ToolPlacement.LAYER_SELECTED,
            pages = listOf(
                FeaturePageDef(
                    id = "template_main",
                    title = "Template Controls",
                    sections = listOf(
                        FeatureSectionDef(
                            id = "sec_basic",
                            title = "Basic Settings",
                            controls = listOf(
                                FeatureControlDef(
                                    id = "toggle_demo",
                                    label = "Enable Feature",
                                    type = BoundControlType.SWITCH,
                                    defaultValue = true,
                                    binding = PropertyBinding(
                                        getter = { doc, _ -> doc.editorSettings.snapEnabled },
                                        setter = { doc, _, checked ->
                                            doc.copy(editorSettings = doc.editorSettings.copy(snapEnabled = checked)) to
                                                    if (checked) "Enable Snapping" else "Disable Snapping"
                                        }
                                    )
                                ),
                                FeatureControlDef(
                                    id = "ruler_demo",
                                    label = "Grid Size",
                                    type = BoundControlType.RULER_SLIDER,
                                    range = 10f..200f,
                                    step = 10f,
                                    unit = "px",
                                    defaultValue = 50f,
                                    binding = PropertyBinding(
                                        getter = { doc, _ -> doc.editorSettings.gridSize.toFloat() },
                                        setter = { doc, _, newSize ->
                                            val sz = newSize.toInt().coerceIn(10, 200)
                                            doc.copy(editorSettings = doc.editorSettings.copy(gridSize = sz)) to "Change Grid Size"
                                        }
                                    )
                                ),
                                FeatureControlDef(
                                    id = "progress_demo",
                                    label = "Opacity Preview",
                                    type = BoundControlType.PROGRESS_SLIDER,
                                    range = 0f..100f,
                                    step = 1f,
                                    unit = "%",
                                    defaultValue = 100f,
                                    binding = PropertyBinding(
                                        getter = { doc, layerId ->
                                            val layer = layerId?.let { doc.findLayerById(it) }
                                            ((layer?.opacity ?: 1f) * 100f)
                                        },
                                        setter = { doc, layerId, newPct ->
                                            if (layerId == null) return@PropertyBinding doc to "No-op"
                                            doc.updateLayer(layerId) { it.copy(opacity = newPct / 100f) } to "Change Layer Opacity"
                                        }
                                    )
                                )
                            )
                        ),
                        FeatureSectionDef(
                            id = "sec_color",
                            title = "Color & Mode",
                            controls = listOf(
                                FeatureControlDef(
                                    id = "color_demo",
                                    label = "Grid Color",
                                    type = BoundControlType.COLOR_PICKER,
                                    defaultValue = 0x66808080L,
                                    binding = PropertyBinding(
                                        getter = { doc, _ -> doc.editorSettings.gridColor },
                                        setter = { doc, _, newColor ->
                                            doc.copy(editorSettings = doc.editorSettings.copy(gridColor = newColor)) to "Change Grid Color"
                                        }
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}
