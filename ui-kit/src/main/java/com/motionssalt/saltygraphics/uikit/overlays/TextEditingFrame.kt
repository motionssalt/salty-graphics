package com.motionssalt.saltygraphics.uikit.overlays

import com.motionssalt.saltygraphics.engine.model.SelectionBounds
import com.motionssalt.saltygraphics.engine.model.TextLayerData

/** Shared frame geometry for both drawing and hit testing; never measure one long text line. */
object TextEditingFrame {
    fun bounds(text: TextLayerData): SelectionBounds {
        val width = text.boxWidth.takeIf { it.isFinite() && it > 0f } ?: 1f
        val height = text.boxHeight.takeIf { it.isFinite() && it > 0f } ?: 1f
        return SelectionBounds(-width / 2f, -height / 2f, width / 2f, height / 2f)
    }
}
