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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GroupCompositingAndContextTest {

    @Test
    fun groupRendersAllChildTypesRecursively() {
        // Create children of multiple types: Shape, Text, and Vector
        val shapeChild = Layer(
            id = "child_shape",
            name = "Shape Child",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 100f, height = 100f, fillColor = 0xFFFF0000L, hasFill = true),
            transform = Transform2D(scaleX = 1f, scaleY = 1f)
        )
        val textChild = Layer(
            id = "child_text",
            name = "Text Child",
            type = LayerType.TEXT,
            textData = TextLayerData(text = "Hello", fontSize = 24f, color = 0xFF00FF00L),
            transform = Transform2D(translationY = 50f)
        )
        val vectorChild = Layer(
            id = "child_vector",
            name = "Vector Child",
            type = LayerType.VECTOR,
            vectorPathData = ShapeData(type = ShapeType.CIRCLE, width = 80f, height = 80f).toVectorPath().copy(
                hasFill = true,
                fillColor = 0xFF0000FFL
            )
        )

        // Nested child group
        val nestedChild = Layer(
            id = "child_nested",
            name = "Nested Child",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 40f, height = 40f, fillColor = 0xFFFFFF00L, hasFill = true)
        )
        val nestedGroup = Layer(
            id = "group_nested",
            name = "Nested Group",
            type = LayerType.GROUP,
            children = listOf(nestedChild),
            transform = Transform2D(translationX = 100f)
        )

        val rootGroup = Layer(
            id = "group_root",
            name = "Root Group",
            type = LayerType.GROUP,
            children = listOf(shapeChild, textChild, vectorChild, nestedGroup),
            transform = Transform2D(translationX = 20f, translationY = 30f)
        )

        val doc = ProjectDocument(
            id = "test_doc",
            name = "Group Test",
            canvasSize = CanvasSize(width = 400, height = 400),
            layers = listOf(rootGroup)
        )

        val rendered = LayerCompositor.renderLayerContent(rootGroup, 400, 400, doc, forceVisible = true)
        assertNotNull(rendered)
        assertEquals(400, rendered.width)
        assertEquals(400, rendered.height)

        // Verify non-empty rendering of group composite
        val pixels = IntArray(400 * 400)
        rendered.getPixels(pixels, 0, 400, 0, 0, 400, 400)
        val nonZeroPixels = pixels.count { (it ushr 24) > 0 }
        assertTrue("Group rendering must produce visible pixels for all child types", nonZeroPixels > 0)
    }

    @Test
    fun groupTransformMatrixCascadesToChildren() {
        val child = Layer(
            id = "child1",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 100f, height = 100f, fillColor = 0xFFFF0000L, hasFill = true)
        )
        val group = Layer(
            id = "parent_group",
            type = LayerType.GROUP,
            children = listOf(child),
            transform = Transform2D(translationX = 50f, translationY = 50f, scaleX = 2f, scaleY = 2f)
        )
        val doc = ProjectDocument(
            id = "test_doc_scale",
            name = "Scale Test",
            canvasSize = CanvasSize(width = 400, height = 400),
            layers = listOf(group)
        )

        val rendered = LayerCompositor.renderLayerContent(group, 400, 400, doc, forceVisible = true)
        assertNotNull(rendered)
        val pixels = IntArray(400 * 400)
        rendered.getPixels(pixels, 0, 400, 0, 0, 400, 400)
        val nonZeroPixels = pixels.count { (it ushr 24) > 0 }
        assertTrue("Group transform must correctly composite child geometry", nonZeroPixels > 100)
    }
}
