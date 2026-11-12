package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.motionssalt.saltygraphics.engine.model.EditorSettings
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import com.motionssalt.saltygraphics.uikit.controls.DropdownOption
import com.motionssalt.saltygraphics.uikit.registry.*

/** Production settings are data and typed bindings only. All layout belongs to :ui-kit. */
object ProjectSettingsFeature {
    private val defaults = EditorSettings()
    private fun <T> setting(
        key: String, target: String, property: String,
        read: (EditorSettings) -> T,
        write: (EditorSettings, T) -> EditorSettings
    ) = PropertyBinding(DocumentPropertyPath(
        key = "editorSettings.$key", target = target, property = property,
        read = { doc, _ -> read(doc.editorSettings) },
        write = { live, _, value -> live.updateEditorSettings { write(it, value) } }
    ))

    val gridColor = FeatureControlDef(
        "grid_color", "Grid colour", BoundControlType.COLOR_PICKER,
        setting("gridColor", "Grid", "color", { it.gridColor }, { s, v -> s.copy(gridColor = v) }),
        defaultValue = defaults.gridColor
    )
    val gridSize = FeatureControlDef(
        "grid_size", "Grid size", BoundControlType.RULER_SLIDER,
        setting("gridSize", "Grid", "size", { it.gridSize.toFloat() }, { s, v -> s.copy(gridSize = v.toInt()) }),
        range = 1f..1024f, step = 1f, unit = "px", defaultValue = defaults.gridSize.toFloat()
    )
    val definition = FeatureDefinition(
        id = "project_settings", title = "Project settings", icon = Icons.Default.Settings,
        placement = ToolPlacement.ALWAYS, defaultPageId = "project_settings_main",
        pages = listOf(FeaturePageDef(
            id = "project_settings_main", title = "Project settings",
            sections = listOf(FeatureSectionDef("settings", controls = listOf(
                FeatureControlDef("project_name", "Project name", BoundControlType.TEXT_FIELD,
                    PropertyBinding(DocumentPropertyPath(
                        "name", "Project", "name", { doc, _ -> doc.name },
                        { live, _, value -> live.copy(name = value) }, verb = "Rename"
                    )), defaultValue = "Untitled Project"),
                FeatureControlDef("canvas_background", "Canvas background", BoundControlType.COLOR_PICKER,
                    setting("backgroundColor", "Canvas", "color", { it.backgroundColor }, { s, v -> s.copy(backgroundColor = v) }),
                    defaultValue = defaults.backgroundColor),
                FeatureControlDef("show_grid", "Gridlines", BoundControlType.SWITCH,
                    setting("showGrid", "Grid", "lines", { it.showGrid }, { s, v -> s.copy(showGrid = v) }),
                    defaultValue = defaults.showGrid),
                gridSize,
                FeatureControlDef("square_grid", "Square grid cells", BoundControlType.SWITCH,
                    setting("isSquareGrid", "Square Grid", "cells", { it.isSquareGrid }, { s, v -> s.copy(isSquareGrid = v) }),
                    defaultValue = defaults.isSquareGrid),
                gridColor,
                FeatureControlDef("snap_objects", "Snap to objects", BoundControlType.SWITCH,
                    setting("snapEnabled", "Snap", "objects", { it.snapEnabled }, { s, v -> s.copy(snapEnabled = v) }),
                    defaultValue = defaults.snapEnabled),
                FeatureControlDef("snap_grid", "Snap to grid lines", BoundControlType.SWITCH,
                    setting("snapToGrid", "Snap", "grid lines", { it.snapToGrid }, { s, v -> s.copy(snapToGrid = v) }),
                    defaultValue = defaults.snapToGrid),
                FeatureControlDef("snap_tolerance", "Snap tolerance", BoundControlType.RULER_SLIDER,
                    setting("snapTolerancePx", "Snap", "tolerance", { it.snapTolerancePx }, { s, v -> s.copy(snapTolerancePx = v) }),
                    range = 2f..32f, step = 1f, unit = "px", defaultValue = defaults.snapTolerancePx,
                    isVisible = { doc, _ -> doc.editorSettings.snapEnabled }),
                FeatureControlDef("preview_quality", "Default preview quality", BoundControlType.INLINE_DROPDOWN,
                    setting("defaultPreviewQuality", "Preview", "quality", { canonicalQuality(it.defaultPreviewQuality) }, { s, v -> s.copy(defaultPreviewQuality = v) }),
                    options = listOf(PreviewQuality.QUARTER, PreviewQuality.HALF, PreviewQuality.TWO_THIRDS, PreviewQuality.FULL)
                        .map { DropdownOption(it, it.displayName) }, defaultValue = canonicalQuality(defaults.defaultPreviewQuality))
            ))),
            readouts = listOf(
                FeatureReadoutDef("preview_dimensions") { doc, _ ->
                    val quality = doc.editorSettings.defaultPreviewQuality
                    "Preview render: ${(doc.canvasSize.width * quality.scaleFactor).toInt()} × ${(doc.canvasSize.height * quality.scaleFactor).toInt()} px (${quality.displayName})"
                },
                FeatureReadoutDef("canvas_dimensions") { doc, _ -> "Canvas: ${doc.canvasSize.width} × ${doc.canvasSize.height} px" },
                FeatureReadoutDef("aspect_ratio") { doc, _ ->
                    val divisor = gcd(doc.canvasSize.width, doc.canvasSize.height)
                    "Aspect ratio: ${doc.canvasSize.width / divisor}:${doc.canvasSize.height / divisor}"
                }
            )
        ))
    )
    // Validate at construction, without registering editor callbacks in a global singleton.
    val catalog = FeatureCatalog(requireTypedBindings = true).apply { register(definition) }
    private fun canonicalQuality(quality: PreviewQuality): PreviewQuality = when (quality) {
        PreviewQuality.LOW -> PreviewQuality.QUARTER
        PreviewQuality.MEDIUM -> PreviewQuality.HALF
        PreviewQuality.HIGH -> PreviewQuality.FULL
        PreviewQuality.THREE_QUARTERS -> PreviewQuality.TWO_THIRDS
        else -> quality
    }
    private fun gcd(a: Int, b: Int): Int = if (b == 0) a.coerceAtLeast(1) else gcd(b, a % b)
}
