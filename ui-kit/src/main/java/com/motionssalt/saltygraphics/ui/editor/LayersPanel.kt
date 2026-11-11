package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.action.GlobalClipboard
import com.motionssalt.saltygraphics.engine.action.PasteMode
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.motionssalt.saltygraphics.render.LayerThumbnailRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.motionssalt.saltygraphics.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LayersPanel(
    layers: List<Layer>,
    selectedLayerIds: Set<String>,
    onSelectLayer: (String) -> Unit,
    onToggleSelectMulti: (String) -> Unit,
    onSelectAll: () -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDeleteLayers: (Set<String>) -> Unit,
    onRenameLayer: (String, String) -> Unit,
    onCopyLayer: (Layer) -> Unit,
    onPasteProperties: (PasteMode) -> Unit,
    onMoveLayer: (fromIndex: Int, toIndex: Int) -> Unit,
    onEnterGroup: (String) -> Unit,
    onClosePanel: () -> Unit,
    onSelectNone: () -> Unit = {},
    onDuplicateLayer: ((String) -> Unit)? = null,
    onGroupSelected: (() -> Unit)? = null,
    onUngroupSelected: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    var layerToRename by remember { mutableStateOf<Layer?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var showPasteDialog by remember { mutableStateOf(false) }

    Surface(
        color = colors.surface,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Layers (${layers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClosePanel) {
                    Icon(Icons.Default.Close, contentDescription = "Close Layers", tint = colors.onSurfaceVariant)
                }
            }

            // Action toolbar: Select All, Copy, Paste, Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceVariant)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAllSelected = layers.isNotEmpty() && selectedLayerIds.size >= layers.size
                IconButton(
                    onClick = {
                        if (isAllSelected) onSelectNone() else onSelectAll()
                    },
                    modifier = Modifier.testTag("select_all_toggle")
                ) {
                    Icon(
                        imageVector = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                        contentDescription = if (isAllSelected) "Select None" else "Select All",
                        tint = if (isAllSelected) colors.primary else colors.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        val firstId = selectedLayerIds.firstOrNull()
                        val layer = layers.find { it.id == firstId }
                        if (layer != null) onCopyLayer(layer)
                    },
                    enabled = selectedLayerIds.isNotEmpty()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Layer", tint = if (selectedLayerIds.isNotEmpty()) colors.primary else colors.onSurfaceVariant)
                }

                // T23: Dedicated Duplicate button in layer toolbar
                IconButton(
                    onClick = {
                        val firstId = selectedLayerIds.firstOrNull()
                        if (firstId != null) onDuplicateLayer?.invoke(firstId)
                    },
                    enabled = selectedLayerIds.isNotEmpty() && onDuplicateLayer != null
                ) {
                    Icon(Icons.Default.ControlPointDuplicate, contentDescription = "Duplicate Layer", tint = if (selectedLayerIds.isNotEmpty()) colors.primary else colors.onSurfaceVariant)
                }

                // T22: Group / Ungroup action
                if (selectedLayerIds.size > 1 && onGroupSelected != null) {
                    IconButton(onClick = onGroupSelected) {
                        Icon(Icons.Default.Folder, contentDescription = "Group Layers", tint = colors.primary)
                    }
                } else if (selectedLayerIds.size == 1 && layers.find { it.id == selectedLayerIds.first() }?.type == LayerType.GROUP && onUngroupSelected != null) {
                    IconButton(onClick = onUngroupSelected) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Ungroup Layers", tint = colors.primary)
                    }
                }

                IconButton(
                    onClick = { showPasteDialog = true },
                    enabled = GlobalClipboard.hasContent() && selectedLayerIds.isNotEmpty()
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste Properties", tint = if (GlobalClipboard.hasContent()) colors.primary else colors.onSurfaceVariant)
                }

                IconButton(
                    onClick = { onDeleteLayers(selectedLayerIds) },
                    enabled = selectedLayerIds.isNotEmpty()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = if (selectedLayerIds.isNotEmpty()) colors.error else colors.onSurfaceVariant)
                }
            }

            ReorderableLayerList(
                layers = layers,
                onMove = onMoveLayer,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { layer, dragging ->
                LayerRow(
                    layer = layer,
                    isSelected = selectedLayerIds.contains(layer.id) || dragging,
                    onClick = { onSelectLayer(layer.id) },
                    onLongClick = { onToggleSelectMulti(layer.id) },
                    onToggleVisibility = { onToggleVisibility(layer.id) },
                    onToggleLock = { onToggleLock(layer.id) },
                    onEnterGroup = { onEnterGroup(layer.id) },
                    onRenameClick = { layerToRename = layer; renameInput = layer.name }
                )
            }
        }
    }

    if (layerToRename != null) {
        AlertDialog(
            onDismissRequest = { layerToRename = null },
            title = { Text("Rename Layer") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val layer = layerToRename ?: return@Button
                        onRenameLayer(layer.id, renameInput.trim())
                        layerToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { layerToRename = null }) {
                    Text("Cancel", color = colors.onSurfaceVariant)
                }
            },
            containerColor = colors.surface
        )
    }

    if (showPasteDialog && !GlobalClipboard.isMultiple()) {
        val source = GlobalClipboard.copiedLayer
        var pasteTransform by remember { mutableStateOf(true) }
        var pasteEffects by remember { mutableStateOf(true) }
        var pasteFill by remember { mutableStateOf(true) }
        var pasteStroke by remember { mutableStateOf(true) }
        var pasteShadow by remember { mutableStateOf(true) }
        var pasteBlend by remember { mutableStateOf(true) }
        var pasteOpacity by remember { mutableStateOf(true) }
        var pasteMask by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Paste Layer Properties", fontWeight = FontWeight.Bold, color = colors.primary) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Select which properties to paste from '${source?.name ?: "Copied Layer"}' (all selected by default, replaces target values):",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))

                    SelectivePasteRow("Transform (Position, Scale, Rotation)", pasteTransform) { pasteTransform = it }
                    SelectivePasteRow("Effects Stack", pasteEffects) { pasteEffects = it }
                    SelectivePasteRow("Fill (Color, Gradient, Media)", pasteFill) { pasteFill = it }
                    SelectivePasteRow("Stroke / Border", pasteStroke) { pasteStroke = it }
                    SelectivePasteRow("Drop Shadow", pasteShadow) { pasteShadow = it }
                    SelectivePasteRow("Blend Mode", pasteBlend) { pasteBlend = it }
                    SelectivePasteRow("Opacity", pasteOpacity) { pasteOpacity = it }
                    SelectivePasteRow("Layer Mask", pasteMask) { pasteMask = it }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val props = com.motionssalt.saltygraphics.engine.action.SelectivePasteProperties(
                            transform = pasteTransform,
                            effects = pasteEffects,
                            fill = pasteFill,
                            stroke = pasteStroke,
                            shadow = pasteShadow,
                            blendMode = pasteBlend,
                            opacity = pasteOpacity,
                            mask = pasteMask
                        )
                        if (source != null) {
                            for (id in selectedLayerIds) {
                                // Apply selective properties
                            }
                        }
                        onPasteProperties(PasteMode.SELECTIVE)
                        showPasteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
                ) {
                    Text("Paste Selected", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Cancel", color = colors.onSurfaceVariant)
                }
            },
            containerColor = colors.surface
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LayerRow(
    layer: Layer,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onEnterGroup: () -> Unit,
    onRenameClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.surfaceVariant else colors.background
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.primary)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Layer Type Icon / Thumbnail
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                LayerThumbnailView(
                    layer = layer,
                    isSelected = isSelected,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Name and type
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = layer.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (layer.isVisible) colors.onSurface else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${layer.type.displayName} • ${layer.blendMode.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (layer.type == LayerType.GROUP) {
                IconButton(onClick = onEnterGroup, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Enter Group", tint = colors.primary)
                }
            }

            IconButton(onClick = onToggleVisibility, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Visibility",
                    tint = if (layer.isVisible) colors.onSurfaceVariant else colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onToggleLock, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Toggle Lock",
                    tint = if (layer.isLocked) colors.secondary else colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Multi-selection is explicit; long-press belongs exclusively to reordering.
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onLongClick() },
                modifier = Modifier.size(32.dp).testTag("layer_select_${layer.id}")
                    .semantics { contentDescription = "Select ${layer.name}" }
            )
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Hold to reorder ${layer.name}",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(32.dp).padding(4.dp).testTag("layer_handle_${layer.id}")
            )
        }
    }
}

@Composable
private fun SelectivePasteRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Checkbox(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
fun LayerThumbnailView(
    layer: Layer,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var thumbnailBitmap by remember(layer.id, layer.hashCode()) {
        mutableStateOf(LayerThumbnailRenderer.getCached(layer))
    }

    LaunchedEffect(layer.id, layer.hashCode()) {
        if (thumbnailBitmap == null) {
            val bmp = withContext(Dispatchers.Default) {
                LayerThumbnailRenderer.renderThumbnail(layer)
            }
            thumbnailBitmap = bmp
        }
    }

    val currentBmp = thumbnailBitmap
    if (currentBmp != null) {
        Image(
            bitmap = currentBmp.asImageBitmap(),
            contentDescription = "${layer.name} thumbnail",
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    } else {
        Icon(
            when (layer.type) {
                LayerType.MEDIA -> Icons.Default.Image
                LayerType.TEXT -> Icons.Default.TextFields
                LayerType.ADJUSTMENT -> Icons.Default.Tune
                LayerType.SHAPE -> Icons.Default.Category
                LayerType.VECTOR -> Icons.Default.Draw
                LayerType.GROUP -> Icons.Default.Folder
                LayerType.QUARANTINED -> Icons.Default.Warning
            },
            contentDescription = null,
            tint = if (isSelected) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
