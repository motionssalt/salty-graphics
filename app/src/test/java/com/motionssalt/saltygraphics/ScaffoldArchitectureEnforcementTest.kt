package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.uikit.registry.ExampleTemplateFeature
import com.motionssalt.saltygraphics.uikit.registry.FeatureRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Test

/**
 * Enforces the internal UI scaffold architecture rules across feature modules:
 * 1. Features may not use raw Material Switch or Slider components directly (must use SaltySwitch, SaltyRulerSlider, SaltyProgressSlider).
 * 2. Editing flow code strictly forbids pop-ups, dialogs, dropdown menus, and modal sheets (standing UI rule).
 * 3. Every registered feature in FeatureRegistry must have a title, an icon, and valid bindings.
 */
class ScaffoldArchitectureEnforcementTest {

    @Test
    fun featureCodeContainsNoRawMaterialSwitchesOrSliders() {
        val root = File("src/main/java/com/motionssalt/saltygraphics").takeIf { it.exists() }
            ?: File("app/src/main/java/com/motionssalt/saltygraphics")
        assertTrue("Source root must exist: ${root.absolutePath}", root.exists())

        // Raw Material 3 Switch and Slider calls to detect
        val rawSwitchRegex = Regex("(?<!Salty)\\bSwitch\\s*\\(")
        val rawSliderRegex = Regex("(?<!Ruler|Progress)\\bSlider\\s*\\(")

        val scannedFiles = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.path.contains("ui/controls") } // UI controls library itself implements the shared primitives
            .toList()

        for (file in scannedFiles) {
            val text = file.readText()
            val switchMatch = rawSwitchRegex.find(text)
            assertNull(
                "Raw Material Switch used in feature file ${file.name}. Must use SaltySwitch from UI kit.",
                switchMatch
            )

            val sliderMatch = rawSliderRegex.find(text)
            assertNull(
                "Raw Material Slider used in feature file ${file.name}. Must use SaltyRulerSlider or SaltyProgressSlider from UI kit.",
                sliderMatch
            )
        }
    }

    @Test
    fun editingFlowContainsNoForbiddenPopupComponents() {
        val root = File("src/main/java/com/motionssalt/saltygraphics").takeIf { it.exists() }
            ?: File("app/src/main/java/com/motionssalt/saltygraphics")
        assertTrue("Source root must exist", root.exists())

        val forbiddenRegex = Regex("\\b(ModalBottomSheet|Popup|DropdownMenu|DropdownMenuItem)\\s*\\(")
        val dialogRegex = Regex("\\b(Dialog|AlertDialog)\\s*\\(")

        val editingFlowFiles = listOf(
            "ui/controls/FillPanelPage.kt",
            "ui/controls/BorderShadowPanelPage.kt",
            "ui/controls/MiscControls.kt",
            "ui/controls/ColorPickerComponents.kt",
            "ui/editor/BottomPanel.kt",
            "ui/editor/BlendingMaskDialog.kt",
            "ui/editor/ShapeSettingsDialog.kt",
            "ui/editor/ShapePickerDialog.kt",
            "ui/editor/TextSettingsDialog.kt",
            "ui/editor/TopPanel.kt",
            "ui/editor/TransformPanel.kt",
            "ui/editor/EffectSettingsSheet.kt",
            "ui/editor/VectorPage.kt",
            "ui/editor/ProjectSettingsPage.kt"
        )

        for (relPath in editingFlowFiles) {
            val file = File(root, relPath)
            if (!file.exists()) continue
            val text = file.readText()

            val match = forbiddenRegex.find(text)
            assertNull("Forbidden overlay component found in $relPath: ${match?.value}", match)

            val dialogMatch = dialogRegex.find(text)
            assertNull("Dialog/AlertDialog must not be used in editing flow file $relPath: ${dialogMatch?.value}", dialogMatch)
        }
    }

    @Test
    fun featureRegistryDefinitionsAreValid() {
        // Register the template feature and verify all registered features
        FeatureRegistry.register(ExampleTemplateFeature.createDefinition())

        val all = FeatureRegistry.getAllFeatures()
        assertTrue("Feature registry must contain registered features", all.isNotEmpty())

        for (feat in all) {
            assertTrue("Feature id must not be blank: ${feat.id}", feat.id.isNotBlank())
            assertTrue("Feature title must not be blank: ${feat.title}", feat.title.isNotBlank())
            assertNotNull("Feature icon must not be null for ${feat.id}", feat.icon)

            for (page in feat.pages) {
                assertTrue("Page id must not be blank in feature ${feat.id}", page.id.isNotBlank())
                assertTrue("Page title must not be blank in feature ${feat.id}", page.title.isNotBlank())
                for (section in page.sections) {
                    assertTrue("Section id must not be blank in feature ${feat.id}", section.id.isNotBlank())
                    for (control in section.controls) {
                        assertTrue("Control id must not be blank in feature ${feat.id}", control.id.isNotBlank())
                        assertTrue("Control label must not be blank in feature ${feat.id}", control.label.isNotBlank())
                        assertNotNull("Control default value must not be null in feature ${feat.id}", control.defaultValue)
                    }
                }
            }
        }
    }
}
