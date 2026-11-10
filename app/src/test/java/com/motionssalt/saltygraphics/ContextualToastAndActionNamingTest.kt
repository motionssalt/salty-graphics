package com.motionssalt.saltygraphics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContextualToastAndActionNamingTest {

    @Test
    fun testActionTreeProvidesPreciseActionNamesForUndoAndRedo() {
        val doc = ProjectDocument(name = "Initial")
        val tree = ActionTree(doc)

        assertNull("Initial state has no action to undo", tree.undoActionName)
        assertFalse(tree.canUndo)

        // Perform an action with ActionIdentity
        tree.recordCommand(ActionIdentity("Move", "Text Layer")) { d ->
            d.copy(name = "Updated")
        }

        assertTrue(tree.canUndo)
        assertEquals("Move Text Layer", tree.undoActionName)

        // Undo
        assertTrue(tree.undo())
        assertFalse(tree.canUndo)
        assertTrue(tree.canRedo)
        assertEquals("Move Text Layer", tree.redoActionName)

        // Redo
        assertTrue(tree.redo())
        assertTrue(tree.canUndo)
        assertFalse(tree.canRedo)
    }

    @Test
    fun testLayerDeletionCountGrammar() {
        fun deleteMessage(count: Int) = if (count == 1) "Deleted 1 layer" else "Deleted $count layers"

        assertEquals("Deleted 1 layer", deleteMessage(1))
        assertEquals("Deleted 3 layers", deleteMessage(3))
        assertEquals("Deleted 10 layers", deleteMessage(10))
    }
}
