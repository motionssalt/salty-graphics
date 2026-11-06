package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.action.GlobalClipboard
import com.motionssalt.saltygraphics.engine.action.PasteMode
import com.motionssalt.saltygraphics.engine.effect.EffectInstance
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.Transform2D
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CopyPasteTest {

    @Before
    fun setup() {
        GlobalClipboard.clear()
    }

    @Test
    fun testPasteTransformOnlyReplacesTransform() {
        val source = Layer(
            id = "source",
            name = "Source",
            transform = Transform2D(translationX = 50f, translationY = 80f, scaleX = 1.8f, scaleY = 1.8f, rotationDegrees = 45f),
            effects = listOf(EffectInstance(effectId = "source_effect"))
        )
        val target = Layer(
            id = "target",
            name = "Target",
            transform = Transform2D(translationX = 0f, translationY = 0f, scaleX = 1f, scaleY = 1f, rotationDegrees = 0f),
            effects = listOf(
                EffectInstance(effectId = "target_effect_1"),
                EffectInstance(effectId = "target_effect_2")
            )
        )

        GlobalClipboard.copy(source)
        val result = GlobalClipboard.applyTo(target, PasteMode.TRANSFORM_ONLY)

        // Transform replaced
        assertEquals(50f, result.transform.translationX, 0.001f)
        assertEquals(80f, result.transform.translationY, 0.001f)
        assertEquals(45f, result.transform.rotationDegrees, 0.001f)
        // Effects left untouched
        assertEquals(2, result.effects.size)
        assertEquals("target_effect_1", result.effects[0].effectId)
    }

    @Test
    fun testPasteEffectsOnlyReplacesEffects() {
        val source = Layer(
            id = "source",
            name = "Source",
            transform = Transform2D(translationX = 50f, translationY = 80f),
            effects = listOf(EffectInstance(effectId = "single_source_effect"))
        )
        val target = Layer(
            id = "target",
            name = "Target",
            transform = Transform2D(translationX = 10f, translationY = 20f),
            effects = (1..10).map { EffectInstance(effectId = "old_effect_$it") }
        )

        assertEquals(10, target.effects.size)

        GlobalClipboard.copy(source)
        val result = GlobalClipboard.applyTo(target, PasteMode.EFFECTS_ONLY)

        // As specified in 3.9: target ends up with only the copied effect (replaces existing)
        assertEquals(1, result.effects.size)
        assertEquals("single_source_effect", result.effects[0].effectId)
        // Transform left untouched
        assertEquals(10f, result.transform.translationX, 0.001f)
    }

    @Test
    fun testPasteBothReplacesTransformAndEffects() {
        val source = Layer(
            id = "source",
            name = "Source",
            transform = Transform2D(translationX = 99f, translationY = 99f, rotationDegrees = 90f),
            effects = listOf(EffectInstance(effectId = "new_effect"))
        )
        val target = Layer(
            id = "target",
            name = "Target",
            transform = Transform2D(translationX = 0f, translationY = 0f),
            effects = emptyList()
        )

        GlobalClipboard.copy(source)
        val result = GlobalClipboard.applyTo(target, PasteMode.BOTH)

        assertEquals(99f, result.transform.translationX, 0.001f)
        assertEquals(90f, result.transform.rotationDegrees, 0.001f)
        assertEquals(1, result.effects.size)
        assertEquals("new_effect", result.effects[0].effectId)
    }
}
