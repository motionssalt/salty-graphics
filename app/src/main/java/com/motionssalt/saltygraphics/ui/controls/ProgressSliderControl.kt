package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.uikit.controls.SaltyProgressNumberControl

@Composable
fun ProgressSliderControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    minValue: Float = 0f,
    maxValue: Float = 1f,
    defaultValue: Float = minValue,
    step: Float = 0.01f,
    unit: String = "",
    modifier: Modifier = Modifier
) {
    SaltyProgressNumberControl(
        label = label,
        value = value,
        min = minValue,
        max = maxValue,
        onPreview = onValueChange,
        onCommit = onValueChange,
        modifier = modifier,
        step = step,
        unit = unit,
        defaultValue = defaultValue
    )
}
