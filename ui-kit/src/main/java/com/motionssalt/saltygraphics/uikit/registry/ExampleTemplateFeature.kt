package com.motionssalt.saltygraphics.uikit.registry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import com.motionssalt.saltygraphics.engine.model.ProjectDocument

/**
 * Example template feature registered into the scaffold as a working template.
 * Hidden behind the developer switch in production; demonstrates:
 * - Declarative registration (zero custom layout code in features)
 * - Typed DocumentPropertyPath bindings across Switch, Ruler slider, Progress slider, and Color picker
 * - Automatic verb/target/property history labels
 */
object ExampleTemplateFeature {
    fun createDefinition(
        visibleCondition: (ProjectDocument, Set<String>) -> Boolean = { _, _ -> false }
    ): FeatureDefinition {
        return FeatureDefinition(
            id = "template_example",
            title = "Template Demo",
            icon = Icons.Default.Tune,
            placement = ToolPlacement.LAYER_SELECTED,
            visibleCondition = visibleCondition,
            defaultPageId = "template_main",
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
                                    label = "Enable Snapping",
                                    type = BoundControlType.SWITCH,
                                    defaultValue = true,
                                    binding = PropertyBinding(
                                        DocumentPropertyPath(
                                            key = "template.snapEnabled",
                                            target = "Snapping",
                                            property = "enabled",
                                            read = { doc, _ -> doc.editorSettings.snapEnabled },
                                            write = { live, _, checked ->
                                                live.updateEditorSettings { it.copy(snapEnabled = checked) }
                                            }
                                        )
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
                                        DocumentPropertyPath(
                                            key = "template.gridSize",
                                            target = "Grid",
                                            property = "size",
                                            read = { doc, _ -> doc.editorSettings.gridSize.toFloat() },
                                            write = { live, _, newSize ->
                                                val sz = newSize.toInt().coerceIn(10, 200)
                                                live.updateEditorSettings { it.copy(gridSize = sz) }
                                            }
                                        )
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
                                        DocumentPropertyPath(
                                            key = "template.layerOpacity",
                                            target = "Layer",
                                            property = "opacity",
                                            exists = { doc, layerId -> layerId != null && doc.findLayerById(layerId) != null },
                                            read = { doc, layerId ->
                                                val layer = layerId?.let { doc.findLayerById(it) }
                                                (layer?.opacity ?: 1f) * 100f
                                            },
                                            write = { live, layerId, newPct ->
                                                if (layerId == null) live
                                                else live.updateLayer(layerId) { it.copy(opacity = (newPct / 100f).coerceIn(0f, 1f)) }
                                            }
                                        )
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
                                        DocumentPropertyPath(
                                            key = "template.gridColor",
                                            target = "Grid",
                                            property = "color",
                                            read = { doc, _ -> doc.editorSettings.gridColor },
                                            write = { live, _, newColor ->
                                                live.updateEditorSettings { it.copy(gridColor = newColor) }
                                            }
                                        )
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
