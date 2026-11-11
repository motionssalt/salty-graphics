package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.ui.editor.ActionTreeDialog
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryTreeViewportTest {
    @get:Rule val compose=createEmptyComposeRule()
    @Test fun realTreeCentersCurrentCullsOffscreenNodesAndSupportsPanPinchAndIntentionalJump() {
        val tree=ActionTree(ProjectDocument())
        repeat(500) {tree.recordAction("Edit $it",tree.currentDocument.copy(name="State $it"))}
        val current=tree.currentNodeId
        var jumped:String?=null
        val host=Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            host.get().setContent {MaterialTheme {ActionTreeDialog(tree,{jumped=it},{})}}
            compose.onNodeWithText("Actions: 501").assertIsDisplayed()
            val node=compose.onNodeWithTag("history_node_$current")
            node.assertIsDisplayed().assertIsSelected()
            compose.onNodeWithTag("history_node_root").assertDoesNotExist()
            val before=node.fetchSemanticsNode().boundsInRoot
            val viewport=compose.onNodeWithTag("history_viewport")
            viewport.performTouchInput {swipe(center,center+Offset(35f,35f),250)}
            val after=node.fetchSemanticsNode().boundsInRoot
            assertTrue(after.left>before.left+10f);assertTrue(after.top>before.top+10f)
            assertNull(jumped)
            val oldZoom=viewport.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            viewport.performTouchInput {
                down(0,center-Offset(50f,0f));down(1,center+Offset(50f,0f))
                moveTo(0,center-Offset(85f,0f),200);moveTo(1,center+Offset(85f,0f),200)
                up(0);up(1)
            }
            assertNotEquals(oldZoom,viewport.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
            assertNull(jumped)
            compose.onNodeWithContentDescription("Current action").performClick()
            compose.onNodeWithTag("history_node_$current").assertIsDisplayed().performClick()
            assertEquals(current,jumped)
        } finally {host.pause().stop().destroy()}
    }
}
