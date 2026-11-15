package com.motionssalt.saltygraphics

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import com.motionssalt.saltygraphics.engine.file.ProjectLoadState
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.ui.editor.EditorToolFeatures
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class DeveloperTemplateFixture : FillEditorFixture() {
    protected val prefs get() = RuntimeEnvironment.getApplication()
        .getSharedPreferences("salty_dev_prefs", Context.MODE_PRIVATE)

    @Before fun resetBefore() { prefs.edit().clear().commit(); EditorToolFeatures.setDeveloperModeEnabled(false) }
    @After fun resetAfter() { prefs.edit().clear().commit(); EditorToolFeatures.setDeveloperModeEnabled(false) }

    protected val target = Layer(id = "template_target", name = "Template target", type = LayerType.SHAPE,
        shapeData = ShapeData(width = 100f, height = 100f),
        fillData = FillData(solidColor = 0xFF336699L),
        strokeData = StrokeData(isEnabled = true, color = 0xFFFF0000L, opacity = .4f, width = 7f))

    protected fun selectTarget() {
        compose.onNodeWithContentDescription("Layers").performClick()
        compose.onNodeWithText(target.name).performClick()
        compose.onNodeWithContentDescription("Close Layers").performClick()
    }

}

// Keep each real editor host in its own existing forkEvery=1 worker.
class OrdinaryTemplateEditorTest : DeveloperTemplateFixture() {
    @Test fun ordinaryEditorNeverShowsTheDeveloperSample() {
        withEditorFixture(listOf(target)) { _, _ ->
            selectTarget()
            compose.onNodeWithText("Template Demo").assertDoesNotExist()
            compose.onNodeWithText("Template Controls").assertDoesNotExist()
            compose.onNodeWithText("Transform").assertIsDisplayed()
        }
    }

}

class DeveloperTemplateEditorTest : DeveloperTemplateFixture() {
    @Test fun realDeveloperTileResolvesRegisteredPageAndWritesSavedLabelledHistory() {
        prefs.edit().putBoolean("dev_options", true).commit()
        withEditorFixture(listOf(target)) { id, repo ->
            fun saved() = runBlocking { repo.getProject(id)!! }
            fun history() = (runBlocking { repo.openProject(id) } as ProjectLoadState.Loaded).result.historySnapshot!!
            selectTarget()
            compose.onNodeWithText("Template Demo").assertIsDisplayed().performClick()
            compose.onNodeWithText("Template Controls").assertIsDisplayed()
            compose.onNodeWithTag("scaffold_feature_page").assertIsDisplayed()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            val initial = saved()
            compose.onNode(isToggleable() and hasAnyAncestor(hasTestTag("control_toggle_demo"))).performClick()
            compose.waitUntil(10000) { saved().editorSettings.snapEnabled != initial.editorSettings.snapEnabled }
            assertEquals("Disable Snapping enabled", history().nodes.getValue(history().currentNodeId).description)
            assertEquals(2, history().nodes.size)

            // A second property must preserve the first rather than copying a stale document.
            compose.onNodeWithTag("control_progress_demo").performScrollTo()
            compose.onNode(hasTestTag("progress_track") and hasAnyAncestor(hasTestTag("control_progress_demo")))
                .performTouchInput {
                    down(Offset(width * .5f, centerY)); moveTo(Offset(width * .3f, centerY), 300); up()
                }
            compose.mainClock.advanceTimeBy(5000)
            compose.waitUntil(10000) { saved().findLayerById(target.id)!!.opacity < .9f }
            val opacity = saved().findLayerById(target.id)!!.opacity
            assertFalse(saved().editorSettings.snapEnabled)
            assertEquals(3, history().nodes.size)
            assertEquals("Decrease Layer opacity", history().nodes.getValue(history().currentNodeId).description)
            assertEquals(target.effectiveFill, saved().findLayerById(target.id)!!.effectiveFill)
            assertEquals(target.effectiveStroke, saved().findLayerById(target.id)!!.effectiveStroke)

            // Close through the shared panel header; the canvas never becomes an overlay page.
            compose.onAllNodesWithContentDescription("Back").filterToOne(hasAnyAncestor(hasTestTag("tools"))).performClick()
            compose.onNodeWithTag("scaffold_tool_panel").assertIsDisplayed()
            compose.onNodeWithTag("preview").assertIsDisplayed()
            compose.onNodeWithTag("tools").assertIsDisplayed()
            compose.onNodeWithContentDescription("Undo").performClick()
            compose.waitUntil(10000) { saved().findLayerById(target.id)!!.opacity == 1f }
            assertFalse(saved().editorSettings.snapEnabled)
            compose.onNodeWithContentDescription("Redo").performClick()
            compose.waitUntil(10000) { saved().findLayerById(target.id)!!.opacity == opacity }
            assertFalse(saved().editorSettings.snapEnabled)
            compose.onAllNodes(isDialog()).assertCountEquals(0)
        }
    }
}
