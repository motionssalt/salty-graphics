package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSettingsDialog(
    initialData: TextLayerData,
    candidatePathLayers: List<Layer>,
    onSave: (TextLayerData) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var textData by remember { mutableStateOf(initialData) }
    var pathDropdownExpanded by remember { mutableStateOf(false) }

    PanelPage("Text", onBack = onDismiss) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = textData.text,
                    onValueChange = { textData = textData.copy(text = it) },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary)
                )

                Text("Font Size: ${textData.fontSize.toInt()} sp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = textData.fontSize,
                    onValueChange = { textData = textData.copy(fontSize = it) },
                    valueRange = 12f..160f,
                    colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                )

                Text("Letter Spacing: ${textData.letterSpacing} sp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = textData.letterSpacing,
                    onValueChange = { textData = textData.copy(letterSpacing = it) },
                    valueRange = -2f..20f
                )

                Text("Line Spacing: ${textData.lineSpacing}x", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = textData.lineSpacing,
                    onValueChange = { textData = textData.copy(lineSpacing = it) },
                    valueRange = 0.8f..2.5f
                )

                // Move Along Path (Section 3.11)
                HorizontalDivider(color = colors.outline)
                Text(
                    "Move Along Path",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.secondary
                )
                Text(
                    "Pick a shape or vector layer to follow its outline:",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = pathDropdownExpanded,
                    onExpandedChange = { pathDropdownExpanded = !pathDropdownExpanded }
                ) {
                    val selectedPathLayerName = candidatePathLayers.find { it.id == textData.followPathLayerId }?.name ?: "None (Flat text)"
                    OutlinedTextField(
                        value = selectedPathLayerName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pathDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.secondary)
                    )
                    ExposedDropdownMenu(
                        expanded = pathDropdownExpanded,
                        onDismissRequest = { pathDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Flat text)") },
                            onClick = {
                                textData = textData.copy(followPathLayerId = null)
                                pathDropdownExpanded = false
                            }
                        )
                        candidatePathLayers.forEach { layer ->
                            DropdownMenuItem(
                                text = { Text("${layer.name} (${layer.type.displayName})") },
                                onClick = {
                                    textData = textData.copy(followPathLayerId = layer.id)
                                    pathDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (textData.followPathLayerId != null) {
                    Text("Path Offset: ${(textData.pathOffsetPercent * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = textData.pathOffsetPercent,
                        onValueChange = { textData = textData.copy(pathOffsetPercent = it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = colors.secondary, activeTrackColor = colors.secondary)
                    )
                }
            }


            Button(
                onClick = { onSave(textData) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
            ) {
                Text("Apply")
            }

    }
}
