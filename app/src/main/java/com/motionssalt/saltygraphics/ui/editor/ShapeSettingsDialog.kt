package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.ShapeData
import com.motionssalt.saltygraphics.engine.model.ShapeType
import com.motionssalt.saltygraphics.theme.*

@Composable
fun ShapeSettingsDialog(
    initialShape: ShapeData,
    onShapeChange: (ShapeData) -> Unit,
    onShapeCommit: (ShapeData) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var shape by remember(initialShape) { mutableStateOf(initialShape) }

    PanelPage("Shape", onBack = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Width
            Text("Width: ${shape.width.toInt()} px", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = shape.width,
                onValueChange = {
                    val updated = shape.copy(width = it)
                    shape = updated
                    onShapeChange(updated)
                },
                onValueChangeFinished = { onShapeCommit(shape) },
                valueRange = 20f..2000f,
                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
            )

            // Height
            Text("Height: ${shape.height.toInt()} px", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = shape.height,
                onValueChange = {
                    val updated = shape.copy(height = it)
                    shape = updated
                    onShapeChange(updated)
                },
                onValueChangeFinished = { onShapeCommit(shape) },
                valueRange = 20f..2000f,
                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
            )

            // Corner radius
            if (shape.type == ShapeType.ROUNDED_RECTANGLE) {
                val maxRadius = minOf(shape.width, shape.height) / 2f
                Text("Corner Radius: ${shape.cornerRadius.toInt()} px", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = shape.cornerRadius.coerceIn(0f, maxRadius),
                    onValueChange = {
                        val updated = shape.copy(cornerRadius = it)
                        shape = updated
                        onShapeChange(updated)
                    },
                    onValueChangeFinished = { onShapeCommit(shape) },
                    valueRange = 0f..maxRadius,
                    colors = SliderDefaults.colors(thumbColor = colors.secondary, activeTrackColor = colors.secondary)
                )
            }

            // Polygon Sides
            if (shape.type == ShapeType.POLYGON) {
                Text("Sides: ${shape.sides}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = shape.sides.toFloat(),
                    onValueChange = {
                        val updated = shape.copy(sides = it.toInt())
                        shape = updated
                        onShapeChange(updated)
                    },
                    onValueChangeFinished = { onShapeCommit(shape) },
                    valueRange = 3f..16f,
                    steps = 12,
                    colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                )
            }

            // Star Points
            if (shape.type == ShapeType.STAR) {
                Text("Star Points: ${shape.starPoints}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = shape.starPoints.toFloat(),
                    onValueChange = {
                        val updated = shape.copy(starPoints = it.toInt())
                        shape = updated
                        onShapeChange(updated)
                    },
                    onValueChangeFinished = { onShapeCommit(shape) },
                    valueRange = 3f..24f,
                    steps = 20,
                    colors = SliderDefaults.colors(thumbColor = colors.secondary, activeTrackColor = colors.secondary)
                )
            }
        }
    }
}
