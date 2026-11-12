package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.ui.editor.*
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
class BottomPanelEditingFlowAuditTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun editingFlowsRenderInsideBottomPanelWithoutForbiddenPopups() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            var activeBlendMode by mutableStateOf(BlendMode.NORMAL)
            var activeOpacity by mutableFloatStateOf(1f)

            host.get().setContent {
                MaterialTheme {
                    BlendingPage(
                        currentBlendMode = activeBlendMode,
                        currentOpacity = activeOpacity,
                        currentMaskType = MaskType.NONE,
                        currentMaskSourceLayerId = null,
                        isMaskInverted = false,
                        allAvailableLayers = emptyList(),
                        onBlendChange = { m, o ->
                            activeBlendMode = m
                            activeOpacity = o
                        },
                        onMaskChange = { _, _, _, _ -> },
                        onBack = {}
                    )
                }
            }

            compose.waitForIdle()

            // Verify BlendingPage rendered as a panel page
            compose.onNodeWithText("Blending & Mask").assertIsDisplayed()
            compose.onNodeWithText("Blend Mode Categories").assertIsDisplayed()
            compose.onNodeWithText("Opacity").assertIsDisplayed()

            // Main blend mode items must be visible in grid
            compose.onNodeWithContentDescription("Normal").assertIsDisplayed()
            compose.onNodeWithContentDescription("Multiply").assertIsDisplayed()

            // No modal dialogs/popovers shown
            compose.onAllNodes(isDialog()).assertCountEquals(0)
        } finally {
            host.pause().stop().destroy()
        }
    }
}
