package com.motionssalt.saltygraphics.uikit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
import com.motionssalt.saltygraphics.engine.render.FillRenderer
import com.motionssalt.saltygraphics.uikit.overlays.SaltyGradientLayerOverlay
import com.motionssalt.saltygraphics.uikit.overlays.SaltyGradientOverlay
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
class GradientHandleIndependenceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun linearStartEndAndCenterHandlesMoveIndependently() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var layer by mutableStateOf(
            Layer(
                id = "grad_layer",
                type = LayerType.SHAPE,
                shapeData = ShapeData(width = 200f, height = 200f),
                fillData = FillData(
                    type = FillType.GRADIENT,
                    gradient = GradientData(
                        type = GradientType.LINEAR,
                        startX = 0.2f,
                        startY = 0.5f,
                        endX = 0.8f,
                        endY = 0.5f,
                        centerX = 0.5f,
                        centerY = 0.5f
                    )
                )
            )
        )
        var commits = 0

        try {
            host.get().setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(400.dp, 400.dp)) {
                        SaltyGradientLayerOverlay(
                            layer = layer,
                            canvasSize = CanvasSize(400, 400),
                            zoom = 1f,
                            pan = Offset.Zero,
                            onPreview = { updated -> layer = layer.copy(fillData = updated) },
                            onCommit = { updated -> layer = layer.copy(fillData = updated); commits++ }
                        )
                    }
                }
            }

            compose.waitForIdle()
            val node = compose.onNodeWithTag("gradient_handles")
            val rect = node.getUnclippedBoundsInRoot()
            val widthPx = (rect.right - rect.left).value
            val heightPx = (rect.bottom - rect.top).value
            val fit = 0.94f // 400dp container for 400x400 canvas at mdpi (1dp == 1px)
            val cx = widthPx / 2f
            val cy = heightPx / 2f

            // Layer spans [-100, 100] -> width 200. Start is at normalized (0.2, 0.5) => (-60, 0).
            val startScreen = Offset(cx - 60f * fit, cy)
            val endBeforeX = layer.effectiveFill.gradient.endX
            val endBeforeY = layer.effectiveFill.gradient.endY

            node.performTouchInput {
                down(startScreen)
                moveTo(startScreen + Offset(-20f * fit, -40f * fit), 100)
                up()
            }
            compose.waitForIdle()

            val afterStart = layer.effectiveFill.gradient
            assertNotNull(afterStart.startX)
            assertEquals(1, commits)
            // End stayed fixed while Start moved independently
            assertEquals(endBeforeX ?: 0.8f, afterStart.endX ?: -1f, 0.02f)
            assertEquals(endBeforeY ?: 0.5f, afterStart.endY ?: -1f, 0.02f)
            assertTrue("Start X should move left (<0.15), got ${afterStart.startX}", (afterStart.startX ?: 1f) < 0.15f)
            assertTrue("Start Y should move up (<0.35), got ${afterStart.startY}", (afterStart.startY ?: 1f) < 0.35f)

            // Drag End handle cleanly without disturbing Start
            val frozenStartX = afterStart.startX!!
            val frozenStartY = afterStart.startY!!
            // End is at (0.8, 0.5) => (+60, 0)
            val endScreen = Offset(cx + 60f * fit, cy)
            node.performTouchInput {
                down(endScreen)
                moveTo(endScreen + Offset(10f * fit, 30f * fit), 100)
                up()
            }
            compose.waitForIdle()

            val afterEnd = layer.effectiveFill.gradient
            assertEquals(2, commits)
            assertEquals(frozenStartX, afterEnd.startX ?: -1f, 0.02f)
            assertEquals(frozenStartY, afterEnd.startY ?: -1f, 0.02f)
            assertTrue("End Y should move down (>0.6), got ${afterEnd.endY}", (afterEnd.endY ?: 0f) > 0.6f)
        } finally {
            host.pause().stop().destroy()
        }
    }

    @Test
    fun primitiveOverlayAndFillRendererUseExplicitIndependentEndpoints() {
        val g = GradientData(
            type = GradientType.LINEAR,
            startX = 0.0f,
            startY = 0.0f,
            endX = 1.0f,
            endY = 0.0f,
            stops = listOf(
                GradientStop(position = 0f, color = 0xFFFF0000L),
                GradientStop(position = 1f, color = 0xFF0000FFL)
            )
        )
        assertTrue(g.hasExplicitEndpoints())
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = RectF(0f, 0f, 100f, 100f)
        val paint = Paint().apply { shader = FillRenderer.createGradientShader(g, bounds) }
        canvas.drawRect(bounds, paint)

        val leftPixel = bitmap.getPixel(5, 50)
        val rightPixel = bitmap.getPixel(95, 50)
        assertTrue(Color.red(leftPixel) > 220 && Color.blue(leftPixel) < 35)
        assertTrue(Color.blue(rightPixel) > 220 && Color.red(rightPixel) < 35)
        bitmap.recycle()

        // Verify SaltyGradientOverlay primitive callbacks fire only for dragged handle
        var s = Offset(10f, 10f)
        var e = Offset(90f, 90f)
        var c: Offset? = Offset(50f, 50f)
        var lastCommit = ""
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            host.get().setContent {
                SaltyGradientOverlay(
                    startOffset = s,
                    endOffset = e,
                    centerOffset = c,
                    onStartChange = { s = it },
                    onEndChange = { e = it },
                    onCenterChange = { c = it },
                    onTranslateBoth = { dx, dy -> s += Offset(dx, dy); e += Offset(dx, dy) },
                    onDragCommit = { lastCommit = it }
                )
            }
            compose.waitForIdle()
            val node = compose.onNodeWithTag("gradient_handles")
            node.performTouchInput {
                down(Offset(10f, 10f))
                moveTo(Offset(30f, 20f), 80)
                up()
            }
            compose.waitForIdle()
            assertEquals("Adjust Gradient Start", lastCommit)
            assertEquals(Offset(90f, 90f), e)
        } finally {
            host.pause().stop().destroy()
        }
    }
}
