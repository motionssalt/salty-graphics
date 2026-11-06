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
    onClosePanel: () -> Unit
) {
    var layerToRename by remember { mutableStateOf<Layer?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var showPasteDialog by remember { mutableStateOf(false) }

    Surface(
        color = SaltySurfaceDark,
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
                    color = SaltyCyan,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClosePanel) {
                    Icon(Icons.Default.Close, contentDescription = "Close Layers", tint = SaltyTextSecondary)
                }
            }

            // Action toolbar: Select All, Copy, Paste, Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SaltySurfaceVariantDark)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSelectAll) {
                    Text("Select All", style = MaterialTheme.typography.labelSmall, color = SaltyTextPrimary)
                }

                IconButton(
                    onClick = {
                        val firstId = selectedLayerIds.firstOrNull()
                        val layer = layers.find { it.id == firstId }
                        if (layer != null) onCopyLayer(layer)
                    },
                    enabled = selectedLayerIds.isNotEmpty()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Layer", tint = if (selectedLayerIds.isNotEmpty()) SaltyCyan else SaltyTextTertiary)
                }

                IconButton(
                    onClick = { showPasteDialog = true },
                    enabled = GlobalClipboard.hasContent() && selectedLayerIds.isNotEmpty()
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste Properties", tint = if (GlobalClipboard.hasContent()) SaltyCyan else SaltyTextTertiary)
                }

                IconButton(
                    onClick = { onDeleteLayers(selectedLayerIds) },
                    enabled = selectedLayerIds.isNotEmpty()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = if (selectedLayerIds.isNotEmpty()) SaltyError else SaltyTextTertiary)
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
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SaltyCyan)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val layer = layerToRename ?: return@Button
                        onRenameLayer(layer.id, renameInput.trim())
                        layerToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaltyCyan, contentColor = SaltyDarkBackground)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { layerToRename = null }) {
                    Text("Cancel", color = SaltyTextSecondary)
                }
            },
            containerColor = SaltySurfaceDark
        )
    }

    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Paste Properties (Section 3.9)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose which properties to apply (will replace existing values on selected layers):", style = MaterialTheme.typography.bodyMedium, color = SaltyTextSecondary)
                    Button(
                        onClick = {
                            onPasteProperties(PasteMode.TRANSFORM_ONLY)
                            showPasteDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SaltySurfaceVariantDark, contentColor = SaltyTextPrimary)
                    ) {
                        Text("Transform Only")
                    }
                    Button(
                        onClick = {
                            onPasteProperties(PasteMode.EFFECTS_ONLY)
                            showPasteDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SaltySurfaceVariantDark, contentColor = SaltyTextPrimary)
                    ) {
                        Text("Effects Only")
                    }
                    Button(
                        onClick = {
                            onPasteProperties(PasteMode.BOTH)
                            showPasteDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SaltyCyan, contentColor = SaltyDarkBackground)
                    ) {
                        Text("Both (Transform & Effects)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Cancel", color = SaltyTextSecondary)
                }
            },
            containerColor = SaltySurfaceDark
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SaltySurfaceVariantDark else SaltyDarkBackground
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SaltyCyan)) else null
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
                    .background(SaltySurfaceVariantDark),
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
                    },
                    contentDescription = null,
                    tint = if (isSelected) SaltyCyan else SaltyTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Name and type
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = layer.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (layer.isVisible) SaltyTextPrimary else SaltyTextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${layer.type.displayName} • ${layer.blendMode.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SaltyTextTertiary
                )
            }

            if (layer.type == LayerType.GROUP) {
                IconButton(onClick = onEnterGroup, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Enter Group", tint = SaltyCyan)
                }
            }

            IconButton(onClick = onToggleVisibility, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Visibility",
                    tint = if (layer.isVisible) SaltyTextSecondary else SaltyTextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onToggleLock, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Toggle Lock",
                    tint = if (layer.isLocked) SaltyAmber else SaltyTextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Move handle
            Column {
                if (canMoveUp) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(16.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = SaltyTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
                if (canMoveDown) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(16.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = SaltyTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
