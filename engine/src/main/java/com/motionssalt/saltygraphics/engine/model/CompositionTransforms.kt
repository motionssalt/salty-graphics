package com.motionssalt.saltygraphics.engine.model

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class QuickTransformAction(val label: String, val shortLabel: String) {
    ALIGN_LEFT("Align Left", "Left"),
    ALIGN_H_CENTER("Align Horizontal Center", "H Center"),
    ALIGN_RIGHT("Align Right", "Right"),
    ALIGN_TOP("Align Top", "Top"),
    ALIGN_V_CENTER("Align Vertical Center", "V Center"),
    ALIGN_BOTTOM("Align Bottom", "Bottom"),
    FIT_TO_COMPOSITION("Fit to Composition", "Fit"),
    FILL_COMPOSITION("Fill Composition", "Fill"),
    STRETCH_TO_COMPOSITION("Stretch to Composition", "Stretch"),
    RESET_ACTUAL_SIZE("Reset to Actual Size", "100%")
}

/**
 * Pure composition-relative align, fit/fill/stretch, reset-size, and precision nudge transforms (V39 & V43).
 * Typed/tapped transforms are exact (never snapped).
 */
object CompositionTransforms {
    fun apply(
        layer: Layer,
        bounds: SelectionBounds,
        canvasSize: CanvasSize,
        action: QuickTransformAction
    ): Transform2D {
        val t = layer.transform
        if (layer.isLocked || layer.type == LayerType.ADJUSTMENT || !SelectionGeometry.valid(t)) {
            return t
        }
        val canvasW = canvasSize.width.toFloat().coerceAtLeast(1f)
        val canvasH = canvasSize.height.toFloat().coerceAtLeast(1f)
        val halfCW = canvasW / 2f
        val halfCH = canvasH / 2f

        val worldCorners = bounds.corners.map { SelectionGeometry.world(it, t) }
        val minX = worldCorners.minOf { it.x }
        val maxX = worldCorners.maxOf { it.x }
        val minY = worldCorners.minOf { it.y }
        val maxY = worldCorners.maxOf { it.y }
        val boxCenterX = (minX + maxX) / 2f
        val boxCenterY = (minY + maxY) / 2f

        return when (action) {
            QuickTransformAction.ALIGN_LEFT -> t.copy(translationX = t.translationX + (-halfCW - minX))
            QuickTransformAction.ALIGN_H_CENTER -> t.copy(translationX = t.translationX + (0f - boxCenterX))
            QuickTransformAction.ALIGN_RIGHT -> t.copy(translationX = t.translationX + (halfCW - maxX))
            QuickTransformAction.ALIGN_TOP -> t.copy(translationY = t.translationY + (-halfCH - minY))
            QuickTransformAction.ALIGN_V_CENTER -> t.copy(translationY = t.translationY + (0f - boxCenterY))
            QuickTransformAction.ALIGN_BOTTOM -> t.copy(translationY = t.translationY + (halfCH - maxY))
            QuickTransformAction.FIT_TO_COMPOSITION, QuickTransformAction.FILL_COMPOSITION -> {
                val rad = Math.toRadians(t.rotationDegrees.toDouble())
                val absCos = abs(cos(rad)).toFloat()
                val absSin = abs(sin(rad)).toFloat()
                val bw = bounds.width.coerceAtLeast(0.001f)
                val bh = bounds.height.coerceAtLeast(0.001f)
                val rotatedW1 = (bw * absCos + bh * absSin).coerceAtLeast(0.001f)
                val rotatedH1 = (bw * absSin + bh * absCos).coerceAtLeast(0.001f)
                val scale = if (action == QuickTransformAction.FIT_TO_COMPOSITION) {
                    min(canvasW / rotatedW1, canvasH / rotatedH1)
                } else {
                    max(canvasW / rotatedW1, canvasH / rotatedH1)
                }.coerceAtLeast(SelectionGeometry.MIN_SCALE)
                val centerOffset = SelectionPoint(bounds.center.x * scale, bounds.center.y * scale)
                    .rotated(t.rotationDegrees)
                t.copy(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = -centerOffset.x,
                    translationY = -centerOffset.y
                )
            }
            QuickTransformAction.STRETCH_TO_COMPOSITION -> {
                val rad = Math.toRadians(t.rotationDegrees.toDouble())
                val bw = bounds.width.coerceAtLeast(0.001f)
                val bh = bounds.height.coerceAtLeast(0.001f)
                val quarterTurn = abs(sin(rad)) > abs(cos(rad))
                val sx = ((if (quarterTurn) canvasH else canvasW) / bw).coerceAtLeast(SelectionGeometry.MIN_SCALE)
                val sy = ((if (quarterTurn) canvasW else canvasH) / bh).coerceAtLeast(SelectionGeometry.MIN_SCALE)
                val centerOffset = SelectionPoint(bounds.center.x * sx, bounds.center.y * sy)
                    .rotated(t.rotationDegrees)
                t.copy(
                    scaleX = sx,
                    scaleY = sy,
                    translationX = -centerOffset.x,
                    translationY = -centerOffset.y
                )
            }
            QuickTransformAction.RESET_ACTUAL_SIZE -> {
                val visualCenter = SelectionGeometry.world(bounds.center, t)
                val offsetAtOne = SelectionPoint(bounds.center.x, bounds.center.y).rotated(t.rotationDegrees)
                t.copy(
                    scaleX = 1f,
                    scaleY = 1f,
                    translationX = visualCenter.x - offsetAtOne.x,
                    translationY = visualCenter.y - offsetAtOne.y
                )
            }
        }
    }

    fun applyToDocument(
        doc: ProjectDocument,
        targetIds: Set<String>,
        boundsProvider: (Layer) -> SelectionBounds,
        action: QuickTransformAction
    ): ProjectDocument {
        var current = doc
        for (id in targetIds) {
            val layer = current.findLayerById(id) ?: continue
            if (layer.isLocked || layer.type == LayerType.ADJUSTMENT) continue
            val updatedTransform = apply(layer, boundsProvider(layer), current.canvasSize, action)
            current = current.updateLayer(id) { it.copy(transform = updatedTransform) }
        }
        return current
    }

    fun nudgeDocument(
        doc: ProjectDocument,
        targetIds: Set<String>,
        dx: Float,
        dy: Float
    ): ProjectDocument {
        if (!dx.isFinite() || !dy.isFinite() || (dx == 0f && dy == 0f)) return doc
        var current = doc
        for (id in targetIds) {
            val layer = current.findLayerById(id) ?: continue
            if (layer.isLocked || layer.type == LayerType.ADJUSTMENT) continue
            val next = layer.transform.copy(
                translationX = layer.transform.translationX + dx,
                translationY = layer.transform.translationY + dy
            )
            if (SelectionGeometry.valid(next)) {
                current = current.updateLayer(id) { it.copy(transform = next) }
            }
        }
        return current
    }
}
