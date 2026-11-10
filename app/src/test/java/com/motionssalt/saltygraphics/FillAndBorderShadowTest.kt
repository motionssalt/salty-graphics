package com.motionssalt.saltygraphics

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.FillRenderer
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import com.motionssalt.saltygraphics.ui.editor.EditorScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Acceptance test suite for V7 (Universal Fill) and V8 (Border & Shadow).
 * Covers CPU reference rendering (stroke alignment, dashes, caps, shadow, transparent fills)
 * and real UI interaction across all supported layer types (Text, Shape, Vector, Media).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FillAndBorderShadowTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    // =========================================================================
    // 1. CPU Reference Tests: Stroke Alignment, Dashes, Caps, Shadows & Fills
    // =========================================================================

    @Test
    fun testStrokeAlignmentRendersDistinctOutputs() {
        val size = 200
        val center = size / 2f
        val radius = 50f
        val strokeWidth = 20f

        fun renderAlignedCircle(alignment: StrokeAlignment): Bitmap {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            val path = Path().apply {
                addCircle(center, center, radius, Path.Direction.CW)
            }
            val stroke = StrokeData(
                isEnabled = true,
                color = 0xFFFF0000L,
                width = strokeWidth,
                alignment = alignment,
                opacity = 1f
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            FillRenderer.configureStrokePaint(paint, stroke)
            FillRenderer.drawPathWithStroke(canvas, path, stroke, paint)
            return bmp
        }

        val bmpCenter = renderAlignedCircle(StrokeAlignment.CENTER)
        val bmpInside = renderAlignedCircle(StrokeAlignment.INSIDE)
        val bmpOutside = renderAlignedCircle(StrokeAlignment.OUTSIDE)

        // Inside stroke: point just inside the circle (radius - 5) should be colored; point outside (radius + 5) must be 0
        val insideSampleIn = bmpInside.getPixel((center + radius - 5).toInt(), center.toInt())
        val insideSampleOut = bmpInside.getPixel((center + radius + 5).toInt(), center.toInt())
        assertTrue("Inside stroke must paint inside the boundary", Color.alpha(insideSampleIn) > 200)
        assertEquals("Inside stroke must NOT paint outside the boundary", 0, Color.alpha(insideSampleOut))

        // Outside stroke: point just outside the circle (radius + 5) should be colored; point inside (radius - 5) must be 0
        val outsideSampleIn = bmpOutside.getPixel((center + radius - 5).toInt(), center.toInt())
        val outsideSampleOut = bmpOutside.getPixel((center + radius + 5).toInt(), center.toInt())
        assertEquals("Outside stroke must NOT paint inside the boundary", 0, Color.alpha(outsideSampleIn))
        assertTrue("Outside stroke must paint outside the boundary", Color.alpha(outsideSampleOut) > 200)

        // Center stroke straddles: both inside and outside should have color
        val centerSampleIn = bmpCenter.getPixel((center + radius - 5).toInt(), center.toInt())
        val centerSampleOut = bmpCenter.getPixel((center + radius + 5).toInt(), center.toInt())
        assertTrue("Center stroke must paint inside", Color.alpha(centerSampleIn) > 200)
        assertTrue("Center stroke must paint outside", Color.alpha(centerSampleOut) > 200)
    }

    @Test
    fun testDashedStrokeProducesAlternatingGaps() {
        val size = 200
        fun renderLine(isDashed: Boolean): Bitmap {
            val bmp = Bitmap.createBitmap(size, 40, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            val path = Path().apply {
                moveTo(10f, 20f)
                lineTo(190f, 20f)
            }
            val stroke = StrokeData(
                isEnabled = true,
                color = 0xFF00FF00L,
                width = 6f,
                isDashed = isDashed,
                dashLength = 12f,
                dashGap = 12f,
                opacity = 1f
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            FillRenderer.configureStrokePaint(paint, stroke)
            FillRenderer.drawPathWithStroke(canvas, path, stroke, paint)
            return bmp
        }

        val solidBmp = renderLine(isDashed = false)
        val dashedBmp = renderLine(isDashed = true)

        var solidCount = 0
        var dashedCount = 0
        for (x in 20 until 180) {
            if (Color.alpha(solidBmp.getPixel(x, 20)) > 100) solidCount++
            if (Color.alpha(dashedBmp.getPixel(x, 20)) > 100) dashedCount++
        }

        assertEquals("Solid line should be unbroken", 160, solidCount)
        assertTrue("Dashed line must have gaps (significantly fewer pixels)", dashedCount in 60..100)
    }

    @Test
    fun testStrokeCapsAndJoinsConfiguredCorrectly() {
        val paint = Paint()
        val roundCapStroke = StrokeData(isEnabled = true, cap = VectorCap.ROUND, join = VectorJoin.BEVEL)
        FillRenderer.configureStrokePaint(paint, roundCapStroke)
        assertEquals(Paint.Cap.ROUND, paint.strokeCap)
        assertEquals(Paint.Join.BEVEL, paint.strokeJoin)

        val squareCapStroke = StrokeData(isEnabled = true, cap = VectorCap.SQUARE, join = VectorJoin.MITER)
        FillRenderer.configureStrokePaint(paint, squareCapStroke)
        assertEquals(Paint.Cap.SQUARE, paint.strokeCap)
        assertEquals(Paint.Join.MITER, paint.strokeJoin)

        val buttCapStroke = StrokeData(isEnabled = true, cap = VectorCap.BUTT, join = VectorJoin.ROUND)
        FillRenderer.configureStrokePaint(paint, buttCapStroke)
        assertEquals(Paint.Cap.BUTT, paint.strokeCap)
        assertEquals(Paint.Join.ROUND, paint.strokeJoin)
    }

    @Test
    fun testDropShadowOffsetAndOpacity() {
        val size = 200
        val center = 100f
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)

        val shape = Layer(
            id = "s1",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 40f, height = 40f),
            fillData = FillData(solidColor = 0xFFFFFFFFL, solidOpacity = 1f),
            shadowData = ShadowData(
                isEnabled = true,
                color = 0xFF000000L,
                opacity = 0.8f,
                distance = 30f,
                lightAngleDegrees = 0f, // Drops right along +X
                blurRadius = 8f
            )
        )

        val rendered = LayerCompositor.renderLayerContent(shape, size, size)

        // Point to the right (+30px offset from center) should have shadow alpha
        val rightPixel = rendered.getPixel((center + 30).toInt(), center.toInt())
        assertTrue("Shadow must be present at offset +X", Color.alpha(rightPixel) > 0)

        // Point to the far left (-40px) should have zero shadow
        val leftPixel = rendered.getPixel((center - 40).toInt(), center.toInt())
        assertEquals("Shadow must not be present on opposite side", 0, Color.alpha(leftPixel))
    }

    @Test
    fun testTransparentFillWithVisibleStrokeOnShape() {
        val size = 200
        val center = 100
        val shape = Layer(
            id = "s1",
            type = LayerType.SHAPE,
            shapeData = ShapeData(type = ShapeType.RECTANGLE, width = 80f, height = 80f),
            fillData = FillData(type = FillType.SOLID, solidColor = 0xFF00E5FFL, solidOpacity = 0.0f),
            strokeData = StrokeData(isEnabled = true, color = 0xFFFF0000L, width = 8f, opacity = 1.0f)
        )

        val rendered = LayerCompositor.renderLayerContent(shape, size, size)

        // Center must be transparent (0 alpha)
        val centerPixel = rendered.getPixel(center, center)
        assertEquals("Center of transparent-fill shape must be 0 alpha", 0, Color.alpha(centerPixel))

        // Boundary edge (stroke) must be opaque red
        val edgePixel = rendered.getPixel(center + 40, center)
        assertTrue("Stroke edge must be opaque", Color.alpha(edgePixel) > 200)
        assertEquals(255, Color.red(edgePixel))
    }

    @Test
    fun testTransparentFillWithVisibleStrokeOnVector() {
        val size = 200
        val center = 100
        val vectorPath = VectorPathData(
            points = listOf(
                VectorPoint(50f, 50f),
                VectorPoint(150f, 50f),
                VectorPoint(150f, 150f),
                VectorPoint(50f, 150f)
            ),
            isClosed = true
        )
        val vectorLayer = Layer(
            id = "v1",
            type = LayerType.VECTOR,
            vectorPathData = vectorPath,
            fillData = FillData(solidOpacity = 0f),
            strokeData = StrokeData(isEnabled = true, color = 0xFF00FF00L, width = 6f, opacity = 1f)
        )

        val rendered = LayerCompositor.renderLayerContent(vectorLayer, size, size)
        val centerPixel = rendered.getPixel(center, center)
        assertEquals("Vector center with transparent fill must be 0 alpha", 0, Color.alpha(centerPixel))

        val strokePixel = rendered.getPixel(150, center)
        assertTrue("Vector stroke must be drawn", Color.alpha(strokePixel) > 150)
    }

    // =========================================================================
    // 2. Real UI Tests: Fill and Border & Shadow on Text, Shape, Vector, Media
    // =========================================================================

    private fun withEditorFixture(
        layers: List<Layer>,
        check: (docId: String, repo: ProjectRepository) -> Unit
    ) {
        val repo = ProjectRepository(context)
        val doc = runBlocking {
            val project = repo.createProject("Fill & Border Fixture", CanvasSize(1000, 1000))
            project.copy(layers = layers).also { repo.saveProject(it) }
        }
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            host.get().setContent {
                MaterialTheme {
                    EditorScreen(
                        projectId = doc.id,
                        repository = repo,
                        onNavigateBack = {},
                        onNavigateToExport = {},
                        onNavigateToSettings = {}
                    )
                }
            }
            compose.waitUntil(5000) {
                compose.onAllNodesWithTag("editor_canvas").fetchSemanticsNodes().isNotEmpty()
            }
            check(doc.id, repo)
        } finally {
            host.pause().stop().destroy()
        }
    }

    @Test
    fun testFillAndBorderButtonsExistOnAllLayerTypes() {
        val shapeLayer = Layer(id = "layer_shape", name = "Test Shape", type = LayerType.SHAPE, shapeData = ShapeData())
        val textLayer = Layer(id = "layer_text", name = "Test Text", type = LayerType.TEXT, textData = TextLayerData())
        val vectorLayer = Layer(id = "layer_vector", name = "Test Vector", type = LayerType.VECTOR, vectorPathData = VectorPathData())
        val mediaLayer = Layer(id = "layer_media", name = "Test Media", type = LayerType.MEDIA, mediaWidth = 200f, mediaHeight = 200f)
        val adjLayer = Layer(id = "layer_adj", name = "Test Adj", type = LayerType.ADJUSTMENT)

        withEditorFixture(listOf(shapeLayer, textLayer, vectorLayer, mediaLayer, adjLayer)) { _, _ ->
            // 1. Check Shape Layer
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText("Test Shape").performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()

            compose.onNodeWithText("Fill", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithText("Border & Shadow", useUnmergedTree = true).assertIsDisplayed()

            // Open Fill dialog on Shape
            compose.onNodeWithText("Fill", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Fill Settings", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithContentDescription("Close Fill").performClick()

            // Open Border & Shadow dialog on Shape
            compose.onNodeWithText("Border & Shadow", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Border & Shadow", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithContentDescription("Close").performClick()

            // 2. Check Text Layer
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText("Test Text").performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()

            compose.onNodeWithText("Fill", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithText("Border & Shadow", useUnmergedTree = true).assertIsDisplayed()

            // 3. Check Vector Layer
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText("Test Vector").performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()

            compose.onNodeWithText("Fill", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithText("Border & Shadow", useUnmergedTree = true).assertIsDisplayed()

            // 4. Check Media Layer
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText("Test Media").performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()

            compose.onNodeWithText("Fill", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithText("Border & Shadow", useUnmergedTree = true).assertIsDisplayed()

            // 5. Check Adjustment Layer: Fill and Border & Shadow should NOT be shown
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText("Test Adj").performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()

            compose.onNodeWithText("Fill", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText("Border & Shadow", useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun testFillEditorModifiesDocumentAndSynchronizes() {
        val shapeLayer = Layer(
            id = "target_shape",
            name = "Target Shape",
            type = LayerType.SHAPE,
            shapeData = ShapeData(fillColor = 0xFF0000FFL)
        )

        withEditorFixture(listOf(shapeLayer)) { docId, repo ->
            // Select shape
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText("Target Shape").performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()

            // Open Fill dialog
            compose.onNodeWithText("Fill", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Fill Settings", useUnmergedTree = true).assertIsDisplayed()

            // Switch to Gradient tab
            compose.onNodeWithText("Gradient", useUnmergedTree = true).performClick()

            // Close dialog
            compose.onNodeWithContentDescription("Close Fill").performClick()

            // Verify document was updated with Gradient fill
            runBlocking {
                val saved = repo.loadProject(docId)
                val updatedLayer = saved.findLayerById("target_shape")
                assertNotNull(updatedLayer)
                assertEquals(FillType.GRADIENT, updatedLayer?.effectiveFill?.type)
            }
        }
    }

    @Test
    fun testBorderShadowEditorModifiesDocumentAndStrokeSettings() {
        val shapeLayer = Layer(
            id = "stroke_shape",
            name = "Stroke Shape",
            type = LayerType.SHAPE,
            shapeData = ShapeData()
        )

        withEditorFixture(listOf(shapeLayer)) { docId, repo ->
            // Select shape
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText("Stroke Shape").performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()

            // Open Border & Shadow dialog
            compose.onNodeWithText("Border & Shadow", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Border & Shadow", useUnmergedTree = true).assertIsDisplayed()

            // Toggle Stroke on
            compose.onNodeWithTag("stroke_switch").performClick()

            // Close dialog
            compose.onNodeWithContentDescription("Close").performClick()

            // Verify document was updated with stroke enabled
            runBlocking {
                val saved = repo.loadProject(docId)
                val updatedLayer = saved.findLayerById("stroke_shape")
                assertNotNull(updatedLayer)
                assertTrue("Stroke should now be enabled", updatedLayer!!.effectiveStroke.isEnabled)
            }
        }
    }
}
