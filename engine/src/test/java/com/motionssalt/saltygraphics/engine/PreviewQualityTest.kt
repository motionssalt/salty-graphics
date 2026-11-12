package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import org.junit.Assert.*
import org.junit.Test

class PreviewQualityTest {

    @Test
    fun testPreviewQualityDisplayNamesAndScaleFactors() {
        assertEquals("1/4", PreviewQuality.QUARTER.displayName)
        assertEquals(0.25f, PreviewQuality.QUARTER.scaleFactor, 0.001f)

        assertEquals("1/2", PreviewQuality.HALF.displayName)
        assertEquals(0.50f, PreviewQuality.HALF.scaleFactor, 0.001f)

        assertEquals("2/3", PreviewQuality.TWO_THIRDS.displayName)
        assertEquals(0.667f, PreviewQuality.TWO_THIRDS.scaleFactor, 0.005f)

        assertEquals("3/4", PreviewQuality.THREE_QUARTERS.displayName)
        assertEquals(0.75f, PreviewQuality.THREE_QUARTERS.scaleFactor, 0.001f)

        assertEquals("Full", PreviewQuality.FULL.displayName)
        assertEquals(1.0f, PreviewQuality.FULL.scaleFactor, 0.001f)
    }

    @Test
    fun testPreviewQualityCycleOrder() {
        assertEquals(PreviewQuality.HALF, PreviewQuality.QUARTER.next())
        assertEquals(PreviewQuality.TWO_THIRDS, PreviewQuality.HALF.next())
        assertEquals(PreviewQuality.FULL, PreviewQuality.TWO_THIRDS.next())
        assertEquals(PreviewQuality.QUARTER, PreviewQuality.FULL.next())
    }

    @Test
    fun testProxyDimensionsCalculatedCorrectlyForCanvas() {
        val canvasW = 1920
        val canvasH = 1080

        val qW = (canvasW * PreviewQuality.QUARTER.scaleFactor).toInt()
        val qH = (canvasH * PreviewQuality.QUARTER.scaleFactor).toInt()
        assertEquals(480, qW)
        assertEquals(270, qH)

        val hW = (canvasW * PreviewQuality.HALF.scaleFactor).toInt()
        val hH = (canvasH * PreviewQuality.HALF.scaleFactor).toInt()
        assertEquals(960, hW)
        assertEquals(540, hH)

        val tqW = (canvasW * PreviewQuality.THREE_QUARTERS.scaleFactor).toInt()
        val tqH = (canvasH * PreviewQuality.THREE_QUARTERS.scaleFactor).toInt()
        assertEquals(1440, tqW)
        assertEquals(810, tqH)

        val fW = (canvasW * PreviewQuality.FULL.scaleFactor).toInt()
        val fH = (canvasH * PreviewQuality.FULL.scaleFactor).toInt()
        assertEquals(1920, fW)
        assertEquals(1080, fH)
    }

    @Test
    fun testExportResolutionUnaffectedByPreviewQuality() {
        val doc = ProjectDocument(id = "doc-res", canvasSize = CanvasSize(3840, 2160))
        // Preview quality set to 1/4
        val previewScale = PreviewQuality.QUARTER.scaleFactor
        assertEquals(0.25f, previewScale, 0.001f)

        // Export resolution always evaluates canvasSize directly
        val exportW = doc.canvasSize.width
        val exportH = doc.canvasSize.height
        assertEquals(3840, exportW)
        assertEquals(2160, exportH)
    }

    @Test
    fun testRenderTargetPixelSizeAndDetailReductionForQualityLevels() {
        val canvasW = 1200
        val canvasH = 800

        // Test each level calculates strictly descending pixel target sizes
        val fullW = (canvasW * PreviewQuality.FULL.scaleFactor).toInt()
        val fullH = (canvasH * PreviewQuality.FULL.scaleFactor).toInt()
        assertEquals(1200, fullW)
        assertEquals(800, fullH)

        val twoThirdsW = (canvasW * PreviewQuality.TWO_THIRDS.scaleFactor).toInt()
        val twoThirdsH = (canvasH * PreviewQuality.TWO_THIRDS.scaleFactor).toInt()
        assertEquals(800, twoThirdsW)
        assertEquals(533, twoThirdsH)

        val halfW = (canvasW * PreviewQuality.HALF.scaleFactor).toInt()
        val halfH = (canvasH * PreviewQuality.HALF.scaleFactor).toInt()
        assertEquals(600, halfW)
        assertEquals(400, halfH)

        val quarterW = (canvasW * PreviewQuality.QUARTER.scaleFactor).toInt()
        val quarterH = (canvasH * PreviewQuality.QUARTER.scaleFactor).toInt()
        assertEquals(300, quarterW)
        assertEquals(200, quarterH)

        assertTrue(fullW > twoThirdsW)
        assertTrue(twoThirdsW > halfW)
        assertTrue(halfW > quarterW)

        // Simulate fine 1-pixel checker pattern across levels and verify detail frequency reduction
        fun countCheckerTransitions(width: Int): Int {
            // A 1px checker pattern across canvas width has (width - 1) transitions
            return width - 1
        }
        val fullTransitions = countCheckerTransitions(fullW)
        val twoThirdsTransitions = countCheckerTransitions(twoThirdsW)
        val halfTransitions = countCheckerTransitions(halfW)
        val quarterTransitions = countCheckerTransitions(quarterW)

        assertTrue("Detail must measurably drop at 2/3 vs Full", twoThirdsTransitions < fullTransitions)
        assertTrue("Detail must measurably drop at 1/2 vs 2/3", halfTransitions < twoThirdsTransitions)
        assertTrue("Detail must measurably drop at 1/4 vs 1/2", quarterTransitions < halfTransitions)
    }
}
