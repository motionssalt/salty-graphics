package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.registry.FeatureChange
import com.motionssalt.saltygraphics.uikit.registry.ScaffoldFeaturePage

/** Binding-only facade. Layout, scrolling, numeric entry and color navigation belong to :ui-kit. */
@Composable
fun BorderShadowPanelPage(
    document: ProjectDocument,
    layerId: String,
    onCommit: (FeatureChange<*>) -> Unit,
    onBack: () -> Unit,
    onPreview: (FeatureChange<*>?) -> Unit = {}
) {
    ScaffoldFeaturePage(
        feature = BorderShadowFeature.definition,
        pageId = "appearance",
        document = document,
        layerId = layerId,
        onCommit = onCommit,
        onPreview = onPreview,
        onBack = onBack
    )
}
