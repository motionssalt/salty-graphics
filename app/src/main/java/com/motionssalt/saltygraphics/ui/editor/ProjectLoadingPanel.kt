package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.file.ProjectLoadState
import com.motionssalt.saltygraphics.uikit.structure.SaltyLoadingSurface

@Composable
internal fun ProjectLoadingPanel(
    state: ProjectLoadState,
    onRetry: () -> Unit,
    onSafeMode: () -> Unit,
    onBack: () -> Unit
) {
    SaltyLoadingSurface(
        state = state,
        onRetry = onRetry,
        onSafeMode = onSafeMode,
        onBack = onBack
    )
}
