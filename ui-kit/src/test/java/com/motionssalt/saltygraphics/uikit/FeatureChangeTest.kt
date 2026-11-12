package com.motionssalt.saltygraphics.uikit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.uikit.registry.*
import org.junit.Assert.*
import org.junit.Test

class FeatureChangeTest {
    private val size = FeatureControlDef("size", "Size", BoundControlType.RULER_SLIDER,
        PropertyBinding(DocumentPropertyPath("editorSettings.gridSize", "Grid", "size",
            { d, _ -> d.editorSettings.gridSize.toFloat() },
            { d, _, v -> d.copy(editorSettings = d.editorSettings.copy(gridSize = v.toInt())) })),
        range = 1f..1024f, defaultValue = 20f)
    private val color = FeatureControlDef("color", "Color", BoundControlType.COLOR_PICKER,
        PropertyBinding(DocumentPropertyPath("editorSettings.gridColor", "Grid", "color",
            { d, _ -> d.editorSettings.gridColor },
            { d, _, v -> d.copy(editorSettings = d.editorSettings.copy(gridColor = v)) })), defaultValue = 0x66808080L)

    @Test fun staleEventsRebaseOnCurrentDocumentAndKeepSpecificHistory() {
        val old = ProjectDocument()
        val sizeEvent = size.change(old, null, 72f) // event created BEFORE the color commit
        val tree = ActionTree(old)
        val colorEvent = color.change(old, null, 0xFF000000L)
        tree.recordCommand(colorEvent.identity(tree.currentDocument), colorEvent::apply)
        tree.recordCommand(sizeEvent.identity(tree.currentDocument), sizeEvent::apply)
        assertEquals(0xFF000000L, tree.currentDocument.editorSettings.gridColor)
        assertEquals(72, tree.currentDocument.editorSettings.gridSize)
        assertEquals("Increase Grid size", tree.undoActionName)
        assertEquals(3, tree.nodes.size)
        tree.undo()
        assertEquals(0xFF000000L, tree.currentDocument.editorSettings.gridColor)
        val branchEvent = size.change(tree.currentDocument, null, 40f)
        tree.recordCommand(branchEvent.identity(tree.currentDocument), branchEvent::apply)
        assertEquals(2, tree.nodes.values.first { it.description == "Change Grid color" }.childrenIds.size)
        tree.undo(); tree.redo()
        assertEquals(40, tree.currentDocument.editorSettings.gridSize)
        assertEquals(0xFF000000L, tree.currentDocument.editorSettings.gridColor)
    }

    @Test fun manyPreviewFramesCommitExactlyOnceAndCancellationDoesNotRecord() {
        val initial = ProjectDocument()
        val tree = ActionTree(initial)
        var preview = initial
        repeat(60) { preview = size.change(initial, null, 21f + it).apply(tree.currentDocument) }
        assertEquals(1, tree.nodes.size)
        assertEquals(80, preview.editorSettings.gridSize)
        val last = size.change(initial, null, 80f)
        tree.recordCommand(last.identity(tree.currentDocument), last::apply)
        assertEquals(2, tree.nodes.size)
        assertTrue(tree.undo()); assertEquals(initial, tree.currentDocument)
    }

    @Test fun eventsCannotLeakBetweenProjectsOrResurrectDeletedTargets() {
        val original = ProjectDocument(layers = listOf(Layer(id = "target", type = LayerType.SHAPE)))
        val other = original.copy(id = "another-project")
        val event = size.change(original, null, 88f)
        assertFalse(event.appliesTo(other)); assertEquals(other, event.apply(other))
        val opacity = FeatureControlDef("opacity", "Opacity", BoundControlType.PROGRESS_SLIDER,
            PropertyBinding(DocumentPropertyPath("layer.opacity", "Layer", "opacity",
                { doc, id -> doc.findLayerById(id.orEmpty())?.opacity ?: 1f },
                { doc, id, value -> doc.updateLayer(id.orEmpty()) { it.copy(opacity = value) } },
                exists = { doc, id -> doc.findLayerById(id.orEmpty()) != null })), range = 0f..1f, defaultValue = 1f)
        val captured = opacity.change(original, "target", .5f)
        val deleted = original.copy(layers = emptyList())
        assertFalse(captured.appliesTo(deleted)); assertEquals(deleted, captured.apply(deleted))
    }

    @Test fun strictCatalogRejectsUnboundDeclarationsAndValuesOutsideContracts() {
        fun feature(control: FeatureControlDef<*>) = FeatureDefinition("test", "Test", Icons.Default.Star,
            ToolPlacement.ALWAYS, pages = listOf(FeaturePageDef("main", "Main", listOf(FeatureSectionDef("section", controls = listOf(control))))))
        FeatureCatalog(requireTypedBindings = true).register(feature(size))
        try {
            FeatureCatalog(requireTypedBindings = true).register(feature(size.copy(binding = PropertyBinding(
                { d, _ -> d.editorSettings.gridSize.toFloat() }, { d, _, _ -> d to "Unbound" }))))
            fail("Untyped production binding accepted")
        } catch (_: IllegalArgumentException) { }
        for (invalid in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 1025f)) {
            try { size.change(ProjectDocument(), null, invalid); fail("Invalid value accepted: $invalid") }
            catch (_: IllegalArgumentException) { }
        }
        val identity = size.change(ProjectDocument(), null, 12f).identity(ProjectDocument())
        assertEquals("Decrease Grid size", identity.toLabel())
    }
}
