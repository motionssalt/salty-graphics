package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.ShapeData
import com.motionssalt.saltygraphics.engine.model.ShapeType
import com.motionssalt.saltygraphics.theme.*

@Composable
fun ShapeSettingsDialog(
    initialShape: ShapeData,
    onSave: (ShapeData) -> Unit,
    onDismiss: () -> Unit
) {
    var shape by remember { mutableStateOf(initialShape) }

    PanelPage("Shape", onBack = onDismiss) {

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Width & Height
                Text("Width: ${shape.width.toInt()} px", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = shape.width,
                    onValueChange = { shape = shape.copy(width = it) },
                    valueRange = 50f..1000f,
                    colors = SliderDefaults.colors(thumbColor = SaltyCyan, activeTrackColor = SaltyCyan)
                )

                Text("Height: ${shape.height.toInt()} px", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = shape.height,
                    onValueChange = { shape = shape.copy(height = it) },
                    valueRange = 50f..1000f,
                    colors = SliderDefaults.colors(thumbColor = SaltyCyan, activeTrackColor = SaltyCyan)
                )

                // Corner radius slider (Can go all the way until it becomes a circle/oval per Section 3.6)
                if (shape.type == ShapeType.ROUNDED_RECTANGLE) {
                    val maxRadius = minOf(shape.width, shape.height) / 2f
                    Text("Corner Radius: ${shape.cornerRadius.toInt()} px (Max: ${maxRadius.toInt()} px)", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = shape.cornerRadius.coerceIn(0f, maxRadius),
                        onValueChange = { shape = shape.copy(cornerRadius = it) },
                        valueRange = 0f..maxRadius,
                        colors = SliderDefaults.colors(thumbColor = SaltyAmber, activeTrackColor = SaltyAmber)
                    )
                }

                if (shape.type == ShapeType.POLYGON) {
                    Text("Sides: ${shape.sides}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = shape.sides.toFloat(),
                        onValueChange = { shape = shape.copy(sides = it.toInt()) },
                        valueRange = 3f..12f,
                        steps = 8
                    )
                }

                if (shape.type == ShapeType.STAR) {
                    Text("Star Points: ${shape.starPoints}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = shape.starPoints.toFloat(),
                        onValueChange = { shape = shape.copy(starPoints = it.toInt()) },
                        valueRange = 3f..16f,
                        steps = 12
                    )
                }
            }


            Button(
                onClick = { onSave(shape) },
                colors = ButtonDefaults.buttonColors(containerColor = SaltyCyan, contentColor = SaltyDarkBackground)
            ) {
                Text("Apply")
            }

    }
}
