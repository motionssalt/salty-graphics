package com.motionssalt.saltygraphics.uikit

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
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.uikit.registry.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Exercises the registered library overlay, not an isolated geometry helper. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RegisteredGradientOverlayTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun registeredStartAndEndUseLiveBindingsAtSeveralZoomsAndRotations() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var document by mutableStateOf(ProjectDocument())
        var zoom by mutableFloatStateOf(1f)
        var tree = ActionTree(document)
        try {
            host.get().setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(400.dp, 400.dp)) {
                        document.layers.firstOrNull()?.let { layer ->
                            ScaffoldCanvasOverlay("gradient_handles", OverlayRenderContext(
                                document, layer, CanvasSize(400, 400), zoom, Offset.Zero,
                                GradientOverlayBindings(
                                    onPreview = { fill -> document = document.updateLayer(layer.id) { it.withUniversalFill(fill) } },
                                    onCommit = { identity, fill ->
                                        tree.recordCommand(identity) { live -> live.updateLayer(layer.id) { it.withUniversalFill(fill) } }
                                        document = tree.currentDocument
                                    },
                                    onCancel = { document = tree.currentDocument }
                                )
                            ))
                        }
                    }
                }
            }
            for (z in listOf(.75f, 1f, 2f)) for (angle in listOf(0f, 32f)) {
                val original = Layer(
                    id = "gradient_${z}_$angle", type = LayerType.SHAPE,
                    transform = Transform2D(rotationDegrees = angle, scaleX = 1.1f, scaleY = .8f),
                    shapeData = ShapeData(width = 200f, height = 200f),
                    fillData = FillData(type = FillType.GRADIENT, gradient = GradientData(
                        startX = .2f, startY = .5f, endX = .8f, endY = .5f
                    ))
                )
                compose.runOnIdle {
                    zoom = z
                    document = ProjectDocument(id = original.id, canvasSize = CanvasSize(400, 400), layers = listOf(original))
                    tree = ActionTree(document)
                }
                compose.onNodeWithTag("registered_overlay_gradient_handles").assertExists()
                val node = compose.onNodeWithTag("gradient_handles")
                fun screen(x: Float, y: Float): Offset {
                    val world = SelectionGeometry.world(SelectionPoint(x, y), original.transform)
                    return Offset(200f, 200f) + Offset(world.x, world.y) * (.94f * z)
                }
                node.performTouchInput {
                    down(screen(-60f, 0f))
                    moveTo(screen(-72f, -16f), 100)
                    moveTo(screen(-80f, -24f), 100)
                    up()
                }
                compose.runOnIdle {
                    val g = document.layers.single().effectiveFill.gradient
                    assertEquals(.1f, g.startX!!, .005f)
                    assertEquals(.38f, g.startY!!, .005f)
                    assertEquals(.8f, g.endX!!, 0f)
                    assertEquals(.5f, g.endY!!, 0f)
                    assertEquals(2, tree.nodes.size)
                    assertEquals("Move gradient start", tree.nodes.getValue(tree.currentNodeId).description)
                }
                node.performTouchInput {
                    down(screen(60f, 0f)); moveTo(screen(76f, 24f), 100); up()
                }
                compose.runOnIdle {
                    val finalFill = document.layers.single().effectiveFill
                    assertEquals(.1f, finalFill.gradient.startX!!, .005f)
                    assertEquals(.38f, finalFill.gradient.startY!!, .005f)
                    assertEquals(.88f, finalFill.gradient.endX!!, .005f)
                    assertEquals(.62f, finalFill.gradient.endY!!, .005f)
                    assertEquals(3, tree.nodes.size)
                    assertEquals("Move gradient end", tree.nodes.getValue(tree.currentNodeId).description)
                    tree.undo(); tree.undo()
                    assertEquals(original.effectiveFill, tree.currentDocument.layers.single().effectiveFill)
                    tree.redo(); tree.redo()
                    assertEquals(finalFill, tree.currentDocument.layers.single().effectiveFill)
                }
            }
        } finally { host.pause().stop().destroy() }
    }

    @Test fun cancelAndDisposalRollBackDraftsWithoutCreatingHistory() {
        val layer = Layer(id = "target", type = LayerType.SHAPE,
            shapeData = ShapeData(width = 200f, height = 200f),
            fillData = FillData(type = FillType.GRADIENT, gradient = GradientData(
                startX = .2f, startY = .5f, endX = .8f, endY = .5f)))
        var document by mutableStateOf(ProjectDocument(canvasSize = CanvasSize(400, 400), layers = listOf(layer)))
        val tree = ActionTree(document)
        var visible by mutableStateOf(true)
        var cancellations = 0
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            host.get().setContent {
                MaterialTheme { Box(Modifier.requiredSize(400.dp, 400.dp)) {
                    if (visible) ScaffoldCanvasOverlay("gradient_handles", OverlayRenderContext(
                        document, document.layers.single(), document.canvasSize, 1f, Offset.Zero,
                        GradientOverlayBindings(
                            onPreview = { fill -> document = document.updateLayer(layer.id) { it.withUniversalFill(fill) } },
                            onCommit = { _, _ -> fail("Cancelled or disposed gesture must not commit") },
                            onCancel = { cancellations++; document = tree.currentDocument }
                        )
                    ))
                } }
            }
            val node = compose.onNodeWithTag("gradient_handles")
            val start = Offset(200f - 60f * .94f, 200f)
            node.performTouchInput { down(start); moveTo(start + Offset(-20f, -20f), 100) }
            compose.runOnIdle { assertNotEquals(layer.effectiveFill, document.layers.single().effectiveFill) }
            node.performTouchInput { cancel() }
            compose.runOnIdle {
                assertEquals(layer.effectiveFill, document.layers.single().effectiveFill)
                assertEquals(1, cancellations)
                assertEquals(1, tree.nodes.size)
            }
            node.performTouchInput { down(start); moveTo(start + Offset(-20f, -20f), 100) }
            compose.runOnIdle { assertNotEquals(layer.effectiveFill, document.layers.single().effectiveFill); visible = false }
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals(layer.effectiveFill, document.layers.single().effectiveFill)
                assertEquals(2, cancellations)
                assertEquals(1, tree.nodes.size)
            }
        } finally { host.pause().stop().destroy() }
    }
}
