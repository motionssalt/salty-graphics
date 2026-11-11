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
import com.motionssalt.saltygraphics.uikit.registry.IconToken
import com.motionssalt.saltygraphics.uikit.registry.LocalFeatureRegistry

private data class Tool(val label: String, val icon: ImageVector, val action: () -> Unit)
@Composable private fun IconToken.image(): ImageVector = when(this) {
 IconToken.MEDIA -> Icons.Default.AddPhotoAlternate; IconToken.TEXT -> Icons.Default.TextFields
 IconToken.ADJUSTMENT -> Icons.Default.Tune; IconToken.SHAPE -> Icons.Default.Category
 IconToken.VECTOR -> Icons.Default.Draw; IconToken.EDIT -> Icons.Default.Edit
 IconToken.TRANSFORM -> Icons.Default.OpenWith; IconToken.FILL -> Icons.Default.FormatColorFill
 IconToken.BORDER_SHADOW -> Icons.Default.BorderStyle; IconToken.EFFECTS -> Icons.Default.AutoAwesome
 IconToken.BLENDING -> Icons.Default.Layers; IconToken.DUPLICATE -> Icons.Default.ControlPointDuplicate
 IconToken.RASTERIZE -> Icons.Default.PhotoFilter; IconToken.DELETE -> Icons.Default.Delete
 else -> Icons.Default.Settings
}

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
    modifier: Modifier = Modifier,
    onOpenFeature: ((String) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val registry = LocalFeatureRegistry.current
    val document = selectedLayer?.let { com.motionssalt.saltygraphics.engine.model.ProjectDocument(layers=listOf(it)) }
        ?: com.motionssalt.saltygraphics.engine.model.ProjectDocument()
    val tools = registry.toolsFor(document,selectedLayer).mapNotNull { feature ->
        val action = when(feature.id) {
            "media" -> onAddMedia; "text" -> onAddText; "adjustment" -> onAddAdjustment
            "shape" -> onAddShape; "vector" -> onAddVector; "edit" -> onEditLayer
            "transform" -> onOpenTransform; "effects" -> onOpenEffects; "blending" -> onOpenBlending
            "delete" -> onDelete; "fill" -> onOpenFill; "border_shadow" -> onOpenBorderShadow
            "duplicate" -> onDuplicate; "rasterize" -> onRasterize
            else -> onOpenFeature?.let { opener -> { opener(feature.id) } }
        }
        action?.let { Tool(feature.title,feature.icon.image(),it) }
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
