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

    @Test
    fun alphaCutoutLeavesCanvasBackgroundUntouched() {
        // AMENDMENT (V23): Canvas background is not a layer! Applying alpha cutout must not cut the canvas background or turn it black.
        val grayBackground = 0xFF808080L // Standard neutral gray canvas
        val redLayer = Layer(
            id = "layer_red",
            name = "Red Layer",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 100f, height = 100f, fillColor = 0xFFFF0000L, hasFill = true),
            transform = Transform2D(translationX = 0f, translationY = 0f)
        )
        val cutoutLayer = Layer(
            id = "layer_cutout",
            name = "Cutout Layer",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.CIRCLE, width = 50f, height = 50f, fillColor = 0xFF00FF00L, hasFill = true),
            transform = Transform2D(translationX = 0f, translationY = 0f),
            blendMode = BlendMode.ALPHA_CUTOUT_INWARD
        )
        val doc = ProjectDocument(
            id = "doc_canvas_protect",
            canvasSize = CanvasSize(width = 200, height = 200),
            editorSettings = EditorSettings(backgroundColor = grayBackground),
            layers = listOf(cutoutLayer, redLayer)
        )
        val composite = LayerCompositor.compositeDocument(doc, 200, 200)
        assertNotNull(composite)

        // At center (100, 100), the cutout layer punches out the red layer.
        // Because canvas background is NOT a layer, the pixel must show the gray canvas color, NOT black or transparent!
        val centerPixel = composite.getPixel(100, 100)
        assertEquals("Center pixel punched by cutout must retain the canvas background gray", grayBackground.toInt(), centerPixel)

        // At (70, 100), inside the red layer but outside the 50px circle cutout, red layer remains visible
        val redPixel = composite.getPixel(70, 100)
        assertEquals("Pixel outside cutout must retain red layer", 0xFFFF0000.toInt(), redPixel)

        // At corner (10, 10), outside all layers, canvas background remains gray
        val cornerPixel = composite.getPixel(10, 10)
        assertEquals("Corner pixel outside all layers must retain canvas background gray", grayBackground.toInt(), cornerPixel)
    }
