package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.registry.FeatureChange
import com.motionssalt.saltygraphics.uikit.registry.ScaffoldFeaturePage

/** No feature-owned widgets, colors, dimensions or snapshots: only scaffold parameters. */
@Composable
fun ProjectSettingsPage(
    document: ProjectDocument,
    onCommit: (FeatureChange<*>) -> Unit,
    onBack: () -> Unit,
    onPreview: (FeatureChange<*>?) -> Unit = {}
) {
    ScaffoldFeaturePage(
        feature = ProjectSettingsFeature.catalog.getFeature("project_settings")!!,
        pageId = "project_settings_main",
        document = document,
        layerId = null,
        onCommit = onCommit,
        onPreview = onPreview,
        onBack = onBack
    )
}
