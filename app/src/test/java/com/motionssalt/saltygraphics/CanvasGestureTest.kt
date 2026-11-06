package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class CanvasGestureTest {

    @Test
    fun testScreenToCanvas1To1Translation() {
        val effectiveZoom = 1.5f
        val screenDx = 30f
        val screenDy = -45f

        val canvasDx = screenDx / effectiveZoom
        val canvasDy = screenDy / effectiveZoom

        assertEquals(20f, canvasDx, 0.001f)
        assertEquals(-30f, canvasDy, 0.001f)

        val initialTransform = Transform2D(translationX = 100f, translationY = 50f)
        val updated = initialTransform.copy(
            translationX = initialTransform.translationX + canvasDx,
            translationY = initialTransform.translationY + canvasDy
        )

        assertEquals(120f, updated.translationX, 0.001f)
        assertEquals(20f, updated.translationY, 0.001f)
    }

    @Test
    fun testRotationHandleAngleCalculationAndSnapping() {
        val layerCenterScreen = Pair(500f, 500f)

        fun calculateSnappedAngle(touchX: Float, touchY: Float): Float {
            val angleRad = atan2(touchY - layerCenterScreen.second, touchX - layerCenterScreen.first)
            var angleDeg = (Math.toDegrees(angleRad.toDouble()).toFloat() + 90f) % 360f
            if (angleDeg < 0f) angleDeg += 360f

            val snapAngles = floatArrayOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f, 360f)
            for (snap in snapAngles) {
                if (abs(angleDeg - snap) <= 3f) {
                    return if (snap == 360f) 0f else snap
                }
            }
            return angleDeg
        }

        // Touch straight up from center -> 0 degrees
        assertEquals(0f, calculateSnappedAngle(500f, 300f), 0.5f)

        // Touch slightly off 45 degrees (46.5 deg) -> snaps to 45
        // At 45 deg, dx = 100, dy = -100 relative to center (touch is top-right).
        // Angle from center: atan2(-100, 100) = -45 deg. + 90 = 45 deg.
        assertEquals(45f, calculateSnappedAngle(600f, 400f), 0.01f)

        // Touch at 43.5 degrees (within 3 degrees of 45) -> snaps to 45
        val testX = 500f + 100f * cos(Math.toRadians(-43.5 + 90.0)).toFloat()
        val testY = 500f + 100f * sin(Math.toRadians(-43.5 + 90.0)).toFloat()
        // angleDeg will be close to 45 -> snaps to 45
        val angleSnap = calculateSnappedAngle(500f + 100f * sin(Math.toRadians(46.0)).toFloat(), 500f - 100f * cos(Math.toRadians(46.0)).toFloat())
        assertEquals(45f, angleSnap, 0.01f)

        // Touch straight right -> 90 degrees
        assertEquals(90f, calculateSnappedAngle(700f, 500f), 0.5f)

        // Touch straight down -> 180 degrees
        assertEquals(180f, calculateSnappedAngle(500f, 700f), 0.5f)
    }

    @Test
    fun testAccurateBoundingBoxDimensions() {
        // Shape Layer
        val shapeLayer = Layer(
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 350f, height = 220f)
        )
        assertEquals(350f, shapeLayer.localWidth, 0.01f)
        assertEquals(220f, shapeLayer.localHeight, 0.01f)

        // Media Layer
        val mediaLayer = Layer(
            type = LayerType.MEDIA,
            mediaWidth = 800f,
            mediaHeight = 600f
        )
        assertEquals(800f, mediaLayer.localWidth, 0.01f)
        assertEquals(600f, mediaLayer.localHeight, 0.01f)

        // Vector Layer with points
        val vectorLayer = Layer(
            type = LayerType.VECTOR,
            vectorPathData = VectorPathData(
                points = listOf(
                    VectorPoint(10f, 20f),
                    VectorPoint(110f, 20f),
                    VectorPoint(110f, 170f),
                    VectorPoint(10f, 170f)
                )
            )
        )
        assertEquals(100f, vectorLayer.localWidth, 0.01f) // 110 - 10
        assertEquals(150f, vectorLayer.localHeight, 0.01f) // 170 - 20
    }

    @Test
    fun testLinkedVsUnlinkedScale() {
        val transformLinked = Transform2D(scaleX = 1.0f, scaleY = 1.0f, isScaleLinked = true)
        val scaledLinked = transformLinked.setScaleUniform(2.5f)
        assertEquals(2.5f, scaledLinked.scaleX, 0.01f)
        assertEquals(2.5f, scaledLinked.scaleY, 0.01f)

        val transformUnlinked = Transform2D(scaleX = 1.0f, scaleY = 1.0f, isScaleLinked = false)
        val scaledXOnly = transformUnlinked.copy(scaleX = 3.0f)
        assertEquals(3.0f, scaledXOnly.scaleX, 0.01f)
        assertEquals(1.0f, scaledXOnly.scaleY, 0.01f)
    }
}
