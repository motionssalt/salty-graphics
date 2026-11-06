package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import com.motionssalt.saltygraphics.data.FontManager
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.theme.*

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
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val fit = if (viewport.width > 0 && viewport.height > 0)
        minOf(viewport.width / document.canvasSize.width.toFloat(), viewport.height / document.canvasSize.height.toFloat()) * .94f else 1f
    val effectiveZoom = fit * zoom
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { viewport = it }
            .background(colors.background)
            .pointerInput(zoom, panOffset, viewport) {
                // Two-finger pinch-to-zoom and pan
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    val newZoom = (zoom * gestureZoom).coerceIn(0.1f, 10f)
                    val newPan = panOffset + pan
                    onZoomPanChange(newZoom, newPan)
                }
            }
            .pointerInput(document.layers, selectedLayerIds, zoom, panOffset, viewport) {
                detectTapGestures { tapOffset ->
                    // Convert screen tap to canvas space
                    val canvasCenterX = size.width / 2f + panOffset.x
                    val canvasCenterY = size.height / 2f + panOffset.y

                    val canvasW = document.canvasSize.width.toFloat()
                    val canvasH = document.canvasSize.height.toFloat()
                    val canvasLeft = canvasCenterX - (canvasW * effectiveZoom / 2f)
                    val canvasTop = canvasCenterY - (canvasH * effectiveZoom / 2f)

                    val xInCanvas = (tapOffset.x - canvasLeft) / effectiveZoom
                    val yInCanvas = (tapOffset.y - canvasTop) / effectiveZoom

                    // Hit test from topmost layer to bottom
                    var hitLayer: Layer? = null
                    for (layer in document.activeLayers.reversed()) {
                        if (!layer.isVisible || layer.isLocked) continue
                        val lx = layer.transform.translationX + (canvasW / 2f)
                        val ly = layer.transform.translationY + (canvasH / 2f)
                        val halfW = (layer.localWidth * layer.transform.scaleX) / 2f
                        val halfH = (layer.localHeight * layer.transform.scaleY) / 2f

                        if (xInCanvas in (lx - halfW)..(lx + halfW) &&
                            yInCanvas in (ly - halfH)..(ly + halfH)
                        ) {
                            hitLayer = layer
                            break
                        }
                    }
                    onSelectLayer(hitLayer?.id)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f + panOffset.x, size.height / 2f + panOffset.y)
            val canvasW = document.canvasSize.width.toFloat()
            val canvasH = document.canvasSize.height.toFloat()

            // Draw Canvas Document Boundary
            val scaledW = canvasW * effectiveZoom
            val scaledH = canvasH * effectiveZoom
            val topLeft = Offset(center.x - (scaledW / 2f), center.y - (scaledH / 2f))

            // Checkerboard belongs only to the preview, never to exported pixels.
            clipRect(topLeft.x, topLeft.y, topLeft.x + scaledW, topLeft.y + scaledH) {
                val cell = 12.dp.toPx()
                val cols = (scaledW / cell).toInt().coerceAtMost(2000) + 1
                val rows = (scaledH / cell).toInt().coerceAtMost(2000) + 1
                for (row in 0..rows) for (col in 0..cols) {
                    drawRect(if ((row + col) % 2 == 0) Color.LightGray else Color.Gray,
                        Offset(topLeft.x + col * cell, topLeft.y + row * cell), Size(cell, cell))
                }
                drawRect(safeColor(document.editorSettings.backgroundColor), topLeft, Size(scaledW, scaledH))
            }

            // Optional Gridlines
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

            // Draw Canvas Border Outline
            drawRect(
                color = colors.outline,
                topLeft = topLeft,
                size = Size(scaledW, scaledH),
                style = Stroke(width = 1.5f)
            )

            // Render Layers in order (bottom to top)
            for (layer in document.activeLayers) {
                if (!layer.isVisible) continue

                val layerCenterX = center.x + (layer.transform.translationX * effectiveZoom)
                val layerCenterY = center.y + (layer.transform.translationY * effectiveZoom)

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
                            val shapeColor = safeColor(shape.fillColor)

                            when (shape.type) {
                                ShapeType.CIRCLE -> {
                                    drawOval(
                                        color = shapeColor,
                                        topLeft = Offset(-halfW, -halfH),
                                        size = Size(shape.width, shape.height)
                                    )
                                }
                                ShapeType.ROUNDED_RECTANGLE -> {
                                    drawRoundRect(
                                        color = shapeColor,
                                        topLeft = Offset(-halfW, -halfH),
                                        size = Size(shape.width, shape.height),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(shape.cornerRadius)
                                    )
                                }
                                else -> {
                                    drawRect(
                                        color = shapeColor,
                                        topLeft = Offset(-halfW, -halfH),
                                        size = Size(shape.width, shape.height)
                                    )
                                }
                            }
                        }
                        LayerType.TEXT -> {
                            val textData = layer.textData ?: TextLayerData()
                            drawIntoCanvas { canvas ->
                                val nativeCanvas = canvas.nativeCanvas
                                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                    textSize = textData.fontSize
                                    color = safeColor(textData.color).toArgb()
                                    letterSpacing = textData.letterSpacing / 1000f
                                    textAlign = when (textData.justification) {
                                        TextJustification.LEFT -> android.graphics.Paint.Align.LEFT
                                        TextJustification.RIGHT -> android.graphics.Paint.Align.RIGHT
                                        else -> android.graphics.Paint.Align.CENTER
                                    }
                                    isFakeBoldText = textData.isBold
                                }

                                val allFonts = FontManager.getAllFonts(context)
                                val matchedFont = allFonts.find { it.familyName.equals(textData.fontFamily, ignoreCase = true) }
                                paint.typeface = FontManager.getTypeface(context, matchedFont)

                                val pathLayer = if (textData.followPathLayerId != null) document.findLayerById(textData.followPathLayerId!!) else null
                                val vectorPath = pathLayer?.vectorPathData ?: pathLayer?.shapeData?.toVectorPath()

                                if (vectorPath != null && vectorPath.points.size >= 2) {
                                    val path = nativeVectorPath(vectorPath)
                                    // Referenced geometry includes the target layer transform, expressed in text-local coordinates.
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
                                        val outlinePaint = android.graphics.Paint(paint).apply {
                                            style = android.graphics.Paint.Style.STROKE
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
                                        val outlinePaint = android.graphics.Paint(paint).apply {
                                            style = android.graphics.Paint.Style.STROKE
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
                            drawRect(
                                color = colors.surfaceVariant,
                                topLeft = Offset(-150f, -150f),
                                size = Size(300f, 300f)
                            )
                        }
                        LayerType.ADJUSTMENT -> {
                            drawRect(
                                color = colors.primary.copy(alpha = 0.15f),
                                topLeft = Offset(-scaledW / 2f, -scaledH / 2f),
                                size = Size(scaledW, scaledH)
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
                            // Show harmless quarantined placeholder
                            drawRoundRect(
                                color = colors.error.copy(alpha = 0.25f),
                                topLeft = Offset(-120f, -60f),
                                size = Size(240f, 120f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
                            )
                            drawRoundRect(
                                color = colors.error,
                                topLeft = Offset(-120f, -60f),
                                size = Size(240f, 120f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }

                // Bounding Box & Handles for Selected Layer (Section 3.6)
                if (selectedLayerIds.contains(layer.id)) {
                    val halfW = (layer.localWidth * layer.transform.scaleX * effectiveZoom) / 2f
                    val halfH = (layer.localHeight * layer.transform.scaleY * effectiveZoom) / 2f

                    withTransform({
                        translate(layerCenterX, layerCenterY)
                        rotate(layer.transform.rotationDegrees, pivot = Offset.Zero)
                    }) {
                        // Bounding Box Outline
                        drawRect(
                            color = colors.primary,
                            topLeft = Offset(-halfW, -halfH),
                            size = Size(halfW * 2f, halfH * 2f),
                            style = Stroke(width = 2f)
                        )

                        // Corner Scale Handles
                        val handleRadius = 7f
                        val corners = listOf(
                            Offset(-halfW, -halfH),
                            Offset(halfW, -halfH),
                            Offset(halfW, halfH),
                            Offset(-halfW, halfH)
                        )
                        for (corner in corners) {
                            drawCircle(color = Color.White, radius = handleRadius, center = corner)
                            drawCircle(color = colors.primary, radius = handleRadius, center = corner, style = Stroke(width = 2f))
                        }

                        // Top Rotation Handle
                        val rotateHandleCenter = Offset(0f, -halfH - 24f)
                        drawLine(
                            color = colors.primary,
                            start = Offset(0f, -halfH),
                            end = rotateHandleCenter,
                            strokeWidth = 1.5f
                        )
                        drawCircle(color = colors.secondary, radius = handleRadius, center = rotateHandleCenter)
                    }
                }
            }
        }
    }
}
