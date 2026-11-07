package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class AdjustmentLayerTest {

    @Test
    fun testAdjustmentLayerPropertiesAndScoping() {
        val baseLayer1 = Layer(id = "layer-1", name = "Photo 1", type = LayerType.MEDIA)
        val baseLayer2 = Layer(id = "layer-2", name = "Photo 2", type = LayerType.MEDIA)

        val adjustmentAll = Layer(
            id = "adj-all",
            name = "Global Color Grading",
            type = LayerType.ADJUSTMENT,
            blendMode = BlendMode.OVERLAY,
            opacity = 0.75f,
            affectsLayerIds = emptyList() // Affects all underlying layers
        )

        assertEquals(LayerType.ADJUSTMENT, adjustmentAll.type)
        assertEquals(BlendMode.OVERLAY, adjustmentAll.blendMode)
        assertEquals(0.75f, adjustmentAll.opacity, 0.01f)
        assertTrue(adjustmentAll.affectsLayerIds.isEmpty())

        // Target scoped adjustment layer
        val adjustmentScoped = Layer(
            id = "adj-scoped",
            name = "Scoped Brightness",
            type = LayerType.ADJUSTMENT,
            blendMode = BlendMode.SCREEN,
            opacity = 0.5f,
            affectsLayerIds = listOf("layer-1") // Affects only layer-1
        )

        assertTrue(adjustmentScoped.affectsLayerIds.contains("layer-1"))
        assertFalse(adjustmentScoped.affectsLayerIds.contains("layer-2"))

        val doc = ProjectDocument(
            id = "doc-adj",
            name = "Adj Test",
            layers = listOf(baseLayer1, baseLayer2, adjustmentScoped, adjustmentAll)
        )

        assertEquals(4, doc.layers.size)
        val adjLayers = doc.layers.filter { it.type == LayerType.ADJUSTMENT }
        assertEquals(2, adjLayers.size)
    }

    @Test
    fun testAdjustmentLayerMaskModeIntegration() {
        val adjWithMask = Layer(
            id = "adj-masked",
            name = "Vignette Mask",
            type = LayerType.ADJUSTMENT,
            blendMode = BlendMode.MULTIPLY,
            maskMode = MaskMode.IN_MASK,
            affectsLayerIds = listOf("layer-1")
        )

        assertEquals(MaskMode.IN_MASK, adjWithMask.maskMode)
        assertEquals(MaskType.ALPHA, adjWithMask.effectiveMaskType)
        assertFalse(adjWithMask.effectiveMaskInverted)
        assertEquals("layer-1", adjWithMask.effectiveMaskSourceLayerId)
        assertEquals(BlendMode.MULTIPLY, adjWithMask.blendMode)
    }
}
