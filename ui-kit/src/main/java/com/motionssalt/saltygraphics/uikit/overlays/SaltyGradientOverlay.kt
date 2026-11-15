package com.motionssalt.saltygraphics.uikit.overlays

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens
import kotlin.math.*

enum class GradientDragTarget {
    NONE,
    START_HANDLE,
    END_HANDLE,
    CENTER_HANDLE,
    LINE_BODY
}

/**
 * On-canvas gradient overlay with independent handles.
 * - Dragging start handle moves ONLY the start.
 * - Dragging end handle moves ONLY the end.
 * - Dragging center handle moves ONLY the center.
 * - Dragging the gradient line moves the whole gradient.
 * - Each drag action commits one clearly labelled history action.
 */
@Composable
fun SaltyGradientOverlay(
    startOffset: Offset,
    endOffset: Offset,
    centerOffset: Offset?,
    onStartChange: (Offset) -> Unit,
    onEndChange: (Offset) -> Unit,
    onCenterChange: ((Offset) -> Unit)? = null,
    onTranslateBoth: (dx: Float, dy: Float) -> Unit,
    onDragCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    isRadialOrAngular: Boolean = false
) {
    val colors = SaltyTokens.colors
    var activeTarget by remember { mutableStateOf(GradientDragTarget.NONE) }
    val liveStart by rememberUpdatedState(startOffset)
    val liveEnd by rememberUpdatedState(endOffset)
    val liveCenter by rememberUpdatedState(centerOffset)
    val hitRadiusPx = with(LocalDensity.current) { 24.dp.toPx() }

    fun distance(p1: Offset, p2: Offset): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
        val l2 = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
        if (l2 == 0f) return distance(p, a)
        val t = (((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2).coerceIn(0f, 1f)
        val projection = Offset(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
        return distance(p, projection)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("gradient_handles")
            .pointerInput(isRadialOrAngular) {
                detectDragGestures(
                    onDragStart = { pos ->
                        val s = liveStart
                        val e = liveEnd
                        val c = liveCenter
                        activeTarget = when {
                            c != null && distance(pos, c) <= hitRadiusPx -> GradientDragTarget.CENTER_HANDLE
                            distance(pos, s) <= hitRadiusPx -> GradientDragTarget.START_HANDLE
                            distance(pos, e) <= hitRadiusPx -> GradientDragTarget.END_HANDLE
                            distanceToSegment(pos, s, e) <= hitRadiusPx -> GradientDragTarget.LINE_BODY
                            else -> GradientDragTarget.NONE
                        }
                    },
                    onDragEnd = {
                        val label = when (activeTarget) {
                            GradientDragTarget.START_HANDLE -> "Adjust Gradient Start"
                            GradientDragTarget.END_HANDLE -> "Adjust Gradient End"
                            GradientDragTarget.CENTER_HANDLE -> "Move Gradient Center"
                            GradientDragTarget.LINE_BODY -> "Move Gradient"
                            GradientDragTarget.NONE -> null
                        }
                        activeTarget = GradientDragTarget.NONE
                        if (label != null) onDragCommit(label)
                    },
                    onDragCancel = {
                        activeTarget = GradientDragTarget.NONE
                    }
                ) { change, dragAmount ->
                    if (activeTarget == GradientDragTarget.NONE) return@detectDragGestures
                    change.consume()

                    when (activeTarget) {
                        GradientDragTarget.START_HANDLE -> onStartChange(liveStart + dragAmount)
                        GradientDragTarget.END_HANDLE -> onEndChange(liveEnd + dragAmount)
                        GradientDragTarget.CENTER_HANDLE -> {
                            val c = liveCenter
                            if (c != null && onCenterChange != null) {
                                onCenterChange(c + dragAmount)
                            }
                        }
                        GradientDragTarget.LINE_BODY -> onTranslateBoth(dragAmount.x, dragAmount.y)
                        GradientDragTarget.NONE -> {}
                    }
                }
            }
    ) {
        drawLine(
            color = colors.primary.copy(alpha = 0.8f),
            start = startOffset,
            end = endOffset,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        centerOffset?.let { c ->
            drawCircle(color = Color.White, radius = 7.dp.toPx(), center = c)
            drawCircle(color = colors.secondary, radius = 5.dp.toPx(), center = c)
        }

        drawCircle(color = Color.White, radius = 9.dp.toPx(), center = startOffset)
        drawCircle(color = colors.primary, radius = 6.5.dp.toPx(), center = startOffset)

        drawCircle(color = Color.White, radius = 9.dp.toPx(), center = endOffset)
        drawCircle(color = colors.primary, radius = 6.5.dp.toPx(), center = endOffset)
    }
}

/**
 * Full layer-aware on-canvas gradient overlay (V0 + V7).
 * Computes world/screen handle positions from layer bounds and writes independent normalized
 * start/end/center coordinates into GradientData without rotating/squashing unintended handles.
 */
@Composable
fun SaltyGradientLayerOverlay(
    layer: Layer,
    canvasSize: CanvasSize,
    zoom: Float,
    pan: Offset,
    onPreview: (FillData) -> Unit,
    onCommit: (FillData) -> Unit,
    modifier: Modifier = Modifier,
    onGestureCommit: ((ActionIdentity, FillData) -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    val colors = SaltyTokens.colors
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val live by rememberUpdatedState(layer)
    val preview by rememberUpdatedState(onPreview)
    val commit by rememberUpdatedState(onCommit)
    val gestureCommit by rememberUpdatedState(onGestureCommit)
    val cancel by rememberUpdatedState(onCancel)
    val livePan by rememberUpdatedState(pan)
    val fit = if (viewport.width > 0 && viewport.height > 0) {
        min(viewport.width / canvasSize.width.toFloat(), viewport.height / canvasSize.height.toFloat()) * 0.94f
    } else 1f
    val scale by rememberUpdatedState(fit * zoom)

    fun bounds(l: Layer): SelectionBounds = when (l.type) {
        LayerType.TEXT -> {
            val t = l.textData ?: TextLayerData()
            SelectionBounds(-t.boxWidth / 2f, -t.boxHeight / 2f, t.boxWidth / 2f, t.boxHeight / 2f)
        }
        LayerType.VECTOR -> {
            val pts = l.editablePaths.flatMap { path ->
                path.points.flatMap { p ->
                    buildList {
                        add(SelectionPoint(p.x, p.y))
                        if (p.hasInHandle) add(SelectionPoint(p.inHandleX, p.inHandleY))
                        if (p.hasOutHandle) add(SelectionPoint(p.outHandleX, p.outHandleY))
                    }
                }
            }
            if (pts.isEmpty()) SelectionBounds(-l.localWidth / 2f, -l.localHeight / 2f, l.localWidth / 2f, l.localHeight / 2f)
            else SelectionBounds.enclosing(pts)
        }
        else -> SelectionBounds(-l.localWidth / 2f, -l.localHeight / 2f, l.localWidth / 2f, l.localHeight / 2f)
    }

    fun gradientEndpoints(g: GradientData, b: SelectionBounds): Triple<SelectionPoint, SelectionPoint, SelectionPoint> {
        val cx = b.left + b.width * g.centerX
        val cy = b.top + b.height * g.centerY
        val c = SelectionPoint(cx, cy)
        return if (g.type == GradientType.LINEAR && g.hasExplicitEndpoints()) {
            val s = SelectionPoint(b.left + b.width * g.startX!!, b.top + b.height * g.startY!!)
            val e = SelectionPoint(b.left + b.width * g.endX!!, b.top + b.height * g.endY!!)
            val mid = SelectionPoint((s.x + e.x) / 2f, (s.y + e.y) / 2f)
            Triple(mid, s, e)
        } else {
            val aspect = (b.width / b.height.coerceAtLeast(1f)).coerceAtLeast(0.01f)
            val (nsx, nsy, nex, ney) = g.resolvedEndpoints(aspect)
            val s = SelectionPoint(b.left + b.width * nsx, b.top + b.height * nsy)
            val e = SelectionPoint(b.left + b.width * nex, b.top + b.height * ney)
            Triple(c, s, e)
        }
    }

    fun handles(l: Layer): List<SelectionPoint> {
        val b = bounds(l)
        val g = l.effectiveFill.gradient
        if (g.type == GradientType.FREEFORM) {
            return g.freeformPoints.map { SelectionPoint(b.left + b.width * it.x, b.top + b.height * it.y) }
        }
        val (c, s, e) = gradientEndpoints(g, b)
        return if (g.type == GradientType.LINEAR) listOf(c, s, e) else listOf(c, e)
    }

    fun screen(p: SelectionPoint, l: Layer): Offset {
        val w = SelectionGeometry.world(p, l.transform)
        return Offset(viewport.width / 2f + livePan.x + w.x * scale, viewport.height / 2f + livePan.y + w.y * scale)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewport = it }
            .testTag("gradient_handles")
            .pointerInput(layer.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val original = live
                    if (original.isLocked || scale <= 0f) return@awaitEachGesture
                    val points = handles(original)
                    val hit = points.indices.minByOrNull { (screen(points[it], original) - down.position).getDistance() }
                        ?.takeIf { (screen(points[it], original) - down.position).getDistance() <= 24.dp.toPx() }
                        ?: return@awaitEachGesture
                    down.consume()
                    var changed = false
                    var cancelled = true
                    var completed = false
                    var working = original.effectiveFill
                    val startWorldCenter = points.firstOrNull()
                    try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val c = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (event.changes.count { it.pressed } > 1) {
                            cancelled = true
                            break
                        }
                        if (!c.pressed) {
                            cancelled = c.isConsumed
                            break
                        }
                        val worldX = (c.position.x - viewport.width / 2f - livePan.x) / scale
                        val worldY = (c.position.y - viewport.height / 2f - livePan.y) / scale
                        val xy = VectorEditing.toLocal(worldX, worldY, original.transform)
                        val b = bounds(original)
                        val bw = b.width.coerceAtLeast(1f)
                        val bh = b.height.coerceAtLeast(1f)
                        val g = original.effectiveFill.gradient
                        val p = SelectionPoint(xy.first, xy.second)

                        val updated = when {
                            g.type == GradientType.FREEFORM -> g.copy(
                                freeformPoints = g.freeformPoints.mapIndexed { i, fp ->
                                    if (i == hit) fp.copy(
                                        x = ((p.x - b.left) / bw).coerceIn(0f, 1f),
                                        y = ((p.y - b.top) / bh).coerceIn(0f, 1f)
                                    ) else fp
                                }
                            )
                            g.type == GradientType.LINEAR -> {
                                val curStart = points[1]
                                val curEnd = points[2]
                                val curCenter = points[0]
                                when (hit) {
                                    0 -> {
                                        // Center moves the center & translates start/end together by delta
                                        val dx = p.x - (startWorldCenter?.x ?: curCenter.x)
                                        val dy = p.y - (startWorldCenter?.y ?: curCenter.y)
                                        val ns = SelectionPoint(curStart.x + dx, curStart.y + dy)
                                        val ne = SelectionPoint(curEnd.x + dx, curEnd.y + dy)
                                        g.copy(
                                            centerX = ((p.x - b.left) / bw).coerceIn(-1f, 2f),
                                            centerY = ((p.y - b.top) / bh).coerceIn(-1f, 2f),
                                            startX = (ns.x - b.left) / bw,
                                            startY = (ns.y - b.top) / bh,
                                            endX = (ne.x - b.left) / bw,
                                            endY = (ne.y - b.top) / bh
                                        )
                                    }
                                    1 -> {
                                        // Start moves ONLY start; end stays fixed!
                                        val sxN = (p.x - b.left) / bw
                                        val syN = (p.y - b.top) / bh
                                        val exN = (curEnd.x - b.left) / bw
                                        val eyN = (curEnd.y - b.top) / bh
                                        val dx = curEnd.x - p.x
                                        val dy = curEnd.y - p.y
                                        g.copy(
                                            startX = sxN,
                                            startY = syN,
                                            endX = exN,
                                            endY = eyN,
                                            centerX = ((sxN + exN) / 2f).coerceIn(0f, 1f),
                                            centerY = ((syN + eyN) / 2f).coerceIn(0f, 1f),
                                            angleDegrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat(),
                                            radius = (hypot(dx, dy) / (2f * hypot(bw, bh))).coerceIn(0.01f, 2f)
                                        )
                                    }
                                    else -> {
                                        // End moves ONLY end; start stays fixed!
                                        val sxN = (curStart.x - b.left) / bw
                                        val syN = (curStart.y - b.top) / bh
                                        val exN = (p.x - b.left) / bw
                                        val eyN = (p.y - b.top) / bh
                                        val dx = p.x - curStart.x
                                        val dy = p.y - curStart.y
                                        g.copy(
                                            startX = sxN,
                                            startY = syN,
                                            endX = exN,
                                            endY = eyN,
                                            centerX = ((sxN + exN) / 2f).coerceIn(0f, 1f),
                                            centerY = ((syN + eyN) / 2f).coerceIn(0f, 1f),
                                            angleDegrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat(),
                                            radius = (hypot(dx, dy) / (2f * hypot(bw, bh))).coerceIn(0.01f, 2f)
                                        )
                                    }
                                }
                            }
                            hit == 0 -> g.copy(
                                centerX = ((p.x - b.left) / bw).coerceIn(0f, 1f),
                                centerY = ((p.y - b.top) / bh).coerceIn(0f, 1f)
                            )
                            else -> {
                                val oldCenter = SelectionPoint(b.left + bw * g.centerX, b.top + bh * g.centerY)
                                val delta = p - oldCenter
                                val divisor = if (g.type == GradientType.REFLECTED) hypot(bw, bh) else max(bw, bh)
                                g.copy(
                                    angleDegrees = Math.toDegrees(atan2(delta.y.toDouble(), delta.x.toDouble())).toFloat(),
                                    radius = (hypot(delta.x, delta.y) / divisor).coerceIn(0.01f, 2f)
                                )
                            }
                        }
                        working = original.effectiveFill.copy(gradient = updated)
                        changed = true
                        preview(working)
                        c.consume()
                    }
                    if (changed && !cancelled) {
                        completed = true
                        val identity = when {
                            original.effectiveFill.gradient.type == GradientType.FREEFORM -> ActionIdentity("Move", "gradient", "point")
                            original.effectiveFill.gradient.type == GradientType.LINEAR && hit == 1 -> ActionIdentity("Move", "gradient", "start")
                            original.effectiveFill.gradient.type == GradientType.LINEAR && hit == 2 -> ActionIdentity("Move", "gradient", "end")
                            original.effectiveFill.gradient.type == GradientType.LINEAR -> ActionIdentity("Move", "gradient")
                            hit == 0 -> ActionIdentity("Move", "gradient", "center")
                            else -> ActionIdentity("Change", "gradient", "radius")
                        }
                        val labelled = gestureCommit
                        if (labelled != null) labelled(identity, working) else commit(working)
                    }
                    } finally {
                        // Also runs if the page/target is disposed during pointer input.
                        // A draft is never a committed document or an autosave.
                        if (changed && !completed) {
                            val rollback = cancel
                            if (rollback != null) rollback() else preview(original.effectiveFill)
                        }
                    }
                }
            }
    ) {
        val points = handles(layer).map { screen(it, layer) }
        if (layer.effectiveFill.gradient.type != GradientType.FREEFORM && points.size >= 2) {
            drawLine(colors.onPrimary.copy(alpha = 0.9f), points.last(), points[if (points.size == 3) 1 else 0], 2.dp.toPx())
        }
        points.forEachIndexed { i, p ->
            drawCircle(Color.Black, 9.dp.toPx(), p)
            drawCircle(if (i == 0) colors.secondary else colors.selectionHandle, 6.dp.toPx(), p)
        }
    }
}
