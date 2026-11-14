package com.motionssalt.saltygraphics.uikit.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens
import kotlin.math.roundToInt

/**
 * Standard checkerboard pattern indicating transparency behind swatches and alpha rails.
 */
@Composable
fun SaltyCheckerboard(modifier: Modifier = Modifier, squareSizePx: Float = 12f) {
    Canvas(modifier = modifier) {
        val cols = (size.width / squareSizePx).toInt() + 1
        val rows = (size.height / squareSizePx).toInt() + 1
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val color = if ((r + c) % 2 == 0) Color(0xFF252936) else Color(0xFF1B1E28)
                drawRect(
                    color = color,
                    topLeft = Offset(c * squareSizePx, r * squareSizePx),
                    size = Size(squareSizePx, squareSizePx)
                )
            }
        }
    }
}

/**
 * Universal Color Picker embedded natively inside the bottom panel (never a dialog or popup).
 * Supports HSV 2D gradient, Hue rail, Alpha slider, optional ARGB channel rulers, Hex input,
 * presets strip, and quick Transparent option.
 */
@Composable
fun SaltyColorPicker(
    colorArgb: Long,
    onColorChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    supportsAlpha: Boolean = true,
    supportsTransparent: Boolean = true,
    applyLabel: String = "Apply color",
    showChannelSliders: Boolean = false,
    presets: List<Long> = emptyList(),
    onColorCommit: (Long) -> Unit = {},
    onColorCancel: () -> Unit = {}
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes

    val colorCallback by rememberUpdatedState(onColorChange)
    val commitCallback by rememberUpdatedState(onColorCommit)
    val cancelCallback by rememberUpdatedState(onColorCancel)
    var lastColor by remember { mutableLongStateOf(colorArgb) }
    var hexInput by remember(colorArgb) { mutableStateOf(String.format(java.util.Locale.US, "%08X", colorArgb)) }
    val cleanHex = hexInput.removePrefix("#").trim()
    val parsedHex = cleanHex.takeIf { (it.length == 6 || it.length == 8) && it.all { c -> c in "0123456789abcdefABCDEF" } }
        ?.toLongOrNull(16)?.let { if (cleanHex.length == 6) it or 0xFF000000L else it }

    // Deconstruct ARGB
    val a = ((colorArgb ushr 24) and 0xFF).toFloat() / 255f
    val r = ((colorArgb ushr 16) and 0xFF).toFloat() / 255f
    val g = ((colorArgb ushr 8) and 0xFF).toFloat() / 255f
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
        val rgb = android.graphics.Color.HSVToColor((aF * 255).roundToInt().coerceIn(0, 255), floatArrayOf(h, s, v))
        lastColor = rgb.toLong() and 0xFFFFFFFFL
        colorCallback(lastColor)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.md)
    ) {
        // Swatch preview + Hex readout + Transparent quick button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(shapes.sm)
                        .border(1.dp, colors.outline, shapes.sm)
                ) {
                    SaltyCheckerboard(Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(colorArgb.toInt()))
                    )
                }

                val hexStr = String.format(java.util.Locale.US, "%08X", colorArgb)
                Text(
                    text = "#$hexStr",
                    style = SaltyTokens.typography.mono,
                    color = colors.textPrimary
                )
            }

            if (supportsTransparent) {
                TextButton(
                    onClick = {
                        lastColor = colorArgb and 0x00FFFFFFL
                        colorCallback(lastColor)
                        commitCallback(lastColor)
                    },
                    modifier = Modifier.testTag("transparent_color")
                ) {
                    Text(
                        text = "Transparent",
                        style = SaltyTokens.typography.labelMedium,
                        color = colors.primary
                    )
                }
            }
        }

        SaltyTextField(
            value = hexInput,
            onValueChange = { hexInput = it },
            label = "Hex with alpha",
            isError = parsedHex == null,
            modifier = Modifier.testTag("color_hex")
        )
        Button(
            onClick = {
                parsedHex?.let {
                    lastColor = it
                    colorCallback(it)
                    commitCallback(it)
                }
            },
            enabled = parsedHex != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            )
        ) { Text(applyLabel) }

        // 2D Saturation / Value Gradient Box
        val pureHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(shapes.md)
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        val xF = (pos.x / size.width).coerceIn(0f, 1f)
                        val yF = (1f - (pos.y / size.height)).coerceIn(0f, 1f)
                        sat = xF; valueVal = yF
                        updateColor(hue, sat, valueVal, alpha)
                        commitCallback(lastColor)
                    }
                }
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
                drawRect(brush = Brush.horizontalGradient(colors = listOf(Color.White, pureHueColor)))
                drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black)))

                val thumbX = sat * size.width
                val thumbY = (1f - valueVal) * size.height
                drawCircle(color = Color.White, radius = 8.dp.toPx(), center = Offset(thumbX, thumbY))
                drawCircle(color = Color.Black, radius = 6.dp.toPx(), center = Offset(thumbX, thumbY))
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
                    detectTapGestures { pos ->
                        hue = (pos.x / size.width).coerceIn(0f, 1f) * 360f
                        updateColor(hue, sat, valueVal, alpha)
                        commitCallback(lastColor)
                    }
                }
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

        // Alpha Slider Rail (with checkerboard backing)
        if (supportsAlpha) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(shapes.pill)
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            alpha = (pos.x / size.width).coerceIn(0f, 1f)
                            updateColor(hue, sat, valueVal, alpha)
                            commitCallback(lastColor)
                        }
                    }
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
                SaltyCheckerboard(Modifier.fillMaxSize())
                val fullColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, valueVal)))
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(brush = Brush.horizontalGradient(listOf(Color.Transparent, fullColor)))
                    val x = alpha * size.width
                    drawCircle(color = Color.White, radius = 7.dp.toPx(), center = Offset(x, size.height / 2f))
                    drawCircle(color = Color.Black, radius = 5.dp.toPx(), center = Offset(x, size.height / 2f))
                }
            }
        }

        // Optional ARGB channel sliders
        if (showChannelSliders) {
            fun channel(shift: Int) = ((colorArgb ushr shift) and 255).toFloat()
            fun replace(shift: Int, ch: Float): Long =
                (colorArgb and (255L shl shift).inv()) or (ch.roundToInt().coerceIn(0, 255).toLong() shl shift)
            val channels = buildList {
                if (supportsAlpha) add("Alpha" to 24)
                add("Red" to 16); add("Green" to 8); add("Blue" to 0)
            }
            channels.forEach { (chLabel, shift) ->
                SaltyRulerSlider(
                    value = channel(shift),
                    onValueChange = { ch -> colorCallback(replace(shift, ch)) },
                    onValueCommit = { ch -> commitCallback(replace(shift, ch)) },
                    onValueCancel = cancelCallback,
                    range = 0f..255f,
                    step = 1f,
                    label = chLabel,
                    defaultValue = if (shift == 24) 255f else 0f
                )
            }
        }

        // Optional recent/preset swatch row
        if (presets.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm)
            ) {
                presets.take(8).forEach { preset ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(shapes.pill)
                            .border(1.dp, colors.outline, shapes.pill)
                            .clickable {
                                lastColor = preset
                                colorCallback(preset)
                                commitCallback(preset)
                            }
                    ) {
                        SaltyCheckerboard(Modifier.fillMaxSize(), squareSizePx = 8f)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(preset.toInt()))
                        )
                    }
                }
            }
        }
    }
}
