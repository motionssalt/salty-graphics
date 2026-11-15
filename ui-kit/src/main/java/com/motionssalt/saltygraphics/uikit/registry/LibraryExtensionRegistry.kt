package com.motionssalt.saltygraphics.uikit.registry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.model.FillData
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
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

/** Context passed to library-owned registered overlays, not feature modal windows. */
data class OverlayRenderContext(
    val document: ProjectDocument,
    val layer: Layer,
    val canvasSize: CanvasSize,
    val zoom: Float,
    val pan: Offset,
    val gradientEditing: GradientOverlayBindings? = null,
    val vectorEditing: VectorOverlayBindings? = null
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
