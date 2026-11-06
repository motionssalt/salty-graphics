package com.motionssalt.saltygraphics.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.data.ProjectSummary
import com.motionssalt.saltygraphics.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val importedDoc = repository.importProject(stream)
                        onNavigateToEditor(importedDoc.id)
                    }
                } catch (e: Exception) {
                    // Handle import failure silently or snackbar
                }
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
                                color = SaltyCyan
                            )
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
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
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SaltyError)
                        }
                        IconButton(onClick = { selectedProjectIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    } else {
                        IconButton(onClick = {
                            importLauncher.launch(arrayOf("*/*"))
                        }) {
                            Icon(Icons.Default.FileOpen, contentDescription = "Import .sg Project", tint = SaltyCyan)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = SaltyTextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SaltyDarkBackground,
                    titleContentColor = SaltyTextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewProject,
                containerColor = SaltyCyan,
                contentColor = SaltyDarkBackground,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Project")
            }
        },
        containerColor = SaltyDarkBackground
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
                        tint = SaltyTextTertiary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No projects yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = SaltyTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap the + button to create a new canvas or import a .sg project.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SaltyTextTertiary
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
                        focusedBorderColor = SaltyCyan,
                        focusedLabelColor = SaltyCyan
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
                    colors = ButtonDefaults.buttonColors(containerColor = SaltyCyan, contentColor = SaltyDarkBackground)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text("Cancel", color = SaltyTextSecondary)
                }
            },
            containerColor = SaltySurfaceDark
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
            containerColor = if (isSelected) SaltySurfaceVariantDark else SaltySurfaceDark
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SaltyCyan)) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Thumbnail container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SaltyDarkBackground),
                contentAlignment = Alignment.Center
            ) {
                // Subtle canvas representation
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = if (isSelected) SaltyCyan else SaltyTextTertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${project.width} x ${project.height}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SaltyTextTertiary
                    )
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
                                checkedColor = SaltyCyan,
                                checkmarkColor = SaltyDarkBackground
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = SaltyTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${project.layerCount} layers",
                style = MaterialTheme.typography.bodyMedium,
                color = SaltyTextSecondary
            )
        }
    }
}
