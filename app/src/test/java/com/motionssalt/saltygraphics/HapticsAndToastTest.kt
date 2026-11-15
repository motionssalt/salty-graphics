package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.data.SettingsManager
import com.motionssalt.saltygraphics.theme.SaltyGraphicsTheme
import com.motionssalt.saltygraphics.uikit.controls.*
import com.motionssalt.saltygraphics.uikit.structure.SaltyToastHost
import com.motionssalt.saltygraphics.engine.model.SelectionBounds
import com.motionssalt.saltygraphics.engine.model.Transform2D
import com.motionssalt.saltygraphics.uikit.structure.SaltyToastManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class HapticsAndToastTest {
    @get:Rule val compose = createEmptyComposeRule()

    @After
    fun tearDown() {
        SaltyHaptics.performer = { view, constant -> view.performHapticFeedback(constant) }
        SaltyHaptics.systemHapticsEnabledOverride = null
        SaltyToastManager.clear()
    }

    @Test
    fun rulerThrottleModeratesSlowFastFlingsAndModes() {
        var now = 1000L
        val throttle = RulerHapticThrottle { now }

        // 1. Default LIGHT mode: slow drag crossing ticks 1..20 (step=1, default=50)
        // Minor ticks (1..9) must NOT fire; major tick 10 and 20 fire, spaced >= 100ms
        val firedValues = mutableListOf<Float>()
        for (v in 1..20) {
            now += 110L
            throttle.onDragValueChanged(
                mode = ScrollHapticsMode.LIGHT,
                prevValue = (v - 1).toFloat(),
                newValue = v.toFloat(),
                step = 1f,
                defaultValue = 50f,
                isFling = false,
                systemEnabled = true
            ) { firedValues.add(v.toFloat()) }
        }
        assertEquals(listOf(10f, 20f), firedValues)

        // 2. Fling free-spin must NEVER fire even across major ticks and 0
        throttle.reset()
        var flingCount = 0
        for (v in -15..15) {
            now += 150L
            throttle.onDragValueChanged(
                mode = ScrollHapticsMode.NORMAL,
                prevValue = (v - 1).toFloat(),
                newValue = v.toFloat(),
                step = 1f,
                defaultValue = 0f,
                isFling = true,
                systemEnabled = true
            ) { flingCount++ }
        }
        assertEquals("Fling must emit zero haptics", 0, flingCount)

        // 3. Fast drag enforces >= 60ms interval and <= max-per-second cap
        throttle.reset()
        val fastTimes = mutableListOf<Long>()
        for (stepIdx in 1..40) {
            now += 20L // 50 steps/sec (800ms total)
            throttle.onDragValueChanged(
                mode = ScrollHapticsMode.NORMAL,
                prevValue = ((stepIdx - 1) * 5).toFloat(),
                newValue = (stepIdx * 5).toFloat(),
                step = 1f,
                defaultValue = 0f,
                isFling = false,
                systemEnabled = true
            ) { fastTimes.add(now) }
        }
        for (i in 1 until fastTimes.size) {
            assertTrue("Interval must be >= 60ms", fastTimes[i] - fastTimes[i - 1] >= 60L)
        }
        assertTrue("Fast drag tick count must stay capped (<= 8/sec)", fastTimes.size <= 8)

        // 4. OFF mode and system disabled silence completely
        throttle.reset()
        var offCount = 0
        throttle.onDragValueChanged(ScrollHapticsMode.OFF, -1f, 10f, 1f, 0f, false, true) { offCount++ }
        throttle.onDragValueChanged(ScrollHapticsMode.LIGHT, -1f, 10f, 1f, 0f, false, false) { offCount++ }
        assertEquals(0, offCount)
    }

    @Test
    fun rulerPointerDragFiresOnlyDuringFingerDownAndNotInOffMode() {
        val haptics = mutableListOf<Int>()
        SaltyHaptics.systemHapticsEnabledOverride = true
        SaltyHaptics.performer = { _, c -> haptics.add(c) }

        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var mode by mutableStateOf(ScrollHapticsMode.LIGHT)
        var value by mutableFloatStateOf(-5f)
        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
                CompositionLocalProvider(LocalScrollHapticsMode provides mode) {
                    SaltyRulerSlider(
                        value = value,
                        onValueChange = { value = it },
                        range = -100f..100f,
                        step = 1f,
                        defaultValue = 0f,
                        label = "Position"
                    )
                }
            }
        }
        try {
            compose.onNodeWithTag("ruler_track").performTouchInput {
                swipeLeft(startX = right - 20f, endX = left + 20f, durationMillis = 600)
            }
            val countAfterDrag = haptics.size
            assertTrue("Expected moderated haptic ticks across 0/major ticks", countAfterDrag in 1..6)
            // Advance clock so any fling decay completes; count must NOT increase during fling
            compose.mainClock.advanceTimeBy(1500L)
            compose.waitForIdle()
            assertEquals("No haptics may fire during fling free-spin", countAfterDrag, haptics.size)

            // Switch to OFF mode and drag again -> zero new haptics
            mode = ScrollHapticsMode.OFF
            compose.waitForIdle()
            compose.onNodeWithTag("ruler_track").performTouchInput {
                swipeRight(startX = left + 20f, endX = right - 20f, durationMillis = 500)
            }
            compose.mainClock.advanceTimeBy(1500L)
            compose.waitForIdle()
            assertEquals("OFF mode must emit zero haptics", countAfterDrag, haptics.size)
        } finally {
            host.pause().stop().destroy()
        }
    }

    @Test
    fun settingsDefaultsToLightScrollHapticsAndTogglesFireOnce() {
        val ctx = RuntimeEnvironment.getApplication()
        ctx.getSharedPreferences("salty_settings", 0).edit().clear().commit()
        val mgr = SettingsManager(ctx)
        assertEquals(ScrollHapticsMode.LIGHT, mgr.scrollHapticsMode.value)
        mgr.setScrollHapticsMode(ScrollHapticsMode.NORMAL)
        assertEquals(ScrollHapticsMode.NORMAL, SettingsManager(ctx).scrollHapticsMode.value)

        var hapticCalls = 0
        SaltyHaptics.systemHapticsEnabledOverride = true
        SaltyHaptics.performer = { _, _ -> hapticCalls++ }

        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var checked by mutableStateOf(false)
        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
                CompositionLocalProvider(LocalScrollHapticsMode provides ScrollHapticsMode.LIGHT) {
                    Box(Modifier.requiredSize(200.dp, 80.dp)) {
                        SaltySwitch(checked = checked, onCheckedChange = { checked = it }, modifier = Modifier.testTag("test_switch"))
                    }
                }
            }
        }
        try {
            compose.waitForIdle()
            compose.onNodeWithTag("test_switch").performClick()
            compose.waitForIdle()
            assertTrue(checked)
            assertEquals("Key toggle must fire haptic once", 1, hapticCalls)
        } finally {
            host.pause().stop().destroy()
        }
    }

    @Test
    fun noSystemToastCallsRemainAndInAppToastQueuesMaxTwoWithoutBlockingCanvas() {
        val appMain = File("src/main/java").takeIf(File::exists) ?: File("app/src/main/java")
        val uikitMain = File("../ui-kit/src/main/java").takeIf(File::exists) ?: File("ui-kit/src/main/java")
        for (dir in listOf(appMain, uikitMain)) {
            dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { f ->
                val text = f.readText()
                assertFalse("System Toast found in ${f.name}", text.contains("Toast.makeText") || text.contains("android.widget.Toast"))
            }
        }

        SaltyToastManager.clear()
        var canvasTapped = false
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
                Box(
                    Modifier.fillMaxSize().testTag("canvas_touch_area").pointerInput(Unit) {
                        detectTapGestures { canvasTapped = true }
                    }
                ) {
                    SaltyToastHost(modifier = Modifier.align(Alignment.BottomStart))
                }
            }
        }
        try {
            SaltyToastManager.show("First notice", durationMs = 1500L)
            SaltyToastManager.show("Second notice", durationMs = 1500L)
            SaltyToastManager.show("Third notice", durationMs = 1500L)
            compose.waitForIdle()
            compose.onNodeWithText("First notice").assertIsDisplayed()
            compose.onNodeWithText("Second notice").assertIsDisplayed()
            compose.onNodeWithText("Third notice").assertDoesNotExist()
            // Canvas touches beneath toast host must NOT be intercepted
            val toastBounds = compose.onNodeWithTag("salty_toast_host").fetchSemanticsNode().boundsInRoot
            compose.onNodeWithTag("canvas_touch_area").performTouchInput {
                click(Offset(toastBounds.center.x, toastBounds.center.y))
            }
            assertTrue("In-app toast must not intercept canvas touches", canvasTapped)
            SaltyToastManager.clear()
            compose.mainClock.advanceTimeBy(2000L)
            compose.waitForIdle()
        } finally {
            host.pause().stop().destroy()
        }
    }

    @Test
    fun snapAndGuideLockHapticsFireOncePerEngagementAndOrdinaryDragIsQuiet() {
        val fired = mutableListOf<Int>()
        SaltyHaptics.systemHapticsEnabledOverride = true
        SaltyHaptics.performer = { _, constant -> fired.add(constant) }
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var transform by mutableStateOf(Transform2D(translationX = 50f, translationY = 50f))
        val targetBounds = SelectionBounds(-50f, -50f, 50f, 50f)
        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
                Box(Modifier.requiredSize(400.dp, 360.dp)) {
                    com.motionssalt.saltygraphics.ui.editor.TransformPanel(
                        transform = transform,
                        onTransformChange = { transform = it },
                        onTransformCommit = { transform = it },
                        snapContext = com.motionssalt.saltygraphics.ui.editor.TransformSnapContext(
                            bounds = targetBounds,
                            otherLayers = emptyList(),
                            otherAngles = emptyList(),
                            canvasWidth = 1000f,
                            canvasHeight = 1000f,
                            effectiveZoom = 1f,
                            snapEnabled = true,
                            snapToGrid = false,
                            showGrid = false,
                            gridSize = 20,
                            isSquareGrid = true,
                            tolerancePx = 8f,
                            onGuidesChange = { _, _, _ -> }
                        ),
                        onClose = {},
                        wrapInPanelPage = true
                    )
                }
            }
        }
        try {
            compose.waitForIdle()
            compose.onNodeWithText("Sliders").performScrollTo().performClick()
            compose.waitForIdle()
            compose.onNodeWithText("50 px", useUnmergedTree = true).performScrollTo().performClick()
            compose.onNodeWithTag("entry_Move X").performTextReplacement("200")
            compose.onNodeWithText("Done").performClick()
            compose.waitForIdle()
            assertEquals("Ordinary non-snapped transform must emit zero haptics", 0, fired.size)

            fired.clear()
            compose.onNodeWithText("Trackpad").performScrollTo().performClick()
            compose.waitForIdle()
            val view = host.get().window.decorView
            SaltyHaptics.onGuideLocked(view)
            SaltyHaptics.onSnapEngaged(view)
            assertEquals(2, fired.size)
        } finally {
            SaltyHaptics.systemHapticsEnabledOverride = null
            SaltyHaptics.performer = { view, constant -> view.performHapticFeedback(constant) }
            host.pause().stop().destroy()
        }
    }

}
