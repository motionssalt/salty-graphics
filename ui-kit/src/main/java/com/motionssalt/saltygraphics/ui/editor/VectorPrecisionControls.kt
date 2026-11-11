package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.VectorEditing
import com.motionssalt.saltygraphics.ui.controls.SaltySwitch
import com.motionssalt.saltygraphics.ui.controls.ToggleControl

/** Library-owned vector toolbar. It lives in the panel, never over canvas geometry. */
@Composable
fun VectorPrecisionControls(layer: Layer, pathIndex: Int, selectedPoint: Int?, precision: VectorPrecisionState,
    onPoint: (Int?)->Unit, onUpdate: (String,Layer)->Unit) {
    val paths=layer.editablePaths
    val index=pathIndex.coerceIn(0,(paths.size-1).coerceAtLeast(0))
    val path=paths.getOrNull(index) ?: return
    val point=path.points.getOrNull(selectedPoint ?: -1) ?: return
    ToggleControl("Independent Handles",precision.independentHandles,{precision.independentHandles=it},
        modifier=Modifier.testTag("toggle_independent_handles"))
    OutlinedButton(onClick={
        val updated=if(point.hasInHandle||point.hasOutHandle)VectorEditing.makeCorner(point)else VectorEditing.makeSmooth(point)
        onUpdate(if(point.hasInHandle||point.hasOutHandle)"Convert anchor to corner"else"Convert anchor to smooth",
            layer.withVectorPaths(paths.mapIndexed{i,value->if(i==index)value.copy(points=value.points.mapIndexed{j,p->if(j==selectedPoint)updated else p})else value}))
    },modifier=Modifier.testTag("toggle_corner_smooth")){Text(if(point.hasInHandle||point.hasOutHandle)"To Corner"else"To Smooth")}
    ToggleControl("45° Snap",precision.angleLock45,{precision.angleLock45=it},modifier=Modifier.testTag("toggle_angle_lock"))
    OutlinedButton(onClick={
        onPoint(null)
        onUpdate("Delete vector point",layer.withVectorPaths(paths.mapIndexed{i,value->
            if(i==index)value.copy(points=value.points.filterIndexed{j,_->j!=selectedPoint})else value}))
    },modifier=Modifier.testTag("delete_vector_point")){Text("Delete Point")}
}
