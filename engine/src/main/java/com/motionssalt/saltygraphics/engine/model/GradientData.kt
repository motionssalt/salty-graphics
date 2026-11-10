package com.motionssalt.saltygraphics.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class GradientType(val displayName: String) {
    LINEAR("Linear"),
    RADIAL("Radial"),
    ANGULAR("Angular / Conic"),
    REFLECTED("Reflected"),
    DIAMOND("Diamond"),
    FREEFORM("Freeform")
}

@Serializable
enum class GradientRepeatMode(val displayName: String) {
    CLAMP("Clamp"),
    REPEAT("Repeat"),
    MIRROR("Mirror")
}

@Serializable
enum class GradientInterpolation(val displayName: String) {
    LINEAR("Linear"),
    SMOOTH("Smooth")
}

@Serializable
data class GradientStop(
    val position: Float = 0f, // 0.0 to 1.0 along gradient line/radius
    val color: Long = 0xFFFFFFFFL, // ARGB color
    val opacity: Float = 1.0f, // 0.0 to 1.0 opacity
    val midpoint: Float = 0.5f // 0.0 to 1.0 relative bias between this stop and the next
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
    val centerX: Float = 0.5f, // For radial/angular/diamond (normalized 0..1)
    val centerY: Float = 0.5f,
    val radius: Float = 0.5f, // Normalized radius or scale
    val aspect: Float = 1.0f, // Aspect ratio for radial and diamond
    val repeatMode: GradientRepeatMode = GradientRepeatMode.CLAMP,
    val interpolation: GradientInterpolation = GradientInterpolation.SMOOTH,
    val dither: Boolean = true,
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

    fun reversed(): GradientData {
        val sorted = sortedStops()
        val newStops = sorted.reversed().map { stop ->
            stop.copy(position = (1f - stop.position).coerceIn(0f, 1f))
        }
        return copy(stops = newStops)
    }

    fun distributeEvenly(): GradientData {
        if (stops.size <= 1) return this
        val sorted = sortedStops()
        val step = 1f / (sorted.size - 1)
        val newStops = sorted.mapIndexed { idx, stop ->
            stop.copy(position = (idx * step).coerceIn(0f, 1f))
        }
        return copy(stops = newStops)
    }
}
