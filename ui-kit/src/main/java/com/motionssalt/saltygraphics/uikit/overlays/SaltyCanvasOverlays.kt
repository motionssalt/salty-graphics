package com.motionssalt.saltygraphics.uikit.overlays

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.uikit.structure.SaltyChip
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextStyle
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextTone
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextWeight
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens
import kotlin.math.*

enum class SaltyVectorToolMode(val label: String, val hint: String) {
    SELECT("Select", "Tap a point or handle; drag to move"),
    ADD("Add point", "Tap a curve or empty space; drag a new point to bend"),
    REMOVE("Remove point", "Tap a square point to remove it"),
    SMOOTH("Corner/Smooth", "Tap a square point to switch corner or smooth")
}

/**
 * Floating loupe overlay shown ONLY during active canvas eyedropper sampling.
 */
@Composable
fun SaltyEyedropperLoupeOverlay(
    touchPosition: Offset,
    sampledColor: Long,
    magnificationBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val loupeRadius = 50.dp
    val strokeWidth = 3.dp

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = touchPosition - Offset(0f, 60.dp.toPx())
            val radiusPx = loupeRadius.toPx()

            if (magnificationBitmap != null) {
                val path = Path().apply {
                    addOval(Rect(center, radiusPx))
                }
                clipPath(path) {
                    drawImage(
                        image = magnificationBitmap.asImageBitmap(),
                        dstOffset = IntOffset((center.x - radiusPx).toInt(), (center.y - radiusPx).toInt()),
                        dstSize = IntSize((radiusPx * 2).toInt(), (radiusPx * 2).toInt()),
                        filterQuality = FilterQuality.None
                    )
                }
            }

            drawCircle(Color.White, radius = radiusPx, center = center, style = Stroke(strokeWidth.toPx()))
            drawCircle(Color.Black, radius = radiusPx + 1.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))

            drawCircle(Color(sampledColor.toInt()), radius = 8.dp.toPx(), center = center)
            drawCircle(Color.White, radius = 8.dp.toPx(), center = center, style = Stroke(1.5f))
        }
    }
}

/**
 * Live angle readout badge anchored near the active rotation handle on canvas.
 */
@Composable
fun SaltyAngleReadoutPill(
    label: String,
    screenPosition: Offset,
    modifier: Modifier = Modifier
) {
    val colors = SaltyTokens.colors
    Surface(
        color = colors.primary,
        shape = SaltyTokens.shapes.lg,
        tonalElevation = SaltyTokens.elevation.md,
        modifier = modifier.offset {
            IntOffset((screenPosition.x - 30.dp.toPx()).roundToInt(), screenPosition.y.roundToInt())
        }
    ) {
        SaltyText(
            text = label,
            style = SaltyTextStyle.LABEL_SMALL,
            tone = SaltyTextTone.ON_PRIMARY,
            weight = SaltyTextWeight.BOLD,
            modifier = Modifier.padding(horizontal = SaltyTokens.spacing.sm, vertical = SaltyTokens.spacing.xs)
        )
    }
}

/**
 * On-canvas vector path & bezier handle manipulation overlay.
 */
@Composable
fun SaltyVectorCanvasOverlay(
    layer: Layer,
    canvasSize: CanvasSize,
    pathIndex: Int,
    selectedPoint: Int?,
    tool: SaltyVectorToolMode,
    zoom: Float,
    pan: Offset,
    onSelectPoint: (Int?) -> Unit,
    onPreview: (Layer?) -> Unit,
    onCommit: (Layer) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var draft by remember(layer.id) { mutableStateOf<Layer?>(null) }
    val latestLayer by rememberUpdatedState(layer)
    val latestSelect by rememberUpdatedState(onSelectPoint)
    val latestPreview by rememberUpdatedState(onPreview)
    val latestCommit by rememberUpdatedState(onCommit)
    val colors = SaltyTokens.colors

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

    Box(modifier.fillMaxSize().onSizeChanged { viewport = it }) {
        Canvas(
            Modifier
                .fillMaxSize()
                .testTag("vector_handles")
                .pointerInput(layer.id, pathIndex, tool, scale, pan, independentHandles, angleLock45) {
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

                        if (tool == SaltyVectorToolMode.SELECT) {
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
                                    handle = 2
                                    break
                                }
                            }
                        }

                        when (tool) {
                            SaltyVectorToolMode.ADD -> {
                                if (pointIndex < 0) {
                                    val (added, selected) = VectorEditing.addPoint(
                                        working, x, y,
                                        hitRadius / scale / maxOf(abs(original.transform.scaleX), 0.0001f)
                                    )
                                    working = added
                                    pointIndex = selected
                                    handle = 2
                                }
                            }
                            SaltyVectorToolMode.REMOVE -> {
                                if (pointIndex >= 0) {
                                    working = working.copy(points = working.points.filterIndexed { i, _ -> i != pointIndex })
                                    pointIndex = -1
                                }
                            }
                            SaltyVectorToolMode.SMOOTH -> {
                                if (pointIndex >= 0) {
                                    working = working.copy(
                                        points = working.points.mapIndexed { i, p ->
                                            if (i == pointIndex) VectorEditing.toggleSmooth(p) else p
                                        }
                                    )
                                }
                            }
                            SaltyVectorToolMode.SELECT -> {}
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
                                if (change.pressed && pointIndex >= 0 &&
                                    (tool == SaltyVectorToolMode.SELECT || tool == SaltyVectorToolMode.ADD)
                                ) {
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
                                                    val dx = mx - p.x
                                                    val dy = my - p.y
                                                    p.copy(
                                                        inHandleX = mx, inHandleY = my,
                                                        outHandleX = p.x - dx, outHandleY = p.y - dy
                                                    )
                                                }
                                            }
                                            1 -> {
                                                if (independentHandles) {
                                                    p.copy(outHandleX = mx, outHandleY = my)
                                                } else {
                                                    val dx = mx - p.x
                                                    val dy = my - p.y
                                                    p.copy(
                                                        outHandleX = mx, outHandleY = my,
                                                        inHandleX = p.x - dx, inHandleY = p.y - dy
                                                    )
                                                }
                                            }
                                            2 -> if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                                VectorEditing.bend(p, mx, my)
                                            } else p
                                            else -> VectorEditing.movePoint(p, mx, my)
                                        }
                                        working = working.copy(
                                            points = working.points.mapIndexed { i, v -> if (i == pointIndex) next else v }
                                        )
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
                }
        ) {
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
                drawRect(
                    if (isSelected) colors.snapGuideGrid else colors.primary,
                    at - Offset(radius, radius),
                    Size(radius * 2f, radius * 2f),
                    style = Stroke(1.5.dp.toPx())
                )
            }
        }

        // Top compact status bar during active vector point editing
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(SaltyTokens.spacing.xs),
            shape = SaltyTokens.shapes.sm,
            color = colors.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = SaltyTokens.spacing.sm, vertical = SaltyTokens.spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SaltyText(tool.hint, style = SaltyTextStyle.LABEL_SMALL, modifier = Modifier.weight(1f))
                TextButton(onClick = onDone) { Text("Done", color = colors.primary) }
            }
        }

        // Contextual anchor point precision strip
        if (selectedPoint != null && selectedPoint in path.points.indices) {
            val selectedBezierPoint = path.points[selectedPoint]
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp)
                    .testTag("vector_precision_controls"),
                shape = SaltyTokens.shapes.xl,
                color = colors.surface,
                tonalElevation = SaltyTokens.elevation.md,
                shadowElevation = SaltyTokens.elevation.lg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = SaltyTokens.spacing.md, vertical = SaltyTokens.spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaltyChip(
                        label = "Independent",
                        selected = independentHandles,
                        onClick = { independentHandles = !independentHandles },
                        modifier = Modifier.testTag("toggle_independent_handles")
                    )

                    val isSmooth = selectedBezierPoint.hasInHandle || selectedBezierPoint.hasOutHandle
                    OutlinedButton(
                        onClick = {
                            val newPoint = if (isSmooth) VectorEditing.makeCorner(selectedBezierPoint) else VectorEditing.makeSmooth(selectedBezierPoint)
                            val updatedPath = path.copy(points = path.points.mapIndexed { idx, p -> if (idx == selectedPoint) newPoint else p })
                            val paths = current.editablePaths.toMutableList()
                            paths[index] = updatedPath
                            latestCommit(current.withVectorPaths(paths))
                        },
                        contentPadding = PaddingValues(horizontal = SaltyTokens.spacing.sm, vertical = SaltyTokens.spacing.xs),
                        modifier = Modifier.testTag("toggle_corner_smooth")
                    ) {
                        Text(if (isSmooth) "To Corner" else "To Smooth", style = SaltyTokens.typography.labelSmall)
                    }

                    SaltyChip(
                        label = "45° Snap",
                        selected = angleLock45,
                        onClick = { angleLock45 = !angleLock45 },
                        modifier = Modifier.testTag("toggle_angle_lock")
                    )

                    IconButton(
                        onClick = {
                            val updatedPath = path.copy(points = path.points.filterIndexed { idx, _ -> idx != selectedPoint })
                            val paths = current.editablePaths.toMutableList()
                            paths[index] = updatedPath
                            latestSelect(null)
                            latestCommit(current.withVectorPaths(paths))
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_vector_point")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Point", tint = colors.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
