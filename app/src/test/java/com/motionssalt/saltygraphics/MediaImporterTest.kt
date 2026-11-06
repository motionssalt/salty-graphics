package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.media.MediaCache
import org.junit.Assert.*
import org.junit.Test

class MediaImporterTest {

    @Test
    fun testSampleSizeCalculation() {
        val maxTextureDim = 4096

        fun calculateSampleSize(w: Int, h: Int): Int {
            val maxDim = maxOf(w, h)
            var sampleSize = 1
            while (maxDim / sampleSize > maxTextureDim) {
                sampleSize *= 2
            }
            return sampleSize
        }

        // Standard 1080p -> sampleSize 1
        assertEquals(1, calculateSampleSize(1920, 1080))

        // 4K (3840x2160) -> sampleSize 1
        assertEquals(1, calculateSampleSize(3840, 2160))

        // 8K (7680x4320) -> exceeds 4096 -> sampleSize 2
        assertEquals(2, calculateSampleSize(7680, 4320))

        // Ultra high 16K (15360x8640) -> sampleSize 4
        assertEquals(4, calculateSampleSize(15360, 8640))
    }

    @Test
    fun testInitialLayerDimensionFitting() {
        val canvasWidth = 1080
        val canvasHeight = 1920
        val maxInitialSize = minOf(canvasWidth, canvasHeight) * 0.7f // 756px

        fun calculateFitDimensions(imgW: Float, imgH: Float): Pair<Float, Float> {
            val fitScale = if (maxOf(imgW, imgH) > maxInitialSize) {
                maxInitialSize / maxOf(imgW, imgH)
            } else 1f
            return Pair(imgW * fitScale, imgH * fitScale)
        }

        // Large image 3000x2000 fits to 756x504
        val (w1, h1) = calculateFitDimensions(3000f, 2000f)
        assertEquals(756f, w1, 0.01f)
        assertEquals(504f, h1, 0.01f)

        // Small image 300x200 remains 300x200
        val (w2, h2) = calculateFitDimensions(300f, 200f)
        assertEquals(300f, w2, 0.01f)
        assertEquals(200f, h2, 0.01f)
    }

    @Test
    fun testMediaLayerProperties() {
        val mediaLayer = Layer(
            id = "media-1",
            name = "Hero Background",
            type = LayerType.MEDIA,
            mediaAssetPath = "assets/media_123.png",
            mediaWidth = 800f,
            mediaHeight = 600f
        )

        assertEquals(LayerType.MEDIA, mediaLayer.type)
        assertEquals("assets/media_123.png", mediaLayer.mediaAssetPath)
        assertEquals(800f, mediaLayer.mediaWidth ?: 0f, 0.01f)
        assertEquals(600f, mediaLayer.mediaHeight ?: 0f, 0.01f)
        assertEquals(800f, mediaLayer.localWidth, 0.01f)
        assertEquals(600f, mediaLayer.localHeight, 0.01f)
    }
}
