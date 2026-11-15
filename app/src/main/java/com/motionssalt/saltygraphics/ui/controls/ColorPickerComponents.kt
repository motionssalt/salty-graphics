package com.motionssalt.saltygraphics.ui.controls

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.motionssalt.saltygraphics.uikit.overlays.SaltyEyedropperLoupeOverlay

object RecentColorsRegistry {
    val recentColors get() = SaltyRecentColorsRegistry.recentColors
    fun addColor(color: Long) {
        if (color == 0L) return
        SaltyRecentColorsRegistry.recordColor(color)
    }
}

@Composable
fun CheckerboardPattern(modifier: Modifier = Modifier, squareSize: Float = 16f) {
    SaltyCheckerboard(modifier = modifier, squareSizePx = squareSize)
}

@Composable
fun ColorPickerRow(
    label: String,
    color: Long,
    onColorChange: (Long) -> Unit,
    allowTransparent: Boolean = false,
    modifier: Modifier = Modifier
) {
    SaltyColorPickerRow(
        label = label,
        color = color,
        onColorChange = onColorChange,
        allowTransparent = allowTransparent,
        modifier = modifier
    )
}

@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Long,
    allowTransparent: Boolean = false,
    onApply: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    SaltyColorPickerPage(
        title = title,
        colorArgb = initialColor,
        onPreview = {},
        onCommit = {
            onApply(it)
            RecentColorsRegistry.addColor(it)
            onDismiss()
        },
        onBack = onDismiss,
        presets = SaltyRecentColorsRegistry.recentColors,
        supportsTransparent = allowTransparent,
        applyLabel = "Apply"
    )
}

@Composable
fun EyedropperLoupeOverlay(
    touchPosition: Offset,
    sampledColor: Long,
    magnificationBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    SaltyEyedropperLoupeOverlay(
        touchPosition = touchPosition,
        sampledColor = sampledColor,
        magnificationBitmap = magnificationBitmap,
        modifier = modifier
    )
}
