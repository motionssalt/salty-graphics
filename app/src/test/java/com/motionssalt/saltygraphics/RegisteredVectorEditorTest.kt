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
import kotlin.math.min

/** Real editor tool -> registered overlay -> saved command -> renderer, not just registry ids. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RegisteredVectorEditorTest : FillEditorFixture() {
    @Test fun selectedAnchorDragUsesRealRegisteredCallbacksAndUndoRestoresThePath() {
        val target = Layer(id = "registered_vector", name = "Registered vector", type = LayerType.VECTOR,
            vectorPathData = VectorPathData(isClosed = true, points = listOf(
                BezierPoint(-200f, -150f), BezierPoint(200f, -150f), BezierPoint(0f, 200f)
            )))
        withEditorFixture(listOf(target)) { id, repo ->
            fun saved() = runBlocking { repo.getProject(id)!! }.findLayerById(target.id)!!
            fun history() = (runBlocking { repo.openProject(id) } as ProjectLoadState.Loaded).result.historySnapshot!!
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText(target.name).performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()
            compose.onNodeWithText("Edit").performClick()
            compose.onNodeWithTag("registered_overlay_vector_editor").assertIsDisplayed()
            val handles = compose.onNodeWithTag("vector_handles")
            val rect = handles.getUnclippedBoundsInRoot()
            val fit = min((rect.right - rect.left).value / 1000f, (rect.bottom - rect.top).value / 1000f) * .94f
            val before = LayerCompositor.renderLayerContent(target, 600, 600)
            handles.performTouchInput {
                down(center + Offset(-200f * fit, -150f * fit))
                moveTo(center + Offset(-160f * fit, -100f * fit), 200)
                up()
            }
            compose.waitUntil(10000) { saved().editablePaths.first().points.first().x > -180f }
            val changed = saved()
            assertEquals(3, changed.editablePaths.first().points.size)
            assertEquals(-160f, changed.editablePaths.first().points.first().x, 1f)
            assertEquals(-100f, changed.editablePaths.first().points.first().y, 1f)
            assertEquals(target.transform, changed.transform)
            assertEquals(2, history().nodes.size)
            assertEquals("Edit vector points", history().nodes.getValue(history().currentNodeId).description)
            val after = LayerCompositor.renderLayerContent(changed, 600, 600)
            assertFalse("Registered edit must change pixels through the real pipeline", before.sameAs(after))
            before.recycle(); after.recycle()
            compose.onNodeWithTag("preview").assertIsDisplayed()
            compose.onNodeWithTag("tools").assertIsDisplayed()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            compose.onNodeWithContentDescription("Undo").performClick()
            compose.waitUntil(10000) { saved().editablePaths == target.editablePaths }
            compose.onNodeWithContentDescription("Redo").performClick()
            compose.waitUntil(10000) { saved().editablePaths == changed.editablePaths }
        }
    }
}
