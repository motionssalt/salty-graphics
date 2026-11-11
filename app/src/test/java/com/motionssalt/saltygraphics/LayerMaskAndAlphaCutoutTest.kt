package com.motionssalt.saltygraphics

import android.graphics.Bitmap
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayerMaskAndAlphaCutoutTest {

    @Test
    fun alphaCutoutInwardMaskPunchesHoleInBackdrop() {
        val bottomLayer = Layer(
            id = "backdrop",
            name = "Backdrop",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 200f, height = 200f, fillColor = 0xFF0000FFL, hasFill = true)
        )
        val cutoutLayer = Layer(
            id = "cutout_punch",
            name = "Cutout Layer",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.CIRCLE, width = 60f, height = 60f, fillColor = 0xFFFF0000L, hasFill = true),
            maskMode = MaskMode.IN_MASK
        )
        val doc = ProjectDocument(
            id = "test_doc_cutout",
            name = "Cutout Doc",
            canvasSize = CanvasSize(width = 200, height = 200),
            editorSettings = EditorSettings(backgroundColor = 0L),
            layers = listOf(cutoutLayer, bottomLayer)
        )

        val fullComposite = LayerCompositor.compositeDocument(doc, 200, 200)
        assertNotNull(fullComposite)

        // Center pixel (100, 100) where circle punches through should have reduced or 0 alpha
        val centerPixel = fullComposite.getPixel(100, 100)
        val centerAlpha = (centerPixel ushr 24) and 0xFF
        assertEquals("Inward cutout must punch a hole through backdrop", 0L, centerAlpha.toLong())
    }

    @Test
    fun alphaCutoutOutwardMaskMattesBackdrop() {
        val bottomLayer = Layer(
            id = "backdrop",
            name = "Backdrop",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 200f, height = 200f, fillColor = 0xFF0000FFL, hasFill = true)
        )
        val cutoutLayer = Layer(
            id = "cutout_matte",
            name = "Cutout Layer",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.CIRCLE, width = 60f, height = 60f, fillColor = 0xFFFF0000L, hasFill = true),
            maskMode = MaskMode.OUT_MASK
        )
        val doc = ProjectDocument(
            id = "test_doc_matte",
            name = "Matte Doc",
            canvasSize = CanvasSize(width = 200, height = 200),
            editorSettings = EditorSettings(backgroundColor = 0L),
            layers = listOf(cutoutLayer, bottomLayer)
        )

        val fullComposite = LayerCompositor.compositeDocument(doc, 200, 200)
        assertNotNull(fullComposite)

        // Center pixel (100, 100) where circle exists should retain opacity
        val centerPixel = fullComposite.getPixel(100, 100)
        val centerAlpha = (centerPixel ushr 24) and 0xFF
        assertTrue("Outward matte retains backdrop inside mask shape", centerAlpha > 200)

        // Edge pixel (10, 10) outside circle should be matted out (0 alpha)
        val cornerPixel = fullComposite.getPixel(10, 10)
        val cornerAlpha = (cornerPixel ushr 24) and 0xFF
        assertEquals("Outward matte eliminates backdrop outside mask shape", 0L, cornerAlpha.toLong())
    }
}
