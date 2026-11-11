package com.motionssalt.saltygraphics.uikit.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.uikit.tokens.*
import com.motionssalt.saltygraphics.ui.controls.*
import com.motionssalt.saltygraphics.ui.editor.PanelPage

/** Common non-canvas screen host. Domain screens provide state/actions, not window layout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldScreen(title: String, onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}, floatingAction: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back") }
    }, actions = actions) }, floatingActionButton = floatingAction, content = content)
}

@Composable fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = SaltyShapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(SaltySpacing.lg), verticalArrangement = Arrangement.spacedBy(SaltySpacing.md)) {
            if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
@Composable fun LabelRow(label: String, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) { Text(label); control() }
}
@Composable fun TileGrid(tiles: List<String>, selected: String? = null, onSelect: (String) -> Unit) {
    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxWidth().heightIn(max = 240.dp),
        horizontalArrangement = Arrangement.spacedBy(SaltySpacing.md),
        verticalArrangement = Arrangement.spacedBy(SaltySpacing.md)) {
        items(tiles) { Tile(it, it == selected) { onSelect(it) } }
    }
}
@Composable fun Tile(label: String, selected: Boolean = false, thumbnail: ImageBitmap? = null, onClick: () -> Unit = {}) {
    Surface(onClick = onClick, shape = SaltyShapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (selected) CardDefaults.outlinedCardBorder() else null) {
        Column(Modifier.padding(SaltySpacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
            if (thumbnail != null) androidx.compose.foundation.Image(thumbnail, null, Modifier.size(SaltySpacing.touchTarget))
            else Icon(if (selected) Icons.Default.Check else Icons.Default.Add, null)
            Text(label, maxLines = 1)
        }
    }
}
@Composable fun ThumbnailListItem(title: String, subtitle: String = "", thumbnail: ImageBitmap? = null,
    handle: (@Composable () -> Unit)? = null, onClick: () -> Unit = {}) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(SaltySpacing.md), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SaltySpacing.md)) {
            if (thumbnail != null) androidx.compose.foundation.Image(thumbnail,null,Modifier.size(SaltySpacing.touchTarget))
            Column(Modifier.weight(1f)) { Text(title); if (subtitle.isNotBlank()) Text(subtitle,style=MaterialTheme.typography.bodySmall) }
            handle?.invoke()
        }
    }
}
@Composable fun EmptyState(title: String, detail: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(SaltySpacing.xxl), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SaltySpacing.md)) {
        Text(title,style=MaterialTheme.typography.titleMedium); Text(detail,style=MaterialTheme.typography.bodyMedium)
        if(actionLabel!=null && onAction!=null) Button(onClick=onAction){Text(actionLabel)}
    }
}
@Composable fun PanelToast(message: String) {
    Surface(shape=SaltyShapes.medium,color=MaterialTheme.colorScheme.inverseSurface,
        modifier=Modifier.testTag("library_toast")) { Text(message,Modifier.padding(SaltySpacing.lg),color=MaterialTheme.colorScheme.inverseOnSurface) }
}
@Composable fun ConfirmationDialog(title: String, detail: String, confirmLabel: String,
    onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(onDismissRequest=onCancel,title={Text(title)},text={Text(detail)},
        confirmButton={Button(onClick=onConfirm){Text(confirmLabel)}},dismissButton={TextButton(onClick=onCancel){Text("Cancel")}})
}
@Composable fun TopBarItem(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick=onClick,enabled=enabled){Icon(Icons.Default.Check,label)}
}
@Composable fun ProjectCard(title: String, detail: String, thumbnail: ImageBitmap? = null, onClick: () -> Unit = {}) =
    ThumbnailListItem(title,detail,thumbnail,onClick=onClick)
@Composable fun LayerPanelRow(title: String, subtitle: String, visible: Boolean, locked: Boolean,
    onVisibility: (Boolean)->Unit, onLock: (Boolean)->Unit, onClick: () -> Unit = {}) =
    ThumbnailListItem(title,subtitle,handle={Row{ToggleControl("Visible",visible,onVisibility);ToggleControl("Locked",locked,onLock)}},onClick=onClick)

/** Special library surface: gradient stops, selection and transparent checkerboard stay panel-native. */
@Composable fun GradientEditorControl(gradient: GradientData, onChange: (GradientData)->Unit,
    modifier: Modifier = Modifier) {
    val stops=gradient.stops
    Canvas(modifier.fillMaxWidth().height(SaltySpacing.touchTarget).testTag("gradient_bar")) {
        drawRect(Brush.linearGradient(stops.map{Color(it.effectiveArgb)}.ifEmpty{listOf(Color.Transparent,Color.Transparent)}))
    }
    stops.forEachIndexed{index,stop->ColorPickerRow("Stop ${index+1}",stop.effectiveArgb,{
        onChange(gradient.copy(stops=stops.mapIndexed{i,value->if(i==index)value.copy(color=it,opacity=1f)else value}))
    },allowTransparent=true)}
}
@Composable fun MediaPickerGrid(items: List<Pair<String,ImageBitmap?>>, onSelect: (String)->Unit) {
    if(items.isEmpty()) EmptyState("No media","Import an image to use it here") else TileGrid(items.map{it.first},onSelect=onSelect)
}
@Composable fun LayerSelector(layers: List<Layer>, selectedId: String?, onSelect: (String?)->Unit) {
    DropdownControl("Layer",listOf("None")+layers.map{it.id},selectedId?:"None",onOptionSelected={value->onSelect(value.takeUnless{it=="None"})})
}
@Composable fun DockedValueEntry(label: String, value: Float, min: Float, max: Float,
    unit: String = "", onApply: (Float)->Unit, onDismiss: ()->Unit) =
    ValueEditorDialog(label,value,min,max,unit=unit,onValueApplied=onApply,onDismiss=onDismiss)
@Composable fun TrackpadControl(onPreview: (Offset)->Unit, onCommit: ()->Unit = {}) {
    var total by remember{mutableStateOf(Offset.Zero)}
    Box(Modifier.fillMaxWidth().height(96.dp).background(MaterialTheme.colorScheme.surfaceContainerLow,SaltyShapes.medium)
        .pointerInput(Unit){detectDragGestures(onDragEnd={total=Offset.Zero;onCommit()},onDragCancel={total=Offset.Zero}){change,amount->
            change.consume();total+=amount;onPreview(amount)
        }}.testTag("library_trackpad"),contentAlignment=Alignment.Center){Text("Drag to adjust precisely")}
}

@Composable fun CanvasOverlayPreview(kind: String) {
    Canvas(Modifier.fillMaxWidth().height(120.dp).testTag("overlay_$kind")) {
        drawRect(Color.DarkGray)
        when(kind) {
            "selection" -> { drawRect(Color.Cyan,style=androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())); drawCircle(Color.White,6.dp.toPx(),center) }
            "gradient" -> { drawLine(Color.Yellow,Offset.Zero,center,2.dp.toPx());drawCircle(Color.White,6.dp.toPx(),center) }
            "vector" -> { drawCircle(Color.Cyan,6.dp.toPx(),center);drawLine(Color.Cyan,center,center+Offset(40f,-20f)) }
            "snapping" -> drawLine(Color.Red,Offset(size.width/2,0f),Offset(size.width/2,size.height),2.dp.toPx())
            "grid" -> { for(x in 0..size.width.toInt() step 24)drawLine(Color.Gray,Offset(x.toFloat(),0f),Offset(x.toFloat(),size.height)) }
        }
    }
}
