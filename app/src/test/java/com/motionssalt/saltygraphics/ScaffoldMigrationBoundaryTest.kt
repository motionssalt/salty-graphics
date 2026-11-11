package com.motionssalt.saltygraphics

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Strict boundary for migrated entry points. Legacy screens are explicitly NOT certified by this test. */
class ScaffoldMigrationBoundaryTest {
    private val forbidden = Regex(
        "import\\s+androidx\\.compose\\.(material3|foundation)(\\.|\\s)|" +
        "\\b\\d+(?:\\.\\d+)?\\.(?:dp|sp)\\b|" +
        "\\bColor\\s*\\(|\\b(?:Dialog|AlertDialog|ModalBottomSheet|Popup|DropdownMenu|Popover)\\s*\\("
    )

    @Test fun migratedFeatureEntryPointsCannotIntroduceRawLayoutsOrTokens() {
        val root = File("src/main/java/com/motionssalt/saltygraphics/ui/editor")
            .takeIf(File::exists) ?: File("app/src/main/java/com/motionssalt/saltygraphics/ui/editor")
        for (name in listOf("BottomPanel.kt", "EditorToolFeatures.kt", "TopPanel.kt")) {
            val source = File(root, name)
            assertTrue("Migrated source must exist: $name", source.isFile)
            assertFalse("$name bypasses :ui-kit; add missing UI to the library instead", forbidden.containsMatchIn(source.readText()))
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
