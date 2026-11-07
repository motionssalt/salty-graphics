package com.motionssalt.saltygraphics.engine.model

import kotlin.math.*

enum class GuideOrientation { VERTICAL, HORIZONTAL }

data class SmartGuideLine(
    val orientation: GuideOrientation,
    val position: Float, // Document coordinate along X (vertical line) or Y (horizontal line)
    val start: Float,    // Extent start in document units
    val end: Float,      // Extent end in document units
    val isSpacingGuide: Boolean = false,
    val spacingValue: Float = 0f
)

data class SnapResult(
    val snappedTransform: Transform2D,
    val activeGuides: List<SmartGuideLine> = emptyList(),
    val didSnapX: Boolean = false,
    val didSnapY: Boolean = false,
    val didSnapRotation: Boolean = false
)

object SmartSnapEngine {
    // Default snap tolerance in screen pixels (zoom-independent)
    const val DEFAULT_TOLERANCE_PX = 8f
    // Hysteresis multiplier to prevent flickering around threshold
    const val HYSTERESIS_FACTOR = 1.35f

    /**
     * Snaps a moving/resizing layer's transform to canvas edges/center and other layers' geometry.
     * toleranceScreenPx is stable across zoom levels by dividing by effectiveZoom.
     */
    fun snapTranslation(
        targetBounds: SelectionBounds,
        targetTransform: Transform2D,
        otherLayers: List<Pair<SelectionBounds, Transform2D>>,
        canvasWidth: Float,
        canvasHeight: Float,
        effectiveZoom: Float,
        tolerancePx: Float = DEFAULT_TOLERANCE_PX,
        previouslySnappedX: Boolean = false,
        previouslySnappedY: Boolean = false
    ): SnapResult {
        val zoom = effectiveZoom.coerceAtLeast(0.001f)
        val toleranceDocX = (tolerancePx / zoom) * (if (previouslySnappedX) HYSTERESIS_FACTOR else 1.0f)
        val toleranceDocY = (tolerancePx / zoom) * (if (previouslySnappedY) HYSTERESIS_FACTOR else 1.0f)

        val tx = targetTransform.translationX
        val ty = targetTransform.translationY
        val halfW = targetBounds.width * targetTransform.scaleX / 2f
        val halfH = targetBounds.height * targetTransform.scaleY / 2f

        val left = tx - halfW
        val right = tx + halfW
        val top = ty - halfH
        val bottom = ty + halfH
        val centerX = tx
        val centerY = ty

        // Canvas anchors (relative to center 0,0)
        val canvasHalfW = canvasWidth / 2f
        val canvasHalfH = canvasHeight / 2f
        val xTargets = mutableListOf(-canvasHalfW, 0f, canvasHalfW)
        val yTargets = mutableListOf(-canvasHalfH, 0f, canvasHalfH)

        // Other layers' anchors (left, center, right, top, center, bottom)
        for ((b, t) in otherLayers) {
            val ox = t.translationX
            val oy = t.translationY
            val ohw = b.width * t.scaleX / 2f
            val ohh = b.height * t.scaleY / 2f
            xTargets.add(ox - ohw)
            xTargets.add(ox)
            xTargets.add(ox + ohw)
            yTargets.add(oy - ohh)
            yTargets.add(oy)
            yTargets.add(oy + ohh)
        }

        var bestDeltaX = Float.MAX_VALUE
        var bestSnapX: Float? = null
        var guideX: Float? = null

        // Test center, left, right against X targets
        for (targetX in xTargets) {
            val dCenter = targetX - centerX
            if (abs(dCenter) < abs(bestDeltaX) && abs(dCenter) <= toleranceDocX) {
                bestDeltaX = dCenter
                bestSnapX = tx + dCenter
                guideX = targetX
            }
            val dLeft = targetX - left
            if (abs(dLeft) < abs(bestDeltaX) && abs(dLeft) <= toleranceDocX) {
                bestDeltaX = dLeft
                bestSnapX = tx + dLeft
                guideX = targetX
            }
            val dRight = targetX - right
            if (abs(dRight) < abs(bestDeltaX) && abs(dRight) <= toleranceDocX) {
                bestDeltaX = dRight
                bestSnapX = tx + dRight
                guideX = targetX
            }
        }

        var bestDeltaY = Float.MAX_VALUE
        var bestSnapY: Float? = null
        var guideY: Float? = null

        // Test center, top, bottom against Y targets
        for (targetY in yTargets) {
            val dCenter = targetY - centerY
            if (abs(dCenter) < abs(bestDeltaY) && abs(dCenter) <= toleranceDocY) {
                bestDeltaY = dCenter
                bestSnapY = ty + dCenter
                guideY = targetY
            }
            val dTop = targetY - top
            if (abs(dTop) < abs(bestDeltaY) && abs(dTop) <= toleranceDocY) {
                bestDeltaY = dTop
                bestSnapY = ty + dTop
                guideY = targetY
            }
            val dBottom = targetY - bottom
            if (abs(dBottom) < abs(bestDeltaY) && abs(dBottom) <= toleranceDocY) {
                bestDeltaY = dBottom
                bestSnapY = ty + dBottom
                guideY = targetY
            }
        }

        val guides = mutableListOf<SmartGuideLine>()
        if (guideX != null) {
            guides.add(SmartGuideLine(
                orientation = GuideOrientation.VERTICAL,
                position = guideX,
                start = -canvasHalfH,
                end = canvasHalfH
            ))
        }
        if (guideY != null) {
            guides.add(SmartGuideLine(
                orientation = GuideOrientation.HORIZONTAL,
                position = guideY,
                start = -canvasHalfW,
                end = canvasHalfW
            ))
        }

        val finalTransform = targetTransform.copy(
            translationX = bestSnapX ?: tx,
            translationY = bestSnapY ?: ty
        )

        return SnapResult(
            snappedTransform = finalTransform,
            activeGuides = guides,
            didSnapX = bestSnapX != null,
            didSnapY = bestSnapY != null
        )
    }

    /**
     * Snaps rotation degrees to meaningful angles (0, 45, 90, 135, 180, etc.)
     * and other layers' angles within a tolerance degrees.
     */
    fun snapRotation(
        currentDegrees: Float,
        otherAngles: List<Float> = emptyList(),
        toleranceDeg: Float = 3.5f
    ): Pair<Float, Boolean> {
        val standardAngles = floatArrayOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f, 360f)
        val normalized = ((currentDegrees % 360f) + 360f) % 360f

        for (target in standardAngles) {
            if (abs(normalized - target) <= toleranceDeg || abs(normalized - (target - 360f)) <= toleranceDeg) {
                return Pair(if (target == 360f) 0f else target, true)
            }
        }

        for (other in otherAngles) {
            val normOther = ((other % 360f) + 360f) % 360f
            if (abs(normalized - normOther) <= toleranceDeg) {
                return Pair(normOther, true)
            }
        }
        return Pair(currentDegrees, false)
    }
}
