package com.motionssalt.saltygraphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.theme.SaltyGraphicsTheme
import com.motionssalt.saltygraphics.uikit.controls.DropdownOption
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorPicker
import com.motionssalt.saltygraphics.uikit.controls.SaltyInlineDropdown
import com.motionssalt.saltygraphics.uikit.controls.SaltyProgressSlider
import com.motionssalt.saltygraphics.uikit.controls.SaltyRulerSlider
import com.motionssalt.saltygraphics.uikit.controls.SaltySegmentedChips
import com.motionssalt.saltygraphics.uikit.controls.SaltySwitch
import com.motionssalt.saltygraphics.uikit.controls.SaltyTextField
import com.motionssalt.saltygraphics.uikit.controls.SaltyTrackpad
import com.motionssalt.saltygraphics.uikit.structure.SaltyBadge
import com.motionssalt.saltygraphics.uikit.structure.SaltyButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyListItem
import com.motionssalt.saltygraphics.uikit.structure.SaltyOutlinedButton
import com.motionssalt.saltygraphics.uikit.structure.SaltySectionCard
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltySpacer
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyToast
import com.motionssalt.saltygraphics.uikit.structure.SaltyTonalButton
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

/**
 * Native rendered golden coverage for the named library surfaces below.
 * This is not yet the complete every-component/state matrix required for V0.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h920dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScaffoldComponentMatrixGoldenTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun renderAndCheckGolden(
        namePrefix: String,
        host: org.robolectric.android.controller.ActivityController<ComponentActivity>,
        onTheme: (Boolean) -> Unit
    ) {
        val failures = mutableListOf<String>()
        var previous: Bitmap? = null
        for (isDark in listOf(true, false)) {
            compose.runOnIdle { onTheme(isDark) }
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
            assertTrue("$namePrefix must contain real rendered pixels", pixels.distinct().size > 20)
            previous?.let { assertFalse("Light and dark themes must differ for $namePrefix", it.sameAs(actual)) }
            previous = actual

            val name = "${namePrefix}_${if (isDark) "dark" else "light"}.png"
            File("build/outputs/scaffold-screenshots", name).apply { parentFile.mkdirs() }
                .outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val expected = File("src/test/goldens/scaffold", name)
            if (!expected.isFile) {
                failures += "Missing reviewed golden: $name"
                continue
            }
            val baseline = BitmapFactory.decodeFile(expected.absolutePath)
            assertEquals("Width mismatch for $name", baseline.width, actual.width)
            assertEquals("Height mismatch for $name", baseline.height, actual.height)
            var different = 0
            for (y in 0 until actual.height) for (x in 0 until actual.width) {
                if (baseline.getPixel(x, y) != actual.getPixel(x, y)) different++
            }
            if (different > 0) failures += "$name: $different changed pixels"
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test fun controlsMatrixHasReviewedLightAndDarkGoldens() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var dark by mutableStateOf(true)
        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = dark, dynamicColor = false) {
                Surface(Modifier.requiredSize(412.dp, 860.dp).testTag("matrix_root")) {
                    Column(Modifier.fillMaxSize().saltyPadding(SaltySpace.MD)) {
                        SaltySectionCard(title = "Switches & Actions") {
                            Row(Modifier.fillMaxWidth()) {
                                SaltySwitch(checked = true, onCheckedChange = {})
                                SaltySpacer(SaltySpace.SM, horizontal = true)
                                SaltySwitch(checked = false, onCheckedChange = {})
                                SaltySpacer(SaltySpace.SM, horizontal = true)
                                SaltySwitch(checked = true, onCheckedChange = {}, enabled = false)
                                SaltySpacer(SaltySpace.SM, horizontal = true)
                                SaltySwitch(checked = false, onCheckedChange = {}, enabled = false)
                                SaltySpacer(SaltySpace.SM, horizontal = true)
                                SaltyBadge("Active")
                            }
                            Row(Modifier.fillMaxWidth()) {
                                SaltyButton("Primary", onClick = {}, icon = Icons.Default.Check)
                                SaltySpacer(SaltySpace.XS, horizontal = true)
                                SaltyTonalButton("Tonal", onClick = {})
                                SaltySpacer(SaltySpace.XS, horizontal = true)
                                SaltyOutlinedButton("Outline", onClick = {})
                            }
                        }
                        SaltySpacer(SaltySpace.SM)
                        SaltySectionCard(title = "Sliders & Selectors") {
                            SaltyRulerSlider(
                                value = 25f,
                                onValueChange = {},
                                range = -100f..100f,
                                step = 1f,
                                unit = "px",
                                label = "Position X"
                            )
                            SaltyProgressSlider(
                                value = 65f,
                                onValueChange = {},
                                range = 0f..100f,
                                step = 1f,
                                unit = "%",
                                label = "Opacity"
                            )
                            SaltySegmentedChips(
                                options = listOf("Alpha", "Luma", "Vector"),
                                selectedOption = "Alpha",
                                onOptionSelected = {},
                                label = "Mask Mode"
                            )
                            SaltyInlineDropdown(
                                selected = "Fit",
                                options = listOf(
                                    DropdownOption("Fill", "Fill"),
                                    DropdownOption("Fit", "Fit"),
                                    DropdownOption("Stretch", "Stretch")
                                ),
                                onSelect = {},
                                label = "Scaling"
                            )
                        }
                        SaltySpacer(SaltySpace.SM)
                        SaltyTextField(value = "Header Layer", onValueChange = {}, label = "Layer Name")
                        SaltySpacer(SaltySpace.XS)
                        SaltyTextField(value = "invalid!", onValueChange = {}, label = "Numeric Input", isError = true)
                        SaltySpacer(SaltySpace.SM)
                        SaltyToast(message = "Undid Move Text Layer", visible = true, icon = Icons.Default.Star)
                    }
                }
            }
        }
        try {
            // Expand the inline dropdown so both collapsed & expanded geometry are exercised
            compose.onNodeWithText("Fit").performClick()
            renderAndCheckGolden("components_controls", host) { dark = it }
        } finally {
            host.pause().stop().destroy()
        }
    }

}
