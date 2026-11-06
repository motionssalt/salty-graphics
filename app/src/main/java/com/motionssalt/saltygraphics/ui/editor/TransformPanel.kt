package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.Transform2D
import com.motionssalt.saltygraphics.theme.*

@Composable
fun TransformPanel(
    transform: Transform2D,
    onTransformChange: (Transform2D) -> Unit,
    onClose: () -> Unit
) {
    var isTrackpadMode by remember { mutableStateOf(transform.isTrackpadMode) }

    Surface(
        color = SaltySurfaceDark,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "2D Transform",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SaltyCyan
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SaltyTextSecondary)
                }
            }

            // Scale Section (Linked / Unlinked Chain Toggle)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Scale", style = MaterialTheme.typography.bodyLarge, color = SaltyTextPrimary)
                    // Chain link toggle button
                    FilledIconToggleButton(
                        checked = transform.isScaleLinked,
                        onCheckedChange = { linked ->
                            // Re-linking does NOT reset values per Section 3.7
                            onTransformChange(transform.copy(isScaleLinked = linked))
                        },
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            containerColor = SaltySurfaceVariantDark,
                            checkedContainerColor = SaltyCyan,
                            contentColor = SaltyTextSecondary,
                            checkedContentColor = SaltyDarkBackground
                        )
                    ) {
                        Icon(
                            if (transform.isScaleLinked) Icons.Default.Link else Icons.Default.LinkOff,
                            contentDescription = "Link Scale X/Y"
                        )
                    }
                }

                if (transform.isScaleLinked) {
                    val uniformScale = (transform.scaleX + transform.scaleY) / 2f
                    Text("Uniform Scale: ${(uniformScale * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = SaltyTextSecondary)
                    Slider(
                        value = uniformScale,
                        onValueChange = { onTransformChange(transform.setScaleUniform(it)) },
                        valueRange = 0.1f..5.0f,
                        colors = SliderDefaults.colors(thumbColor = SaltyCyan, activeTrackColor = SaltyCyan)
                    )
                } else {
                    Text("Scale X: ${(transform.scaleX * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = SaltyTextSecondary)
                    Slider(
                        value = transform.scaleX,
                        onValueChange = { onTransformChange(transform.copy(scaleX = it)) },
                        valueRange = 0.1f..5.0f,
                        colors = SliderDefaults.colors(thumbColor = SaltyCyan, activeTrackColor = SaltyCyan)
                    )
                    Text("Scale Y: ${(transform.scaleY * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = SaltyTextSecondary)
                    Slider(
                        value = transform.scaleY,
                        onValueChange = { onTransformChange(transform.copy(scaleY = it)) },
                        valueRange = 0.1f..5.0f,
                        colors = SliderDefaults.colors(thumbColor = SaltyAmber, activeTrackColor = SaltyAmber)
                    )
                }
            }

            // Move Section: Trackpad + Sensitivity OR Sliders
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Move", style = MaterialTheme.typography.bodyLarge, color = SaltyTextPrimary)
                    TextButton(onClick = {
                        val newMode = !isTrackpadMode
                        isTrackpadMode = newMode
                        onTransformChange(transform.copy(isTrackpadMode = newMode))
                    }) {
                        Text(if (isTrackpadMode) "Switch to Sliders" else "Switch to Trackpad", color = SaltyCyan)
                    }
                }

                if (isTrackpadMode) {
                    // Precision Trackpad with drag gesture detection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SaltyDarkBackground)
                            .pointerInput(transform.moveSensitivity) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onTransformChange(
                                        transform.translateBy(dragAmount.x, dragAmount.y)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = SaltyTextTertiary)
                            Text(
                                "Drag here for precision translation",
                                style = MaterialTheme.typography.labelSmall,
                                color = SaltyTextTertiary
                            )
                            Text(
                                "X: ${transform.translationX.toInt()} px | Y: ${transform.translationY.toInt()} px",
                                style = MaterialTheme.typography.labelSmall,
                                color = SaltyCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Trackpad Sensitivity: ${(transform.moveSensitivity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = SaltyTextSecondary
                    )
                    Slider(
                        value = transform.moveSensitivity,
                        onValueChange = { onTransformChange(transform.copy(moveSensitivity = it)) },
                        valueRange = 0.1f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = SaltyCyan, activeTrackColor = SaltyCyan)
                    )
                } else {
                    Text("Move X: ${transform.translationX.toInt()} px", style = MaterialTheme.typography.labelSmall, color = SaltyTextSecondary)
                    Slider(
                        value = transform.translationX,
                        onValueChange = { onTransformChange(transform.copy(translationX = it)) },
                        valueRange = -1000f..1000f,
                        colors = SliderDefaults.colors(thumbColor = SaltyCyan, activeTrackColor = SaltyCyan)
                    )
                    Text("Move Y: ${transform.translationY.toInt()} px", style = MaterialTheme.typography.labelSmall, color = SaltyTextSecondary)
                    Slider(
                        value = transform.translationY,
                        onValueChange = { onTransformChange(transform.copy(translationY = it)) },
                        valueRange = -1000f..1000f,
                        colors = SliderDefaults.colors(thumbColor = SaltyCyan, activeTrackColor = SaltyCyan)
                    )
                }
            }

            // Rotate Section (around layer center on flat canvas)
            Column {
                Text(
                    "Rotate: ${transform.rotationDegrees.toInt()}°",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SaltyTextPrimary
                )
                Slider(
                    value = transform.rotationDegrees,
                    onValueChange = { onTransformChange(transform.copy(rotationDegrees = it)) },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(thumbColor = SaltyAmber, activeTrackColor = SaltyAmber)
                )
            }
        }
    }
}
