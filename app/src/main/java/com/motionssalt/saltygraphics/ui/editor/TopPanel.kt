package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import com.motionssalt.saltygraphics.theme.*

@Composable
fun TopPanel(
    previewQuality: PreviewQuality,
    canUndo: Boolean,
    canRedo: Boolean,
    showGrid: Boolean,
    gridSize: Int,
    onNavigateBack: () -> Unit,
    onToggleLayers: () -> Unit,
    onPreviewQualityClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onOpenActionTree: () -> Unit,
    onExportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFitToScreen: () -> Unit,
    onReset100Percent: () -> Unit,
    onToggleGrid: () -> Unit,
    onGridSizeChange: (Int) -> Unit,
    onSaveSgClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    var showViewModeMenu by remember { mutableStateOf(false) }
    var showOperationMenu by remember { mutableStateOf(false) }

    Surface(
        color = colors.surface,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .displayCutoutPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Group: Back, Layers, View mode, Preview quality
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onSurface)
                }

                IconButton(onClick = onToggleLayers, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.Layers, contentDescription = "Layers", tint = colors.primary)
                }

                // View Mode Dropdown
                Box {
                    IconButton(onClick = { showViewModeMenu = true }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "View Mode", tint = colors.onSurface)
                    }
                    DropdownMenu(
                        expanded = showViewModeMenu,
                        onDismissRequest = { showViewModeMenu = false },
                        modifier = Modifier.background(colors.surfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Zoom In (+)", color = colors.onSurface) },
                            leadingIcon = { Icon(Icons.Default.ZoomIn, contentDescription = null, tint = colors.primary) },
                            onClick = {
                                onZoomIn()
                                showViewModeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Zoom Out (-)", color = colors.onSurface) },
                            leadingIcon = { Icon(Icons.Default.ZoomOut, contentDescription = null, tint = colors.primary) },
                            onClick = {
                                onZoomOut()
                                showViewModeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Fit to Screen", color = colors.onSurface) },
                            leadingIcon = { Icon(Icons.Default.FitScreen, contentDescription = null, tint = colors.primary) },
                            onClick = {
                                onFitToScreen()
                                showViewModeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("100% Zoom", color = colors.onSurface) },
                            leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = null, tint = colors.primary) },
                            onClick = {
                                onReset100Percent()
                                showViewModeMenu = false
                            }
                        )
                        HorizontalDivider(color = colors.outline)
                        DropdownMenuItem(
                            text = { Text(if (showGrid) "Hide Gridlines" else "Show Gridlines", color = colors.onSurface) },
                            leadingIcon = { Icon(Icons.Default.GridOn, contentDescription = null, tint = if (showGrid) colors.primary else colors.onSurfaceVariant) },
                            onClick = {
                                onToggleGrid()
                                showViewModeMenu = false
                            }
                        )
                        if (showGrid) {
                            DropdownMenuItem(
                                text = { Text("Grid Size: ${gridSize}px", color = colors.onSurfaceVariant) },
                                onClick = {
                                    val nextSize = when (gridSize) {
                                        20 -> 50
                                        50 -> 100
                                        else -> 20
                                    }
                                    onGridSizeChange(nextSize)
                                }
                            )
                        }
                    }
                }

                // Preview Quality Badge / Button
                Surface(
                    onClick = onPreviewQualityClick,
                    shape = RoundedCornerShape(6.dp),
                    color = colors.surfaceVariant,
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Text(
                        text = when (previewQuality) {
                            PreviewQuality.LOW -> "25%"
                            PreviewQuality.MEDIUM -> "50%"
                            PreviewQuality.HIGH -> "100%"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = when (previewQuality) {
                            PreviewQuality.LOW -> colors.secondary
                            PreviewQuality.MEDIUM -> colors.primary
                            PreviewQuality.HIGH -> Color.White
                        },
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                    )
                }
            }

            // Divider spacer
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(colors.outline)
            )

            // Right Group: Operation, Export, Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Operation (Undo, Redo, Action Tree)
                Box {
                    IconButton(onClick = { showOperationMenu = true }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.History, contentDescription = "Operation & Action Tree", tint = colors.onSurface)
                    }
                    DropdownMenu(
                        expanded = showOperationMenu,
                        onDismissRequest = { showOperationMenu = false },
                        modifier = Modifier.background(colors.surfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Undo", color = if (canUndo) colors.onSurface else colors.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = if (canUndo) colors.primary else colors.onSurfaceVariant) },
                            enabled = canUndo,
                            onClick = {
                                onUndoClick()
                                showOperationMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Redo", color = if (canRedo) colors.onSurface else colors.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = null, tint = if (canRedo) colors.primary else colors.onSurfaceVariant) },
                            enabled = canRedo,
                            onClick = {
                                onRedoClick()
                                showOperationMenu = false
                            }
                        )
                        HorizontalDivider(color = colors.outline)
                        DropdownMenuItem(
                            text = { Text("Open Action Tree", color = colors.primary) },
                            leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null, tint = colors.primary) },
                            onClick = {
                                onOpenActionTree()
                                showOperationMenu = false
                            }
                        )
                    }
                }

                IconButton(onClick = onExportClick, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = colors.primary)
                }

                IconButton(onClick = onSettingsClick, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = colors.onSurfaceVariant)
                }
            }
        }
    }
}
