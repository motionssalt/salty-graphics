package com.motionssalt.saltygraphics.uikit.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

/**
 * Universal Color Picker embedded natively inside the bottom panel (never a dialog or popup).
 * Supports HSV 2D gradient, Hue rail, Alpha slider, Hex input, and quick Transparent option.
 */
@Composable
fun SaltyColorPicker(
    colorArgb: Long,
    onColorChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    supportsAlpha: Boolean = true,
    supportsTransparent: Boolean = true,
    onColorCommit: (Long) -> Unit = {},
    onColorCancel: () -> Unit = {}
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes

    val colorCallback by rememberUpdatedState(onColorChange)
    val commitCallback by rememberUpdatedState(onColorCommit)
    val cancelCallback by rememberUpdatedState(onColorCancel)
    var lastColor by remember { mutableLongStateOf(colorArgb) }
    var hexInput by remember(colorArgb) { mutableStateOf("%08X".format(colorArgb)) }
    val parsedHex = hexInput.removePrefix("#").takeIf { it.length == 6 || it.length == 8 }
        ?.toLongOrNull(16)?.let { if (hexInput.removePrefix("#").length == 6) it or 0xFF000000L else it }

    // Deconstruct ARGB
    val a = ((colorArgb shr 24) and 0xFF).toFloat() / 255f
    val r = ((colorArgb shr 16) and 0xFF).toFloat() / 255f
    val g = ((colorArgb shr 8) and 0xFF).toFloat() / 255f
    val b = (colorArgb and 0xFF).toFloat() / 255f

    val hsv = remember(colorArgb) {
        val outHsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (r * 255).toInt(),
            (g * 255).toInt(),
            (b * 255).toInt(),
            outHsv
        )
        outHsv
    }

    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var valueVal by remember { mutableFloatStateOf(hsv[2]) }
    var alpha by remember { mutableFloatStateOf(a) }

    LaunchedEffect(colorArgb) {
        hue = hsv[0]; sat = hsv[1]; valueVal = hsv[2]; alpha = a; lastColor = colorArgb
    }

    fun updateColor(h: Float, s: Float, v: Float, aF: Float) {
        val rgb = android.graphics.Color.HSVToColor((aF * 255).toInt(), floatArrayOf(h, s, v))
        lastColor = rgb.toLong() and 0xFFFFFFFFL
        colorCallback(lastColor)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.md)
    ) {
        // Quick transparent option & Swatch preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm)
            ) {
                // Current color swatch
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(shapes.sm)
                        .background(Color(colorArgb))
                        .border(1.dp, colors.outline, shapes.sm)
                )

                // Hex readout
                val hexStr = String.format("%08X", colorArgb)
                Text(
                    text = "#$hexStr",
                    style = SaltyTokens.typography.mono,
                    color = colors.textPrimary
                )
            }

            if (supportsTransparent) {
                TextButton(
                    onClick = { updateColor(hue, sat, valueVal, 0f); commitCallback(lastColor) }
                ) {
                    Text(
                        text = "Transparent",
                        style = SaltyTokens.typography.labelMedium,
                        color = colors.primary
                    )
                }
            }
        }

        SaltyTextField(value = hexInput, onValueChange = { hexInput = it }, label = "Hex with alpha",
            isError = parsedHex == null)
        TextButton(onClick = {
            parsedHex?.let { lastColor = it; colorCallback(it); commitCallback(it) }
        }, enabled = parsedHex != null) { Text("Apply color") }

        // 2D Saturation / Value Gradient Box
        val pureHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(shapes.md)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { commitCallback(lastColor) },
                        onDragCancel = { cancelCallback() }
                    ) { change, _ ->
                        change.consume()
                        val xF = (change.position.x / size.width).coerceIn(0f, 1f)
                        val yF = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                        sat = xF
                        valueVal = yF
                        updateColor(hue, sat, valueVal, alpha)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Horizontal white to pure color gradient
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, pureHueColor)
                    )
                )
                // Vertical transparent to black gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )

                // Thumb marker
                val thumbX = sat * size.width
                val thumbY = (1f - valueVal) * size.height
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(thumbX, thumbY)
                )
                drawCircle(
                    color = Color.Black,
                    radius = 6.dp.toPx(),
                    center = Offset(thumbX, thumbY)
                )
            }
        }

        // Hue Slider Rail
        val hueColors = remember {
            (0..360 step 30).map { hDeg ->
                Color(android.graphics.Color.HSVToColor(floatArrayOf(hDeg.toFloat(), 1f, 1f)))
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(shapes.pill)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { commitCallback(lastColor) },
                        onDragCancel = { cancelCallback() }
                    ) { change, _ ->
                        change.consume()
                        val f = (change.position.x / size.width).coerceIn(0f, 1f)
                        hue = f * 360f
                        updateColor(hue, sat, valueVal, alpha)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(brush = Brush.horizontalGradient(hueColors))
                val x = (hue / 360f) * size.width
                drawCircle(color = Color.White, radius = 7.dp.toPx(), center = Offset(x, size.height / 2f))
                drawCircle(color = Color.Black, radius = 5.dp.toPx(), center = Offset(x, size.height / 2f))
            }
        }

        // Alpha Slider Rail (if supported)
        if (supportsAlpha) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(shapes.pill)
                    .pointerInput(Unit) {
                        detectDragGestures(
                        onDragEnd = { commitCallback(lastColor) },
                        onDragCancel = { cancelCallback() }
                    ) { change, _ ->
                        change.consume()
                            val f = (change.position.x / size.width).coerceIn(0f, 1f)
                            alpha = f
                            updateColor(hue, sat, valueVal, alpha)
                        }
                    }
            ) {
                val fullColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, valueVal)))
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, fullColor)
                        )
                    )
                    val x = alpha * size.width
                    drawCircle(color = Color.White, radius = 7.dp.toPx(), center = Offset(x, size.height / 2f))
                    drawCircle(color = Color.Black, radius = 5.dp.toPx(), center = Offset(x, size.height / 2f))
                }
            }
        }
    }
}
