package com.motionssalt.saltygraphics

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Strict module-boundary guard enforcing that :app UI screens/controls stay on :ui-kit. */
class ScaffoldMigrationBoundaryTest {
    private val forbidden = Regex(
        """import\s+androidx\.compose\.(material3|foundation)(\.|\s)|""" +
        """\b\d+(?:\.\d+)?\.(?:dp|sp)\b|""" +
        """\bColor\s*\(|(?:^|[^\w])(?:Dialog|AlertDialog|ModalBottomSheet|Popup|DropdownMenu|Popover)\s*\("""
    )

    @Test fun migratedFeatureEntryPointsCannotIntroduceRawLayoutsOrTokens() {
        val appRoot = File("src/main/java/com/motionssalt/saltygraphics")
            .takeIf(File::exists) ?: File("app/src/main/java/com/motionssalt/saltygraphics")
        val migratedPaths = listOf(
            "MainActivity.kt",
            "navigation/NavGraph.kt",
            "ui/home/HomeScreen.kt",
            "ui/settings/SettingsScreen.kt",
            "ui/newproject/NewProjectScreen.kt",
            "ui/export/ExportScreen.kt",
            "ui/editor/BottomPanel.kt",
            "ui/editor/EditorToolFeatures.kt",
            "ui/editor/TopPanel.kt",
            "ui/editor/ProjectSettingsPage.kt",
            "ui/editor/ProjectSettingsFeature.kt",
            "ui/editor/ShapeSettingsDialog.kt",
            "ui/editor/ShapeSettingsFeature.kt",
            "ui/editor/ShapePickerDialog.kt",
            "ui/editor/ShapePickerFeature.kt",
            "ui/editor/VectorCreationPage.kt",
            "ui/editor/VectorCreationFeature.kt",
            "ui/editor/ProjectLoadingPanel.kt",
            "ui/editor/ActionTreeDialog.kt",
            "ui/editor/EditorWorkspace.kt",
            "ui/editor/BlendingMaskDialog.kt",
            "ui/editor/ReorderableLayerList.kt",
            "ui/editor/LayersPanel.kt",
            "ui/editor/GradientCanvasOverlay.kt",
            "ui/editor/TransformPanel.kt",
            "ui/editor/VectorCanvasOverlay.kt",
            "ui/editor/VectorPage.kt",
            "ui/editor/TextSettingsDialog.kt",
            "ui/editor/EffectSettingsSheet.kt",
            "ui/editor/EditorScreen.kt",
            "ui/controls/FillPanelPage.kt",
            "ui/controls/BorderShadowPanelPage.kt",
            "ui/controls/BorderShadowFeature.kt",
            "ui/controls/ColorPickerComponents.kt",
            "ui/controls/MiscControls.kt",
            "ui/controls/PanelControls.kt",
            "ui/controls/ProgressSliderControl.kt",
            "ui/controls/RulerSlider.kt",
            "ui/controls/SaltySwitch.kt",
            "ui/controls/ValueEditorDialog.kt"
        )
        for (rel in migratedPaths) {
            val source = File(appRoot, rel)
            assertTrue("Migrated source must exist: $rel", source.isFile)
            assertFalse("$rel bypasses :ui-kit; add missing UI to the library instead", forbidden.containsMatchIn(source.readText()))
        }

        // Also scan all UI files in app/ui excluding CanvasView.kt (raw drawing canvas surface)
        val uiDir = File(appRoot, "ui")
        val excluded = setOf("CanvasView.kt")
        val allUiFiles = uiDir.walkTopDown().filter { it.isFile && it.extension == "kt" && it.name !in excluded }.toList()
        assertTrue("Expected migrated UI files", allUiFiles.size >= 30)
        for (file in allUiFiles) {
            val text = file.readText()
            if (text.contains("@Composable") || text.contains("androidx.compose")) {
                assertFalse(
                    "Unlisted UI file ${file.relativeTo(appRoot)} bypasses :ui-kit",
                    forbidden.containsMatchIn(text)
                )
            }
        }
    }

    @Test fun declarationOnlyFeaturesCannotHideLayoutsBehindLibraryWrappers() {
        val root = File("src/main/java/com/motionssalt/saltygraphics")
            .takeIf(File::exists) ?: File("app/src/main/java/com/motionssalt/saltygraphics")
        val layouts = Regex("""\bSalty(?:Row|Column|Box|Card|SectionCard|LazyColumn|Text|Button|PanelPage|NumberControl)\s*\(""")
        for (path in listOf("ui/editor/ProjectSettingsPage.kt", "ui/editor/ProjectSettingsFeature.kt",
            "ui/editor/ShapeSettingsDialog.kt", "ui/editor/ShapeSettingsFeature.kt",
            "ui/controls/BorderShadowPanelPage.kt", "ui/controls/BorderShadowFeature.kt")) {
            assertFalse("$path must declare controls/use ScaffoldFeaturePage, not hand-code layouts with wrappers",
                layouts.containsMatchIn(File(root, path).readText()))
        }
    }

    @Test fun guardRejectsRawLayoutsWidgetsColorsAndPopupRegressions() {
        for (snippet in listOf(
            "import androidx.compose.foundation.layout.Row", "import androidx.compose.material3.Text",
            "Modifier.padding(8.dp)", "fontSize = 12.sp", "Color(0xFF000000)",
            "AlertDialog(", "Dialog(", "ModalBottomSheet(", "Popup(", "DropdownMenu(", "Popover("
        )) assertTrue("Guard must reject $snippet", forbidden.containsMatchIn(snippet))
        assertFalse(forbidden.containsMatchIn("ScaffoldToolPanel(catalog = EditorToolFeatures.catalog)"))
        assertFalse(forbidden.containsMatchIn("SaltyConfirmDialog(title = \"Delete\")"))
        assertFalse(forbidden.containsMatchIn("SaltyRecentColorsRegistry.recordColor(c)"))
    }
}
