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
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.data.FontManager
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.media.MediaCache
import com.motionssalt.saltygraphics.theme.*
import kotlin.math.*

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
    modifier: Modifier = Modifier,
    onUpdateLayerTransforms: ((Map<String, Transform2D>) -> Unit)? = null,
    onCommitLayerTransforms: ((Map<String, Transform2D>) -> Unit)? = null
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

    val updateTransform by rememberUpdatedState(onUpdateLayerTransform)
    val commitTransform by rememberUpdatedState(onCommitLayerTransform)
    val updateTransforms by rememberUpdatedState(onUpdateLayerTransforms)
    val commitTransforms by rememberUpdatedState(onCommitLayerTransforms)
    val selectLayer by rememberUpdatedState(onSelectLayer)
    val changeZoomPan by rememberUpdatedState(onZoomPanChange)
    val selectedLayer = remember(document, selectedLayerIds) { selectionProxy(context, document, selectedLayerIds) }
    val selectedBounds = remember(document, selectedLayer) { selectedLayer?.let { selectionBounds(context,document,it) } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("editor_canvas")
            .clipToBounds()
            .onSizeChanged { viewport = it }
            .background(colors.background)
            .pointerInput(Unit) {
                val touchSlop = viewConfiguration.touchSlop
                val touchRadius = 22.dp.toPx() // 44dp minimum target, independent of zoom.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val activeDoc = currentDoc
                    val effZoom = currentFit * currentZoom
                    if (!effZoom.isFinite() || effZoom <= 0f) return@awaitEachGesture
                    val screenCenter = Offset(size.width/2f+currentPan.x,size.height/2f+currentPan.y)
                    fun documentPoint(p: Offset) = SelectionPoint((p.x-screenCenter.x)/effZoom,(p.y-screenCenter.y)/effZoom)
                    val downPoint = documentPoint(down.position)
                    var target = selectionProxy(context,activeDoc,currentSelectedIds)
                    var bounds = target?.let { selectionBounds(context,activeDoc,it) }
                    var handle = SelectionHandle.NONE
                    var ids = currentSelectedIds
                    if(target != null && bounds != null && !target.isLocked) {
                        val t=target.transform; val box=bounds
                        val local=VectorEditing.toLocal(downPoint.x,downPoint.y,t)
                        val candidates=SelectionHandle.entries.filter { it != SelectionHandle.NONE && it != SelectionHandle.BODY }.map { h ->
                            val point=if(h==SelectionHandle.ROTATE) {
                                val edge=SelectionGeometry.world(SelectionPoint(box.center.x,box.top),t)
                                val stem=SelectionPoint(0f,-32.dp.toPx()/effZoom).rotated(t.rotationDegrees)
                                edge+stem
                            } else SelectionGeometry.world(SelectionPoint(
                                if(h.x<0) box.left else if(h.x>0) box.right else box.center.x,
                                if(h.y<0) box.top else if(h.y>0) box.bottom else box.center.y),t)
                            h to hypot(point.x-downPoint.x,point.y-downPoint.y)*effZoom
                        }
                        handle=candidates.filter { it.second<=touchRadius }.minByOrNull { it.second }?.first
                            ?: if(local.first in box.left..box.right && local.second in box.top..box.bottom) SelectionHandle.BODY else SelectionHandle.NONE
                    }
                    if(handle==SelectionHandle.NONE) {
                        target=activeDoc.activeLayers.asReversed().firstOrNull { layer ->
                            if(!layer.isVisible || layer.isLocked || layer.type==LayerType.ADJUSTMENT) false
                            else {
                                val local=VectorEditing.toLocal(downPoint.x,downPoint.y,layer.transform)
                                val box=layerSelectionBounds(context,activeDoc,layer)
                                local.first in box.left..box.right && local.second in box.top..box.bottom
                            }
                        }
                        if(target!=null) {
                            ids=setOf(target.id); selectLayer(target.id)
                            bounds=selectionBounds(context,activeDoc,target); handle=SelectionHandle.BODY
                        }
                    }
                    val start=target?.transform ?: Transform2D()
                    val targetId=target?.id
                    val originals=ids.mapNotNull { id -> activeDoc.findLayerById(id)?.let { id to it.transform } }.toMap()
                    var transform=start
                    var dragDistance=0f
                    var last=down.position
                    var multiTouch=false
                    var pinchDistance=0f
                    var pinchCenter=Offset.Zero
                    var finished=false
                    fun values(t: Transform2D): Map<String,Transform2D> {
                        if(targetId!="__selection__") return targetId?.let { mapOf(it to t) } ?: emptyMap()
                        return originals.mapValues { (_,base) ->
                            val relative=SelectionPoint((base.translationX-start.translationX)*t.scaleX,
                                (base.translationY-start.translationY)*t.scaleY).rotated(t.rotationDegrees)
                            base.copy(translationX=t.translationX+relative.x,translationY=t.translationY+relative.y,
                                scaleX=base.scaleX*t.scaleX,scaleY=base.scaleY*t.scaleY,
                                rotationDegrees=(base.rotationDegrees+t.rotationDegrees)%360f)
                        }
                    }
                    fun preview(t: Transform2D) {
                        val batch=values(t)
                        if(batch.values.all { SelectionGeometry.valid(it) }) {
                            if(updateTransforms!=null) updateTransforms!!.invoke(batch)
                            else batch.forEach { (id,value) -> updateTransform(id,value) }
                        }
                    }
                    try {
                        while(true) {
                            val event=awaitPointerEvent()
                            val pressed=event.changes.filter { it.pressed }
                            if(pressed.isEmpty()) {
                                // Android cancellation synthesizes consumed up changes. They are not a release/commit.
                                val cancelled=event.changes.any { it.isConsumed }
                                if(cancelled && transform!=start) preview(start)
                                // A tap is selection, not a transform/history command.
                                if(!cancelled && !multiTouch && transform!=start) {
                                    val batch=values(transform)
                                    if(commitTransforms!=null) commitTransforms!!.invoke(batch)
                                    else batch.forEach { (id,t) -> (commitTransform ?: updateTransform).invoke(id,t) }
                                } else if(!cancelled && !multiTouch && handle==SelectionHandle.NONE && dragDistance<touchSlop) selectLayer(null)
                                finished=true
                                break
                            }
                            if(pressed.size>=2) {
                                if(!multiTouch && transform!=start) { preview(start);transform=start }
                                multiTouch=true
                                val a=pressed[0].position; val b=pressed[1].position
                                val distance=(a-b).getDistance();val centroid=(a+b)/2f
                                if(pinchDistance>0f && distance>0f) {
                                    val factor=(distance/pinchDistance).coerceIn(.8f,1.25f)
                                    val newZoom=(currentZoom*factor).coerceIn(.1f,10f)
                                    // Keep the artwork under the pinch centroid stationary.
                                    val center=Offset(size.width/2f,size.height/2f)
                                    val relative=pinchCenter-center-currentPan
                                    changeZoomPan(newZoom,currentPan+(centroid-pinchCenter)-relative*(newZoom/currentZoom-1f))
                                }
                                pinchDistance=distance;pinchCenter=centroid
                                event.changes.forEach { it.consume() }
                                continue
                            }
                            if(multiTouch) continue // Never resume a partially cancelled layer drag.
                            val change=pressed.first()
                            val delta=change.position-last
                            dragDistance+=delta.getDistance()
                            if(handle==SelectionHandle.NONE) {
                                if(dragDistance>=touchSlop) {changeZoomPan(currentZoom,currentPan+delta);change.consume()}
                            } else if(bounds!=null && dragDistance>=touchSlop) {
                                transform=SelectionGeometry.drag(start,bounds,handle,downPoint,documentPoint(change.position))
                                preview(transform);change.consume()
                                if(handle==SelectionHandle.ROTATE) {
                                    rotatingAnglePill="${transform.rotationDegrees.roundToInt()}°"
                                    rotatingPillPosition=change.position-Offset(0f,48.dp.toPx())
                                }
                            }
                            last=change.position
                        }
                    } finally {
                        if(!finished && transform!=start) preview(start)
                        rotatingAnglePill=null;rotatingPillPosition=null
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
                                val paint = selectionTextPaint(context,textData).apply {
                                    color=safeColor(textData.color).toArgb()
                                    alpha=(layer.opacity*255).roundToInt().coerceIn(0,255)
                                }

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

            }
            // Draw selection last so it remains legible over every layer/background.
            if(selectedLayer!=null && selectedBounds!=null) {
                val t=selectedLayer.transform;val b=selectedBounds
                val centerLocal=SelectionGeometry.world(b.center,t)
                val halfW=b.width*t.scaleX*effectiveZoom/2f
                val halfH=b.height*t.scaleY*effectiveZoom/2f
                withTransform({
                    translate(center.x+centerLocal.x*effectiveZoom,center.y+centerLocal.y*effectiveZoom)
                    rotate(t.rotationDegrees,pivot=Offset.Zero)
                }) {
                    val origin=Offset(-halfW,-halfH);val dimensions=Size(halfW*2,halfH*2)
                    drawRect(Color.Black,origin,dimensions,style=Stroke(3.dp.toPx()))
                    drawRect(Color.White,origin,dimensions,style=Stroke(1.dp.toPx()))
                    if(!selectedLayer.isLocked) {
                        fun handle(p: Offset, side: Float) {
                            val top=p-Offset(side/2,side/2)
                            drawRect(Color.White,top,Size(side,side))
                            drawRect(Color.Black,top,Size(side,side),style=Stroke(1.dp.toPx()))
                        }
                        listOf(Offset(-halfW,-halfH),Offset(halfW,-halfH),Offset(halfW,halfH),Offset(-halfW,halfH))
                            .forEach { handle(it,9.dp.toPx()) }
                        listOf(Offset(0f,-halfH),Offset(0f,halfH),Offset(-halfW,0f),Offset(halfW,0f))
                            .forEach { handle(it,6.dp.toPx()) }
                        val rotate=Offset(0f,-halfH-32.dp.toPx())
                        drawLine(Color.Black,Offset(0f,-halfH),rotate,3.dp.toPx())
                        drawLine(Color.White,Offset(0f,-halfH),rotate,1.dp.toPx())
                        drawCircle(Color.Black,6.dp.toPx(),rotate)
                        drawCircle(Color.White,4.dp.toPx(),rotate)
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
