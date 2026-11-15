package com.motionssalt.saltygraphics.uikit.registry

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.engine.model.FillData
import com.motionssalt.saltygraphics.engine.model.GradientData
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.uikit.controls.DropdownOption
import com.motionssalt.saltygraphics.uikit.controls.SaltyGradientStopEditor
import com.motionssalt.saltygraphics.uikit.controls.SaltyInlineDropdown
import com.motionssalt.saltygraphics.uikit.controls.SaltyMediaPickerGrid
import com.motionssalt.saltygraphics.uikit.controls.SaltyPositionTrackpad
import com.motionssalt.saltygraphics.uikit.controls.SaltySegmentedChips
import com.motionssalt.saltygraphics.uikit.overlays.SaltyGradientLayerOverlay
import com.motionssalt.saltygraphics.uikit.overlays.SaltySelectionOverlay
import com.motionssalt.saltygraphics.uikit.overlays.SaltyVectorCanvasOverlay
import com.motionssalt.saltygraphics.uikit.overlays.SaltyVectorToolMode
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
                    SaltyPositionTrackpad(
                        x = current.first,
                        y = current.second,
                        sensitivity = 1f,
                        onDelta = { dx, dy -> onPreview((current.first + dx) to (current.second + dy)) },
                        onDragEnd = { onCommit(current) },
                        onCommitX = { nx -> onCommit(nx to current.second) },
                        onCommitY = { ny -> onCommit(current.first to ny) }
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
                    SaltyGradientStopEditor(
                        gradient = current,
                        selectedStopIndex = 0,
                        onSelectStop = {},
                        onGradientPreview = onPreview,
                        onGradientCommit = { _, updated -> onCommit(updated) }
                    )
                }
            )
        }
        if (!LibraryComponentRegistry.contains("history_tree")) {
            LibraryComponentRegistry.register(
                LibraryComponentEntry<String>(
                    id = "history_tree",
                    description = "History node summary badge"
                ) { label, current, _, _ ->
                    SaltyText(
                        text = "$label: $current",
                        style = SaltyTextStyle.BODY_SMALL,
                        tone = SaltyTextTone.SECONDARY
                    )
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
                LibraryComponentEntry<List<Pair<String, Bitmap?>>>(
                    id = "media_picker_grid",
                    description = "In-panel project media asset picker grid"
                ) { _, current, _, onCommit ->
                    SaltyMediaPickerGrid(
                        media = current,
                        onSelectMedia = { _ -> onCommit(current) },
                        onImport = {}
                    )
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
                    SaltyGradientLayerOverlay(
                        layer = ctx.layer,
                        canvasSize = ctx.canvasSize,
                        zoom = ctx.zoom,
                        pan = ctx.pan,
                        onPreview = { _: FillData -> },
                        onCommit = { _: FillData -> }
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
                    SaltyVectorCanvasOverlay(
                        layer = ctx.layer,
                        canvasSize = ctx.canvasSize,
                        pathIndex = 0,
                        selectedPoint = null,
                        tool = SaltyVectorToolMode.SELECT,
                        zoom = ctx.zoom,
                        pan = ctx.pan,
                        onSelectPoint = {},
                        onPreview = {},
                        onCommit = {},
                        onDone = {}
                    )
                }
            )
        }
        if (!OverlayRegistry.contains("selection_box")) {
            OverlayRegistry.register(
                OverlayEntry(
                    id = "selection_box",
                    description = "Center-anchored selection box and handles"
                ) { ctx ->
                    SaltySelectionOverlay(
                        bounds = androidx.compose.ui.geometry.Rect(
                            0f, 0f, ctx.canvasSize.width.toFloat(), ctx.canvasSize.height.toFloat()
                        ),
                        rotationDeg = ctx.layer.transform.rotationDegrees,
                        onScaleAroundCenter = { _, _ -> },
                        onRotate = {},
                        onTranslate = { _, _ -> },
                        onTransformCommit = {},
                        isLocked = ctx.layer.isLocked
                    )
                }
            )
        }
    }
}
