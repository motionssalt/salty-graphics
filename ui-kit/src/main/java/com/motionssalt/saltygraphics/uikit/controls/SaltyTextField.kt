package com.motionssalt.saltygraphics.uikit.controls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

/**
 * Standard text field adhering strictly to SaltyTokens.
 */
@Composable
fun SaltyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = if (label.isNotEmpty()) { { Text(label) } } else null,
        placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = shapes.sm,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outline,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedLabelColor = colors.primary,
            unfocusedLabelColor = colors.textSecondary,
            errorBorderColor = colors.error
        )
    )
}
