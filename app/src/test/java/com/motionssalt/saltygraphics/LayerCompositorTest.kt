package com.motionssalt.saltygraphics

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import com.motionssalt.saltygraphics.engine.model.*

import android.graphics.Bitmap
import android.graphics.Color
import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.BlendMath
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import org.junit.Assert.*
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
class LayerCompositorTest {

    private fun sampleBackdrop(): Bitmap {
        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        // Fill with blue #0000FF
        val pixels = IntArray(100 * 100) { 0xFF0000FF.toInt() }
        bmp.setPixels(pixels, 0, 100, 0, 0, 100, 100)
        return bmp
    }

    // --- T9: Blend modes on all layer types reach renderer and change output ---

    @Test
    fun testAll23BlendModesProduceDistinctOutputAgainstBackdrop() {
        val baseColor = 0xFF668099L // Midtone slate
        val blendColor = 0xFFB34DCCL // Midtone purple

        var differentCount = 0
        for (mode in BlendMode.entries) {
            val blended = BlendMath.blendColor(mode, baseColor, blendColor, opacity = 1f)
            if (mode == BlendMode.NORMAL) {
                val r = (blended shr 16) and 0xFF
                val g = (blended shr 8) and 0xFF
                val b = blended and 0xFF
                assertEquals("Normal mode red component", 0xB3L, r)
                assertEquals("Normal mode green component", 0x4DL, g)
                assertEquals("Normal mode blue component", 0xCCL, b)
            } else {
                // In non-normal modes, blending with backdrop must alter output
                if (blended != BlendMath.blendColor(BlendMode.NORMAL, baseColor, blendColor, opacity = 1f)) {
                    differentCount++
                }
            }
        }
        // At least 20 modes produce different results than NORMAL for Blue + Yellow
        assertTrue("Non-normal blend modes must change output", differentCount >= 20)
    }

    @Test
    fun testBlendModesReachRendererOnShapeLayer() {
        val bg = Layer(
            id = "bg",
            name = "Background",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 100f, height = 100f, fillColor = 0xFF0000FFL, hasFill = true, hasStroke = false)
        )

        // Normal mode
        val shapeNormal = Layer(
            id = "fg-normal",
            name = "Shape Normal",
            type = LayerType.SHAPE,
            blendMode = BlendMode.NORMAL,
            shapeData = ShapeData(width = 60f, height = 60f, fillColor = 0xFFFFFF00L, hasFill = true, hasStroke = false)
        )
        val docNormal = ProjectDocument(id = "doc1", canvasSize = CanvasSize(100, 100), layers = listOf(bg, shapeNormal))
        val bmpNormal = LayerCompositor.compositeDocument(docNormal, 100, 100)
        val centerPixelNormal = bmpNormal.getPixel(50, 50)

        // Multiply mode
        val shapeMultiply = shapeNormal.copy(id = "fg-multiply", blendMode = BlendMode.MULTIPLY)
        val docMultiply = ProjectDocument(id = "doc2", canvasSize = CanvasSize(100, 100), layers = listOf(bg, shapeMultiply))
        val bmpMultiply = LayerCompositor.compositeDocument(docMultiply, 100, 100)
        val centerPixelMultiply = bmpMultiply.getPixel(50, 50)

        // Screen mode
        val shapeScreen = shapeNormal.copy(id = "fg-screen", blendMode = BlendMode.SCREEN)
        val docScreen = ProjectDocument(id = "doc3", canvasSize = CanvasSize(100, 100), layers = listOf(bg, shapeScreen))
        val bmpScreen = LayerCompositor.compositeDocument(docScreen, 100, 100)
        val centerPixelScreen = bmpScreen.getPixel(50, 50)

        // Difference mode
        val shapeDiff = shapeNormal.copy(id = "fg-diff", blendMode = BlendMode.DIFFERENCE)
        val docDiff = ProjectDocument(id = "doc4", canvasSize = CanvasSize(100, 100), layers = listOf(bg, shapeDiff))
        val bmpDiff = LayerCompositor.compositeDocument(docDiff, 100, 100)
        val centerPixelDiff = bmpDiff.getPixel(50, 50)

        assertNotEquals("Multiply must differ from Normal on shape layer", centerPixelNormal, centerPixelMultiply)
        assertNotEquals("Screen must differ from Normal on shape layer", centerPixelNormal, centerPixelScreen)
        assertNotEquals("Difference must differ from Normal on shape layer", centerPixelNormal, centerPixelDiff)
    }

    @Test
    fun testBlendModesReachRendererOnTextLayer() {
        val bg = Layer(
            id = "bg",
            name = "Background",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 100f, height = 100f, fillColor = 0xFF0000FFL, hasFill = true)
        )

        val textNormal = Layer(
            id = "text-norm",
            name = "Text",
            type = LayerType.TEXT,
            blendMode = BlendMode.NORMAL,
            textData = TextLayerData(text = "A", fontSize = 50f, color = 0xFFFFFF00L)
        )
        val textDiff = textNormal.copy(id = "text-diff", blendMode = BlendMode.DIFFERENCE)

        val docNormal = ProjectDocument(id = "d1", canvasSize = CanvasSize(100, 100), layers = listOf(bg, textNormal))
        val docDiff = ProjectDocument(id = "d2", canvasSize = CanvasSize(100, 100), layers = listOf(bg, textDiff))

        val bmpNormal = LayerCompositor.compositeDocument(docNormal, 100, 100)
        val bmpDiff = LayerCompositor.compositeDocument(docDiff, 100, 100)

        // Find a pixel where text was drawn
        var foundDifferent = false
        for (y in 0 until 100) {
            for (x in 0 until 100) {
                if (bmpNormal.getPixel(x, y) != bmpDiff.getPixel(x, y)) {
                    foundDifferent = true
                    break
                }
            }
            if (foundDifferent) break
        }
        assertTrue("Text layer blend mode must reach renderer and alter pixels", foundDifferent)
    }

    @Test
    fun testBlendModesReachRendererOnVectorLayer() {
        val bg = Layer(
            id = "bg",
            name = "Background",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 100f, height = 100f, fillColor = 0xFF0000FFL, hasFill = true)
        )
        val vecPath = VectorPathData(
            points = listOf(BezierPoint(-30f, -30f), BezierPoint(30f, -30f), BezierPoint(0f, 30f)),
            isClosed = true,
            hasFill = true,
            fillColor = 0xFFFFFF00L
        )
        val vecNormal = Layer(id = "v-norm", name = "Vec", type = LayerType.VECTOR, blendMode = BlendMode.NORMAL, vectorPathData = vecPath)
        val vecDiff = vecNormal.copy(id = "v-diff", blendMode = BlendMode.DIFFERENCE)

        val bmpNormal = LayerCompositor.compositeDocument(ProjectDocument(id = "d1", canvasSize = CanvasSize(100, 100), layers = listOf(bg, vecNormal)), 100, 100)
        val bmpDiff = LayerCompositor.compositeDocument(ProjectDocument(id = "d2", canvasSize = CanvasSize(100, 100), layers = listOf(bg, vecDiff)), 100, 100)

        var foundDifferent = false
        for (y in 0 until 100) {
            for (x in 0 until 100) {
                if (bmpNormal.getPixel(x, y) != bmpDiff.getPixel(x, y)) {
                    foundDifferent = true
                    break
                }
            }
            if (foundDifferent) break
        }
        assertTrue("Vector layer blend mode must reach renderer and alter pixels", foundDifferent)
    }

    @Test
    fun testBlendModesReachRendererInsideGroups() {
        val bg = Layer(
            id = "bg",
            name = "Background",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 100f, height = 100f, fillColor = 0xFF0000FFL, hasFill = true)
        )
        val child = Layer(
            id = "child",
            name = "Child",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 60f, height = 60f, fillColor = 0xFFFFFF00L, hasFill = true)
        )
        val groupNormal = Layer(id = "grp-norm", name = "Group", type = LayerType.GROUP, blendMode = BlendMode.NORMAL, children = listOf(child))
        val groupScreen = groupNormal.copy(id = "grp-screen", blendMode = BlendMode.SCREEN)

        val bmpNormal = LayerCompositor.compositeDocument(ProjectDocument(id = "d1", canvasSize = CanvasSize(100, 100), layers = listOf(bg, groupNormal)), 100, 100)
        val bmpScreen = LayerCompositor.compositeDocument(ProjectDocument(id = "d2", canvasSize = CanvasSize(100, 100), layers = listOf(bg, groupScreen)), 100, 100)

        assertNotEquals("Group blend mode must alter composited result", bmpNormal.getPixel(50, 50), bmpScreen.getPixel(50, 50))
    }

    // --- T10: Correct non-destructive layer masks ---

    @Test
    fun testAlphaMaskControlsVisibilityWhilePreservingBlendMode() {
        val bg = Layer(
            id = "bg",
            name = "BG",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 100f, height = 100f, fillColor = 0xFF0000FFL, hasFill = true)
        )
        // Mask source: half-sized circle in center
        val maskSource = Layer(
            id = "mask-source",
            name = "Mask Source",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 40f, height = 40f, fillColor = 0xFFFFFFFFL, hasFill = true)
        )
        // Target: 100x100 yellow square with Difference blend mode, masked by maskSource
        val target = Layer(
            id = "target",
            name = "Target",
            type = LayerType.SHAPE,
            blendMode = BlendMode.DIFFERENCE,
            maskType = MaskType.ALPHA,
            maskSourceLayerId = "mask-source",
            shapeData = ShapeData(width = 100f, height = 100f, fillColor = 0xFFFFFF00L, hasFill = true)
        )

        val doc = ProjectDocument(id = "d-mask", canvasSize = CanvasSize(100, 100), layers = listOf(bg, target))
        // Add mask-source to doc layers lookup so compositor finds it
        val docWithSource = doc.copy(layers = listOf(bg, maskSource.copy(isVisible = false), target))

        val composited = LayerCompositor.compositeDocument(docWithSource, 100, 100)

        // Outside the mask (e.g. at 10, 10): target is masked out, so backdrop (blue) remains!
        val cornerPixel = composited.getPixel(10, 10)
        assertEquals("Outside mask area should show background blue", 0xFF0000FF.toInt(), cornerPixel)

        // Inside the mask (center 50, 50): target is visible AND Difference blend mode is applied!
        val centerPixel = composited.getPixel(50, 50)
        assertNotEquals("Inside mask should blend target over background", 0xFF0000FF.toInt(), centerPixel)
    }

    @Test
    fun testInvertedAlphaMaskReversesVisibility() {
        val bg = Layer(
            id = "bg",
            name = "BG",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 100f, height = 100f, fillColor = 0xFF0000FFL, hasFill = true)
        )
        val maskSource = Layer(
            id = "mask-src",
            name = "Mask Source",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 40f, height = 40f, fillColor = 0xFFFFFFFFL, hasFill = true)
        )
        val target = Layer(
            id = "target-inv",
            name = "Target Inverted",
            type = LayerType.SHAPE,
            maskType = MaskType.ALPHA,
            maskSourceLayerId = "mask-src",
            isMaskInverted = true, // Inverted!
            shapeData = ShapeData(width = 100f, height = 100f, fillColor = 0xFFFFFF00L, hasFill = true)
        )

        val doc = ProjectDocument(id = "d-inv", canvasSize = CanvasSize(100, 100), layers = listOf(bg, maskSource.copy(isVisible = false), target))
        val composited = LayerCompositor.compositeDocument(doc, 100, 100)

        // Inside inverted mask (center 50, 50): target is masked OUT (hole), showing background blue!
        val centerPixel = composited.getPixel(50, 50)
        assertEquals("Center hole should show background blue when inverted", 0xFF0000FF.toInt(), centerPixel)

        // Outside inverted mask (10, 10): target is visible (yellow)!
        val cornerPixel = composited.getPixel(10, 10)
        assertEquals("Outside inverted mask area should show yellow target", 0xFFFFFF00.toInt(), cornerPixel)
    }

    @Test
    fun testLuminanceMaskPreservesPartialValuesForSoftEdges() {
        // Luminance mask: white = 100% visible, 50% gray = 50% visible, black = 0% visible
        val layerPixels = IntArray(3) { 0xFFFF0000.toInt() } // Red target
        val maskPixels = intArrayOf(
            0xFFFFFFFF.toInt(), // 100% white
            0xFF808080.toInt(), // ~50% gray
            0xFF000000.toInt()  // 0% black
        )

        LayerCompositor.applyMask(layerPixels, maskPixels, MaskType.LUMINANCE, isInverted = false, 3, 1)

        val alpha0 = (layerPixels[0] ushr 24) and 0xFF
        val alpha1 = (layerPixels[1] ushr 24) and 0xFF
        val alpha2 = (layerPixels[2] ushr 24) and 0xFF

        assertEquals("White luminance mask gives ~255 alpha", 255, alpha0)
        assertTrue("Gray luminance mask gives partial alpha (~128)", alpha1 in 120..136)
        assertEquals("Black luminance mask gives 0 alpha", 0, alpha2)
    }

    @Test
    fun testLegacyMaskMigrationRoundTrip() {
        // Legacy layer with maskMode = IN_MASK
        val legacyLayerIn = Layer(
            id = "leg-in",
            name = "Legacy In",
            maskMode = MaskMode.IN_MASK,
            affectsLayerIds = listOf("source-1")
        )
        assertEquals(MaskType.ALPHA, legacyLayerIn.effectiveMaskType)
        assertFalse(legacyLayerIn.effectiveMaskInverted)
        assertEquals("source-1", legacyLayerIn.effectiveMaskSourceLayerId)

        // Legacy layer with maskMode = OUT_MASK
        val legacyLayerOut = Layer(
            id = "leg-out",
            name = "Legacy Out",
            maskMode = MaskMode.OUT_MASK,
            affectsLayerIds = listOf("source-2")
        )
        assertEquals(MaskType.ALPHA, legacyLayerOut.effectiveMaskType)
        assertTrue(legacyLayerOut.effectiveMaskInverted)
        assertEquals("source-2", legacyLayerOut.effectiveMaskSourceLayerId)

        // Document migration in SgProjectArchive
        val doc = ProjectDocument(id = "d-leg", layers = listOf(legacyLayerIn, legacyLayerOut))
        val migrated = SgProjectArchive.migrateLegacyMasks(doc)
        assertEquals(MaskType.ALPHA, migrated.layers[0].maskType)
        assertFalse(migrated.layers[0].isMaskInverted)
        assertEquals("source-1", migrated.layers[0].maskSourceLayerId)

        assertEquals(MaskType.ALPHA, migrated.layers[1].maskType)
        assertTrue(migrated.layers[1].isMaskInverted)
        assertEquals("source-2", migrated.layers[1].maskSourceLayerId)
    }

    // --- T12: Adjustment layer behavior ---

    @Test
    fun testAdjustmentLayerDrawsNothingByItselfWithZeroEffects() {
        val bg = Layer(
            id = "bg",
            name = "Background",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 100f, height = 100f, fillColor = 0xFF0000FFL, hasFill = true)
        )
        val adjZero = Layer(
            id = "adj-zero",
            name = "Clean Adjustment",
            type = LayerType.ADJUSTMENT,
            blendMode = BlendMode.NORMAL,
            opacity = 1f,
            effects = emptyList()
        )

        val docWithout = ProjectDocument(id = "d1", canvasSize = CanvasSize(100, 100), layers = listOf(bg))
        val docWith = ProjectDocument(id = "d2", canvasSize = CanvasSize(100, 100), layers = listOf(bg, adjZero))

        val bmpWithout = LayerCompositor.compositeDocument(docWithout, 100, 100)
        val bmpWith = LayerCompositor.compositeDocument(docWith, 100, 100)

        // Zero-effect adjustment layer must produce identical pixels to document without it
        for (y in 0 until 100) {
            for (x in 0 until 100) {
                assertEquals("Pixel ($x,$y) must be unaffected by clean adjustment layer",
                    bmpWithout.getPixel(x, y), bmpWith.getPixel(x, y))
            }
        }
    }

    @Test
    fun testAdjustmentLayerChangesOnlyUnderlyingLayers() {
        val bg = Layer(
            id = "bg",
            name = "Background",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 100f, height = 100f, fillColor = 0xFF0000FFL, hasFill = true)
        )
        // Adjustment layer with Screen mode
        val adj = Layer(
            id = "adj",
            name = "Adjustment Screen",
            type = LayerType.ADJUSTMENT,
            blendMode = BlendMode.SCREEN,
            opacity = 0.5f
        )
        // Layer ABOVE adjustment layer
        val topLayer = Layer(
            id = "top",
            name = "Top Layer",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 30f, height = 30f, fillColor = 0xFFFF0000L, hasFill = true)
        )

        val doc = ProjectDocument(id = "d-adj", canvasSize = CanvasSize(100, 100), layers = listOf(bg, adj, topLayer))
        val bmp = LayerCompositor.compositeDocument(doc, 100, 100)

        // Top layer (red square at center) is drawn AFTER the adjustment layer, so it remains full red!
        val topPixel = bmp.getPixel(50, 50)
        assertEquals("Top layer above adjustment layer should remain unaffected red", 0xFFFF0000.toInt(), topPixel)
    }
}
