package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.action.ActionNode
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActionTreeDialog(
    actionTree: ActionTree,
    onJumpToNode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val allNodes = actionTree.nodes.values.sortedByDescending { it.timestamp }
    val currentNodeId = actionTree.currentNodeId
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountTree, contentDescription = null, tint = colors.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Action Tree History", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Branching command history. Tap any action node across any branch to jump directly to that project state. Clipboard survives branch jumps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allNodes, key = { it.id }) { node ->
                        val isCurrent = node.id == currentNodeId
                        val isRoot = node.id == "root"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = if (isCurrent) colors.primary else colors.outline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onJumpToNode(node.id)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) colors.surfaceVariant else colors.background
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = node.description,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isCurrent) colors.primary else colors.onSurface
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Active State", tint = colors.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Text(
                                        text = if (isRoot) "Root Node" else "Branch Node • ${dateFormat.format(Date(node.timestamp))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    color = if (isCurrent) colors.primary else colors.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isCurrent) "Current" else "Jump",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCurrent) colors.background else colors.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.onSurfaceVariant)
            }
        },
        containerColor = colors.surface
    )
}
