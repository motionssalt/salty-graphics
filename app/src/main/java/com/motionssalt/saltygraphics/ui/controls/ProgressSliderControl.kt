package com.motionssalt.saltygraphics.ui.controls

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.theme.*

@Composable
fun ProgressSliderControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    minValue: Float = 0f,
    maxValue: Float = 1f,
    defaultValue: Float = minValue,
    step: Float = 0.01f,
    unit: String = "",
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    var isEditorOpen by remember { mutableStateOf(false) }

    val formattedValue = remember(value, step, unit) {
        val numStr = if (step >= 1f && value % 1f == 0f) {
            value.toInt().toString()
        } else if (unit == "%") {
            (value * 100).toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
        if (unit.isNotBlank()) "$numStr $unit" else numStr
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label Button (reset to default on tap)
        Surface(
            onClick = {
                onValueChange(defaultValue)
                try {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                } catch (_: Throwable) {}
            },
            shape = RoundedCornerShape(6.dp),
            color = Color.Transparent,
            modifier = Modifier.widthIn(min = 64.dp, max = 96.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = if (value == defaultValue) colors.onSurfaceVariant else colors.primary,
                maxLines = 1
            )
        }

        // Draggable Progress Slider
        Slider(
            value = value.coerceIn(minValue, maxValue),
            onValueChange = { onValueChange(it) },
            valueRange = minValue..maxValue,
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.surfaceVariant
            ),
            modifier = Modifier.weight(1f)
        )

        // Tappable Value Pill
        Surface(
            onClick = { isEditorOpen = true },
            shape = RoundedCornerShape(8.dp),
            color = colors.surfaceVariant,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.primary.copy(alpha = 0.4f))),
            modifier = Modifier.widthIn(min = 68.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formattedValue,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = colors.primary,
                    maxLines = 1
                )
            }
        }
    }

    if (isEditorOpen) {
        ValueEditorDialog(
            label = label,
            currentValue = value,
            minValue = minValue,
            maxValue = maxValue,
            step = step,
            unit = unit,
            onValueApplied = { onValueChange(it) },
            onDismiss = { isEditorOpen = false }
        )
    }
}
