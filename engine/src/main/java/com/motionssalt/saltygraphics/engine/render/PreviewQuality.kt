package com.motionssalt.saltygraphics.engine.render

import kotlinx.serialization.Serializable

@Serializable
enum class PreviewQuality(val displayName: String, val scaleFactor: Float) {
    QUARTER("1/4", 0.25f),
    HALF("1/2", 0.50f),
    THREE_QUARTERS("3/4", 0.75f),
    FULL("Full", 1.0f),
    // Legacy entries for backwards compatibility with saved archives
    LOW("1/4", 0.25f),
    MEDIUM("1/2", 0.50f),
    HIGH("Full", 1.0f);

    fun next(): PreviewQuality = when (this) {
        QUARTER, LOW -> HALF
        HALF, MEDIUM -> THREE_QUARTERS
        THREE_QUARTERS -> FULL
        FULL, HIGH -> QUARTER
    }
}
