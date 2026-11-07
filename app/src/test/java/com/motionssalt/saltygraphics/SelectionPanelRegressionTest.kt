package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.ui.editor.EditorScreen
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Exercise real EditorScreen flags/callbacks, not an isolated always-visible pane. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class SelectionPanelFixture {
    @get:Rule val compose=createEmptyComposeRule()
    private val context get()=RuntimeEnvironment.getApplication()

    protected fun withEditor(historySetup: (ProjectDocument) -> ActionTree? = { null }, check: () -> Unit) {
        val repo=ProjectRepository(context)
        val doc=runBlocking {
            val project=repo.createProject("Selection fixture",CanvasSize(1000,1000))
            project.copy(layers=listOf(
                Layer(id="left",name="Left shape",shapeData=ShapeData(width=160f,height=160f),transform=Transform2D(translationX=-240f)),
                Layer(id="right",name="Right shape",shapeData=ShapeData(width=160f,height=160f),transform=Transform2D(translationX=240f))))
                .let { fixture ->
                    val history = historySetup(fixture)
                    (history?.currentDocument ?: fixture).also { repo.saveProject(it, history?.toSnapshot()) }
                }
        }
        val host=Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            host.get().setContent {MaterialTheme {EditorScreen(doc.id,repository=repo,onNavigateBack={},onNavigateToExport={},onNavigateToSettings={})}}
            compose.waitUntil(5000) {compose.onAllNodesWithTag("editor_canvas").fetchSemanticsNodes().isNotEmpty()}
            check()
        } finally {host.pause().stop().destroy()}
    }

    protected fun tapArtwork(x: Float,y: Float=0f) {
        val node=compose.onNodeWithTag("editor_canvas")
        val size=node.fetchSemanticsNode().size
        val fit=minOf(size.width/1000f,size.height/1000f)*.94f
        node.performTouchInput {click(Offset(size.width/2f+x*fit,size.height/2f+y*fit))}
    }

    protected fun selection(count: Int) {
        compose.onNodeWithTag("editor_canvas").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
            if(count==0) "No layers selected" else "$count layers selected"))
    }

}

class TransformPanelSelectionRegressionTest : SelectionPanelFixture() {
    @Test fun transformSurvivesLayerAndOutsideTapsAndEmptyTapShowsNoSelectionTools() = withEditor {
        tapArtwork(-240f)
        compose.onNodeWithText("Transform",useUnmergedTree=true).performClick()
        compose.onNodeWithText("2D Transform").assertIsDisplayed()
        tapArtwork(-240f) // Same layer: do not dismiss Transform.
        compose.onNodeWithText("2D Transform").assertIsDisplayed()
        tapArtwork(240f) // Different layer: Transform is still applicable.
        compose.onNodeWithText("2D Transform").assertIsDisplayed()
        compose.onNodeWithTag("tools").assertIsDisplayed()
        tapArtwork(0f,350f) // Empty canvas: pane remains, grid replaces the page.
        selection(0)
        compose.onNodeWithTag("tools").assertIsDisplayed()
        compose.onNodeWithText("Media",useUnmergedTree=true).assertIsDisplayed()
        compose.onNodeWithText("Shape",useUnmergedTree=true).assertIsDisplayed()
        tapArtwork(-240f)
        compose.onNodeWithText("2D Transform").assertDoesNotExist() // No stale page resurrection.
        compose.onNodeWithText("Transform",useUnmergedTree=true).assertIsDisplayed()
    }

}

class SelectNoneRegressionTest : SelectionPanelFixture() {
    @Test fun selectNoneAndEmptyTapLeaveNoStaleSelectionAndCreationToolsWork() = withEditor {
        tapArtwork(-240f)
        selection(1)
        compose.onNodeWithContentDescription("Layers").performClick()
        compose.onNodeWithContentDescription("Select None").performClick()
        selection(0)
        compose.onNodeWithContentDescription("Close Layers").performClick()
        compose.onNodeWithText("Shape",useUnmergedTree=true).performClick()
        selection(1)
        tapArtwork(0f,350f)
        selection(0)
        compose.onNodeWithText("Media",useUnmergedTree=true).assertIsDisplayed()
        compose.onNodeWithText("Adjustment",useUnmergedTree=true).performClick()
        selection(1)
        compose.onNodeWithTag("tools").assertIsDisplayed()
    }

}

class IncompatibleSelectionPanelRegressionTest : SelectionPanelFixture() {
    @Test fun incompatibleSelectionLeavesTextPageForToolsRatherThanAnEmptyPane() = withEditor {
        compose.onNodeWithText("Text",useUnmergedTree=true).performClick()
        // Text creation opens its settings. Select through the real drawer so the
        // newly created text's visible bounds cannot intercept the shape tap.
        compose.onNodeWithContentDescription("Layers").performClick()
        compose.onNodeWithText("Left shape").performClick()
        compose.onNodeWithContentDescription("Close Layers").performClick()
        compose.onNodeWithTag("tools").assertIsDisplayed()
        compose.onNodeWithText("Transform",useUnmergedTree=true).assertIsDisplayed()
    }
}
