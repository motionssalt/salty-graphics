package com.motionssalt.saltygraphics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.junit.Before
import org.junit.After
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.ui.editor.*
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h1000dp-mdpi")
class EditorWorkspaceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var controller: ActivityController<ComponentActivity>
    @Before fun startActivity() { controller = Robolectric.buildActivity(ComponentActivity::class.java).setup() }
    @After fun stopActivity() { controller.pause().stop().destroy() }

    @Test fun gridsAndPreviewStayInsideUsableBoundsAcrossScreenClasses() {
        var dimensions by mutableStateOf(320 to 480)
        var selected by mutableStateOf<Layer?>(null)
        controller.get().setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(dimensions.first.dp, dimensions.second.dp)
                    .windowInsetsPadding(WindowInsets(left = 10.dp, top = 24.dp, right = 10.dp, bottom = 32.dp))) {
                    EditorWorkspace(preview = { Text("Canvas") }, panel = {
                        BottomPanel(selected, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
                    })
                }
            }
        }
        for ((width, height) in listOf(320 to 480, 412 to 860, 1000 to 800, 640 to 360)) {
            compose.runOnIdle { dimensions = width to height }
            compose.waitForIdle()
            val canvas = compose.onNodeWithTag("preview").fetchSemanticsNode().boundsInRoot
            val panel = compose.onNodeWithTag("tools").fetchSemanticsNode().boundsInRoot
            assertTrue(canvas.width > 0 && canvas.height > 0)
            if (PanelSizing.sideDock((width - 20).toFloat(), (height - 56).toFloat())) {
                assertTrue(canvas.right <= panel.left + 1)
            } else {
                assertTrue(canvas.bottom <= panel.top + 1)
                assertTrue(panel.height <= (height - 56) * .45f + 1)
            }
            assertTrue(canvas.top >= 24 && panel.top >= 24)
            assertTrue(panel.bottom <= height - 32 + 1)
            listOf("Media", "Text", "Adjustment", "Shape", "Vector").forEach {
                compose.onNodeWithText(it, useUnmergedTree = true).assertIsDisplayed()
            }
            compose.runOnIdle { selected = Layer(type = LayerType.TEXT) }
            listOf("Edit", "Transform", "Effects", "Blending & Mask", "Delete").forEach {
                compose.onNodeWithText(it, useUnmergedTree = true).assertIsDisplayed()
            }
            compose.runOnIdle { selected = null }
        }
    }

    @Test fun deleteIsOneUndoableActionAndRestoresLayer() {
        val layer = Layer(type = LayerType.TEXT, textData = TextLayerData())
        val tree = ActionTree(ProjectDocument(layers = listOf(layer)))
        var doc by mutableStateOf(tree.currentDocument)
        controller.get().setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(320.dp, 260.dp)) {
                    BottomPanel(doc.layers.firstOrNull(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {
                        val updated = doc.removeLayer(layer.id)
                        tree.recordAction("Delete Layers", updated)
                        doc = updated
                    })
                }
            }
        }
        compose.onNodeWithText("Delete").performClick()
        compose.runOnIdle {
            assertTrue(doc.layers.isEmpty())
            assertEquals(2, tree.nodes.size)
            assertTrue(tree.undo())
            doc = tree.currentDocument
            assertEquals(layer, doc.layers.single())
        }
        compose.onNodeWithText("Delete").assertIsDisplayed()
    }
}
