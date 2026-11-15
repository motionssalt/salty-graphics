package com.motionssalt.saltygraphics

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Strict boundary for migrated entry points. Legacy screens are explicitly NOT certified by this test. */
class ScaffoldMigrationBoundaryTest {
    private val forbidden = Regex(
        "import\s+androidx\.compose\.(material3|foundation)(\.|\s)|" +
        "\b\d+(?:\.\d+)?\.(?:dp|sp)\b|" +
        "\bColor\s*\(|\b(?:Dialog|AlertDialog|ModalBottomSheet|Popup|DropdownMenu|Popover)\s*\("
    )

    @Test fun migratedFeatureEntryPointsCannotIntroduceRawLayoutsOrTokens() {
        val appRoot = File("src/main/java/com/motionssalt/saltygraphics")
            .takeIf(File::exists) ?: File("app/src/main/java/com/motionssalt/saltygraphics")
        val migratedPaths = listOf(
            "MainActivity.kt",
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
            "ui/editor/EditorWorkspace.kt"
        )
        for (rel in migratedPaths) {
            val source = File(appRoot, rel)
            assertTrue("Migrated source must exist: $rel", source.isFile)
            assertFalse("$rel bypasses :ui-kit; add missing UI to the library instead", forbidden.containsMatchIn(source.readText()))
        }
    }

    @Test fun guardRejectsRawLayoutsWidgetsColorsAndPopupRegressions() {
        for (snippet in listOf(
            "import androidx.compose.foundation.layout.Row", "import androidx.compose.material3.Text",
            "Modifier.padding(8.dp)", "fontSize = 12.sp", "Color(0xFF000000)",
            "AlertDialog(", "Dialog(", "ModalBottomSheet(", "Popup(", "DropdownMenu(", "Popover("
        )) assertTrue("Guard must reject $snippet", forbidden.containsMatchIn(snippet))
        assertFalse(forbidden.containsMatchIn("ScaffoldToolPanel(catalog = EditorToolFeatures.catalog)"))
    }
}
