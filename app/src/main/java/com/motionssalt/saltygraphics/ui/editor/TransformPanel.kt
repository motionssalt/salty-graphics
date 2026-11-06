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
    onTransformCommit: (Transform2D) -> Unit = onTransformChange,
    onClose: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var currentTransform by remember(transform) { mutableStateOf(transform) }
    var isTrackpadMode by remember { mutableStateOf(transform.isTrackpadMode) }

    Surface(
        color = colors.surface,
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
                    color = colors.primary
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.onSurfaceVariant)
                }
            }

            // Scale Section (Linked / Unlinked Chain Toggle)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Scale", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    FilledIconToggleButton(
                        checked = currentTransform.isScaleLinked,
                        onCheckedChange = { linked ->
                            val updated = currentTransform.copy(isScaleLinked = linked)
                            currentTransform = updated
                            onTransformChange(updated)
                            onTransformCommit(updated)
                        },
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            containerColor = colors.surfaceVariant,
                            checkedContainerColor = colors.primary,
                            contentColor = colors.onSurfaceVariant,
                            checkedContentColor = colors.background
                        )
                    ) {
                        Icon(
                            if (currentTransform.isScaleLinked) Icons.Default.Link else Icons.Default.LinkOff,
                            contentDescription = "Link Scale X/Y"
                        )
                    }
                }

                if (currentTransform.isScaleLinked) {
                    val uniformScale = (currentTransform.scaleX + currentTransform.scaleY) / 2f
                    Text("Uniform Scale: ${(uniformScale * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Slider(
                        value = uniformScale,
                        onValueChange = {
                            val updated = currentTransform.setScaleUniform(it)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = { onTransformCommit(currentTransform) },
                        valueRange = 0.1f..5.0f,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )
                } else {
                    Text("Scale X: ${(currentTransform.scaleX * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Slider(
                        value = currentTransform.scaleX,
                        onValueChange = {
                            val updated = currentTransform.copy(scaleX = it)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = { onTransformCommit(currentTransform) },
                        valueRange = 0.1f..5.0f,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )
                    Text("Scale Y: ${(currentTransform.scaleY * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Slider(
                        value = currentTransform.scaleY,
                        onValueChange = {
                            val updated = currentTransform.copy(scaleY = it)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = { onTransformCommit(currentTransform) },
                        valueRange = 0.1f..5.0f,
                        colors = SliderDefaults.colors(thumbColor = colors.secondary, activeTrackColor = colors.secondary)
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
                    Text("Move", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    TextButton(onClick = {
                        val newMode = !isTrackpadMode
                        isTrackpadMode = newMode
                        val updated = currentTransform.copy(isTrackpadMode = newMode)
                        currentTransform = updated
                        onTransformChange(updated)
                        onTransformCommit(updated)
                    }) {
                        Text(if (isTrackpadMode) "Switch to Sliders" else "Switch to Trackpad", color = colors.primary)
                    }
                }

                if (isTrackpadMode) {
                    // Precision Trackpad with smooth in-flight dragging and commit on release
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.background)
                            .pointerInput(currentTransform.moveSensitivity) {
                                detectDragGestures(
                                    onDragEnd = { onTransformCommit(currentTransform) },
                                    onDragCancel = { onTransformCommit(currentTransform) },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val updated = currentTransform.translateBy(dragAmount.x, dragAmount.y)
                                        currentTransform = updated
                                        onTransformChange(updated)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = colors.onSurfaceVariant)
                            Text(
                                "Drag here for precision translation",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                            Text(
                                "X: ${currentTransform.translationX.toInt()} px | Y: ${currentTransform.translationY.toInt()} px",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Trackpad Sensitivity: ${(currentTransform.moveSensitivity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    Slider(
                        value = currentTransform.moveSensitivity,
                        onValueChange = {
                            val updated = currentTransform.copy(moveSensitivity = it)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = { onTransformCommit(currentTransform) },
                        valueRange = 0.1f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )
                } else {
                    Text("Move X: ${currentTransform.translationX.toInt()} px", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Slider(
                        value = currentTransform.translationX,
                        onValueChange = {
                            val updated = currentTransform.copy(translationX = it)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = { onTransformCommit(currentTransform) },
                        valueRange = -1000f..1000f,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )
                    Text("Move Y: ${currentTransform.translationY.toInt()} px", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Slider(
                        value = currentTransform.translationY,
                        onValueChange = {
                            val updated = currentTransform.copy(translationY = it)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = { onTransformCommit(currentTransform) },
                        valueRange = -1000f..1000f,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )
                }
            }

            // Rotate Section (around layer center on flat canvas)
            Column {
                Text(
                    "Rotate: ${currentTransform.rotationDegrees.toInt()}°",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface
                )
                Slider(
                    value = currentTransform.rotationDegrees,
                    onValueChange = {
                        val updated = currentTransform.copy(rotationDegrees = it)
                        currentTransform = updated
                        onTransformChange(updated)
                    },
                    onValueChangeFinished = { onTransformCommit(currentTransform) },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(thumbColor = colors.secondary, activeTrackColor = colors.secondary)
                )
            }
        }
    }
}
