package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.TextLayerData
import com.motionssalt.saltygraphics.ui.editor.TextSettingsDialog
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
class TextOptionsUITest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun testTextOptionsRendersAndUpdatesLive() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            var latestTextData = TextLayerData(text = "Hello Salty")
            var dismissed = false

            host.get().setContent {
                MaterialTheme {
                    TextSettingsDialog(
                        initialData = latestTextData,
                        candidatePathLayers = emptyList(),
                        onSave = { latestTextData = it },
                        onDismiss = { dismissed = true }
                    )
                }
            }

            // Verify title and content preview
            compose.onNodeWithText("Text Options").assertIsDisplayed()
            compose.onNodeWithText("Hello Salty").assertIsDisplayed()
            compose.onNodeWithText("Font Family").assertIsDisplayed()
            compose.onNodeWithText("Alignment").assertIsDisplayed()
            compose.onNodeWithText("Box Framing").performScrollTo().assertIsDisplayed()

            // Verify Box Framing chips exist
            compose.onNodeWithText("Free").assertIsDisplayed()
            compose.onNodeWithText("Paragraph").assertIsDisplayed()
            compose.onNodeWithText("Fixed").assertIsDisplayed()
            compose.onNodeWithText("Shrink fit").assertExists()

            // Click Paragraph chip -> should invoke onSave live
            compose.onNodeWithText("Paragraph").performClick()
            assertEquals(com.motionssalt.saltygraphics.engine.model.TextBoxMode.PARAGRAPH, latestTextData.boxMode)

            // Click Center alignment chip
            compose.onNodeWithText("Center").performClick()
            assertEquals(com.motionssalt.saltygraphics.engine.model.TextJustification.CENTER, latestTextData.justification)

            // Navigate to Font Page
            compose.onNodeWithText("Font Family").performScrollTo().performClick()
            compose.onNodeWithText("Fonts").assertIsDisplayed()
            compose.onNodeWithText("Import").assertIsDisplayed()
            compose.onNodeWithText("Device Default").assertIsDisplayed()

            // Built-in font tags exist
            compose.onAllNodesWithText("built-in").onFirst().assertIsDisplayed()

            // Select Inter font
            compose.onNodeWithText("Inter").performClick()
            assertEquals("Inter", latestTextData.fontFamily)

            // Back to main options
            compose.onNodeWithText("Text Options").assertIsDisplayed()
        } finally {
            host.pause().stop().destroy()
        }
    }
}
