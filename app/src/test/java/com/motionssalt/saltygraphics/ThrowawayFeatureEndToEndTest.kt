package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import com.motionssalt.saltygraphics.theme.SaltyGraphicsTheme
import com.motionssalt.saltygraphics.uikit.controls.DropdownOption
import com.motionssalt.saltygraphics.uikit.controls.SegmentChipItem
import com.motionssalt.saltygraphics.uikit.registry.*
import com.motionssalt.saltygraphics.uikit.structure.SaltyButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyRow
import com.motionssalt.saltygraphics.uikit.structure.ScaffoldToolPanel
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-end throwaway feature registration proof:
 * Registers a complete feature page purely as data against a strict FeatureCatalog(requireTypedBindings = true),
 * exercises Switch, RulerSlider, ProgressSlider, SegmentedChips, InlineDropdown, TextField, ColorPicker,
 * and a registered LibraryComponent without touching core scaffold code, and verifies labelled ActionTree history.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
class ThrowawayFeatureEndToEndTest {
    @get:Rule val compose = createEmptyComposeRule()

    @After fun cleanup() {
        LibraryComponentRegistry.unregister("throwaway_stepper")
    }

    @Test fun throwawayFeatureRegistersAllControlTypesAndDrivesLabelledHistory() {
        LibraryComponentRegistry.register(
            LibraryComponentEntry<Float>(
                id = "throwaway_stepper",
                description = "Library component incrementing a float step"
            ) { label, current, _, onCommit ->
                SaltyRow(label = label) {
                    SaltyButton(text = "Bump+10 (${current.toInt()})", onClick = { onCommit(current + 10f) })
                }
            }
        )

        val showGridPath = DocumentPropertyPath<Boolean>(
            key = "throwaway.showGrid", target = "Grid", property = "Visibility",
            read = { d, _ -> d.editorSettings.showGrid },
            write = { d, _, v -> d.updateEditorSettings { it.copy(showGrid = v) } }
        )
        val gridSizePath = DocumentPropertyPath<Float>(
            key = "throwaway.gridSize", target = "Grid", property = "Size",
            read = { d, _ -> d.editorSettings.gridSize.toFloat() },
            write = { d, _, v -> d.updateEditorSettings { it.copy(gridSize = v.toInt()) } }
        )
        val zoomPath = DocumentPropertyPath<Float>(
            key = "throwaway.zoom", target = "Viewport", property = "Zoom",
            read = { d, _ -> d.editorSettings.defaultZoom },
            write = { d, _, v -> d.updateEditorSettings { it.copy(defaultZoom = v) } }
        )
        val qualityPath = DocumentPropertyPath<PreviewQuality>(
            key = "throwaway.quality", target = "Preview", property = "Quality",
            read = { d, _ -> d.editorSettings.defaultPreviewQuality },
            write = { d, _, v -> d.updateEditorSettings { it.copy(defaultPreviewQuality = v) } }
        )
        val squarePath = DocumentPropertyPath<Boolean>(
            key = "throwaway.square", target = "Grid", property = "Square",
            read = { d, _ -> d.editorSettings.isSquareGrid },
            write = { d, _, v -> d.updateEditorSettings { it.copy(isSquareGrid = v) } }
        )
        val namePath = DocumentPropertyPath<String>(
            key = "throwaway.name", target = "Project", property = "Title",
            read = { d, _ -> d.name },
            write = { d, _, v -> d.copy(name = v) }
        )
        val colorPath = DocumentPropertyPath<Long>(
            key = "throwaway.bg", target = "Canvas", property = "Background",
            read = { d, _ -> d.editorSettings.backgroundColor },
            write = { d, _, v -> d.updateEditorSettings { it.copy(backgroundColor = v) } }
        )

        val definition = FeatureDefinition(
            id = "throwaway_e2e",
            title = "Throwaway E2E",
            icon = Icons.Default.Build,
            placement = ToolPlacement.ALWAYS,
            defaultPageId = "main",
            pages = listOf(
                FeaturePageDef(
                    id = "main",
                    title = "Throwaway Controls",
                    sections = listOf(
                        FeatureSectionDef(
                            id = "sec_all",
                            title = "All Controls",
                            controls = listOf(
                                FeatureControlDef("sw", "Grid Visible", BoundControlType.SWITCH, PropertyBinding(showGridPath), defaultValue = false),
                                FeatureControlDef("r1", "Grid Spacing", BoundControlType.RULER_SLIDER, PropertyBinding(gridSizePath), range = 4f..200f, step = 1f, unit = "px", defaultValue = 20f),
                                FeatureControlDef("p1", "Default Zoom", BoundControlType.PROGRESS_SLIDER, PropertyBinding(zoomPath), range = 0.25f..4f, step = 0.25f, unit = "x", defaultValue = 1f),
                                FeatureControlDef("c1", "Grid Type", BoundControlType.SEGMENTED_CHIPS, PropertyBinding(squarePath),
                                    segmentOptions = listOf(SegmentChipItem(false, "Auto"), SegmentChipItem(true, "Square")), defaultValue = false),
                                FeatureControlDef("d1", "Preview Quality", BoundControlType.INLINE_DROPDOWN, PropertyBinding(qualityPath),
                                    options = PreviewQuality.primaryLevels.map { DropdownOption(it, it.displayName) }, defaultValue = PreviewQuality.FULL),
                                FeatureControlDef("t1", "Document Name", BoundControlType.TEXT_FIELD, PropertyBinding(namePath), defaultValue = "Untitled"),
                                FeatureControlDef("col1", "Canvas Tint", BoundControlType.COLOR_PICKER, PropertyBinding(colorPath), defaultValue = 0xFF808080L),
                                FeatureControlDef("lib1", "Library Stepper", BoundControlType.LIBRARY_COMPONENT, PropertyBinding(gridSizePath), componentId = "throwaway_stepper", defaultValue = 20f)
                            )
                        )
                    ),
                    readouts = listOf(
                        FeatureReadoutDef("summary") { d, _ -> "Size=${d.editorSettings.gridSize} Quality=${d.editorSettings.defaultPreviewQuality.displayName}" }
                    )
                )
            )
        )

        val strictCatalog = FeatureCatalog(requireTypedBindings = true)
        strictCatalog.register(definition)

        val tree = ActionTree(ProjectDocument(id = "doc-e2e", name = "Initial"))
        var docState by mutableStateOf(tree.currentDocument)
        var openPage by mutableStateOf(false)

        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
                if (!openPage) {
                    ScaffoldToolPanel(
                        catalog = strictCatalog,
                        document = docState,
                        selectedLayerIds = emptySet(),
                        implementedTools = setOf("throwaway_e2e"),
                        onFeatureClick = { openPage = true }
                    )
                } else {
                    ScaffoldFeaturePage(
                        feature = definition,
                        pageId = "main",
                        document = docState,
                        layerId = null,
                        onCommit = { change ->
                            val live = tree.currentDocument
                            if (change.appliesTo(live)) {
                                tree.recordCommand(change.identity(live), change::apply)
                                docState = tree.currentDocument
                            }
                        },
                        onBack = { openPage = false }
                    )
                }
            }
        }
        try {
            // 1) Registered tool tile appears in ScaffoldToolPanel and opens ScaffoldFeaturePage
            compose.onNodeWithText("Throwaway E2E").assertIsDisplayed().performClick()
            compose.onNodeWithTag("scaffold_feature_page").assertIsDisplayed()
            compose.onNodeWithTag("readout_summary").assertTextEquals("Size=20 Quality=Full")

            // 2) Drive segmented chips, library component, dropdown, and color picker page
            compose.onNodeWithText("Square").performClick()
            compose.onNodeWithText("Bump+10 (20)").performClick()
            compose.onNodeWithText("Full").performClick()
            compose.onNodeWithText("1/2").performClick()

            compose.runOnIdle {
                assertTrue(docState.editorSettings.isSquareGrid)
                assertEquals(30, docState.editorSettings.gridSize)
                assertEquals(PreviewQuality.HALF, docState.editorSettings.defaultPreviewQuality)
            }
            compose.onNodeWithTag("readout_summary").assertTextEquals("Size=30 Quality=1/2")

            // Verify color picker panel page works inline inside scaffold without popups
            compose.onNodeWithText("#FF808080").performClick()
            compose.onNodeWithTag("scaffold_color_page").assertIsDisplayed()
            compose.onNodeWithTag("transparent_color").performClick()
            compose.onNodeWithContentDescription("Back").performClick()

            compose.runOnIdle {
                assertEquals(0x00808080L, docState.editorSettings.backgroundColor)
                // Undo restores step-by-step with labelled history identities
                assertTrue(tree.canUndo)
                assertTrue(tree.undo())
                assertEquals(0xFF808080L, tree.currentDocument.editorSettings.backgroundColor)
                assertTrue(tree.undo())
                assertEquals(PreviewQuality.FULL, tree.currentDocument.editorSettings.defaultPreviewQuality)
                assertTrue(tree.undo())
                assertEquals(20, tree.currentDocument.editorSettings.gridSize)
            }
        } finally {
            host.pause().stop().destroy()
        }
    }
}
