package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.*
import kotlin.math.*

/**
 * Vector editing overlay:
 * Touch priority in Select mode:
 * 1. Bezier handle (in/out)
 * 2. Anchor point
 * 3. Path segment (dragging bends curve)
 * 4. Empty space: DOES NOTHING (never creates a new path!)
 */
@Composable
fun VectorCanvasOverlay(
    layer: Layer,
    canvasSize: CanvasSize,
    pathIndex: Int,
    selectedPoint: Int?,
    tool: VectorTool,
    zoom: Float,
    pan: Offset,
    onSelectPoint: (Int?) -> Unit,
    onPreview: (Layer?) -> Unit,
    onCommit: (Layer) -> Unit,
    onDone: () -> Unit
) {
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var draft by remember(layer.id) { mutableStateOf<Layer?>(null) }
    val latestLayer by rememberUpdatedState(layer)
    val latestSelect by rememberUpdatedState(onSelectPoint)
    val latestPreview by rememberUpdatedState(onPreview)
    val latestCommit by rememberUpdatedState(onCommit)
    val colors = MaterialTheme.colorScheme

    // V18 Controls
    var independentHandles by remember { mutableStateOf(false) }
    var angleLock45 by remember { mutableStateOf(false) }

    val current = draft ?: layer
    val index = pathIndex.coerceIn(0, (current.editablePaths.size - 1).coerceAtLeast(0))
    val path = current.editablePaths.getOrNull(index) ?: VectorPathData()

    val fit = if (viewport.width > 0 && viewport.height > 0) {
        minOf(viewport.width / canvasSize.width.toFloat(), viewport.height / canvasSize.height.toFloat()) * 0.94f
    } else 1f
    val scale = fit * zoom

    fun screen(x: Float, y: Float, t: Transform2D): Offset {
        val r = Math.toRadians(t.rotationDegrees.toDouble())
        val sx = x * t.scaleX
        val sy = y * t.scaleY
        return Offset(
            viewport.width / 2f + pan.x + (sx * cos(r) - sy * sin(r) + t.translationX).toFloat() * scale,
            viewport.height / 2f + pan.y + (sx * sin(r) + sy * cos(r) + t.translationY).toFloat() * scale
        )
    }

    Box(Modifier.fillMaxSize().onSizeChanged { viewport = it }) {
        Canvas(Modifier.fillMaxSize().testTag("vector_handles").pointerInput(layer.id, pathIndex, tool, scale, pan, independentHandles, angleLock45) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val original = latestLayer
                if (original.isLocked) return@awaitEachGesture
                val paths = original.editablePaths.ifEmpty { listOf(VectorPathData()) }.toMutableList()
                val pi = pathIndex.coerceIn(0, paths.lastIndex)
                var working = paths[pi]

                fun local(at: Offset) = VectorEditing.toLocal(
                    (at.x - viewport.width / 2f - pan.x) / scale,
                    (at.y - viewport.height / 2f - pan.y) / scale,
                    original.transform
                )

                val hitRadius = 24.dp.toPx()
                var pointIndex = -1
                var handle = 0 // -1: inHandle, 1: outHandle, 0: anchor, 2: segment curve
                var bestDist = hitRadius

                // Priority 1: Bezier Handles
                if (tool == VectorTool.SELECT) {
                    working.points.forEachIndexed { i, p ->
                        if (p.hasInHandle) {
                            val inScreen = screen(p.inHandleX, p.inHandleY, original.transform)
                            val d = (inScreen - down.position).getDistance()
                            if (d < bestDist) {
                                bestDist = d
                                pointIndex = i
                                handle = -1
                            }
                        }
                        if (p.hasOutHandle) {
                            val outScreen = screen(p.outHandleX, p.outHandleY, original.transform)
                            val d = (outScreen - down.position).getDistance()
                            if (d < bestDist) {
                                bestDist = d
                                pointIndex = i
                                handle = 1
                            }
                        }
                    }
                }

                // Priority 2: Anchor Points
                if (pointIndex < 0) {
                    working.points.forEachIndexed { i, p ->
                        val anchorScreen = screen(p.x, p.y, original.transform)
                        val d = (anchorScreen - down.position).getDistance()
                        if (d < bestDist) {
                            bestDist = d
                            pointIndex = i
                            handle = 0
                        }
                    }
                }

                val (x, y) = local(down.position)

                // Priority 3: Path Segments (Curvature editing)
                if (pointIndex < 0 && working.points.size >= 2) {
                    val count = if (working.isClosed) working.points.size else working.points.size - 1
                    for (i in 0 until count) {
                        val p0 = working.points[i]
                        val p1 = working.points[(i + 1) % working.points.size]
                        val p0s = screen(p0.x, p0.y, original.transform)
                        val p1s = screen(p1.x, p1.y, original.transform)
                        val midS = (p0s + p1s) / 2f
                        if ((midS - down.position).getDistance() < hitRadius * 1.2f) {
                            pointIndex = i
                            handle = 2 // Bending curve
                            break
                        }
                    }
                }

                // Tool actions
                when (tool) {
                    VectorTool.ADD -> {
                        if (pointIndex < 0) {
                            val (added, selected) = VectorEditing.addPoint(working, x, y, hitRadius / scale / maxOf(abs(original.transform.scaleX), 0.0001f))
                            working = added
                            pointIndex = selected
                            handle = 2
                        }
                    }
                    VectorTool.REMOVE -> {
                        if (pointIndex >= 0) {
                            working = working.copy(points = working.points.filterIndexed { i, _ -> i != pointIndex })
                            pointIndex = -1
                        }
                    }
                    VectorTool.SMOOTH -> {
                        if (pointIndex >= 0) {
                            working = working.copy(points = working.points.mapIndexed { i, p -> if (i == pointIndex) VectorEditing.toggleSmooth(p) else p })
                        }
                    }
                    VectorTool.SELECT -> {
                        // Empty space touched in Select mode: DOES NOTHING
                    }
                }

                latestSelect(pointIndex.takeIf { it >= 0 })

                fun preview() {
                    paths[pi] = working
                    draft = original.withVectorPaths(paths.toList())
                    latestPreview(draft)
                }

                preview()
                down.consume()

                var cancelled = false
                try {
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } > 1) {
                            cancelled = true
                            break
                        }
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed && pointIndex >= 0 && (tool == VectorTool.SELECT || tool == VectorTool.ADD)) {
                            val (rawMx, rawMy) = local(change.position)
                            val p = working.points.getOrNull(pointIndex)
                            if (p != null) {
                                val (mx, my) = if (angleLock45) {
                                    VectorEditing.snapTo45Degrees(p.x, p.y, rawMx, rawMy)
                                } else {
                                    rawMx to rawMy
                                }
                                val next = when (handle) {
                                    -1 -> {
                                        if (independentHandles) {
                                            p.copy(inHandleX = mx, inHandleY = my)
                                        } else {
                                            // Mirrored handle move
                                            val dx = mx - p.x
                                            val dy = my - p.y
                                            p.copy(inHandleX = mx, inHandleY = my, outHandleX = p.x - dx, outHandleY = p.y - dy)
                                        }
                                    }
                                    1 -> {
                                        if (independentHandles) {
                                            p.copy(outHandleX = mx, outHandleY = my)
                                        } else {
                                            // Mirrored handle move
                                            val dx = mx - p.x
                                            val dy = my - p.y
                                            p.copy(outHandleX = mx, outHandleY = my, inHandleX = p.x - dx, inHandleY = p.y - dy)
                                        }
                                    }
                                    2 -> if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) VectorEditing.bend(p, mx, my) else p
                                    else -> VectorEditing.movePoint(p, mx, my)
                                }
                                working = working.copy(points = working.points.mapIndexed { i, v -> if (i == pointIndex) next else v })
                                preview()
                            }
                        }
                        change.consume()
                    } while (event.changes.any { it.pressed })

                    val result = original.withVectorPaths(paths.toList())
                    if (!cancelled && result != original) {
                        latestCommit(result)
                    }
                } finally {
                    draft = null
                    latestPreview(null)
                }
            }
        }) {
            path.points.forEachIndexed { i, p ->
                val at = screen(p.x, p.y, current.transform)
                if (p.hasInHandle) {
                    val h = screen(p.inHandleX, p.inHandleY, current.transform)
                    drawLine(colors.secondary, at, h, 1.dp.toPx())
                    drawCircle(colors.secondary, 5.dp.toPx(), h)
                }
                if (p.hasOutHandle) {
                    val h = screen(p.outHandleX, p.outHandleY, current.transform)
                    drawLine(colors.secondary, at, h, 1.dp.toPx())
                    drawCircle(colors.secondary, 5.dp.toPx(), h)
                }
                val radius = 6.dp.toPx()
                val isSelected = (i == selectedPoint)
                drawRect(if (isSelected) colors.primary else colors.surface, at - Offset(radius, radius), Size(radius * 2f, radius * 2f))
                drawRect(if (isSelected) Color(0xFF00E5FF) else colors.primary, at - Offset(radius, radius), Size(radius * 2f, radius * 2f), style = Stroke(1.5.dp.toPx()))
            }
        }

        // Top toolbar with status and Done
        Surface(Modifier.align(Alignment.TopCenter).padding(6.dp), shape = MaterialTheme.shapes.small, color = colors.surfaceContainer) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tool.hint, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = onDone) { Text("Done") }
            }
        }

        // V18 Contextual Floating Controls when an anchor point is selected
        if (selectedPoint != null && selectedPoint in path.points.indices) {
            val selectedBezierPoint = path.points[selectedPoint]
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp)
                    .testTag("vector_precision_controls"),
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Independent Handles Toggle
                    FilterChip(
                        selected = independentHandles,
                        onClick = { independentHandles = !independentHandles },
                        label = { Text("Independent", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("toggle_independent_handles")
                    )

                    // 2. Corner / Smooth Anchor Toggle
                    val isSmooth = selectedBezierPoint.hasInHandle || selectedBezierPoint.hasOutHandle
                    OutlinedButton(
                        onClick = {
                            val newPoint = if (isSmooth) VectorEditing.makeCorner(selectedBezierPoint) else VectorEditing.makeSmooth(selectedBezierPoint)
                            val updatedPath = path.copy(points = path.points.mapIndexed { idx, p -> if (idx == selectedPoint) newPoint else p })
                            val paths = current.editablePaths.toMutableList()
                            paths[index] = updatedPath
                            latestCommit(current.withVectorPaths(paths))
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("toggle_corner_smooth")
                    ) {
                        Text(if (isSmooth) "To Corner" else "To Smooth", style = MaterialTheme.typography.labelSmall)
                    }

                    // 3. 45-degree angle lock toggle
                    FilterChip(
                        selected = angleLock45,
                        onClick = { angleLock45 = !angleLock45 },
                        label = { Text("45° Snap", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("toggle_angle_lock")
                    )

                    // 4. Delete Point button
                    IconButton(
                        onClick = {
                            val updatedPath = path.copy(points = path.points.filterIndexed { idx, _ -> idx != selectedPoint })
                            val paths = current.editablePaths.toMutableList()
                            paths[index] = updatedPath
                            latestSelect(null)
                            latestCommit(current.withVectorPaths(paths))
                        },
                        modifier = Modifier.size(32.dp).testTag("delete_vector_point")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Point", tint = colors.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
