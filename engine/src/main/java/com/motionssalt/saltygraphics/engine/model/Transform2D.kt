package com.motionssalt.saltygraphics.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class Transform2D(
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val isScaleLinked: Boolean = true,
    val rotationDegrees: Float = 0f,
    val moveSensitivity: Float = 1f,
    val isTrackpadMode: Boolean = true
) {
    /**
     * Updates scale linked: scales both axes relatively without resetting base values.
     */
    fun scaleLinked(multiplier: Float): Transform2D {
        return copy(
            scaleX = (scaleX * multiplier).coerceIn(0.01f, 50f),
            scaleY = (scaleY * multiplier).coerceIn(0.01f, 50f)
        )
    }

    fun setScaleUniform(newScale: Float): Transform2D {
        if (!isScaleLinked) {
            return copy(scaleX = newScale.coerceIn(0.01f, 50f))
        }
        val currentAvg = (scaleX + scaleY) / 2f
        val ratio = if (currentAvg != 0f) newScale / currentAvg else 1f
        return copy(
            scaleX = (scaleX * ratio).coerceIn(0.01f, 50f),
            scaleY = (scaleY * ratio).coerceIn(0.01f, 50f)
        )
    }

    fun translateBy(dx: Float, dy: Float, sensitivity: Float = moveSensitivity): Transform2D {
        return copy(
            translationX = translationX + (dx * sensitivity),
            translationY = translationY + (dy * sensitivity)
        )
    }

    fun rotateBy(deltaDegrees: Float): Transform2D {
        val total = (rotationDegrees + deltaDegrees) % 360f
        return copy(rotationDegrees = if (total < 0) total + 360f else total)
    }
}
