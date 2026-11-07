package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.ShapeData
import com.motionssalt.saltygraphics.engine.model.ShapeType
import com.motionssalt.saltygraphics.theme.*
import com.motionssalt.saltygraphics.ui.controls.RulerSlider

@Composable
fun ShapeSettingsDialog(
    initialShape: ShapeData,
    onShapeChange: (ShapeData) -> Unit,
    onShapeCommit: (ShapeData) -> Unit,
    onDismiss: () -> Unit
) {
    var shape by remember(initialShape) { mutableStateOf(initialShape) }

    PanelPage("Shape", onBack = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Width
            RulerSlider(
                label = "Width",
                value = shape.width,
                onValueChange = {
                    val updated = shape.copy(width = it)
                    shape = updated
                    onShapeChange(updated)
                },
                onValueChangeFinished = { onShapeCommit(shape) },
                minValue = 1f,
                maxValue = 50000f,
                defaultValue = 200f,
                step = 1f,
                unit = "px"
            )

            // Height
            RulerSlider(
                label = "Height",
                value = shape.height,
                onValueChange = {
                    val updated = shape.copy(height = it)
                    shape = updated
                    onShapeChange(updated)
                },
                onValueChangeFinished = { onShapeCommit(shape) },
                minValue = 1f,
                maxValue = 50000f,
                defaultValue = 200f,
                step = 1f,
                unit = "px"
            )

            // Corner radius
            if (shape.type == ShapeType.ROUNDED_RECTANGLE) {
                RulerSlider(
                    label = "Corner Radius",
                    value = shape.cornerRadius,
                    onValueChange = {
                        val updated = shape.copy(cornerRadius = it)
                        shape = updated
                        onShapeChange(updated)
                    },
                    onValueChangeFinished = { onShapeCommit(shape) },
                    minValue = 0f,
                    maxValue = 50000f,
                    defaultValue = 16f,
                    step = 1f,
                    unit = "px"
                )
            }

            // Polygon Sides
            if (shape.type == ShapeType.POLYGON) {
                RulerSlider(
                    label = "Sides",
                    value = shape.sides.toFloat(),
                    onValueChange = {
                        val updated = shape.copy(sides = it.toInt().coerceAtLeast(3))
                        shape = updated
                        onShapeChange(updated)
                    },
                    onValueChangeFinished = { onShapeCommit(shape) },
                    minValue = 3f,
                    maxValue = 100f,
                    defaultValue = 5f,
                    step = 1f,
                    unit = ""
                )
            }

            // Star Points
            if (shape.type == ShapeType.STAR) {
                RulerSlider(
                    label = "Star Points",
                    value = shape.starPoints.toFloat(),
                    onValueChange = {
                        val updated = shape.copy(starPoints = it.toInt().coerceAtLeast(3))
                        shape = updated
                        onShapeChange(updated)
                    },
                    onValueChangeFinished = { onShapeCommit(shape) },
                    minValue = 3f,
                    maxValue = 100f,
                    defaultValue = 5f,
                    step = 1f,
                    unit = ""
                )
            }
        }
    }
}
