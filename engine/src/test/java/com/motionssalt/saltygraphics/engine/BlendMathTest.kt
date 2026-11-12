package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.model.BlendMode
import com.motionssalt.saltygraphics.engine.render.BlendMath
import org.junit.Assert.*
import org.junit.Test

class BlendMathTest {

    @Test
    fun testAllBlendModesCovered() {
        assertEquals("All 25 blend modes must be defined", 25, BlendMode.entries.size)
    }

    @Test
    fun testNormalBlend() {
        assertEquals(0.8f, BlendMath.blendChannel(BlendMode.NORMAL, 0.4f, 0.8f), 0.001f)
    }

    @Test
    fun testMultiplyBlend() {
        assertEquals(0.5f * 0.5f, BlendMath.blendChannel(BlendMode.MULTIPLY, 0.5f, 0.5f), 0.001f)
        assertEquals(0f, BlendMath.blendChannel(BlendMode.MULTIPLY, 0.0f, 0.9f), 0.001f)
        assertEquals(0.7f, BlendMath.blendChannel(BlendMode.MULTIPLY, 1.0f, 0.7f), 0.001f)
    }

    @Test
    fun testScreenBlend() {
        // Screen = 1 - (1 - 0.5)(1 - 0.5) = 0.75
        assertEquals(0.75f, BlendMath.blendChannel(BlendMode.SCREEN, 0.5f, 0.5f), 0.001f)
        assertEquals(1.0f, BlendMath.blendChannel(BlendMode.SCREEN, 1.0f, 0.2f), 0.001f)
        assertEquals(0.4f, BlendMath.blendChannel(BlendMode.SCREEN, 0.0f, 0.4f), 0.001f)
    }

    @Test
    fun testOverlayBlend() {
        // Overlay when b <= 0.5: 2 * b * s
        assertEquals(2f * 0.25f * 0.4f, BlendMath.blendChannel(BlendMode.OVERLAY, 0.25f, 0.4f), 0.001f)
        // Overlay when b > 0.5: 1 - 2*(1-b)*(1-s)
        val expected = 1f - 2f * 0.25f * 0.25f
        assertEquals(expected, BlendMath.blendChannel(BlendMode.OVERLAY, 0.75f, 0.75f), 0.001f)
    }

    @Test
    fun testDarkenAndLighten() {
        assertEquals(0.3f, BlendMath.blendChannel(BlendMode.DARKEN, 0.7f, 0.3f), 0.001f)
        assertEquals(0.7f, BlendMath.blendChannel(BlendMode.LIGHTEN, 0.7f, 0.3f), 0.001f)
    }

    @Test
    fun testLinearDodgeAndBurn() {
        assertEquals(0.8f, BlendMath.blendChannel(BlendMode.LINEAR_DODGE, 0.5f, 0.3f), 0.001f)
        assertEquals(1.0f, BlendMath.blendChannel(BlendMode.LINEAR_DODGE, 0.7f, 0.5f), 0.001f)
        assertEquals(0.2f, BlendMath.blendChannel(BlendMode.LINEAR_BURN, 0.7f, 0.5f), 0.001f)
        assertEquals(0.0f, BlendMath.blendChannel(BlendMode.LINEAR_BURN, 0.3f, 0.2f), 0.001f)
    }

    @Test
    fun testDifferenceAndExclusion() {
        assertEquals(0.4f, BlendMath.blendChannel(BlendMode.DIFFERENCE, 0.7f, 0.3f), 0.001f)
        assertEquals(0.4f, BlendMath.blendChannel(BlendMode.DIFFERENCE, 0.3f, 0.7f), 0.001f)
        // Exclusion = b + s - 2*b*s = 0.5 + 0.5 - 2*0.25 = 0.5
        assertEquals(0.5f, BlendMath.blendChannel(BlendMode.EXCLUSION, 0.5f, 0.5f), 0.001f)
    }

    @Test
    fun testSubtractAndDivide() {
        assertEquals(0.4f, BlendMath.blendChannel(BlendMode.SUBTRACT, 0.9f, 0.5f), 0.001f)
        assertEquals(0.0f, BlendMath.blendChannel(BlendMode.SUBTRACT, 0.3f, 0.5f), 0.001f)
        assertEquals(1.0f, BlendMath.blendChannel(BlendMode.DIVIDE, 0.5f, 0.5f), 0.001f)
        assertEquals(0.5f, BlendMath.blendChannel(BlendMode.DIVIDE, 0.25f, 0.5f), 0.001f)
    }

    @Test
    fun testFullPixelAlphaCompositing() {
        val opaqueBackdrop = 0xFFFFFFFFL // White
        val semiRedSource = 0x80FF0000L  // 50% Red
        val result = BlendMath.blendColor(BlendMode.NORMAL, opaqueBackdrop, semiRedSource, opacity = 1.0f)

        val a = (result shr 24 and 0xFF)
        val r = (result shr 16 and 0xFF)
        val g = (result shr 8 and 0xFF)
        val b = (result and 0xFF)

        assertEquals(255L, a)
        assertEquals(255L, r)
        // Green and Blue are blended 50% white + 50% zero = ~127
        assertTrue("Green should be intermediate between white and red", g in 120..135)
        assertTrue("Blue should be intermediate between white and red", b in 120..135)
    }
}
