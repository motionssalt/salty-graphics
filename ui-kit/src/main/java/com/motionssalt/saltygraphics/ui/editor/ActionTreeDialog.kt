package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.history.*
import kotlin.math.roundToInt

@Composable
fun ActionTreePanelPage("Action History Tree", onDismiss, scrollable = false) {
    val colors=MaterialTheme.colorScheme
    val density=LocalDensity.current
    // A map copy is an immutable cache key; the engine's live mutable Map is not.
    val snapshot=actionTree.toSnapshot()
    val graph=remember(snapshot) {HistoryGraphCalculator.computeGraph(snapshot.nodes,snapshot.rootNodeId,snapshot.currentNodeId)}
    var viewport by remember {mutableStateOf(HistoryViewport())}
    val current=graph.nodes.firstOrNull {it.isCurrent}

    PanelPage("Action History Tree", onDismiss, scrollable = false) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.94f).padding(12.dp),shape=RoundedCornerShape(16.dp),color=colors.surface) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountTree,null,tint=colors.primary)
                    Text("Action History Tree",Modifier.weight(1f).padding(horizontal=8.dp),style=MaterialTheme.typography.titleMedium)
                    IconButton(onClick=onDismiss) {Icon(Icons.Default.Close,"Close history")}
                }
                Text("Active path highlighted; faded branches are retained. Tap a node to continue there.",style=MaterialTheme.typography.bodySmall)
                Text("Actions: ${graph.nodes.size}",style=MaterialTheme.typography.labelSmall)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val width=maxWidth.value;val height=maxHeight.value
                    fun centered()=current?.let {viewport.centered(HistoryGeometry.x(it)+100f,HistoryGeometry.y(it)+24f,width,height)} ?: viewport
                    fun fit():HistoryViewport {
                        val right=(graph.nodes.maxOfOrNull {HistoryGeometry.x(it)+HistoryGeometry.NODE_WIDTH} ?: 200f)+24f
                        val bottom=(graph.nodes.maxOfOrNull {HistoryGeometry.y(it)+HistoryGeometry.NODE_HEIGHT} ?: 48f)+24f
                        val scale=minOf(width/right,(height-52f)/bottom,1f).coerceIn(.25f,1f)
                        return HistoryViewport(scale).centered(right/2f,bottom/2f,width,height-52f)
                    }
                    LaunchedEffect(width,height,snapshot.currentNodeId) {viewport=centered()}
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceEvenly) {
                            IconButton(onClick={viewport=viewport.zoomAt(.8f,width/2,height/2)}) {Icon(Icons.Default.ZoomOut,"Zoom out history")}
                            Text("${(viewport.scale*100).roundToInt()}%",style=MaterialTheme.typography.labelSmall)
                            IconButton(onClick={viewport=viewport.zoomAt(1.25f,width/2,height/2)}) {Icon(Icons.Default.ZoomIn,"Zoom in history")}
                            IconButton(onClick={viewport=centered()}) {Icon(Icons.Default.MyLocation,"Current action")}
                            IconButton(onClick={viewport=fit()}) {Icon(Icons.Default.FitScreen,"Fit tree")}
                        }
                        val visible=graph.nodes.filter {HistoryGeometry.visible(it,viewport,width,height-48f)}
                        Box(Modifier.fillMaxSize().clipToBounds().background(colors.surfaceContainerLowest)
                            .testTag("history_viewport")
                            .semantics {stateDescription="Zoom ${(viewport.scale*100).roundToInt()} percent; ${visible.size} visible nodes"}
                            .pointerInput(Unit) {
                                detectTransformGestures {centroid,pan,zoom,_ ->
                                    viewport=viewport.zoomAt(zoom,centroid.x/density.density,centroid.y/density.density)
                                        .pan(pan.x/density.density,pan.y/density.density)
                                }
                            }) {
                            Canvas(Modifier.fillMaxSize()) {
                                graph.edges.forEach {edge ->
                                    if(HistoryGeometry.edgeVisible(edge,viewport,width,height-48f)) {
                                        fun point(lane:Int,row:Int,bottom:Boolean)=Offset(
                                            viewport.screenX(lane*HistoryGeometry.LANE_STEP+24f+HistoryGeometry.NODE_WIDTH/2)*density.density,
                                            viewport.screenY(row*HistoryGeometry.ROW_STEP+24f+(if(bottom) HistoryGeometry.NODE_HEIGHT else 0f))*density.density)
                                        val a=point(edge.fromLane,edge.fromYIndex,true);val b=point(edge.toLane,edge.toYIndex,false)
                                        val middle=(a.y+b.y)/2f
                                        val path=Path().apply {moveTo(a.x,a.y);cubicTo(a.x,middle,b.x,middle,b.x,b.y)}
                                        drawPath(path,if(edge.isOnMainPath) colors.primary else colors.outline.copy(alpha=.45f),style=Stroke((if(edge.isOnMainPath) 2.5f else 1.5f)*density.density))
                                    }
                                }
                            }
                            visible.forEach {node ->
                                key(node.node.id) {
                                    val scale=viewport.scale
                                    val active=node.isOnMainPath
                                    val outline=if(node.isCurrent) colors.primary else if(active) colors.outline else colors.outline.copy(alpha=.45f)
                                    val icon=when {
                                        node.isCurrent -> Icons.Default.CheckCircle
                                        node.isBranchPoint -> Icons.Default.AccountTree
                                        node.node.description.contains("Delete",true) -> Icons.Default.Delete
                                        node.node.description.contains("Move",true) || node.node.description.contains("Transform",true) -> Icons.Default.OpenWith
                                        node.node.description.contains("Add",true) || node.node.description.contains("Create",true) -> Icons.Default.Add
                                        else -> Icons.Default.Edit
                                    }
                                    Row(Modifier.offset {IntOffset((viewport.screenX(HistoryGeometry.x(node))*density.density).roundToInt(),(viewport.screenY(HistoryGeometry.y(node))*density.density).roundToInt())}
                                        .size((HistoryGeometry.NODE_WIDTH*scale).dp,(HistoryGeometry.NODE_HEIGHT*scale).dp)
                                        .testTag("history_node_${node.node.id}")
                                        .semantics {selected=node.isCurrent;stateDescription=if(node.isCurrent) "Current action" else if(active) "Active path" else "Retained branch"}
                                        .background(if(node.isCurrent) colors.primaryContainer else if(active) colors.surfaceContainerHigh else colors.surfaceContainer.copy(alpha=.65f),RoundedCornerShape(6.dp))
                                        .border(if(node.isCurrent) 2.dp else 1.dp,outline,RoundedCornerShape(6.dp))
                                        .clickable {onJumpToNode(node.node.id);onDismiss()}
                                        .padding((6f*scale).dp),verticalAlignment=Alignment.CenterVertically) {
                                        Icon(icon,null,Modifier.size((18f*scale).dp),tint=if(active) colors.primary else colors.onSurfaceVariant.copy(alpha=.65f))
                                        Column(Modifier.weight(1f).padding(start=(5f*scale).dp)) {
                                            Text(node.node.description,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=(12f*scale).sp,
                                                fontWeight=if(active) FontWeight.Bold else FontWeight.Normal,color=if(active) colors.onSurface else colors.onSurfaceVariant.copy(alpha=.7f))
                                            Text(if(node.isCurrent) "Current" else if(node.isBranchPoint) "Branch" else if(!active) "Retained" else "Active",
                                                fontSize=(9f*scale).sp,maxLines=1,color=if(active) colors.primary else colors.onSurfaceVariant.copy(alpha=.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

