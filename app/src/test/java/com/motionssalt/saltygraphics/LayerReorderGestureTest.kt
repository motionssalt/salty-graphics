package com.motionssalt.saltygraphics

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import com.motionssalt.saltygraphics.ui.editor.EditorScreen
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], qualifiers="w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class LayerReorderFixture {
    @get:Rule val compose=createEmptyComposeRule()
    protected fun withLayers(count: Int, test: (ProjectRepository,String)->Unit) {
        val repo=ProjectRepository(RuntimeEnvironment.getApplication())
        val doc=runBlocking {
            val d=repo.createProject("Reorder fixture",CanvasSize(200,200))
            d.copy(layers=(0 until count).map { i -> Layer(id="l$i",name="Layer $i",type=LayerType.SHAPE,
                shapeData=ShapeData(width=100f,height=100f,fillColor=if(i==0) 0xFFFF0000L else 0xFF0000FFL)) }).also {repo.saveProject(it)}
        }
        val host=Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            host.get().setContent { MaterialTheme { EditorScreen(doc.id,repository=repo,onNavigateBack={},onNavigateToExport={},onNavigateToSettings={}) } }
            compose.waitUntil(10000) {compose.onAllNodesWithTag("editor_canvas").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithContentDescription("Layers").performClick()
            test(repo,doc.id)
        } finally { host.pause().stop().destroy() }
    }
    protected fun saved(repo: ProjectRepository,id:String)=runBlocking {repo.getProjectFull(id)!!}
    protected fun waitForOrder(repo:ProjectRepository,id:String,expected: List<String>) {
        compose.waitUntil(10000) { saved(repo,id).document.layers.map{it.id}==expected }
    }
    protected fun point(id:String): Offset {
        val list=compose.onNodeWithTag("layer_list").fetchSemanticsNode().boundsInRoot
        val handle=compose.onNodeWithTag("layer_handle_$id",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        return handle.center-list.topLeft
    }
}

class LayerReorderGestureTest : LayerReorderFixture() {
    @Test fun plainTapDoesNotMoveButLongPressDragCommitsExactlyOneActionAndChangesRealRenderOrder() = withLayers(4) {repo,id ->
        compose.onAllNodesWithContentDescription("Move Up").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Move Down").assertCountEquals(0)
        val initial=listOf("l0","l1","l2","l3")
        compose.onNodeWithTag("layer_handle_l0",useUnmergedTree=true).performTouchInput {click()}
        assertEquals(initial,saved(repo,id).document.layers.map{it.id})
        assertEquals(Color.RED,LayerCompositor.compositeDocument(saved(repo,id).document).getPixel(100,100))
        val start=point("l0");val end=point("l2")
        compose.onNodeWithTag("layer_list").performTouchInput {
            down(start);advanceEventTime(700);moveTo(end,300);up()
        }
        val reordered=listOf("l1","l2","l0","l3")
        waitForOrder(repo,id,reordered)
        val snapshot=saved(repo,id)
        assertEquals(2,snapshot.historySnapshot!!.nodes.size)
        assertEquals("Reorder Layers",snapshot.historySnapshot!!.nodes[snapshot.historySnapshot!!.currentNodeId]!!.description)
        assertEquals(Color.BLUE,LayerCompositor.compositeDocument(snapshot.document).getPixel(100,100))
        compose.onNodeWithContentDescription("Undo").performClick()
        waitForOrder(repo,id,initial)
        compose.onNodeWithContentDescription("Redo").performClick()
        waitForOrder(repo,id,reordered)
    }
}

class LayerAutoScrollGestureTest : LayerReorderFixture() {
    @Test fun stationaryEdgeDragCrossesRecycledRowsAndCancellationDoesNotReorder() = withLayers(30) {repo,id ->
        val start=point("l0")
        val list=compose.onNodeWithTag("layer_list")
        val size=list.fetchSemanticsNode().size
        val edge=Offset(start.x,size.height-3f)
        list.performTouchInput {down(start);advanceEventTime(700);moveTo(edge,300)}
        compose.mainClock.advanceTimeBy(2400)
        // A real long-lived pointer is still held while the LazyColumn recycles the source row.
        list.performTouchInput {moveTo(edge,16);up()}
        compose.waitUntil(10000) {saved(repo,id).document.layers.indexOfFirst{it.id=="l0"}>5}
        val after=saved(repo,id)
        assertEquals(2,after.historySnapshot!!.nodes.size)
        assertEquals(30,after.document.layers.map{it.id}.toSet().size)
        // Cancellation must discard feedback, never issue a second history command.
        compose.onNodeWithTag("layer_scroll").performScrollToIndex(0)
        val first=after.document.layers.first().id
        val from=point(first)
        list.performTouchInput {down(from);advanceEventTime(700);moveBy(Offset(0f,60f));cancel()}
        compose.waitForIdle()
        assertEquals(after.document.layers,saved(repo,id).document.layers)
        compose.onAllNodesWithTag("layer_drag_feedback").assertCountEquals(0)
    }
}

class LayerGroupingHistoryTest : LayerReorderFixture() {
    @Test fun multiSelectGroupUngroupAndUndoUseRealButtonsAndPreserveTopFirstChildren() = withLayers(4) {repo,id ->
        compose.onNodeWithTag("layer_select_l1",useUnmergedTree=true).performClick()
        compose.onNodeWithTag("layer_select_l2",useUnmergedTree=true).performClick()
        compose.onNodeWithContentDescription("Group Layers").performScrollTo().performClick()
        compose.waitUntil(10000) {saved(repo,id).document.layers.any{it.type==LayerType.GROUP}}
        val grouped=saved(repo,id)
        val group=grouped.document.layers[1]
        assertEquals(listOf("l1","l2"),group.children.map{it.id})
        assertEquals(listOf("l0",group.id,"l3"),grouped.document.layers.map{it.id})
        assertEquals(2,grouped.historySnapshot!!.nodes.size)
        compose.onNodeWithContentDescription("Ungroup Layers").performScrollTo().performClick()
        waitForOrder(repo,id,listOf("l0","l1","l2","l3"))
        assertEquals(3,saved(repo,id).historySnapshot!!.nodes.size)
        compose.onNodeWithContentDescription("Undo").performClick()
        compose.waitUntil(10000) {saved(repo,id).document.layers==grouped.document.layers}
        compose.onNodeWithContentDescription("Redo").performClick()
        waitForOrder(repo,id,listOf("l0","l1","l2","l3"))
    }
}
