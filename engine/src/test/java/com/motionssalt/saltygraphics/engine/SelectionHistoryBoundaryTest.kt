package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.action.*
import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class SelectionHistoryBoundaryTest {
    @Test fun thousandsOfSelectionOnlyCommandsNeverChangeCurrentNodeOrBranch() {
        val a=Layer(id="a");val b=Layer(id="b")
        val tree=ActionTree(ProjectDocument(layers=listOf(a,b)))
        val fork=tree.recordAction("Move",tree.currentDocument.updateLayer("a") {it.copy(transform=Transform2D(translationX=20f))})
        tree.recordAction("Delete",tree.currentDocument.removeLayer("b"));tree.undo()
        tree.recordAction("Rename",tree.currentDocument.updateLayer("a") {it.copy(name="Changed")});tree.undo()
        val baseline=tree.toSnapshot()
        repeat(4000) { i ->
            val ids=when(i%4) {0->setOf("a");1->setOf("b");2->setOf("a","b");else->emptySet()}
            val result=tree.recordAction("Select layer",tree.currentDocument.copy(selectedLayerIds=ids,updatedAt=10000L+i))
            assertEquals(fork.id,result.id)
            assertEquals(baseline,tree.toSnapshot())
            assertTrue(tree.currentDocument.selectedLayerIds.isEmpty())
        }
    }

    @Test fun legacySnapshotSelectionIsDiscardedWithoutLosingDocumentsBranchesOrNodeIds() {
        val selected=ProjectDocument(layers=listOf(Layer(id="a")),selectedLayerIds=setOf("a"))
        val tree=ActionTree(selected)
        assertTrue(tree.currentDocument.selectedLayerIds.isEmpty())
        tree.recordAction("Rename",selected.copy(name="New"))
        val original=tree.toSnapshot()
        val legacy=original.copy(nodes=original.nodes.mapValues {(_,node)->node.copy(snapshot=node.snapshot?.copy(selectedLayerIds=setOf("ghost")))})
        val restored=ActionTree.fromSnapshot(legacy,selected)
        assertEquals(original,restored.toSnapshot())
        assertEquals("New",restored.currentDocument.name)
        assertTrue(restored.undo());assertEquals(selected.copy(selectedLayerIds=emptySet()),restored.currentDocument)
        assertTrue(restored.redo());assertTrue(restored.currentDocument.selectedLayerIds.isEmpty())
    }
}
