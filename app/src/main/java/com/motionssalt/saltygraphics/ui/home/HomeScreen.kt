package com.motionssalt.saltygraphics.ui.home

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.data.ProjectSummary
import com.motionssalt.saltygraphics.render.ProjectThumbnailRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    repository: ProjectRepository,
    onNavigateToNewProject: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val projects by repository.projects.collectAsState()
    var selectedProjectIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedProjectIds.isNotEmpty()
    var projectToRename by remember { mutableStateOf<ProjectSummary?>(null) }
    var renameText by remember { mutableStateOf("") }

    // Multi-file .sg import launcher
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
                if (successCount == 1 && lastImportedId != null) {
                    onNavigateToEditor(lastImportedId)
                } else if (successCount > 1) {
                    Toast.makeText(context, "Imported $successCount projects successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Export .sg file launcher
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
                            // Bundle multiple .sg projects into a zip archive
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
            // Never crash home screen initialization
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("${selectedProjectIds.size} Selected", style = MaterialTheme.typography.titleLarge)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "SALTY GRAPHICS",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = colors.primary
                            )
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // S2: Save project (.sg) available on Home per project and for multi-selection
                        IconButton(onClick = {
                            exportingProjectIds = selectedProjectIds
                            if (selectedProjectIds.size == 1) {
                                val proj = projects.find { it.id == selectedProjectIds.first() }
                                val fileName = "${(proj?.name ?: "Project").replace(" ", "_")}.sg"
                                exportSgLauncher.launch(fileName)
                            } else {
                                exportSgLauncher.launch("Salty_Projects_${selectedProjectIds.size}.zip")
                            }
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export .sg", tint = colors.primary)
                        }

                        if (selectedProjectIds.size == 1) {
                            IconButton(onClick = {
                                val id = selectedProjectIds.first()
                                val project = projects.find { it.id == id }
                                if (project != null) {
                                    projectToRename = project
                                    renameText = project.name
                                }
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename")
                            }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    repository.duplicateProject(selectedProjectIds.first())
                                    selectedProjectIds = emptySet()
                                }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                            }
                        }
                        IconButton(onClick = {
                            coroutineScope.launch {
                                repository.deleteProjects(selectedProjectIds)
                                selectedProjectIds = emptySet()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = colors.error)
                        }
                        IconButton(onClick = { selectedProjectIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    } else {
                        IconButton(onClick = {
                            importLauncher.launch(arrayOf("*/*"))
                        }) {
                            Icon(Icons.Default.FileOpen, contentDescription = "Import .sg Project", tint = colors.primary)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = colors.onSurfaceVariant)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewProject,
                containerColor = colors.primary,
                contentColor = colors.background,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Project")
            }
        },
        containerColor = colors.background
    ) { innerPadding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No projects yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap the + button to create a new canvas or import a .sg project.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(projects, key = { it.id }) { project ->
                    val isSelected = selectedProjectIds.contains(project.id)
                    ProjectCard(
                        project = project,
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
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        focusedLabelColor = colors.primary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val proj = projectToRename ?: return@Button
                        coroutineScope.launch {
                            repository.renameProject(proj.id, renameText.trim())
                            projectToRename = null
                            selectedProjectIds = emptySet()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text("Cancel", color = colors.onSurfaceVariant)
                }
            },
            containerColor = colors.surface
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectCard(
    project: ProjectSummary,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    // S2b: Load real rendered thumbnail from storage / LRU cache
    val thumbnailBitmap by produceState<Bitmap?>(
        initialValue = ProjectThumbnailRenderer.getCached(project.id),
        key1 = project.id,
        key2 = project.modifiedAt
    ) {
        value = withContext(Dispatchers.IO) {
            ProjectThumbnailRenderer.getOrLoad(context, project.id)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.surfaceVariant else colors.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.primary)) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Thumbnail container preserving aspect ratio without stretching
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.background),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = "Project Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Placeholder while loading or if rendering fails
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = if (isSelected) colors.primary else colors.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${project.width} x ${project.height}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.primary,
                                checkmarkColor = colors.background
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // S2b: Layer count text with correct plural: "No layers", "1 layer", "N layers"
                val layerCountText = when (project.layerCount) {
                    0 -> "No layers"
                    1 -> "1 layer"
                    else -> "${project.layerCount} layers"
                }
                Text(
                    text = layerCountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = "${project.width}x${project.height}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
