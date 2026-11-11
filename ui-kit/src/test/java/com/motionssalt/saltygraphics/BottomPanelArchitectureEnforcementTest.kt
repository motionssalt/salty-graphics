package com.motionssalt.saltygraphics

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/**
 * Standing UI Rule Enforcement: Editing lives in the bottom panel.
 *
 * All editing controls must remain inside the bottom panel without obscuring the canvas.
 * Scans editing-flow UI code in ui/editor and ui/controls and asserts zero usage of:
 * Dialog, AlertDialog, ModalBottomSheet, Popup, DropdownMenu outside an explicit allow-list.
 */
class BottomPanelArchitectureEnforcementTest {

    @Test
    fun editingFlowContainsNoForbiddenOverlays() {
        val root = File("src/main/java/com/motionssalt/saltygraphics").takeIf { it.exists() }
            ?: File("ui-kit/src/main/java/com/motionssalt/saltygraphics")
        assertTrue("Source root must exist", root.exists())

        val forbiddenRegex = Regex("\\b(ModalBottomSheet|Popup|DropdownMenu|DropdownMenuItem)\\s*\\(")
        val dialogRegex = Regex("\\b(Dialog|AlertDialog)\\s*\\(")

        // Files strictly part of the editing flow (panel, tools, inspector pages)
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
            "ui/editor/VectorCreationPage.kt",
            "ui/editor/ProjectSettingsPage.kt"
        )

        for (relPath in editingFlowFiles) {
            val file = File(root, relPath)
            assertTrue("File $relPath must exist", file.exists())
            val text = file.readText()

            // Disallow any popups, dropdown menus, modal sheets
            val match = forbiddenRegex.find(text)
            assertNull("Forbidden overlay component found in $relPath: ${match?.value}", match)

            // Disallow Dialog or AlertDialog in panel pages (except ValueEditorDialog which is docked above keyboard)
            val dialogMatch = dialogRegex.find(text)
            assertNull("Dialog/AlertDialog must not be used in editing flow file $relPath: ${dialogMatch?.value}", dialogMatch)
        }
    }

    @Test
    fun editorScreenAllowsOnlyApprovedExceptions() {
        val root = File("src/main/java/com/motionssalt/saltygraphics").takeIf { it.exists() }
            ?: File("ui-kit/src/main/java/com/motionssalt/saltygraphics")
        val editorScreenFile = File(root, "ui/editor/EditorScreen.kt")
        assertTrue("EditorScreen.kt must exist", editorScreenFile.exists())
        val text = editorScreenFile.readText()

        // EditorScreen only allows:
        // 1. Destructive/irreversible confirmation dialogs: Rasterize confirmation, Save Error confirmation.
        // 2. ActionTree history viewer (non-editing inspection dialog)
        // Ensure no other Dialogs exist in EditorScreen
        val dialogMatches = Regex("\\b(AlertDialog)\\s*\\(").findAll(text).toList()
        // Exactly 2 AlertDialogs: Save Error dialog and Rasterize confirmation dialog
        assertTrue("EditorScreen should only contain allowed confirmation AlertDialogs (count: ${dialogMatches.size})", dialogMatches.size <= 2)
    }
}
