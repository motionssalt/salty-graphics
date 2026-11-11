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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.motionssalt.saltygraphics.data.FontManager
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import com.motionssalt.saltygraphics.engine.render.FillRenderer
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import com.motionssalt.saltygraphics.engine.render.TextLayoutEngine
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
    previewQuality: PreviewQuality = PreviewQuality.FULL,
    isCanvasLocked: Boolean = false,
    onUpdateLayerTransforms: ((Map<String, Transform2D>) -> Unit)? = null,
    onCommitLayerTransforms: ((Map<String, Transform2D>) -> Unit)? = null,
    onUpdateTextData: ((String, TextLayerData) -> Unit)? = null,
    onCommitTextData: ((String, TextLayerData) -> Unit)? = null,
    externalSmartGuides: List<SmartGuideLine> = emptyList(),
    externalSnappedGridX: Float? = null,
    externalSnappedGridY: Float? = null
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val density = LocalDensity.current
    val mediaGeneration by MediaCache.revision.collectAsState()
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    val fit = if (viewport.width > 0 && viewport.height > 0)
        minOf(viewport.width / document.canvasSize.width.toFloat(), viewport.height / document.canvasSize.height.toFloat()) * .94f else 1f
    val effectiveZoom = fit * zoom

    // Active rotating angle pill readout
    var rotatingAnglePill by remember { mutableStateOf<String?>(null) }
    var rotatingPillPosition by remember { mutableStateOf<Offset?>(null) }
    var activeSmartGuides by remember { mutableStateOf<List<SmartGuideLine>>(emptyList()) }
    var activeSnappedGridX by remember { mutableStateOf<Float?>(null) }
    var activeSnappedGridY by remember { mutableStateOf<Float?>(null) }

    val displaySmartGuides = if (activeSmartGuides.isNotEmpty()) activeSmartGuides else externalSmartGuides
    val displaySnappedGridX = activeSnappedGridX ?: externalSnappedGridX
    val displaySnappedGridY = activeSnappedGridY ?: externalSnappedGridY

    // Use rememberUpdatedState to avoid gesture coroutine teardown on rapid zoom/pan changes
    val currentZoom by rememberUpdatedState(zoom)
    val currentPan by rememberUpdatedState(panOffset)
    val currentDoc by rememberUpdatedState(document)
    val currentSelectedIds by rememberUpdatedState(selectedLayerIds)
    val currentFit by rememberUpdatedState(fit)
    val canvasLocked by rememberUpdatedState(isCanvasLocked)

    val updateTransform by rememberUpdatedState(onUpdateLayerTransform)
    val commitTransform by rememberUpdatedState(onCommitLayerTransform)
    val updateTransforms by rememberUpdatedState(onUpdateLayerTransforms)
    val commitTransforms by rememberUpdatedState(onCommitLayerTransforms)
    val updateTextData by rememberUpdatedState(onUpdateTextData)
    val commitTextData by rememberUpdatedState(onCommitTextData)
    val selectLayer by rememberUpdatedState(onSelectLayer)
    val changeZoomPan by rememberUpdatedState(onZoomPanChange)
    val selectedLayer = remember(document, selectedLayerIds) { selectionProxy(context, document, selectedLayerIds) }
    val selectedBounds = remember(document, selectedLayer) { selectedLayer?.let { selectionBounds(context,document,it) } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("editor_canvas")
            .semantics { stateDescription = if (selectedLayerIds.isEmpty()) "No layers selected" else "${selectedLayerIds.size} layers selected" }
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
                    var isTextFrameDrag = false
                    var textStartBoxWidth = 0f
                    var ids = currentSelectedIds
                    val isSelectedTextLayer = target != null && target.type == LayerType.TEXT && target.textData != null
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
                        val closest = candidates.filter { it.second<=touchRadius }.minByOrNull { it.second }?.first
                        if (isSelectedTextLayer && (closest == SelectionHandle.EDGE_LEFT || closest == SelectionHandle.EDGE_RIGHT)) {
                            handle = closest
                            isTextFrameDrag = true
                            textStartBoxWidth = target.textData!!.boxWidth
                        } else {
                            handle = closest
                                ?: if(local.first in box.left..box.right && local.second in box.top..box.bottom) SelectionHandle.BODY else SelectionHandle.NONE
                        }
                    }
                    if(handle==SelectionHandle.NONE) {
                        // Hit-test from topmost layer (index 0 = top of panel = topmost on canvas)
                        target=activeDoc.activeLayers.firstOrNull { layer ->
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
                                if(!cancelled && !multiTouch && isTextFrameDrag && targetId != null) {
                                    val currentTextData = activeDoc.findLayerById(targetId)?.textData
                                    if (currentTextData != null) {
                                        commitTextData?.invoke(targetId, currentTextData)
                                    }
                                } else if(!cancelled && !multiTouch && transform!=start) {
                                    val batch=values(transform)
                                    if(commitTransforms!=null) commitTransforms!!.invoke(batch)
                                    else batch.forEach { (id,t) -> (commitTransform ?: updateTransform).invoke(id,t) }
                                } else if(!cancelled && !multiTouch && handle==SelectionHandle.NONE && dragDistance<touchSlop) selectLayer(null)
                                activeSmartGuides = emptyList()
                                activeSnappedGridX = null
                                activeSnappedGridY = null
                                rotatingAnglePill = null
                                rotatingPillPosition = null
                                finished=true
                                break
                            }
                            if(pressed.size>=2) {
                                if(!multiTouch && transform!=start) { preview(start);transform=start }
                                multiTouch=true
                                val a=pressed[0].position; val b=pressed[1].position
                                val distance=(a-b).getDistance();val centroid=(a+b)/2f
                                if(!canvasLocked && pinchDistance>0f && distance>0f) {
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
                                if(!canvasLocked && dragDistance>=touchSlop) {changeZoomPan(currentZoom,currentPan+delta);change.consume()}
                            } else if(isTextFrameDrag && targetId != null && bounds != null && dragDistance>=touchSlop) {
                                val currentPoint = documentPoint(change.position)
                                val t = start
                                val localCurrent = VectorEditing.toLocal(currentPoint.x, currentPoint.y, t)
                                val localDown = VectorEditing.toLocal(downPoint.x, downPoint.y, t)
                                val dx = localCurrent.first - localDown.first
                                val newBoxW = if (handle == SelectionHandle.EDGE_RIGHT) {
                                    max(50f, textStartBoxWidth + (dx * 2f))
                                } else {
                                    max(50f, textStartBoxWidth - (dx * 2f))
                                }
                                val layerText = activeDoc.findLayerById(targetId)?.textData
                                if (layerText != null) {
                                    updateTextData?.invoke(targetId, layerText.copy(boxWidth = newBoxW, boxMode = TextBoxMode.PARAGRAPH))
                                }
                                change.consume()
                            } else if(bounds!=null && dragDistance>=touchSlop) {
                                var rawTransform = SelectionGeometry.drag(start,bounds,handle,downPoint,documentPoint(change.position))
                                
                                // T21: Smart Guides & Snapping
                                if (activeDoc.editorSettings.snapEnabled) {
                                    if (handle == SelectionHandle.BODY) {
                                        val otherLayersList = activeDoc.activeLayers
                                            .filter { it.id != targetId && it.isVisible && it.type != LayerType.ADJUSTMENT }
                                            .map { (selectionBounds(context, activeDoc, it) ?: SelectionBounds(-100f, -100f, 100f, 100f)) to it.transform }
                                        val snapRes = SmartSnapEngine.snapTranslation(
                                            targetBounds = bounds,
                                            targetTransform = rawTransform,
                                            otherLayers = otherLayersList,
                                            canvasWidth = activeDoc.canvasSize.width.toFloat(),
                                            canvasHeight = activeDoc.canvasSize.height.toFloat(),
                                            effectiveZoom = effZoom,
                                            tolerancePx = activeDoc.editorSettings.snapTolerancePx
                                        )
                                        rawTransform = snapRes.snappedTransform
                                        activeSmartGuides = snapRes.activeGuides
                                    } else if (handle == SelectionHandle.ROTATE) {
                                        val otherAngles = activeDoc.activeLayers.filter { it.id != targetId }.map { it.transform.rotationDegrees }
                                        val (snappedDeg, didSnap) = SmartSnapEngine.snapRotation(rawTransform.rotationDegrees, otherAngles)
                                        if (didSnap) {
                                            rawTransform = rawTransform.copy(rotationDegrees = snappedDeg)
                                        }
                                        activeSmartGuides = emptyList()
                                    } else {
                                        activeSmartGuides = emptyList()
                                    }
                                } else {
                                    activeSmartGuides = emptyList()
                                }

                                // V15: Optional grid-line snapping with transient feedback (engages only when grid is visible)
                                if (activeDoc.editorSettings.snapToGrid && activeDoc.editorSettings.showGrid && handle == SelectionHandle.BODY) {
                                    val gridSnap = SmartSnapEngine.snapToGridLines(
                                        targetBounds = bounds,
                                        targetTransform = rawTransform,
                                        canvasWidth = activeDoc.canvasSize.width.toFloat(),
                                        canvasHeight = activeDoc.canvasSize.height.toFloat(),
                                        gridSize = activeDoc.editorSettings.gridSize,
                                        isSquareGrid = activeDoc.editorSettings.isSquareGrid,
                                        effectiveZoom = effZoom,
                                        tolerancePx = activeDoc.editorSettings.snapTolerancePx
                                    )
                                    rawTransform = gridSnap.snappedTransform
                                    activeSnappedGridX = gridSnap.snappedGridX
                                    activeSnappedGridY = gridSnap.snappedGridY
                                } else if (!activeDoc.editorSettings.snapToGrid) {
                                    activeSnappedGridX = null
                                    activeSnappedGridY = null
                                }

                                transform = rawTransform
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
                        rotatingAnglePill=null;rotatingPillPosition=null;activeSmartGuides=emptyList()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE") val cacheGeneration=mediaGeneration
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

            // Gridlines (T26: Square toggle and canvas-matched aspect ratio; V15: Snapping transient feedback)
            if (showGrid && gridSize > 0) {
                val gridColor = safeColor(document.editorSettings.gridColor)
                val snapHighlightColor = Color(0xFFFF9100)
                if (document.editorSettings.isSquareGrid) {
                    // Square grid mode: equal-sized cells
                    val cellPx = gridSize * effectiveZoom
                    if (cellPx > 4f) {
                        var x = topLeft.x
                        while (x <= topLeft.x + scaledW + 0.1f) {
                            val docX = (x - center.x) / effectiveZoom
                            val isSnapped = displaySnappedGridX != null && abs(docX - displaySnappedGridX!!) < (4f / effectiveZoom)
                            drawLine(
                                color = if (isSnapped) snapHighlightColor else gridColor,
                                start = Offset(x, topLeft.y),
                                end = Offset(x, topLeft.y + scaledH),
                                strokeWidth = if (isSnapped) 2.5f else 1f
                            )
                            x += cellPx
                        }
                        var y = topLeft.y
                        while (y <= topLeft.y + scaledH + 0.1f) {
                            val docY = (y - center.y) / effectiveZoom
                            val isSnapped = displaySnappedGridY != null && abs(docY - displaySnappedGridY!!) < (4f / effectiveZoom)
                            drawLine(
                                color = if (isSnapped) snapHighlightColor else gridColor,
                                start = Offset(topLeft.x, y),
                                end = Offset(topLeft.x + scaledW, y),
                                strokeWidth = if (isSnapped) 2.5f else 1f
                            )
                            y += cellPx
                        }
                    }
                } else {
                    // Default mode: match grid's aspect ratio to the canvas so lines wrap corner to corner
                    val cols = maxOf(1, (canvasW / gridSize).roundToInt())
                    val rows = maxOf(1, (canvasH / gridSize).roundToInt())
                    val colSpacing = scaledW / cols
                    val rowSpacing = scaledH / rows
                    if (colSpacing > 2f && rowSpacing > 2f) {
                        for (c in 0..cols) {
                            val x = topLeft.x + c * colSpacing
                            val docX = (x - center.x) / effectiveZoom
                            val isSnapped = displaySnappedGridX != null && abs(docX - displaySnappedGridX!!) < (4f / effectiveZoom)
                            drawLine(
                                color = if (isSnapped) snapHighlightColor else gridColor,
                                start = Offset(x, topLeft.y),
                                end = Offset(x, topLeft.y + scaledH),
                                strokeWidth = if (isSnapped) 2.5f else 1f
                            )
                        }
                        for (r in 0..rows) {
                            val y = topLeft.y + r * rowSpacing
                            val docY = (y - center.y) / effectiveZoom
                            val isSnapped = displaySnappedGridY != null && abs(docY - displaySnappedGridY!!) < (4f / effectiveZoom)
                            drawLine(
                                color = if (isSnapped) snapHighlightColor else gridColor,
                                start = Offset(topLeft.x, y),
                                end = Offset(topLeft.x + scaledW, y),
                                strokeWidth = if (isSnapped) 2.5f else 1f
                            )
                        }
                    }
                }
            }

            fun drawActiveLayers(nativeCanvas: NativeCanvas, alphaMult: Float = 1.0f) {
                for (layer in document.activeLayers.asReversed()) {
                    if (!layer.isVisible) continue
                    if (layer.type == LayerType.ADJUSTMENT) continue // Adjustment layers draw no direct shape

                    val layerCenterX = center.x + (layer.transform.translationX * effectiveZoom)
                    val layerCenterY = center.y + (layer.transform.translationY * effectiveZoom)

                    val layerPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                        alpha = (layer.opacity * 255 * alphaMult).roundToInt().coerceIn(0, 255)
                        LayerCompositor.applyLayerBlendMode(this, layer.blendMode)
                    }

                    // saveLayer isolates the layer and applies blend mode and opacity upon restore
                    nativeCanvas.saveLayer(null, layerPaint)

                    nativeCanvas.save()
                    nativeCanvas.translate(layerCenterX, layerCenterY)
                    nativeCanvas.rotate(layer.transform.rotationDegrees)
                    nativeCanvas.scale(layer.transform.scaleX * effectiveZoom, layer.transform.scaleY * effectiveZoom)

                    when (layer.type) {
                        LayerType.SHAPE -> {
                            val shape = layer.shapeData ?: ShapeData()
                            val halfW = shape.width / 2f
                            val halfH = shape.height / 2f
                            val rect = RectF(-halfW, -halfH, halfW, halfH)
                            val shapePath = when (shape.type) {
                                ShapeType.RECTANGLE -> NativePath().apply { addRect(rect, NativePath.Direction.CW) }
                                ShapeType.ROUNDED_RECTANGLE -> NativePath().apply { addRoundRect(rect, shape.cornerRadius, shape.cornerRadius, NativePath.Direction.CW) }
                                ShapeType.CIRCLE -> NativePath().apply { addOval(rect, NativePath.Direction.CW) }
                                else -> nativeVectorPath(shape.toVectorPath())
                            }
                            val fill = layer.effectiveFill
                            val stroke = layer.effectiveStroke
                            val shadow = layer.effectiveShadow

                            // 1. Drop Shadow
                            if (shadow.isEnabled) {
                                val shadowPaint = NativePaint()
                                if (FillRenderer.configureShadowPaint(shadowPaint, shadow)) {
                                    nativeCanvas.save()
                                    nativeCanvas.translate(shadow.offsetX, shadow.offsetY)
                                    nativeCanvas.drawPath(shapePath, shadowPaint)
                                    nativeCanvas.restore()
                                }
                            }

                            // 2. Fill (solid, gradient, media)
                            val shapeMediaAsset = fill.mediaAssetPath
                            if (fill.type == FillType.MEDIA && shapeMediaAsset != null) {
                                val bmp = MediaCache.getOrRequest(context, document.id, shapeMediaAsset)
                                if (bmp != null) {
                                    nativeCanvas.save()
                                    nativeCanvas.clipPath(shapePath)
                                    FillRenderer.drawMediaFill(nativeCanvas,bmp,fill,rect,shapePath)
                                    nativeCanvas.restore()
                                }
                            } else {
                                val fillPaint = NativePaint()
                                if (FillRenderer.configureFillPaint(fillPaint, fill, rect)) {
                                    nativeCanvas.drawPath(shapePath, fillPaint)
                                } else if (shape.hasFill && fill.type == FillType.SOLID && fill.solidOpacity > 0f) {
                                    fillPaint.color = shape.fillColor.toInt()
                                    fillPaint.style = NativePaint.Style.FILL
                                    nativeCanvas.drawPath(shapePath, fillPaint)
                                }
                            }

                            // 3. Stroke
                            val strokePaint = NativePaint()
                            if (FillRenderer.configureStrokePaint(strokePaint, stroke)) {
                                FillRenderer.drawPathWithStroke(nativeCanvas, shapePath, stroke, strokePaint)
                            } else if (shape.hasStroke && shape.strokeWidth > 0) {
                                val legacyStroke = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                                    color = shape.strokeColor.toInt()
                                    strokeWidth = shape.strokeWidth
                                    style = NativePaint.Style.STROKE
                                }
                                nativeCanvas.drawPath(shapePath, legacyStroke)
                            }
                        }
                        LayerType.TEXT -> {
                            val textData = layer.textData ?: TextLayerData()
                            val fill = layer.effectiveFill
                            val stroke = layer.effectiveStroke
                            val shadow = layer.effectiveShadow
                            val paint = selectionTextPaint(context, textData).apply {
                                color = fill.effectiveSolidArgb.toInt()
                            }
                            val bounds = RectF(-textData.boxWidth / 2f, -textData.boxHeight / 2f, textData.boxWidth / 2f, textData.boxHeight / 2f)
                            FillRenderer.configureFillPaint(paint,fill,bounds) { asset -> MediaCache.getOrRequest(context,document.id,asset) }
                            val pathLayer = if (textData.followPathLayerId != null) document.findLayerById(textData.followPathLayerId!!) else null
                            val vectorPath = pathLayer?.vectorPathData ?: pathLayer?.shapeData?.toVectorPath()
                            val nativePath = if (vectorPath != null && vectorPath.points.size >= 2) {
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
                                path
                            } else null

                            var strokePaint: NativePaint? = null
                            if (stroke.isEnabled && stroke.width > 0f) {
                                strokePaint = NativePaint(paint)
                                FillRenderer.configureStrokePaint(strokePaint, stroke)
                            } else if (textData.hasOutline) {
                                strokePaint = NativePaint(paint).apply {
                                    style = NativePaint.Style.STROKE
                                    strokeWidth = textData.outlineWidth
                                    color = textData.outlineColor.toInt()
                                }
                            }

                            var shadowPaint: NativePaint? = null
                            if (shadow.isEnabled) {
                                shadowPaint = NativePaint(paint)
                                FillRenderer.configureShadowPaint(shadowPaint, shadow)
                            }

                            TextLayoutEngine.drawTextLayout(
                                canvas = nativeCanvas,
                                textData = textData,
                                textPaint = paint,
                                strokePaint = strokePaint,
                                shadowPaint = shadowPaint,
                                path = nativePath
                            )
                        }
                        LayerType.MEDIA -> {
                            val fill = layer.effectiveFill
                            val stroke = layer.effectiveStroke
                            val shadow = layer.effectiveShadow
                            val mediaAsset = if (fill.type == FillType.MEDIA) fill.mediaAssetPath ?: layer.mediaAssetPath else null
                            val bmp = if (mediaAsset != null) {
                                MediaCache.getOrRequest(context, document.id, mediaAsset)
                            } else null
                            val mw = layer.mediaWidth ?: 300f
                            val mh = layer.mediaHeight ?: 300f
                            val halfW = mw / 2f
                            val halfH = mh / 2f
                            val dst = RectF(-halfW, -halfH, halfW, halfH)
                            if (shadow.isEnabled) {
                                val shadowPaint = NativePaint()
                                if (FillRenderer.configureShadowPaint(shadowPaint, shadow)) {
                                    nativeCanvas.save()
                                    nativeCanvas.translate(shadow.offsetX, shadow.offsetY)
                                    nativeCanvas.drawRect(dst, shadowPaint)
                                    nativeCanvas.restore()
                                }
                            }
                            if (bmp != null) {
                                val paint = NativePaint(NativePaint.ANTI_ALIAS_FLAG)
                                FillRenderer.drawMediaFill(nativeCanvas,bmp,fill,dst)
                            } else {
                                val fillPaint = NativePaint()
                                if (FillRenderer.configureFillPaint(fillPaint,fill,dst)) nativeCanvas.drawRect(dst,fillPaint)
                                else if(layer.fillData==null && layer.mediaAssetPath==null) {
                                    nativeCanvas.drawRect(dst,NativePaint().apply { color=0xFF888888.toInt() })
                                }
                            }
                            val strokePaint = NativePaint()
                            if (FillRenderer.configureStrokePaint(strokePaint, stroke)) {
                                val mediaPath = NativePath().apply { addRect(dst, NativePath.Direction.CW) }
                                FillRenderer.drawPathWithStroke(nativeCanvas, mediaPath, stroke, strokePaint)
                            }
                        }
                        LayerType.VECTOR -> {
                            val fill = layer.effectiveFill
                            val stroke = layer.effectiveStroke
                            val shadow = layer.effectiveShadow
                            layer.editablePaths.forEach { data ->
                                val path = nativeVectorPath(data)
                                val bounds = RectF()
                                path.computeBounds(bounds, true)
                                if (shadow.isEnabled) {
                                    val shadowPaint = NativePaint()
                                    if (FillRenderer.configureShadowPaint(shadowPaint, shadow)) {
                                        nativeCanvas.save()
                                        nativeCanvas.translate(shadow.offsetX, shadow.offsetY)
                                        nativeCanvas.drawPath(path, shadowPaint)
                                        nativeCanvas.restore()
                                    }
                                }
                                val fillPaint = NativePaint()
                                if (FillRenderer.configureFillPaint(fillPaint, fill, bounds) { asset -> MediaCache.getOrRequest(context,document.id,asset) }) {
                                    nativeCanvas.drawPath(path, fillPaint)
                                } else if (data.hasFill && fill.type == FillType.SOLID && fill.solidOpacity > 0f) {
                                    fillPaint.color = data.fillColor.toInt()
                                    fillPaint.style = NativePaint.Style.FILL
                                    nativeCanvas.drawPath(path, fillPaint)
                                }
                                val strokePaint = NativePaint()
                                if (FillRenderer.configureStrokePaint(strokePaint, stroke)) {
                                    FillRenderer.drawPathWithStroke(nativeCanvas, path, stroke, strokePaint)
                                } else if (data.hasStroke && data.strokeWidth > 0f) {
                                    drawNativeVector(nativeCanvas, data, 1f)
                                }
                            }
                        }
                        LayerType.GROUP -> {
                            for (child in layer.children) {
                                if (!child.isVisible) continue
                                val childPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                                    alpha = (child.opacity * 255).roundToInt().coerceIn(0, 255)
                                    LayerCompositor.applyLayerBlendMode(this, child.blendMode)
                                }
                                nativeCanvas.saveLayer(null, childPaint)
                                val childCenterX = child.transform.translationX * effectiveZoom
                                val childCenterY = child.transform.translationY * effectiveZoom
                                nativeCanvas.save()
                                nativeCanvas.translate(childCenterX, childCenterY)
                                nativeCanvas.rotate(child.transform.rotationDegrees)
                                nativeCanvas.scale(child.transform.scaleX * effectiveZoom, child.transform.scaleY * effectiveZoom)
                                if (child.type == LayerType.SHAPE) {
                                    val s = child.shapeData ?: ShapeData()
                                    val rect = RectF(-s.width / 2f, -s.height / 2f, s.width / 2f, s.height / 2f)
                                    val childFill = child.effectiveFill
                                    val fp = NativePaint()
                                    if (FillRenderer.configureFillPaint(fp, childFill, rect)) {
                                        nativeCanvas.drawRect(rect, fp)
                                    } else if (s.hasFill && childFill.type == FillType.SOLID && childFill.solidOpacity > 0f) {
                                        fp.color = s.fillColor.toInt()
                                        fp.style = NativePaint.Style.FILL
                                        nativeCanvas.drawRect(rect, fp)
                                    }
                                }
                                nativeCanvas.restore()
                                nativeCanvas.restore()
                            }
                        }
                        LayerType.ADJUSTMENT -> Unit
                        LayerType.QUARANTINED -> {
                            val qPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                                color = 0x40FF0000.toInt()
                            }
                            nativeCanvas.drawRoundRect(RectF(-120f, -60f, 120f, 60f), 8f, 8f, qPaint)
                        }
                    }
                    nativeCanvas.restore() // Restores layer transform

                    // Apply non-destructive mask if configured
                    if (layer.effectiveMaskType != MaskType.NONE) {
                        val maskSourceId = layer.effectiveMaskSourceLayerId
                        val maskLayer = if (maskSourceId != null) document.findLayerById(maskSourceId) else null
                        if (maskLayer != null) {
                            val maskPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                                xfermode = PorterDuffXfermode(
                                    if (layer.effectiveMaskInverted) PorterDuff.Mode.DST_OUT else PorterDuff.Mode.DST_IN
                                )
                                if (layer.effectiveMaskType == MaskType.LUMINANCE) {
                                    colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                                        0f, 0f, 0f, 0f, 0f,
                                        0f, 0f, 0f, 0f, 0f,
                                        0f, 0f, 0f, 0f, 0f,
                                        0.299f, 0.587f, 0.114f, 0f, 0f
                                    )))
                                }
                            }
                            val maskCenterX = center.x + (maskLayer.transform.translationX * effectiveZoom)
                            val maskCenterY = center.y + (maskLayer.transform.translationY * effectiveZoom)
                            nativeCanvas.save()
                            nativeCanvas.translate(maskCenterX, maskCenterY)
                            nativeCanvas.rotate(maskLayer.transform.rotationDegrees)
                            nativeCanvas.scale(maskLayer.transform.scaleX * effectiveZoom, maskLayer.transform.scaleY * effectiveZoom)
                            drawLayerMask(nativeCanvas, maskLayer, maskPaint, context)
                            nativeCanvas.restore()
                        }
                    }

                    nativeCanvas.restore() // Restores saveLayer, blending into canvas backdrop
                }
            }

            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas

                // V12: Outside canvas: objects outside the canvas stay visible but very faint (30% opacity)
                nativeCanvas.save()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    nativeCanvas.clipOutRect(topLeft.x, topLeft.y, topLeft.x + scaledW, topLeft.y + scaledH)
                } else {
                    nativeCanvas.clipRect(topLeft.x, topLeft.y, topLeft.x + scaledW, topLeft.y + scaledH, android.graphics.Region.Op.DIFFERENCE)
                }
                drawActiveLayers(nativeCanvas, alphaMult = 0.30f)
                nativeCanvas.restore()

                // V12: Inside canvas: objects rendered at normal 100% opacity, strictly clipped to canvas
                nativeCanvas.save()
                nativeCanvas.clipRect(topLeft.x, topLeft.y, topLeft.x + scaledW, topLeft.y + scaledH)
                drawActiveLayers(nativeCanvas, alphaMult = 1.0f)
                nativeCanvas.restore()
            }

            // Canvas Border (distinct boundary between full opacity inside and faint outside)
            drawRect(
                color = colors.outline,
                topLeft = topLeft,
                size = Size(scaledW, scaledH),
                style = Stroke(width = 1.5f)
            )

            // Draw selection last so it remains legible over every layer/background.
            // Adjustment layers have no canvas selection box or handles (T12)
            if(selectedLayer!=null && selectedLayer.type != LayerType.ADJUSTMENT && selectedBounds!=null) {
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
                        listOf(Offset(0f,-halfH),Offset(0f,halfH))
                            .forEach { handle(it,6.dp.toPx()) }
                        if (selectedLayer.type == LayerType.TEXT) {
                            // Text box frame width resize handles: rounded pill style with cyan accent
                            fun textSideHandle(p: Offset) {
                                val pillW = 6.dp.toPx()
                                val pillH = 16.dp.toPx()
                                val top = p - Offset(pillW / 2f, pillH / 2f)
                                drawRoundRect(Color.Black, top, Size(pillW, pillH), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                                drawRoundRect(Color(0xFF00E5FF), top + Offset(1.dp.toPx(), 1.dp.toPx()), Size(pillW - 2.dp.toPx(), pillH - 2.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
                            }
                            textSideHandle(Offset(-halfW, 0f))
                            textSideHandle(Offset(halfW, 0f))
                        } else {
                            listOf(Offset(-halfW,0f),Offset(halfW,0f))
                                .forEach { handle(it,6.dp.toPx()) }
                        }
                        val rotate=Offset(0f,-halfH-32.dp.toPx())
                        drawLine(Color.Black,Offset(0f,-halfH),rotate,3.dp.toPx())
                        drawLine(Color.White,Offset(0f,-halfH),rotate,1.dp.toPx())
                        drawCircle(Color.Black,6.dp.toPx(),rotate)
                        drawCircle(Color.White,4.dp.toPx(),rotate)

                        // T20: Vector-aware / Shape-specific live geometry handles
                        val shape = selectedLayer.shapeData
                        if (selectedLayer.type == LayerType.SHAPE && shape != null) {
                            if (shape.type == ShapeType.ROUNDED_RECTANGLE) {
                                // Corner radius handle inside top-left corner
                                val rOffset = min(shape.cornerRadius * t.scaleX * effectiveZoom, min(halfW, halfH) * 0.8f)
                                val crCenter = Offset(-halfW + rOffset, -halfH + rOffset)
                                drawCircle(Color.Black, 6.dp.toPx(), crCenter)
                                drawCircle(Color(0xFF00E5FF), 4.5.dp.toPx(), crCenter)
                            } else if (shape.type == ShapeType.STAR) {
                                // Star inner radius handle
                                val innerR = halfH * shape.starInnerRadiusRatio
                                val innerOffset = Offset(0f, -innerR)
                                drawCircle(Color.Black, 6.dp.toPx(), innerOffset)
                                drawCircle(Color(0xFFFFB300), 4.5.dp.toPx(), innerOffset)
                            }
                        }
                    }
                }
            }

            // Visible Red Smart Guides (T21: Figma/Illustrator style snap indicators)
            if (displaySmartGuides.isNotEmpty()) {
                displaySmartGuides.forEach { guide ->
                    val redColor = Color(0xFFFF1744) // Bright crisp alignment red
                    if (guide.orientation == GuideOrientation.VERTICAL) {
                        val sx = center.x + guide.position * effectiveZoom
                        val sy1 = center.y + guide.start * effectiveZoom
                        val sy2 = center.y + guide.end * effectiveZoom
                        drawLine(
                            color = redColor,
                            start = Offset(sx, sy1),
                            end = Offset(sx, sy2),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    } else {
                        val sy = center.y + guide.position * effectiveZoom
                        val sx1 = center.x + guide.start * effectiveZoom
                        val sx2 = center.x + guide.end * effectiveZoom
                        drawLine(
                            color = redColor,
                            start = Offset(sx1, sy),
                            end = Offset(sx2, sy),
                            strokeWidth = 1.5.dp.toPx()
                        )
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

private fun drawLayerMask(
    canvas: android.graphics.Canvas,
    layer: Layer,
    maskPaint: NativePaint,
    context: Context
) {
    val mw = layer.localWidth
    val mh = layer.localHeight
    val halfW = mw / 2f
    val halfH = mh / 2f
    val rect = RectF(-halfW, -halfH, halfW, halfH)

    when (layer.type) {
        LayerType.SHAPE -> {
            val shape = layer.shapeData ?: ShapeData()
            when (shape.type) {
                ShapeType.RECTANGLE -> canvas.drawRect(rect, maskPaint)
                ShapeType.ROUNDED_RECTANGLE -> canvas.drawRoundRect(rect, shape.cornerRadius, shape.cornerRadius, maskPaint)
                ShapeType.CIRCLE -> canvas.drawOval(rect, maskPaint)
                else -> {
                    val path = nativeVectorPath(shape.toVectorPath())
                    canvas.drawPath(path, maskPaint)
                }
            }
        }
        LayerType.TEXT -> {
            val textData = layer.textData ?: TextLayerData()
            val textPaint = selectionTextPaint(context, textData).apply {
                color = 0xFFFFFFFF.toInt()
                xfermode = maskPaint.xfermode
                colorFilter = maskPaint.colorFilter
            }
            TextLayoutEngine.drawTextLayout(canvas, textData, textPaint)
        }
        LayerType.MEDIA -> {
            val mediaAsset = layer.mediaAssetPath
            val bmp = if (mediaAsset != null) MediaCache.getOrRequest(context, "", mediaAsset) else null
            if (bmp != null) {
                canvas.drawBitmap(bmp, null, rect, maskPaint)
            } else {
                canvas.drawRect(rect, maskPaint)
            }
        }
        LayerType.VECTOR -> {
            layer.editablePaths.forEach { data ->
                val path = nativeVectorPath(data)
                canvas.drawPath(path, maskPaint)
            }
        }
        else -> canvas.drawRect(rect, maskPaint)
    }
}
