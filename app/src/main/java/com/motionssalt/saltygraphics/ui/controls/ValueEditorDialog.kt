package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.motionssalt.saltygraphics.theme.*

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
        if (number == null) {
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
    // Format initial text
    val initialText = if (step >= 1f && currentValue % 1f == 0f) {
        currentValue.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", currentValue).trimEnd('0').trimEnd('.')
    }

    var textInput by remember { mutableStateOf(initialText) }
    val (isValid, errorMessage) = remember(textInput, minValue, maxValue, unit) {
        ValueValidation.validate(textInput, minValue, maxValue, unit)
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SaltySurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SaltyOutlineDark)),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SaltyCyan
                        )
                        val unitBadge = if (unit.isNotBlank()) " ($unit)" else ""
                        Text(
                            text = "Range: $minValue .. $maxValue$unitBadge",
                            style = MaterialTheme.typography.labelSmall,
                            color = SaltyTextTertiary
                        )
                    }

                    // Text Input Field
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        isError = !isValid,
                        supportingText = {
                            if (!isValid && errorMessage != null) {
                                Text(errorMessage, color = SaltyError, style = MaterialTheme.typography.bodySmall)
                            } else {
                                val currentFormatted = if (unit.isNotBlank()) "$textInput $unit" else textInput
                                Text("Current: $currentFormatted", color = SaltyTextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (minValue < 0) KeyboardType.Text else KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isValid) {
                                    val number = textInput.trim().toFloatOrNull()
                                    if (number != null) {
                                        onValueApplied(number)
                                        onDismiss()
                                    }
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaltyCyan,
                            unfocusedBorderColor = SaltyOutlineDark,
                            errorBorderColor = SaltyError,
                            focusedTextColor = SaltyTextPrimary,
                            unfocusedTextColor = SaltyTextPrimary,
                            cursorColor = SaltyCyan
                        )
                    )

                    // Action Buttons (OK disabled when input is invalid or out-of-range)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = SaltyTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val number = textInput.trim().toFloatOrNull()
                                if (number != null && isValid) {
                                    onValueApplied(number)
                                    onDismiss()
                                }
                            },
                            enabled = isValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SaltyCyan,
                                contentColor = SaltyDarkBackground,
                                disabledContainerColor = SaltySurfaceVariantDark,
                                disabledContentColor = SaltyTextTertiary
                            )
                        ) {
                            Text("OK", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
