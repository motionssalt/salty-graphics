package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.Layer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

/** Gesture ownership stays on the list, never on a LazyColumn row that can be recycled mid-drag. */
@Composable
internal fun ReorderableLayerList(
    layers: List<Layer>,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (Layer, Boolean) -> Unit
) {
    val state = rememberLazyListState()
    val currentLayers by rememberUpdatedState(layers)
    val currentMove by rememberUpdatedState(onMove)
    var draggedId by remember { mutableStateOf<String?>(null) }
    var pointerY by remember { mutableFloatStateOf(0f) }
    var target by remember { mutableIntStateOf(-1) }
    val edge = with(LocalDensity.current) { 48.dp.toPx() }
    val maxSpeed = with(LocalDensity.current) { 14.dp.toPx() }

    fun updateTarget() {
        target = state.layoutInfo.visibleItemsInfo.minByOrNull {
            abs(pointerY - (it.offset + it.size / 2f))
        }?.index ?: -1
    }
    fun finish(commit: Boolean) {
        val from = currentLayers.indexOfFirst { it.id == draggedId }
        val to = target
        draggedId = null
        target = -1
        if (commit && from >= 0 && to in currentLayers.indices && from != to) currentMove(from, to)
    }

    // Keep scrolling even when the user's finger is stationary at an edge.
    LaunchedEffect(draggedId) {
        if (draggedId != null) {
            while (isActive) {
                val height = state.layoutInfo.viewportSize.height.toFloat()
                val delta = when {
                    pointerY < edge -> -maxSpeed * ((edge - pointerY) / edge).coerceIn(0f, 1f)
                    pointerY > height - edge -> maxSpeed * ((pointerY - height + edge) / edge).coerceIn(0f, 1f)
                    else -> 0f
                }
                if (delta != 0f) {
                    state.scrollBy(delta)
                    updateTarget()
                }
                delay(16)
            }
        }
    }

    Box(modifier.testTag("layer_list").semantics {
        stateDescription = draggedId?.let { "Reordering $it to ${target + 1}" } ?: "Layer order"
    }.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { position ->
                val item = state.layoutInfo.visibleItemsInfo.find {
                    position.y >= it.offset && position.y < it.offset + it.size
                }
                if (item != null) {
                    draggedId = currentLayers.getOrNull(item.index)?.id
                    pointerY = position.y
                    target = item.index
                }
            },
            onDrag = { change, _ ->
                if (draggedId != null) {
                    change.consume()
                    pointerY = change.position.y
                    updateTarget()
                }
            },
            onDragEnd = { finish(true) },
            onDragCancel = { finish(false) }
        )
    }) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(layers, key = { _, layer -> layer.id }) { index, layer ->
                Box(Modifier.fillMaxWidth().testTag("layer_row_${layer.id}").alpha(if (draggedId == layer.id) 0.45f else 1f)) {
                    row(layer, draggedId == layer.id)
                    if (draggedId != null && target == index) {
                        val from = layers.indexOfFirst { it.id == draggedId }
                        Box(Modifier.align(if (target > from) Alignment.BottomCenter else Alignment.TopCenter)
                            .fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary)
                            .testTag("layer_drop_indicator"))
                    }
                }
            }
        }
        if (draggedId != null) {
            val layer = layers.find { it.id == draggedId }
            if (layer != null) {
                // No interactive duplicate of the row; feedback follows the finger across recycled rows.
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.align(Alignment.TopStart).offset { IntOffset(8, pointerY.toInt() - 20) }
                        .testTag("layer_drag_feedback")
                ) {
                    androidx.compose.material3.Text(layer.name, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}
