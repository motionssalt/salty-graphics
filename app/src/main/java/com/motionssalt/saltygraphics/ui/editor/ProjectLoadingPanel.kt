package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.file.ProjectLoadState

@Composable
internal fun ProjectLoadingPanel(state: ProjectLoadState, onRetry: ()->Unit, onSafeMode: ()->Unit, onBack: ()->Unit) {
    Box(Modifier.fillMaxSize().testTag("project_load_surface"),contentAlignment=Alignment.Center) {
        Column(Modifier.widthIn(max=420.dp).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(16.dp)) {
            when(state) {
                is ProjectLoadState.Error -> {
                    Text("Could not open project",style=MaterialTheme.typography.titleMedium)
                    Text(state.message)
                    Button(onClick=onRetry) {Text("Retry")}
                    OutlinedButton(onClick=onSafeMode) {Text("Open in safe mode")}
                    TextButton(onClick=onBack) {Text("Back")}
                }
                is ProjectLoadState.Loading -> {
                    LinearProgressIndicator(progress={state.step.toFloat()/state.total},modifier=Modifier.fillMaxWidth())
                    Text("Opening project: ${state.phase}")
                    Text("Step ${state.step} of ${state.total}",style=MaterialTheme.typography.labelMedium)
                    TextButton(onClick=onBack) {Text("Back")}
                }
                is ProjectLoadState.Loaded -> Text("Project ready")
            }
        }
    }
}
