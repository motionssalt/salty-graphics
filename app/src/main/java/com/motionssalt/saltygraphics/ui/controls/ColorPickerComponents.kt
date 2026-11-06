package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.motionssalt.saltygraphics.theme.*

// Shared recent colors across sessions in memory
object RecentColorsRegistry {
    val recentColors = mutableStateListOf(
        0xFF00E5FFL, 0xFFFFB300L, 0xFF2979FFL, 0xFFFF5252L, 0xFFFFFFFFL, 0xFF000000L
    )
    fun addColor(color: Long) {
        if (color == 0L) return
        recentColors.remove(color)
        recentColors.add(0, color)
        if (recentColors.size > 8) {
            recentColors.removeAt(recentColors.size - 1)
        }
    }
}

/**
 * Draws a checkerboard pattern representing transparency.
 */
@Composable
fun CheckerboardPattern(modifier: Modifier = Modifier, squareSize: Float = 16f) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cols = (w / squareSize).toInt() + 1
        val rows = (h / squareSize).toInt() + 1

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val isDark = (r + c) % 2 == 0
                val color = if (isDark) Color(0xFF252936) else Color(0xFF1B1E28)
                drawRect(
                    color = color,
                    topLeft = Offset(c * squareSize, r * squareSize),
                    size = Size(squareSize, squareSize)
                )
            }
        }
    }
}

@Composable
fun ColorPickerRow(
    label: String,
    color: Long,
    onColorChange: (Long) -> Unit,
    allowTransparent: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isDialogOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = SaltyTextPrimary
        )

        // Swatch Pill
        Surface(
            onClick = { isDialogOpen = true },
            shape = RoundedCornerShape(8.dp),
            color = SaltySurfaceVariantDark,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SaltyOutlineDark)),
            modifier = Modifier.height(34.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, SaltyOutlineDark, RoundedCornerShape(6.dp))
                ) {
                    if (color == 0L || (color and 0xFF000000L) == 0L) {
                        CheckerboardPattern(modifier = Modifier.fillMaxSize(), squareSize = 8f)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(safeColor(color))
                        )
                    }
                }

                val hexString = if (color == 0L) "Transparent" else String.format(java.util.Locale.US, "#%06X", color and 0xFFFFFFL)
                Text(
                    text = hexString,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                    color = if (color == 0L) SaltyAmber else SaltyCyan
                )
            }
        }
    }

    if (isDialogOpen) {
        ColorPickerDialog(
            title = label,
            initialColor = color,
            allowTransparent = allowTransparent,
            onApply = {
                onColorChange(it)
                RecentColorsRegistry.addColor(it)
                isDialogOpen = false
            },
            onDismiss = { isDialogOpen = false }
        )
    }
}

@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Long,
    allowTransparent: Boolean = false,
    onApply: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val isInitiallyTransparent = initialColor == 0L || (initialColor and 0xFF000000L) == 0L
    var isTransparentSelected by remember { mutableStateOf(isInitiallyTransparent && allowTransparent) }

    // Color components: HSV + Alpha
    var hue by remember { mutableStateOf(180f) }
    var saturation by remember { mutableStateOf(1f) }
    var brightness by remember { mutableStateOf(1f) }
    var alpha by remember { mutableStateOf(1f) }

    // Init from initialColor
    LaunchedEffect(initialColor) {
        if (!isInitiallyTransparent) {
            val a = ((initialColor shr 24) and 0xFF).toFloat() / 255f
            val r = ((initialColor shr 16) and 0xFF).toFloat() / 255f
            val g = ((initialColor shr 8) and 0xFF).toFloat() / 255f
            val b = (initialColor and 0xFF).toFloat() / 255f
            alpha = if (a <= 0f) 1f else a

            val hsv = FloatArray(3)
            android.graphics.Color.RGBToHSV((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), hsv)
            hue = hsv[0]
            saturation = hsv[1]
            brightness = hsv[2]
        }
    }

    // Computed current ARGB Long
    val currentRgbInt = remember(hue, saturation, brightness) {
        android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
    }
    val currentArgbLong = remember(currentRgbInt, alpha, isTransparentSelected) {
        if (isTransparentSelected) {
            0x00000000L
        } else {
            val aInt = (alpha * 255).toInt().coerceIn(0, 255)
            val rgbInt = currentRgbInt and 0x00FFFFFF
            ((aInt.toLong() shl 24) or rgbInt.toLong()) and 0xFFFFFFFFL
        }
    }

    var hexInput by remember(currentArgbLong) {
        mutableStateOf(if (isTransparentSelected) "00000000" else String.format(java.util.Locale.US, "%08X", currentArgbLong))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
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
                    // Header with Preview Swatch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SaltyCyan
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, SaltyOutlineDark, RoundedCornerShape(8.dp))
                        ) {
                            if (isTransparentSelected) {
                                CheckerboardPattern(modifier = Modifier.fillMaxSize(), squareSize = 8f)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(safeColor(currentArgbLong))
                                )
                            }
                        }
                    }

                    // Optional Transparent Toggle Chip (R7 Canvas Background)
                    if (allowTransparent) {
                        Surface(
                            onClick = { isTransparentSelected = !isTransparentSelected },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTransparentSelected) SaltyCyan.copy(alpha = 0.2f) else SaltySurfaceVariantDark,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(if (isTransparentSelected) SaltyCyan else SaltyOutlineDark)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, SaltyOutlineDark, CircleShape)
                                ) {
                                    CheckerboardPattern(modifier = Modifier.fillMaxSize(), squareSize = 6f)
                                }
                                Text(
                                    "Transparent Background",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isTransparentSelected) SaltyCyan else SaltyTextPrimary,
                                    fontWeight = if (isTransparentSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (!isTransparentSelected) {
                        // 2D Saturation / Brightness Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, SaltyOutlineDark, RoundedCornerShape(10.dp))
                                .pointerInput(hue) {
                                    detectTapGestures { offset ->
                                        saturation = (offset.x / size.width).coerceIn(0f, 1f)
                                        brightness = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                                    }
                                }
                                .pointerInput(hue) {
                                    detectDragGestures { change, _ ->
                                        saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                                        brightness = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                                    }
                                }
                        ) {
                            val pureHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Horizontal saturation gradient (white to pure hue)
                                drawRect(
                                    brush = Brush.horizontalGradient(listOf(Color.White, pureHueColor))
                                )
                                // Vertical brightness gradient (transparent to black)
                                drawRect(
                                    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
                                )

                                // Current selection circle
                                val thumbX = saturation * size.width
                                val thumbY = (1f - brightness) * size.height
                                drawCircle(Color.White, radius = 9f, center = Offset(thumbX, thumbY))
                                drawCircle(Color.Black, radius = 9f, center = Offset(thumbX, thumbY), style = Stroke(2f))
                            }
                        }

                        // Hue Slider (Rainbow Gradient)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Hue (${hue.toInt()}°)", style = MaterialTheme.typography.labelSmall, color = SaltyTextSecondary)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, SaltyOutlineDark, RoundedCornerShape(6.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, _ ->
                                            hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                                        }
                                    }
                            ) {
                                val hueColors = listOf(
                                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                )
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawRect(brush = Brush.horizontalGradient(hueColors))
                                    val thumbX = (hue / 360f) * size.width
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(thumbX, 0f),
                                        end = Offset(thumbX, size.height),
                                        strokeWidth = 3f
                                    )
                                }
                            }
                        }

                        // Alpha Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Opacity (${(alpha * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = SaltyTextSecondary)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, SaltyOutlineDark, RoundedCornerShape(6.dp))
                                    .pointerInput(currentRgbInt) {
                                        detectTapGestures { offset ->
                                            alpha = (offset.x / size.width).coerceIn(0f, 1f)
                                        }
                                    }
                                    .pointerInput(currentRgbInt) {
                                        detectDragGestures { change, _ ->
                                            alpha = (change.position.x / size.width).coerceIn(0f, 1f)
                                        }
                                    }
                            ) {
                                CheckerboardPattern(modifier = Modifier.fillMaxSize(), squareSize = 8f)
                                val solidColor = Color(currentRgbInt)
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawRect(
                                        brush = Brush.horizontalGradient(listOf(Color.Transparent, solidColor))
                                    )
                                    val thumbX = alpha * size.width
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(thumbX, 0f),
                                        end = Offset(thumbX, size.height),
                                        strokeWidth = 3f
                                    )
                                }
                            }
                        }

                        // Hex Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = hexInput,
                                onValueChange = { input ->
                                    hexInput = input
                                    val clean = input.removePrefix("#").trim()
                                    if (clean.length == 6 || clean.length == 8) {
                                        try {
                                            val parsed = clean.toLong(16)
                                            val fullArgb = if (clean.length == 6) parsed or 0xFF000000L else parsed
                                            val a = ((fullArgb shr 24) and 0xFF).toFloat() / 255f
                                            val r = ((fullArgb shr 16) and 0xFF).toFloat() / 255f
                                            val g = ((fullArgb shr 8) and 0xFF).toFloat() / 255f
                                            val b = (fullArgb and 0xFF).toFloat() / 255f
                                            alpha = a
                                            val hsv = FloatArray(3)
                                            android.graphics.Color.RGBToHSV((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), hsv)
                                            hue = hsv[0]
                                            saturation = hsv[1]
                                            brightness = hsv[2]
                                        } catch (_: Throwable) {}
                                    }
                                },
                                label = { Text("HEX Code") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SaltyCyan,
                                    unfocusedBorderColor = SaltyOutlineDark
                                )
                            )
                        }

                        // Presets & Recent Colors
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Presets & Recent", style = MaterialTheme.typography.labelSmall, color = SaltyTextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val palette = RecentColorsRegistry.recentColors.take(6)
                                palette.forEach { preset ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(safeColor(preset))
                                            .border(1.dp, SaltyOutlineDark, CircleShape)
                                            .clickable {
                                                isTransparentSelected = false
                                                val a = ((preset shr 24) and 0xFF).toFloat() / 255f
                                                val r = ((preset shr 16) and 0xFF).toFloat() / 255f
                                                val g = ((preset shr 8) and 0xFF).toFloat() / 255f
                                                val b = (preset and 0xFF).toFloat() / 255f
                                                alpha = if (a <= 0f) 1f else a
                                                val hsv = FloatArray(3)
                                                android.graphics.Color.RGBToHSV((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), hsv)
                                                hue = hsv[0]
                                                saturation = hsv[1]
                                                brightness = hsv[2]
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // Dialog Actions
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
                                onApply(currentArgbLong)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SaltyCyan,
                                contentColor = SaltyDarkBackground
                            )
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
