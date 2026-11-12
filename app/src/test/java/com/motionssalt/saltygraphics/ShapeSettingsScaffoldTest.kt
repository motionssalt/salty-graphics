package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import com.motionssalt.saltygraphics.ui.editor.ShapeSettingsDialog
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShapeSettingsScaffoldTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun fixture() = ProjectDocument(canvasSize = CanvasSize(1000, 1000), layers = listOf(
        Layer(id = "shape", type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 100f, height = 100f),
            fillData = FillData(solidColor = 0x804477DDL, solidOpacity = .35f))))

    @Test fun dragPreviewsWithoutHistoryCommitsOnceAndChangesRealRenderedPixels() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val tree = ActionTree(fixture())
        var shown by mutableStateOf(tree.currentDocument)
        var previews = 0
        var closed = false
        host.get().setContent { MaterialTheme { Box(Modifier.requiredSize(412.dp, 400.dp)) {
            ShapeSettingsDialog(shown, "shape", onCommit = { change ->
                tree.recordCommand(change.identity(tree.currentDocument), change::apply); shown = tree.currentDocument
            }, onDismiss = { closed = true }, onPreview = { change ->
                previews++; shown = change?.apply(tree.currentDocument) ?: tree.currentDocument
            })
        } } }
        try {
            val before = LayerCompositor.compositeDocument(tree.currentDocument, 1000, 1000)
            val ruler = compose.onNode(hasTestTag("ruler_track") and hasAnyAncestor(hasTestTag("control_shape_width")))
            ruler.performTouchInput { down(center); moveBy(Offset(-120f, 0f), 300) }
            compose.runOnIdle {
                assertTrue(previews > 0)
                assertEquals(1, tree.nodes.size)
                assertTrue(shown.findLayerById("shape")!!.shapeData!!.width > 100f)
            }
            ruler.performTouchInput { moveBy(Offset(-60f, 0f), 300); advanceEventTime(200); up() }
            compose.mainClock.advanceTimeBy(5000)
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals(2, tree.nodes.size)
                assertTrue(tree.undoActionName!!.contains("Shape width"))
                assertEquals(fixture().layers.single().fillData, tree.currentDocument.layers.single().fillData)
                val after = LayerCompositor.compositeDocument(tree.currentDocument, 1000, 1000)
                assertFalse("Geometry edit must reach compositor", before.sameAs(after))
                after.recycle(); before.recycle()
                assertTrue(tree.undo()); shown = tree.currentDocument
                assertEquals(100f, shown.layers.single().shapeData!!.width, 0f)
                assertTrue(tree.redo()); shown = tree.currentDocument
            }
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            compose.onNodeWithContentDescription("Back").performClick()
            assertTrue(closed)
        } finally { host.pause().stop().destroy() }
    }

    @Test fun cancellationAndBackDiscardDraftWithoutHistory() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val tree = ActionTree(fixture())
        var shown by mutableStateOf(tree.currentDocument)
        host.get().setContent { MaterialTheme { Box(Modifier.requiredSize(412.dp, 400.dp)) {
            ShapeSettingsDialog(shown, "shape", onCommit = { fail("Cancelled gesture cannot commit") },
                onDismiss = {}, onPreview = { change -> shown = change?.apply(tree.currentDocument) ?: tree.currentDocument })
        } } }
        try {
            compose.onNode(hasTestTag("ruler_track") and hasAnyAncestor(hasTestTag("control_shape_width")))
                .performTouchInput { down(center); moveBy(Offset(-100f, 0f), 300); cancel() }
            compose.runOnIdle { assertEquals(tree.currentDocument, shown); assertEquals(1, tree.nodes.size) }
            compose.onAllNodes(isDialog()).assertCountEquals(0)
        } finally { host.pause().stop().destroy() }
    }

    @Test fun shapeSpecificControlsFollowTargetAndAllNumericEntryStaysInPanel() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var shown by mutableStateOf(fixture())
        var commits = 0
        host.get().setContent { MaterialTheme { Box(Modifier.requiredSize(412.dp, 650.dp)) {
            ShapeSettingsDialog(shown, "shape", onCommit = { shown = it.apply(shown); commits++ }, onDismiss = {})
        } } }
        try {
            compose.onNodeWithTag("control_shape_corner_radius").assertDoesNotExist()
            compose.runOnIdle { shown = shown.updateLayer("shape") { it.copy(shapeData = it.shapeData!!.copy(type = ShapeType.ROUNDED_RECTANGLE)) } }
            compose.onNodeWithTag("control_shape_corner_radius").assertExists()
            compose.runOnIdle { shown = shown.updateLayer("shape") { it.copy(shapeData = it.shapeData!!.copy(type = ShapeType.POLYGON)) } }
            compose.onNodeWithTag("control_shape_corner_radius").assertDoesNotExist()
            compose.onNodeWithTag("control_shape_sides").assertExists()
            compose.runOnIdle { shown = shown.updateLayer("shape") { it.copy(shapeData = it.shapeData!!.copy(type = ShapeType.STAR)) } }
            compose.onNodeWithTag("control_shape_sides").assertDoesNotExist()
            compose.onNodeWithTag("control_shape_star_points").assertExists()
            compose.onNodeWithTag("control_shape_inner_radius").assertExists()
            compose.onNode(hasText("100 px") and hasAnyAncestor(hasTestTag("control_shape_width"))).performClick()
            val input = compose.onNode(hasSetTextAction())
            input.performTextReplacement("0")
            compose.onNodeWithText("OK").assertIsNotEnabled()
            input.performTextReplacement("410.25")
            compose.onNodeWithText("OK").assertIsEnabled().performClick()
            compose.runOnIdle {
                assertEquals(1, commits)
                assertEquals(410.25f, shown.layers.single().shapeData!!.width, 0f)
                assertEquals(fixture().layers.single().fillData, shown.layers.single().fillData)
            }
            compose.onAllNodes(isDialog()).assertCountEquals(0)
        } finally { host.pause().stop().destroy() }
    }
}
