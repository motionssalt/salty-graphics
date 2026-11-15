package com.motionssalt.saltygraphics.uikit.overlays

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.model.GuideOrientation
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.SelectionGeometry
import com.motionssalt.saltygraphics.engine.model.SelectionHandle
import com.motionssalt.saltygraphics.engine.model.SelectionPoint
import com.motionssalt.saltygraphics.engine.model.Transform2D
import com.motionssalt.saltygraphics.uikit.registry.OverlayRenderContext
import com.motionssalt.saltygraphics.uikit.registry.SelectionOverlayBindings
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class SelectionHandleType {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    EDGE_TOP,
    EDGE_BOTTOM,
    EDGE_LEFT,
    EDGE_RIGHT,
    ROTATE_STEM,
    BODY
}

/**
 * Library-owned on-canvas selection bounding box, handles, smart guides, and rotation readout pill.
 * Supports both unified canvas gesture ownership (when CanvasView owns multi-touch) and
 * standalone interactive overlay gesture ownership (when handleGesturesInternally = true).
 */
@Composable
fun SaltySelectionBoxOverlay(
    context: OverlayRenderContext,
    bindings: SelectionOverlayBindings,
    modifier: Modifier = Modifier
) {
    val colors = SaltyTokens.colors
    val sizes = SaltyTokens.sizes
    val layer = context.layer
    if (layer.type == LayerType.ADJUSTMENT) return

    val latestContext by rememberUpdatedState(context)
    val latestBindings by rememberUpdatedState(bindings)
    var gestureActive by remember { mutableStateOf(false) }
    var gestureCommitted by remember { mutableStateOf(false) }

    DisposableEffect(context.document.id, layer.id) {
        onDispose {
            if (gestureActive && !gestureCommitted) {
                latestBindings.onCancelTransform()
            }
        }
    }

    val gestureModifier = if (bindings.handleGesturesInternally && !layer.isLocked) {
        Modifier.pointerInput(context.document.id, layer.id, layer.isLocked) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val ctx = latestContext
                val bnd = latestBindings
                val fit = if (size.width > 0 && size.height > 0) {
                    minOf(
                        size.width / ctx.canvasSize.width.toFloat(),
                        size.height / ctx.canvasSize.height.toFloat()
                    ) * 0.94f
                } else 1f
                val effZoom = fit * ctx.zoom
                val viewportCenter = Offset(size.width / 2f + ctx.pan.x, size.height / 2f + ctx.pan.y)
                fun docPt(screen: Offset): SelectionPoint =
                    SelectionPoint((screen.x - viewportCenter.x) / effZoom, (screen.y - viewportCenter.y) / effZoom)

                val downDoc = docPt(down.position)
                val startTransform = ctx.layer.transform
                val hitRadiusDoc = sizes.touchTargetRadius.toPx() / effZoom
                val handle = SelectionGeometry.hitHandle(
                    point = downDoc,
                    transform = startTransform,
                    bounds = bnd.bounds,
                    touchRadius = hitRadiusDoc,
                    rotateStemLength = sizes.rotateStemLength.toPx() / effZoom
                )
                if (handle == SelectionHandle.NONE) return@awaitEachGesture
                down.consume()

                gestureActive = true
                gestureCommitted = false
                var currentTransform = startTransform
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) {
                            val cancelled = event.changes.any { it.isConsumed }
                            if (cancelled) {
                                bnd.onCancelTransform()
                            } else if (currentTransform != startTransform) {
                                val actionIdentity = when (handle) {
                                    SelectionHandle.ROTATE -> ActionIdentity("Rotate", "Layer")
                                    SelectionHandle.BODY -> ActionIdentity("Move", "Layer")
                                    else -> ActionIdentity("Scale", "Layer")
                                }
                                gestureCommitted = true
                                bnd.onCommitTransform(actionIdentity, currentTransform)
                            }
                            break
                        }
                        if (pressed.size > 1) {
                            bnd.onCancelTransform()
                            break
                        }
                        val change = pressed.first()
                        val pt = docPt(change.position)
                        currentTransform = SelectionGeometry.drag(startTransform, bnd.bounds, handle, downDoc, pt)
                        if (SelectionGeometry.valid(currentTransform)) {
                            bnd.onPreviewTransform(currentTransform)
                        }
                        change.consume()
                    }
                } finally {
                    if (!gestureCommitted) {
                        bnd.onCancelTransform()
                    }
                    gestureActive = false
                }
            }
        }
    } else {
        Modifier
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().then(gestureModifier)) {
            val fit = if (size.width > 0 && size.height > 0) {
                minOf(
                    size.width / context.canvasSize.width.toFloat(),
                    size.height / context.canvasSize.height.toFloat()
                ) * 0.94f
            } else 1f
            val effectiveZoom = fit * context.zoom
            val center = Offset(size.width / 2f + context.pan.x, size.height / 2f + context.pan.y)
            val t = layer.transform
            val b = bindings.bounds
            val centerLocal = SelectionGeometry.world(b.center, t)
            val halfW = b.width * t.scaleX * effectiveZoom / 2f
            val halfH = b.height * t.scaleY * effectiveZoom / 2f

            withTransform({
                translate(center.x + centerLocal.x * effectiveZoom, center.y + centerLocal.y * effectiveZoom)
                rotate(t.rotationDegrees, pivot = Offset.Zero)
            }) {
                val origin = Offset(-halfW, -halfH)
                val dimensions = Size(halfW * 2, halfH * 2)
                drawRect(
                    colors.selectionOutlineContrast,
                    origin,
                    dimensions,
                    style = Stroke(sizes.selectionStrokeOuter.toPx())
                )
                drawRect(
                    colors.selectionHandle,
                    origin,
                    dimensions,
                    style = Stroke(sizes.selectionStrokeInner.toPx())
                )
                if (!layer.isLocked) {
                    fun handleRect(p: Offset, side: Float) {
                        val top = p - Offset(side / 2, side / 2)
                        drawRect(colors.selectionHandle, top, Size(side, side))
                        drawRect(
                            colors.selectionOutlineContrast,
                            top,
                            Size(side, side),
                            style = Stroke(sizes.selectionStrokeInner.toPx())
                        )
                    }
                    listOf(
                        Offset(-halfW, -halfH),
                        Offset(halfW, -halfH),
                        Offset(halfW, halfH),
                        Offset(-halfW, halfH)
                    ).forEach { handleRect(it, sizes.selectionCornerHandle.toPx()) }

                    listOf(Offset(0f, -halfH), Offset(0f, halfH))
                        .forEach { handleRect(it, sizes.selectionEdgeHandle.toPx()) }

                    if (layer.type == LayerType.TEXT && bindings.isTextEditing) {
                        fun textPillHandle(p: Offset, isVertical: Boolean) {
                            val pillW = if (isVertical) sizes.textPillShort.toPx() else sizes.textPillLong.toPx()
                            val pillH = if (isVertical) sizes.textPillLong.toPx() else sizes.textPillShort.toPx()
                            val top = p - Offset(pillW / 2f, pillH / 2f)
                            drawRoundRect(
                                colors.selectionOutlineContrast,
                                top,
                                Size(pillW, pillH),
                                CornerRadius(sizes.textPillCornerOuter.toPx())
                            )
                            val inset = sizes.textPillInset.toPx()
                            drawRoundRect(
                                colors.selectionTextFrame,
                                top + Offset(inset, inset),
                                Size(pillW - inset * 2f, pillH - inset * 2f),
                                CornerRadius(sizes.textPillCornerInner.toPx())
                            )
                        }
                        textPillHandle(Offset(-halfW, 0f), isVertical = true)
                        textPillHandle(Offset(halfW, 0f), isVertical = true)
                        textPillHandle(Offset(0f, -halfH), isVertical = false)
                        textPillHandle(Offset(0f, halfH), isVertical = false)
                    } else {
                        listOf(Offset(-halfW, 0f), Offset(halfW, 0f))
                            .forEach { handleRect(it, sizes.selectionEdgeHandle.toPx()) }
                    }

                    val rotate = Offset(0f, -halfH - sizes.rotateStemLength.toPx())
                    drawLine(colors.selectionOutlineContrast, Offset(0f, -halfH), rotate, sizes.selectionStrokeOuter.toPx())
                    drawLine(colors.selectionHandle, Offset(0f, -halfH), rotate, sizes.selectionStrokeInner.toPx())
                    drawCircle(colors.selectionOutlineContrast, sizes.rotateKnobOuterRadius.toPx(), rotate)
                    drawCircle(colors.selectionHandle, sizes.rotateKnobInnerRadius.toPx(), rotate)

                    if (bindings.isShapeCornerHandleEnabled) {
                        val rOffset = min(bindings.shapeCornerRadius * t.scaleX * effectiveZoom, min(halfW, halfH) * 0.8f)
                        val crCenter = Offset(-halfW + rOffset, -halfH + rOffset)
                        drawCircle(colors.selectionOutlineContrast, sizes.shapeHandleOuterRadius.toPx(), crCenter)
                        drawCircle(colors.selectionTextFrame, sizes.shapeHandleInnerRadius.toPx(), crCenter)
                    } else if (bindings.isShapeStarHandleEnabled) {
                        val innerR = halfH * bindings.starInnerRadiusRatio
                        val innerOffset = Offset(0f, -innerR)
                        drawCircle(colors.selectionOutlineContrast, sizes.shapeHandleOuterRadius.toPx(), innerOffset)
                        drawCircle(colors.selectionStarHandle, sizes.shapeHandleInnerRadius.toPx(), innerOffset)
                    }
                }
            }

            if (bindings.smartGuides.isNotEmpty()) {
                bindings.smartGuides.forEach { guide ->
                    if (guide.orientation == GuideOrientation.VERTICAL) {
                        val sx = center.x + guide.position * effectiveZoom
                        val sy1 = center.y + guide.start * effectiveZoom
                        val sy2 = center.y + guide.end * effectiveZoom
                        drawLine(
                            color = colors.smartGuideCrisp,
                            start = Offset(sx, sy1),
                            end = Offset(sx, sy2),
                            strokeWidth = sizes.smartGuideStroke.toPx()
                        )
                    } else {
                        val sy = center.y + guide.position * effectiveZoom
                        val sx1 = center.x + guide.start * effectiveZoom
                        val sx2 = center.x + guide.end * effectiveZoom
                        drawLine(
                            color = colors.smartGuideCrisp,
                            start = Offset(sx1, sy),
                            end = Offset(sx2, sy),
                            strokeWidth = sizes.smartGuideStroke.toPx()
                        )
                    }
                }
            }
        }

        val angleLabel = bindings.rotatingAngleLabel
        val pillPos = bindings.rotatingPillScreenPosition
        if (angleLabel != null && pillPos != null) {
            SaltyAngleReadoutPill(label = angleLabel, screenPosition = pillPos)
        }
    }
}

/**
 * On-canvas Selection Bounding Box overlay.
 * Follows V20: Scale bounding-box transforms around the object's center!
 * Supports 4 corner handles, 4 edge midpoint handles, top rotation stem with knob,
 * and 45-degree angle snapping.
 */
@Composable
fun SaltySelectionOverlay(
    bounds: Rect,
    rotationDeg: Float,
    onScaleAroundCenter: (scaleXDelta: Float, scaleYDelta: Float) -> Unit,
    onRotate: (newAngleDeg: Float) -> Unit,
    onTranslate: (dx: Float, dy: Float) -> Unit,
    onTransformCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false
) {
    val colors = SaltyTokens.colors
    var activeHandle by remember { mutableStateOf(SelectionHandleType.NONE) }
    val rad = Math.toRadians(rotationDeg.toDouble()).toFloat()
    val cosR = cos(rad)
    val sinR = sin(rad)
    val center = bounds.center

    fun toWorld(local: Offset): Offset {
        val dx = local.x - center.x
        val dy = local.y - center.y
        val rx = dx * cosR - dy * sinR
        val ry = dx * sinR + dy * cosR
        return Offset(center.x + rx, center.y + ry)
    }

    val tl = toWorld(Offset(bounds.left, bounds.top))
    val tr = toWorld(Offset(bounds.right, bounds.top))
    val bl = toWorld(Offset(bounds.left, bounds.bottom))
    val br = toWorld(Offset(bounds.right, bounds.bottom))
    val edgeTop = toWorld(Offset(center.x, bounds.top))
    val edgeBottom = toWorld(Offset(center.x, bounds.bottom))
    val edgeLeft = toWorld(Offset(bounds.left, center.y))
    val edgeRight = toWorld(Offset(bounds.right, center.y))
    val stemKnob = toWorld(Offset(center.x, bounds.top - 28.dp.value))

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(bounds, rotationDeg, isLocked) {
                if (isLocked) return@pointerInput
                detectDragGestures(
                    onDragStart = { pos ->
                        val hitDist = 24.dp.toPx()
                        fun hit(target: Offset) = (pos - target).getDistance() <= hitDist
                        activeHandle = when {
                            hit(stemKnob) -> SelectionHandleType.ROTATE_STEM
                            hit(tl) -> SelectionHandleType.TOP_LEFT
                            hit(tr) -> SelectionHandleType.TOP_RIGHT
                            hit(bl) -> SelectionHandleType.BOTTOM_LEFT
                            hit(br) -> SelectionHandleType.BOTTOM_RIGHT
                            hit(edgeTop) -> SelectionHandleType.EDGE_TOP
                            hit(edgeBottom) -> SelectionHandleType.EDGE_BOTTOM
                            hit(edgeLeft) -> SelectionHandleType.EDGE_LEFT
                            hit(edgeRight) -> SelectionHandleType.EDGE_RIGHT
                            bounds.contains(pos) -> SelectionHandleType.BODY
                            else -> SelectionHandleType.NONE
                        }
                    },
                    onDragEnd = {
                        val label = when (activeHandle) {
                            SelectionHandleType.ROTATE_STEM -> "Rotate Layer"
                            SelectionHandleType.BODY -> "Move Layer"
                            SelectionHandleType.NONE -> null
                            else -> "Scale Layer"
                        }
                        activeHandle = SelectionHandleType.NONE
                        if (label != null) onTransformCommit(label)
                    },
                    onDragCancel = {
                        activeHandle = SelectionHandleType.NONE
                    }
                ) { change, dragAmount ->
                    if (activeHandle == SelectionHandleType.NONE) return@detectDragGestures
                    change.consume()
                    when (activeHandle) {
                        SelectionHandleType.ROTATE_STEM -> {
                            val curAngle = Math.toDegrees(atan2((change.position.y - center.y).toDouble(), (change.position.x - center.x).toDouble())).toFloat()
                            val normalized = (curAngle + 90f + 360f) % 360f
                            val snapped = (Math.round(normalized / 45f) * 45f)
                            val finalAngle = if (kotlin.math.abs(normalized - snapped) < 3.5f) snapped else normalized
                            onRotate(finalAngle)
                        }
                        SelectionHandleType.BODY -> onTranslate(dragAmount.x, dragAmount.y)
                        SelectionHandleType.TOP_LEFT, SelectionHandleType.BOTTOM_RIGHT -> {
                            val delta = (dragAmount.x + dragAmount.y) / 200f
                            onScaleAroundCenter(delta, delta)
                        }
                        SelectionHandleType.TOP_RIGHT, SelectionHandleType.BOTTOM_LEFT -> {
                            val delta = (-dragAmount.x + dragAmount.y) / 200f
                            onScaleAroundCenter(delta, delta)
                        }
                        SelectionHandleType.EDGE_LEFT, SelectionHandleType.EDGE_RIGHT -> {
                            onScaleAroundCenter(dragAmount.x / 150f, 0f)
                        }
                        SelectionHandleType.EDGE_TOP, SelectionHandleType.EDGE_BOTTOM -> {
                            onScaleAroundCenter(0f, dragAmount.y / 150f)
                        }
                        SelectionHandleType.NONE -> {}
                    }
                }
            }
    ) {
        val strokeColor = if (isLocked) colors.outline else colors.selectionOutline
        val handleColor = colors.selectionHandle

        val path = Path().apply {
            moveTo(tl.x, tl.y)
            lineTo(tr.x, tr.y)
            lineTo(br.x, br.y)
            lineTo(bl.x, bl.y)
            close()
        }
        drawPath(path, strokeColor, style = Stroke(width = 1.5.dp.toPx()))

        if (!isLocked) {
            drawLine(color = strokeColor, start = edgeTop, end = stemKnob, strokeWidth = 1.5.dp.toPx())
            drawCircle(color = strokeColor, radius = 6.dp.toPx(), center = stemKnob)
            drawCircle(color = handleColor, radius = 4.dp.toPx(), center = stemKnob)
            listOf(tl, tr, bl, br).forEach { pt ->
                drawRect(
                    color = strokeColor,
                    topLeft = pt - Offset(5.dp.toPx(), 5.dp.toPx()),
                    size = Size(10.dp.toPx(), 10.dp.toPx())
                )
                drawRect(
                    color = handleColor,
                    topLeft = pt - Offset(3.5.dp.toPx(), 3.5.dp.toPx()),
                    size = Size(7.dp.toPx(), 7.dp.toPx())
                )
            }
            listOf(edgeTop, edgeBottom, edgeLeft, edgeRight).forEach { pt ->
                drawCircle(color = strokeColor, radius = 4.5.dp.toPx(), center = pt)
                drawCircle(color = handleColor, radius = 3.dp.toPx(), center = pt)
            }
        }
    }
}
