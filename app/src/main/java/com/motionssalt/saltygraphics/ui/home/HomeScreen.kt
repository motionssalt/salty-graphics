package com.motionssalt.saltygraphics.ui.home

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.render.ProjectThumbnailRenderer
import com.motionssalt.saltygraphics.uikit.sublibs.home.SaltyHomeProjectItem
import com.motionssalt.saltygraphics.uikit.sublibs.home.SaltyHomeScreen
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

    val homeItems = remember(projects) {
        projects.map {
            SaltyHomeProjectItem(
                id = it.id,
                name = it.name,
                width = it.width,
                height = it.height,
                layerCount = it.layerCount,
                modifiedAt = it.modifiedAt
            )
        }
    }

    SaltyHomeScreen(
        projects = homeItems,
        selectedProjectIds = selectedProjectIds,
        onSelectionChange = { selectedProjectIds = it },
        onOpenProject = onNavigateToEditor,
        onNewProject = onNavigateToNewProject,
        onImportProjects = { importLauncher.launch(arrayOf("*/*")) },
        onOpenSettings = onNavigateToSettings,
        onExportSelected = { targetIds ->
            exportingProjectIds = targetIds
            if (targetIds.size == 1) {
                val proj = projects.find { it.id == targetIds.first() }
                val fileName = "${(proj?.name ?: "Project").replace(" ", "_")}.sg"
                exportSgLauncher.launch(fileName)
            } else {
                exportSgLauncher.launch("Salty_Projects_${targetIds.size}.zip")
            }
        },
        onDuplicateProject = { id ->
            coroutineScope.launch { repository.duplicateProject(id) }
        },
        onDeleteProjects = { ids ->
            coroutineScope.launch { repository.deleteProjects(ids) }
        },
        onRenameProject = { id, newName ->
            coroutineScope.launch { repository.renameProject(id, newName) }
        },
        thumbnailLoader = { item ->
            val thumbnailBitmap by produceState<Bitmap?>(
                initialValue = ProjectThumbnailRenderer.getCached(item.id),
                key1 = item.id,
                key2 = item.modifiedAt
            ) {
                value = withContext(Dispatchers.IO) {
                    ProjectThumbnailRenderer.getOrLoad(context, item.id)
                }
            }
            thumbnailBitmap
        }
    )
}
