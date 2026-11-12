package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.TextLayoutEngine
import com.motionssalt.saltygraphics.ui.editor.CanvasView
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextFrameWrappingBoxTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun draggingTextFrameHandlesInTextEditingModeResizesBoxAndWrapsText() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            val sentence = "The quick brown fox jumps over the lazy dog and runs freely across the field"
            val initialTextData = TextLayerData(
                text = sentence,
                fontSize = 24f,
                boxWidth = 500f,
                boxHeight = 200f,
                boxMode = TextBoxMode.FREE
            )
            val textLayer = Layer(
                id = "text_test_layer",
                name = "Test Text",
                type = LayerType.TEXT,
                textData = initialTextData,
                transform = Transform2D(translationX = 0f, translationY = 0f)
            )
            var currentDoc by mutableStateOf(
                ProjectDocument(
                    id = "text_box_doc",
                    canvasSize = CanvasSize(800, 800),
                    layers = listOf(textLayer)
                )
            )
            var committedActions = mutableListOf<String>()

            host.get().setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(400.dp, 400.dp)) {
                        CanvasView(
                            document = currentDoc,
                            selectedLayerIds = setOf(textLayer.id),
                            zoom = 1f,
                            panOffset = Offset.Zero,
                            showGrid = false,
                            gridSize = 20,
                            onZoomPanChange = { _, _ -> },
                            onSelectLayer = { },
                            onUpdateLayerTransform = { _, _ -> },
                            isTextEditing = true, // Text editing mode active
                            onUpdateTextData = { id, updated ->
                                currentDoc = currentDoc.updateLayer(id) { it.copy(textData = updated) }
                            },
                            onCommitTextData = { _, _ ->
                                committedActions.add("Resize text box")
                            }
                        )
                    }
                }
            }

            compose.waitForIdle()

            val canvas = compose.onNodeWithTag("editor_canvas")
            val canvasBounds = canvas.fetchSemanticsNode().size
            val center = Offset(canvasBounds.width / 2f, canvasBounds.height / 2f)

            // At 1f zoom and 800x800 canvas in 400x400 view, fit = 400/800 * 0.94 = 0.47
            val fit = (400f / 800f) * 0.94f
            // Right edge handle is at center.x + halfW * fit.
            // HalfW of 500f boxWidth is 250f.
            val rightHandleX = center.x + (250f * fit)
            val rightHandleY = center.y

            // Drag right handle outward by 50px on screen
            canvas.performTouchInput {
                down(Offset(rightHandleX, rightHandleY))
                moveBy(Offset(40f, 0f), 100)
                up()
            }

            compose.waitForIdle()

            val updatedLayer = currentDoc.findLayerById(textLayer.id)
            assertNotNull(updatedLayer)
            val updatedText = updatedLayer!!.textData!!

            // Box width must have expanded
            assertTrue("Box width must have increased from 500f, was: ${updatedText.boxWidth}", updatedText.boxWidth > 500f)
            assertEquals(TextBoxMode.PARAGRAPH, updatedText.boxMode)
            assertTrue("Commit action must be recorded", committedActions.contains("Resize text box"))

            // Verify that re-wrapping text reflects new boxWidth in layout engine
            val paint = android.graphics.Paint().apply { textSize = updatedText.fontSize }
            val layout = TextLayoutEngine.computeLayout(updatedText, paint)
            assertTrue("Layout should have valid lines", layout.lines.isNotEmpty())
        } finally {
            host.pause().stop().destroy()
        }
    }
}
