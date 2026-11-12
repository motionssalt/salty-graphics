package com.motionssalt.saltygraphics

import android.graphics.Bitmap
import android.graphics.Color
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EyedropperSamplingTest {

    @Test
    fun sampleCompositePixelReturnsAccurateColorAndAlpha() {
        // Create document with colored shape on canvas
        val shape = Layer(
            id = "sample_target",
            name = "Target Shape",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 100f, height = 100f, fillColor = 0xFF2979FFL, hasFill = true),
            transform = Transform2D(translationX = 0f, translationY = 0f)
        )
        val doc = ProjectDocument(
            id = "eyedropper_doc",
            canvasSize = CanvasSize(200, 200),
            editorSettings = EditorSettings(backgroundColor = 0xFF808080L),
            layers = listOf(shape)
        )

        val composite = LayerCompositor.compositeDocument(doc, 200, 200)
        assertNotNull(composite)

        // 1. Sample at center (100, 100) -> Should be the blue shape color (0xFF2979FF)
        val centerPixel = composite.getPixel(100, 100)
        val sampledColor = centerPixel.toLong() and 0xFFFFFFFFL
        assertEquals(0xFF2979FFL, sampledColor)

        // 2. Sample at corner (10, 10) -> Outside shape, should be gray canvas background (0xFF808080)
        val cornerPixel = composite.getPixel(10, 10)
        val cornerColor = cornerPixel.toLong() and 0xFFFFFFFFL
        assertEquals(0xFF808080L, cornerColor)

        // 3. Apply sampled color to a target layer's fill
        val updatedShape = shape.copy(
            fillData = FillData(type = FillType.SOLID, solidColor = sampledColor, solidOpacity = 1f)
        )
        val updatedDoc = doc.updateLayer(shape.id) { updatedShape }
        assertEquals(0xFF2979FFL, updatedDoc.findLayerById(shape.id)?.fillData?.solidColor)
    }
}
