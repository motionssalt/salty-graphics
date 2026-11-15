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

data class GridSnapResult(
    val snappedTransform: Transform2D,
    val snappedGridX: Float? = null,
    val snappedGridY: Float? = null,
    val didSnapX: Boolean = false,
    val didSnapY: Boolean = false
)

data class SnapResult(
    val snappedTransform: Transform2D,
    val activeGuides: List<SmartGuideLine> = emptyList(),
    val didSnapX: Boolean = false,
    val didSnapY: Boolean = false,
    val didSnapRotation: Boolean = false,
    val didSnapCenterX: Boolean = false,
    val didSnapCenterY: Boolean = false
)

object SmartSnapEngine {
    // Default snap tolerance in screen pixels (zoom-independent)
    const val DEFAULT_TOLERANCE_PX = 8f
    // Hysteresis multiplier to prevent flickering around threshold
    const val HYSTERESIS_FACTOR = 1.35f
    const val CENTER_TOLERANCE_MULTIPLIER = 1.25f
    const val CENTER_HYSTERESIS_FACTOR = 1.85f

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
        previouslySnappedY: Boolean = false,
        previouslySnappedCenterX: Boolean = false,
        previouslySnappedCenterY: Boolean = false
    ): SnapResult {
        val zoom = effectiveZoom.coerceAtLeast(0.001f)
        val toleranceDocX = (tolerancePx / zoom) * (if (previouslySnappedX) HYSTERESIS_FACTOR else 1.0f)
        val toleranceDocY = (tolerancePx / zoom) * (if (previouslySnappedY) HYSTERESIS_FACTOR else 1.0f)
        val centerToleranceDocX = (tolerancePx * CENTER_TOLERANCE_MULTIPLIER / zoom) *
            (if (previouslySnappedCenterX) CENTER_HYSTERESIS_FACTOR else 1.0f)
        val centerToleranceDocY = (tolerancePx * CENTER_TOLERANCE_MULTIPLIER / zoom) *
            (if (previouslySnappedCenterY) CENTER_HYSTERESIS_FACTOR else 1.0f)

        val tx = targetTransform.translationX
        val ty = targetTransform.translationY
        val worldBounds = SelectionBounds.enclosing(
            targetBounds.corners.map { SelectionGeometry.world(it, targetTransform) }
        )
        val left = worldBounds.left
        val right = worldBounds.right
        val top = worldBounds.top
        val bottom = worldBounds.bottom
        val centerX = worldBounds.center.x
        val centerY = worldBounds.center.y

        val canvasHalfW = canvasWidth / 2f
        val canvasHalfH = canvasHeight / 2f

        val xTargets = mutableListOf(-canvasHalfW, 0f, canvasHalfW)
        val yTargets = mutableListOf(-canvasHalfH, 0f, canvasHalfH)

        for ((b, t) in otherLayers) {
            val ow = SelectionBounds.enclosing(b.corners.map { SelectionGeometry.world(it, t) })
            xTargets.add(ow.left)
            xTargets.add(ow.center.x)
            xTargets.add(ow.right)
            yTargets.add(ow.top)
            yTargets.add(ow.center.y)
            yTargets.add(ow.bottom)
        }

        var bestDeltaX = Float.MAX_VALUE
        var bestSnapX: Float? = null
        var guideX: Float? = null
        var snappedCenterX = false

        val dCanvasCenterX = 0f - centerX
        if (abs(dCanvasCenterX) <= centerToleranceDocX) {
            bestDeltaX = dCanvasCenterX
            bestSnapX = tx + dCanvasCenterX
            guideX = 0f
            snappedCenterX = true
        } else {
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
        }

        var bestDeltaY = Float.MAX_VALUE
        var bestSnapY: Float? = null
        var guideY: Float? = null
        var snappedCenterY = false

        val dCanvasCenterY = 0f - centerY
        if (abs(dCanvasCenterY) <= centerToleranceDocY) {
            bestDeltaY = dCanvasCenterY
            bestSnapY = ty + dCanvasCenterY
            guideY = 0f
            snappedCenterY = true
        } else {
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
            didSnapY = bestSnapY != null,
            didSnapCenterX = snappedCenterX,
            didSnapCenterY = snappedCenterY
        )    }

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

    /**
     * V15: Snaps moving/resizing layer to visible grid lines with transient feedback.
     * Uses screen-pixel tolerance invariant across zoom levels.
     */
    fun snapToGridLines(
        targetBounds: SelectionBounds,
        targetTransform: Transform2D,
        canvasWidth: Float,
        canvasHeight: Float,
        gridSize: Int,
        isSquareGrid: Boolean,
        effectiveZoom: Float,
        tolerancePx: Float = DEFAULT_TOLERANCE_PX,
        previouslySnappedX: Boolean = false,
        previouslySnappedY: Boolean = false
    ): GridSnapResult {
        if (gridSize <= 0) return GridSnapResult(targetTransform)

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

        val canvasHalfW = canvasWidth / 2f
        val canvasHalfH = canvasHeight / 2f

        val xTargets = mutableListOf<Float>()
        val yTargets = mutableListOf<Float>()

        if (isSquareGrid) {
            val step = gridSize.toFloat()
            var x = -canvasHalfW
            while (x <= canvasHalfW + 0.1f) {
                xTargets.add(x)
                x += step
            }
            var y = -canvasHalfH
            while (y <= canvasHalfH + 0.1f) {
                yTargets.add(y)
                y += step
            }
        } else {
            val cols = maxOf(1, (canvasWidth / gridSize).roundToInt())
            val rows = maxOf(1, (canvasHeight / gridSize).roundToInt())
            val colSpacing = canvasWidth / cols
            val rowSpacing = canvasHeight / rows
            for (c in 0..cols) {
                xTargets.add(-canvasHalfW + c * colSpacing)
            }
            for (r in 0..rows) {
                yTargets.add(-canvasHalfH + r * rowSpacing)
            }
        }

        var bestDeltaX = Float.MAX_VALUE
        var bestSnapX: Float? = null
        var snappedLineX: Float? = null

        for (targetX in xTargets) {
            val dCenter = targetX - centerX
            if (abs(dCenter) < abs(bestDeltaX) && abs(dCenter) <= toleranceDocX) {
                bestDeltaX = dCenter
                bestSnapX = tx + dCenter
                snappedLineX = targetX
            }
            val dLeft = targetX - left
            if (abs(dLeft) < abs(bestDeltaX) && abs(dLeft) <= toleranceDocX) {
                bestDeltaX = dLeft
                bestSnapX = tx + dLeft
                snappedLineX = targetX
            }
            val dRight = targetX - right
            if (abs(dRight) < abs(bestDeltaX) && abs(dRight) <= toleranceDocX) {
                bestDeltaX = dRight
                bestSnapX = tx + dRight
                snappedLineX = targetX
            }
        }

        var bestDeltaY = Float.MAX_VALUE
        var bestSnapY: Float? = null
        var snappedLineY: Float? = null

        for (targetY in yTargets) {
            val dCenter = targetY - centerY
            if (abs(dCenter) < abs(bestDeltaY) && abs(dCenter) <= toleranceDocY) {
                bestDeltaY = dCenter
                bestSnapY = ty + dCenter
                snappedLineY = targetY
            }
            val dTop = targetY - top
            if (abs(dTop) < abs(bestDeltaY) && abs(dTop) <= toleranceDocY) {
                bestDeltaY = dTop
                bestSnapY = ty + dTop
                snappedLineY = targetY
            }
            val dBottom = targetY - bottom
            if (abs(dBottom) < abs(bestDeltaY) && abs(dBottom) <= toleranceDocY) {
                bestDeltaY = dBottom
                bestSnapY = ty + dBottom
                snappedLineY = targetY
            }
        }

        val finalTransform = targetTransform.copy(
            translationX = bestSnapX ?: tx,
            translationY = bestSnapY ?: ty
        )

        return GridSnapResult(
            snappedTransform = finalTransform,
            snappedGridX = snappedLineX,
            snappedGridY = snappedLineY,
            didSnapX = bestSnapX != null,
            didSnapY = bestSnapY != null
        )
    }

    /**
     * V38: Snaps dragged scale-handle edge to the canvas center axis (0, 0) when unrotated
     * so canvas center holds across handle drags as well as body drags.
     */
    fun snapScaleHandleToCanvasCenter(
        targetBounds: SelectionBounds,
        startTransform: Transform2D,
        rawTransform: Transform2D,
        handle: SelectionHandle,
        canvasWidth: Float,
        canvasHeight: Float,
        effectiveZoom: Float,
        tolerancePx: Float = DEFAULT_TOLERANCE_PX,
        previouslySnappedCenterX: Boolean = false,
        previouslySnappedCenterY: Boolean = false
    ): SnapResult {
        if (handle == SelectionHandle.NONE || handle == SelectionHandle.BODY || handle == SelectionHandle.ROTATE) {
            return SnapResult(rawTransform)
        }
        val rotMod = abs(rawTransform.rotationDegrees % 180f)
        if (rotMod > 0.01f && abs(rotMod - 180f) > 0.01f) {
            return SnapResult(rawTransform)
        }
        val zoom = effectiveZoom.coerceAtLeast(0.001f)
        val centerTolX = (tolerancePx * CENTER_TOLERANCE_MULTIPLIER / zoom) *
            (if (previouslySnappedCenterX) CENTER_HYSTERESIS_FACTOR else 1.0f)
        val centerTolY = (tolerancePx * CENTER_TOLERANCE_MULTIPLIER / zoom) *
            (if (previouslySnappedCenterY) CENTER_HYSTERESIS_FACTOR else 1.0f)

        var sx = rawTransform.scaleX
        var sy = rawTransform.scaleY
        var snappedCX = false
        var snappedCY = false
        val guides = mutableListOf<SmartGuideLine>()
        val worldCenter = SelectionGeometry.world(targetBounds.center, rawTransform)
        val canvasHalfW = canvasWidth / 2f
        val canvasHalfH = canvasHeight / 2f

        val rawBounds = SelectionBounds.enclosing(
            targetBounds.corners.map { SelectionGeometry.world(it, rawTransform) }
        )

        if (handle.x != 0) {
            val edgeX = if (handle.x < 0) rawBounds.left else rawBounds.right
            if (abs(edgeX) <= centerTolX) {
                val desiredHalfW = if (handle.x < 0) worldCenter.x else -worldCenter.x
                if (desiredHalfW > 1f) {
                    val targetSx = ((2f * desiredHalfW) / targetBounds.width.coerceAtLeast(0.001f))
                        .coerceAtLeast(SelectionGeometry.MIN_SCALE)
                    if (handle.y != 0 && startTransform.isScaleLinked && sx > 0f) {
                        sy = (sy * (targetSx / sx)).coerceAtLeast(SelectionGeometry.MIN_SCALE)
                    }
                    sx = targetSx
                    snappedCX = true
                    guides.add(SmartGuideLine(GuideOrientation.VERTICAL, 0f, -canvasHalfH, canvasHalfH))
                }
            }
        }
        if (handle.y != 0) {
            val edgeY = if (handle.y < 0) rawBounds.top else rawBounds.bottom
            if (abs(edgeY) <= centerTolY) {
                val desiredHalfH = if (handle.y < 0) worldCenter.y else -worldCenter.y
                if (desiredHalfH > 1f) {
                    val targetSy = ((2f * desiredHalfH) / targetBounds.height.coerceAtLeast(0.001f))
                        .coerceAtLeast(SelectionGeometry.MIN_SCALE)
                    if (handle.x != 0 && startTransform.isScaleLinked && !snappedCX && sy > 0f) {
                        sx = (sx * (targetSy / sy)).coerceAtLeast(SelectionGeometry.MIN_SCALE)
                    }
                    sy = targetSy
                    snappedCY = true
                    guides.add(SmartGuideLine(GuideOrientation.HORIZONTAL, 0f, -canvasHalfW, canvasHalfW))
                }
            }
        }

        val offset = SelectionPoint(targetBounds.center.x * sx, targetBounds.center.y * sy)
            .rotated(rawTransform.rotationDegrees)
        val finalT = if (snappedCX || snappedCY) {
            rawTransform.copy(
                scaleX = sx,
                scaleY = sy,
                translationX = worldCenter.x - offset.x,
                translationY = worldCenter.y - offset.y
            )
        } else {
            rawTransform
        }
        return SnapResult(
            snappedTransform = finalT,
            activeGuides = guides,
            didSnapX = snappedCX,
            didSnapY = snappedCY,
            didSnapCenterX = snappedCX,
            didSnapCenterY = snappedCY
        )
    }
}
