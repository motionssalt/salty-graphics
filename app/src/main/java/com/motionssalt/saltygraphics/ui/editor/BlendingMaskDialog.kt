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
    var blendMode by remember { mutableStateOf(currentBlendMode) }
    var opacity by remember { mutableStateOf(currentOpacity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Layer Blending", fontWeight = FontWeight.Bold, color = SaltyCyan) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Opacity: ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = SaltyCyan, activeTrackColor = SaltyCyan)
                )

                HorizontalDivider(color = SaltyOutlineDark)
                Text("Blend Modes (All 16 Photoshop Modes):", style = MaterialTheme.typography.bodyMedium, color = SaltyTextSecondary)

                BlendMode.entries.forEach { mode ->
                    val isSelected = blendMode == mode
                    Surface(
                        onClick = { blendMode = mode },
                        color = if (isSelected) SaltySurfaceVariantDark else SaltyDarkBackground,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) SaltyCyan else SaltyTextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(blendMode, opacity) },
                colors = ButtonDefaults.buttonColors(containerColor = SaltyCyan, contentColor = SaltyDarkBackground)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SaltyTextSecondary)
            }
        },
        containerColor = SaltySurfaceDark
    )
}

@Composable
fun MaskDialog(
    currentMaskMode: MaskMode,
    onApply: (MaskMode) -> Unit,
    onDismiss: () -> Unit
) {
    var maskMode by remember { mutableStateOf(currentMaskMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Layer Masking", fontWeight = FontWeight.Bold, color = SaltyCyan) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Configure how this layer masks underlying content:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SaltyTextSecondary
                )

                MaskMode.entries.forEach { mode ->
                    val isSelected = maskMode == mode
                    Surface(
                        onClick = { maskMode = mode },
                        color = if (isSelected) SaltySurfaceVariantDark else SaltyDarkBackground,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) SaltyCyan else SaltyTextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(maskMode) },
                colors = ButtonDefaults.buttonColors(containerColor = SaltyCyan, contentColor = SaltyDarkBackground)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SaltyTextSecondary)
            }
        },
        containerColor = SaltySurfaceDark
    )
}
