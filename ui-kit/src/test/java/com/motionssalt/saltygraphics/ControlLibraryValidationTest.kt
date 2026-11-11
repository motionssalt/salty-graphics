package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.engine.effect.ControlDefinition
import com.motionssalt.saltygraphics.engine.effect.ControlType
import com.motionssalt.saltygraphics.engine.effect.EffectRegistry
import com.motionssalt.saltygraphics.engine.effect.SliderStyle
import com.motionssalt.saltygraphics.ui.controls.ValueValidation
import org.junit.Assert.*
import org.junit.Test

class ControlLibraryValidationTest {

    @Test
    fun testValueValidationPositionControl() {
        val min = -5000f
        val max = 5000f
        val unit = "px"

        // In-range value
        val (valid1, err1) = ValueValidation.validate("250", min, max, unit)
        assertTrue("In-range value 250 must be valid", valid1)
        assertNull(err1)

        // Negative in-range value
        val (valid2, err2) = ValueValidation.validate("-1200", min, max, unit)
        assertTrue("In-range negative value -1200 must be valid", valid2)
        assertNull(err2)

        // Out-of-range value (above max)
        val (validAbove, errAbove) = ValueValidation.validate("5500", min, max, unit)
        assertFalse("Out-of-range value 5500 must be refused", validAbove)
        assertNotNull(errAbove)
        assertTrue(errAbove!!.contains("Allowed range"))

        // Out-of-range value (below min)
        val (validBelow, errBelow) = ValueValidation.validate("-6000", min, max, unit)
        assertFalse("Out-of-range value -6000 must be refused", validBelow)
        assertNotNull(errBelow)

        // Non-numeric text
        val (validText, errText) = ValueValidation.validate("abc", min, max, unit)
        assertFalse("Non-numeric text 'abc' must be refused", validText)
        assertNotNull(errText)
    }

    @Test
    fun testValueValidationOpacityControl() {
        val min = 0f
        val max = 1f
        val unit = "%"

        // In-range
        val (valid, _) = ValueValidation.validate("0.85", min, max, unit)
        assertTrue("0.85 must be valid for opacity", valid)

        // Out-of-range (negative)
        val (validNeg, errNeg) = ValueValidation.validate("-0.1", min, max, unit)
        assertFalse("Negative opacity must be refused", validNeg)
        assertNotNull(errNeg)

        // Out-of-range (> 1)
        val (validOver, errOver) = ValueValidation.validate("1.5", min, max, unit)
        assertFalse("Opacity > 1 must be refused", validOver)
        assertNotNull(errOver)

        // Non-numeric
        val (validJunk, _) = ValueValidation.validate("half", min, max, unit)
        assertFalse("Text 'half' must be refused", validJunk)
    }

    @Test
    fun testValueValidationFontSizeControl() {
        val min = 8f
        val max = 500f
        val unit = "px"

        // In-range
        val (valid, _) = ValueValidation.validate("72", min, max, unit)
        assertTrue("Font size 72px must be valid", valid)

        // Out-of-range (< 8)
        val (validSmall, errSmall) = ValueValidation.validate("2", min, max, unit)
        assertFalse("Font size 2px must be refused", validSmall)
        assertNotNull(errSmall)

        // Out-of-range (> 500)
        val (validLarge, errLarge) = ValueValidation.validate("999", min, max, unit)
        assertFalse("Font size 999px must be refused", validLarge)
        assertNotNull(errLarge)

        // Non-numeric
        val (validSymbols, errSymbols) = ValueValidation.validate("@#$%", min, max, unit)
        assertFalse("Special symbols must be refused", validSymbols)
        assertNotNull(errSymbols)
    }

    @Test
    fun testEffectDefinitionControlTypesAndStyles() {
        val testEffect = EffectRegistry.TEST_EFFECT
        assertEquals(6, testEffect.controls.size)

        val slider = testEffect.controls.find { it.type == ControlType.SLIDER }
        assertNotNull(slider)
        assertEquals(SliderStyle.RULER, slider?.sliderStyle)
        assertEquals("%", slider?.unit)

        val progress = testEffect.controls.find { it.type == ControlType.PROGRESS_SLIDER }
        assertNotNull(progress)
        assertEquals(SliderStyle.PROGRESS, progress?.sliderStyle)
        assertEquals("%", progress?.unit)

        val color = testEffect.controls.find { it.type == ControlType.COLOR_PICKER }
        assertNotNull(color)
        assertEquals(0xFF00E5FFL, color?.defaultColor)

        val toggle = testEffect.controls.find { it.type == ControlType.TOGGLE }
        assertNotNull(toggle)
        assertTrue(toggle?.defaultToggle == true)

        val dropdown = testEffect.controls.find { it.type == ControlType.DROPDOWN }
        assertNotNull(dropdown)
        assertEquals(4, dropdown?.options?.size)
    }
}
