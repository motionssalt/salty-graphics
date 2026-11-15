package com.motionssalt.saltygraphics.uikit.registry

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.model.GradientData
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.uikit.controls.DropdownOption
import com.motionssalt.saltygraphics.uikit.controls.SaltyGradientStopEditor
import com.motionssalt.saltygraphics.uikit.controls.SaltyInlineDropdown
import com.motionssalt.saltygraphics.uikit.controls.SaltyMediaPickerGrid
import com.motionssalt.saltygraphics.uikit.controls.SaltyPositionTrackpad
import com.motionssalt.saltygraphics.uikit.controls.SaltySegmentedChips
import com.motionssalt.saltygraphics.uikit.overlays.SaltyGradientLayerOverlay
import com.motionssalt.saltygraphics.uikit.overlays.SaltySelectionBoxOverlay
import com.motionssalt.saltygraphics.uikit.overlays.SaltyVectorCanvasOverlay
import com.motionssalt.saltygraphics.uikit.overlays.SaltyVectorToolMode
import com.motionssalt.saltygraphics.uikit.structure.SaltyHistoryTree
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextStyle
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextTone

/**
 * Built-in library components and on-canvas overlays registered into the shared registries
 * so declarative features and production screens resolve shared components by id.
 */
object SaltyBuiltInLibraryRegistry {
    @Volatile
    private var registered = false

    fun ensureRegistered() {
        if (registered) return
        synchronized(this) {
            if (registered) return
            registerDefaults()
            registered = true
        }
    }

    private fun registerDefaults() {
        if (!LibraryComponentRegistry.contains("transform_trackpad")) {
            LibraryComponentRegistry.register(
                LibraryComponentEntry<Pair<Float, Float>>(
                    id = "transform_trackpad",
                    description = "2D position trackpad with direct coordinate entry"
                ) { _, current, onPreview, onCommit ->
                    var dragging by remember { mutableStateOf(false) }
                    var draft by remember { mutableStateOf(current) }
                    if (!dragging) {
                        draft = current
                    }
                    SaltyPositionTrackpad(
                        x = draft.first,
                        y = draft.second,
                        sensitivity = 1f,
                        onDelta = { dx, dy ->
                            dragging = true
                            val next = (draft.first + dx) to (draft.second + dy)
                            draft = next
                            onPreview(next)
                        },
                        onDragEnd = {
                            dragging = false
                            onCommit(draft)
                        },
                        onCommitX = { nx ->
                            val next = nx to draft.second
                            draft = next
                            onCommit(next)
                        },
                        onCommitY = { ny ->
                            val next = draft.first to ny
                            draft = next
                            onCommit(next)
                        }
                    )
                }
            )
        }

        if (!LibraryComponentRegistry.contains("gradient_bar")) {
            LibraryComponentRegistry.register(
                LibraryComponentEntry<GradientData>(
                    id = "gradient_bar",
                    description = "Interactive gradient stop bar editor"
                ) { _, current, onPreview, onCommit ->
                    var selectedStopIndex by remember { mutableIntStateOf(0) }
                    val safeIndex = selectedStopIndex.coerceIn(0, (current.stops.size - 1).coerceAtLeast(0))
                    SaltyGradientStopEditor(
                        gradient = current,
                        selectedStopIndex = safeIndex,
                        onSelectStop = { selectedStopIndex = it },
                        onGradientPreview = onPreview,
                        onGradientCommit = { _, updated -> onCommit(updated) }
                    )
                }
            )
        }

        if (!LibraryComponentRegistry.contains("history_tree")) {
            LibraryComponentRegistry.register(
                LibraryComponentEntry<Any>(
                    id = "history_tree",
                    description = "Interactive lane-based action history tree"
                ) { label, current, _, _ ->
                    when (current) {
                        is SaltyHistoryTreeAdapterState -> SaltyHistoryTree(
                            actionTree = current.actionTree,
                            onJumpToNode = current.onJumpToNode,
                            onDismiss = current.onDismiss
                        )
                        else -> SaltyText(
                            text = "$label: $current",
                            style = SaltyTextStyle.BODY_SMALL,
                            tone = SaltyTextTone.SECONDARY
                        )
                    }
                }
            )
        }

        if (!LibraryComponentRegistry.contains("vector_toolbar")) {
            LibraryComponentRegistry.register(
                LibraryComponentEntry<String>(
                    id = "vector_toolbar",
                    description = "Vector tool mode selector"
                ) { label, current, _, onCommit ->
                    SaltySegmentedChips(
                        options = SaltyVectorToolMode.entries.map { it.label },
                        selectedOption = current,
                        onOptionSelected = onCommit,
                        label = label
                    )
                }
            )
        }

        if (!LibraryComponentRegistry.contains("media_picker_grid")) {
            LibraryComponentRegistry.register(
                LibraryComponentEntry<Any>(
                    id = "media_picker_grid",
                    description = "In-panel project media asset picker grid"
                ) { _, current, _, onCommit ->
                    when (current) {
                        is SaltyMediaPickerState -> SaltyMediaPickerGrid(
                            media = current.media,
                            onSelectMedia = { path -> onCommit(current.copy(selectedPath = path)) },
                            onImport = current.onImportRequest
                        )
                        is List<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            val items = current as List<Pair<String, Bitmap?>>
                            SaltyMediaPickerGrid(
                                media = items,
                                onSelectMedia = { path ->
                                    val chosen = items.filter { it.first == path }
                                    onCommit(if (chosen.isNotEmpty()) chosen + items.filter { it.first != path } else items)
                                },
                                onImport = {}
                            )
                        }
                    }
                }
            )
        }

        if (!LibraryComponentRegistry.contains("layer_selector")) {
            LibraryComponentRegistry.register(
                LibraryComponentEntry<Pair<String, List<Layer>>>(
                    id = "layer_selector",
                    description = "Inline layer target selector"
                ) { label, current, _, onCommit ->
                    val options = listOf(DropdownOption("", "None")) +
                        current.second.map { DropdownOption(it.id, it.name) }
                    SaltyInlineDropdown(
                        selected = current.first,
                        options = options,
                        onSelect = { nextId -> onCommit(nextId to current.second) },
                        label = label
                    )
                }
            )
        }

        if (!OverlayRegistry.contains("gradient_handles")) {
            OverlayRegistry.register(
                OverlayEntry(
                    id = "gradient_handles",
                    description = "Independent on-canvas gradient start, end, and center handles"
                ) { ctx ->
                    val bindings = requireNotNull(ctx.gradientEditing) {
                        "gradient_handles requires preview, labelled commit and cancellation bindings"
                    }
                    SaltyGradientLayerOverlay(
                        layer = ctx.layer,
                        canvasSize = ctx.canvasSize,
                        zoom = ctx.zoom,
                        pan = ctx.pan,
                        onPreview = bindings.onPreview,
                        onCommit = { fill ->
                            bindings.onCommit(ActionIdentity("Change", "Layer", "gradient geometry"), fill)
                        },
                        onGestureCommit = bindings.onCommit,
                        onCancel = bindings.onCancel
                    )
                }
            )
        }

        if (!OverlayRegistry.contains("vector_editor")) {
            OverlayRegistry.register(
                OverlayEntry(
                    id = "vector_editor",
                    description = "On-canvas vector Bezier point and handle editor"
                ) { ctx ->
                    val bindings = requireNotNull(ctx.vectorEditing) {
                        "vector_editor requires real tool, selection, preview, commit and Done bindings"
                    }
                    SaltyVectorCanvasOverlay(
                        layer = ctx.layer,
                        canvasSize = ctx.canvasSize,
                        pathIndex = bindings.pathIndex,
                        selectedPoint = bindings.selectedPoint,
                        tool = bindings.tool,
                        zoom = ctx.zoom,
                        pan = ctx.pan,
                        onSelectPoint = bindings.onSelectPoint,
                        onPreview = bindings.onPreview,
                        onCommit = bindings.onCommit,
                        onDone = bindings.onDone
                    )
                }
            )
        }

        if (!OverlayRegistry.contains("selection_box")) {
            OverlayRegistry.register(
                OverlayEntry(
                    id = "selection_box",
                    description = "Center-anchored selection box, handles, smart guides, and angle pill"
                ) { ctx ->
                    val bindings = requireNotNull(ctx.selectionEditing) {
                        "selection_box requires real SelectionOverlayBindings"
                    }
                    SaltySelectionBoxOverlay(context = ctx, bindings = bindings)
                }
            )
        }
    }
}
