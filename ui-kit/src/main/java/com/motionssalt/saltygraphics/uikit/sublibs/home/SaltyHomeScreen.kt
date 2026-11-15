package com.motionssalt.saltygraphics.uikit.sublibs.home

import android.graphics.Bitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.uikit.controls.SaltyTextField
import com.motionssalt.saltygraphics.uikit.structure.SaltyAdaptiveGrid
import com.motionssalt.saltygraphics.uikit.structure.SaltyBox
import com.motionssalt.saltygraphics.uikit.structure.SaltyConfirmDialog
import com.motionssalt.saltygraphics.uikit.structure.SaltyEmptyState
import com.motionssalt.saltygraphics.uikit.structure.SaltyProjectCard
import com.motionssalt.saltygraphics.uikit.structure.SaltyScreenScaffold
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextTone
import com.motionssalt.saltygraphics.uikit.structure.SaltyTopBarAction
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxSize
import com.motionssalt.saltygraphics.uikit.structure.saltyGridItems
import com.motionssalt.saltygraphics.uikit.structure.saltyPadding

data class SaltyHomeProjectItem(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val layerCount: Int,
    val modifiedAt: Long
)

@Composable
fun SaltyHomeScreen(
    projects: List<SaltyHomeProjectItem>,
    selectedProjectIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onOpenProject: (String) -> Unit,
    onNewProject: () -> Unit,
    onImportProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onExportSelected: (Set<String>) -> Unit,
    onDuplicateProject: (String) -> Unit,
    onDeleteProjects: (Set<String>) -> Unit,
    onRenameProject: (String, String) -> Unit,
    thumbnailLoader: @Composable (SaltyHomeProjectItem) -> Bitmap?
) {
    val isSelectionMode = selectedProjectIds.isNotEmpty()
    var projectToRename by remember { mutableStateOf<SaltyHomeProjectItem?>(null) }
    var renameText by remember { mutableStateOf("") }

    val topActions = buildList {
        if (isSelectionMode) {
            add(
                SaltyTopBarAction(
                    icon = Icons.Default.FileDownload,
                    contentDescription = "Export .sg",
                    tone = SaltyTextTone.ACCENT
                ) { onExportSelected(selectedProjectIds) }
            )
            if (selectedProjectIds.size == 1) {
                add(
                    SaltyTopBarAction(
                        icon = Icons.Default.Edit,
                        contentDescription = "Rename"
                    ) {
                        val id = selectedProjectIds.first()
                        val project = projects.find { it.id == id }
                        if (project != null) {
                            projectToRename = project
                            renameText = project.name
                        }
                    }
                )
                add(
                    SaltyTopBarAction(
                        icon = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate"
                    ) {
                        onDuplicateProject(selectedProjectIds.first())
                        onSelectionChange(emptySet())
                    }
                )
            }
            add(
                SaltyTopBarAction(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tone = SaltyTextTone.ERROR
                ) {
                    onDeleteProjects(selectedProjectIds)
                    onSelectionChange(emptySet())
                }
            )
            add(
                SaltyTopBarAction(
                    icon = Icons.Default.Close,
                    contentDescription = "Clear Selection"
                ) {
                    onSelectionChange(emptySet())
                }
            )
        } else {
            add(
                SaltyTopBarAction(
                    icon = Icons.Default.FileOpen,
                    contentDescription = "Import .sg Project",
                    tone = SaltyTextTone.ACCENT
                ) { onImportProjects() }
            )
            add(
                SaltyTopBarAction(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tone = SaltyTextTone.SECONDARY
                ) { onOpenSettings() }
            )
        }
    }

    SaltyScreenScaffold(
        title = if (isSelectionMode) "${selectedProjectIds.size} Selected" else "SALTY GRAPHICS",
        accentTitle = !isSelectionMode,
        actions = topActions,
        fabIcon = Icons.Default.Add,
        fabContentDescription = "New Project",
        onFabClick = onNewProject
    ) { innerPadding ->
        if (projects.isEmpty()) {
            SaltyBox(
                modifier = Modifier
                    .saltyFillMaxSize()
                    .saltyPadding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                SaltyEmptyState(
                    message = "No projects yet. Tap + to create a new canvas or import a .sg project.",
                    icon = Icons.Default.Palette,
                    actionText = "New Project",
                    onAction = onNewProject
                )
            }
        } else {
            SaltyAdaptiveGrid(
                modifier = Modifier
                    .saltyFillMaxSize()
                    .saltyPadding(innerPadding),
                spacing = SaltySpace.MD
            ) {
                saltyGridItems(projects, key = { it.id }) { project ->
                    val isSelected = selectedProjectIds.contains(project.id)
                    val thumbnailBitmap = thumbnailLoader(project)
                    SaltyProjectCard(
                        name = project.name,
                        width = project.width,
                        height = project.height,
                        layerCount = project.layerCount,
                        thumbnailBitmap = thumbnailBitmap,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                onSelectionChange(
                                    if (isSelected) selectedProjectIds - project.id
                                    else selectedProjectIds + project.id
                                )
                            } else {
                                onOpenProject(project.id)
                            }
                        },
                        onLongClick = {
                            onSelectionChange(
                                if (isSelected) selectedProjectIds - project.id
                                else selectedProjectIds + project.id
                            )
                        }
                    )
                }
            }
        }
    }

    val renaming = projectToRename
    if (renaming != null) {
        SaltyConfirmDialog(
            title = "Rename Project",
            confirmText = "Rename",
            dismissText = "Cancel",
            confirmEnabled = renameText.isNotBlank(),
            onConfirm = {
                onRenameProject(renaming.id, renameText.trim())
                projectToRename = null
                onSelectionChange(emptySet())
            },
            onDismiss = { projectToRename = null },
            bodyContent = {
                SaltyTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = "Project Name"
                )
            }
        )
    }
}
