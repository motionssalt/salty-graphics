package com.motionssalt.saltygraphics.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class AspectRatioPreset(val displayName: String, val ratioWidth: Float, val ratioHeight: Float) {
    SQUARE("1:1", 1f, 1f),
    PORTRAIT_4_5("4:5", 4f, 5f),
    STORY_9_16("9:16", 9f, 16f),
    LANDSCAPE_16_9("16:9", 16f, 9f),
    PHOTO_3_2("3:2", 3f, 2f),
    A4("A4 (1:1.414)", 1f, 1.414f),
    CUSTOM("Custom", 1f, 1f);

    fun computeDimensions(quality: QualityPreset): Pair<Int, Int> {
        val baseDim = quality.baseDimension
        return when {
            ratioWidth >= ratioHeight -> {
                val w = (baseDim * (ratioWidth / ratioHeight)).toInt()
                val h = baseDim
                Pair(w, h)
            }
            else -> {
                val w = baseDim
                val h = (baseDim * (ratioHeight / ratioWidth)).toInt()
                Pair(w, h)
            }
        }
    }
}

@Serializable
enum class QualityPreset(val displayName: String, val baseDimension: Int, val isHeavy: Boolean = false) {
    P360("360p", 360),
    P720("720p HD", 720),
    P1080("1080p FHD", 1080),
    P1440("1440p 2K", 1440),
    P4K("4K UHD", 2160, isHeavy = false),
    P8K("8K FUHD", 4320, isHeavy = true);

    val memoryEstimatePerLayerMB: Float
        get() = (baseDimension * baseDimension * 4f) / (1024f * 1024f)
}

@Serializable
data class CanvasSize(
    val width: Int = 1080,
    val height: Int = 1920,
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.STORY_9_16,
    val quality: QualityPreset = QualityPreset.P1080
) {
    val isHeavyMemory: Boolean
        get() = (width.toLong() * height.toLong() * 4L) > (30L * 1024L * 1024L) // > 30MB per layer

    val memoryBytesPerLayer: Long
        get() = width.toLong() * height.toLong() * 4L
}
