package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.ExperimentalComposeUiApi
import android.view.MotionEvent
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
@OptIn(ExperimentalComposeUiApi::class)
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
    }.motionEventSpy { event ->
        // Compose synthesizes an all-pointers-up event for Android ACTION_CANCEL.
        // Observe the native cancellation before it can be mistaken for a valid drop.
        if (event.actionMasked == MotionEvent.ACTION_CANCEL) finish(false)
    }.pointerInput(Unit) {
        // Observe before the LazyColumn's scroll recognizer. After a real hold wins,
        // consume on Initial so its first movement cannot be stolen as scrolling.
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val item = state.layoutInfo.visibleItemsInfo.find {
                down.position.y >= it.offset && down.position.y < it.offset + it.size
            } ?: return@awaitEachGesture
            val earlyEnd = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val pointer = event.changes.find { it.id == down.id }
                    if (pointer == null || !pointer.pressed || event.changes.count { it.pressed } > 1 ||
                        (pointer.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                }
                true
            }
            if (earlyEnd != null) return@awaitEachGesture
            draggedId = currentLayers.getOrNull(item.index)?.id
            pointerY = down.position.y
            target = item.index
            try {
                while (draggedId != null) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val pointer = event.changes.find { it.id == down.id }
                    event.changes.forEach { it.consume() }
                    if (pointer == null || event.changes.count { it.pressed } > 1) break
                    pointerY = pointer.position.y
                    updateTarget()
                    if (!pointer.pressed) {
                        finish(true)
                        break
                    }
                }
            } finally {
                finish(false)
            }
        }
    }) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().testTag("layer_scroll"),
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
