package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import com.motionssalt.saltygraphics.uikit.registry.*

/** Application integration: feature declarations and property bindings only. No feature layout. */
object SaltyFeatureRegistrations {
    private fun tool(id: String, title: String, icon: IconToken, placement: FeaturePlacement,
        layerTypes: Set<LayerType>? = null, componentId: String = "editor.$id",
        overlays: Set<CanvasOverlayToken> = emptySet()) = FeatureSpec(
        id = id, title = title, icon = icon, placement = placement, layerTypes = layerTypes, overlays = overlays,
        pages = listOf(PanelPageSpec(id, title, listOf(SectionSpec("main", title,
            listOf(RowSpec(id, ControlSpec(id, title, ControlKind.CUSTOM, CustomBinding(componentId))))))))
    private val rasterizable = setOf(LayerType.TEXT, LayerType.VECTOR, LayerType.SHAPE)
    private val fillable = setOf(LayerType.TEXT, LayerType.SHAPE, LayerType.VECTOR, LayerType.MEDIA, LayerType.GROUP)
    fun registry(developerFeaturesEnabled: Boolean = false) = FeatureRegistry(listOf(
        tool("media", "Media", IconToken.MEDIA, FeaturePlacement.NO_SELECTION),
        tool("text", "Text", IconToken.TEXT, FeaturePlacement.NO_SELECTION),
        tool("adjustment", "Adjustment", IconToken.ADJUSTMENT, FeaturePlacement.NO_SELECTION),
        tool("shape", "Shape", IconToken.SHAPE, FeaturePlacement.NO_SELECTION, componentId = "editor.shape_picker"),
        tool("vector", "Vector", IconToken.VECTOR, FeaturePlacement.NO_SELECTION, componentId = "editor.vector_creation"),
        tool("edit", "Edit", IconToken.EDIT, FeaturePlacement.LAYER_SELECTED),
        tool("transform", "Transform", IconToken.TRANSFORM, FeaturePlacement.LAYER_SELECTED,
            overlays = setOf(CanvasOverlayToken.SELECTION, CanvasOverlayToken.SNAPPING)),
        tool("fill", "Fill", IconToken.FILL, FeaturePlacement.LAYER_SELECTED, fillable,
            "editor.fill", setOf(CanvasOverlayToken.GRADIENT)),
        tool("border_shadow", "Border & Shadow", IconToken.BORDER_SHADOW, FeaturePlacement.LAYER_SELECTED, fillable),
        tool("effects", "Effects", IconToken.EFFECTS, FeaturePlacement.LAYER_SELECTED, componentId = "effects.panel"),
        tool("blending", "Blending & Mask", IconToken.BLENDING, FeaturePlacement.LAYER_SELECTED),
        tool("duplicate", "Duplicate", IconToken.DUPLICATE, FeaturePlacement.LAYER_SELECTED),
        tool("rasterize", "Rasterize", IconToken.RASTERIZE, FeaturePlacement.LAYER_SELECTED, rasterizable),
        tool("delete", "Delete", IconToken.DELETE, FeaturePlacement.LAYER_SELECTED),
        tool("layers", "Layers", IconToken.LAYERS, FeaturePlacement.OPERATION, componentId = "layers.panel"),
        tool("history", "Operation history", IconToken.HISTORY, FeaturePlacement.OPERATION, componentId = "history.tree"),
        tool("view", "View mode", IconToken.VIEW, FeaturePlacement.OPERATION,
            componentId = "view.controls", overlays = setOf(CanvasOverlayToken.GRID)),
        tool("project_settings", "Project settings", IconToken.SETTINGS, FeaturePlacement.OPERATION,
            componentId = "project.settings"),
        FeatureSpec("developer_scaffold_sample", "Scaffold sample", IconToken.SAMPLE, FeaturePlacement.DEVELOPER,
            condition = { _, _ -> developerFeaturesEnabled }, pages = listOf(ScaffoldSample.page))
    ))
}

/** Hidden diagnostic registration: every generic control is data-bound and history-ready. */
object ScaffoldSample {
    private val grid = PropertyPath("sample.grid", { d: ProjectDocument, _ -> d.editorSettings.showGrid },
        { d, _, value -> d.updateEditorSettings { it.copy(showGrid = value) } })
    private val size = PropertyPath("sample.grid_size", { d: ProjectDocument, _ -> d.editorSettings.gridSize.toFloat() },
        { d, _, value -> d.updateEditorSettings { it.copy(gridSize = value.toInt().coerceIn(4, 200)) } })
    private val quality = PropertyPath("sample.preview", { d: ProjectDocument, _ -> d.editorSettings.defaultPreviewQuality.name },
        { d, _, value -> d.updateEditorSettings { it.copy(defaultPreviewQuality = PreviewQuality.valueOf(value)) } })
    private val name = PropertyPath("sample.project_name", { d: ProjectDocument, _ -> d.name },
        { d, _, value -> d.copy(name = value) })
    private val color = PropertyPath("sample.canvas_color", { d: ProjectDocument, _ -> d.editorSettings.backgroundColor },
        { d, _, value -> d.updateEditorSettings { it.copy(backgroundColor = value) } })
    val page = PanelPageSpec("sample", "Scaffold sample", listOf(SectionSpec("controls", "Every control kind", listOf(
        RowSpec("switch", ControlSpec("switch", "Grid enabled", ControlKind.SWITCH, SwitchBinding(grid), target = "Project")),
        RowSpec("ruler", ControlSpec("ruler", "Grid size", ControlKind.RULER, NumberBinding(size, 4f..200f, 1f, "px"), target = "Project")),
        RowSpec("progress", ControlSpec("progress", "Grid size progress", ControlKind.PROGRESS, NumberBinding(size, 4f..200f, 1f, "px"), target = "Project")),
        RowSpec("choice", ControlSpec("choice", "Preview quality", ControlKind.DROPDOWN,
            ChoiceBinding(quality, PreviewQuality.entries.map { it.name }), target = "Project")),
        RowSpec("chips", ControlSpec("chips", "Preview chips", ControlKind.SEGMENTED,
            ChoiceBinding(quality, PreviewQuality.entries.map { it.name }), target = "Project")),
        RowSpec("color", ControlSpec("color", "Canvas color", ControlKind.COLOR, ColorBinding(color), target = "Project")),
        RowSpec("text", ControlSpec("text", "Project name", ControlKind.TEXT, TextBinding(name), target = "Project")),
        RowSpec("trackpad", ControlSpec("trackpad", "Trackpad", ControlKind.TRACKPAD, CustomBinding("sample.trackpad"))),
        RowSpec("gradient", ControlSpec("gradient", "Gradient editor", ControlKind.GRADIENT_EDITOR, CustomBinding("sample.gradient"))),
        RowSpec("media", ControlSpec("media", "Media grid", ControlKind.MEDIA_GRID, CustomBinding("sample.media"))),
        RowSpec("layers", ControlSpec("layers", "Layer selector", ControlKind.LAYER_SELECTOR, CustomBinding("sample.layers")))
    ))))
}
