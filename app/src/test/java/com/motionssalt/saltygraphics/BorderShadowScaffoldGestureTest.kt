package com.motionssalt.saltygraphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import com.motionssalt.saltygraphics.engine.file.ProjectLoadState
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Production Border tool -> typed scaffold -> live editor -> render/history/archive path. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BorderShadowScaffoldGestureTest : FillEditorFixture() {
    @Test fun strokeGestureCommitsOnceCancelRollsBackAndColorNeverResetsOpacity() {
        val layer = Layer(id = "border_target", name = "Border target", type = LayerType.SHAPE,
            shapeData = ShapeData(width = 100f, height = 100f),
            fillData = FillData(solidColor = 0xFF224466L, solidOpacity = .2f),
            strokeData = StrokeData(isEnabled = true, color = 0xFFFF0000L, opacity = .4f, width = 4f))
        withEditorFixture(listOf(layer)) { id, repo ->
            fun saved() = runBlocking { repo.getProject(id)!! }.findLayerById(layer.id)!!
            fun nodeCount() = (runBlocking { repo.openProject(id) } as ProjectLoadState.Loaded)
                .result.historySnapshot!!.nodes.size
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText(layer.name).performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()
            compose.onNodeWithText("Border & Shadow").performClick()
            compose.onNodeWithTag("scaffold_feature_page").assertIsDisplayed()
            compose.onNodeWithTag("control_stroke_width").performScrollTo()
            val ruler = compose.onNode(hasTestTag("ruler_track") and hasAnyAncestor(hasTestTag("control_stroke_width")))
            val before = LayerCompositor.renderLayerContent(layer, 200, 200)
            ruler.performTouchInput {
                down(center); moveBy(Offset(-80f, 0f), 300); moveBy(Offset(-64f, 0f), 300); up()
            }
            compose.mainClock.advanceTimeBy(5000)
            compose.waitUntil(10000) { saved().effectiveStroke.width > 4f }
            val after = saved()
            assertEquals(2, nodeCount())
            assertEquals(.4f, after.effectiveStroke.opacity, 0f)
            assertEquals(layer.fillData, after.fillData)
            val rendered = LayerCompositor.renderLayerContent(after, 200, 200)
            assertFalse("The real stroke renderer must change", before.sameAs(rendered))
            before.recycle(); rendered.recycle()

            ruler.performTouchInput { down(center); moveBy(Offset(-70f, 0f), 300); cancel() }
            compose.waitForIdle()
            assertEquals(after.effectiveStroke, saved().effectiveStroke)
            assertEquals(2, nodeCount())

            // Color uses the raw color field, not effective color with opacity folded in.
            compose.onNodeWithTag("control_stroke_color").performScrollTo()
                .onChildren().filter(hasClickAction()).onFirst().performClick()
            compose.onNodeWithTag("scaffold_color_page").assertIsDisplayed()
            compose.onNodeWithText("Hex with alpha").performTextReplacement("8000FF00")
            compose.onNodeWithText("Apply color").performScrollTo().performClick()
            compose.waitUntil(10000) { saved().effectiveStroke.color == 0x8000FF00L }
            assertEquals(.4f, saved().effectiveStroke.opacity, 0f)
            assertEquals(3, nodeCount())
            compose.onNodeWithContentDescription("Undo").performClick()
            compose.waitUntil(10000) { saved().effectiveStroke.color == layer.effectiveStroke.color }
            compose.onNodeWithContentDescription("Redo").performClick()
            compose.waitUntil(10000) { saved().effectiveStroke.color == 0x8000FF00L }
            compose.onNodeWithTag("tools").assertIsDisplayed()
            compose.onNodeWithTag("preview").assertIsDisplayed()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
        }
    }
}
