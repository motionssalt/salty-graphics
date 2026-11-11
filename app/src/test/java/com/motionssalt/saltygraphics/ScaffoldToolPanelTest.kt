package com.motionssalt.saltygraphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.ui.editor.BottomPanel
import com.motionssalt.saltygraphics.ui.editor.EditorToolFeatures
import com.motionssalt.saltygraphics.uikit.registry.*
import com.motionssalt.saltygraphics.uikit.structure.ScaffoldToolPanel
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScaffoldToolPanelTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun productionToolGridDispatchesRealCallbacksAndTracksEligibility() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var selected by mutableStateOf<Layer?>(null)
        val clicks = mutableListOf<String>()
        host.get().setContent {
            MaterialTheme { Box(Modifier.requiredSize(412.dp, 300.dp)) {
                BottomPanel(selected, { clicks += "media" }, {}, {}, {}, {}, {}, {}, {}, {}, {},
                    onOpenFill = { clicks += "fill" }, onOpenBorderShadow = { clicks += "border" },
                    onRasterize = { clicks += "rasterize" })
            } }
        }
        try {
            compose.onNodeWithTag("scaffold_tool_panel").assertIsDisplayed()
            compose.onNodeWithText("Media").performClick()
            compose.onNodeWithText("Fill").assertDoesNotExist()
            compose.runOnIdle { selected = Layer(id = "shape", type = LayerType.SHAPE, shapeData = ShapeData()) }
            compose.onNodeWithText("Fill").performClick()
            compose.onNodeWithText("Border & Shadow").performClick()
            compose.onNodeWithText("Rasterize").performClick()
            compose.onNodeWithText("Media").assertDoesNotExist()
            compose.runOnIdle { selected = Layer(id = "adjust", type = LayerType.ADJUSTMENT) }
            compose.onNodeWithText("Fill").assertDoesNotExist()
            compose.onNodeWithText("Rasterize").assertDoesNotExist()
            compose.runOnIdle { assertEquals(listOf("media", "fill", "border", "rasterize"), clicks) }
        } finally { host.pause().stop().destroy() }
    }

    @Test fun throwawayRegistrationAppearsWithoutFeatureLayoutAndHonorsEnabledCondition() {
        val catalog = FeatureCatalog()
        var enabled by mutableStateOf(false)
        var clicks = 0
        catalog.register(FeatureDefinition("throwaway", "Throwaway", Icons.Default.Star, ToolPlacement.ALWAYS,
            enabledCondition = { _, _ -> enabled }))
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        host.get().setContent {
            MaterialTheme { Box(Modifier.requiredSize(412.dp, 300.dp)) {
                ScaffoldToolPanel(catalog, ProjectDocument(), emptySet(), setOf("throwaway"), { clicks++ })
            } }
        }
        try {
            compose.onNodeWithText("Throwaway").assertIsDisplayed().assertIsNotEnabled()
            compose.runOnIdle { enabled = true }
            compose.onNodeWithText("Throwaway").assertIsEnabled().performClick()
            compose.runOnIdle { assertEquals(1, clicks) }
        } finally { host.pause().stop().destroy() }
    }

    @Test fun toolEligibilityRejectsUnknownOrMixedLayerSelections() {
        val shape = Layer(id = "shape", type = LayerType.SHAPE)
        val adjustment = Layer(id = "adjustment", type = LayerType.ADJUSTMENT)
        val doc = ProjectDocument(layers = listOf(shape, adjustment))
        val catalog = EditorToolFeatures.catalog
        assertFalse(catalog.queryTools(doc, setOf("missing")).any { it.id == "fill" })
        assertFalse(catalog.queryTools(doc, setOf(shape.id, adjustment.id)).any { it.id == "fill" })
        assertTrue(catalog.queryTools(doc, setOf(shape.id)).any { it.id == "fill" })
        assertTrue(catalog.queryTools(doc, emptySet()).any { it.id == "media" })
    }

    /** Actual native-rendered PNG comparison, not a token/contrast test. No auto-accept mode. */
    @Test fun productionGridHasReviewedDarkAndLightPixelGoldens() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var dark by mutableStateOf(true)
        var state by mutableIntStateOf(0)
        host.get().setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.requiredSize(412.dp, 300.dp)) {
                    val shape = Layer(id = "shape", type = LayerType.SHAPE, shapeData = ShapeData())
                    val selected = if (state == 0) null else shape
                    BottomPanel(selected, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                        onOpenFill = {}, onOpenBorderShadow = {}, onDuplicate = {}, onRasterize = {},
                        groupBreadcrumbs = if (state == 2) listOf("group" to "Group") else emptyList(),
                        onPopGroup = {}, onJumpToGroup = {})
                }
            }
        }
        val failures = mutableListOf<String>()
        try {
            for (isDark in listOf(true, false)) for (selection in 0..2) {
                compose.runOnIdle { dark = isDark; state = selection }
                compose.mainClock.advanceTimeBy(1000)
                compose.waitForIdle()
                val name = "tools_${if (isDark) "dark" else "light"}_${selection}.png"
                val actual = compose.onNodeWithTag("scaffold_tool_panel").captureToImage().asAndroidBitmap()
                val out = File("build/outputs/scaffold-screenshots", name).apply { parentFile.mkdirs() }
                out.outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val expected = File("src/test/goldens/scaffold", name)
                if (!expected.isFile) { failures += "Missing reviewed golden: $name"; continue }
                val baseline = BitmapFactory.decodeFile(expected.absolutePath)
                if (baseline.width != actual.width || baseline.height != actual.height) {
                    failures += "Dimensions changed: $name"; continue
                }
                var different = 0
                for (y in 0 until actual.height) for (x in 0 until actual.width) {
                    if (baseline.getPixel(x,y) != actual.getPixel(x,y)) different++
                }
                if (different > 0) failures += "$name: $different changed pixels"
            }
            assertTrue(failures.joinToString("\n"), failures.isEmpty())
        } finally { host.pause().stop().destroy() }
    }
}
