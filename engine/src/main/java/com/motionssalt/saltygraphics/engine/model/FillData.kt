package com.motionssalt.saltygraphics.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class FillType(val displayName: String) {
    SOLID("Solid Color"),
    GRADIENT("Gradient"),
    MEDIA("Media Fill")
}

@Serializable
enum class MediaScaleMode(val displayName: String) {
    FILL("Fill"),       // Cover layer area, crop overflow
    FIT("Fit"),         // Fit completely inside bounds, letterbox/transparent empty space
    STRETCH("Stretch")  // Distort independently in X and Y to exact layer bounds
}

@Serializable
data class FillData(
    val type: FillType = FillType.SOLID,
    val solidColor: Long = 0xFF00E5FFL, // ARGB
    val solidOpacity: Float = 1.0f,
    val gradient: GradientData = GradientData(),
    val mediaAssetPath: String? = null,
    val mediaScaleMode: MediaScaleMode = MediaScaleMode.FILL,
    val mediaOffsetX: Float = 0f, // Offset within fill bounds (-1..1)
    val mediaOffsetY: Float = 0f
) {
    val effectiveSolidArgb: Long
        get() {
            val a = ((solidColor shr 24 and 0xFF) / 255f * solidOpacity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
            return ((a.toLong() and 0xFF) shl 24) or (solidColor and 0x00FFFFFFL)
        }
}
