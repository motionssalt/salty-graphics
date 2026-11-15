package com.motionssalt.saltygraphics.uikit.registry

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.GradientData
import com.motionssalt.saltygraphics.engine.model.FillData
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.model.SelectionBounds
import com.motionssalt.saltygraphics.engine.model.SmartGuideLine
import com.motionssalt.saltygraphics.engine.model.Transform2D
import com.motionssalt.saltygraphics.uikit.overlays.SaltyVectorToolMode

/** Required callbacks for a real, history-aware gradient editing session. No no-op defaults. */
data class GradientOverlayBindings(
    val onPreview: (FillData) -> Unit,
    val onCommit: (ActionIdentity, FillData) -> Unit,
    val onCancel: () -> Unit
)

/** Vector session values and real editor callbacks, never silent no-op bindings. */
data class VectorOverlayBindings(
    val pathIndex: Int,
    val selectedPoint: Int?,
    val tool: SaltyVectorToolMode,
    val onSelectPoint: (Int?) -> Unit,
    val onPreview: (Layer?) -> Unit,
    val onCommit: (Layer) -> Unit,
    val onDone: () -> Unit
)

/** Selection overlay session values and real transform callbacks, never placeholder bounds or no-op defaults. */
data class SelectionOverlayBindings(
    val bounds: SelectionBounds,
    val isTextEditing: Boolean = false,
    val isShapeCornerHandleEnabled: Boolean = false,
    val shapeCornerRadius: Float = 0f,
    val isShapeStarHandleEnabled: Boolean = false,
    val starInnerRadiusRatio: Float = 0.4f,
    val smartGuides: List<SmartGuideLine> = emptyList(),
    val rotatingAngleLabel: String? = null,
    val rotatingPillScreenPosition: Offset? = null,
    val handleGesturesInternally: Boolean = false,
    val onPreviewTransform: (Transform2D) -> Unit = {},
    val onCommitTransform: (ActionIdentity, Transform2D) -> Unit = { _, _ -> },
    val onCancelTransform: () -> Unit = {}
)

/** Production gradient session: selection and every edit callback come from its real host. */
data class GradientBarBindings(
    val gradient: GradientData,
    val selectedStopIndex: Int,
    val onSelectStop: (Int) -> Unit,
    val onPreview: (GradientData) -> Unit,
    val onCommit: (String, GradientData) -> Unit
)

/** Production trackpad session preserves host snapping, exact entry, sensitivity and history. */
data class PositionTrackpadBindings(
    val x: Float,
    val y: Float,
    val sensitivity: Float,
    val onDelta: (Float, Float) -> Unit,
    val onDragEnd: () -> Unit,
    val onCommitX: (Float) -> Unit,
    val onCommitY: (Float) -> Unit,
    val range: ClosedFloatingPointRange<Float> = -100000f..100000f
)

/** Production adapter state for the registered media_picker_grid library component. */
data class SaltyMediaPickerState(
    val media: List<Pair<String, Bitmap?>>,
    val selectedPath: String? = null,
    val onImportRequest: () -> Unit
)

/** Production adapter state for the registered history_tree library component. */
data class SaltyHistoryTreeAdapterState(
    val actionTree: ActionTree,
    val onJumpToNode: (String) -> Unit,
    val onDismiss: () -> Unit
)

/** Context passed to library-owned registered overlays, not feature modal windows. */
data class OverlayRenderContext(
    val document: ProjectDocument,
    val layer: Layer,
    val canvasSize: CanvasSize,
    val zoom: Float,
    val pan: Offset,
    val gradientEditing: GradientOverlayBindings? = null,
    val vectorEditing: VectorOverlayBindings? = null,
    val selectionEditing: SelectionOverlayBindings? = null
)

data class LibraryComponentEntry<T>(
    val id: String,
    val description: String = "",
    val renderer: @Composable (
        label: String,
        current: T,
        onPreview: (T) -> Unit,
        onCommit: (T) -> Unit
    ) -> Unit
)

data class OverlayEntry(
    val id: String,
    val description: String = "",
    val renderer: @Composable (OverlayRenderContext) -> Unit
)

/**
 * Registry of reusable library-owned control renderers.
 * Features refer to registered components by componentId via BoundControlType.LIBRARY_COMPONENT
 * rather than embedding ad-hoc layouts in feature modules.
 */
object LibraryComponentRegistry {
    private val components = mutableStateMapOf<String, LibraryComponentEntry<Any?>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> register(entry: LibraryComponentEntry<T>) {
        require(entry.id.isNotBlank()) { "Library component id is required" }
        components[entry.id] = entry as LibraryComponentEntry<Any?>
    }

    fun unregister(id: String) {
        components.remove(id)
    }

    fun contains(id: String): Boolean = components.containsKey(id)

    fun get(id: String): LibraryComponentEntry<Any?>? = components[id]

    fun resolveRequired(id: String): LibraryComponentEntry<Any?> =
        requireNotNull(get(id)) { "Unregistered library component: $id" }

    fun registeredIds(): Set<String> = components.keys.toSet()
}

/**
 * Registry of library-owned on-canvas interactive overlays.
 */
object OverlayRegistry {
    private val overlays = mutableStateMapOf<String, OverlayEntry>()

    fun register(entry: OverlayEntry) {
        require(entry.id.isNotBlank()) { "Overlay id is required" }
        overlays[entry.id] = entry
    }

    fun unregister(id: String) {
        overlays.remove(id)
    }

    fun contains(id: String): Boolean = overlays.containsKey(id)

    fun get(id: String): OverlayEntry? = overlays[id]

    fun allOverlays(): List<OverlayEntry> = overlays.values.toList()
}

/**
 * Library-owned overlay host. Feature facades pass data and bindings, never a renderer.
 * Keying by document/target cancels an active gesture before another target is mounted.
 */
@Composable
fun ScaffoldCanvasOverlay(id: String, context: OverlayRenderContext) {
    SaltyBuiltInLibraryRegistry.ensureRegistered()
    val entry = requireNotNull(OverlayRegistry.get(id)) { "Unregistered canvas overlay: $id" }
    key(id, context.document.id, context.layer.id) {
        Box(Modifier.fillMaxSize().testTag("registered_overlay_$id")) {
            entry.renderer(context)
        }
    }
}

/**
 * Shared host for a registered library surface. Production adapters and declarative
 * property pages use the same resolver; a missing registration must not silently
 * remove an editing control. The library renderer, never the feature, owns layout.
 */
@Suppress("UNCHECKED_CAST")
@Composable
fun <T> ScaffoldLibraryComponent(
    id: String,
    label: String,
    current: T,
    onPreview: (T) -> Unit,
    onCommit: (T) -> Unit
) {
    SaltyBuiltInLibraryRegistry.ensureRegistered()
    val entry = LibraryComponentRegistry.resolveRequired(id)
    key(id) {
        Box(Modifier.testTag("registered_component_$id")) {
            entry.renderer(label, current, { onPreview(it as T) }, { onCommit(it as T) })
        }
    }
}
