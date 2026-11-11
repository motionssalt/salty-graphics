package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.engine.action.*
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.history.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class HistoryTreeAcceptanceTest {
    private fun flags(tree:ActionTree) {
        val graph=HistoryGraphCalculator.computeGraph(tree.nodes,tree.rootNodeId,tree.currentNodeId)
        val path=tree.getCurrentBranchPath().map {it.id}.toSet()
        assertEquals(tree.nodes.size,graph.nodes.size)
        graph.nodes.forEach {
            assertEquals(it.node.id in path,it.isOnMainPath)
            assertEquals(it.node.id !in path,it.isUndone)
            assertEquals(it.node.id==tree.currentNodeId,it.isCurrent)
            assertEquals(it.node.childrenIds.size>1,it.isBranchPoint)
        }
        graph.edges.forEach {assertEquals(it.fromNodeId in path && it.toNodeId in path,it.isOnMainPath)}
    }
    @Test fun createMoveDeleteCreateUndoRedoRestoresEveryStateAndPathFlag() {
        val tree=ActionTree(ProjectDocument())
        val first=Layer(id="first")
        val created=tree.recordAction("Create",tree.currentDocument.addLayer(first));flags(tree)
        val moved=tree.recordAction("Move",tree.currentDocument.updateLayer(first.id) {it.copy(transform=Transform2D(translationX=123f))});flags(tree)
        val deleted=tree.recordAction("Delete",tree.currentDocument.removeLayer(first.id));flags(tree)
        val second=Layer(id="second")
        val recreated=tree.recordAction("Create",tree.currentDocument.addLayer(second));flags(tree)
        assertTrue(tree.undo());assertEquals(deleted.id,tree.currentNodeId);assertTrue(tree.currentDocument.layers.isEmpty());flags(tree)
        assertTrue(tree.redo());assertEquals(recreated.id,tree.currentNodeId);assertEquals(listOf(second),tree.currentDocument.layers);flags(tree)
        GlobalClipboard.copy(second)
        assertEquals(123f,tree.jumpTo(moved.id)!!.layers.single().transform.translationX,.001f);flags(tree)
        assertEquals(first,tree.jumpTo(created.id)!!.layers.single());flags(tree)
        assertEquals(second,GlobalClipboard.copiedLayer)
    }
    @Test fun severalUndosCreateBranchesAndRedoFollowsLastVisitedRatherThanNewestChild() {
        val tree=ActionTree(ProjectDocument())
        val a=tree.recordAction("Create",tree.currentDocument.addLayer(Layer(id="a")))
        val b=tree.recordAction("Move",tree.currentDocument.copy(name="Moved"))
        val c=tree.recordAction("Delete",tree.currentDocument.removeLayer("a"))
        tree.undo();tree.undo();flags(tree)
        val d=tree.recordAction("Alternate",tree.currentDocument.copy(name="Alternate"));flags(tree)
        assertEquals(listOf(b.id,d.id),tree.nodes.getValue(a.id).childrenIds)
        tree.jumpTo(c.id);tree.undo();tree.undo();flags(tree)
        assertTrue(tree.redo());assertEquals(b.id,tree.currentNodeId)
        assertTrue(tree.redo());assertEquals(c.id,tree.currentNodeId);flags(tree)
        tree.jumpTo(d.id);tree.undo();assertTrue(tree.redo());assertEquals(d.id,tree.currentNodeId);flags(tree)
        val encoded=Json.encodeToString(tree.toSnapshot())
        val restored=ActionTree.fromSnapshot(Json.decodeFromString<ActionTreeSnapshot>(encoded),tree.currentDocument)
        restored.undo();assertTrue(restored.redo());assertEquals(d.id,restored.currentNodeId);flags(restored)
        // Old archives have no preferredChildren field: the newest child is a safe fallback.
        val old=tree.toSnapshot().copy(currentNodeId=a.id,preferredChildren=emptyMap())
        val legacy=ActionTree.fromSnapshot(old,tree.currentDocument)
        assertTrue(legacy.redo());assertEquals(d.id,legacy.currentNodeId)
    }
    @Test fun viewportZoomKeepsFocusAndHundredsOfNodesAreCulled() {
        val tree=ActionTree(ProjectDocument())
        repeat(800) {tree.recordAction("Edit $it",tree.currentDocument.copy(name="State $it"))}
        val graph=HistoryGraphCalculator.computeGraph(tree.nodes,tree.rootNodeId,tree.currentNodeId)
        val current=graph.nodes.single {it.isCurrent}
        var view=HistoryViewport().centered(HistoryGeometry.x(current)+100f,HistoryGeometry.y(current)+24f,400f,600f)
        assertTrue(HistoryGeometry.visible(current,view,400f,600f))
        val visible=graph.nodes.filter {HistoryGeometry.visible(it,view,400f,600f)}
        assertTrue(visible.size<20);assertFalse(HistoryGeometry.visible(graph.nodes.first(),view,400f,600f))
        val x=view.worldX(180f);val y=view.worldY(210f)
        view=view.zoomAt(1.5f,180f,210f)
        assertEquals(x,view.worldX(180f),.01f);assertEquals(y,view.worldY(210f),.01f)
        val before=view;view=view.pan(47f,-31f)
        assertEquals(before.panX+47f,view.panX,.001f);assertEquals(before.panY-31f,view.panY,.001f)
        assertEquals(view,view.zoomAt(Float.NaN,0f,0f))
    }
}
