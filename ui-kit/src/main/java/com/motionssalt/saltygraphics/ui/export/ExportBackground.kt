package com.motionssalt.saltygraphics.ui.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff

/** Clears stale pixels then composites the document ARGB; checkerboard is editor-only. */
fun drawExportBackground(canvas: Canvas, argb: Long, flatten: Boolean) {
    canvas.drawColor(if (flatten) Color.WHITE else Color.TRANSPARENT, PorterDuff.Mode.SRC)
    canvas.drawColor(argb.toInt())
}
