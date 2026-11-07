package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.action.GlobalClipboard
import com.motionssalt.saltygraphics.engine.action.PasteMode
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
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
            .fillMaxHeight()
            .width(320.dp)
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
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSelectAll) {
                    Text("Select All", style = MaterialTheme.typography.labelSmall, color = colors.onSurface)
                }

                IconButton(onClick = onSelectNone, enabled = selectedLayerIds.isNotEmpty()) {
                    Icon(Icons.Default.Deselect, contentDescription = "Select None")
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

            // Layer list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(layers, key = { _, l -> l.id }) { index, layer ->
                    val isSelected = selectedLayerIds.contains(layer.id)

                    LayerRow(
                        layer = layer,
                        isSelected = isSelected,
                        canMoveUp = index > 0,
                        canMoveDown = index < layers.size - 1,
                        onClick = { onSelectLayer(layer.id) },
                        onLongClick = { onToggleSelectMulti(layer.id) },
                        onToggleVisibility = { onToggleVisibility(layer.id) },
                        onToggleLock = { onToggleLock(layer.id) },
                        onMoveUp = { onMoveLayer(index, index - 1) },
                        onMoveDown = { onMoveLayer(index, index + 1) },
                        onEnterGroup = { onEnterGroup(layer.id) },
                        onRenameClick = {
                            layerToRename = layer
                            renameInput = layer.name
                        }
                    )
                }
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

                    fun propRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
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

                    propRow("Transform (Position, Scale, Rotation)", pasteTransform) { pasteTransform = it }
                    propRow("Effects Stack", pasteEffects) { pasteEffects = it }
                    propRow("Fill (Color, Gradient, Media)", pasteFill) { pasteFill = it }
                    propRow("Stroke / Border", pasteStroke) { pasteStroke = it }
                    propRow("Drop Shadow", pasteShadow) { pasteShadow = it }
                    propRow("Blend Mode", pasteBlend) { pasteBlend = it }
                    propRow("Opacity", pasteOpacity) { pasteOpacity = it }
                    propRow("Layer Mask", pasteMask) { pasteMask = it }
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
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEnterGroup: () -> Unit,
    onRenameClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
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

            // T22: Clear Grab Area with drag handle icon
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = colors.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp)
            )

            // Step move handles
            Column {
                if (canMoveUp) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(16.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
                if (canMoveDown) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(16.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
