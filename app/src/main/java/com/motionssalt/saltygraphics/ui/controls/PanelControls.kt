package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorPickerPage
import com.motionssalt.saltygraphics.uikit.controls.SaltyNumberControl
import com.motionssalt.saltygraphics.uikit.controls.SaltyRecentColorsRegistry

/** Shared wheel + exact entry, hosted in the panel rather than a separate window. */
@Composable
fun PanelNumberControl(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    step: Float = 1f,
    unit: String = "",
    onPreview: (Float) -> Unit,
    onCommit: (Float) -> Unit
) {
    SaltyNumberControl(
        label = label,
        value = value,
        min = min,
        max = max,
        onPreview = onPreview,
        onCommit = onCommit,
        step = step,
        unit = unit
    )
}

/** ARGB has one alpha channel: editing alpha never also multiplies an opacity field. */
@Composable
fun PanelColorPage(
    title: String,
    color: Long,
    onPreview: (Long) -> Unit,
    onCommit: (Long) -> Unit,
    onBack: () -> Unit
) {
    SaltyColorPickerPage(
        title = title,
        colorArgb = color,
        onPreview = onPreview,
        onCommit = onCommit,
        onBack = onBack,
        presets = SaltyRecentColorsRegistry.recentColors
    )
}
