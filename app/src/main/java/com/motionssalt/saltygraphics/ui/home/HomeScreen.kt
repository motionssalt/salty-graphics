package com.motionssalt.saltygraphics.ui.home

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.data.ProjectSummary
import com.motionssalt.saltygraphics.render.ProjectThumbnailRenderer
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
fun HomeScreen(
    repository: ProjectRepository,
    onNavigateToNewProject: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val projects by repository.projects.collectAsState()

    var selectedProjectIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedProjectIds.isNotEmpty()
    var projectToRename by remember { mutableStateOf<ProjectSummary?>(null) }
    var renameText by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                var lastImportedId: String? = null
                var successCount = 0
                var errorMsg: String? = null
                for (uri in uris) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val importedDoc = repository.importProject(stream)
                            lastImportedId = importedDoc.id
                            successCount++
                        }
                    } catch (e: Exception) {
                        errorMsg = e.message ?: "Failed to import project"
                    }
                }
                if (errorMsg != null) {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
                val targetId = lastImportedId
                if (successCount == 1 && targetId != null) {
                    onNavigateToEditor(targetId)
                } else if (successCount > 1) {
                    Toast.makeText(context, "Imported $successCount projects successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var exportingProjectIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val exportSgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val targetIds = exportingProjectIds
        if (uri != null && targetIds.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        if (targetIds.size == 1) {
                            repository.exportProjectToStream(targetIds.first(), out)
                        } else {
                            val zipOut = ZipOutputStream(out)
                            for (id in targetIds) {
                                val proj = projects.find { it.id == id }
                                val entryName = "${(proj?.name ?: id).replace(" ", "_")}.sg"
                                zipOut.putNextEntry(ZipEntry(entryName))
                                val file = repository.getProjectFile(id)
                                if (file.exists()) {
                                    file.inputStream().use { it.copyTo(zipOut) }
                                }
                                zipOut.closeEntry()
                            }
                            zipOut.finish()
                        }
                    }
                    Toast.makeText(context, "Exported project(s) successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                exportingProjectIds = emptySet()
                selectedProjectIds = emptySet()
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            repository.loadProjects()
        } catch (_: Throwable) {
        }
    }

    val topActions = buildList {
        if (isSelectionMode) {
            add(
                SaltyTopBarAction(
                    icon = Icons.Default.FileDownload,
                    contentDescription = "Export .sg",
                    tone = SaltyTextTone.ACCENT
                ) {
                    exportingProjectIds = selectedProjectIds
                    if (selectedProjectIds.size == 1) {
                        val proj = projects.find { it.id == selectedProjectIds.first() }
                        val fileName = "${(proj?.name ?: "Project").replace(" ", "_")}.sg"
                        exportSgLauncher.launch(fileName)
                    } else {
                        exportSgLauncher.launch("Salty_Projects_${selectedProjectIds.size}.zip")
                    }
                }
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
                        coroutineScope.launch {
                            repository.duplicateProject(selectedProjectIds.first())
                            selectedProjectIds = emptySet()
                        }
                    }
                )
            }
            add(
                SaltyTopBarAction(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tone = SaltyTextTone.ERROR
                ) {
                    coroutineScope.launch {
                        repository.deleteProjects(selectedProjectIds)
                        selectedProjectIds = emptySet()
                    }
                }
            )
            add(
                SaltyTopBarAction(
                    icon = Icons.Default.Close,
                    contentDescription = "Clear Selection"
                ) {
                    selectedProjectIds = emptySet()
                }
            )
        } else {
            add(
                SaltyTopBarAction(
                    icon = Icons.Default.FileOpen,
                    contentDescription = "Import .sg Project",
                    tone = SaltyTextTone.ACCENT
                ) {
                    importLauncher.launch(arrayOf("*/*"))
                }
            )
            add(
                SaltyTopBarAction(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tone = SaltyTextTone.SECONDARY
                ) {
                    onNavigateToSettings()
                }
            )
        }
    }

    SaltyScreenScaffold(
        title = if (isSelectionMode) "${selectedProjectIds.size} Selected" else "SALTY GRAPHICS",
        accentTitle = !isSelectionMode,
        actions = topActions,
        fabIcon = Icons.Default.Add,
        fabContentDescription = "New Project",
        onFabClick = onNavigateToNewProject
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
                    onAction = onNavigateToNewProject
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
                    val thumbnailBitmap by produceState<Bitmap?>(
                        initialValue = ProjectThumbnailRenderer.getCached(project.id),
                        key1 = project.id,
                        key2 = project.modifiedAt
                    ) {
                        value = withContext(Dispatchers.IO) {
                            ProjectThumbnailRenderer.getOrLoad(context, project.id)
                        }
                    }
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
                                selectedProjectIds = if (isSelected) {
                                    selectedProjectIds - project.id
                                } else {
                                    selectedProjectIds + project.id
                                }
                            } else {
                                onNavigateToEditor(project.id)
                            }
                        },
                        onLongClick = {
                            selectedProjectIds = if (isSelected) {
                                selectedProjectIds - project.id
                            } else {
                                selectedProjectIds + project.id
                            }
                        }
                    )
                }
            }
        }
    }

    if (projectToRename != null) {
        SaltyConfirmDialog(
            title = "Rename Project",
            confirmText = "Rename",
            dismissText = "Cancel",
            confirmEnabled = renameText.isNotBlank(),
            onConfirm = {
                val proj = projectToRename ?: return@SaltyConfirmDialog
                coroutineScope.launch {
                    repository.renameProject(proj.id, renameText.trim())
                    projectToRename = null
                    selectedProjectIds = emptySet()
                }
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
