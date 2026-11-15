package com.motionssalt.saltygraphics.ui.newproject

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.uikit.sublibs.newproject.SaltyNewProjectScreen
import kotlinx.coroutines.launch

@Composable
fun NewProjectScreen(
    repository: ProjectRepository,
    onNavigateBack: () -> Unit,
    onProjectCreated: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    SaltyNewProjectScreen(
        onNavigateBack = onNavigateBack,
        onCreateProject = { projectName, canvasSize ->
            coroutineScope.launch {
                val doc = repository.createProject(projectName, canvasSize)
                onProjectCreated(doc.id)
            }
        }
    )
}
