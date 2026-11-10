package com.motionssalt.saltygraphics.ui.controls

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.FillRenderer
import com.motionssalt.saltygraphics.ui.editor.PanelPage
import kotlin.math.abs

/** All navigation replaces content within the fixed tools pane. No nested windows. */
@Composable
fun FillPanelPage(
    fill: FillData,
    media: List<Pair<String, android.graphics.Bitmap?>>,
    onPreview: (FillData) -> Unit,
    onCommit: (String, FillData) -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit
) {
    var page by remember { mutableStateOf("fill") }
    var stopIndex by remember { mutableIntStateOf(0) }
    var pointIndex by remember { mutableIntStateOf(0) }
    val live by rememberUpdatedState(fill)
    val preview by rememberUpdatedState(onPreview)
    val commit by rememberUpdatedState(onCommit)
    fun back() { page = when (page) { "stopColor", "pointColor" -> "gradient"; else -> "fill" } }
    BackHandler(page != "fill") { back() }
    fun stop(g: GradientData) = stopIndex.coerceIn(0, (g.stops.size - 1).coerceAtLeast(0))
    fun point(g: GradientData) = pointIndex.coerceIn(0, (g.freeformPoints.size - 1).coerceAtLeast(0))
    fun setStopColor(c: Long, save: Boolean) {
        val f = live; val i = stop(f.gradient)
        val updated = f.copy(gradient = f.gradient.copy(stops = f.gradient.stops.mapIndexed { idx, s ->
            if (idx == i) s.copy(color = c, opacity = 1f) else s
        }))
        if (save) commit("Change gradient stop color", updated) else preview(updated)
    }
    fun setPointColor(c: Long, save: Boolean) {
        val f = live; val i = point(f.gradient)
        val updated = f.copy(gradient = f.gradient.copy(freeformPoints = f.gradient.freeformPoints.mapIndexed { idx, p ->
            if (idx == i) p.copy(color = c, opacity = 1f) else p
        }))
        if (save) commit("Change freeform point color", updated) else preview(updated)
    }
    when (page) {
        "solidColor" -> PanelColorPage("Fill Color", fill.effectiveSolidArgb,
            { preview(live.copy(solidColor = it, solidOpacity = 1f)) },
            { commit("Change fill color", live.copy(solidColor = it, solidOpacity = 1f)) }, { back() })
        "stopColor" -> PanelColorPage("Gradient Stop Color", fill.gradient.stops.getOrNull(stop(fill.gradient))?.effectiveArgb ?: 0L,
            { setStopColor(it,false) }, { setStopColor(it,true) }, { back() })
        "pointColor" -> PanelColorPage("Freeform Point Color", fill.gradient.freeformPoints.getOrNull(point(fill.gradient))?.effectiveArgb ?: 0L,
            { setPointColor(it,false) }, { setPointColor(it,true) }, { back() })
        "gradient" -> PanelPage("Gradient Settings", { page = "fill" }) {
            val g = fill.gradient
            SegmentedChipsControl("Gradient type", GradientType.entries.map { it.displayName }, g.type.displayName, { label ->
                commit("Change gradient type", live.copy(gradient = live.gradient.copy(type = GradientType.entries.first { it.displayName == label })))
            })
            Canvas(Modifier.fillMaxWidth().height(60.dp).testTag("gradient_preview")) {
                drawIntoCanvas { c ->
                    val bounds = android.graphics.RectF(0f,0f,size.width,size.height)
                    val paint = android.graphics.Paint().apply {
                        shader = FillRenderer.createGradientShader(g,bounds); isDither = g.dither
                    }
                    c.nativeCanvas.drawRect(bounds,paint)
                }
            }
            if (g.type != GradientType.FREEFORM) {
                Text("Color Stops (${g.stops.size})")
                Canvas(Modifier.fillMaxWidth().height(48.dp).testTag("gradient_stop_rail")
                    .pointerInput(Unit) {
                        var dragged = -1
                        detectDragGestures(onDragStart = { p ->
                            dragged = live.gradient.stops.indices.minByOrNull { abs(live.gradient.stops[it].position - p.x / size.width) } ?: -1
                            if (dragged >= 0) stopIndex = dragged
                        }, onDragEnd = { if (dragged >= 0) commit("Move gradient stop",live) },
                            onDragCancel = { if (dragged >= 0) commit("Move gradient stop",live) }) { change, _ ->
                            if (dragged >= 0) {
                                val f = live
                                preview(f.copy(gradient = f.gradient.copy(stops = f.gradient.stops.mapIndexed { i,s ->
                                    if (i == dragged) s.copy(position = (change.position.x / size.width).coerceIn(0f,1f)) else s
                                })))
                                change.consume()
                            }
                        }
                    }) {
                    drawLine(Color.Gray,Offset(0f,size.height/2),Offset(size.width,size.height/2),3f)
                    g.stops.forEachIndexed { i,s ->
                        drawCircle(if (i == stop(g)) Color.White else Color.Gray,12f,Offset(s.position*size.width,size.height/2))
                        drawCircle(Color(s.effectiveArgb.toInt()),8f,Offset(s.position*size.width,size.height/2))
                    }
                }
                SegmentedChipsControl("Selected stop", g.stops.indices.map { "Stop ${it+1}" }, "Stop ${stop(g)+1}", {
                    stopIndex = it.substringAfter("Stop ").toInt()-1
                })
                Row {
                    TextButton(onClick = {
                        val f=live; stopIndex=f.gradient.stops.size
                        commit("Add gradient stop",f.copy(gradient=f.gradient.copy(stops=f.gradient.stops+GradientStop(position=.5f))))
                    }) { Text("Add stop") }
                    TextButton(enabled=g.stops.size>2,onClick={
                        val f=live; val i=stop(f.gradient)
                        stopIndex=(i-1).coerceAtLeast(0)
                        commit("Delete gradient stop",f.copy(gradient=f.gradient.copy(stops=f.gradient.stops.filterIndexed { idx,_ -> idx!=i })))
                    }) { Text("Delete stop") }
                }
                g.stops.getOrNull(stop(g))?.let { s ->
                    TextButton(onClick={page="stopColor"}) { Text("Stop color and alpha") }
                    GradientNumber("Stop position",s.position,0f,1f,fill,onPreview,onCommit) { grad,v ->
                        grad.copy(stops=grad.stops.mapIndexed { i,it -> if(i==stop(grad)) it.copy(position=v) else it })
                    }
                    GradientNumber("Stop opacity",s.opacity,0f,1f,fill,onPreview,onCommit) { grad,v ->
                        grad.copy(stops=grad.stops.mapIndexed { i,it -> if(i==stop(grad)) it.copy(opacity=v) else it })
                    }
                    GradientNumber("Midpoint",s.midpoint,.01f,.99f,fill,onPreview,onCommit) { grad,v ->
                        grad.copy(stops=grad.stops.mapIndexed { i,it -> if(i==stop(grad)) it.copy(midpoint=v) else it })
                    }
                }
                Row {
                    TextButton(onClick={commit("Reverse gradient",live.copy(gradient=live.gradient.reversed()))}) { Text("Reverse") }
                    TextButton(onClick={commit("Distribute gradient stops",live.copy(gradient=live.gradient.distributeEvenly()))}) { Text("Distribute evenly") }
                }
            } else {
                SegmentedChipsControl("Selected point",g.freeformPoints.indices.map{"Point ${it+1}"},"Point ${point(g)+1}",{
                    pointIndex=it.substringAfter("Point ").toInt()-1
                })
                Row {
                    TextButton(onClick={
                        val f=live; pointIndex=f.gradient.freeformPoints.size
                        commit("Add freeform point",f.copy(gradient=f.gradient.copy(freeformPoints=f.gradient.freeformPoints+FreeformPoint())))
                    }) { Text("Add point") }
                    TextButton(enabled=g.freeformPoints.size>1,onClick={
                        val f=live;val i=point(f.gradient); pointIndex=(i-1).coerceAtLeast(0)
                        commit("Delete freeform point",f.copy(gradient=f.gradient.copy(freeformPoints=f.gradient.freeformPoints.filterIndexed{idx,_->idx!=i})))
                    }) { Text("Delete point") }
                }
                TextButton(onClick={page="pointColor"}) { Text("Point color and alpha") }
                g.freeformPoints.getOrNull(point(g))?.let { p ->
                    listOf("Point X" to p.x,"Point Y" to p.y,"Point radius" to p.radius).forEach { (label,value) ->
                        GradientNumber(label,value,0f,1f,fill,onPreview,onCommit) { grad,v ->
                            grad.copy(freeformPoints=grad.freeformPoints.mapIndexed { i,it ->
                                if(i!=point(grad)) it else when(label) {"Point X"->it.copy(x=v);"Point Y"->it.copy(y=v);else->it.copy(radius=v.coerceAtLeast(.01f))}
                            })
                        }
                    }
                }
            }
            GradientNumber("Angle",g.angleDegrees,-360f,360f,fill,onPreview,onCommit){it,v->it.copy(angleDegrees=v)}
            GradientNumber("Center X",g.centerX,0f,1f,fill,onPreview,onCommit){it,v->it.copy(centerX=v)}
            GradientNumber("Center Y",g.centerY,0f,1f,fill,onPreview,onCommit){it,v->it.copy(centerY=v)}
            GradientNumber("Radius",g.radius,.01f,2f,fill,onPreview,onCommit){it,v->it.copy(radius=v)}
            GradientNumber("Aspect",g.aspect,.05f,10f,fill,onPreview,onCommit){it,v->it.copy(aspect=v)}
            SegmentedChipsControl("Repeat",GradientRepeatMode.entries.map{it.displayName},g.repeatMode.displayName,{ label ->
                commit("Change gradient repeat",live.copy(gradient=live.gradient.copy(repeatMode=GradientRepeatMode.entries.first{it.displayName==label})))
            })
            SegmentedChipsControl("Interpolation",GradientInterpolation.entries.map{it.displayName},g.interpolation.displayName,{ label ->
                commit("Change gradient interpolation",live.copy(gradient=live.gradient.copy(interpolation=GradientInterpolation.entries.first{it.displayName==label})))
            })
            ToggleControl("Dither",g.dither,{commit("Change gradient dithering",live.copy(gradient=live.gradient.copy(dither=it)))})
        }
        "media" -> PanelPage("Project Images",{page="fill"}) {
            Button(onClick=onImport) { Text("Import from device") }
            if(media.isEmpty()) Text("No images in this project yet")
            media.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { (path,bitmap) ->
                        Column(Modifier.weight(1f).clickable {
                            commit("Choose media fill",live.copy(type=FillType.MEDIA,mediaAssetPath=path));page="fill"
                        }.testTag("fill_media_$path")) {
                            if(bitmap!=null) Image(bitmap.asImageBitmap(),path,Modifier.size(72.dp))
                            else Text("Image")
                            Text(path.substringAfterLast('/'),maxLines=1)
                        }
                    }
                }
            }
        }
        else -> PanelPage("Fill Settings",onBack) {
            SegmentedChipsControl("Fill type",FillType.entries.map{it.displayName},fill.type.displayName, { label ->
                commit("Change fill type",live.copy(type=FillType.entries.first{it.displayName==label}))
            })
            when(fill.type) {
                FillType.SOLID -> {
                    TextButton(onClick={page="solidColor"}) {Text("Color and alpha")}
                    TextButton(onClick={commit("Make fill transparent",live.copy(solidOpacity=0f))}) {Text("Transparent")}
                    PanelNumberControl("Fill Opacity",fill.solidOpacity*100f,0f,100f,unit="%",
                        onPreview={preview(live.copy(solidOpacity=it/100f))},onCommit={commit("Change fill opacity",live.copy(solidOpacity=it/100f))})
                }
                FillType.GRADIENT -> Button(onClick={page="gradient"}) {Text("Edit Gradient")}
                FillType.MEDIA -> {
                    Button(onClick={page="media"}) {Text(if(fill.mediaAssetPath==null) "Choose Media Image" else "Replace")}
                    SegmentedChipsControl("Media Scale Mode",MediaScaleMode.entries.map{it.displayName},fill.mediaScaleMode.displayName,{label ->
                        commit("Change media fit",live.copy(mediaScaleMode=MediaScaleMode.entries.first{it.displayName==label}))
                    })
                    listOf("Media scale" to fill.mediaScale,"Media offset X" to fill.mediaOffsetX,"Media offset Y" to fill.mediaOffsetY,"Media rotation" to fill.mediaRotation).forEach { (label,value) ->
                        val range=when(label){"Media scale"->.05f..20f;"Media rotation"->-360f..360f;else->-2f..2f}
                        fun set(v:Float)=when(label){"Media scale"->live.copy(mediaScale=v);"Media offset X"->live.copy(mediaOffsetX=v);"Media offset Y"->live.copy(mediaOffsetY=v);else->live.copy(mediaRotation=v)}
                        PanelNumberControl(label,value,range.start,range.endInclusive,step=.01f,
                            onPreview={preview(set(it))},onCommit={commit("Change ${label.lowercase()}",set(it))})
                    }
                }
            }
        }
    }
}

@Composable
private fun GradientNumber(label:String,value:Float,min:Float,max:Float,fill:FillData,
    preview:(FillData)->Unit,commit:(String,FillData)->Unit,change:(GradientData,Float)->GradientData) {
    val live by rememberUpdatedState(fill)
    PanelNumberControl(label,value,min,max,step=if(max>100f) 1f else .01f,
        onPreview={preview(live.copy(gradient=change(live.gradient,it)))},
        onCommit={commit("Change gradient ${label.lowercase()}",live.copy(gradient=change(live.gradient,it)))})
}
