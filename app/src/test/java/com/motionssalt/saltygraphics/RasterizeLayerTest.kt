package com.motionssalt.saltygraphics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RasterizeLayerTest {

    @Test
    fun rasterizeShapeLayerReplacesOriginalWithMediaLayer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shape = Layer(
            id = "shape1",
            name = "Test Rectangle",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 120f, height = 80f, fillColor = 0xFFFF0000L, hasFill = true),
            blendMode = BlendMode.MULTIPLY,
            opacity = 0.85f
        )
        val doc = ProjectDocument(
            id = "test_raster_proj",
            name = "Rasterize Test",
            canvasSize = CanvasSize(width = 300, height = 300),
            layers = listOf(shape)
        )

        val updatedDoc = LayerRasterizer.rasterizeLayer(context, doc, "shape1", duplicateOriginal = false)
        assertEquals(1, updatedDoc.layers.size)
        val rasterized = updatedDoc.layers.first()
        assertEquals(LayerType.MEDIA, rasterized.type)
        assertEquals(BlendMode.MULTIPLY, rasterized.blendMode)
        assertEquals(0.85f, rasterized.opacity, 0.001f)
        assertNotNull(rasterized.mediaAssetPath)
        val asset = File(File(context.filesDir, "projects/${doc.id}_assets"), rasterized.mediaAssetPath!!)
        assertTrue("Rasterized asset must exist at the production resolver path", asset.isFile)
        val pixels = android.graphics.BitmapFactory.decodeFile(asset.absolutePath)
        assertNotNull("Rasterized asset must decode", pixels)
        assertEquals(120, pixels.width)
        assertEquals(80, pixels.height)
        assertEquals(0xFFFF0000.toInt(), pixels.getPixel(60, 40))
    }

    @Test
    fun rasterizeWithDuplicatePreservesOriginalLayer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val textLayer = Layer(
            id = "text1",
            name = "Title Text",
            type = LayerType.TEXT,
            textData = TextLayerData(text = "Awesome Graphic", fontSize = 32f, color = 0xFF00FF00L)
        )
        val doc = ProjectDocument(
            id = "test_raster_dup",
            name = "Duplicate Test",
            canvasSize = CanvasSize(width = 400, height = 400),
            layers = listOf(textLayer)
        )

        val updatedDoc = LayerRasterizer.rasterizeLayer(context, doc, "text1", duplicateOriginal = true)
        assertEquals(2, updatedDoc.layers.size)
        val original = updatedDoc.layers.find { it.id == "text1" }
        assertNotNull(original)
        assertEquals(LayerType.TEXT, original?.type)

        val rasterized = updatedDoc.layers.find { it.id != "text1" }
        assertNotNull(rasterized)
        assertEquals(LayerType.MEDIA, rasterized?.type)
        assertEquals("Title Text (Rasterized)", rasterized?.name)
    }

    @Test
    fun rasterizePreservesContentOutsideCanvasBounds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Place a shape that partially extends outside a 200x200 canvas (e.g. translationX = 120, width = 100 -> extends to +170, canvas border is +100)
        val shape = Layer(
            id = "shape_off_canvas",
            name = "Off Canvas Shape",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 100f, height = 80f, fillColor = 0xFF00FF00L, hasFill = true),
            transform = Transform2D(translationX = 120f, translationY = 0f)
        )
        val doc = ProjectDocument(
            id = "test_off_canvas_raster",
            name = "Off Canvas Test",
            canvasSize = CanvasSize(width = 200, height = 200),
            layers = listOf(shape)
        )
        val updatedDoc = LayerRasterizer.rasterizeLayer(context, doc, "shape_off_canvas", duplicateOriginal = false)
        val rasterized = updatedDoc.layers.first()
        assertEquals(LayerType.MEDIA, rasterized.type)
        val asset = File(File(context.filesDir, "projects/${doc.id}_assets"), rasterized.mediaAssetPath!!)
        val pixels = android.graphics.BitmapFactory.decodeFile(asset.absolutePath)
        assertNotNull(pixels)
        // Full width of 100px must be preserved, not clipped to the 30px that fit inside canvas!
        assertTrue("Width must be full 100px, but was: ${pixels.width}", pixels.width in 98..102)
        assertTrue("Height must be full 80px, but was: ${pixels.height}", pixels.height in 78..82)
        // Center position must match the unclipped center around 120f
        assertEquals(120f, rasterized.transform.translationX, 2f)
    }

    @Test
    fun rasterizeVectorLayerCreatesPixelMediaLayer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val path = VectorPathData(
            points = listOf(
                BezierPoint(-40f, -40f),
                BezierPoint(40f, -40f),
                BezierPoint(40f, 40f),
                BezierPoint(-40f, 40f)
            ),
            isClosed = true,
            hasFill = true,
            fillColor = 0xFF0000FFL
        )
        val vectorLayer = Layer(
            id = "vector1",
            name = "Vector Path",
            type = LayerType.VECTOR,
            vectorPathData = path
        )
        val doc = ProjectDocument(
            id = "test_vector_raster",
            name = "Vector Raster Test",
            canvasSize = CanvasSize(width = 300, height = 300),
            layers = listOf(vectorLayer)
        )
        val updatedDoc = LayerRasterizer.rasterizeLayer(context, doc, "vector1", duplicateOriginal = false)
        assertEquals(1, updatedDoc.layers.size)
        val rasterized = updatedDoc.layers.first()
        assertEquals(LayerType.MEDIA, rasterized.type)
        assertNotNull(rasterized.mediaAssetPath)
        val asset = File(File(context.filesDir, "projects/${doc.id}_assets"), rasterized.mediaAssetPath!!)
        assertTrue(asset.isFile)
        val pixels = android.graphics.BitmapFactory.decodeFile(asset.absolutePath)
        assertNotNull(pixels)
        assertTrue(pixels.width in 75..85)
        assertTrue(pixels.height in 75..85)
    }

}
