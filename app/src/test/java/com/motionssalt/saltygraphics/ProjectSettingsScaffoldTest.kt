package com.motionssalt.saltygraphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import com.motionssalt.saltygraphics.ui.editor.ProjectSettingsFeature
import com.motionssalt.saltygraphics.ui.editor.ProjectSettingsPage
import com.motionssalt.saltygraphics.uikit.registry.change
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectSettingsScaffoldTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun realSettingsPageUsesPanelColorAndLiveCommandsWithOneNodePerNumericGesture() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val tree = ActionTree(ProjectDocument())
        var shown by mutableStateOf(tree.currentDocument)
        var closed = false
        var previewFrames = 0
        host.get().setContent {
            MaterialTheme { Box(Modifier.requiredSize(412.dp, 450.dp)) {
                ProjectSettingsPage(shown,
                    onCommit = { change -> tree.recordCommand(change.identity(tree.currentDocument), change::apply); shown = tree.currentDocument },
                    onBack = { closed = true },
                    onPreview = { change -> previewFrames++; shown = change?.apply(tree.currentDocument) ?: tree.currentDocument })
            } }
        }
        try {
            compose.onNodeWithTag("control_grid_color").performScrollTo()
                .onChildren().filter(hasClickAction()).onFirst().performClick()
            compose.onNodeWithTag("scaffold_color_page").assertIsDisplayed()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            compose.onNodeWithText("Hex with alpha").performTextReplacement("FF000000")
            compose.onNodeWithText("Apply color").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(0xFF000000L, tree.currentDocument.editorSettings.gridColor) }
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithTag("control_grid_size").performScrollTo()
            val ruler = compose.onNode(hasTestTag("ruler_track") and hasAnyAncestor(hasTestTag("control_grid_size")))
            ruler.performTouchInput {
                down(center); moveBy(Offset(-100f, 0f), 300); moveBy(Offset(-60f, 0f), 300); up()
            }
            compose.mainClock.advanceTimeBy(5000)
            compose.waitForIdle()
            compose.runOnIdle {
                assertTrue(previewFrames > 0)
                assertTrue(tree.currentDocument.editorSettings.gridSize > 20)
                assertEquals(0xFF000000L, tree.currentDocument.editorSettings.gridColor)
                assertEquals(3, tree.nodes.size)
                assertTrue(tree.undoActionName!!.contains("Grid size"))
                tree.undo(); shown = tree.currentDocument
                assertEquals(20, shown.editorSettings.gridSize)
                assertEquals(0xFF000000L, shown.editorSettings.gridColor)
            }
            compose.onNodeWithContentDescription("Back").performClick()
            assertTrue(closed)
        } finally { host.pause().stop().destroy() }
    }

    @Test fun settingsBindingCreatedBeforeAnotherEditCannotOverwriteIt() {
        val original = ProjectDocument()
        val size = ProjectSettingsFeature.gridSize.change(original, null, 90f)
        val color = ProjectSettingsFeature.gridColor.change(original, null, 0xFF000000L)
        val tree = ActionTree(original)
        tree.recordCommand(color.identity(tree.currentDocument), color::apply)
        tree.recordCommand(size.identity(tree.currentDocument), size::apply)
        assertEquals(0xFF000000L, tree.currentDocument.editorSettings.gridColor)
        assertEquals(90, tree.currentDocument.editorSettings.gridSize)
        assertEquals(3, tree.nodes.size)
    }

}
