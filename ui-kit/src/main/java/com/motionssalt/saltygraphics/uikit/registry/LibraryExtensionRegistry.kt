package com.motionssalt.saltygraphics.uikit.registry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.ProjectDocument

/**
 * Context passed to registered on-canvas overlays.
 * Overlays register in :ui-kit and never live inside feature-specific modal windows.
 */
data class OverlayRenderContext(
    val document: ProjectDocument,
    val layer: Layer,
    val canvasSize: CanvasSize,
    val zoom: Float,
    val pan: Offset
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
