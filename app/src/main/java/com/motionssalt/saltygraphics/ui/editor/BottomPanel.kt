package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.Layer

private data class Tool(val label: String, val icon: ImageVector, val action: () -> Unit)

@Composable
fun BottomPanel(
    selectedLayer: Layer?, onAddMedia: () -> Unit, onAddText: () -> Unit,
    onAddAdjustment: () -> Unit, onAddShape: () -> Unit, onAddVector: () -> Unit,
    onEditLayer: () -> Unit, onOpenTransform: () -> Unit, onOpenEffects: () -> Unit,
    onOpenBlending: () -> Unit, onOpenMask: () -> Unit, onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tools = if (selectedLayer == null) listOf(
        Tool("Media", Icons.Default.AddPhotoAlternate, onAddMedia),
        Tool("Text", Icons.Default.TextFields, onAddText),
        Tool("Adjustment", Icons.Default.Tune, onAddAdjustment),
        Tool("Shape", Icons.Default.Category, onAddShape),
        Tool("Vector", Icons.Default.Draw, onAddVector)
    ) else listOf(
        Tool("Edit", Icons.Default.Edit, onEditLayer),
        Tool("Transform", Icons.Default.OpenWith, onOpenTransform),
        Tool("Effects", Icons.Default.AutoAwesome, onOpenEffects),
        Tool("Blending", Icons.Default.Layers, onOpenBlending),
        Tool("Mask", Icons.Default.FilterFrames, onOpenMask),
        Tool("Delete", Icons.Default.Delete, onDelete)
    )
    Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
