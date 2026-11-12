package com.motionssalt.saltygraphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.model.ShapeType
import com.motionssalt.saltygraphics.theme.SaltyGraphicsTheme
import com.motionssalt.saltygraphics.theme.ThemeChoice
import com.motionssalt.saltygraphics.ui.editor.ShapePickerDialog
import com.motionssalt.saltygraphics.ui.editor.ShapePickerFeature
import com.motionssalt.saltygraphics.uikit.registry.*
import com.motionssalt.saltygraphics.uikit.structure.ScaffoldToolPicker
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
class ScaffoldToolPickerTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun productionShapeChoicesKeepDefaultsAndDispatchOnceInsidePanel() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val picked = mutableListOf<com.motionssalt.saltygraphics.engine.model.ShapeData>()
        var dismissals = 0
        host.get().setContent { SaltyGraphicsTheme(themeChoice = ThemeChoice.DARK, dynamicColor = false) {
            ShapePickerDialog(ProjectDocument(), { picked += it }, { dismissals++ })
        } }
        try {
            compose.onNodeWithTag("scaffold_tool_picker").assertIsDisplayed()
            for (choice in ShapePickerFeature.choices) {
                compose.onNodeWithText(choice.name, useUnmergedTree = true).performClick()
                compose.runOnIdle { assertEquals(choice.shape, picked.last()) }
            }
            compose.runOnIdle { assertEquals(7, picked.size); assertEquals(7, dismissals) }
            assertEquals(ShapeType.entries.toSet() - ShapeType.TRIANGLE, picked.map { it.type }.toSet())
            val line = picked.first { it.type == ShapeType.LINE }
            assertFalse(line.hasFill); assertTrue(line.hasStroke); assertEquals(6f, line.strokeWidth, 0f)
            assertNull(ShapePickerFeature.shape("missing"))
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            compose.onNodeWithContentDescription("Back").performClick()
            assertEquals(8, dismissals)
        } finally { host.pause().stop().destroy() }
    }

    @Test fun throwawayPickerUsesRegistrationsAndEnabledAndAvailableConditions() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var enabled by mutableStateOf(false)
        val catalog = FeatureCatalog(requireTypedBindings = true).apply {
            register(FeatureDefinition("one", "Registered choice", Icons.Default.Star, ToolPlacement.ALWAYS,
                enabledCondition = { _, _ -> enabled }))
            register(FeatureDefinition("hidden", "Unavailable", Icons.Default.Star, ToolPlacement.ALWAYS))
        }
        var clicks = 0
        host.get().setContent { SaltyGraphicsTheme(themeChoice = ThemeChoice.DARK, dynamicColor = false) {
            ScaffoldToolPicker("Choices", catalog, ProjectDocument(), setOf("one", "later"), { clicks++ }, {})
        } }
        try {
            compose.onNodeWithText("Registered choice").assertIsNotEnabled()
            compose.onNodeWithText("Unavailable").assertDoesNotExist()
            compose.runOnIdle { enabled = true }
            compose.onNodeWithText("Registered choice").assertIsEnabled().performClick()
            compose.runOnIdle { catalog.register(FeatureDefinition("later", "Added later", Icons.Default.Star, ToolPlacement.ALWAYS)) }
            compose.onNodeWithText("Added later").performClick()
            compose.runOnIdle { assertEquals(2, clicks) }
        } finally { host.pause().stop().destroy() }
    }

    /** Library surface golden, produced only by CI test; absent baselines fail rather than auto-accept. */
    @Test fun sharedPickerHasReviewedDarkAndLightGoldensAtPhonePanelSize() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var dark by mutableStateOf(true)
        host.get().setContent { SaltyGraphicsTheme(
            themeChoice = if (dark) ThemeChoice.DARK else ThemeChoice.LIGHT, dynamicColor = false
        ) {
            ScaffoldToolPicker("Choose a Shape", ShapePickerFeature.catalog, ProjectDocument(),
                ShapePickerFeature.availableActions, {}, {}, Modifier.requiredSize(412.dp, 300.dp))
        } }
        val failures = mutableListOf<String>()
        val themePixels = mutableListOf<IntArray>()
        try {
            for (isDark in listOf(true, false)) {
                compose.runOnIdle { dark = isDark }
                compose.mainClock.advanceTimeBy(1000); compose.waitForIdle()
                val node = compose.onNodeWithTag("scaffold_tool_picker").fetchSemanticsNode()
                val bounds = node.boundsInWindow
                val actual = Bitmap.createBitmap(node.size.width, node.size.height, Bitmap.Config.ARGB_8888)
                compose.runOnIdle { Canvas(actual).apply {
                    translate(-bounds.left, -bounds.top); host.get().window.decorView.draw(this)
                } }
                val pixels = IntArray(actual.width * actual.height)
                actual.getPixels(pixels, 0, actual.width, 0, 0, actual.width, actual.height)
                assertTrue("Rendered controls cannot be blank", pixels.distinct().size > 20)
                themePixels += pixels
                val name = "picker_${if (isDark) "dark" else "light"}.png"
                val out = File("build/outputs/scaffold-screenshots", name).apply { parentFile.mkdirs() }
                out.outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val expected = File("src/test/goldens/scaffold", name)
                if (!expected.isFile) { failures += "Missing reviewed golden: $name"; actual.recycle(); continue }
                val baseline = BitmapFactory.decodeFile(expected.absolutePath)
                if (baseline.width != actual.width || baseline.height != actual.height) failures += "Changed dimensions: $name"
                else {
                    var changed = 0
                    for (y in 0 until actual.height) for (x in 0 until actual.width)
                        if (baseline.getPixel(x, y) != actual.getPixel(x, y)) changed++
                    if (changed > 0) failures += "$name: $changed changed pixels"
                }
                baseline.recycle(); actual.recycle()
            }
            assertFalse("Real selected theme must reach the shared picker", themePixels[0].contentEquals(themePixels[1]))
            assertTrue(failures.joinToString("\n"), failures.isEmpty())
        } finally { host.pause().stop().destroy() }
    }
}
