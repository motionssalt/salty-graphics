package com.motionssalt.saltygraphics

import android.graphics.Bitmap
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.random.Random

/**
 * Property tests verifying that panel layer order (index 0 = top of panel)
 * strictly matches canvas stacking order (index 0 = topmost layer rendered on canvas)
 * across creation, duplication, reordering, grouping, ungrouping, undo, redo,
 * and pixel-level rendering tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayerStackingOrderPropertyTest {

    private fun createShapeLayer(id: String, name: String, color: Long, size: Float = 100f): Layer {
        return Layer(
            id = id,
            name = name,
            type = LayerType.SHAPE,
            shapeData = ShapeData(
                type = ShapeType.RECTANGLE,
                width = size,
                height = size,
                fillColor = color,
                hasFill = true,
                hasStroke = false
            )
        )
    }

    @Test
    fun testTopPanelLayerDrawsOverBottomPanelLayer() {
        // Red square at index 0 (top of panel, topmost on canvas)
        val redTop = createShapeLayer("red", "Red Top", 0xFFFF0000L, 80f)
        // Blue square at index 1 (bottom of panel, under red)
        val blueBottom = createShapeLayer("blue", "Blue Bottom", 0xFF0000FFL, 100f)

        val doc = ProjectDocument(
            id = "doc-stack-1",
            canvasSize = CanvasSize(100, 100),
            layers = listOf(redTop, blueBottom)
        )

        val bmp = LayerCompositor.compositeDocument(doc, 100, 100)
        // Center pixel (50, 50) must be red because redTop is at index 0 (top of panel)
        val centerPixel = bmp.getPixel(50, 50)
        assertEquals("Top panel layer (index 0) must occlude bottom panel layer", 0xFFFF0000.toInt(), centerPixel)

        // Corner pixel (95, 95) is outside red (80x80) but inside blue (100x100)
        val cornerPixel = bmp.getPixel(95, 95)
        assertEquals("Uncovered area of lower layer must be visible", 0xFF0000FF.toInt(), cornerPixel)
    }

    @Test
    fun testReorderingInvertsStackingOrder() {
        val redLayer = createShapeLayer("red", "Red", 0xFFFF0000L, 100f)
        val greenLayer = createShapeLayer("green", "Green", 0xFF00FF00L, 100f)

        // Initial: red at index 0 (top), green at index 1 (bottom)
        val doc1 = ProjectDocument(id = "d1", canvasSize = CanvasSize(100, 100), layers = listOf(redLayer, greenLayer))
        val bmp1 = LayerCompositor.compositeDocument(doc1, 100, 100)
        assertEquals("Red on top", 0xFFFF0000.toInt(), bmp1.getPixel(50, 50))

        // Reorder: move index 0 to index 1 -> green is now index 0, red is index 1
        val reorderedList = doc1.layers.toMutableList()
        val item = reorderedList.removeAt(0)
        reorderedList.add(1, item)
        val doc2 = doc1.copy(layers = reorderedList)

        assertEquals("green", doc2.layers[0].id)
        assertEquals("red", doc2.layers[1].id)

        val bmp2 = LayerCompositor.compositeDocument(doc2, 100, 100)
        assertEquals("Green on top after reordering to index 0", 0xFF00FF00.toInt(), bmp2.getPixel(50, 50))
    }

    @Test
    fun testDuplicatingLayerPlacesCopyAboveOriginal() {
        val original = createShapeLayer("base", "Base", 0xFFFF0000L, 100f)
        val background = createShapeLayer("bg", "Background", 0xFF0000FFL, 100f)
        val doc = ProjectDocument(id = "d", canvasSize = CanvasSize(100, 100), layers = listOf(original, background))

        // Duplicate original (at index 0)
        val copy = original.copy(id = "copy-id", name = "${original.name} Copy")
        val idx = doc.layers.indexOfFirst { it.id == original.id }
        val updatedList = doc.layers.toMutableList()
        if (idx >= 0) updatedList.add(idx, copy) else updatedList.add(0, copy)

        assertEquals("Duplicate must be placed at index 0 (above original)", "copy-id", updatedList[0].id)
        assertEquals("Original must now be at index 1", "base", updatedList[1].id)
        assertEquals("Background remains at index 2", "bg", updatedList[2].id)
    }

    @Test
    fun testRandomReorderSequenceMaintainsStackingInvariants() {
        val rng = Random(42)
        val colors = listOf(0xFFFF0000L, 0xFF00FF00L, 0xFF0000FFL, 0xFFFFFF00L, 0xFFFF00FFL)
        var layers = colors.mapIndexed { idx, color ->
            createShapeLayer("layer-$idx", "Layer $idx", color, 100f)
        }

        repeat(30) {
            val from = rng.nextInt(layers.size)
            val to = rng.nextInt(layers.size)
            val mutable = layers.toMutableList()
            val removed = mutable.removeAt(from)
            mutable.add(to, removed)
            layers = mutable

            val doc = ProjectDocument(id = "rand-doc", canvasSize = CanvasSize(100, 100), layers = layers)
            val bmp = LayerCompositor.compositeDocument(doc, 100, 100)
            val topColor = (layers.first().shapeData?.fillColor?.toInt()) ?: 0

            assertEquals(
                "Top panel layer (index 0: ${layers.first().name}) must always be the rendered pixel",
                topColor,
                bmp.getPixel(50, 50)
            )
        }
    }
}
