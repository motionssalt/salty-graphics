package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.ui.editor.ShapeSettingsFeature as Shape
import com.motionssalt.saltygraphics.uikit.registry.change
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.random.Random

class ShapeSettingsBindingTest {
    private val target = Layer(id = "target", type = LayerType.SHAPE,
        shapeData = ShapeData(type = ShapeType.STAR),
        fillData = FillData(solidColor = 0x804477DDL, solidOpacity = .35f),
        strokeData = StrokeData(isEnabled = true, width = 9f), opacity = .6f)
    private val original = ProjectDocument(layers = listOf(target))

    @Test fun staleWidthEventPreservesNewHeightPaintTransformAndOtherLayer() {
        val width = Shape.width.change(original, target.id, 410.25f)
        val changed = original.copy(layers = listOf(Layer(id = "other")) + original.layers)
            .updateLayer(target.id) { it.copy(
                shapeData = it.shapeData!!.copy(height = 730f),
                fillData = FillData(solidColor = 0x40112233L, solidOpacity = .2f),
                transform = Transform2D(rotation = 37f)) }
        val result = width.apply(changed)
        assertEquals(changed.findLayerById(target.id)!!.copy(
            shapeData = changed.findLayerById(target.id)!!.shapeData!!.copy(width = 410.25f)),
            result.findLayerById(target.id))
        assertEquals(changed.layers.first(), result.layers.first())
    }

    @Test fun nestedTargetIsStableAndMissingOrWrongProjectIsNoOp() {
        val nested = original.copy(layers = listOf(Layer(id = "group", type = LayerType.GROUP,
            children = listOf(target, target.copy(id = "sibling")))))
        val event = Shape.width.change(nested, target.id, 520f)
        val changed = event.apply(nested)
        assertEquals(520f, changed.findLayerById(target.id)!!.shapeData!!.width, 0f)
        assertEquals(target.shapeData, changed.findLayerById("sibling")!!.shapeData)
        val removed = nested.removeLayer(target.id)
        assertFalse(event.appliesTo(removed)); assertSame(removed, event.apply(removed))
        val otherProject = nested.copy(id = "other-project")
        assertFalse(event.appliesTo(otherProject)); assertSame(otherProject, event.apply(otherProject))
        val media = nested.updateLayer(target.id) { it.copy(type = LayerType.MEDIA) }
        assertFalse(event.appliesTo(media)); assertSame(media, event.apply(media))
    }

    @Test fun shapeSpecificEventsCannotApplyAfterTypeChanges() {
        val event = Shape.starPoints.change(original, target.id, 9f)
        val rectangle = original.updateLayer(target.id) { it.copy(shapeData = it.shapeData!!.copy(type = ShapeType.RECTANGLE)) }
        assertFalse(event.appliesTo(rectangle)); assertSame(rectangle, event.apply(rectangle))
        assertFalse(Shape.cornerRadius.isVisible!!(original, target.id))
        assertTrue(Shape.starPoints.isVisible!!(original, target.id))
        assertTrue(Shape.innerRadius.isVisible!!(original, target.id))
    }

    @Test fun everyGeometryControlHasTypedBindingValidDefaultsAndSafeRanges() {
        val controls = Shape.catalog.getFeature("shape_settings")!!.pages.single().sections.single().controls
        assertEquals(6, controls.size)
        assertTrue(controls.all { it.binding.path != null && it.customRenderer == null })
        for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f, 50001f)) {
            try { Shape.width.change(original, target.id, bad); fail("Rejected width required: $bad") }
            catch (_: IllegalArgumentException) { }
        }
        val polygon = original.updateLayer(target.id) { it.copy(shapeData = ShapeData(type = ShapeType.POLYGON)) }
        assertEquals(3, Shape.sides.change(polygon, target.id, 3f).apply(polygon)
            .findLayerById(target.id)!!.shapeData!!.sides)
    }

    @Test fun randomStaleEventsKeepEachLatestPropertyAndUndoRedoAndArchive() {
        val random = Random(442)
        val tree = ActionTree(original)
        var width = target.shapeData!!.width
        var height = target.shapeData!!.height
        repeat(120) {
            val value = random.nextInt(1, 1000).toFloat() + .25f
            val event = if (it % 2 == 0) Shape.width.change(original, target.id, value).also { width = value }
                else Shape.height.change(original, target.id, value).also { height = value }
            tree.recordCommand(event.identity(tree.currentDocument), event::apply)
            val layer = tree.currentDocument.findLayerById(target.id)!!
            assertEquals(width, layer.shapeData!!.width, 0f)
            assertEquals(height, layer.shapeData!!.height, 0f)
            assertEquals(target.fillData, layer.fillData)
            assertEquals(target.strokeData, layer.strokeData)
            assertEquals(target.opacity, layer.opacity, 0f)
        }
        val final = tree.currentDocument
        assertTrue(tree.undoActionName!!.contains("height"))
        assertTrue(tree.undo()); assertTrue(tree.redo()); assertEquals(final, tree.currentDocument)
        val bytes = ByteArrayOutputStream().also { SgProjectArchive.exportToStream(final, it) }.toByteArray()
        val loaded = SgProjectArchive.importFromStream(ByteArrayInputStream(bytes)).first
        assertEquals(final, loaded)
    }
}
