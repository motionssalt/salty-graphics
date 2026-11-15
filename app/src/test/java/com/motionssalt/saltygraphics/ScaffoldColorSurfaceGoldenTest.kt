package com.motionssalt.saltygraphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.theme.SaltyGraphicsTheme
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorPicker
import com.motionssalt.saltygraphics.uikit.controls.SaltyTrackpad
import com.motionssalt.saltygraphics.uikit.structure.SaltyListItem
import com.motionssalt.saltygraphics.uikit.structure.SaltySectionCard
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltySpacer
import com.motionssalt.saltygraphics.uikit.structure.saltyPadding
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** One native sandbox per static theme; no runtime theme/input animation in a golden fixture. */
abstract class ScaffoldColorSurfaceGoldenFixture(private val dark: Boolean) {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun staticLibrarySurfaceMatchesReviewedThemeAndIsNotBlankOrTheOppositeTheme() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var expectedSurfaceColor = 0
        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = dark, dynamicColor = false) {
                expectedSurfaceColor = MaterialTheme.colorScheme.surface.toArgb()
                Surface(Modifier.requiredSize(412.dp, 860.dp).testTag("matrix_root")) {
                    Column(Modifier.fillMaxSize().saltyPadding(SaltySpace.MD)) {
                        SaltySectionCard(title = "List Items & Trackpad") {
                            SaltyListItem(title = "Selected Shape", subtitle = "Rectangle • 100%", isSelected = true)
                            SaltyListItem(title = "Background Layer", subtitle = "Media • 80%", isSelected = false)
                            SaltyTrackpad(onDelta = { _, _ -> }, sensitivity = 1f, label = "Precision Trackpad")
                        }
                        SaltySpacer(SaltySpace.SM)
                        SaltySectionCard(title = "Color Picker") {
                            SaltyColorPicker(colorArgb = 0xCCFF7A00L, onColorChange = {},
                                presets = listOf(0xFFFF0000L, 0xFF00FF00L, 0xFF0000FFL, 0xFFFFFFFFL))
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
            assertTrue("Surface golden must contain real controls", pixels.distinct().size > 20)
            assertEquals("Rendered pixels must use this fixture's actual theme, not a cached frame",
                expectedSurfaceColor, actual.getPixel(2, 2))
            val name = "components_surfaces_${if (dark) "dark" else "light"}.png"
            File("build/outputs/scaffold-screenshots", name).apply { parentFile.mkdirs() }
                .outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val expected = File("src/test/goldens/scaffold", name)
            val opposite = File("src/test/goldens/scaffold", "components_surfaces_${if (dark) "light" else "dark"}.png")
            assertTrue("Missing reviewed golden: $name", expected.isFile)
            assertTrue("Missing reviewed opposite-theme golden: ${opposite.name}", opposite.isFile)
            val baseline = BitmapFactory.decodeFile(expected.absolutePath)
            val otherTheme = BitmapFactory.decodeFile(opposite.absolutePath)
            assertEquals(baseline.width, actual.width)
            assertEquals(baseline.height, actual.height)
            assertTrue("Changed reviewed golden: $name", baseline.sameAs(actual))
            assertFalse("Light and dark surface themes must differ", otherTheme.sameAs(actual))
        } finally {
            host.pause().stop().destroy()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h920dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScaffoldColorSurfaceDarkGoldenTest : ScaffoldColorSurfaceGoldenFixture(true)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h920dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScaffoldColorSurfaceLightGoldenTest : ScaffoldColorSurfaceGoldenFixture(false)
