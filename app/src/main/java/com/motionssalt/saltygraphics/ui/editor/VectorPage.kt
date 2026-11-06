package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.ui.controls.*

enum class VectorTool(val label: String, val hint: String) {
    SELECT("Select", "Tap a point or handle; drag to move"),
    ADD("Add point", "Tap a curve or empty space; drag a new point to bend"),
    REMOVE("Remove point", "Tap a square point to remove it"),
    SMOOTH("Corner/Smooth", "Tap a square point to switch corner or smooth")
}

@Composable
fun VectorCreationPage(onChoose: (String) -> Unit, onBack: () -> Unit) {
    PanelPage("Create vector", onBack, scrollable = false) {
        listOf("Draw with points", "Rectangle", "Ellipse", "Polygon", "Star", "Import SVG").chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { label -> OutlinedButton(onClick = { onChoose(label) }, modifier = Modifier.weight(1f)) { Text(label) } }
            }
        }
    }
}

@Composable
fun VectorPage(layer: Layer, pathIndex: Int, selectedPoint: Int?, tool: VectorTool,
    onPathIndex: (Int) -> Unit, onPoint: (Int?) -> Unit, onTool: (VectorTool) -> Unit,
    onUpdate: (String, Layer) -> Unit, onBack: () -> Unit) {
    var tab by remember(layer.id) { mutableStateOf("Shape") }
    val paths = layer.editablePaths.ifEmpty { listOf(VectorPathData()) }
    val index = pathIndex.coerceIn(0, paths.lastIndex)
    val path = paths[index]
    fun updatePath(description: String, newPath: VectorPathData) {
        onUpdate(description, layer.withVectorPaths(paths.mapIndexed { i,p -> if (i==index) newPath else p }))
    }
    PanelPage("Vector", onBack, scrollable = false) {
        ScrollableTabRow(selectedTabIndex = listOf("Shape","Fill","Border","Path","Transform","Opacity").indexOf(tab), edgePadding = 0.dp) {
            listOf("Shape","Fill","Border","Path","Transform","Opacity").forEach { title ->
                Tab(selected = tab==title, onClick = { tab=title }, text = { Text(title) })
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                paths.forEachIndexed { i,_ -> FilterChip(selected=i==index, onClick={onPathIndex(i);onPoint(null)}, label={Text("Path ${i+1}")}) }
                InputChip(selected=false, onClick={onUpdate("Add vector path", layer.withVectorPaths(paths+VectorPathData()));onPathIndex(paths.size);onPoint(null)}, label={Text("+ Add")})
            }
            when(tab) {
                "Shape" -> {
                    VectorTool.entries.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth()) {
                        row.forEach { item -> FilledTonalButton(onClick={onTool(item)}, modifier=Modifier.weight(1f).padding(2.dp)) { Text(item.label) } }
                    } }
                    Text(tool.hint, style=MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick={updatePath(if(path.isClosed) "Open path" else "Close path",path.copy(isClosed=!path.isClosed))}) { Text(if(path.isClosed) "Open path" else "Close path") }
                    Text("${path.points.size} points", style=MaterialTheme.typography.labelSmall)
                }
                "Fill" -> {
                    ToggleControl("Solid fill",path.hasFill,{updatePath("Vector fill",path.copy(hasFill=it))})
                    ColorPickerRow("Fill colour",path.fillColor,{updatePath("Vector fill colour",path.copy(fillColor=it,hasFill=true))},allowTransparent=true)
                }
                "Border" -> {
                    ToggleControl("Border",path.hasStroke,{updatePath("Vector border",path.copy(hasStroke=it))})
                    ColorPickerRow("Border colour",path.strokeColor,{updatePath("Vector border colour",path.copy(strokeColor=it))},allowTransparent=true)
                    RulerSlider("Width",path.strokeWidth,{updatePath("Border width",path.copy(strokeWidth=it))},0f,200f,4f,1f,"px")
                    DropdownControl("Cap",VectorCap.entries.map{it.name},path.strokeCap.name,{v->updatePath("Border cap",path.copy(strokeCap=VectorCap.valueOf(v)))})
                    DropdownControl("Join",VectorJoin.entries.map{it.name},path.strokeJoin.name,{v->updatePath("Border join",path.copy(strokeJoin=VectorJoin.valueOf(v)))})
                    RulerSlider("Dash",path.dashLength,{updatePath("Border dash",path.copy(dashLength=it))},0f,200f,0f,1f,"px")
                    RulerSlider("Gap",path.dashGap,{updatePath("Border gap",path.copy(dashGap=it))},1f,200f,8f,1f,"px")
                }
                "Path" -> {
                    val point=path.points.getOrNull(selectedPoint ?: -1)
                    if(point==null) Text("Select a point on the canvas to edit its coordinates") else {
                        fun change(p: BezierPoint) = updatePath("Point coordinates",path.copy(points=path.points.mapIndexed{i,v->if(i==selectedPoint)p else v}))
                        RulerSlider("Point X",point.x,{change(VectorEditing.movePoint(point,it,point.y))},-32768f,32768f,0f,1f,"px")
                        RulerSlider("Point Y",point.y,{change(VectorEditing.movePoint(point,point.x,it))},-32768f,32768f,0f,1f,"px")
                        RulerSlider("In X",point.inHandleX,{change(point.copy(inHandleX=it))},-32768f,32768f,point.x,1f,"px")
                        RulerSlider("In Y",point.inHandleY,{change(point.copy(inHandleY=it))},-32768f,32768f,point.y,1f,"px")
                        RulerSlider("Out X",point.outHandleX,{change(point.copy(outHandleX=it))},-32768f,32768f,point.x,1f,"px")
                        RulerSlider("Out Y",point.outHandleY,{change(point.copy(outHandleY=it))},-32768f,32768f,point.y,1f,"px")
                    }
                }
                "Transform" -> TransformPanel(layer.transform, onTransformChange = { onUpdate("Vector transform", layer.copy(transform = it)) }, onClose = onBack)
                "Opacity" -> ProgressSliderControl("Opacity",layer.opacity*100f,{onUpdate("Vector opacity",layer.copy(opacity=it/100f))},0f,100f,100f,1f,"%")
            }
        }
    }
}
