package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.theme.SaltyGraphicsTheme
import com.motionssalt.saltygraphics.ui.editor.VectorCreationFeature
import com.motionssalt.saltygraphics.ui.editor.VectorCreationPage
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
class VectorCreationScaffoldTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun secondProductionFeatureUsesSameRegisteredChooserWithoutLayout() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val dispatched = mutableListOf<String>()
        var back = 0
        host.get().setContent { SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
            VectorCreationPage(ProjectDocument(), { dispatched += it }, { back++ })
        } }
        try {
            compose.onNodeWithTag("scaffold_tool_picker").assertIsDisplayed()
            val labels = listOf("Draw with points", "Rectangle", "Ellipse", "Polygon", "Star", "Import SVG")
            labels.forEach { compose.onNodeWithText(it).performClick() }
            assertEquals(labels, dispatched)
            assertEquals(0, back) // Import route stays open until its system picker resolves, as before.
            compose.onNodeWithContentDescription("Back").performClick()
            assertEquals(1, back)
            assertNull(VectorCreationFeature.actionLabel("unknown"))
            compose.onAllNodes(isDialog()).assertCountEquals(0)
        } finally { host.pause().stop().destroy() }
    }
}

class VectorCreationEditorTest : SelectionPanelFixture() {
    @Test fun realCreationEntryAddsEditablePathAndUndoRemovesOnlyNewLayer() = withEditor {
        compose.onNodeWithText("Vector", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("scaffold_tool_picker").assertIsDisplayed()
        compose.onNodeWithText("Rectangle", useUnmergedTree = true).performClick()
        selection(1)
        compose.onNodeWithText("4 points").assertExists()
        compose.onNodeWithContentDescription("Undo").performClick()
        selection(0)
        compose.onNodeWithText("Vector", useUnmergedTree = true).assertIsDisplayed()
        tapArtwork(-240f)
        selection(1) // Existing artwork survives creation/undo.
    }
}
