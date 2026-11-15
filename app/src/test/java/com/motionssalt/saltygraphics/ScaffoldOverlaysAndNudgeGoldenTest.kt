package com.motionssalt.saltygraphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.GradientData
import com.motionssalt.saltygraphics.engine.model.GradientStop
import com.motionssalt.saltygraphics.engine.model.GuideOrientation
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.model.SelectionBounds
import com.motionssalt.saltygraphics.engine.model.ShapeData
import com.motionssalt.saltygraphics.engine.model.SmartGuideLine
import com.motionssalt.saltygraphics.engine.model.Transform2D
import com.motionssalt.saltygraphics.theme.SaltyGraphicsTheme
import com.motionssalt.saltygraphics.uikit.controls.SaltyGradientStopEditor
import com.motionssalt.saltygraphics.uikit.controls.SaltyMediaPickerGrid
import com.motionssalt.saltygraphics.uikit.controls.SaltyNudgePad
import com.motionssalt.saltygraphics.uikit.controls.SaltyNumberControl
import com.motionssalt.saltygraphics.uikit.registry.OverlayRenderContext
import com.motionssalt.saltygraphics.uikit.registry.ScaffoldCanvasOverlay
import com.motionssalt.saltygraphics.uikit.registry.SelectionOverlayBindings
import com.motionssalt.saltygraphics.uikit.structure.SaltySectionCard
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.saltyPadding
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

abstract class ScaffoldOverlaysAndNudgeGoldenFixture(private val dark: Boolean) {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun overlaysAndNudgeLibraryComponentsMatchReviewedTheme() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var expectedSurfaceColor = 0
        val layer = Layer(
            id = "g-layer",
            name = "Hero Shape",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 160f, height = 90f),
            transform = Transform2D(rotationDegrees = 12f)
        )
        val doc = ProjectDocument(canvasSize = CanvasSize(360, 220), layers = listOf(layer))
        val previewBmp = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF00B8D4.toInt()) }

        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = dark, dynamicColor = false) {
                expectedSurfaceColor = MaterialTheme.colorScheme.surface.toArgb()
                Surface(Modifier.requiredSize(412.dp, 1180.dp).testTag("matrix_root")) {
                    Column(
                        modifier = Modifier.fillMaxSize().saltyPadding(SaltySpace.MD),
                        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm)
                    ) {
                        SaltySectionCard(title = "Precision Nudge & Number Control") {
                            SaltyNudgePad(
                                stepPx = 4f,
                                onStepChange = {},
                                onNudgeDelta = { _, _ -> },
                                onNudgeCommit = { _, _ -> }
                            )
                            SaltyNumberControl(
                                label = "Rotation",
                                value = 12f,
                                min = -180f,
                                max = 180f,
                                onPreview = {},
                                onCommit = {},
                                step = 1f,
                                unit = "°"
                            )
                        }
                        SaltySectionCard(title = "Gradient Bar & Media Picker") {
                            SaltyGradientStopEditor(
                                gradient = GradientData(
                                    stops = listOf(
                                        GradientStop(0f, 0xFFFF7A00L),
                                        GradientStop(0.5f, 0xFF00E5FFL),
                                        GradientStop(1f, 0xFFE056FDL)
                                    )
                                ),
                                selectedStopIndex = 1,
                                onSelectStop = {},
                                onGradientPreview = {},
                                onGradientCommit = { _, _ -> }
                            )
                            SaltyMediaPickerGrid(
                                media = listOf("poster_bg.png" to previewBmp),
                                onSelectMedia = {},
                                onImport = {}
                            )
                        }
                        SaltySectionCard(title = "Selection Box, Guides & Angle Pill") {
                            Box(Modifier.fillMaxWidth().height(220.dp)) {
                                ScaffoldCanvasOverlay(
                                    id = "selection_box",
                                    context = OverlayRenderContext(
                                        document = doc,
                                        layer = layer,
                                        canvasSize = doc.canvasSize,
                                        zoom = 1f,
                                        pan = Offset.Zero,
                                        selectionEditing = SelectionOverlayBindings(
                                            bounds = SelectionBounds(-80f, -45f, 80f, 45f),
                                            smartGuides = listOf(
                                                SmartGuideLine(GuideOrientation.VERTICAL, 0f, -110f, 110f),
                                                SmartGuideLine(GuideOrientation.HORIZONTAL, 0f, -180f, 180f)
                                            ),
                                            rotatingAngleLabel = "12°",
                                            rotatingPillScreenPosition = Offset(190f, 40f),
                                            handleGesturesInternally = false
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        try {
            compose.mainClock.advanceTimeBy(1000)
            compose.waitForIdle()
            val node = compose.onNodeWithTag("matrix_root").fetchSemanticsNode()
            val actual = Bitmap.createBitmap(node.size.width, node.size.height, Bitmap.Config.ARGB_8888)
            compose.runOnIdle {
                val canvas = android.graphics.Canvas(actual)
                canvas.translate(-node.boundsInWindow.left, -node.boundsInWindow.top)
                host.get().window.decorView.draw(canvas)
            }
            val pixels = IntArray(actual.width * actual.height)
            actual.getPixels(pixels, 0, actual.width, 0, 0, actual.width, actual.height)
            assertTrue("Overlays golden must contain real controls", pixels.distinct().size > 20)
            assertEquals("Rendered pixels must match static theme surface", expectedSurfaceColor, actual.getPixel(2, 2))
            val name = "components_overlays_${if (dark) "dark" else "light"}.png"
            File("build/outputs/scaffold-screenshots", name).apply { parentFile.mkdirs() }
                .outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val expected = File("src/test/goldens/scaffold", name)
            val opposite = File("src/test/goldens/scaffold", "components_overlays_${if (dark) "light" else "dark"}.png")
            assertTrue("Missing reviewed golden: $name", expected.isFile)
            assertTrue("Missing reviewed opposite-theme golden: ${opposite.name}", opposite.isFile)
            val baseline = BitmapFactory.decodeFile(expected.absolutePath)
            val otherTheme = BitmapFactory.decodeFile(opposite.absolutePath)
            assertEquals(baseline.width, actual.width)
            assertEquals(baseline.height, actual.height)
            assertTrue("Changed reviewed golden: $name", baseline.sameAs(actual))
            assertFalse("Light and dark themes must differ", otherTheme.sameAs(actual))
        } finally {
            host.pause().stop().destroy()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h1220dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScaffoldOverlaysAndNudgeDarkGoldenTest : ScaffoldOverlaysAndNudgeGoldenFixture(true)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h1220dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScaffoldOverlaysAndNudgeLightGoldenTest : ScaffoldOverlaysAndNudgeGoldenFixture(false)
