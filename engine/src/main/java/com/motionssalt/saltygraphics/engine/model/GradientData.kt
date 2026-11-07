package com.motionssalt.saltygraphics.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class GradientType(val displayName: String) {
    LINEAR("Linear"),
    RADIAL("Radial"),
    FREEFORM("Freeform")
}

@Serializable
data class GradientStop(
    val position: Float = 0f, // 0.0 to 1.0 along gradient line/radius
    val color: Long = 0xFFFFFFFFL, // ARGB color
    val opacity: Float = 1.0f // 0.0 to 1.0 opacity
) {
    val effectiveArgb: Long
        get() {
            val a = ((color shr 24 and 0xFF) / 255f * opacity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
            return ((a.toLong() and 0xFF) shl 24) or (color and 0x00FFFFFFL)
        }
}

@Serializable
data class FreeformPoint(
    val x: Float = 0.5f, // Normalized 0.0 to 1.0 within bounding box
    val y: Float = 0.5f,
    val color: Long = 0xFF00E5FFL,
    val opacity: Float = 1.0f,
    val radius: Float = 0.5f // Falloff radius
) {
    val effectiveArgb: Long
        get() {
            val a = ((color shr 24 and 0xFF) / 255f * opacity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
            return ((a.toLong() and 0xFF) shl 24) or (color and 0x00FFFFFFL)
        }
}

@Serializable
data class GradientData(
    val type: GradientType = GradientType.LINEAR,
    val angleDegrees: Float = 0f, // 0 = left-to-right, 90 = top-to-bottom
    val centerX: Float = 0.5f, // For radial gradient (normalized 0..1)
    val centerY: Float = 0.5f,
    val radius: Float = 0.5f, // Normalized radius
    val stops: List<GradientStop> = listOf(
        GradientStop(position = 0f, color = 0xFF00E5FFL, opacity = 1f),
        GradientStop(position = 1f, color = 0xFF2979FFL, opacity = 1f)
    ),
    val freeformPoints: List<FreeformPoint> = listOf(
        FreeformPoint(x = 0.2f, y = 0.2f, color = 0xFFFF5252L, opacity = 1f, radius = 0.6f),
        FreeformPoint(x = 0.8f, y = 0.2f, color = 0xFFFFB300L, opacity = 1f, radius = 0.6f),
        FreeformPoint(x = 0.5f, y = 0.8f, color = 0xFF00E5FFL, opacity = 1f, radius = 0.6f)
    )
) {
    fun sortedStops(): List<GradientStop> = stops.sortedBy { it.position }
}
