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
import androidx.compose.ui.text.font.FontWeight
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
    onSaveSgClick: () -> Unit = {},
    isCanvasLocked: Boolean = false,
    onToggleCanvasLock: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme

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

                // View Mode Actions (Zoom, Fit, Grid toggle)
                IconButton(onClick = { if (!isCanvasLocked) onZoomIn() }, enabled = !isCanvasLocked, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = if (isCanvasLocked) colors.onSurface.copy(alpha = 0.38f) else colors.onSurface)
                }
                IconButton(onClick = { if (!isCanvasLocked) onZoomOut() }, enabled = !isCanvasLocked, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = if (isCanvasLocked) colors.onSurface.copy(alpha = 0.38f) else colors.onSurface)
                }
                IconButton(onClick = { if (!isCanvasLocked) onFitToScreen() }, enabled = !isCanvasLocked, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.FitScreen, contentDescription = "Fit to Screen", tint = if (isCanvasLocked) colors.onSurface.copy(alpha = 0.38f) else colors.onSurface)
                }
                IconButton(onClick = { onToggleGrid() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.GridOn, contentDescription = "Toggle Grid", tint = if (showGrid) colors.primary else colors.onSurfaceVariant)
                }
                }

                // V13: Canvas Lock Toggle
                IconButton(
                    onClick = onToggleCanvasLock,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isCanvasLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isCanvasLocked) "Unlock Canvas" else "Lock Canvas",
                        tint = if (isCanvasLocked) colors.secondary else colors.onSurfaceVariant
                    )
                }

                // Preview Quality Badge / Button (T13: distinct from zoom with icon and fractional label)
                Surface(
                    onClick = onPreviewQualityClick,
                    shape = RoundedCornerShape(6.dp),
                    color = colors.surfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.HighQuality,
                            contentDescription = "Preview Quality",
                            tint = colors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = previewQuality.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = colors.onSurface
                        )
                    }
                }
            }

            // Divider spacer
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(colors.outline)
            )

            // Right Group: Direct Undo/Redo (S13), Action Tree, Export, Settings (T27: redundant 3-dot menu removed)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Always visible Undo button
                IconButton(
                    onClick = onUndoClick,
                    enabled = canUndo,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) colors.onSurface else colors.onSurface.copy(alpha = 0.35f)
                    )
                }

                // Always visible Redo button
                IconButton(
                    onClick = onRedoClick,
                    enabled = canRedo,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) colors.onSurface else colors.onSurface.copy(alpha = 0.35f)
                    )
                }

                // History / Action Tree button
                IconButton(
                    onClick = onOpenActionTree,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = "Action Tree History",
                        tint = colors.primary
                    )
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
