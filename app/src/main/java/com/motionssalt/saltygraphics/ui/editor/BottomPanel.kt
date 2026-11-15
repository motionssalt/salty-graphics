package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.sublibs.bottompanel.SaltyBottomToolGridSublib

@Composable
fun BottomPanel(
    selectedLayer: Layer?, onAddMedia: () -> Unit, onAddText: () -> Unit,
    onAddAdjustment: () -> Unit, onAddShape: () -> Unit, onAddVector: () -> Unit,
    onEditLayer: () -> Unit, onOpenTransform: () -> Unit, onOpenEffects: () -> Unit,
    onOpenBlending: () -> Unit, onDelete: () -> Unit,
    onOpenFill: (() -> Unit)? = null,
    onOpenBorderShadow: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onRasterize: (() -> Unit)? = null,
    onOpenTemplateExample: (() -> Unit)? = null,
    groupBreadcrumbs: List<Pair<String, String>> = emptyList(),
    onPopGroup: (() -> Unit)? = null,
    onJumpToGroup: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    document: ProjectDocument = ProjectDocument(layers = listOfNotNull(selectedLayer))
) {
    val actions = buildMap<String, () -> Unit> {
        put("media", onAddMedia); put("text", onAddText); put("adjustment", onAddAdjustment)
        put("shape", onAddShape); put("vector", onAddVector); put("edit", onEditLayer)
        put("transform", onOpenTransform); put("effects", onOpenEffects)
        put("blending", onOpenBlending); put("delete", onDelete)
        onOpenFill?.let { put("fill", it) }
        onOpenBorderShadow?.let { put("border_shadow", it) }
        onDuplicate?.let { put("duplicate", it) }
        onRasterize?.let { put("rasterize", it) }
        onOpenTemplateExample?.let { put("template_example", it) }
    }
    SaltyBottomToolGridSublib(
        catalog = EditorToolFeatures.catalog,
        document = document,
        selectedLayerIds = setOfNotNull(selectedLayer?.id),
        availableActions = actions.keys,
        onToolClick = { actions[it]?.invoke() },
        groupBreadcrumbs = groupBreadcrumbs,
        onPopGroup = onPopGroup,
        onJumpToGroup = onJumpToGroup,
        modifier = modifier
    )
}
