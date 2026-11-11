package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.reflect.KProperty

/** One fixed-pane page stack replaces the former independent, competing visibility flags. */
enum class EditorPanelPage { LAYERS, VECTOR_MENU, VECTOR_SETTINGS, PROJECT_SETTINGS, TRANSFORM,
    EFFECTS, SHAPE_SETTINGS, TEXT_SETTINGS, BLENDING, FILL, BORDER_SHADOW, SHAPE_PICKER, HISTORY, SCAFFOLD_SAMPLE }

@Stable
class EditorPanelState {
    private val stack = mutableStateListOf<EditorPanelPage>()
    val current: EditorPanelPage? get() = stack.lastOrNull()
    fun open(page: EditorPanelPage) { stack.removeAll { it == page }; stack.add(page) }
    fun back(): Boolean = if (stack.isEmpty()) false else { stack.removeAt(stack.lastIndex); true }
    fun close() = stack.clear()
    inner class PageBinding(private val page: EditorPanelPage) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = current == page
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            if (value) open(page) else if (current == page) close() else stack.removeAll { it == page }
        }
    }
    var isLayersPanelOpen by PageBinding(EditorPanelPage.LAYERS)
    var isVectorMenuOpen by PageBinding(EditorPanelPage.VECTOR_MENU)
    var isVectorSettingsOpen by PageBinding(EditorPanelPage.VECTOR_SETTINGS)
    var isProjectSettingsOpen by PageBinding(EditorPanelPage.PROJECT_SETTINGS)
    var isTransformPanelOpen by PageBinding(EditorPanelPage.TRANSFORM)
    var isEffectsSheetOpen by PageBinding(EditorPanelPage.EFFECTS)
    var isShapeSettingsOpen by PageBinding(EditorPanelPage.SHAPE_SETTINGS)
    var isTextSettingsOpen by PageBinding(EditorPanelPage.TEXT_SETTINGS)
    var isBlendingOpen by PageBinding(EditorPanelPage.BLENDING)
    var isMaskOpen by PageBinding(EditorPanelPage.BLENDING)
    var isFillPanelOpen by PageBinding(EditorPanelPage.FILL)
    var isBorderShadowPanelOpen by PageBinding(EditorPanelPage.BORDER_SHADOW)
    var isShapePickerOpen by PageBinding(EditorPanelPage.SHAPE_PICKER)
    var isActionTreeOpen by PageBinding(EditorPanelPage.HISTORY)
    var isScaffoldSampleOpen by PageBinding(EditorPanelPage.SCAFFOLD_SAMPLE)
}

/** Vector gesture modifiers are shared by the geometry overlay and its panel controls. */
@Stable
class VectorPrecisionState {
    var independentHandles by mutableStateOf(false)
    var angleLock45 by mutableStateOf(false)
}
