package com.motionssalt.saltygraphics

import androidx.compose.ui.test.*
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Test

class SelectionHistoryRegressionTest : SelectionPanelFixture() {
    @Test fun repeatedCanvasAndLayerRowSelectionsNeverMoveHistoryPointerOrCreateBranches() {
        lateinit var fork: String
        lateinit var otherBranch: String
        withEditor(historySetup = { fixture ->
            val tree=ActionTree(fixture)
            fork=tree.recordAction("Move right",fixture.updateLayer("right") {
                it.copy(transform=it.transform.copy(translationX=260f))
            }).id
            otherBranch=tree.recordAction("Delete left",tree.currentDocument.removeLayer("left")).id
            tree.undo()
            tree.recordAction("Rename right",tree.currentDocument.updateLayer("right") {it.copy(name="Renamed")})
            tree.undo() // Current fork has two retained children.
            tree
        }) {
            repeat(12) {
                tapArtwork(-240f);tapArtwork(260f);tapArtwork(0f,350f)
            }
            compose.onNodeWithContentDescription("Layers").performClick()
            repeat(6) {
                compose.onNodeWithText("Left shape").performClick()
                compose.onNodeWithText("Right shape").performClick()
            }
            compose.onNodeWithText("Select All").performClick()
            selection(2)
            compose.onNodeWithContentDescription("Select None").performClick()
            selection(0)
            compose.onNodeWithContentDescription("Close Layers").performClick()
            compose.onNodeWithContentDescription("Action Tree History").performClick()
            compose.onNodeWithTag("history_node_$fork").assertIsSelected()
            compose.onNodeWithTag("history_node_$otherBranch").assertIsNotSelected()
            compose.onNodeWithText("Rename right").assertExists()
            // Only intentional tree interaction may navigate to an alternate branch.
            compose.onNodeWithTag("history_node_$otherBranch").performClick()
            compose.onNodeWithContentDescription("Action Tree History").performClick()
            compose.onNodeWithTag("history_node_$otherBranch").assertIsSelected()
        }
    }
}
