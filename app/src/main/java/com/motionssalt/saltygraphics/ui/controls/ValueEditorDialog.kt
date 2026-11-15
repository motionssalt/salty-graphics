package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.uikit.controls.SaltyValueEntryStrip

object ValueValidation {
    /**
     * Validates numeric input against min and max bounds.
     * Returns Pair(isValid, errorMessage)
     */
    fun validate(input: String, min: Float, max: Float, unit: String = ""): Pair<Boolean, String?> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return Pair(false, "Value cannot be empty")
        }
        val number = trimmed.toFloatOrNull()
        if (number == null || !number.isFinite()) {
            return Pair(false, "Invalid number")
        }
        if (number < min || number > max) {
            val unitStr = if (unit.isNotBlank()) " $unit" else ""
            return Pair(false, "Allowed range: $min .. $max$unitStr")
        }
        return Pair(true, null)
    }
}

@Composable
fun ValueEditorDialog(
    label: String,
    currentValue: Float,
    minValue: Float,
    maxValue: Float,
    step: Float = 1f,
    unit: String = "",
    onValueApplied: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    SaltyValueEntryStrip(
        label = label,
        currentValue = currentValue,
        range = minValue..maxValue,
        unit = unit,
        onConfirm = {
            onValueApplied(it)
            onDismiss()
        },
        onDismiss = onDismiss
    )
}
