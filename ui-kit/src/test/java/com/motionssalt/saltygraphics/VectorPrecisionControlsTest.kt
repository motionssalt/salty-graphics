package com.motionssalt.saltygraphics

import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.motionssalt.saltygraphics.engine.model.BezierPoint
import com.motionssalt.saltygraphics.engine.model.VectorEditing
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

@RunWith(AndroidJUnit4::class)
class VectorPrecisionControlsTest {

    @Test
    fun testMakeCornerRemovesHandles() {
        val smoothPoint = BezierPoint(100f, 100f, inHandleX = 80f, inHandleY = 100f, outHandleX = 120f, outHandleY = 100f)
        assertTrue(smoothPoint.hasInHandle)
        assertTrue(smoothPoint.hasOutHandle)

        val cornerPoint = VectorEditing.makeCorner(smoothPoint)
        assertEquals(100f, cornerPoint.x)
        assertEquals(100f, cornerPoint.y)
        assertFalse(cornerPoint.hasInHandle)
        assertFalse(cornerPoint.hasOutHandle)
    }

    @Test
    fun testMakeSmoothCreatesCollinearOppositeHandles() {
        val cornerPoint = BezierPoint(200f, 200f)
        val smoothPoint = VectorEditing.makeSmooth(cornerPoint)

        assertTrue(smoothPoint.hasInHandle)
        assertTrue(smoothPoint.hasOutHandle)
        assertEquals(160f, smoothPoint.inHandleX)
        assertEquals(240f, smoothPoint.outHandleX)
        assertEquals(200f, smoothPoint.inHandleY)
        assertEquals(200f, smoothPoint.outHandleY)
    }

    @Test
    fun testAngleLock45Degrees() {
        val originX = 100f
        val originY = 100f

        // Near 45 degrees
        val targetX = 140f
        val targetY = 138f
        val (snappedX, snappedY) = VectorEditing.snapTo45Degrees(originX, originY, targetX, targetY)

        val angleRad = atan2(snappedY - originY, snappedX - originX)
        val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
        assertEquals(45f, angleDeg, 0.5f)

        // Distance preserved
        val origDist = hypot(targetX - originX, targetY - originY)
        val snapDist = hypot(snappedX - originX, snappedY - originY)
        assertEquals(origDist, snapDist, 0.01f)
    }

    @Test
    fun testIndependentHandlesVsMirrored() {
        val base = BezierPoint(100f, 100f, inHandleX = 60f, inHandleY = 100f, outHandleX = 140f, outHandleY = 100f)

        // When independent, moving inHandle does not alter outHandle
        val newInX = 50f
        val newInY = 90f
        val independent = base.copy(inHandleX = newInX, inHandleY = newInY)
        assertEquals(140f, independent.outHandleX)
        assertEquals(100f, independent.outHandleY)

        // When mirrored, moving inHandle reflects across anchor
        val dx = newInX - base.x
        val dy = newInY - base.y
        val mirrored = base.copy(
            inHandleX = newInX,
            inHandleY = newInY,
            outHandleX = base.x - dx,
            outHandleY = base.y - dy
        )
        assertEquals(150f, mirrored.outHandleX)
        assertEquals(110f, mirrored.outHandleY)
    }
}
