package com.motionssalt.saltygraphics.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class StrokeAlignment(val displayName: String) {
    INSIDE("Inside"),
    CENTER("Center"),
    OUTSIDE("Outside")
}

@Serializable
data class StrokeData(
    val isEnabled: Boolean = false,
    val color: Long = 0xFFFFFFFFL,
    val opacity: Float = 1.0f,
    val width: Float = 4f,
    val alignment: StrokeAlignment = StrokeAlignment.CENTER,
    val cap: VectorCap = VectorCap.ROUND,
    val join: VectorJoin = VectorJoin.ROUND,
    val isDashed: Boolean = false,
    val dashLength: Float = 12f,
    val dashGap: Float = 8f
) {
    val effectiveArgb: Long
        get() {
            val a = ((color shr 24 and 0xFF) / 255f * opacity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
            return ((a.toLong() and 0xFF) shl 24) or (color and 0x00FFFFFFL)
        }
}

@Serializable
data class ShadowData(
    val isEnabled: Boolean = false,
    val color: Long = 0xFF000000L,
    val opacity: Float = 0.5f,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val lightAngleDegrees: Float = 45f, // Direction light comes from or points to (45° = bottom-right drop)
    val distance: Float = 10f,
    val blurRadius: Float = 16f,
    val spread: Float = 0f
) {
    val effectiveArgb: Long
        get() {
            val a = ((color shr 24 and 0xFF) / 255f * opacity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
            return ((a.toLong() and 0xFF) shl 24) or (color and 0x00FFFFFFL)
        }

    val offsetX: Float
        get() {
            val rad = Math.toRadians(lightAngleDegrees.toDouble())
            return (distance * kotlin.math.cos(rad)).toFloat()
        }

    val offsetY: Float
        get() {
            val rad = Math.toRadians(lightAngleDegrees.toDouble())
            return (distance * kotlin.math.sin(rad)).toFloat()
        }
}
