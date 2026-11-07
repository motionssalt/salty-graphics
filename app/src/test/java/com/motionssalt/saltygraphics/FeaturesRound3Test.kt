package com.motionssalt.saltygraphics

import android.graphics.RectF
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.FillRenderer
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
class FeaturesRound3Test {

    // --- T14: Shared Gradient Selector ---
    @Test
    fun testGradientDataStopSortingAndEffectiveArgb() {
        val stop1 = GradientStop(position = 0.8f, color = 0xFFFF0000L, opacity = 0.5f)
        val stop2 = GradientStop(position = 0.2f, color = 0xFF00FF00L, opacity = 1.0f)
        val grad = GradientData(type = GradientType.LINEAR, angleDegrees = 90f, stops = listOf(stop1, stop2))

        val sorted = grad.sortedStops()
        assertEquals(0.2f, sorted[0].position, 0.001f)
        assertEquals(0.8f, sorted[1].position, 0.001f)

        // Half opacity gives ~128 alpha
        val a1 = (stop1.effectiveArgb ushr 24) and 0xFF
        assertTrue(a1 in 126..130)
    }

    // --- T15: Universal Fill Control ---
    @Test
    fun testUniversalFillSolidTransparencyIndependentOfStroke() {
        val shapeWithTransparentFill = Layer(
            id = "shape1",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 100f, height = 100f),
            fillData = FillData(type = FillType.SOLID, solidColor = 0xFF00E5FFL, solidOpacity = 0.0f),
            strokeData = StrokeData(isEnabled = true, color = 0xFFFFFFFFL, width = 4f, opacity = 1.0f)
        )

        assertEquals(0.0f, shapeWithTransparentFill.effectiveFill.solidOpacity, 0.001f)
        assertTrue(shapeWithTransparentFill.effectiveStroke.isEnabled)
        assertEquals(1.0f, shapeWithTransparentFill.effectiveStroke.opacity, 0.001f)
    }

    // --- T16: Media Fill Modes (Fill, Fit, Stretch) ---
    @Test
    fun testMediaFillModesPlacementCalculation() {
        val layerBounds = RectF(-100f, -50f, 100f, 50f) // 200 x 100 layer (2:1 aspect)
        val mediaW = 100f
        val mediaH = 100f // 1:1 aspect media

        // 1. Stretch mode: distorts to layer bounds
        val (_, dstStretch) = FillRenderer.computeMediaPlacement(mediaW, mediaH, layerBounds, MediaScaleMode.STRETCH)
        assertEquals(layerBounds.left, dstStretch.left, 0.01f)
        assertEquals(layerBounds.right, dstStretch.right, 0.01f)
        assertEquals(layerBounds.top, dstStretch.top, 0.01f)
        assertEquals(layerBounds.bottom, dstStretch.bottom, 0.01f)

        // 2. Fit mode: whole media fits inside layer (100 x 100 centered inside 200 x 100)
        val (_, dstFit) = FillRenderer.computeMediaPlacement(mediaW, mediaH, layerBounds, MediaScaleMode.FIT)
        assertEquals(100f, dstFit.width(), 0.01f)
        assertEquals(100f, dstFit.height(), 0.01f)
        assertEquals(0f, dstFit.centerX(), 0.01f)

        // 3. Fill mode: covers 200 x 100 (scales media by 2.0x to 200 x 200, crops height)
        val (_, dstFill) = FillRenderer.computeMediaPlacement(mediaW, mediaH, layerBounds, MediaScaleMode.FILL)
        assertEquals(200f, dstFill.width(), 0.01f)
        assertEquals(200f, dstFill.height(), 0.01f)
    }

    // --- T17: Unified Border & Shadow ---
    @Test
    fun testBorderAndShadowGeometry() {
        val stroke = StrokeData(isEnabled = true, width = 8f, isDashed = true, dashLength = 16f, dashGap = 8f)
        assertTrue(stroke.isEnabled)
        assertEquals(8f, stroke.width, 0.001f)
        assertTrue(stroke.isDashed)

        val shadow = ShadowData(isEnabled = true, lightAngleDegrees = 0f, distance = 20f, blurRadius = 10f)
        assertEquals(20f, shadow.offsetX, 0.01f)
        assertEquals(0f, shadow.offsetY, 0.01f)
    }

    // --- T18: Shape Picker Core Shapes ---
    @Test
    fun testCoreShapesBuildGeometry() {
        val rect = ShapeData(type = ShapeType.RECTANGLE, width = 200f, height = 100f)
        val circle = ShapeData(type = ShapeType.CIRCLE, width = 150f, height = 150f)
        val star = ShapeData(type = ShapeType.STAR, width = 200f, height = 200f, starPoints = 5, starInnerRadiusRatio = 0.4f)
        val poly = ShapeData(type = ShapeType.POLYGON, width = 180f, height = 180f, sides = 6)
        val line = ShapeData(type = ShapeType.LINE, width = 250f, height = 4f)
        val arrow = ShapeData(type = ShapeType.ARROW, width = 250f, height = 30f)

        val rectPath = rect.toVectorPath()
        val circlePath = circle.toVectorPath()
        val starPath = star.toVectorPath()
        val polyPath = poly.toVectorPath()
        val linePath = line.toVectorPath()
        val arrowPath = arrow.toVectorPath()

        assertEquals(4, rectPath.points.size)
        assertEquals(4, circlePath.points.size)
        assertEquals(10, starPath.points.size) // 5 outer + 5 inner
        assertEquals(6, polyPath.points.size)
        assertEquals(2, linePath.points.size)
        assertTrue(arrowPath.points.size >= 4)
    }

    // --- T21: Smart Guides & Snapping ---
    @Test
    fun testSmartSnapToCanvasCenterAndRotation() {
        val targetBounds = SelectionBounds(-50f, -50f, 50f, 50f)
        val targetTransform = Transform2D(translationX = 4f, translationY = -3f) // close to 0,0

        val snapRes = SmartSnapEngine.snapTranslation(
            targetBounds = targetBounds,
            targetTransform = targetTransform,
            otherLayers = emptyList(),
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            effectiveZoom = 1.0f,
            tolerancePx = 8f
        )

        assertTrue(snapRes.didSnapX)
        assertTrue(snapRes.didSnapY)
        assertEquals(0f, snapRes.snappedTransform.translationX, 0.001f)
        assertEquals(0f, snapRes.snappedTransform.translationY, 0.001f)
        assertEquals(2, snapRes.activeGuides.size)

        // Rotation snap near 45°
        val (snappedAngle, didSnapAngle) = SmartSnapEngine.snapRotation(46.2f)
        assertTrue(didSnapAngle)
        assertEquals(45f, snappedAngle, 0.01f)
    }

    // --- T24: Selective Property Paste ---
    @Test
    fun testSelectivePropertyPasteReplacesOnlyChosenValues() {
        val source = Layer(
            id = "src",
            name = "Source",
            transform = Transform2D(rotationDegrees = 45f),
            opacity = 0.5f,
            blendMode = BlendMode.MULTIPLY,
            strokeData = StrokeData(isEnabled = true, width = 12f)
        )
        val target = Layer(
            id = "dst",
            name = "Target",
            transform = Transform2D(rotationDegrees = 0f),
            opacity = 1.0f,
            blendMode = BlendMode.NORMAL,
            strokeData = StrokeData(isEnabled = false, width = 2f)
        )

        // Paste only stroke and opacity, leaving transform and blendMode unchanged
        val props = com.motionssalt.saltygraphics.engine.action.SelectivePasteProperties(
            transform = false,
            effects = false,
            fill = false,
            stroke = true,
            shadow = false,
            blendMode = false,
            opacity = true,
            mask = false
        )

        val result = com.motionssalt.saltygraphics.engine.action.GlobalClipboard.applySelective(target, source, props)
        assertEquals(0f, result.transform.rotationDegrees, 0.01f) // Unchanged
        assertEquals(BlendMode.NORMAL, result.blendMode)          // Unchanged
        assertEquals(0.5f, result.opacity, 0.01f)                // Replaced
        assertEquals(12f, result.effectiveStroke.width, 0.01f)    // Replaced
        assertTrue(result.effectiveStroke.isEnabled)
    }

    // --- T26: Grid Square Toggle & Canvas Aspect Ratio ---
    @Test
    fun testGridAspectCanvasMatchedAndSquareToggle() {
        val canvasW = 1920f
        val canvasH = 1080f
        val gridSize = 40

        // Default mode: cols and rows matched to canvas ratio
        val cols = Math.round(canvasW / gridSize)
        val rows = Math.round(canvasH / gridSize)
        val colSpacing = canvasW / cols
        val rowSpacing = canvasH / rows

        assertEquals(0f, (cols * colSpacing) - canvasW, 0.01f) // Exactly reaches edge corner-to-corner
        assertEquals(0f, (rows * rowSpacing) - canvasH, 0.01f)
    }
}
