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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RegisteredGradientBarEditorTest : FillEditorFixture() {
    @Test fun stopRailUsesRealRegisteredBindingsAndCommitsOneSavedRenderedUndoableEdit() {
        val layer = Layer(id = "registered_stops", name = "Registered stops", type = LayerType.SHAPE,
            shapeData = ShapeData(width = 400f, height = 400f), fillData = FillData(type = FillType.GRADIENT))
        withEditorFixture(listOf(layer)) { id, repo ->
            fun saved() = runBlocking { repo.getProject(id)!! }.findLayerById(layer.id)!!
            fun history() = (runBlocking { repo.openProject(id) } as ProjectLoadState.Loaded).result.historySnapshot!!
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText(layer.name).performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()
            compose.onNodeWithText("Fill").performClick()
            compose.onNodeWithText("Edit Gradient").performClick()
            val rail = compose.onNodeWithTag("gradient_stop_rail").performScrollTo()
            compose.onNodeWithTag("registered_component_gradient_bar").assertIsDisplayed()
            rail.performTouchInput {
                down(Offset(width * .10f, centerY))
                moveTo(Offset(width * .25f, centerY), 100)
                moveTo(Offset(width * .40f, centerY), 100)
                up()
            }
            compose.waitUntil(10000) { saved().effectiveFill.gradient.stops.first().position > .35f }
            val changed = saved()
            assertEquals(.40f, changed.effectiveFill.gradient.stops.first().position, .01f)
            assertEquals(2, history().nodes.size)
            assertEquals("Move gradient stop", history().nodes.getValue(history().currentNodeId).description)
            val before = LayerCompositor.renderLayerContent(layer, 500, 500)
            val after = LayerCompositor.renderLayerContent(changed, 500, 500)
            assertFalse("Registered stop edit must change real rendered pixels", before.sameAs(after))
            before.recycle(); after.recycle()
            rail.performTouchInput {
                down(Offset(width * .40f, centerY)); moveTo(Offset(width * .60f, centerY), 100); cancel()
            }
            compose.waitForIdle()
            assertEquals(changed.effectiveFill, saved().effectiveFill)
            assertEquals(2, history().nodes.size)
            compose.onNodeWithContentDescription("Undo").performClick()
            compose.waitUntil(10000) { saved().effectiveFill == layer.effectiveFill }
            compose.onNodeWithContentDescription("Redo").performClick()
            compose.waitUntil(10000) { saved().effectiveFill == changed.effectiveFill }
            compose.onNodeWithTag("preview").assertIsDisplayed()
            compose.onNodeWithTag("tools").assertIsDisplayed()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RegisteredTrackpadEditorTest : FillEditorFixture() {
    @Test fun transformTrackpadUsesRealSessionAndExactEntryWithSingleDragHistory() {
        val layer = Layer(id = "registered_position", name = "Registered position", type = LayerType.SHAPE,
            shapeData = ShapeData(width = 100f, height = 80f),
            transform = Transform2D(translationX = 120f, translationY = 80f))
        withEditorFixture(listOf(layer)) { id, repo ->
            fun saved() = runBlocking { repo.getProject(id)!! }.findLayerById(layer.id)!!
            fun history() = (runBlocking { repo.openProject(id) } as ProjectLoadState.Loaded).result.historySnapshot!!
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText(layer.name).performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()
            compose.onNodeWithText("Transform").performClick()
            compose.onNodeWithTag("trackpad_surface").performScrollTo()
            compose.onNodeWithTag("registered_component_transform_trackpad").assertIsDisplayed()
            compose.onNodeWithTag("trackpad_surface").performTouchInput {
                down(center); moveBy(Offset(45f, 20f), 200); up()
            }
            compose.waitUntil(10000) { saved().transform.translationX > 130f }
            val changed = saved().transform
            assertEquals(layer.transform.scaleX, changed.scaleX, 0f)
            assertEquals(layer.transform.rotationDegrees, changed.rotationDegrees, 0f)
            assertEquals(2, history().nodes.size)
            compose.onNodeWithText("X: ${changed.translationX.toInt()} px").performScrollTo().performClick()
            compose.onNodeWithTag("entry_Move X").performScrollTo().performTextReplacement("123.25")
            compose.onNodeWithText("Done").performScrollTo().performClick()
            compose.waitUntil(10000) { saved().transform.translationX == 123.25f }
            val exact = saved().transform
            assertEquals(changed.translationY, exact.translationY, 0f)
            assertEquals(3, history().nodes.size)
            compose.onNodeWithContentDescription("Undo").performClick()
            compose.waitUntil(10000) { saved().transform == changed }
            compose.onNodeWithContentDescription("Redo").performClick()
            compose.waitUntil(10000) { saved().transform == exact }
            compose.onNodeWithTag("preview").assertIsDisplayed()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
        }
    }
}
