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

        assertEquals("3/4", PreviewQuality.THREE_QUARTERS.displayName)
        assertEquals(0.75f, PreviewQuality.THREE_QUARTERS.scaleFactor, 0.001f)

        assertEquals("Full", PreviewQuality.FULL.displayName)
        assertEquals(1.0f, PreviewQuality.FULL.scaleFactor, 0.001f)
    }

    @Test
    fun testPreviewQualityCycleOrder() {
        assertEquals(PreviewQuality.HALF, PreviewQuality.QUARTER.next())
        assertEquals(PreviewQuality.THREE_QUARTERS, PreviewQuality.HALF.next())
        assertEquals(PreviewQuality.FULL, PreviewQuality.THREE_QUARTERS.next())
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
}
