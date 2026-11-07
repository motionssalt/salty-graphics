package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.engine.model.Transform2D
import com.motionssalt.saltygraphics.theme.*
import com.motionssalt.saltygraphics.ui.controls.RulerSlider
import com.motionssalt.saltygraphics.ui.controls.ValueEditorDialog

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

    var isTrackpadXEditorOpen by remember { mutableStateOf(false) }
    var isTrackpadYEditorOpen by remember { mutableStateOf(false) }

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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    RulerSlider(
                        label = "Scale",
                        value = uniformScale * 100f,
                        onValueChange = {
                            val updated = currentTransform.setScaleUniform(it / 100f)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = { onTransformCommit(currentTransform) },
                        minValue = 1f,
                        maxValue = 10000f,
                        defaultValue = 100f,
                        step = 1f,
                        unit = "%"
                    )
                } else {
                    RulerSlider(
                        label = "Scale X",
                        value = currentTransform.scaleX * 100f,
                        onValueChange = {
                            val updated = currentTransform.copy(scaleX = it / 100f)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = { onTransformCommit(currentTransform) },
                        minValue = 1f,
                        maxValue = 10000f,
                        defaultValue = 100f,
                        step = 1f,
                        unit = "%"
                    )
                    RulerSlider(
                        label = "Scale Y",
                        value = currentTransform.scaleY * 100f,
                        onValueChange = {
                            val updated = currentTransform.copy(scaleY = it / 100f)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = { onTransformCommit(currentTransform) },
                        minValue = 1f,
                        maxValue = 10000f,
                        defaultValue = 100f,
                        step = 1f,
                        unit = "%"
                    )
                }
            }

            // Move Section: Trackpad + Sensitivity OR Sliders
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = colors.onSurfaceVariant)
                            Text(
                                "Drag here for precision translation",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { isTrackpadXEditorOpen = true },
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.surfaceVariant,
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        text = "X: ${currentTransform.translationX.toInt()} px",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    onClick = { isTrackpadYEditorOpen = true },
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.surfaceVariant,
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        text = "Y: ${currentTransform.translationY.toInt()} px",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    RulerSlider(
                        label = "Sensitivity",
                        value = currentTransform.moveSensitivity * 100f,
                        onValueChange = {
                            val updated = currentTransform.copy(moveSensitivity = it / 100f)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = { onTransformCommit(currentTransform) },
                        minValue = 10f,
                        maxValue = 500f,
                        defaultValue = 100f,
                        step = 5f,
                        unit = "%"
                    )
                } else {
                    RulerSlider(
                        label = "Move X",
                        value = currentTransform.translationX,
                        onValueChange = {
                            val updated = currentTransform.copy(translationX = it)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = { onTransformCommit(currentTransform) },
                        minValue = -100000f,
                        maxValue = 100000f,
                        defaultValue = 0f,
                        step = 1f,
                        unit = "px"
                    )
                    RulerSlider(
                        label = "Move Y",
                        value = currentTransform.translationY,
                        onValueChange = {
                            val updated = currentTransform.copy(translationY = it)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = { onTransformCommit(currentTransform) },
                        minValue = -100000f,
                        maxValue = 100000f,
                        defaultValue = 0f,
                        step = 1f,
                        unit = "px"
                    )
                }
            }

            // Rotate Section (around layer center on flat canvas)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RulerSlider(
                    label = "Rotate",
                    value = currentTransform.rotationDegrees,
                    onValueChange = {
                        val updated = currentTransform.copy(rotationDegrees = it)
                        currentTransform = updated
                        onTransformChange(updated)
                    },
                    onValueChangeFinished = { onTransformCommit(currentTransform) },
                    minValue = -36000f,
                    maxValue = 36000f,
                    defaultValue = 0f,
                    step = 1f,
                    unit = "°"
                )
            }
        }
    }

    if (isTrackpadXEditorOpen) {
        ValueEditorDialog(
            label = "Move X",
            currentValue = currentTransform.translationX,
            minValue = -100000f,
            maxValue = 100000f,
            step = 1f,
            unit = "px",
            onValueApplied = {
                val updated = currentTransform.copy(translationX = it)
                currentTransform = updated
                onTransformChange(updated)
                onTransformCommit(updated)
            },
            onDismiss = { isTrackpadXEditorOpen = false }
        )
    }

    if (isTrackpadYEditorOpen) {
        ValueEditorDialog(
            label = "Move Y",
            currentValue = currentTransform.translationY,
            minValue = -100000f,
            maxValue = 100000f,
            step = 1f,
            unit = "px",
            onValueApplied = {
                val updated = currentTransform.copy(translationY = it)
                currentTransform = updated
                onTransformChange(updated)
                onTransformCommit(updated)
            },
            onDismiss = { isTrackpadYEditorOpen = false }
        )
    }
}
