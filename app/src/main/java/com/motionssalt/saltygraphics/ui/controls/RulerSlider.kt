package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.uikit.controls.SaltyNumberControl

@Composable
fun RulerSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    minValue: Float,
    maxValue: Float,
    defaultValue: Float = minValue,
    step: Float = 1f,
    unit: String = "",
    onValueChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onEditValue: (() -> Unit)? = null
) {
    SaltyNumberControl(
        label = label,
        value = value,
        min = minValue,
        max = maxValue,
        onPreview = onValueChange,
        onCommit = {
            onValueChange(it)
            onValueChangeFinished?.invoke()
        },
        modifier = modifier,
        step = step,
        unit = unit,
        defaultValue = defaultValue
    )
}
