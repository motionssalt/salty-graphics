package com.motionssalt.saltygraphics.uikit.registry

/** Stable ids for registered custom domain surfaces. No feature may inject arbitrary UI into the app shell. */
object UiComponentRegistry {
    val ids = setOf(
        "screen.home","screen.settings","screen.new_project","screen.editor","screen.export",
        "editor.media","editor.text","editor.adjustment","editor.shape","editor.vector","editor.edit",
        "editor.transform","editor.fill","editor.border_shadow","effects.panel","editor.blending",
        "editor.duplicate","editor.rasterize","editor.delete","editor.text_options","editor.shape_picker",
        "editor.vector_creation","layers.panel","history.tree","view.controls","project.settings",
        "sample.trackpad","sample.gradient","sample.media","sample.layers",
        "canvas.selection","canvas.gradient","canvas.vector","canvas.snapping","canvas.grid",
        "history.operation","text.font_picker","vector.precision","gradient.editor","value.entry"
    )
    fun require(id: String): String { require(id in ids) { "Unregistered UI component: $id" }; return id }
}
