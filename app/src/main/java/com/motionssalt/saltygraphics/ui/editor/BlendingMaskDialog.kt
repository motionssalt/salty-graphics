package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.BlendMode
import com.motionssalt.saltygraphics.engine.model.MaskMode
import com.motionssalt.saltygraphics.theme.*

@Composable
fun BlendingDialog(
    currentBlendMode: BlendMode,
    currentOpacity: Float,
    onApply: (BlendMode, Float) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var blendMode by remember { mutableStateOf(currentBlendMode) }
    var opacity by remember { mutableStateOf(currentOpacity) }

    PanelPage("Blending", onBack = onDismiss) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Opacity: ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                )

                HorizontalDivider(color = colors.outline)
                Text("Blend Modes (All 16 Photoshop Modes):", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)

                BlendMode.entries.forEach { mode ->
                    val isSelected = blendMode == mode
                    Surface(
                        onClick = { blendMode = mode },
                        color = if (isSelected) colors.surfaceVariant else colors.background,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) colors.primary else colors.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }


            Button(
                onClick = { onApply(blendMode, opacity) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
            ) {
                Text("Apply")
            }

    }
}

@Composable
fun MaskDialog(
    currentMaskMode: MaskMode,
    onApply: (MaskMode) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var maskMode by remember { mutableStateOf(currentMaskMode) }

    PanelPage("Mask", onBack = onDismiss) {

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Configure how this layer masks underlying content:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

                MaskMode.entries.forEach { mode ->
                    val isSelected = maskMode == mode
                    Surface(
                        onClick = { maskMode = mode },
                        color = if (isSelected) colors.surfaceVariant else colors.background,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) colors.primary else colors.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }


            Button(
                onClick = { onApply(maskMode) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
            ) {
                Text("Apply")
            }

    }
}
