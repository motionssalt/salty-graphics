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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.BlendMode
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.MaskMode
import com.motionssalt.saltygraphics.engine.model.MaskType
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.model.ShapeData
import com.motionssalt.saltygraphics.theme.SaltyGraphicsTheme
import com.motionssalt.saltygraphics.uikit.controls.SaltyBlendAndMaskPanel
import com.motionssalt.saltygraphics.uikit.structure.SaltyPanelPage
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextStyle
import com.motionssalt.saltygraphics.uikit.structure.saltyPadding
import com.motionssalt.saltygraphics.uikit.sublibs.effects.SaltyEffectsPage
import com.motionssalt.saltygraphics.uikit.sublibs.history.SaltyHistorySublib
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

abstract class ScaffoldSublibsGoldenFixture(private val dark: Boolean) {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun sublibSurfacesMatchReviewedThemeAndDifferAcrossThemes() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var expectedSurfaceColor = 0
        val layer = Layer(id = "s1", name = "Shape 1", type = LayerType.SHAPE, shapeData = ShapeData())
        val doc = ProjectDocument(name = "Poster", layers = listOf(layer))
        val tree = ActionTree(doc).apply {
            recordAction("Move Shape", doc.copy(name = "Poster v2"))
        }

        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = dark, dynamicColor = false) {
                expectedSurfaceColor = MaterialTheme.colorScheme.surface.toArgb()
                Surface(Modifier.requiredSize(412.dp, 1180.dp).testTag("matrix_root")) {
                    Column(
                        modifier = Modifier.fillMaxSize().saltyPadding(SaltySpace.MD),
                        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm)
                    ) {
                        Box(Modifier.fillMaxWidth().height(110.dp)) {
                            SaltyPanelPage(title = "Panel Page", onBack = {}) {
                                SaltyText("Uniform scaffold header & content", style = SaltyTextStyle.BODY_MEDIUM)
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(260.dp)) {
                            SaltyHistorySublib(
                                actionTree = tree,
                                onJumpToNode = {},
                                onDismiss = {}
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(340.dp)) {
                            SaltyBlendAndMaskPanel(
                                currentBlendMode = BlendMode.MULTIPLY,
                                currentOpacity = 0.85f,
                                currentMaskType = MaskType.NONE,
                                currentMaskSourceLayerId = null,
                                isMaskInverted = false,
                                currentMaskMode = MaskMode.NONE,
                                allAvailableLayers = listOf(layer),
                                maskPreviewBitmap = null,
                                onBlendChange = { _, _ -> },
                                onMaskChange = { _, _, _, _ -> },
                                onBack = {}
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(320.dp)) {
                            SaltyEffectsPage(
                                targetLayer = layer,
                                allLayers = listOf(layer),
                                includeDebugEffects = true,
                                onAddEffect = {},
                                onRemoveEffect = {},
                                onUpdateEffect = { _, _ -> },
                                onClose = {},
                                wrapInPanelPage = true
                            )
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
            assertTrue("Sublibs golden must contain real controls", pixels.distinct().size > 20)
            assertEquals("Rendered pixels must match static theme surface", expectedSurfaceColor, actual.getPixel(2, 2))
            val name = "components_sublibs_${if (dark) "dark" else "light"}.png"
            File("build/outputs/scaffold-screenshots", name).apply { parentFile.mkdirs() }
                .outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val expected = File("src/test/goldens/scaffold", name)
            val opposite = File("src/test/goldens/scaffold", "components_sublibs_${if (dark) "light" else "dark"}.png")
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
class ScaffoldSublibsDarkGoldenTest : ScaffoldSublibsGoldenFixture(true)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h1220dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScaffoldSublibsLightGoldenTest : ScaffoldSublibsGoldenFixture(false)
