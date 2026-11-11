package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.model.EditorSettings
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import com.motionssalt.saltygraphics.ui.controls.*

@Composable
fun ProjectSettingsPage(
    document: ProjectDocument,
    onUpdateSetting: (ActionIdentity, (EditorSettings) -> EditorSettings) -> Unit,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val settings = document.editorSettings
    PanelPage("Project settings", onBack) {
        OutlinedTextField(
            value = document.name,
            onValueChange = onNameChange,
            label = { Text("Project name") },
            singleLine = true
        )
        ColorPickerRow(
            label = "Canvas background",
            color = settings.backgroundColor,
            onColorChange = { color ->
                onUpdateSetting(ActionIdentity("Change", "Canvas", "color")) { it.copy(backgroundColor = color) }
            },
            allowTransparent = true
        )
        ToggleControl(
            label = "Gridlines",
            checked = settings.showGrid,
            onCheckedChange = { show ->
                onUpdateSetting(ActionIdentity(if (show) "Show" else "Hide", "Gridlines")) { it.copy(showGrid = show) }
            }
        )
        RulerSlider(
            label = "Grid size",
            value = settings.gridSize.toFloat(),
            onValueChange = { size ->
                val newSize = size.toInt()
                val verb = if (newSize >= settings.gridSize) "Increase" else "Decrease"
                onUpdateSetting(ActionIdentity(verb, "Grid", "size")) { it.copy(gridSize = newSize) }
            },
            minValue = 1f,
            maxValue = 1024f,
            defaultValue = 20f,
            step = 1f,
            unit = "px"
        )
        ToggleControl(
            label = "Square grid cells",
            checked = settings.isSquareGrid,
            onCheckedChange = { sq ->
                onUpdateSetting(ActionIdentity(if (sq) "Enable" else "Disable", "Square Grid")) { it.copy(isSquareGrid = sq) }
            }
        )
        ColorPickerRow(
            label = "Grid colour",
            color = settings.gridColor,
            onColorChange = { color ->
                onUpdateSetting(ActionIdentity("Change", "Grid", "color")) { it.copy(gridColor = color) }
            }
        )
        ToggleControl(
            label = "Snap to objects",
            checked = settings.snapEnabled,
            onCheckedChange = { snap ->
                onUpdateSetting(ActionIdentity(if (snap) "Enable" else "Disable", "Snap")) { it.copy(snapEnabled = snap) }
            }
        )
        ToggleControl(
            label = "Snap to grid lines",
            checked = settings.snapToGrid,
            onCheckedChange = { snapGrid ->
                onUpdateSetting(ActionIdentity(if (snapGrid) "Enable" else "Disable", "Snap to Grid")) { it.copy(snapToGrid = snapGrid) }
            }
        )
        if (settings.snapEnabled) {
            RulerSlider(
                label = "Snap tolerance",
                value = settings.snapTolerancePx,
                onValueChange = { tol ->
                    onUpdateSetting(ActionIdentity("Adjust", "Snap", "tolerance")) { it.copy(snapTolerancePx = tol) }
                },
                minValue = 2f,
                maxValue = 32f,
                defaultValue = 8f,
                step = 1f,
                unit = "px"
            )
        }
        val qualityOptions = listOf(PreviewQuality.QUARTER, PreviewQuality.HALF, PreviewQuality.TWO_THIRDS, PreviewQuality.FULL)
        DropdownControl(
            label = "Default preview quality",
            options = qualityOptions.map { it.displayName },
            selectedOption = settings.defaultPreviewQuality.displayName,
            onOptionSelected = { selectedName ->
                val chosen = qualityOptions.find { it.displayName == selectedName } ?: PreviewQuality.FULL
                onUpdateSetting(ActionIdentity("Change", "Preview Quality")) { it.copy(defaultPreviewQuality = chosen) }
            }
        )
        Text("Canvas: ${document.canvasSize.width} × ${document.canvasSize.height} px")
        val divisor = gcd(document.canvasSize.width, document.canvasSize.height)
        Text("Aspect ratio: ${document.canvasSize.width / divisor}:${document.canvasSize.height / divisor}")
    }
}

@Composable
fun ProjectSettingsPage(
    document: ProjectDocument,
    onChange: (ProjectDocument) -> Unit,
    onBack: () -> Unit
) {
    ProjectSettingsPage(
        document = document,
        onUpdateSetting = { identity, transform ->
            onChange(document.updateEditorSettings(transform))
        },
        onNameChange = { newName ->
            onChange(document.copy(name = newName))
        },
        onBack = onBack
    )
}

private fun gcd(a: Int, b: Int): Int = if (b == 0) a.coerceAtLeast(1) else gcd(b, a % b)
