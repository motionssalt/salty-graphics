package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.structure.ScaffoldToolPicker

/** Binding-only facade; the registered library chooser owns navigation and layout. */
@Composable
fun VectorCreationPage(document: ProjectDocument, onChoose: (String) -> Unit, onBack: () -> Unit) {
    ScaffoldToolPicker(
        title = "Create vector",
        catalog = VectorCreationFeature.catalog,
        document = document,
        availableActions = VectorCreationFeature.availableActions,
        onChoose = { id -> VectorCreationFeature.actionLabel(id)?.let(onChoose) },
        onBack = onBack
    )
}
