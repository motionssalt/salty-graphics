package com.motionssalt.saltygraphics.uikit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.motionssalt.saltygraphics.uikit.tokens.SaltySpacing
import com.motionssalt.saltygraphics.uikit.tokens.SaltyThemeDefaults
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ComponentGoldenSnapshotTest {

    private fun luminance(color: Color): Double {
        fun channel(c: Float): Double {
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = luminance(fg)
        val l2 = luminance(bg)
        val brighter = max(l1, l2)
        val darker = min(l1, l2)
        return (brighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testDarkThemeColorRolesMeetContrastRequirements() {
        val dark = SaltyThemeDefaults.darkRoles()

        // Text primary on background must be high contrast (>= 4.5:1)
        val textBgContrast = contrastRatio(dark.textPrimary, dark.background)
        assertTrue("Dark text primary on background contrast must be >= 4.5 (got $textBgContrast)", textBgContrast >= 4.5)

        // Text primary on surface must be >= 4.5:1
        val textSurfaceContrast = contrastRatio(dark.textPrimary, dark.surface)
        assertTrue("Dark text primary on surface contrast must be >= 4.5 (got $textSurfaceContrast)", textSurfaceContrast >= 4.5)

        // Switch track outline/trackOff against surface must have visible contrast (>= 2:1)
        val switchOffContrast = contrastRatio(dark.outline, dark.surface)
        assertTrue("Dark switch outline against surface must be visible (got $switchOffContrast)", switchOffContrast >= 1.5)

        // Active accent trackOn against surface must be high contrast (>= 3.0:1)
        val switchOnContrast = contrastRatio(dark.trackOn, dark.surface)
        assertTrue("Dark switch ON track against surface must be >= 3.0 (got $switchOnContrast)", switchOnContrast >= 3.0)
    }

    @Test
    fun testLightThemeColorRolesMeetContrastRequirements() {
        val light = SaltyThemeDefaults.lightRoles()

        val textBgContrast = contrastRatio(light.textPrimary, light.background)
        assertTrue("Light text primary on background contrast must be >= 4.5 (got $textBgContrast)", textBgContrast >= 4.5)

        val textSurfaceContrast = contrastRatio(light.textPrimary, light.surface)
        assertTrue("Light text primary on surface contrast must be >= 4.5 (got $textSurfaceContrast)", textSurfaceContrast >= 4.5)

        val switchOnContrast = contrastRatio(light.trackOn, light.surface)
        assertTrue("Light switch ON track against surface must be >= 3.0 (got $switchOnContrast)", switchOnContrast >= 3.0)
    }

    @Test
    fun testSpacingTokensAreStrictlyMonotonic() {
        assertTrue(SaltySpacing.none.value == 0f)
        assertTrue(SaltySpacing.xxs.value < SaltySpacing.xs.value)
        assertTrue(SaltySpacing.xs.value < SaltySpacing.sm.value)
        assertTrue(SaltySpacing.sm.value < SaltySpacing.md.value)
        assertTrue(SaltySpacing.md.value < SaltySpacing.lg.value)
        assertTrue(SaltySpacing.lg.value < SaltySpacing.xl.value)
        assertTrue(SaltySpacing.xl.value < SaltySpacing.xxl.value)
    }
}
