package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.*
import kotlin.math.*

/** Geometry handles only; every editing widget remains in the tools pane. */
@Composable
fun GradientCanvasOverlay(layer: Layer, canvasSize: CanvasSize, zoom: Float, pan: Offset,
    onPreview: (FillData) -> Unit, onCommit: (FillData) -> Unit) {
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val live by rememberUpdatedState(layer)
    val preview by rememberUpdatedState(onPreview)
    val commit by rememberUpdatedState(onCommit)
    val livePan by rememberUpdatedState(pan)
    val fit=if(viewport.width>0 && viewport.height>0)
        min(viewport.width/canvasSize.width.toFloat(),viewport.height/canvasSize.height.toFloat())*.94f else 1f
    val scale by rememberUpdatedState(fit*zoom)
    fun bounds(l:Layer):SelectionBounds = when(l.type) {
        LayerType.TEXT -> {val t=l.textData?:TextLayerData();SelectionBounds(-t.boxWidth/2,-t.boxHeight/2,t.boxWidth/2,t.boxHeight/2)}
        LayerType.VECTOR -> {
            val rect=android.graphics.RectF()
            val points=l.editablePaths.flatMap { path ->
                nativeVectorPath(path).computeBounds(rect,true)
                listOf(SelectionPoint(rect.left,rect.top),SelectionPoint(rect.right,rect.bottom))
            }
            SelectionBounds.enclosing(points)
        }
        else -> SelectionBounds(-l.localWidth/2,-l.localHeight/2,l.localWidth/2,l.localHeight/2)
    }
    fun center(g:GradientData,b:SelectionBounds)=SelectionPoint(b.left+b.width*g.centerX,b.top+b.height*g.centerY)
    fun handles(l:Layer):List<SelectionPoint> {
        val b=bounds(l);val g=l.effectiveFill.gradient;val c=center(g,b)
        if(g.type==GradientType.FREEFORM) return g.freeformPoints.map{SelectionPoint(b.left+b.width*it.x,b.top+b.height*it.y)}
        val r=if(g.type==GradientType.LINEAR || g.type==GradientType.REFLECTED) hypot(b.width,b.height)*g.radius else max(b.width,b.height)*g.radius
        val a=Math.toRadians(g.angleDegrees.toDouble());val delta=SelectionPoint((cos(a)*r).toFloat(),(sin(a)*r).toFloat())
        return if(g.type==GradientType.LINEAR) listOf(c,c-delta,c+delta) else listOf(c,c+delta)
    }
    fun screen(p:SelectionPoint,l:Layer):Offset {
        val w=SelectionGeometry.world(p,l.transform)
        return Offset(viewport.width/2f+livePan.x+w.x*scale,viewport.height/2f+livePan.y+w.y*scale)
    }
    Canvas(Modifier.fillMaxSize().onSizeChanged{viewport=it}.testTag("gradient_handles")
        .pointerInput(layer.id) {
            awaitEachGesture {
                val down=awaitFirstDown(requireUnconsumed=false,pass=PointerEventPass.Initial)
                val original=live
                if(original.isLocked || scale<=0f) return@awaitEachGesture
                val points=handles(original)
                val hit=points.indices.minByOrNull{(screen(points[it],original)-down.position).getDistance()}
                    ?.takeIf{(screen(points[it],original)-down.position).getDistance()<=22.dp.toPx()} ?: return@awaitEachGesture
                down.consume()
                var changed=false;var cancelled=false
                var working=original.effectiveFill
                while(true) {
                    val event=awaitPointerEvent(PointerEventPass.Initial)
                    val c=event.changes.firstOrNull{it.id==down.id} ?: break
                    if(event.changes.count{it.pressed}>1) {cancelled=true;break}
                    if(!c.pressed) {cancelled=c.isConsumed;break}
                    val worldX=(c.position.x-viewport.width/2f-livePan.x)/scale
                    val worldY=(c.position.y-viewport.height/2f-livePan.y)/scale
                    val xy=VectorEditing.toLocal(worldX,worldY,original.transform)
                    val b=bounds(original);val g=original.effectiveFill.gradient
                    val p=SelectionPoint(xy.first,xy.second)
                    val updated=if(g.type==GradientType.FREEFORM) {
                        g.copy(freeformPoints=g.freeformPoints.mapIndexed{i,it->if(i==hit) it.copy(
                            x=((p.x-b.left)/b.width).coerceIn(0f,1f),y=((p.y-b.top)/b.height).coerceIn(0f,1f)) else it})
                    } else if(hit==0) g.copy(centerX=((p.x-b.left)/b.width).coerceIn(0f,1f),centerY=((p.y-b.top)/b.height).coerceIn(0f,1f))
                    else {
                        val oldCenter=center(g,b)
                        val other=if(g.type==GradientType.LINEAR) points[if(hit==1) 2 else 1] else oldCenter
                        val newCenter=if(g.type==GradientType.LINEAR) SelectionPoint((p.x+other.x)/2,(p.y+other.y)/2) else oldCenter
                        val delta=if(hit==1 && g.type==GradientType.LINEAR) other-p else p-other
                        val divisor=if(g.type==GradientType.LINEAR) 2*hypot(b.width,b.height) else if(g.type==GradientType.REFLECTED) hypot(b.width,b.height) else max(b.width,b.height)
                        g.copy(centerX=((newCenter.x-b.left)/b.width).coerceIn(0f,1f),centerY=((newCenter.y-b.top)/b.height).coerceIn(0f,1f),
                            angleDegrees=Math.toDegrees(atan2(delta.y.toDouble(),delta.x.toDouble())).toFloat(),radius=(hypot(delta.x,delta.y)/divisor).coerceIn(.01f,2f))
                    }
                    working=original.effectiveFill.copy(gradient=updated)
                    preview(working);changed=true;c.consume()
                }
                if(cancelled) preview(original.effectiveFill) else if(changed) commit(working)
            }
        }) {
        val points=handles(layer).map{screen(it,layer)}
        if(layer.effectiveFill.gradient.type!=GradientType.FREEFORM && points.size>=2) {
            drawLine(Color.White,points.last(),points[if(points.size==3) 1 else 0],2.dp.toPx())
        }
        points.forEachIndexed{i,p ->
            drawCircle(Color.Black,9.dp.toPx(),p)
            drawCircle(if(i==0) Color.Yellow else Color.White,6.dp.toPx(),p)
        }
    }
}
