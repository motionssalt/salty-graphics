package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.model.ShapeData
import com.motionssalt.saltygraphics.uikit.structure.ScaffoldToolPicker

/** Historical name only: registered choices render in the fixed bottom panel. */
@Composable
fun ShapePickerDialog(
    document: ProjectDocument,
    onSelectShape: (ShapeData) -> Unit,
    onDismiss: () -> Unit
) {
    ScaffoldToolPicker(
        title = "Choose a Shape",
        catalog = ShapePickerFeature.catalog,
        document = document,
        availableActions = ShapePickerFeature.availableActions,
        onChoose = { id ->
            ShapePickerFeature.shape(id)?.let { shape ->
                onSelectShape(shape)
                onDismiss()
            }
        },
        onBack = onDismiss
    )
}
