package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.effect.*
import org.junit.Assert.*
import org.junit.Test

class EffectDefinitionTest {

    @Test
    fun testShipZeroRealEffectsInBeta() {
        // As mandated in Section 4.2: The Effects panel stays empty in the beta
        val betaEffects = EffectRegistry.getAvailableEffects(includeDebug = false)
        assertTrue("Beta must ship zero real effects", betaEffects.isEmpty())
    }

    @Test
    fun testTestHarnessExercisesAllSixControlTypes() {
        val testDef = EffectRegistry.TEST_EFFECT
        val controlTypes = testDef.controls.map { it.type }.toSet()

        assertTrue("Must include TOGGLE", controlTypes.contains(ControlType.TOGGLE))
        assertTrue("Must include SLIDER", controlTypes.contains(ControlType.SLIDER))
        assertTrue("Must include DROPDOWN", controlTypes.contains(ControlType.DROPDOWN))
        assertTrue("Must include COLOR_PICKER", controlTypes.contains(ControlType.COLOR_PICKER))
        assertTrue("Must include PROGRESS_SLIDER", controlTypes.contains(ControlType.PROGRESS_SLIDER))
        assertTrue("Must include LAYER_SELECTOR", controlTypes.contains(ControlType.LAYER_SELECTOR))

        assertEquals("Should exercise exactly 6 control types", 6, controlTypes.size)

        // Test parameter instantiation & reading
        val instance = EffectInstance(
            effectId = testDef.id,
            isEnabled = true,
            floatParams = mapOf("intensity_slider" to 0.75f, "progress_sweep" to 0.4f),
            boolParams = mapOf("toggle_active" to true),
            colorParams = mapOf("tint_color" to 0xFFFF00FFL),
            stringParams = mapOf("reference_layer" to "layer_123", "mode_dropdown" to "Add")
        )

        assertEquals(0.75f, instance.getFloat("intensity_slider"), 0.001f)
        assertEquals(0.4f, instance.getFloat("progress_sweep"), 0.001f)
        assertTrue(instance.getBool("toggle_active"))
        assertEquals(0xFFFF00FFL, instance.getColor("tint_color"))
        assertEquals("layer_123", instance.getString("reference_layer"))
        assertEquals("Add", instance.getString("mode_dropdown"))
    }
}
