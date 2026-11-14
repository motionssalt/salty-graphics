package com.motionssalt.saltygraphics.uikit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.registry.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryExtensionRegistryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val namePath = DocumentPropertyPath<String>(
        key = "project.name",
        target = "Project",
        property = "Name",
        read = { d, _ -> d.name },
        write = { d, _, v -> d.copy(name = v) }
    )

    private val boolPath = DocumentPropertyPath<Boolean>(
        key = "project.grid",
        target = "Project",
        property = "Grid",
        read = { d, _ -> d.editorSettings.showGrid },
        write = { d, _, v -> d.copy(editorSettings = d.editorSettings.copy(showGrid = v)) }
    )

    @After
    fun cleanup() {
        LibraryComponentRegistry.unregister("test_widget")
        OverlayRegistry.unregister("test_overlay")
    }

    @Test
    fun libraryComponentRegistryAndOverlayRegistryRegisterAndResolveCleanly() {
        LibraryComponentRegistry.register(
            LibraryComponentEntry<String>(
                id = "test_widget",
                description = "Reusable test control"
            ) { label, current, _, _ ->
                Text("$label:$current")
            }
        )
        assertTrue(LibraryComponentRegistry.contains("test_widget"))

        OverlayRegistry.register(
            OverlayEntry(id = "test_overlay", description = "Overlay") { _ ->
                Text("OverlayActive")
            }
        )
        assertTrue(OverlayRegistry.contains("test_overlay"))
        assertNotNull(OverlayRegistry.get("test_overlay"))

        val control = FeatureControlDef(
            id = "mode",
            label = "Mode",
            type = BoundControlType.LIBRARY_COMPONENT,
            componentId = "test_widget",
            binding = PropertyBinding(namePath),
            defaultValue = "Initial"
        )

        val catalog = FeatureCatalog(requireTypedBindings = true)
        catalog.register(
            FeatureDefinition(
                id = "ext_feature",
                title = "Extension",
                icon = Icons.Default.Tune,
                placement = ToolPlacement.ALWAYS,
                pages = listOf(
                    FeaturePageDef(
                        id = "main",
                        title = "Extension",
                        sections = listOf(FeatureSectionDef("s1", controls = listOf(control)))
                    )
                )
            )
        )

        composeRule.setContent {
            RenderBoundControl(
                control = control,
                document = ProjectDocument(name = "Alpha"),
                layerId = null,
                onCommit = {},
                onPreview = {},
                onCancel = {},
                onOpenColor = {},
                onOpenValue = {}
            )
        }
        composeRule.onNodeWithText("Mode:Alpha").assertIsDisplayed()
    }

    @Test
    fun strictCatalogRejectsMissingComponentIdOrCustomControl() {
        val strict = FeatureCatalog(requireTypedBindings = true)
        val missingId = FeatureControlDef(
            id = "missing",
            label = "Missing",
            type = BoundControlType.LIBRARY_COMPONENT,
            componentId = "does_not_exist",
            binding = PropertyBinding(boolPath),
            defaultValue = false
        )
        try {
            strict.register(
                FeatureDefinition(
                    id = "bad1",
                    title = "Bad",
                    icon = Icons.Default.Tune,
                    placement = ToolPlacement.ALWAYS,
                    pages = listOf(FeaturePageDef("p", "P", listOf(FeatureSectionDef("s", controls = listOf(missingId)))))
                )
            )
            fail("Strict catalog must reject unregistered componentId")
        } catch (_: IllegalArgumentException) {
        }

        val inlineCustom = FeatureControlDef(
            id = "custom",
            label = "Custom",
            type = BoundControlType.CUSTOM,
            binding = PropertyBinding(boolPath),
            defaultValue = false,
            customRenderer = { _, _ -> }
        )
        try {
            strict.register(
                FeatureDefinition(
                    id = "bad2",
                    title = "Bad2",
                    icon = Icons.Default.Tune,
                    placement = ToolPlacement.ALWAYS,
                    pages = listOf(FeaturePageDef("p", "P", listOf(FeatureSectionDef("s", controls = listOf(inlineCustom)))))
                )
            )
            fail("Strict catalog must reject inline CUSTOM controls")
        } catch (_: IllegalArgumentException) {
        }
    }
}
