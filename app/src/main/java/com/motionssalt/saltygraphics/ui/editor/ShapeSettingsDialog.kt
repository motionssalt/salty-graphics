package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.registry.FeatureChange
import com.motionssalt.saltygraphics.uikit.registry.ScaffoldFeaturePage

/** Historical name retained for callers; this is a fixed-panel scaffold page, never a dialog. */
@Composable
fun ShapeSettingsDialog(
    document: ProjectDocument,
    layerId: String,
    onCommit: (FeatureChange<*>) -> Unit,
    onDismiss: () -> Unit,
    onPreview: (FeatureChange<*>?) -> Unit = {}
) {
    ScaffoldFeaturePage(
        feature = ShapeSettingsFeature.catalog.getFeature("shape_settings")!!,
        pageId = "shape_geometry",
        document = document,
        layerId = layerId,
        onCommit = onCommit,
        onPreview = onPreview,
        onBack = onDismiss
    )
}
