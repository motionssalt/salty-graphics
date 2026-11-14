package com.motionssalt.saltygraphics.uikit.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

/**
 * Shared validation logic for keyboard numeric entry (supports bounds and negative numbers).
 */
object ValueValidation {
    fun isValidNumericInput(raw: String, range: ClosedFloatingPointRange<Float>): Boolean {
        val parsed = raw.trim().toFloatOrNull() ?: return false
        return parsed.isFinite() && parsed in range
    }

    fun parseNumericInput(raw: String, range: ClosedFloatingPointRange<Float>): Float? {
        val parsed = raw.trim().toFloatOrNull() ?: return null
        return if (parsed.isFinite() && parsed in range) parsed else null
    }
}

/**
 * Compact numeric value entry strip docked directly above the keyboard.
 * Allowed exception per the standing UI rule: docked strip never floats across canvas.
 */
@Composable
fun SaltyValueEntryStrip(
    label: String,
    currentValue: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String = "",
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SaltyTokens.colors

    var text by remember(label, currentValue) {
        val cleanStr = if (currentValue % 1f == 0f) currentValue.toInt().toString() else currentValue.toString()
        mutableStateOf(cleanStr)
    }
    val focusRequester = remember { FocusRequester() }

    val parsed = ValueValidation.parseNumericInput(text, range)
    val isValid = parsed != null

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Throwable) {
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant)
            .padding(SaltyTokens.spacing.md)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = SaltyTokens.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (unit.isNotBlank()) "Range: ${range.start}..${range.endInclusive} $unit" else "Range: ${range.start}..${range.endInclusive}",
                    style = SaltyTokens.typography.labelSmall,
                    color = if (isValid) colors.textSecondary else colors.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (range.start < 0f) KeyboardType.Text else KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (parsed != null) onConfirm(parsed)
                        }
                    ),
                    isError = !isValid,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isValid) colors.primary else colors.error,
                        unfocusedBorderColor = if (isValid) colors.outline else colors.error,
                        errorBorderColor = colors.error,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = colors.textSecondary)
                }

                Button(
                    onClick = {
                        if (parsed != null) onConfirm(parsed)
                    },
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Text("OK")
                }
            }
        }
    }
}
