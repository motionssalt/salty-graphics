package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.ui.editor.PanelPage
import kotlin.math.roundToInt

/** Shared wheel + exact entry, hosted in the panel rather than a separate window. */
@Composable
fun PanelNumberControl(
    label: String, value: Float, min: Float, max: Float, step: Float = 1f,
    unit: String = "", onPreview: (Float) -> Unit, onCommit: (Float) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var pending by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value) { pending = value }
    Column {
        RulerSlider(label, value, { pending = it; onPreview(it) }, min, max, step = step, unit = unit,
            onValueChangeFinished = { onCommit(pending) },
            onEditValue = { input = value.toString(); editing = true })
        if (editing) {
            val parsed = input.toFloatOrNull()?.takeIf { it.isFinite() && it in min..max }
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(input, { input = it }, label = { Text(label) }, singleLine = true,
                    isError = parsed == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag("entry_$label"))
                TextButton(onClick = { editing = false }) { Text("Cancel") }
                TextButton(enabled = parsed != null, onClick = {
                    parsed?.let { onCommit(it) }; editing = false
                }) { Text("Done") }
            }
        }
    }
}

/** ARGB has one alpha channel: editing alpha never also multiplies an opacity field. */
@Composable
fun PanelColorPage(
    title: String, color: Long, onPreview: (Long) -> Unit,
    onCommit: (Long) -> Unit, onBack: () -> Unit
) {
    var hex by remember(color) { mutableStateOf(String.format(java.util.Locale.US, "%08X", color)) }
    fun channel(shift: Int) = ((color ushr shift) and 255).toFloat()
    fun replace(shift: Int, value: Float): Long =
        (color and (255L shl shift).inv()) or (value.roundToInt().coerceIn(0,255).toLong() shl shift)
    PanelPage(title, onBack) {
        Box(Modifier.fillMaxWidth().height(36.dp)) {
            CheckerboardPattern(Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color(color.toInt())))
        }
        Button(onClick = { onCommit(color and 0x00FFFFFFL) }, modifier = Modifier.testTag("transparent_color")) {
            Text("Transparent")
        }
        listOf("Alpha" to 24, "Red" to 16, "Green" to 8, "Blue" to 0).forEach { (label, shift) ->
            PanelNumberControl(label, channel(shift), 0f, 255f,
                onPreview = { onPreview(replace(shift,it)) }, onCommit = { onCommit(replace(shift,it)) })
        }
        val clean = hex.removePrefix("#")
        val parsed = clean.takeIf { it.length == 8 && it.all { c -> c in "0123456789abcdefABCDEF" } }?.toLongOrNull(16)
        OutlinedTextField(hex, { hex = it }, label = { Text("Hex with alpha (AARRGGBB)") },
            singleLine = true, isError = parsed == null, modifier = Modifier.fillMaxWidth().testTag("color_hex"))
        Button(enabled = parsed != null, onClick = { parsed?.let(onCommit) }) { Text("Set hex") }
        Row {
            RecentColorsRegistry.recentColors.take(6).forEach { preset ->
                TextButton(onClick = { onCommit(preset) }, modifier = Modifier.weight(1f)) {
                    Box(Modifier.size(24.dp).background(Color(preset.toInt())))
                }
            }
        }
    }
}
