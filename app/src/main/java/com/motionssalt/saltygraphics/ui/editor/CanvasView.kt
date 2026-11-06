package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SaltyDarkBackground)
            .pointerInput(Unit) {
                // Two-finger pinch-to-zoom and pan
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    val newZoom = (zoom * gestureZoom).coerceIn(0.1f, 10f)
                    val newPan = panOffset + pan
                    onZoomPanChange(newZoom, newPan)
                }
            }
            .pointerInput(document.layers, selectedLayerIds) {
                detectTapGestures { tapOffset ->
                    // Convert screen tap to canvas space
                    val canvasCenterX = size.width / 2f + panOffset.x
                    val canvasCenterY = size.height / 2f + panOffset.y

                    val canvasW = document.canvasSize.width.toFloat()
                    val canvasH = document.canvasSize.height.toFloat()
                    val canvasLeft = canvasCenterX - (canvasW * zoom / 2f)
                    val canvasTop = canvasCenterY - (canvasH * zoom / 2f)

                    val xInCanvas = (tapOffset.x - canvasLeft) / zoom
                    val yInCanvas = (tapOffset.y - canvasTop) / zoom

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
            val scaledW = canvasW * zoom
            val scaledH = canvasH * zoom
            val topLeft = Offset(center.x - (scaledW / 2f), center.y - (scaledH / 2f))

            // Canvas Background
            drawRect(
                color = Color.Black,
                topLeft = topLeft,
                size = Size(scaledW, scaledH)
            )

            // Optional Gridlines
            if (showGrid && gridSize > 0) {
                val gridSpacing = gridSize * zoom
                if (gridSpacing > 4f) {
                    var x = topLeft.x
                    while (x <= topLeft.x + scaledW) {
                        drawLine(
                            color = SaltyOutlineDark.copy(alpha = 0.4f),
                            start = Offset(x, topLeft.y),
                            end = Offset(x, topLeft.y + scaledH),
                            strokeWidth = 1f
                        )
                        x += gridSpacing
                    }
                    var y = topLeft.y
                    while (y <= topLeft.y + scaledH) {
                        drawLine(
                            color = SaltyOutlineDark.copy(alpha = 0.4f),
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
                color = SaltyOutlineDark,
                topLeft = topLeft,
                size = Size(scaledW, scaledH),
                style = Stroke(width = 1.5f)
            )

            // Render Layers in order (bottom to top)
            for (layer in document.activeLayers) {
                if (!layer.isVisible) continue

                val layerCenterX = center.x + (layer.transform.translationX * zoom)
                val layerCenterY = center.y + (layer.transform.translationY * zoom)

                withTransform({
                    translate(layerCenterX, layerCenterY)
                    rotate(layer.transform.rotationDegrees, pivot = Offset.Zero)
                    scale(layer.transform.scaleX * zoom, layer.transform.scaleY * zoom, pivot = Offset.Zero)
                }) {
                    when (layer.type) {
                        LayerType.SHAPE -> {
                            val shape = layer.shapeData ?: ShapeData()
                            val halfW = shape.width / 2f
                            val halfH = shape.height / 2f
                            val shapeColor = Color(shape.fillColor.toULong())

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
                            // Represent text layer bounds visually
                            drawRoundRect(
                                color = Color(textData.color.toULong()).copy(alpha = 0.85f),
                                topLeft = Offset(-150f, -30f),
                                size = Size(300f, 60f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
                            )
                        }
                        LayerType.MEDIA -> {
                            drawRect(
                                color = SaltySurfaceVariantDark,
                                topLeft = Offset(-150f, -150f),
                                size = Size(300f, 300f)
                            )
                        }
                        LayerType.ADJUSTMENT -> {
                            drawRect(
                                color = SaltyCyan.copy(alpha = 0.15f),
                                topLeft = Offset(-scaledW / 2f, -scaledH / 2f),
                                size = Size(scaledW, scaledH)
                            )
                        }
                        LayerType.VECTOR, LayerType.GROUP -> {
                            drawRect(
                                color = SaltyAmber.copy(alpha = 0.5f),
                                topLeft = Offset(-100f, -100f),
                                size = Size(200f, 200f)
                            )
                        }
                    }
                }

                // Bounding Box & Handles for Selected Layer (Section 3.6)
                if (selectedLayerIds.contains(layer.id)) {
                    val halfW = (layer.localWidth * layer.transform.scaleX * zoom) / 2f
                    val halfH = (layer.localHeight * layer.transform.scaleY * zoom) / 2f

                    withTransform({
                        translate(layerCenterX, layerCenterY)
                        rotate(layer.transform.rotationDegrees, pivot = Offset.Zero)
                    }) {
                        // Bounding Box Outline
                        drawRect(
                            color = SaltyCyan,
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
                            drawCircle(color = SaltyCyan, radius = handleRadius, center = corner, style = Stroke(width = 2f))
                        }

                        // Top Rotation Handle
                        val rotateHandleCenter = Offset(0f, -halfH - 24f)
                        drawLine(
                            color = SaltyCyan,
                            start = Offset(0f, -halfH),
                            end = rotateHandleCenter,
                            strokeWidth = 1.5f
                        )
                        drawCircle(color = SaltyAmber, radius = handleRadius, center = rotateHandleCenter)
                    }
                }
            }
        }
    }
}
