package com.motionssalt.saltygraphics

import androidx.compose.ui.graphics.Color
import com.motionssalt.saltygraphics.theme.*
import com.motionssalt.saltygraphics.ui.controls.SwitchContrastHelper
import org.junit.Assert.assertTrue
import org.junit.Test

class SwitchContrastTest {

    @Test
    fun testSwitchContrastInDarkTheme() {
        val surface = Color(0xFF1E1E2E)
        val primaryAccent = Color(0xFF00E5FF)
        val onPrimary = Color(0xFF00363D)
        val outline = Color(0xFF9399B2)
        val surfaceVariant = Color(0xFF313244)
        val onSurfaceVariant = Color(0xFFCDD6F4)

        // ON State:
        // Track (primaryAccent) vs Surface:
        val onTrackVsSurface = SwitchContrastHelper.contrastRatio(primaryAccent, surface)
        assertTrue("ON track vs surface must exceed 3:1 (was $onTrackVsSurface)", onTrackVsSurface >= 3.0f)

        // Thumb (onPrimary) vs Track (primaryAccent):
        val onThumbVsTrack = SwitchContrastHelper.contrastRatio(onPrimary, primaryAccent)
        assertTrue("ON thumb vs track must exceed 3:1 (was $onThumbVsTrack)", onThumbVsTrack >= 3.0f)

        // OFF State:
        // Outline border vs Surface:
        val offBorderVsSurface = SwitchContrastHelper.contrastRatio(outline, surface)
        assertTrue("OFF outline border vs surface must exceed 3:1 (was $offBorderVsSurface)", offBorderVsSurface >= 3.0f)

        // Thumb (onSurfaceVariant) vs Track (surfaceVariant):
        val offThumbVsTrack = SwitchContrastHelper.contrastRatio(onSurfaceVariant, surfaceVariant)
        assertTrue("OFF thumb vs track must exceed 3:1 (was $offThumbVsTrack)", offThumbVsTrack >= 3.0f)
    }

    @Test
    fun testSwitchContrastInLightTheme() {
        val surface = Color(0xFFEFF1F5)
        val primaryAccent = Color(0xFF006875)
        val onPrimary = Color(0xFFFFFFFF)
        val outline = Color(0xFF5C5F77)
        val surfaceVariant = Color(0xFFCCD0DA)
        val onSurfaceVariant = Color(0xFF4C4F69)

        // ON State:
        val onTrackVsSurface = SwitchContrastHelper.contrastRatio(primaryAccent, surface)
        assertTrue("Light theme ON track vs surface must exceed 3:1 (was $onTrackVsSurface)", onTrackVsSurface >= 3.0f)

        val onThumbVsTrack = SwitchContrastHelper.contrastRatio(onPrimary, primaryAccent)
        assertTrue("Light theme ON thumb vs track must exceed 3:1 (was $onThumbVsTrack)", onThumbVsTrack >= 3.0f)

        // OFF State:
        val offBorderVsSurface = SwitchContrastHelper.contrastRatio(outline, surface)
        assertTrue("Light theme OFF outline vs surface must exceed 3:1 (was $offBorderVsSurface)", offBorderVsSurface >= 3.0f)

        val offThumbVsTrack = SwitchContrastHelper.contrastRatio(onSurfaceVariant, surfaceVariant)
        assertTrue("Light theme OFF thumb vs track must exceed 3:1 (was $offThumbVsTrack)", offThumbVsTrack >= 3.0f)
    }
}
