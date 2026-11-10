package com.motionssalt.saltygraphics

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.render.LayerThumbnailRenderer
import com.motionssalt.saltygraphics.ui.editor.LayersPanel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayerThumbnailsAndSelectToggleTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setup() {
        LayerThumbnailRenderer.clearCache()
    }

    @Test
    fun testLayerThumbnailsGenerateForSupportedTypes() {
        // 1. Shape layer
        val shapeLayer = Layer(
            id = "shape1",
            name = "Test Rectangle",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 150f, height = 150f, fillColor = 0xFFFF5500L)
        )
        val shapeThumb = LayerThumbnailRenderer.renderThumbnail(shapeLayer, 72)
        assertNotNull("Shape thumbnail must generate", shapeThumb)
        assertEquals(72, shapeThumb!!.width)
        assertEquals(72, shapeThumb.height)

        // 2. Text layer
        val textLayer = Layer(
            id = "text1",
            name = "Test Text",
            type = LayerType.TEXT,
            textData = TextLayerData(text = "Salty", fontSize = 48f, color = 0xFF00E5FFL)
        )
        val textThumb = LayerThumbnailRenderer.renderThumbnail(textLayer, 72)
        assertNotNull("Text thumbnail must generate", textThumb)

        // 3. Vector layer
        val vectorLayer = Layer(
            id = "vec1",
            name = "Test Vector",
            type = LayerType.VECTOR,
            vectorPathData = VectorPathData(
                points = listOf(
                    BezierPoint(0f, 0f),
                    BezierPoint(50f, 0f),
                    BezierPoint(50f, 50f)
                ),
                isClosed = true,
                hasFill = true,
                fillColor = 0xFF33CC33L
            )
        )
        val vecThumb = LayerThumbnailRenderer.renderThumbnail(vectorLayer, 72)
        assertNotNull("Vector thumbnail must generate", vecThumb)

        // 4. Group layer with children
        val groupLayer = Layer(
            id = "group1",
            name = "Test Group",
            type = LayerType.GROUP,
            children = listOf(shapeLayer, textLayer)
        )
        val groupThumb = LayerThumbnailRenderer.renderThumbnail(groupLayer, 72)
        assertNotNull("Group thumbnail with children must generate", groupThumb)
    }

    @Test
    fun testLayerThumbnailsFallbackToNullForAdjustmentAndEmptyLayers() {
        // 1. Adjustment layer must return null to fall back to type icon
        val adjLayer = Layer(
            id = "adj1",
            name = "Adjustment 1",
            type = LayerType.ADJUSTMENT
        )
        assertNull(
            "Adjustment layer must return null for icon fallback",
            LayerThumbnailRenderer.renderThumbnail(adjLayer, 72)
        )

        // 2. Empty text layer must return null
        val emptyTextLayer = Layer(
            id = "text_empty",
            name = "Empty Text",
            type = LayerType.TEXT,
            textData = TextLayerData(text = "   ")
        )
        assertNull(
            "Empty text must return null for icon fallback",
            LayerThumbnailRenderer.renderThumbnail(emptyTextLayer, 72)
        )

        // 3. Empty vector layer must return null
        val emptyVecLayer = Layer(
            id = "vec_empty",
            name = "Empty Vector",
            type = LayerType.VECTOR,
            vectorPathData = VectorPathData(points = emptyList())
        )
        assertNull(
            "Empty vector must return null for icon fallback",
            LayerThumbnailRenderer.renderThumbnail(emptyVecLayer, 72)
        )

        // 4. Empty group layer must return null
        val emptyGroupLayer = Layer(
            id = "group_empty",
            name = "Empty Group",
            type = LayerType.GROUP,
            children = emptyList()
        )
        assertNull(
            "Empty group must return null for icon fallback",
            LayerThumbnailRenderer.renderThumbnail(emptyGroupLayer, 72)
        )
    }

    @Test
    fun testLayerThumbnailsCacheAndContentUpdate() {
        val initialLayer = Layer(
            id = "shape1",
            name = "Square",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 100f, height = 100f, fillColor = 0xFFFF0000L)
        )

        val firstThumb = LayerThumbnailRenderer.renderThumbnail(initialLayer, 72)
        assertNotNull(firstThumb)

        // Cached lookup
        val cached = LayerThumbnailRenderer.getCached(initialLayer)
        assertSame("Cached bitmap must be returned for unchanged layer", firstThumb, cached)

        // Update layer content (change color)
        val updatedLayer = initialLayer.copy(
            shapeData = initialLayer.shapeData!!.copy(fillColor = 0xFF00FF00L)
        )
        assertNotEquals(
            "Cache key must change when layer content changes",
            LayerThumbnailRenderer.computeCacheKey(initialLayer),
            LayerThumbnailRenderer.computeCacheKey(updatedLayer)
        )

        // Generate updated thumbnail
        val secondThumb = LayerThumbnailRenderer.renderThumbnail(updatedLayer, 72)
        assertNotNull(secondThumb)
        assertNotSame("Modified layer must generate a new thumbnail", firstThumb, secondThumb)
    }

    @Test
    fun testSelectAllAndSelectNoneToggleUI() {
        val layers = listOf(
            Layer(id = "l1", name = "Layer 1", type = LayerType.SHAPE, shapeData = ShapeData(width = 50f, height = 50f)),
            Layer(id = "l2", name = "Layer 2", type = LayerType.TEXT, textData = TextLayerData(text = "Two"))
        )

        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            var selectedIds by mutableStateOf<Set<String>>(emptySet())

            host.get().setContent {
                MaterialTheme {
                    LayersPanel(
                        layers = layers,
                        selectedLayerIds = selectedIds,
                        onSelectLayer = { selectedIds = setOf(it) },
                        onToggleSelectMulti = { id ->
                            selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                        },
                        onSelectAll = { selectedIds = layers.map { it.id }.toSet() },
                        onSelectNone = { selectedIds = emptySet() },
                        onToggleVisibility = {},
                        onToggleLock = {},
                        onDeleteLayers = {},
                        onRenameLayer = { _, _ -> },
                        onCopyLayer = {},
                        onPasteProperties = {},
                        onMoveLayer = { _, _ -> },
                        onEnterGroup = {},
                        onClosePanel = {}
                    )
                }
            }

            // Initially, nothing is selected -> toggle button should say "Select All"
            compose.onNodeWithContentDescription("Select All").assertExists()

            // Tap toggle button -> selects all
            compose.onNodeWithContentDescription("Select All").performClick()
            assertEquals(2, selectedIds.size)

            // Now all layers are selected -> toggle button should say "Select None"
            compose.onNodeWithContentDescription("Select None").assertExists()

            // Tap toggle button again -> clears selection
            compose.onNodeWithContentDescription("Select None").performClick()
            assertEquals(0, selectedIds.size)

            // Once again unselected -> toggle button should say "Select All"
            compose.onNodeWithContentDescription("Select All").assertExists()
        } finally {
            host.pause().stop().destroy()
        }
    }
}
