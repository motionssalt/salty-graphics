package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.action.*
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

/**
 * Verification suite for V3:
 * (a) Action labels for every command type generated from structured data
 * (b) Branch correctness across undo, redo, several undos and new action branching
 * (c) Grid-color-then-size reproduction (keeps black without reverting to gray)
 * (d) Property test applying random sequences of setting changes across undo, redo and branch jumps
 */
class HistoryActionIdentityAndBranchingTest {

    @Test
    fun testActionLabelsForEveryCommandType() {
        // 1. Layer operations
        val moveText = ActionIdentity("Move", "text")
        assertEquals("Move text", moveText.toLabel())

        val addLayer = ActionIdentity.of(ActionVerb.ADD, "Layer", "shape")
        assertEquals("Add Layer shape", addLayer.toLabel())

        val deleteLayer = ActionIdentity.of(ActionVerb.DELETE, "Text Layer")
        assertEquals("Delete Text Layer", deleteLayer.toLabel())

        val duplicateLayer = ActionIdentity.of(ActionVerb.DUPLICATE, "Layer 1")
        assertEquals("Duplicate Layer 1", duplicateLayer.toLabel())

        val reorderLayers = ActionIdentity.of(ActionVerb.REORDER, "layers")
        assertEquals("Reorder layers", reorderLayers.toLabel())

        val groupLayers = ActionIdentity.of(ActionVerb.GROUP, "layers")
        assertEquals("Group layers", groupLayers.toLabel())

        val ungroupLayers = ActionIdentity.of(ActionVerb.UNGROUP, "layers")
        assertEquals("Ungroup layers", ungroupLayers.toLabel())

        // 2. Effect control operations
        val increaseGlowThreshold = ActionIdentity.forNumericChange("Glow", "threshold", 0.2f, 0.8f)
        assertEquals("Increase Glow threshold", increaseGlowThreshold.toLabel())

        val decreaseGlowThreshold = ActionIdentity.forNumericChange("Glow", "threshold", 0.8f, 0.3f)
        assertEquals("Decrease Glow threshold", decreaseGlowThreshold.toLabel())

        val enableEffect = ActionIdentity.forBooleanChange("Glow", "processing", true)
        assertEquals("Enable Glow processing", enableEffect.toLabel())

        val disableEffect = ActionIdentity.forBooleanChange("Glow", "processing", false)
        assertEquals("Disable Glow processing", disableEffect.toLabel())

        val changeTint = ActionIdentity("Change", "Glow", "tint color")
        assertEquals("Change Glow tint color", changeTint.toLabel())

        val setMode = ActionIdentity("Set", "Glow", "blend mode")
        assertEquals("Set Glow blend mode", setMode.toLabel())

        // 3. Settings operations
        val changeGridColor = ActionIdentity("Change", "Grid", "color")
        assertEquals("Change Grid color", changeGridColor.toLabel())

        val increaseGridSize = ActionIdentity.forNumericChange("Grid", "size", 20f, 50f)
        assertEquals("Increase Grid size", increaseGridSize.toLabel())

        val toggleSnap = ActionIdentity.of(ActionVerb.TOGGLE, "snap")
        assertEquals("Toggle snap", toggleSnap.toLabel())

        val changeCanvasColor = ActionIdentity("Change", "Canvas", "color")
        assertEquals("Change Canvas color", changeCanvasColor.toLabel())

        // 4. Inferred descriptions
        val inferredMove = ActionIdentity.inferFromDescription("Move text")
        assertEquals("Move", inferredMove.verb)
        assertEquals("text", inferredMove.target)

        val inferredIncrease = ActionIdentity.inferFromDescription("Increase glow threshold")
        assertEquals("Increase", inferredIncrease.verb)
        assertEquals("glow", inferredIncrease.target)
        assertEquals("threshold", inferredIncrease.property)
    }

    @Test
    fun testBranchCorrectnessAcrossCreateMoveDeleteCreateUndoRedo() {
        val doc0 = ProjectDocument(name = "Initial")
        val tree = ActionTree(doc0)

        // 1. Create layer A
        val layerA = Layer(id = "layerA", name = "Layer A", type = LayerType.SHAPE)
        val nodeCreateA = tree.recordCommand(ActionIdentity("Create", "Layer A")) { it.addLayer(layerA) }
        assertEquals("Create Layer A", nodeCreateA.description)
        assertEquals(1, tree.currentDocument.layers.size)

        // 2. Move layer A
        val nodeMoveA = tree.recordCommand(ActionIdentity("Move", "Layer A")) {
            it.updateLayer("layerA") { l -> l.copy(transform = Transform2D(translationX = 100f)) }
        }
        assertEquals("Move Layer A", nodeMoveA.description)
        assertEquals(100f, tree.currentDocument.findLayerById("layerA")?.transform?.translationX ?: 0f, 0.001f)

        // 3. Delete layer A
        val nodeDeleteA = tree.recordCommand(ActionIdentity("Delete", "Layer A")) { it.removeLayer("layerA") }
        assertEquals("Delete Layer A", nodeDeleteA.description)
        assertEquals(0, tree.currentDocument.layers.size)

        // 4. Create layer B
        val layerB = Layer(id = "layerB", name = "Layer B", type = LayerType.TEXT)
        val nodeCreateB = tree.recordCommand(ActionIdentity("Create", "Layer B")) { it.addLayer(layerB) }
        assertEquals("Create Layer B", nodeCreateB.description)
        assertEquals(1, tree.currentDocument.layers.size)
        assertEquals("layerB", tree.currentDocument.layers.first().id)

        // 5. Undo back to deleted state
        assertTrue(tree.undo())
        assertEquals(nodeDeleteA.id, tree.currentNodeId)
        assertEquals(0, tree.currentDocument.layers.size)

        // 6. Redo back to layer B
        assertTrue(tree.redo())
        assertEquals(nodeCreateB.id, tree.currentNodeId)
        assertEquals(1, tree.currentDocument.layers.size)
        assertEquals("layerB", tree.currentDocument.layers.first().id)

        // 7. Undo several times back to after Move Layer A
        assertTrue(tree.undo()) // at Delete A
        assertTrue(tree.undo()) // at Move A
        assertEquals(nodeMoveA.id, tree.currentNodeId)
        assertEquals(1, tree.currentDocument.layers.size)
        assertEquals("layerA", tree.currentDocument.layers.first().id)

        // 8. New action from this earlier point creates a new branch!
        val layerC = Layer(id = "layerC", name = "Layer C", type = LayerType.MEDIA)
        val nodeCreateC = tree.recordCommand(ActionIdentity("Create", "Layer C")) { it.addLayer(layerC) }
        assertEquals("Create Layer C", nodeCreateC.description)

        // Node C must be a direct child of Node Move A
        assertEquals(nodeMoveA.id, nodeCreateC.parentId)
        val moveNodeChildren = tree.nodes.getValue(nodeMoveA.id).childrenIds
        assertTrue("Move A must have both Delete A and Create C as children", moveNodeChildren.contains(nodeDeleteA.id))
        assertTrue("Move A must have both Delete A and Create C as children", moveNodeChildren.contains(nodeCreateC.id))

        // State on the new branch must have Layer A and Layer C, and NEVER Layer B
        val branchLayers = tree.currentDocument.layers.map { it.id }
        assertTrue(branchLayers.contains("layerA"))
        assertTrue(branchLayers.contains("layerC"))
        assertFalse("New branch must not contain Layer B from the alternate branch", branchLayers.contains("layerB"))

        // Jumping back to Node Create B restores Layer B and not Layer C
        tree.jumpTo(nodeCreateB.id)
        assertEquals(1, tree.currentDocument.layers.size)
        assertEquals("layerB", tree.currentDocument.layers.first().id)

        // Jumping back to Node Create C restores Layer A and Layer C
        tree.jumpTo(nodeCreateC.id)
        assertEquals(2, tree.currentDocument.layers.size)
        assertEquals("layerA", tree.currentDocument.layers[0].id)
        assertEquals("layerC", tree.currentDocument.layers[1].id)
    }

    @Test
    fun testGridColorThenSizeReproductionKeepsBlack() {
        val initialDoc = ProjectDocument(
            name = "Grid Test",
            editorSettings = EditorSettings(
                gridColor = 0x66808080L, // Initial gray
                gridSize = 20,
                showGrid = true
            )
        )
        val tree = ActionTree(initialDoc)

        // Step 1: Set grid-line color to black
        val blackColor = 0xFF000000L
        tree.recordCommand(ActionIdentity("Change", "Grid", "color")) { doc ->
            doc.updateEditorSettings { it.copy(gridColor = blackColor) }
        }
        assertEquals("Grid color must now be black", blackColor, tree.currentDocument.editorSettings.gridColor)
        assertEquals("Grid size must still be 20", 20, tree.currentDocument.editorSettings.gridSize)

        // Step 2: Increase grid-line size from 20 to 50
        tree.recordCommand(ActionIdentity("Increase", "Grid", "size")) { doc ->
            doc.updateEditorSettings { it.copy(gridSize = 50) }
        }

        // VERIFY: The reproduction bug must NOT happen! Color must remain black!
        assertEquals("Grid color MUST REMAIN BLACK after increasing grid size", blackColor, tree.currentDocument.editorSettings.gridColor)
        assertEquals("Grid size must be 50", 50, tree.currentDocument.editorSettings.gridSize)

        // Step 3: Undo size increase
        assertTrue(tree.undo())
        assertEquals("Grid size reverts to 20", 20, tree.currentDocument.editorSettings.gridSize)
        assertEquals("Grid color stays black after undoing size", blackColor, tree.currentDocument.editorSettings.gridColor)

        // Step 4: Undo color change
        assertTrue(tree.undo())
        assertEquals("Grid color reverts to gray", 0x66808080L, tree.currentDocument.editorSettings.gridColor)
        assertEquals("Grid size is 20", 20, tree.currentDocument.editorSettings.gridSize)

        // Step 5: Redo color change
        assertTrue(tree.redo())
        assertEquals("Grid color is black again", blackColor, tree.currentDocument.editorSettings.gridColor)
        assertEquals("Grid size is 20", 20, tree.currentDocument.editorSettings.gridSize)

        // Step 6: Redo size increase
        assertTrue(tree.redo())
        assertEquals("Grid color is black again", blackColor, tree.currentDocument.editorSettings.gridColor)
        assertEquals("Grid size is 50", 50, tree.currentDocument.editorSettings.gridSize)
    }

    @Test
    fun testPropertyTestSettingsIndependenceAcrossRandomSequence() {
        val rng = Random(12345)
        var expectedSettings = EditorSettings(
            backgroundColor = 0xFF000000L,
            showGrid = false,
            gridSize = 20,
            gridColor = 0x66808080L,
            isSquareGrid = false,
            snapEnabled = false,
            snapTolerancePx = 8f,
            defaultPreviewQuality = PreviewQuality.HIGH
        )

        val doc0 = ProjectDocument(editorSettings = expectedSettings)
        val tree = ActionTree(doc0)

        val historyStates = mutableListOf<EditorSettings>()
        historyStates.add(expectedSettings)

        // Apply 60 randomized per-property updates
        repeat(60) {
            val propertyIndex = rng.nextInt(8)
            when (propertyIndex) {
                0 -> {
                    val color = 0xFF000000L or (rng.nextLong(0xFFFFFF) and 0xFFFFFFL)
                    expectedSettings = expectedSettings.copy(backgroundColor = color)
                    tree.recordCommand(ActionIdentity("Change", "Canvas", "color")) {
                        it.updateEditorSettings { s -> s.copy(backgroundColor = color) }
                    }
                }
                1 -> {
                    val show = rng.nextBoolean()
                    expectedSettings = expectedSettings.copy(showGrid = show)
                    tree.recordCommand(ActionIdentity(if (show) "Show" else "Hide", "Gridlines")) {
                        it.updateEditorSettings { s -> s.copy(showGrid = show) }
                    }
                }
                2 -> {
                    val size = rng.nextInt(5, 200)
                    val verb = if (size >= expectedSettings.gridSize) "Increase" else "Decrease"
                    expectedSettings = expectedSettings.copy(gridSize = size)
                    tree.recordCommand(ActionIdentity(verb, "Grid", "size")) {
                        it.updateEditorSettings { s -> s.copy(gridSize = size) }
                    }
                }
                3 -> {
                    val color = 0xFF000000L or (rng.nextLong(0xFFFFFF) and 0xFFFFFFL)
                    expectedSettings = expectedSettings.copy(gridColor = color)
                    tree.recordCommand(ActionIdentity("Change", "Grid", "color")) {
                        it.updateEditorSettings { s -> s.copy(gridColor = color) }
                    }
                }
                4 -> {
                    val sq = rng.nextBoolean()
                    expectedSettings = expectedSettings.copy(isSquareGrid = sq)
                    tree.recordCommand(ActionIdentity("Toggle", "Square Grid")) {
                        it.updateEditorSettings { s -> s.copy(isSquareGrid = sq) }
                    }
                }
                5 -> {
                    val snap = rng.nextBoolean()
                    expectedSettings = expectedSettings.copy(snapEnabled = snap)
                    tree.recordCommand(ActionIdentity(if (snap) "Enable" else "Disable", "Snap")) {
                        it.updateEditorSettings { s -> s.copy(snapEnabled = snap) }
                    }
                }
                6 -> {
                    val tol = rng.nextFloat() * 30f + 2f
                    expectedSettings = expectedSettings.copy(snapTolerancePx = tol)
                    tree.recordCommand(ActionIdentity("Adjust", "Snap", "tolerance")) {
                        it.updateEditorSettings { s -> s.copy(snapTolerancePx = tol) }
                    }
                }
                7 -> {
                    val qualities = PreviewQuality.values()
                    val pq = qualities[rng.nextInt(qualities.size)]
                    expectedSettings = expectedSettings.copy(defaultPreviewQuality = pq)
                    tree.recordCommand(ActionIdentity("Change", "Preview Quality")) {
                        it.updateEditorSettings { s -> s.copy(defaultPreviewQuality = pq) }
                    }
                }
            }

            assertEquals(
                "Document editorSettings must exactly match expected after mutation $it",
                expectedSettings,
                tree.currentDocument.editorSettings
            )
            historyStates.add(expectedSettings)
        }

        // Now test undo sequence step by step
        for (i in historyStates.size - 2 downTo 0) {
            assertTrue("Undo must succeed", tree.undo())
            assertEquals(
                "Undoing to step $i must restore exact settings for that step",
                historyStates[i],
                tree.currentDocument.editorSettings
            )
        }

        // Redo back to the tip
        for (i in 1 until historyStates.size) {
            assertTrue("Redo must succeed", tree.redo())
            assertEquals(
                "Redoing to step $i must restore exact settings for that step",
                historyStates[i],
                tree.currentDocument.editorSettings
            )
        }
    }
}
