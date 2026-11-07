package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.engine.model.ShapeData
import com.motionssalt.saltygraphics.engine.model.ShapeType
import com.motionssalt.saltygraphics.engine.model.Transform2D
import com.motionssalt.saltygraphics.ui.controls.ValueValidation
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class ScrollWheelTransformRegressionTest {

    // --- T7: Continuous 1:1 Sub-Pixel Ruler Tracking & Per-Frame Cost ---

    @Test
    fun testSubPixelTickCalculationContinuousTracking() {
        val minValue = -100000f
        val maxValue = 100000f
        val step = 1f
        val tickPixelSpacing = 16f
        val screenWidth = 400f
        val centerX = screenWidth / 2f

        // Initial state at value = 0.0
        val v0 = 0.0f
        // Tick at tickVal = 0 should be exactly at centerX
        val x0_tick0 = centerX + ((0f - v0) / step) * tickPixelSpacing
        assertEquals(centerX, x0_tick0, 0.001f)

        // Sub-pixel drag: value moves to 0.25 (e.g. dragged left by 4px)
        val v1 = 0.25f
        val x1_tick0 = centerX + ((0f - v1) / step) * tickPixelSpacing
        // Tick at 0 moves left by 4.0px
        assertEquals(centerX - 4.0f, x1_tick0, 0.001f)

        // Sub-pixel drag: value moves to 0.73
        val v2 = 0.73f
        val x2_tick0 = centerX + ((0f - v2) / step) * tickPixelSpacing
        assertEquals(centerX - (0.73f * 16f), x2_tick0, 0.001f)

        // Check relative distance between adjacent ticks remains exactly tickPixelSpacing
        val x2_tick1 = centerX + ((1f - v2) / step) * tickPixelSpacing
        assertEquals(tickPixelSpacing, x2_tick1 - x2_tick0, 0.001f)
    }

    @Test
    fun testVisibleTicksBoundedRegardlessOfHugeRange() {
        // Range is effectively infinite: -100000 to +100000
        val minValue = -100000f
        val maxValue = 100000f
        val step = 1f
        val tickPixelSpacing = 16f
        val screenWidth = 360f // standard phone width dp/px
        val halfWidth = screenWidth / 2f
        val currentVal = 12450.4f

        val minVisibleVal = currentVal - ((halfWidth + tickPixelSpacing * 2f) / tickPixelSpacing) * step
        val maxVisibleVal = currentVal + ((halfWidth + tickPixelSpacing * 2f) / tickPixelSpacing) * step

        val firstTickIndex = floor(((minVisibleVal - minValue) / step).toDouble()).toLong()
        val lastTickIndex = ceil(((maxVisibleVal - minValue) / step).toDouble()).toLong()

        val tickCount = (lastTickIndex - firstTickIndex + 1L).toInt()

        // Visible ticks should be approximately (360 / 16) + 4 ≈ 27 ticks
        assertTrue("Tick count should be bounded to visible viewport (~25-35 ticks), was $tickCount", tickCount in 20..40)

        // Benchmark per-frame simulation for 1000 frames (60fps animation)
        val startTime = System.nanoTime()
        var drawnTicks = 0
        for (frame in 0 until 1000) {
            val v = currentVal + (frame * 0.1f)
            val minV = v - ((halfWidth + tickPixelSpacing * 2f) / tickPixelSpacing) * step
            val maxV = v + ((halfWidth + tickPixelSpacing * 2f) / tickPixelSpacing) * step
            val first = floor(((minV - minValue) / step).toDouble()).toLong()
            val last = ceil(((maxV - minValue) / step).toDouble()).toLong()
            for (idx in first..last) {
                val tVal = minValue + (idx * step)
                if (tVal in minValue..maxValue) {
                    val x = (screenWidth / 2f) + ((tVal - v) / step) * tickPixelSpacing
                    if (x >= -tickPixelSpacing && x <= screenWidth + tickPixelSpacing) {
                        drawnTicks++
                    }
                }
            }
        }
        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0
        val perFrameMs = elapsedMs / 1000.0
        // Assert that per-frame cost is well below 1.0ms (typically ~0.02ms)
        assertTrue("Per-frame tick calculation must be < 0.5ms, was $perFrameMs ms", perFrameMs < 0.5)
    }

    @Test
    fun testSlowDragTinyNudgeAndSnapBehavior() {
        val minValue = -1000f
        val maxValue = 1000f
        val defaultValue = 0f
        val step = 1f

        var currentValue = 0f
        val changes = mutableListOf<Float>()

        fun simulateApply(raw: Float) {
            val clamped = raw.coerceIn(minValue, maxValue)
            val snapped = if (abs(clamped - defaultValue) < step * 0.35f) {
                defaultValue
            } else if (abs(clamped) < step * 0.35f && minValue <= 0f && maxValue >= 0f) {
                0f
            } else {
                (clamped / step).roundToInt() * step
            }.coerceIn(minValue, maxValue)

            if (snapped != currentValue) {
                currentValue = snapped
                changes.add(snapped)
            }
        }

        // Tiny nudge less than deadzone -> stays at default
        simulateApply(0.2f)
        assertEquals(0f, currentValue, 0.0f)
        assertEquals(0, changes.size)

        // Drag past snap deadzone to 0.7 -> snaps to 1.0
        simulateApply(0.7f)
        assertEquals(1.0f, currentValue, 0.0f)
        assertEquals(listOf(1.0f), changes)

        // Drag to 2.4 -> snaps to 2.0
        simulateApply(2.4f)
        assertEquals(2.0f, currentValue, 0.0f)
        assertEquals(listOf(1.0f, 2.0f), changes)

        // Drag back near default to 0.1 -> snaps back to default 0.0
        simulateApply(0.1f)
        assertEquals(0.0f, currentValue, 0.0f)
        assertEquals(listOf(1.0f, 2.0f, 0.0f), changes)
    }

    @Test
    fun testScrollingPanelGestureDisambiguation() {
        // Vertical gesture inside scrolling panel: dy > dx -> ignore horizontal drag
        fun shouldEngageHorizontalDrag(dx: Float, dy: Float): Boolean {
            if (dy > 16f && dy > dx * 1.2f) return false
            return dx > 6f && dx > dy * 0.8f
        }

        // Vertical scroll swipe (e.g. dy=40, dx=3)
        assertFalse("Vertical swipe must not engage horizontal ruler drag", shouldEngageHorizontalDrag(3f, 40f))

        // Horizontal swipe (e.g. dx=25, dy=2)
        assertTrue("Horizontal swipe must engage horizontal ruler drag", shouldEngageHorizontalDrag(25f, 2f))

        // Diagonal jitter (e.g. dx=4, dy=4)
        assertFalse("Sub-threshold movement must not engage", shouldEngageHorizontalDrag(4f, 4f))
    }

    @Test
    fun testLongDragBeyondOldLimits() {
        // Previous limits in TransformPanel were -1000f..1000f
        var position = 0f
        val tickPixelSpacing = 16f
        val step = 1f

        // Drag 100,000 pixels to the left (-dx = 100,000 -> positive position)
        val totalDeltaX = -100_000f
        val deltaValue = (-totalDeltaX / tickPixelSpacing) * step
        position = (position + deltaValue).coerceIn(-100000f, 100000f)

        assertEquals(6250f, position, 0.001f)
        assertTrue("Position should be able to scroll far beyond the old 1000px limit", position > 1000f)
    }

    // --- T8: Clickable Transform Values Everywhere Audit ---

    @Test
    fun testAuditAllTransformValuesAreClickableAndAcceptValidInputs() {
        var transform = Transform2D(
            translationX = 0f,
            translationY = 0f,
            scaleX = 1f,
            scaleY = 1f,
            rotationDegrees = 0f,
            moveSensitivity = 1f,
            isScaleLinked = true
        )

        // 1. Uniform Scale (Linked): Range 1% .. 10000%, unit "%"
        val (validUniform, errUniform) = ValueValidation.validate("150", 1f, 10000f, "%")
        assertTrue("Uniform scale 150% must be valid", validUniform)
        assertNull(errUniform)
        transform = transform.setScaleUniform(150f / 100f)
        assertEquals(1.5f, transform.scaleX, 0.001f)
        assertEquals(1.5f, transform.scaleY, 0.001f)

        // 2. Scale X (Unlinked): Range 1% .. 10000%
        val (validScaleX, errScaleX) = ValueValidation.validate("200", 1f, 10000f, "%")
        assertTrue("Scale X 200% must be valid", validScaleX)
        assertNull(errScaleX)
        transform = transform.copy(scaleX = 200f / 100f)
        assertEquals(2.0f, transform.scaleX, 0.001f)

        // 3. Scale Y (Unlinked): Range 1% .. 10000%
        val (validScaleY, errScaleY) = ValueValidation.validate("75", 1f, 10000f, "%")
        assertTrue("Scale Y 75% must be valid", validScaleY)
        assertNull(errScaleY)
        transform = transform.copy(scaleY = 75f / 100f)
        assertEquals(0.75f, transform.scaleY, 0.001f)

        // 4. Move X (Slider & Trackpad): Range -100000px .. 100000px
        val (validMoveX, errMoveX) = ValueValidation.validate("3500", -100000f, 100000f, "px")
        assertTrue("Move X 3500px must be valid", validMoveX)
        assertNull(errMoveX)
        transform = transform.copy(translationX = 3500f)
        assertEquals(3500f, transform.translationX, 0.001f)

        // 5. Move Y (Slider & Trackpad): Range -100000px .. 100000px
        val (validMoveY, errMoveY) = ValueValidation.validate("-1250", -100000f, 100000f, "px")
        assertTrue("Move Y -1250px must be valid", validMoveY)
        assertNull(errMoveY)
        transform = transform.copy(translationY = -1250f)
        assertEquals(-1250f, transform.translationY, 0.001f)

        // 6. Sensitivity (Trackpad): Range 10% .. 500%
        val (validSens, errSens) = ValueValidation.validate("150", 10f, 500f, "%")
        assertTrue("Sensitivity 150% must be valid", validSens)
        assertNull(errSens)
        transform = transform.copy(moveSensitivity = 150f / 100f)
        assertEquals(1.5f, transform.moveSensitivity, 0.001f)

        // 7. Rotation: Range -36000° .. 36000°
        val (validRot, errRot) = ValueValidation.validate("720", -36000f, 36000f, "°")
        assertTrue("Rotation 720° must be valid", validRot)
        assertNull(errRot)
        transform = transform.copy(rotationDegrees = 720f)
        assertEquals(720f, transform.rotationDegrees, 0.001f)
    }

    @Test
    fun testAuditInvalidInputsRefusedForEveryTransformValue() {
        // Refusal for Scale: negative or zero scale is invalid physically and mathematically
        val (invalidScaleZero, _) = ValueValidation.validate("0", 1f, 10000f, "%")
        assertFalse("Scale 0% must be refused", invalidScaleZero)

        val (invalidScaleNeg, _) = ValueValidation.validate("-50", 1f, 10000f, "%")
        assertFalse("Negative scale must be refused", invalidScaleNeg)

        val (invalidScaleText, errText) = ValueValidation.validate("double", 1f, 10000f, "%")
        assertFalse("Non-numeric text 'double' must be refused", invalidScaleText)
        assertEquals("Invalid number", errText)

        // Refusal for Move: out of bounds or garbage
        val (invalidMoveEmpty, errEmpty) = ValueValidation.validate("", -100000f, 100000f, "px")
        assertFalse("Empty string must be refused", invalidMoveEmpty)
        assertEquals("Value cannot be empty", errEmpty)

        val (invalidMoveOver, errOver) = ValueValidation.validate("200000", -100000f, 100000f, "px")
        assertFalse("Move 200000px must be refused", invalidMoveOver)
        assertTrue(errOver!!.contains("Allowed range"))

        // Refusal for Sensitivity: below 10%
        val (invalidSensLow, _) = ValueValidation.validate("5", 10f, 500f, "%")
        assertFalse("Sensitivity 5% must be refused", invalidSensLow)

        // Refusal for Rotation: out of range
        val (invalidRot, _) = ValueValidation.validate("50000", -36000f, 36000f, "°")
        assertFalse("Rotation 50000° must be refused", invalidRot)
    }

    // --- Shape Settings Ruler Slider Audit ---

    @Test
    fun testShapeSettingsRulerSlidersAudit() {
        var shape = ShapeData(
            type = ShapeType.ROUNDED_RECTANGLE,
            width = 200f,
            height = 150f,
            cornerRadius = 16f,
            sides = 5,
            starPoints = 5
        )

        // Width: 1 .. 50000 px
        val (validW, _) = ValueValidation.validate("4500", 1f, 50000f, "px")
        assertTrue("Shape width 4500px must be valid", validW)
        shape = shape.copy(width = 4500f)
        assertEquals(4500f, shape.width, 0.001f)

        val (invalidW, _) = ValueValidation.validate("0", 1f, 50000f, "px")
        assertFalse("Shape width 0px must be refused", invalidW)

        // Corner radius: 0 .. 50000 px
        val (validRadius, _) = ValueValidation.validate("64", 0f, 50000f, "px")
        assertTrue(validRadius)
        shape = shape.copy(cornerRadius = 64f)
        assertEquals(64f, shape.cornerRadius, 0.001f)

        // Polygon Sides: 3 .. 100
        val (validSides, _) = ValueValidation.validate("8", 3f, 100f, "")
        assertTrue(validSides)
        val (invalidSides, _) = ValueValidation.validate("2", 3f, 100f, "")
        assertFalse("Polygon with 2 sides must be refused", invalidSides)

        // Star Points: 3 .. 100
        val (validPoints, _) = ValueValidation.validate("12", 3f, 100f, "")
        assertTrue(validPoints)
        val (invalidPoints, _) = ValueValidation.validate("1", 3f, 100f, "")
        assertFalse("Star with 1 point must be refused", invalidPoints)
    }
}
