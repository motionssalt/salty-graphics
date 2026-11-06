package com.motionssalt.saltygraphics.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class AspectRatioCategory {
    POPULAR,
    STANDARD,
    SOCIAL,
    PAPER_PRINT
}

@Serializable
enum class AspectRatioPreset(
    val displayName: String,
    val ratioWidth: Float,
    val ratioHeight: Float,
    val category: AspectRatioCategory = AspectRatioCategory.STANDARD,
    val shortName: String = displayName
) {
    // Original enum entries (retaining exact names for backwards compatibility)
    SQUARE("1:1", 1f, 1f, AspectRatioCategory.POPULAR, "Square"),
    PORTRAIT_4_5("4:5", 4f, 5f, AspectRatioCategory.POPULAR, "Portrait"),
    STORY_9_16("9:16", 9f, 16f, AspectRatioCategory.POPULAR, "Story"),
    LANDSCAPE_16_9("16:9", 16f, 9f, AspectRatioCategory.POPULAR, "Landscape"),
    PHOTO_3_2("3:2", 3f, 2f, AspectRatioCategory.POPULAR, "Photo"),
    A4("A4 (1:1.414)", 1f, 1.414f, AspectRatioCategory.POPULAR, "A4"),
    CUSTOM("Custom", 1f, 1f, AspectRatioCategory.POPULAR, "Custom"),

    // Expanded ratios required by R9
    PORTRAIT_3_4("3:4", 3f, 4f, AspectRatioCategory.STANDARD, "3:4"),
    LANDSCAPE_4_3("4:3", 4f, 3f, AspectRatioCategory.STANDARD, "4:3"),
    LANDSCAPE_5_4("5:4", 5f, 4f, AspectRatioCategory.STANDARD, "5:4"),
    PORTRAIT_2_3("2:3", 2f, 3f, AspectRatioCategory.STANDARD, "2:3"),
    ULTRAWIDE_21_9("21:9", 21f, 9f, AspectRatioCategory.STANDARD, "21:9"),
    LANDSCAPE_2_1("2:1", 2f, 1f, AspectRatioCategory.STANDARD, "2:1"),
    PORTRAIT_1_2("1:2", 1f, 2f, AspectRatioCategory.STANDARD, "1:2"),
    BANNER_3_1("3:1", 3f, 1f, AspectRatioCategory.STANDARD, "3:1"),
    SKYSCRAPER_1_3("1:3", 1f, 3f, AspectRatioCategory.STANDARD, "1:3"),
    SMARTPHONE_9_19_5("9:19.5", 9f, 19.5f, AspectRatioCategory.STANDARD, "Phone"),
    SMARTPHONE_19_5_9("19.5:9", 19.5f, 9f, AspectRatioCategory.STANDARD, "Phone Horiz"),

    // Paper & Print (A-series, US Letter, Business card)
    A3_PORTRAIT("A3 Portrait", 1f, 1.414f, AspectRatioCategory.PAPER_PRINT, "A3"),
    A3_LANDSCAPE("A3 Landscape", 1.414f, 1f, AspectRatioCategory.PAPER_PRINT, "A3 Land"),
    A4_LANDSCAPE("A4 Landscape", 1.414f, 1f, AspectRatioCategory.PAPER_PRINT, "A4 Land"),
    A5_PORTRAIT("A5 Portrait", 1f, 1.414f, AspectRatioCategory.PAPER_PRINT, "A5"),
    A5_LANDSCAPE("A5 Landscape", 1.414f, 1f, AspectRatioCategory.PAPER_PRINT, "A5 Land"),
    US_LETTER_PORTRAIT("US Letter", 8.5f, 11f, AspectRatioCategory.PAPER_PRINT, "Letter"),
    US_LETTER_LANDSCAPE("US Letter Landscape", 11f, 8.5f, AspectRatioCategory.PAPER_PRINT, "Letter Land"),
    BUSINESS_CARD_LANDSCAPE("Business Card", 3.5f, 2f, AspectRatioCategory.PAPER_PRINT, "Biz Card"),
    BUSINESS_CARD_PORTRAIT("Business Card Portrait", 2f, 3.5f, AspectRatioCategory.PAPER_PRINT, "Biz Card Port"),

    // Social formats
    INSTAGRAM_POST("Instagram Post", 1f, 1f, AspectRatioCategory.SOCIAL, "IG Post"),
    INSTAGRAM_PORTRAIT("Instagram Portrait", 4f, 5f, AspectRatioCategory.SOCIAL, "IG 4:5"),
    INSTAGRAM_STORY("Instagram Story / Reel", 9f, 16f, AspectRatioCategory.SOCIAL, "Reels"),
    YOUTUBE_THUMBNAIL("YouTube Thumbnail", 16f, 9f, AspectRatioCategory.SOCIAL, "YT Thumb"),
    FACEBOOK_COVER("Facebook Cover", 820f, 312f, AspectRatioCategory.SOCIAL, "FB Cover"),
    X_POST("X / Twitter Post", 16f, 9f, AspectRatioCategory.SOCIAL, "X Post"),
    X_HEADER("X Header", 3f, 1f, AspectRatioCategory.SOCIAL, "X Header"),
    LINKEDIN_BANNER("LinkedIn Banner", 4f, 1f, AspectRatioCategory.SOCIAL, "LinkedIn"),
    PINTEREST_PIN("Pinterest Pin", 2f, 3f, AspectRatioCategory.SOCIAL, "Pin");

    fun computeDimensions(quality: QualityPreset, isRotated: Boolean = false): Pair<Int, Int> {
        val rw = if (isRotated) ratioHeight else ratioWidth
        val rh = if (isRotated) ratioWidth else ratioHeight
        val baseDim = quality.baseDimension
        return when {
            rw >= rh -> {
                val w = (baseDim * (rw / rh)).toInt()
                val h = baseDim
                Pair(w, h)
            }
            else -> {
                val w = baseDim
                val h = (baseDim * (rh / rw)).toInt()
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
