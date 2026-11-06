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
    onGridSizeChange: (Int) -> Unit
) {
    var showViewModeMenu by remember { mutableStateOf(false) }
    var showOperationMenu by remember { mutableStateOf(false) }

    Surface(
        color = SaltySurfaceDark,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SaltyTextPrimary)
                }

                IconButton(onClick = onToggleLayers, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.Layers, contentDescription = "Layers", tint = SaltyCyan)
                }

                // View Mode Dropdown
                Box {
                    IconButton(onClick = { showViewModeMenu = true }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "View Mode", tint = SaltyTextPrimary)
                    }
                    DropdownMenu(
                        expanded = showViewModeMenu,
                        onDismissRequest = { showViewModeMenu = false },
                        modifier = Modifier.background(SaltySurfaceVariantDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Zoom In (+)", color = SaltyTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.ZoomIn, contentDescription = null, tint = SaltyCyan) },
                            onClick = {
                                onZoomIn()
                                showViewModeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Zoom Out (-)", color = SaltyTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.ZoomOut, contentDescription = null, tint = SaltyCyan) },
                            onClick = {
                                onZoomOut()
                                showViewModeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Fit to Screen", color = SaltyTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.FitScreen, contentDescription = null, tint = SaltyCyan) },
                            onClick = {
                                onFitToScreen()
                                showViewModeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("100% Zoom", color = SaltyTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = null, tint = SaltyCyan) },
                            onClick = {
                                onReset100Percent()
                                showViewModeMenu = false
                            }
                        )
                        HorizontalDivider(color = SaltyOutlineDark)
                        DropdownMenuItem(
                            text = { Text(if (showGrid) "Hide Gridlines" else "Show Gridlines", color = SaltyTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.GridOn, contentDescription = null, tint = if (showGrid) SaltyCyan else SaltyTextSecondary) },
                            onClick = {
                                onToggleGrid()
                                showViewModeMenu = false
                            }
                        )
                        if (showGrid) {
                            DropdownMenuItem(
                                text = { Text("Grid Size: ${gridSize}px", color = SaltyTextSecondary) },
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
                    color = SaltySurfaceVariantDark,
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
                            PreviewQuality.LOW -> SaltyAmber
                            PreviewQuality.MEDIUM -> SaltyCyan
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
                    .background(SaltyOutlineDark)
            )

            // Right Group: Operation, Export, Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Operation (Undo, Redo, Action Tree)
                Box {
                    IconButton(onClick = { showOperationMenu = true }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.History, contentDescription = "Operation & Action Tree", tint = SaltyTextPrimary)
                    }
                    DropdownMenu(
                        expanded = showOperationMenu,
                        onDismissRequest = { showOperationMenu = false },
                        modifier = Modifier.background(SaltySurfaceVariantDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Undo", color = if (canUndo) SaltyTextPrimary else SaltyTextTertiary) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = if (canUndo) SaltyCyan else SaltyTextTertiary) },
                            enabled = canUndo,
                            onClick = {
                                onUndoClick()
                                showOperationMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Redo", color = if (canRedo) SaltyTextPrimary else SaltyTextTertiary) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = null, tint = if (canRedo) SaltyCyan else SaltyTextTertiary) },
                            enabled = canRedo,
                            onClick = {
                                onRedoClick()
                                showOperationMenu = false
                            }
                        )
                        HorizontalDivider(color = SaltyOutlineDark)
                        DropdownMenuItem(
                            text = { Text("Open Action Tree", color = SaltyCyan) },
                            leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null, tint = SaltyCyan) },
                            onClick = {
                                onOpenActionTree()
                                showOperationMenu = false
                            }
                        )
                    }
                }

                IconButton(onClick = onExportClick, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = SaltyCyan)
                }

                IconButton(onClick = onSettingsClick, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = SaltyTextSecondary)
                }
            }
        }
    }
}
