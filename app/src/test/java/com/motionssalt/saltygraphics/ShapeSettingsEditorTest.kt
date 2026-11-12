package com.motionssalt.saltygraphics

import androidx.compose.ui.test.*
import org.junit.Test

/** Actual tool -> selected target -> scaffold -> editor history, not a test-only host. */
class ShapeSettingsEditorTest : SelectionPanelFixture() {
    @Test fun editEntryWritesWidthAndUndoRedoAndTargetSwitchStayLive() = withEditor {
        tapArtwork(-240f)
        compose.onNodeWithText("Edit", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("scaffold_feature_page").assertIsDisplayed()
        compose.onNode(hasText("160 px") and hasAnyAncestor(hasTestTag("control_shape_width"))).performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("222.25")
        compose.onNodeWithText("OK").performClick()
        compose.onNode(hasText("222 px") and hasAnyAncestor(hasTestTag("control_shape_width"))).assertExists()
        compose.onNodeWithContentDescription("Undo").performClick()
        compose.onNode(hasText("160 px") and hasAnyAncestor(hasTestTag("control_shape_width"))).assertExists()
        compose.onNodeWithContentDescription("Redo").performClick()
        compose.onNode(hasText("222 px") and hasAnyAncestor(hasTestTag("control_shape_width"))).assertExists()
        // Same page, different selected target: no stale geometry/entry-strip state.
        tapArtwork(240f)
        compose.onNode(hasText("160 px") and hasAnyAncestor(hasTestTag("control_shape_width"))).assertExists()
        compose.onAllNodes(isDialog()).assertCountEquals(0)
        compose.onNode(hasContentDescription("Back") and hasAnyAncestor(hasTestTag("scaffold_feature_page"))).performClick()
        compose.onNodeWithText("Transform", useUnmergedTree = true).assertIsDisplayed()
    }
}
