package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import com.motionssalt.saltygraphics.ui.controls.*

@Composable
fun ProjectSettingsPage(document: ProjectDocument, onChange: (ProjectDocument) -> Unit, onBack: () -> Unit) {
    val settings = document.editorSettings
    fun update(s: com.motionssalt.saltygraphics.engine.model.EditorSettings) = onChange(document.copy(editorSettings = s))
    PanelPage("Project settings", onBack) {
        OutlinedTextField(value = document.name, onValueChange = { onChange(document.copy(name = it)) },
            label = { Text("Project name") }, singleLine = true)
        ColorPickerRow("Canvas background", settings.backgroundColor,
            { update(settings.copy(backgroundColor = it)) }, allowTransparent = true)
        ToggleControl("Gridlines", settings.showGrid, { update(settings.copy(showGrid = it)) })
        RulerSlider("Grid size", settings.gridSize.toFloat(), { update(settings.copy(gridSize = it.toInt())) },
            1f, 1024f, 20f, 1f, "px")
        ColorPickerRow("Grid colour", settings.gridColor, { update(settings.copy(gridColor = it)) })
        ToggleControl("Snap", settings.snapEnabled, { update(settings.copy(snapEnabled = it)) })
        DropdownControl("Default preview quality", PreviewQuality.entries.map { it.name },
            settings.defaultPreviewQuality.name, { update(settings.copy(defaultPreviewQuality = PreviewQuality.valueOf(it))) })
        Text("Canvas: ${document.canvasSize.width} × ${document.canvasSize.height} px")
        val divisor = gcd(document.canvasSize.width, document.canvasSize.height)
        Text("Aspect ratio: ${document.canvasSize.width / divisor}:${document.canvasSize.height / divisor}")
    }
}

private fun gcd(a: Int, b: Int): Int = if (b == 0) a.coerceAtLeast(1) else gcd(b, a % b)
