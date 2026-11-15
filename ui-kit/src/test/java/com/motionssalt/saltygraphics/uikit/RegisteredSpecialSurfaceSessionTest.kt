package com.motionssalt.saltygraphics.uikit

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.engine.model.GradientData
import com.motionssalt.saltygraphics.uikit.registry.*
import com.motionssalt.saltygraphics.uikit.tokens.SaltyGraphicsTheme
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
class RegisteredGradientBarSessionTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun finalPointerValueDoesNotDependOnPreviewRecompositionAndCancellationDoesNotCommit() {
        val initial = GradientData()
        var selected = -1
        var preview: GradientData? = null
        var committed: GradientData? = null
        var label = ""
        var commits = 0
        var visible by mutableStateOf(true)
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            host.get().setContent {
                SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
                    if (visible) ScaffoldLibraryComponent(
                        id = "gradient_bar", label = "Stops",
                        current = GradientBarBindings(initial, 0,
                            onSelectStop = { selected = it },
                            // Intentionally do not recompose the input after preview:
                            // pointer-up must still commit its exact final local value.
                            onPreview = { preview = it },
                            onCommit = { identity, value -> label = identity; committed = value; commits++ }),
                        onPreview = { fail("Session callbacks must be used") },
                        onCommit = { fail("Session callbacks must be used") }
                    )
                }
            }
            compose.onNodeWithTag("registered_component_gradient_bar").assertIsDisplayed()
            val rail = compose.onNodeWithTag("gradient_stop_rail")
            rail.performTouchInput {
                down(Offset(width * .10f, centerY))
                moveTo(Offset(width * .20f, centerY), 100)
                moveTo(Offset(width * .35f, centerY), 100)
                up()
            }
            compose.runOnIdle {
                assertEquals(0, selected)
                assertEquals(1, commits)
                assertEquals("Move gradient stop", label)
                assertEquals(.35f, committed!!.stops.first().position, .005f)
                assertEquals(committed, preview)
            }
            rail.performTouchInput {
                down(Offset(width * .10f, centerY))
                moveTo(Offset(width * .28f, centerY), 100)
                cancel()
            }
            compose.runOnIdle {
                assertEquals("Cancel must roll back the draft", initial, preview)
                assertEquals("Cancel must not add history", 1, commits)
            }
            rail.performTouchInput {
                down(Offset(width * .10f, centerY))
                moveTo(Offset(width * .25f, centerY), 100)
            }
            compose.runOnIdle { visible = false }
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals("Unmounting a gesture must discard its draft", initial, preview)
                assertEquals(1, commits)
            }
        } finally {
            host.pause().stop().destroy()
        }
    }
}

/** Separate worker, one Activity per class. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RegisteredTrackpadSessionTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun replacingBindingsAtTheSameSensitivityUsesTheNewTargetAndKeepsTypedEntryExact() {
        var secondTarget by mutableStateOf(false)
        var firstX = 0f
        var secondX = 0f
        var firstCommits = 0
        var secondCommits = 0
        var exactX: Float? = null
        var exactY: Float? = null
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            host.get().setContent {
                SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
                    val isSecond = secondTarget
                    ScaffoldLibraryComponent(
                        id = "transform_trackpad", label = "Position",
                        current = PositionTrackpadBindings(
                            x = 0f, y = 0f, sensitivity = .5f,
                            onDelta = { dx, _ -> if (isSecond) secondX += dx else firstX += dx },
                            onDragEnd = { if (isSecond) secondCommits++ else firstCommits++ },
                            onCommitX = { exactX = it }, onCommitY = { exactY = it }),
                        onPreview = { fail("Session callbacks must be used") },
                        onCommit = { fail("Session callbacks must be used") }
                    )
                }
            }
            compose.onNodeWithTag("registered_component_transform_trackpad").assertIsDisplayed()
            fun drag() = compose.onNodeWithTag("trackpad_surface").performTouchInput {
                down(center); moveBy(Offset(60f, 20f), 150); up()
            }
            drag()
            var beforeSwitch = 0f
            compose.runOnIdle {
                assertTrue(firstX > 0f)
                assertEquals(1, firstCommits)
                beforeSwitch = firstX
                secondTarget = true
            }
            compose.waitForIdle()
            drag()
            compose.runOnIdle {
                assertTrue(secondX > 0f)
                assertEquals("Old target cannot receive a new session's input", beforeSwitch, firstX, 0f)
                assertEquals(1, firstCommits)
                assertEquals(1, secondCommits)
            }
            compose.onNodeWithText("X: 0 px").performClick()
            compose.onNodeWithTag("entry_Move X").performTextReplacement("123.5")
            compose.onNodeWithText("Done").performClick()
            compose.onNodeWithText("Y: 0 px").performClick()
            compose.onNodeWithTag("entry_Move Y").performTextReplacement("-17.25")
            compose.onNodeWithText("Done").performClick()
            compose.runOnIdle {
                assertEquals(123.5f, exactX!!, 0f)
                assertEquals(-17.25f, exactY!!, 0f)
                assertEquals(1, firstCommits)
                assertEquals(1, secondCommits)
            }
            compose.onAllNodes(isDialog()).assertCountEquals(0)
        } finally {
            host.pause().stop().destroy()
        }
    }
}
