package com.motionssalt.saltygraphics.uikit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.EditorSettings
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.controls.DropdownOption
import com.motionssalt.saltygraphics.uikit.controls.SegmentChipItem
import com.motionssalt.saltygraphics.uikit.registry.*
import org.junit.Assert.*
import org.junit.Test

class FeatureRegistryTest {

    @Test
    fun testRegisterAndQueryThrowawayFeatureWithAllControlTypes() {
        val testFeature = FeatureDefinition(
            id = "throwaway_test_feature",
            title = "Test Throwaway",
            icon = Icons.Default.Star,
            placement = ToolPlacement.ALWAYS,
            pages = listOf(
                FeaturePageDef(
                    id = "page_all_controls",
                    title = "All Controls Page",
                    sections = listOf(
                        FeatureSectionDef(
                            id = "sec_1",
                            title = "Section 1",
                            controls = listOf(
                                FeatureControlDef(
                                    id = "c_switch",
                                    label = "Switch Control",
                                    type = BoundControlType.SWITCH,
                                    defaultValue = false,
                                    binding = PropertyBinding(
                                        getter = { doc, _ -> doc.editorSettings.snapEnabled },
                                        setter = { doc, _, v ->
                                            doc.copy(editorSettings = doc.editorSettings.copy(snapEnabled = v)) to "Toggle Snap"
                                        }
                                    )
                                ),
                                FeatureControlDef(
                                    id = "c_ruler",
                                    label = "Ruler Control",
                                    type = BoundControlType.RULER_SLIDER,
                                    range = 0f..100f,
                                    step = 1f,
                                    defaultValue = 20f,
                                    binding = PropertyBinding(
                                        getter = { doc, _ -> doc.editorSettings.gridSize.toFloat() },
                                        setter = { doc, _, v ->
                                            doc.copy(editorSettings = doc.editorSettings.copy(gridSize = v.toInt())) to "Change Grid Size"
                                        }
                                    )
                                ),
                                FeatureControlDef(
                                    id = "c_progress",
                                    label = "Progress Control",
                                    type = BoundControlType.PROGRESS_SLIDER,
                                    range = 0f..100f,
                                    defaultValue = 50f,
                                    binding = PropertyBinding(
                                        getter = { _, _ -> 50f },
                                        setter = { doc, _, _ -> doc to "Update Progress" }
                                    )
                                ),
                                FeatureControlDef(
                                    id = "c_dropdown",
                                    label = "Dropdown Control",
                                    type = BoundControlType.INLINE_DROPDOWN,
                                    options = listOf(
                                        DropdownOption("A", "Option A"),
                                        DropdownOption("B", "Option B")
                                    ),
                                    defaultValue = "A",
                                    binding = PropertyBinding(
                                        getter = { _, _ -> "A" },
                                        setter = { doc, _, sel -> doc to "Select Dropdown $sel" }
                                    )
                                ),
                                FeatureControlDef(
                                    id = "c_chips",
                                    label = "Segmented Chips",
                                    type = BoundControlType.SEGMENTED_CHIPS,
                                    segmentOptions = listOf(
                                        SegmentChipItem(1, "One"),
                                        SegmentChipItem(2, "Two")
                                    ),
                                    defaultValue = 1,
                                    binding = PropertyBinding(
                                        getter = { _, _ -> 1 },
                                        setter = { doc, _, v -> doc to "Select Chip $v" }
                                    )
                                ),
                                FeatureControlDef(
                                    id = "c_color",
                                    label = "Color Control",
                                    type = BoundControlType.COLOR_PICKER,
                                    defaultValue = 0xFF00FF00L,
                                    binding = PropertyBinding(
                                        getter = { doc, _ -> doc.editorSettings.gridColor },
                                        setter = { doc, _, c ->
                                            doc.copy(editorSettings = doc.editorSettings.copy(gridColor = c)) to "Set Color"
                                        }
                                    )
                                ),
                                FeatureControlDef(
                                    id = "c_text",
                                    label = "Text Field Control",
                                    type = BoundControlType.TEXT_FIELD,
                                    defaultValue = "Hello",
                                    binding = PropertyBinding(
                                        getter = { doc, _ -> doc.name },
                                        setter = { doc, _, name -> doc.copy(name = name) to "Rename Document" }
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        // Register feature
        FeatureRegistry.register(testFeature)
        val retrieved = FeatureRegistry.getFeature("throwaway_test_feature")
        assertNotNull("Registered feature must be retrievable", retrieved)
        assertEquals("Test Throwaway", retrieved?.title)

        val doc = ProjectDocument(
            id = "test_doc",
            name = "Test Doc",
            canvasSize = CanvasSize(1080, 1080),
            editorSettings = EditorSettings(snapEnabled = false, gridSize = 20, gridColor = 0xFFFFFFFFL)
        )

        val tools = FeatureRegistry.queryTools(doc, emptySet())
        assertTrue("Query tools must include ALWAYS placement feature", tools.any { it.id == "throwaway_test_feature" })

        // Test bindings execute and return correct documents and history labels
        val page = testFeature.pages.first()
        val sec = page.sections.first()

        @Suppress("UNCHECKED_CAST")
        val switchCtrl = sec.controls.first { it.id == "c_switch" } as FeatureControlDef<Boolean>
        val (docAfterSwitch, switchLabel) = switchCtrl.binding.setter(doc, null, true)
        assertTrue(docAfterSwitch.editorSettings.snapEnabled)
        assertEquals("Toggle Snap", switchLabel)

        @Suppress("UNCHECKED_CAST")
        val rulerCtrl = sec.controls.first { it.id == "c_ruler" } as FeatureControlDef<Float>
        val (docAfterRuler, rulerLabel) = rulerCtrl.binding.setter(doc, null, 75f)
        assertEquals(75, docAfterRuler.editorSettings.gridSize)
        assertEquals("Change Grid Size", rulerLabel)

        @Suppress("UNCHECKED_CAST")
        val textCtrl = sec.controls.first { it.id == "c_text" } as FeatureControlDef<String>
        val (docAfterRename, renameLabel) = textCtrl.binding.setter(doc, null, "Renamed Doc")
        assertEquals("Renamed Doc", docAfterRename.name)
        assertEquals("Rename Document", renameLabel)

        FeatureRegistry.unregister("throwaway_test_feature")
    }

    @Test
    fun testExampleTemplateFeatureIsValid() {
        val def = ExampleTemplateFeature.createDefinition()
        assertNotNull("Template ID must be non-null", def.id)
        assertTrue("Template ID must not be blank", def.id.isNotBlank())
        assertTrue("Template Title must not be blank", def.title.isNotBlank())
        assertNotNull("Template Icon must not be null", def.icon)
        assertTrue("Template must have at least one page", def.pages.isNotEmpty())

        val doc = ProjectDocument(
            id = "test_doc",
            name = "Doc",
            canvasSize = CanvasSize(1000, 1000)
        )
        def.pages.forEach { page ->
            assertTrue("Page id must not be blank", page.id.isNotBlank())
            assertTrue("Page title must not be blank", page.title.isNotBlank())
            page.sections.forEach { sec ->
                assertTrue("Section id must not be blank", sec.id.isNotBlank())
                sec.controls.forEach { ctrl ->
                    assertTrue("Control id must not be blank", ctrl.id.isNotBlank())
                    assertTrue("Control label must not be blank", ctrl.label.isNotBlank())
                    assertNotNull("Control default value must not be null", ctrl.defaultValue)
                }
            }
        }
    }
}
