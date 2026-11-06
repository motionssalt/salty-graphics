package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.action.GlobalClipboard
import com.motionssalt.saltygraphics.engine.action.PasteMode
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.model.Transform2D
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ActionTreeTest {

    @Before
    fun setup() {
        GlobalClipboard.clear()
    }

    @Test
    fun testActionTreeBranching() {
        val doc0 = ProjectDocument(name = "Initial")
        val tree = ActionTree(doc0)

        // Action 1 on Branch A
        val layerA = Layer(id = "layerA", name = "Layer A", type = LayerType.SHAPE)
        val doc1 = doc0.addLayer(layerA)
        val node1 = tree.recordAction("Add Layer A", doc1)

        // Action 2 on Branch A
        val layerB = Layer(id = "layerB", name = "Layer B", type = LayerType.TEXT)
        val doc2 = doc1.addLayer(layerB)
        tree.recordAction("Add Layer B", doc2)

        assertEquals(2, tree.currentDocument.layers.size)

        // Undo back to node1
        assertTrue(tree.undo())
        assertEquals(node1.id, tree.currentNodeId)
        assertEquals(1, tree.currentDocument.layers.size)

        // New action from node1 creates Branch B!
        val layerC = Layer(id = "layerC", name = "Layer C", type = LayerType.MEDIA)
        val doc3 = tree.currentDocument.addLayer(layerC)
        val node3 = tree.recordAction("Add Layer C (Branch B)", doc3)

        // Current document now has Layer A and Layer C
        val currentLayers = tree.currentDocument.layers.map { it.name }
        assertTrue(currentLayers.contains("Layer A"))
        assertTrue(currentLayers.contains("Layer C"))
        assertFalse(currentLayers.contains("Layer B"))

        // Jumping back to node1 restores only Layer A
        tree.jumpTo(node1.id)
        assertEquals(1, tree.currentDocument.layers.size)
        assertEquals("Layer A", tree.currentDocument.layers[0].name)
    }

    @Test
    fun testClipboardSurvivesBranchJumps() {
        val doc0 = ProjectDocument(name = "Base")
        val tree = ActionTree(doc0)

        // Branch 1: Add Layer with specific transform
        val specialLayer = Layer(
            id = "special",
            name = "Special",
            transform = Transform2D(translationX = 120f, translationY = 240f, scaleX = 2.5f, scaleY = 2.5f)
        )
        val doc1 = doc0.addLayer(specialLayer)
        val node1 = tree.recordAction("Add Special Layer", doc1)

        // Copy this layer to global clipboard
        GlobalClipboard.copy(specialLayer)
        assertTrue(GlobalClipboard.hasContent())

        // Jump to root or create branch 2
        tree.jumpTo("root")
        assertEquals(0, tree.currentDocument.layers.size)

        // Create target layer on Branch 2
        val targetLayer = Layer(
            id = "target",
            name = "Target Layer",
            transform = Transform2D(translationX = 0f, translationY = 0f, scaleX = 1f, scaleY = 1f)
        )
        val docBranch2 = tree.currentDocument.addLayer(targetLayer)
        tree.recordAction("Add Target", docBranch2)

        // Clipboard survived the jump!
        assertNotNull(GlobalClipboard.copiedLayer)
        assertEquals("Special", GlobalClipboard.copiedLayer?.name)

        // Paste onto target
        val pasted = GlobalClipboard.applyTo(targetLayer, PasteMode.TRANSFORM_ONLY)
        assertEquals(120f, pasted.transform.translationX, 0.001f)
        assertEquals(240f, pasted.transform.translationY, 0.001f)
        assertEquals(2.5f, pasted.transform.scaleX, 0.001f)
        assertEquals(2.5f, pasted.transform.scaleY, 0.001f)
    }
}
