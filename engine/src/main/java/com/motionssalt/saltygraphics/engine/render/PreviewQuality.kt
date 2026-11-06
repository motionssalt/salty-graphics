package com.motionssalt.saltygraphics.engine.render

import kotlinx.serialization.Serializable

@Serializable
enum class PreviewQuality(val displayName: String, val scaleFactor: Float) {
    LOW("Low (25%)", 0.25f),
    MEDIUM("Medium (50%)", 0.50f),
    HIGH("High (100%)", 1.0f);

    fun next(): PreviewQuality = when (this) {
        LOW -> MEDIUM
        MEDIUM -> HIGH
        HIGH -> LOW
    }
}
