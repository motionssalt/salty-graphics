package com.motionssalt.saltygraphics.uikit

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.model.SelectionBounds
import com.motionssalt.saltygraphics.engine.model.ShapeData
import com.motionssalt.saltygraphics.engine.model.Transform2D
import com.motionssalt.saltygraphics.uikit.registry.OverlayRegistry
import com.motionssalt.saltygraphics.uikit.registry.OverlayRenderContext
import com.motionssalt.saltygraphics.uikit.registry.SaltyBuiltInLibraryRegistry
import com.motionssalt.saltygraphics.uikit.registry.ScaffoldCanvasOverlay
import com.motionssalt.saltygraphics.uikit.registry.SelectionOverlayBindings
import com.motionssalt.saltygraphics.uikit.tokens.SaltyGraphicsTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h412dp-mdpi")
class RegisteredSelectionOverlayTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun cornerScaleAndBodyTranslateCommitLabelledActionsAndKeepCenterOnScale() {
        SaltyBuiltInLibraryRegistry.ensureRegistered()
        val layer0 = Layer(
            id = "sel-1",
            name = "Card",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 200f, height = 100f),
            transform = Transform2D(translationX = 0f, translationY = 0f, scaleX = 1f, scaleY = 1f)
        )
        val doc = ProjectDocument(canvasSize = CanvasSize(400, 400), layers = listOf(layer0))
        var activeLayer by mutableStateOf(layer0)
        val commits = mutableListOf<Pair<ActionIdentity, Transform2D>>()
        var cancelledCount = 0

        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
                Box(Modifier.requiredSize(400.dp, 400.dp)) {
                    ScaffoldCanvasOverlay(
                        id = "selection_box",
                        context = OverlayRenderContext(
                            document = doc,
                            layer = activeLayer,
                            canvasSize = doc.canvasSize,
                            zoom = 1f,
                            pan = Offset.Zero,
                            selectionEditing = SelectionOverlayBindings(
                                bounds = SelectionBounds(-100f, -50f, 100f, 50f),
                                handleGesturesInternally = true,
                                onPreviewTransform = { activeLayer = activeLayer.copy(transform = it) },
                                onCommitTransform = { id, t ->
                                    commits += id to t
                                    activeLayer = activeLayer.copy(transform = t)
                                },
                                onCancelTransform = {
                                    cancelledCount++
                                    activeLayer = layer0
                                }
                            )
                        )
                    )
                }
            }
        }

        try {
            // Viewport is 400x400, canvas 400x400, fit = 0.94, so halfW = 100 * 0.94 = 94, halfH = 50 * 0.94 = 47
            // Drag bottom-right corner outward -> scales around center (translation remains 0,0)
            compose.onNodeWithTag("registered_overlay_selection_box").performTouchInput {
                val br = Offset(center.x + 94f, center.y + 47f)
                down(br)
                moveTo(br + Offset(30f, 15f), 200)
                up()
            }
            compose.runOnIdle {
                assertEquals(1, commits.size)
                assertEquals("Scale", commits[0].first.verb)
                assertTrue("Scale X should increase: ${commits[0].second.scaleX}", commits[0].second.scaleX > 1f)
                assertEquals("Center X must stay fixed during center-anchored scale", 0f, commits[0].second.translationX, 0.01f)
                assertEquals("Center Y must stay fixed during center-anchored scale", 0f, commits[0].second.translationY, 0.01f)
            }

            // Drag body -> translates layer and commits Move
            compose.onNodeWithTag("registered_overlay_selection_box").performTouchInput {
                down(center)
                moveTo(center + Offset(40f, -20f), 200)
                up()
            }
            compose.runOnIdle {
                assertEquals(2, commits.size)
                assertEquals("Move", commits[1].first.verb)
                assertTrue("Translation X should be positive", commits[1].second.translationX > 10f)
                assertTrue("Translation Y should be negative", commits[1].second.translationY < -5f)
            }
        } finally {
            host.pause().stop().destroy()
        }
    }

    @Test
    fun lockedSelectionIgnoresDragAndDisposalRollsBackInFlightDrag() {
        SaltyBuiltInLibraryRegistry.ensureRegistered()
        val lockedLayer = Layer(
            id = "locked-1",
            name = "Locked",
            type = LayerType.SHAPE,
            isLocked = true,
            shapeData = ShapeData(width = 200f, height = 100f)
        )
        val doc = ProjectDocument(canvasSize = CanvasSize(400, 400), layers = listOf(lockedLayer))
        var activeLayer by mutableStateOf(lockedLayer)
        var visible by mutableStateOf(true)
        var commits = 0
        var cancels = 0

        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
                Box(Modifier.requiredSize(400.dp, 400.dp)) {
                    if (visible) {
                        ScaffoldCanvasOverlay(
                            id = "selection_box",
                            context = OverlayRenderContext(
                                document = doc,
                                layer = activeLayer,
                                canvasSize = doc.canvasSize,
                                zoom = 1f,
                                pan = Offset.Zero,
                                selectionEditing = SelectionOverlayBindings(
                                    bounds = SelectionBounds(-100f, -50f, 100f, 50f),
                                    handleGesturesInternally = true,
                                    onPreviewTransform = { activeLayer = activeLayer.copy(transform = it) },
                                    onCommitTransform = { _, _ -> commits++ },
                                    onCancelTransform = { cancels++ }
                                )
                            )
                        )
                    }
                }
            }
        }

        try {
            compose.onNodeWithTag("registered_overlay_selection_box").performTouchInput {
                down(center)
                moveTo(center + Offset(40f, 40f), 150)
                up()
            }
            compose.runOnIdle { assertEquals(0, commits) }

            // Unlock, start drag without lifting, dispose overlay -> must roll back
            activeLayer = lockedLayer.copy(isLocked = false)
            compose.waitForIdle()
            compose.onNodeWithTag("registered_overlay_selection_box").performTouchInput {
                down(center)
                moveTo(center + Offset(30f, 0f), 100)
            }
            compose.runOnIdle { visible = false }
            compose.waitForIdle()
            assertEquals("Disposal during active drag must not commit", 0, commits)
            assertTrue("Disposal during active drag must cancel", cancels >= 1)
        } finally {
            host.pause().stop().destroy()
        }
    }
}
