package com.motionssalt.saltygraphics.ui.controls

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.motionssalt.saltygraphics.uikit.controls.SaltyCheckerboard
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorPicker
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorPickerPage
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorSwatchRow
import com.motionssalt.saltygraphics.uikit.controls.SaltyRecentColorsRegistry
import com.motionssalt.saltygraphics.uikit.overlays.SaltyEyedropperLoupeOverlay
import com.motionssalt.saltygraphics.uikit.structure.SaltyAnimatedSection
import com.motionssalt.saltygraphics.uikit.structure.SaltyCard
import com.motionssalt.saltygraphics.uikit.structure.SaltyColumn
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace

// Shared recent colors across sessions in memory
object RecentColorsRegistry {
    val recentColors get() = SaltyRecentColorsRegistry.recentColors
    fun addColor(color: Long) {
        if (color == 0L) return
        SaltyRecentColorsRegistry.recordColor(color)
    }
}

/**
 * Draws a checkerboard pattern representing transparency.
 */
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
    var expanded by remember { mutableStateOf(false) }

    SaltyColumn(modifier = modifier, spacing = SaltySpace.XS) {
        SaltyColorSwatchRow(
            label = label,
            colorArgb = color,
            onClick = { expanded = !expanded }
        )
        SaltyAnimatedSection(visible = expanded) {
            SaltyCard(padding = SaltySpace.SM, spacing = SaltySpace.SM) {
                SaltyColorPicker(
                    colorArgb = color,
                    onColorChange = onColorChange,
                    supportsAlpha = true,
                    supportsTransparent = allowTransparent,
                    applyLabel = "Apply",
                    presets = SaltyRecentColorsRegistry.recentColors,
                    onColorCommit = {
                        onColorChange(it)
                        RecentColorsRegistry.addColor(it)
                        expanded = false
                    },
                    onColorCancel = { expanded = false }
                )
            }
        }
    }
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
