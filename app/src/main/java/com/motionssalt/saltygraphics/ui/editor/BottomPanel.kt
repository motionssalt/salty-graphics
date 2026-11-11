package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.Layer

private data class Tool(val label: String, val icon: ImageVector, val action: () -> Unit)

@Composable
fun BottomPanel(
    selectedLayer: Layer?, onAddMedia: () -> Unit, onAddText: () -> Unit,
    onAddAdjustment: () -> Unit, onAddShape: () -> Unit, onAddVector: () -> Unit,
    onEditLayer: () -> Unit, onOpenTransform: () -> Unit, onOpenEffects: () -> Unit,
    onOpenBlending: () -> Unit, onDelete: () -> Unit,
    onOpenFill: (() -> Unit)? = null,
    onOpenBorderShadow: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onRasterize: (() -> Unit)? = null,
    groupBreadcrumbs: List<Pair<String, String>> = emptyList(),
    onPopGroup: (() -> Unit)? = null,
    onJumpToGroup: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val tools = if (selectedLayer == null) listOf(
        Tool("Media", Icons.Default.AddPhotoAlternate, onAddMedia),
        Tool("Text", Icons.Default.TextFields, onAddText),
        Tool("Adjustment", Icons.Default.Tune, onAddAdjustment),
        Tool("Shape", Icons.Default.Category, onAddShape),
        Tool("Vector", Icons.Default.Draw, onAddVector)
    ) else {
        val list = mutableListOf(
            Tool("Edit", Icons.Default.Edit, onEditLayer),
            Tool("Transform", Icons.Default.OpenWith, onOpenTransform)
        )
        val isVisualLayer = selectedLayer.type != com.motionssalt.saltygraphics.engine.model.LayerType.ADJUSTMENT
        if (onOpenFill != null && isVisualLayer) list.add(Tool("Fill", Icons.Default.FormatColorFill, onOpenFill))
        if (onOpenBorderShadow != null && isVisualLayer) list.add(Tool("Border & Shadow", Icons.Default.BorderStyle, onOpenBorderShadow))
        list.add(Tool("Effects", Icons.Default.AutoAwesome, onOpenEffects))
        list.add(Tool("Blending & Mask", Icons.Default.Layers, onOpenBlending))
        if (onDuplicate != null) list.add(Tool("Duplicate", Icons.Default.ControlPointDuplicate, onDuplicate))
        val isRasterizable = selectedLayer.type in listOf(
            com.motionssalt.saltygraphics.engine.model.LayerType.TEXT,
            com.motionssalt.saltygraphics.engine.model.LayerType.VECTOR,
            com.motionssalt.saltygraphics.engine.model.LayerType.SHAPE
        )
        if (onRasterize != null && isRasterizable) list.add(Tool("Rasterize", Icons.Default.PhotoFilter, onRasterize))
        list.add(Tool("Delete", Icons.Default.Delete, onDelete))
        list
    }
    Column(modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (groupBreadcrumbs.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onPopGroup?.invoke() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Exit Group", tint = colors.primary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Root",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                        modifier = Modifier.clickable { onJumpToGroup?.invoke(-1) }
                    )
                    groupBreadcrumbs.forEachIndexed { idx, crumb ->
                        Text(" / ", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        Text(
                            text = crumb.second,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (idx == groupBreadcrumbs.lastIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (idx == groupBreadcrumbs.lastIndex) colors.onSurface else colors.primary,
                            modifier = Modifier.clickable { onJumpToGroup?.invoke(idx) }
                        )
                    }
                }
            }
        }
        tools.chunked(3).forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { tool ->
                    FilledTonalButton(onClick = tool.action, modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(2.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(tool.icon, null)
                            Text(tool.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
