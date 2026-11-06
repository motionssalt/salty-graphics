package com.motionssalt.saltygraphics.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint as NativePaint
import android.graphics.Path as NativePath
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.data.FontManager
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.media.MediaCache
import com.motionssalt.saltygraphics.theme.*
import kotlin.math.*

private enum class HandleType {
    NONE,
    BODY,
    ROTATE,
    CORNER_TOP_LEFT,
    CORNER_TOP_RIGHT,
    CORNER_BOTTOM_RIGHT,
    CORNER_BOTTOM_LEFT,
    EDGE_TOP,
    EDGE_BOTTOM,
    EDGE_LEFT,
    EDGE_RIGHT
}

fun toComposeBlendMode(mode: com.motionssalt.saltygraphics.engine.model.BlendMode): androidx.compose.ui.graphics.BlendMode {
    return when (mode) {
        com.motionssalt.saltygraphics.engine.model.BlendMode.NORMAL -> androidx.compose.ui.graphics.BlendMode.SrcOver
        com.motionssalt.saltygraphics.engine.model.BlendMode.DARKEN -> androidx.compose.ui.graphics.BlendMode.Darken
        com.motionssalt.saltygraphics.engine.model.BlendMode.MULTIPLY -> androidx.compose.ui.graphics.BlendMode.Multiply
        com.motionssalt.saltygraphics.engine.model.BlendMode.COLOR_BURN -> androidx.compose.ui.graphics.BlendMode.ColorBurn
        com.motionssalt.saltygraphics.engine.model.BlendMode.LINEAR_BURN -> androidx.compose.ui.graphics.BlendMode.Plus
        com.motionssalt.saltygraphics.engine.model.BlendMode.LIGHTEN -> androidx.compose.ui.graphics.BlendMode.Lighten
        com.motionssalt.saltygraphics.engine.model.BlendMode.SCREEN -> androidx.compose.ui.graphics.BlendMode.Screen
        com.motionssalt.saltygraphics.engine.model.BlendMode.COLOR_DODGE -> androidx.compose.ui.graphics.BlendMode.ColorDodge
        com.motionssalt.saltygraphics.engine.model.BlendMode.LINEAR_DODGE -> androidx.compose.ui.graphics.BlendMode.Plus
        com.motionssalt.saltygraphics.engine.model.BlendMode.OVERLAY -> androidx.compose.ui.graphics.BlendMode.Overlay
        com.motionssalt.saltygraphics.engine.model.BlendMode.SOFT_LIGHT -> androidx.compose.ui.graphics.BlendMode.Softlight
        com.motionssalt.saltygraphics.engine.model.BlendMode.HARD_LIGHT -> androidx.compose.ui.graphics.BlendMode.Hardlight
        com.motionssalt.saltygraphics.engine.model.BlendMode.VIVID_LIGHT -> androidx.compose.ui.graphics.BlendMode.Overlay
        com.motionssalt.saltygraphics.engine.model.BlendMode.LINEAR_LIGHT -> androidx.compose.ui.graphics.BlendMode.Hardlight
        com.motionssalt.saltygraphics.engine.model.BlendMode.PIN_LIGHT -> androidx.compose.ui.graphics.BlendMode.Darken
        com.motionssalt.saltygraphics.engine.model.BlendMode.DIFFERENCE -> androidx.compose.ui.graphics.BlendMode.Difference
        com.motionssalt.saltygraphics.engine.model.BlendMode.EXCLUSION -> androidx.compose.ui.graphics.BlendMode.Exclusion
        com.motionssalt.saltygraphics.engine.model.BlendMode.SUBTRACT -> androidx.compose.ui.graphics.BlendMode.Difference
        com.motionssalt.saltygraphics.engine.model.BlendMode.DIVIDE -> androidx.compose.ui.graphics.BlendMode.Screen
        com.motionssalt.saltygraphics.engine.model.BlendMode.HUE -> androidx.compose.ui.graphics.BlendMode.Hue
        com.motionssalt.saltygraphics.engine.model.BlendMode.SATURATION -> androidx.compose.ui.graphics.BlendMode.Saturation
        com.motionssalt.saltygraphics.engine.model.BlendMode.COLOR -> androidx.compose.ui.graphics.BlendMode.Color
        com.motionssalt.saltygraphics.engine.model.BlendMode.LUMINOSITY -> androidx.compose.ui.graphics.BlendMode.Luminosity
    }
}

@Composable
fun CanvasView(
    document: ProjectDocument,
    selectedLayerIds: Set<String>,
    zoom: Float,
    panOffset: Offset,
    showGrid: Boolean,
    gridSize: Int,
    onZoomPanChange: (newZoom: Float, newPan: Offset) -> Unit,
    onSelectLayer: (String?) -> Unit,
    onUpdateLayerTransform: (String, Transform2D) -> Unit,
    onCommitLayerTransform: ((String, Transform2D) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val density = LocalDensity.current
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    val fit = if (viewport.width > 0 && viewport.height > 0)
        minOf(viewport.width / document.canvasSize.width.toFloat(), viewport.height / document.canvasSize.height.toFloat()) * .94f else 1f
    val effectiveZoom = fit * zoom

    // Active rotating angle pill readout
    var rotatingAnglePill by remember { mutableStateOf<String?>(null) }
    var rotatingPillPosition by remember { mutableStateOf<Offset?>(null) }

    // Use rememberUpdatedState to avoid gesture coroutine teardown on rapid zoom/pan changes
    val currentZoom by rememberUpdatedState(zoom)
    val currentPan by rememberUpdatedState(panOffset)
    val currentDoc by rememberUpdatedState(document)
    val currentSelectedIds by rememberUpdatedState(selectedLayerIds)
    val currentFit by rememberUpdatedState(fit)

    val selectedLayer = remember(document, selectedLayerIds) {
        document.findLayerById(selectedLayerIds.firstOrNull() ?: "")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { viewport = it }
            .background(colors.background)
            .pointerInput(Unit) {
                val touchSlop = viewConfiguration.touchSlop
                val handleRadiusPx = 28.dp.toPx()

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val initialDownPos = down.position
                    val activeDoc = currentDoc
                    val curZoom = currentZoom
                    val curPan = currentPan
                    val curFit = currentFit
                    val effZoom = curFit * curZoom

                    val canvasW = activeDoc.canvasSize.width.toFloat()
                    val canvasH = activeDoc.canvasSize.height.toFloat()
                    val centerScreen = Offset(size.width / 2f + curPan.x, size.height / 2f + curPan.y)
                    val canvasLeft = centerScreen.x - (canvasW * effZoom / 2f)
                    val canvasTop = centerScreen.y - (canvasH * effZoom / 2f)

                    // Find currently selected layer if any
                    val selLayer = activeDoc.findLayerById(currentSelectedIds.firstOrNull() ?: "")

                    var activeHandle = HandleType.NONE
                    var initialTransform = selLayer?.transform ?: Transform2D()
                    var initialLocalWidth = selLayer?.localWidth ?: 400f
                    var initialLocalHeight = selLayer?.localHeight ?: 400f
                    var initialLayerCenter = Offset.Zero

                    if (selLayer != null && selLayer.isVisible && !selLayer.isLocked) {
                        val lx = centerScreen.x + (selLayer.transform.translationX * effZoom)
                        val ly = centerScreen.y + (selLayer.transform.translationY * effZoom)
                        initialLayerCenter = Offset(lx, ly)
                        val halfW = (initialLocalWidth * selLayer.transform.scaleX * effZoom) / 2f
                        val halfH = (initialLocalHeight * selLayer.transform.scaleY * effZoom) / 2f

                        // Convert screen touch to layer-local coordinates
                        val dx = initialDownPos.x - lx
                        val dy = initialDownPos.y - ly
                        val rad = -Math.toRadians(selLayer.transform.rotationDegrees.toDouble())
                        val localX = (dx * cos(rad) - dy * sin(rad)).toFloat()
                        val localY = (dx * sin(rad) + dy * cos(rad)).toFloat()

                        // 1. Rotation knob: center is at (0, -halfH - 28dp)
                        val rotCenterY = -halfH - 28.dp.toPx()
                        val distToRot = hypot(localX, localY - rotCenterY)
                        if (distToRot <= handleRadiusPx) {
                            activeHandle = HandleType.ROTATE
                        }
                        // 2. Corner handles
                        else if (hypot(localX - (-halfW), localY - (-halfH)) <= handleRadiusPx) {
                            activeHandle = HandleType.CORNER_TOP_LEFT
                        } else if (hypot(localX - halfW, localY - (-halfH)) <= handleRadiusPx) {
                            activeHandle = HandleType.CORNER_TOP_RIGHT
                        } else if (hypot(localX - halfW, localY - halfH) <= handleRadiusPx) {
                            activeHandle = HandleType.CORNER_BOTTOM_RIGHT
                        } else if (hypot(localX - (-halfW), localY - halfH) <= handleRadiusPx) {
                            activeHandle = HandleType.CORNER_BOTTOM_LEFT
                        }
                        // 3. Edge handles
                        else if (hypot(localX, localY - (-halfH)) <= handleRadiusPx) {
                            activeHandle = HandleType.EDGE_TOP
                        } else if (hypot(localX, localY - halfH) <= handleRadiusPx) {
                            activeHandle = HandleType.EDGE_BOTTOM
                        } else if (hypot(localX - (-halfW), localY) <= handleRadiusPx) {
                            activeHandle = HandleType.EDGE_LEFT
                        } else if (hypot(localX - halfW, localY) <= handleRadiusPx) {
                            activeHandle = HandleType.EDGE_RIGHT
                        }
                        // 4. Inside layer bounding box
                        else if (abs(localX) <= halfW && abs(localY) <= halfH) {
                            activeHandle = HandleType.BODY
                        }
                    }

                    var targetLayerId = selLayer?.id
                    var isPanningCanvas = false
                    var totalDragDistance = 0f
                    var lastPosition = initialDownPos
                    var currentTransform = initialTransform

                    // If neither handle nor selected layer was hit, hit test other layers
                    if (activeHandle == HandleType.NONE) {
                        val touchXInCanvas = (initialDownPos.x - canvasLeft) / effZoom
                        val touchYInCanvas = (initialDownPos.y - canvasTop) / effZoom

                        var hitOther: Layer? = null
                        for (layer in activeDoc.activeLayers.reversed()) {
                            if (!layer.isVisible || layer.isLocked) continue
                            val lx = layer.transform.translationX + (canvasW / 2f)
                            val ly = layer.transform.translationY + (canvasH / 2f)
                            val halfW = (layer.localWidth * layer.transform.scaleX) / 2f
                            val halfH = (layer.localHeight * layer.transform.scaleY) / 2f
                            if (touchXInCanvas in (lx - halfW)..(lx + halfW) &&
                                touchYInCanvas in (ly - halfH)..(ly + halfH)
                            ) {
                                hitOther = layer
                                break
                            }
                        }

                        if (hitOther != null) {
                            onSelectLayer(hitOther.id)
                            targetLayerId = hitOther.id
                            activeHandle = HandleType.BODY
                            initialTransform = hitOther.transform
                            initialLocalWidth = hitOther.localWidth
                            initialLocalHeight = hitOther.localHeight
                            currentTransform = hitOther.transform
                        } else {
                            isPanningCanvas = true
                        }
                    }

                    // Gesture tracking loop
                    var isTwoFinger = false
                    var prevPinchDist = 0f
                    var prevCentroid = initialDownPos

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedPointers = event.changes.filter { it.pressed }

                        if (pressedPointers.isEmpty()) {
                            // Touch released / finished
                            if (activeHandle != HandleType.NONE && targetLayerId != null) {
                                (onCommitLayerTransform ?: onUpdateLayerTransform).invoke(targetLayerId, currentTransform)
                            } else if (isPanningCanvas && totalDragDistance < touchSlop) {
                                // Tap in empty canvas space deselects layer
                                onSelectLayer(null)
                            }
                            rotatingAnglePill = null
                            rotatingPillPosition = null
                            break
                        }

                        if (pressedPointers.size >= 2) {
                            // Two-finger pinch-to-zoom and pan
                            isTwoFinger = true
                            rotatingAnglePill = null
                            rotatingPillPosition = null

                            val p1 = pressedPointers[0].position
                            val p2 = pressedPointers[1].position
                            val curDist = hypot(p1.x - p2.x, p1.y - p2.y)
                            val centroid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                            if (prevPinchDist > 0f) {
                                val zoomFactor = (curDist / prevPinchDist).coerceIn(0.8f, 1.25f)
                                val newZoom = (currentZoom * zoomFactor).coerceIn(0.1f, 10f)
                                val panDelta = centroid - prevCentroid
                                onZoomPanChange(newZoom, currentPan + panDelta)
                            }
                            prevPinchDist = curDist
                            prevCentroid = centroid
                            event.changes.forEach { it.consume() }
                            continue
                        }

                        if (isTwoFinger) {
                            // Recovering from 2 fingers to 1 finger
                            continue
                        }

                        // Single finger active
                        val change = pressedPointers.first()
                        val currentPos = change.position
                        val delta = currentPos - lastPosition
                        totalDragDistance += hypot(delta.x, delta.y)

                        if (isPanningCanvas) {
                            if (totalDragDistance >= touchSlop) {
                                onZoomPanChange(currentZoom, currentPan + delta)
                                change.consume()
                            }
                        } else if (targetLayerId != null) {
                            change.consume()
                            val curEffZoom = currentFit * currentZoom

                            when (activeHandle) {
                                HandleType.BODY -> {
                                    // 1:1 screen delta to canvas translation
                                    val canvasDx = delta.x / curEffZoom
                                    val canvasDy = delta.y / curEffZoom
                                    currentTransform = currentTransform.copy(
                                        translationX = currentTransform.translationX + canvasDx,
                                        translationY = currentTransform.translationY + canvasDy
                                    )
                                    onUpdateLayerTransform(targetLayerId, currentTransform)
                                }
                                HandleType.ROTATE -> {
                                    // Rotation around layer center
                                    val angleRad = atan2(currentPos.y - initialLayerCenter.y, currentPos.x - initialLayerCenter.x)
                                    var angleDeg = (Math.toDegrees(angleRad.toDouble()).toFloat() + 90f) % 360f
                                    if (angleDeg < 0f) angleDeg += 360f

                                    // Snapping within 3 degrees of 0, 45, 90, 135, 180, 225, 270, 315
                                    val snapAngles = floatArrayOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f, 360f)
                                    var displayAngle = angleDeg
                                    for (snap in snapAngles) {
                                        if (abs(angleDeg - snap) <= 3f) {
                                            displayAngle = if (snap == 360f) 0f else snap
                                            angleDeg = displayAngle
                                            break
                                        }
                                    }

                                    currentTransform = currentTransform.copy(rotationDegrees = angleDeg)
                                    onUpdateLayerTransform(targetLayerId, currentTransform)

                                    rotatingAnglePill = "${displayAngle.roundToInt()}°"
                                    rotatingPillPosition = Offset(currentPos.x, currentPos.y - 48.dp.toPx())
                                }
                                HandleType.CORNER_TOP_LEFT, HandleType.CORNER_TOP_RIGHT,
                                HandleType.CORNER_BOTTOM_RIGHT, HandleType.CORNER_BOTTOM_LEFT -> {
                                    // Corner scaling
                                    val initialDist = hypot(
                                        (initialLocalWidth * initialTransform.scaleX * curEffZoom) / 2f,
                                        (initialLocalHeight * initialTransform.scaleY * curEffZoom) / 2f
                                    )
                                    val curDist = hypot(currentPos.x - initialLayerCenter.x, currentPos.y - initialLayerCenter.y)
                                    val scaleRatio = if (initialDist > 0f) curDist / initialDist else 1f
                                    val newScaleX = (initialTransform.scaleX * scaleRatio).coerceIn(0.05f, 20f)
                                    val newScaleY = if (initialTransform.isScaleLinked) newScaleX
                                        else (initialTransform.scaleY * scaleRatio).coerceIn(0.05f, 20f)

                                    currentTransform = currentTransform.copy(scaleX = newScaleX, scaleY = newScaleY)
                                    onUpdateLayerTransform(targetLayerId, currentTransform)
                                }
                                HandleType.EDGE_LEFT, HandleType.EDGE_RIGHT -> {
                                    // Width scaling
                                    val initialHalfW = (initialLocalWidth * initialTransform.scaleX * curEffZoom) / 2f
                                    val dx = currentPos.x - initialLayerCenter.x
                                    val dy = currentPos.y - initialLayerCenter.y
                                    val rad = -Math.toRadians(initialTransform.rotationDegrees.toDouble())
                                    val curHalfW = abs((dx * cos(rad) - dy * sin(rad)).toFloat())
                                    val scaleRatio = if (initialHalfW > 0f) curHalfW / initialHalfW else 1f
                                    val newScaleX = (initialTransform.scaleX * scaleRatio).coerceIn(0.05f, 20f)

                                    currentTransform = currentTransform.copy(scaleX = newScaleX)
                                    onUpdateLayerTransform(targetLayerId, currentTransform)
                                }
                                HandleType.EDGE_TOP, HandleType.EDGE_BOTTOM -> {
                                    // Height scaling
                                    val initialHalfH = (initialLocalHeight * initialTransform.scaleY * curEffZoom) / 2f
                                    val dx = currentPos.x - initialLayerCenter.x
                                    val dy = currentPos.y - initialLayerCenter.y
                                    val rad = -Math.toRadians(initialTransform.rotationDegrees.toDouble())
                                    val curHalfH = abs((dx * sin(rad) + dy * cos(rad)).toFloat())
                                    val scaleRatio = if (initialHalfH > 0f) curHalfH / initialHalfH else 1f
                                    val newScaleY = (initialTransform.scaleY * scaleRatio).coerceIn(0.05f, 20f)

                                    currentTransform = currentTransform.copy(scaleY = newScaleY)
                                    onUpdateLayerTransform(targetLayerId, currentTransform)
                                }
                                else -> {}
                            }
                        }
                        lastPosition = currentPos
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f + panOffset.x, size.height / 2f + panOffset.y)
            val canvasW = document.canvasSize.width.toFloat()
            val canvasH = document.canvasSize.height.toFloat()

            // Scaled canvas boundary
            val scaledW = canvasW * effectiveZoom
            val scaledH = canvasH * effectiveZoom
            val topLeft = Offset(center.x - (scaledW / 2f), center.y - (scaledH / 2f))

            // Checkerboard background
            clipRect(topLeft.x, topLeft.y, topLeft.x + scaledW, topLeft.y + scaledH) {
                val cell = 12.dp.toPx()
                val cols = (scaledW / cell).toInt().coerceAtMost(2000) + 1
                val rows = (scaledH / cell).toInt().coerceAtMost(2000) + 1
                for (row in 0..rows) for (col in 0..cols) {
                    drawRect(
                        if ((row + col) % 2 == 0) Color.LightGray else Color.Gray,
                        Offset(topLeft.x + col * cell, topLeft.y + row * cell),
                        Size(cell, cell)
                    )
                }
                drawRect(safeColor(document.editorSettings.backgroundColor), topLeft, Size(scaledW, scaledH))
            }

            // Gridlines
            if (showGrid && gridSize > 0) {
                val gridSpacing = gridSize * effectiveZoom
                if (gridSpacing > 4f) {
                    var x = topLeft.x
                    while (x <= topLeft.x + scaledW) {
                        drawLine(
                            color = safeColor(document.editorSettings.gridColor),
                            start = Offset(x, topLeft.y),
                            end = Offset(x, topLeft.y + scaledH),
                            strokeWidth = 1f
                        )
                        x += gridSpacing
                    }
                    var y = topLeft.y
                    while (y <= topLeft.y + scaledH) {
                        drawLine(
                            color = safeColor(document.editorSettings.gridColor),
                            start = Offset(topLeft.x, y),
                            end = Offset(topLeft.x + scaledW, y),
                            strokeWidth = 1f
                        )
                        y += gridSpacing
                    }
                }
            }

            // Canvas Border
            drawRect(
                color = colors.outline,
                topLeft = topLeft,
                size = Size(scaledW, scaledH),
                style = Stroke(width = 1.5f)
            )

            // Render Layers Bottom to Top
            for (layer in document.activeLayers) {
                if (!layer.isVisible) continue

                val layerCenterX = center.x + (layer.transform.translationX * effectiveZoom)
                val layerCenterY = center.y + (layer.transform.translationY * effectiveZoom)
                val composeBlend = toComposeBlendMode(layer.blendMode)

                withTransform({
                    translate(layerCenterX, layerCenterY)
                    rotate(layer.transform.rotationDegrees, pivot = Offset.Zero)
                    scale(layer.transform.scaleX * effectiveZoom, layer.transform.scaleY * effectiveZoom, pivot = Offset.Zero)
                }) {
                    when (layer.type) {
                        LayerType.SHAPE -> {
                            val shape = layer.shapeData ?: ShapeData()
                            val halfW = shape.width / 2f
                            val halfH = shape.height / 2f
                            val shapeFillColor = safeColor(shape.fillColor).copy(alpha = layer.opacity)
                            val shapeStrokeColor = safeColor(shape.strokeColor).copy(alpha = layer.opacity)

                            when (shape.type) {
                                ShapeType.CIRCLE -> {
                                    if (shape.hasFill) {
                                        drawOval(
                                            color = shapeFillColor,
                                            topLeft = Offset(-halfW, -halfH),
                                            size = Size(shape.width, shape.height),
                                            blendMode = composeBlend
                                        )
                                    }
                                    if (shape.hasStroke && shape.strokeWidth > 0) {
                                        drawOval(
                                            color = shapeStrokeColor,
                                            topLeft = Offset(-halfW, -halfH),
                                            size = Size(shape.width, shape.height),
                                            style = Stroke(width = shape.strokeWidth),
                                            blendMode = composeBlend
                                        )
                                    }
                                }
                                ShapeType.ROUNDED_RECTANGLE -> {
                                    val r = androidx.compose.ui.geometry.CornerRadius(shape.cornerRadius)
                                    if (shape.hasFill) {
                                        drawRoundRect(
                                            color = shapeFillColor,
                                            topLeft = Offset(-halfW, -halfH),
                                            size = Size(shape.width, shape.height),
                                            cornerRadius = r,
                                            blendMode = composeBlend
                                        )
                                    }
                                    if (shape.hasStroke && shape.strokeWidth > 0) {
                                        drawRoundRect(
                                            color = shapeStrokeColor,
                                            topLeft = Offset(-halfW, -halfH),
                                            size = Size(shape.width, shape.height),
                                            cornerRadius = r,
                                            style = Stroke(width = shape.strokeWidth),
                                            blendMode = composeBlend
                                        )
                                    }
                                }
                                else -> {
                                    if (shape.hasFill) {
                                        drawRect(
                                            color = shapeFillColor,
                                            topLeft = Offset(-halfW, -halfH),
                                            size = Size(shape.width, shape.height),
                                            blendMode = composeBlend
                                        )
                                    }
                                    if (shape.hasStroke && shape.strokeWidth > 0) {
                                        drawRect(
                                            color = shapeStrokeColor,
                                            topLeft = Offset(-halfW, -halfH),
                                            size = Size(shape.width, shape.height),
                                            style = Stroke(width = shape.strokeWidth),
                                            blendMode = composeBlend
                                        )
                                    }
                                }
                            }
                        }
                        LayerType.TEXT -> {
                            val textData = layer.textData ?: TextLayerData()
                            drawIntoCanvas { canvas ->
                                val nativeCanvas = canvas.nativeCanvas
                                val paint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                                    textSize = textData.fontSize
                                    color = safeColor(textData.color).toArgb()
                                    alpha = (layer.opacity * 255).roundToInt().coerceIn(0, 255)
                                    letterSpacing = textData.letterSpacing / 1000f
                                    textAlign = when (textData.justification) {
                                        TextJustification.LEFT -> NativePaint.Align.LEFT
                                        TextJustification.RIGHT -> NativePaint.Align.RIGHT
                                        else -> NativePaint.Align.CENTER
                                    }
                                    isFakeBoldText = textData.isBold
                                    if (textData.isItalic) textSkewX = -0.25f
                                }
                                val allFonts = FontManager.getAllFonts(context)
                                val matchedFont = allFonts.find { it.familyName.equals(textData.fontFamily, ignoreCase = true) }
                                paint.typeface = FontManager.getTypeface(context, matchedFont)

                                val pathLayer = if (textData.followPathLayerId != null) document.findLayerById(textData.followPathLayerId!!) else null
                                val vectorPath = pathLayer?.vectorPathData ?: pathLayer?.shapeData?.toVectorPath()
                                if (vectorPath != null && vectorPath.points.size >= 2) {
                                    val path = nativeVectorPath(vectorPath)
                                    val targetMatrix = android.graphics.Matrix().apply {
                                        setTranslate(pathLayer!!.transform.translationX, pathLayer.transform.translationY)
                                        preRotate(pathLayer.transform.rotationDegrees)
                                        preScale(pathLayer.transform.scaleX, pathLayer.transform.scaleY)
                                    }
                                    val textMatrix = android.graphics.Matrix().apply {
                                        setTranslate(layer.transform.translationX, layer.transform.translationY)
                                        preRotate(layer.transform.rotationDegrees)
                                        preScale(layer.transform.scaleX, layer.transform.scaleY)
                                    }
                                    val inverse = android.graphics.Matrix()
                                    if (textMatrix.invert(inverse)) {
                                        inverse.preConcat(targetMatrix)
                                        path.transform(inverse)
                                    }
                                    val measure = android.graphics.PathMeasure(path, false)
                                    val pathLen = measure.length
                                    val hOffset = pathLen * textData.pathOffsetPercent
                                    if (textData.hasOutline) {
                                        val outlinePaint = NativePaint(paint).apply {
                                            style = NativePaint.Style.STROKE
                                            strokeWidth = textData.outlineWidth
                                            color = safeColor(textData.outlineColor).toArgb()
                                        }
                                        nativeCanvas.drawTextOnPath(textData.text, path, hOffset, 0f, outlinePaint)
                                    }
                                    nativeCanvas.drawTextOnPath(textData.text, path, hOffset, 0f, paint)
                                } else {
                                    val x = when (textData.justification) {
                                        TextJustification.LEFT -> -textData.boxWidth / 2f
                                        TextJustification.RIGHT -> textData.boxWidth / 2f
                                        else -> 0f
                                    }
                                    val y = (paint.descent() + paint.ascent()) / -2f
                                    if (textData.hasOutline) {
                                        val outlinePaint = NativePaint(paint).apply {
                                            style = NativePaint.Style.STROKE
                                            strokeWidth = textData.outlineWidth
                                            color = safeColor(textData.outlineColor).toArgb()
                                        }
                                        nativeCanvas.drawText(textData.text, x, y, outlinePaint)
                                    }
                                    nativeCanvas.drawText(textData.text, x, y, paint)
                                }
                            }
                        }
                        LayerType.MEDIA -> {
                            val mediaAsset = layer.mediaAssetPath
                            val bmp = if (mediaAsset != null) {
                                MediaCache.getOrLoad(context, document.id, mediaAsset)
                            } else null

                            val mw = layer.mediaWidth ?: 300f
                            val mh = layer.mediaHeight ?: 300f
                            val halfW = mw / 2f
                            val halfH = mh / 2f

                            if (bmp != null) {
                                drawIntoCanvas { canvas ->
                                    val paint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                                        alpha = (layer.opacity * 255).roundToInt().coerceIn(0, 255)
                                    }
                                    val dst = RectF(-halfW, -halfH, halfW, halfH)
                                    canvas.nativeCanvas.drawBitmap(bmp, null, dst, paint)
                                }
                            } else {
                                drawRect(
                                    color = colors.surfaceVariant.copy(alpha = layer.opacity),
                                    topLeft = Offset(-halfW, -halfH),
                                    size = Size(mw, mh),
                                    blendMode = composeBlend
                                )
                            }
                        }
                        LayerType.ADJUSTMENT -> {
                            // Adjustment layer affects underlying layers via compositing
                            val adjPaint = safeColor(0x80808080L).copy(alpha = layer.opacity)
                            drawRect(
                                color = adjPaint,
                                topLeft = Offset(-scaledW / 2f, -scaledH / 2f),
                                size = Size(scaledW, scaledH),
                                blendMode = composeBlend
                            )
                        }
                        LayerType.VECTOR -> {
                            drawIntoCanvas { canvas ->
                                layer.editablePaths.forEach { data ->
                                    drawNativeVector(canvas.nativeCanvas, data, layer.opacity)
                                }
                            }
                        }
                        LayerType.GROUP -> Unit
                        LayerType.QUARANTINED -> {
                            drawRoundRect(
                                color = colors.error.copy(alpha = 0.25f),
                                topLeft = Offset(-120f, -60f),
                                size = Size(240f, 120f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
                            )
                        }
                    }
                }

                // Bounding Box & Handles for Selected Layer
                if (selectedLayerIds.contains(layer.id)) {
                    val halfW = (layer.localWidth * layer.transform.scaleX * effectiveZoom) / 2f
                    val halfH = (layer.localHeight * layer.transform.scaleY * effectiveZoom) / 2f

                    withTransform({
                        translate(layerCenterX, layerCenterY)
                        rotate(layer.transform.rotationDegrees, pivot = Offset.Zero)
                    }) {
                        // 1. Crisp Bounding Box Outline
                        drawRect(
                            color = colors.primary,
                            topLeft = Offset(-halfW, -halfH),
                            size = Size(halfW * 2f, halfH * 2f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // 2. Corner Scale Handles (4 corners)
                        val cornerRadius = 6.dp.toPx()
                        val corners = listOf(
                            Offset(-halfW, -halfH),
                            Offset(halfW, -halfH),
                            Offset(halfW, halfH),
                            Offset(-halfW, halfH)
                        )
                        for (corner in corners) {
                            drawCircle(color = Color.White, radius = cornerRadius, center = corner)
                            drawCircle(color = colors.primary, radius = cornerRadius, center = corner, style = Stroke(width = 2.dp.toPx()))
                        }

                        // 3. Edge Midpoint Handles (width/height scaling)
                        val edgeHandleSize = 8.dp.toPx()
                        val edges = listOf(
                            Offset(0f, -halfH),
                            Offset(0f, halfH),
                            Offset(-halfW, 0f),
                            Offset(halfW, 0f)
                        )
                        for (edge in edges) {
                            drawCircle(color = Color.White, radius = edgeHandleSize / 2f, center = edge)
                            drawCircle(color = colors.secondary, radius = edgeHandleSize / 2f, center = edge, style = Stroke(width = 1.5.dp.toPx()))
                        }

                        // 4. Top Rotation Handle with Stem
                        val stemLen = 28.dp.toPx()
                        val rotateHandleCenter = Offset(0f, -halfH - stemLen)
                        drawLine(
                            color = colors.primary,
                            start = Offset(0f, -halfH),
                            end = rotateHandleCenter,
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawCircle(color = colors.primary, radius = 7.dp.toPx(), center = rotateHandleCenter)
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = rotateHandleCenter)
                    }
                }
            }
        }

        // Live Rotation Angle Readout Pill
        if (rotatingAnglePill != null && rotatingPillPosition != null) {
            Surface(
                color = colors.primary,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            (rotatingPillPosition!!.x - 30.dp.toPx()).roundToInt(),
                            rotatingPillPosition!!.y.roundToInt()
                        )
                    }
            ) {
                Text(
                    text = rotatingAnglePill!!,
                    color = colors.background,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
