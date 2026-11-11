package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.view.WindowCompat
import com.motionssalt.saltygraphics.data.SettingsManager
import com.motionssalt.saltygraphics.data.ThemeChoice
import com.motionssalt.saltygraphics.theme.SaltyGraphicsTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class ThemeChoiceTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun defaultDarkAllChoicesPersistAndUpdateThemeAndSystemBarContrast() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("salty_settings", 0).edit().clear().commit()
        val manager = SettingsManager(context)
        assertEquals(ThemeChoice.DARK, manager.theme.value)
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val systemTheme = kotlinx.coroutines.flow.MutableStateFlow(false)
        var background = Color.Unspecified
        host.get().setContent {
            val choice by manager.theme.collectAsState()
            val systemDark by systemTheme.collectAsState()
            SaltyGraphicsTheme(darkTheme = choice.isDark(systemDark), dynamicColor = false) {
                val color = MaterialTheme.colorScheme.background
                SideEffect { background = color }
                androidx.compose.material3.Surface(color = color) {
                    androidx.compose.material3.Text("Theme probe")
                }
            }
        }
        try {
            for ((choice, osDark) in listOf(ThemeChoice.DARK to false, ThemeChoice.LIGHT to false,
                ThemeChoice.SYSTEM to false, ThemeChoice.SYSTEM to true)) {
                compose.runOnIdle { systemTheme.value = osDark; manager.setTheme(choice) }
                compose.waitForIdle()
                assertEquals(choice, SettingsManager(context).theme.value)
                assertEquals(choice.isDark(osDark), background.luminance() < .5f)
                val bars = WindowCompat.getInsetsController(host.get().window, host.get().window.decorView)
                assertEquals(!choice.isDark(osDark), bars.isAppearanceLightStatusBars)
                assertEquals(!choice.isDark(osDark), bars.isAppearanceLightNavigationBars)
            }
        } finally { host.pause().stop().destroy() }
    }
}
