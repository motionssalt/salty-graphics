package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.history.HistoryGraphCalculator
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
    val graphData = remember(actionTree.nodes, actionTree.currentNodeId) {
        HistoryGraphCalculator.computeGraph(
            allNodes = actionTree.nodes,
            rootNodeId = actionTree.rootNodeId,
            currentNodeId = actionTree.currentNodeId
        )
    }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountTree, contentDescription = null, tint = colors.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Action History Tree", color = colors.primary, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Branched history graph. Main path to current state highlighted. Undone and alternative branch actions visible. Tap any state node to jump to it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Graph Container
                val laneWidth = 32.dp
                val rowHeight = 56.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.background)
                        .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        graphData.nodes.forEach { nodeLayout ->
                            val isCurrent = nodeLayout.isCurrent
                            val isMain = nodeLayout.isOnMainPath
                            val isUndone = nodeLayout.isUndone

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isCurrent -> colors.primary.copy(alpha = 0.18f)
                                            isMain -> colors.surfaceVariant
                                            else -> colors.surface.copy(alpha = 0.5f)
                                        }
                                    )
                                    .border(
                                        width = if (isCurrent) 1.5.dp else 1.dp,
                                        color = when {
                                            isCurrent -> colors.primary
                                            isMain -> colors.outline
                                            else -> colors.outline.copy(alpha = 0.3f)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onJumpToNode(nodeLayout.node.id)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Branch Lane Indicator
                                Row(
                                    modifier = Modifier.width((nodeLayout.lane * 20 + 24).dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isCurrent) 14.dp else 10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isCurrent -> colors.primary
                                                    isMain -> colors.primary.copy(alpha = 0.7f)
                                                    else -> colors.onSurfaceVariant.copy(alpha = 0.4f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCurrent) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = colors.background,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Node Description and Timestamp
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = nodeLayout.node.description,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isCurrent || isMain) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = when {
                                                isCurrent -> colors.primary
                                                isMain -> colors.onSurface
                                                else -> colors.onSurfaceVariant.copy(alpha = 0.6f)
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (nodeLayout.isBranchPoint) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = colors.secondary.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Branch",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = colors.secondary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (isUndone) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = colors.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Undone",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (nodeLayout.node.id == "root") "Root" else dateFormat.format(Date(nodeLayout.node.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }

                                // Status Pill
                                Surface(
                                    color = if (isCurrent) colors.primary else colors.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isCurrent) "Current" else "Jump",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCurrent) colors.background else colors.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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
