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


    @Test
    fun rasterizeSelectionFlattensMultipleLayersAtTopMostPositionRespectingOpacity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bottom = Layer(
            id = "b",
            name = "Bottom",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 100f, height = 100f, fillColor = 0xFFFF0000L, hasFill = true),
            transform = Transform2D(translationX = -20f, translationY = 0f)
        )
        val top = Layer(
            id = "t",
            name = "Top",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 100f, height = 100f, fillColor = 0xFF00FF00L, hasFill = true),
            transform = Transform2D(translationX = 20f, translationY = 0f),
            opacity = 0.5f
        )
        val above = Layer(
            id = "above",
            name = "Above",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 40f, height = 40f, fillColor = 0xFF0000FFL, hasFill = true)
        )
        val below = Layer(
            id = "below",
            name = "Below",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 40f, height = 40f, fillColor = 0xFFFFFFFFL, hasFill = true)
        )
        val doc = ProjectDocument(
            id = "test_multi_merge",
            canvasSize = CanvasSize(width = 300, height = 300),
            layers = listOf(above, top, bottom, below)
        )
        val mergedDoc = LayerRasterizer.rasterizeSelection(
            context = context,
            document = doc,
            layerIds = setOf("t", "b"),
            duplicateOriginal = false
        )
        assertEquals(3, mergedDoc.layers.size)
        assertEquals("above", mergedDoc.layers[0].id)
        val merged = mergedDoc.layers[1]
        assertEquals(LayerType.MEDIA, merged.type)
        assertEquals("below", mergedDoc.layers[2].id)
        val asset = File(File(context.filesDir, "projects/${doc.id}_assets"), merged.mediaAssetPath!!)
        val bmp = android.graphics.BitmapFactory.decodeFile(asset.absolutePath)
        assertNotNull(bmp)
        assertTrue("Merged bounds should span both -20..+20 shapes (140px), got ${bmp.width}", bmp.width in 135..145)
        // Left side is pure bottom red, middle overlap blends top 50% green over red, right side is 50% green
        val leftPx = bmp.getPixel(10, 50)
        assertTrue(android.graphics.Color.red(leftPx) > 200 && android.graphics.Color.green(leftPx) < 20)
        val rightPx = bmp.getPixel(bmp.width - 10, 50)
        assertTrue("50% opacity green preserved in alpha=${android.graphics.Color.alpha(rightPx)}", android.graphics.Color.alpha(rightPx) in 115..140)
        bmp.recycle()
    }

    @Test
    fun rasterizeSelectionFlattensGroupRespectingVisibilityAndTransforms() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val visibleChild = Layer(
            id = "c_vis",
            name = "Visible",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 80f, height = 80f, fillColor = 0xFF0000FFL, hasFill = true)
        )
        val hiddenChild = Layer(
            id = "c_hid",
            name = "Hidden",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 200f, height = 200f, fillColor = 0xFFFF0000L, hasFill = true),
            isVisible = false
        )
        val group = Layer(
            id = "grp_1",
            name = "My Group",
            type = LayerType.GROUP,
            children = listOf(hiddenChild, visibleChild),
            transform = Transform2D(translationX = 40f, translationY = -20f)
        )
        val doc = ProjectDocument(
            id = "test_group_raster",
            canvasSize = CanvasSize(width = 300, height = 300),
            layers = listOf(group)
        )
        val updated = LayerRasterizer.rasterizeSelection(context, doc, setOf("grp_1"), duplicateOriginal = true)
        assertEquals(2, updated.layers.size)
        val rasterized = updated.layers[0]
        assertEquals(LayerType.MEDIA, rasterized.type)
        assertEquals("My Group (Rasterized)", rasterized.name)
        assertFalse(updated.layers[1].isVisible)
        assertEquals(40f, rasterized.transform.translationX, 2f)
        assertEquals(-20f, rasterized.transform.translationY, 2f)
        val asset = File(File(context.filesDir, "projects/${doc.id}_assets"), rasterized.mediaAssetPath!!)
        val bmp = android.graphics.BitmapFactory.decodeFile(asset.absolutePath)
        assertNotNull(bmp)
        // Hidden 200x200 child must NOT inflate bounds beyond visible 80x80 child!
        assertTrue("Group raster should match 80px visible child, not 200px hidden child; was ${bmp.width}", bmp.width in 76..84)
        assertEquals(0xFF0000FF.toInt(), bmp.getPixel(bmp.width / 2, bmp.height / 2))
        bmp.recycle()
    }
}
