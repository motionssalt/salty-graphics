package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.motionssalt.saltygraphics.engine.model.*
import kotlin.math.*

/**
 * Shared gradient selector in the app engine (T14).
 * Supports Linear, Radial, and Freeform gradients with color and opacity stops,
 * angle adjustment via RulerSlider and tap-to-edit, and live interactive canvas updates.
 */
@Composable
fun GradientSelectorDialog(
    gradient: GradientData,
    onGradientChange: (GradientData) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var currentGradient by remember(gradient) { mutableStateOf(gradient) }
    var selectedStopIndex by remember { mutableStateOf(0) }
    var showColorPickerForStop by remember { mutableStateOf(false) }

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
                            "Gradient Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.onSurfaceVariant)
                        }
                    }

                    // Gradient Type Tabs (Linear, Radial, Freeform)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceVariant),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        GradientType.entries.forEach { type ->
                            val isSelected = currentGradient.type == type
                            TextButton(
                                onClick = {
                                    val updated = currentGradient.copy(type = type)
                                    currentGradient = updated
                                    onGradientChange(updated)
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = if (isSelected) colors.primary else Color.Transparent,
                                    contentColor = if (isSelected) colors.onPrimary else colors.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(type.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    // Live Interactive Preview Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                    ) {
                        CheckerboardPattern(Modifier.fillMaxSize())

                        val brush = remember(currentGradient) {
                            when (currentGradient.type) {
                                GradientType.LINEAR -> {
                                    val rad = Math.toRadians(currentGradient.angleDegrees.toDouble())
                                    val stops = currentGradient.sortedStops().map {
                                        val c = it.effectiveArgb
                                        val a = ((c ushr 24) and 0xFF) / 255f
                                        val r = ((c ushr 16) and 0xFF) / 255f
                                        val g = ((c ushr 8) and 0xFF) / 255f
                                        val b = (c and 0xFF) / 255f
                                        it.position.coerceIn(0f, 1f) to Color(r, g, b, a)
                                    }
                                    Brush.linearGradient(
                                        colorStops = stops.toTypedArray(),
                                        start = Offset.Zero,
                                        end = Offset(400f * cos(rad).toFloat(), 400f * sin(rad).toFloat())
                                    )
                                }
                                GradientType.RADIAL -> {
                                    val stops = currentGradient.sortedStops().map {
                                        val c = it.effectiveArgb
                                        val a = ((c ushr 24) and 0xFF) / 255f
                                        val r = ((c ushr 16) and 0xFF) / 255f
                                        val g = ((c ushr 8) and 0xFF) / 255f
                                        val b = (c and 0xFF) / 255f
                                        it.position.coerceIn(0f, 1f) to Color(r, g, b, a)
                                    }
                                    Brush.radialGradient(
                                        colorStops = stops.toTypedArray(),
                                        radius = 200f
                                    )
                                }
                                GradientType.FREEFORM -> {
                                    val stops = currentGradient.freeformPoints.map {
                                        val c = it.effectiveArgb
                                        val a = ((c ushr 24) and 0xFF) / 255f
                                        val r = ((c ushr 16) and 0xFF) / 255f
                                        val g = ((c ushr 8) and 0xFF) / 255f
                                        val b = (c and 0xFF) / 255f
                                        Color(r, g, b, a)
                                    }
                                    Brush.horizontalGradient(stops)
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize().background(brush))
                    }

                    // Stops Editor (for Linear & Radial)
                    if (currentGradient.type != GradientType.FREEFORM) {
                        Text("Color Stops (${currentGradient.stops.size})", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)

                        // Interactive Stops Rail
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .pointerInput(currentGradient) {
                                    detectTapGestures { offset ->
                                        val clickPos = (offset.x / size.width).coerceIn(0f, 1f)
                                        // Check if clicked near an existing stop
                                        val clickedIndex = currentGradient.stops.indexOfFirst {
                                            abs(it.position - clickPos) < 0.08f
                                        }
                                        if (clickedIndex >= 0) {
                                            selectedStopIndex = clickedIndex
                                        } else {
                                            // Add stop at clicked position
                                            val newStop = GradientStop(position = clickPos, color = 0xFFFFFFFFL, opacity = 1f)
                                            val updated = currentGradient.copy(stops = currentGradient.stops + newStop)
                                            currentGradient = updated
                                            selectedStopIndex = updated.stops.size - 1
                                            onGradientChange(updated)
                                        }
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                drawLine(
                                    color = Color.Gray,
                                    start = Offset(16f, h / 2f),
                                    end = Offset(w - 16f, h / 2f),
                                    strokeWidth = 4f
                                )
                                currentGradient.stops.forEachIndexed { idx, stop ->
                                    val sx = 16f + stop.position * (w - 32f)
                                    val isSelected = idx == selectedStopIndex
                                    val c = stop.effectiveArgb
                                    val sc = Color(((c ushr 16) and 0xFF) / 255f, ((c ushr 8) and 0xFF) / 255f, (c and 0xFF) / 255f)

                                    drawCircle(color = if (isSelected) Color.White else Color.Black, radius = if (isSelected) 14f else 10f, center = Offset(sx, h / 2f))
                                    drawCircle(color = sc, radius = if (isSelected) 11f else 8f, center = Offset(sx, h / 2f))
                                }
                            }
                        }

                        // Selected Stop Controls (Color, Opacity, Position, Delete)
                        val activeStop = currentGradient.stops.getOrNull(selectedStopIndex)
                        if (activeStop != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceVariant)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(activeStop.color.toInt()))
                                            .border(2.dp, colors.primary, CircleShape)
                                            .clickable { showColorPickerForStop = true }
                                    )
                                    Text("Stop #${selectedStopIndex + 1} (${(activeStop.position * 100).roundToInt()}%)", style = MaterialTheme.typography.labelSmall)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { showColorPickerForStop = true }) {
                                        Text("Pick Color", fontSize = 11.sp)
                                    }
                                    if (currentGradient.stops.size > 2) {
                                        IconButton(onClick = {
                                            val updated = currentGradient.copy(
                                                stops = currentGradient.stops.filterIndexed { i, _ -> i != selectedStopIndex }
                                            )
                                            currentGradient = updated
                                            selectedStopIndex = maxOf(0, selectedStopIndex - 1)
                                            onGradientChange(updated)
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Stop", tint = colors.error)
                                        }
                                    }
                                }
                            }

                            // Ruler slider for stop position
                            RulerSlider(
                                label = "Stop Position",
                                value = activeStop.position * 100f,
                                onValueChange = { newPct ->
                                    val updatedStops = currentGradient.stops.toMutableList()
                                    updatedStops[selectedStopIndex] = activeStop.copy(position = (newPct / 100f).coerceIn(0f, 1f))
                                    val updated = currentGradient.copy(stops = updatedStops)
                                    currentGradient = updated
                                    onGradientChange(updated)
                                },
                                min = 0f,
                                max = 100f,
                                step = 1f,
                                unit = "%",
                                isInteger = true
                            )
                        }

                        // Angle control for Linear gradient
                        if (currentGradient.type == GradientType.LINEAR) {
                            RulerSlider(
                                label = "Angle",
                                value = currentGradient.angleDegrees,
                                onValueChange = { angle ->
                                    val updated = currentGradient.copy(angleDegrees = angle)
                                    currentGradient = updated
                                    onGradientChange(updated)
                                },
                                min = -360f,
                                max = 360f,
                                step = 1f,
                                unit = "°",
                                isInteger = true
                            )
                        }
                    } else {
                        // Freeform Mesh Points Editor
                        Text("Freeform Points (${currentGradient.freeformPoints.size})", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(currentGradient.freeformPoints) { idx, pt ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(pt.color.toInt()))
                                        .border(2.dp, colors.outline, CircleShape)
                                        .clickable {
                                            // Cycle point color or edit
                                            val nextColor = when (pt.color) {
                                                0xFFFF5252L -> 0xFFFFB300L
                                                0xFFFFB300L -> 0xFF00E5FFL
                                                0xFF00E5FFL -> 0xFF2979FFL
                                                else -> 0xFFFF5252L
                                            }
                                            val updatedPts = currentGradient.freeformPoints.toMutableList()
                                            updatedPts[idx] = pt.copy(color = nextColor)
                                            val updated = currentGradient.copy(freeformPoints = updatedPts)
                                            currentGradient = updated
                                            onGradientChange(updated)
                                        }
                                )
                            }
                        }
                    }

                    // Apply / Done button
                    Button(
                        onClick = {
                            onGradientChange(currentGradient)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text("Apply Gradient", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showColorPickerForStop) {
        val activeStop = currentGradient.stops.getOrNull(selectedStopIndex)
        if (activeStop != null) {
            ColorPickerDialog(
                title = "Stop Color",
                initialColor = activeStop.color,
                allowTransparent = true,
                onApply = { newColor ->
                    val updatedStops = currentGradient.stops.toMutableList()
                    updatedStops[selectedStopIndex] = activeStop.copy(color = newColor)
                    val updated = currentGradient.copy(stops = updatedStops)
                    currentGradient = updated
                    onGradientChange(updated)
                    showColorPickerForStop = false
                },
                onDismiss = { showColorPickerForStop = false }
            )
        }
    }
}
