package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.motionssalt.saltygraphics.engine.model.*

/**
 * Unified Border & Shadow panel (T17).
 * Separate Stroke and Shadow sections usable across text, shapes, vectors, and media.
 */
@Composable
fun BorderShadowDialog(
    stroke: StrokeData,
    shadow: ShadowData,
    onStrokeChange: (StrokeData) -> Unit,
    onShadowChange: (ShadowData) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var currentStroke by remember(stroke) { mutableStateOf(stroke) }
    var currentShadow by remember(shadow) { mutableStateOf(shadow) }
    var showStrokeColorPicker by remember { mutableStateOf(false) }
    var showShadowColorPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.outline)),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Border & Shadow",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.onSurfaceVariant)
                        }
                    }

                    // --- SECTION 1: STROKE ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Stroke / Outline", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = currentStroke.isEnabled,
                                onCheckedChange = { en ->
                                    val updated = currentStroke.copy(isEnabled = en)
                                    currentStroke = updated
                                    onStrokeChange(updated)
                                }
                            )
                        }

                        if (currentStroke.isEnabled) {
                            // Stroke Color & Opacity
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Stroke Color", style = MaterialTheme.typography.bodySmall)
                                Button(
                                    onClick = { showStrokeColorPicker = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                                    border = ButtonDefaults.outlinedButtonBorder
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(currentStroke.color.toInt()))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(String.format("#%08X", currentStroke.effectiveArgb), fontSize = 11.sp, color = colors.onSurface)
                                }
                            }

                            RulerSlider(
                                label = "Stroke Width",
                                value = currentStroke.width,
                                onValueChange = { w ->
                                    val updated = currentStroke.copy(width = w.coerceAtLeast(0.5f))
                                    currentStroke = updated
                                    onStrokeChange(updated)
                                },
                                 minValue = 1f,
                                maxValue = 200f,
                                step = 1f,
                                unit = "px"
                            )

                            // Stroke Alignment (Inside, Center, Outside)
                            Text("Alignment", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceVariant),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StrokeAlignment.entries.forEach { align ->
                                    val isSelected = currentStroke.alignment == align
                                    TextButton(
                                        onClick = {
                                            val updated = currentStroke.copy(alignment = align)
                                            currentStroke = updated
                                            onStrokeChange(updated)
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            containerColor = if (isSelected) colors.primary else Color.Transparent,
                                            contentColor = if (isSelected) colors.onPrimary else colors.onSurface
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(align.displayName, fontSize = 11.sp)
                                    }
                                }
                            }

                            // Dashed toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Dashed Style", style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = currentStroke.isDashed,
                                    onCheckedChange = { dashed ->
                                        val updated = currentStroke.copy(isDashed = dashed)
                                        currentStroke = updated
                                        onStrokeChange(updated)
                                    }
                                )
                            }

                            if (currentStroke.isDashed) {
                                RulerSlider(
                                    label = "Dash Length",
                                    value = currentStroke.dashLength,
                                    onValueChange = { d ->
                                        val updated = currentStroke.copy(dashLength = d.coerceAtLeast(1f))
                                        currentStroke = updated
                                        onStrokeChange(updated)
                                    },
                                    minValue = 1f,
                                    maxValue = 100f,
                                    step = 1f,
                                    unit = "px"
                                )
                                RulerSlider(
                                    label = "Dash Gap",
                                    value = currentStroke.dashGap,
                                    onValueChange = { g ->
                                        val updated = currentStroke.copy(dashGap = g.coerceAtLeast(1f))
                                        currentStroke = updated
                                        onStrokeChange(updated)
                                    },
                                    minValue = 1f,
                                    maxValue = 100f,
                                    step = 1f,
                                    unit = "px"
                                )
                            }
                        }
                    }

                    // --- SECTION 2: SHADOW ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Drop Shadow", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = currentShadow.isEnabled,
                                onCheckedChange = { en ->
                                    val updated = currentShadow.copy(isEnabled = en)
                                    currentShadow = updated
                                    onShadowChange(updated)
                                }
                            )
                        }

                        if (currentShadow.isEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Shadow Color", style = MaterialTheme.typography.bodySmall)
                                Button(
                                    onClick = { showShadowColorPicker = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                                    border = ButtonDefaults.outlinedButtonBorder
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(currentShadow.color.toInt()))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(String.format("#%08X", currentShadow.effectiveArgb), fontSize = 11.sp, color = colors.onSurface)
                                }
                            }

                            RulerSlider(
                                label = "Light Angle",
                                value = currentShadow.lightAngleDegrees,
                                onValueChange = { a ->
                                    val updated = currentShadow.copy(lightAngleDegrees = a)
                                    currentShadow = updated
                                    onShadowChange(updated)
                                },
                                minValue = 0f,
                                maxValue = 360f,
                                step = 1f,
                                unit = "°"
                            )

                            RulerSlider(
                                label = "Distance",
                                value = currentShadow.distance,
                                onValueChange = { d ->
                                    val updated = currentShadow.copy(distance = d)
                                    currentShadow = updated
                                    onShadowChange(updated)
                                },
                                minValue = 0f,
                                maxValue = 200f,
                                step = 1f,
                                unit = "px"
                            )

                            RulerSlider(
                                label = "Blur Softness",
                                value = currentShadow.blurRadius,
                                onValueChange = { b ->
                                    val updated = currentShadow.copy(blurRadius = b)
                                    currentShadow = updated
                                    onShadowChange(updated)
                                },
                                minValue = 0f,
                                maxValue = 150f,
                                step = 1f,
                                unit = "px"
                            )

                            RulerSlider(
                                label = "Shadow Opacity",
                                value = currentShadow.opacity * 100f,
                                onValueChange = { op ->
                                    val updated = currentShadow.copy(opacity = (op / 100f).coerceIn(0f, 1f))
                                    currentShadow = updated
                                    onShadowChange(updated)
                                },
                                minValue = 0f,
                                maxValue = 100f,
                                step = 1f,
                                unit = "%"
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showStrokeColorPicker) {
        ColorPickerDialog(
            title = "Stroke Color",
            initialColor = currentStroke.color,
            allowTransparent = false,
            onApply = { newColor ->
                val updated = currentStroke.copy(color = newColor)
                currentStroke = updated
                onStrokeChange(updated)
                showStrokeColorPicker = false
            },
            onDismiss = { showStrokeColorPicker = false }
        )
    }

    if (showShadowColorPicker) {
        ColorPickerDialog(
            title = "Shadow Color",
            initialColor = currentShadow.color,
            allowTransparent = false,
            onApply = { newColor ->
                val updated = currentShadow.copy(color = newColor)
                currentShadow = updated
                onShadowChange(updated)
                showShadowColorPicker = false
            },
            onDismiss = { showShadowColorPicker = false }
        )
    }
}
