package com.motionssalt.saltygraphics

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** V7 guard. V24 still owns the wider legacy-editor migration. */
class FillPanelArchitectureTest {
    @Test fun fillFlowCannotCreateAnOverlayWindow() {
        val dir=File("src/main/java/com/motionssalt/saltygraphics").takeIf{it.exists()}
            ?: File("app/src/main/java/com/motionssalt/saltygraphics")
        val forbidden=Regex("\\b(Dialog|AlertDialog|ModalBottomSheet|Popup|DropdownMenu|DropdownMenuItem)\\s*\\(")
        listOf("ui/controls/FillPanelPage.kt","ui/controls/PanelControls.kt","ui/editor/GradientCanvasOverlay.kt").forEach { path ->
            val file=File(dir,path);assertTrue(file.exists())
            assertFalse("$path must remain panel-native",forbidden.containsMatchIn(file.readText()))
        }
        assertFalse("Obsolete fill dialog must not return",File(dir,"ui/controls/UniversalFillSection.kt").exists())
        assertFalse("Obsolete gradient dialog must not return",File(dir,"ui/controls/GradientSelectorDialog.kt").exists())
    }
}
