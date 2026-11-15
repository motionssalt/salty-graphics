package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.ui.editor.EditorToolFeatures
import com.motionssalt.saltygraphics.uikit.registry.LibraryComponentRegistry
import com.motionssalt.saltygraphics.uikit.registry.OverlayRegistry
import com.motionssalt.saltygraphics.uikit.registry.SaltyBuiltInLibraryRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Strict module-boundary guard enforcing that :app UI screens/controls and :ui-kit sublibs stay on :ui-kit core. */
class ScaffoldMigrationBoundaryTest {
    private val forbidden = Regex(
        """import\s+androidx\.compose\.(material3|foundation)(\.|\s)|""" +
        """\b\d+(?:\.\d+)?\.(?:dp|sp)\b|""" +
        """\bColor\s*\(|(?:^|[^\w])(?:Dialog|AlertDialog|ModalBottomSheet|Popup|DropdownMenu|Popover)\s*\("""
    )

    private fun appRoot(): File = File("src/main/java/com/motionssalt/saltygraphics")
        .takeIf(File::exists) ?: File("app/src/main/java/com/motionssalt/saltygraphics")

    private fun uikitRoot(): File = File("../ui-kit/src/main/java/com/motionssalt/saltygraphics/uikit")
        .takeIf(File::exists) ?: File("ui-kit/src/main/java/com/motionssalt/saltygraphics/uikit")

    @Test fun migratedFeatureEntryPointsCannotIntroduceRawLayoutsOrTokens() {
        val appRoot = appRoot()
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
        val uiDir = File(appRoot, "ui")
        val excluded = emptySet<String>()
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

    @Test fun declarationOnlyFeaturesAndMigratedAppScreensCannotOwnLayouts() {
        val root = appRoot()
        val layouts = Regex("""\bSalty(?:Row|Column|Box|Card|SectionCard|LazyColumn|AdaptiveGrid|Text|Button|PanelPage|ScreenScaffold|NumberControl)\s*\(""")
        val uiDir = File(root, "ui")
        val excluded = setOf("PanelControls.kt", "RulerSlider.kt")
        val allUiFiles = uiDir.walkTopDown().filter { it.isFile && it.extension == "kt" && it.name !in excluded }.toList()
        assertTrue(allUiFiles.size >= 30)
        for (file in allUiFiles) {
            assertFalse(
                "${file.relativeTo(root)} must delegate to :ui-kit sublibs/declarations, not hand-code layouts",
                layouts.containsMatchIn(file.readText())
            )
        }
    }

    @Test fun allElevenSubLibrariesExistAndStayStrictlyIsolated() {
        val sublibsDir = File(uikitRoot(), "sublibs")
        assertTrue("sublibs directory must exist in :ui-kit", sublibsDir.isDirectory)
        val expectedSublibs = listOf(
            "topbar", "bottompanel", "layers", "history",
            "home", "settings", "newproject", "export",
            "text", "vector", "effects"
        )
        for (sub in expectedSublibs) {
            val dir = File(sublibsDir, sub)
            assertTrue("Sub-library $sub must exist", dir.isDirectory)
            val ktFiles = dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
            assertTrue("Sub-library $sub must contain Kotlin source", ktFiles.isNotEmpty())
            for (f in ktFiles) {
                val txt = f.readText()
                assertFalse("Sub-library $sub (${f.name}) cannot bypass :ui-kit core", forbidden.containsMatchIn(txt))
                val crossImport = Regex("""import\s+com\.motionssalt\.saltygraphics\.uikit\.sublibs\.(?!${sub}(?:\.|\s))""")
                assertFalse("Sub-library $sub (${f.name}) cannot depend on sibling sub-libraries", crossImport.containsMatchIn(txt))
            }
        }
    }

    @Test fun builtInLibraryRegistryAndDeveloperExampleFeatureWorkInProduction() {
        SaltyBuiltInLibraryRegistry.ensureRegistered()
        for (id in listOf("transform_trackpad", "gradient_bar", "history_tree", "vector_toolbar", "media_picker_grid", "layer_selector")) {
            assertTrue("Missing built-in library component: $id", LibraryComponentRegistry.contains(id))
        }
        for (id in listOf("gradient_handles", "vector_editor", "selection_box")) {
            assertTrue("Missing built-in overlay: $id", OverlayRegistry.contains(id))
        }

        val layer = Layer(name = "Shape", type = LayerType.SHAPE)
        val doc = ProjectDocument(layers = listOf(layer))
        val prev = EditorToolFeatures.isDeveloperModeEnabled()
        try {
            EditorToolFeatures.setDeveloperModeEnabled(false)
            assertFalse(EditorToolFeatures.catalog.queryTools(doc, setOf(layer.id)).any { it.id == "template_example" })
            EditorToolFeatures.setDeveloperModeEnabled(true)
            val feature = EditorToolFeatures.catalog.queryTools(doc, setOf(layer.id)).single { it.id == "template_example" }
            assertEquals("template_main", feature.defaultPageId)
            assertEquals(4, feature.pages.single().sections.flatMap { it.controls }.size)
            assertTrue(feature.pages.single().sections.flatMap { it.controls }.all { it.binding.path != null })
            val canvasView = File(appRoot(), "ui/editor/CanvasView.kt").readText()
        assertTrue("CanvasView must dispatch registered selection_box overlay", canvasView.contains("ScaffoldCanvasOverlay(") && canvasView.contains("\"selection_box\""))
        assertFalse("CanvasView must not import material3", canvasView.contains("androidx.compose.material3"))
        val mainFiles = File(appRoot().absolutePath).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        for (f in mainFiles) {
            val content = f.readText()
            assertFalse("app/src/main file ${f.name} must not import material3", content.contains("import androidx.compose.material3"))
            assertFalse("app/src/main file ${f.name} must not import foundation.layout", content.contains("import androidx.compose.foundation.layout"))
        }
        val editor = File(appRoot(), "ui/editor/EditorScreen.kt").readText()
            assertTrue(editor.contains("EditorToolFeatures.catalog.getFeature(\"template_example\")"))
            assertFalse("Host must resolve the registered page, not rebuild it", editor.contains("ExampleTemplateFeature.createDefinition("))
        } finally {
            EditorToolFeatures.setDeveloperModeEnabled(prev)
        }
    }

    @Test fun productionOverlayFacadesMustResolveRegisteredBindings() {
        val gradient = File(appRoot(), "ui/editor/GradientCanvasOverlay.kt").readText()
        assertTrue(gradient.contains("ScaffoldCanvasOverlay("))
        assertTrue(gradient.contains("GradientOverlayBindings(onPreview, onCommit, onCancel)"))
        assertFalse("Feature must not bypass gradient registration", gradient.contains("SaltyGradientLayerOverlay"))
        val vector = File(appRoot(), "ui/editor/VectorCanvasOverlay.kt").readText()
        assertTrue(vector.contains("ScaffoldCanvasOverlay("))
        assertTrue(vector.contains("vectorEditing = VectorOverlayBindings("))
        assertFalse("Feature must not bypass vector registration", vector.contains("SaltyVectorCanvasOverlay("))
        val canvasView = File(appRoot(), "ui/editor/CanvasView.kt").readText()
        assertTrue("CanvasView must dispatch registered selection_box overlay", canvasView.contains("ScaffoldCanvasOverlay(") && canvasView.contains("\"selection_box\""))
        assertFalse("CanvasView must not import material3", canvasView.contains("androidx.compose.material3"))
        val mainFiles = File(appRoot().absolutePath).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        for (f in mainFiles) {
            val content = f.readText()
            assertFalse("app/src/main file ${f.name} must not import material3", content.contains("import androidx.compose.material3"))
            assertFalse("app/src/main file ${f.name} must not import foundation.layout", content.contains("import androidx.compose.foundation.layout"))
        }
        val editor = File(appRoot(), "ui/editor/EditorScreen.kt").readText()
        assertTrue(editor.contains("onCommit = { identity, fill ->"))
        assertTrue(editor.contains("onCancel = { projectDocument = actionTree?.currentDocument }"))
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
