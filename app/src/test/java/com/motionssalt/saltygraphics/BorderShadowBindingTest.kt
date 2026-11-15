package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.ui.controls.BorderShadowFeature as Appearance
import com.motionssalt.saltygraphics.uikit.registry.change
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.random.Random

class BorderShadowBindingTest {
    private val target = Layer(id = "target", type = LayerType.SHAPE,
        shapeData = ShapeData(width = 100f, height = 80f),
        fillData = FillData(solidColor = 0x804477DDL, solidOpacity = .35f),
        strokeData = StrokeData(isEnabled = true, width = 9f, color = 0xC0123456L, opacity = .4f),
        shadowData = ShadowData(isEnabled = true, opacity = .3f), opacity = .6f)
    private val original = ProjectDocument(layers = listOf(target))

    @Test fun staleWidthChangesOnlyWidthOnLiveLayerAndKeepsColorOpacityGeometryAndSiblings() {
        val width = Appearance.strokeWidth.change(original, target.id, 15.25f)
        val changed = original.copy(layers = listOf(Layer(id = "sibling")) + original.layers)
            .updateLayer(target.id) { it.copy(
                shapeData = it.shapeData!!.copy(height = 730f),
                strokeData = it.effectiveStroke.copy(color = 0x80112233L, opacity = .2f, isDashed = true),
                transform = Transform2D(rotationDegrees = 37f),
                shadowData = it.effectiveShadow.copy(blurRadius = 37f)) }
        val result = width.apply(changed)
        val actual = result.findLayerById(target.id)!!
        val before = changed.findLayerById(target.id)!!
        assertEquals(before.effectiveStroke.copy(width = 15.25f), actual.effectiveStroke)
        assertEquals(before.shapeData!!.copy(hasStroke = true, strokeColor = before.effectiveStroke.color,
            strokeWidth = 15.25f), actual.shapeData)
        assertEquals(before.fillData, actual.fillData)
        assertEquals(before.shadowData, actual.shadowData)
        assertEquals(before.transform, actual.transform)
        assertEquals(before.opacity, actual.opacity, 0f)
        assertEquals(changed.layers.first(), result.layers.first())
    }

    @Test fun colorAndOpacityRemainSeparateForStrokeAndShadow() {
        val stroke = Appearance.strokeColor.change(original, target.id, 0x80445566L).apply(original)
        assertEquals(.4f, stroke.findLayerById(target.id)!!.effectiveStroke.opacity, 0f)
        assertEquals(0x80445566L, stroke.findLayerById(target.id)!!.effectiveStroke.color)
        val shadow = Appearance.shadowColor.change(stroke, target.id, 0x40112233L).apply(stroke)
        assertEquals(.3f, shadow.findLayerById(target.id)!!.effectiveShadow.opacity, 0f)
        assertEquals(0x40112233L, shadow.findLayerById(target.id)!!.effectiveShadow.color)
        val opacity = Appearance.strokeOpacity.change(shadow, target.id, 25f).apply(shadow)
        assertEquals(0x80445566L, opacity.findLayerById(target.id)!!.effectiveStroke.color)
        assertEquals(.25f, opacity.findLayerById(target.id)!!.effectiveStroke.opacity, 0f)
        assertEquals(target.fillData, opacity.findLayerById(target.id)!!.fillData)
    }

    @Test fun missingTargetWrongDocumentAndAdjustmentAreNoOpsAndNestedIdsStayStable() {
        val nested = original.copy(layers = listOf(Layer(id = "group", type = LayerType.GROUP,
            children = listOf(target, target.copy(id = "sibling")))))
        val event = Appearance.distance.change(nested, target.id, 37f)
        val changed = event.apply(nested)
        assertEquals(37f, changed.findLayerById(target.id)!!.effectiveShadow.distance, 0f)
        assertEquals(target.effectiveShadow, changed.findLayerById("sibling")!!.effectiveShadow)
        for (doc in listOf(nested.removeLayer(target.id), nested.copy(id = "other-project"),
            nested.updateLayer(target.id) { it.copy(type = LayerType.ADJUSTMENT) })) {
            assertFalse(event.appliesTo(doc)); assertSame(doc, event.apply(doc))
        }
    }

    @Test fun declarationsAreTypedCompleteAndUseSharedVisibilityAndValidation() {
        val controls = Appearance.catalog.getFeature("border_shadow_settings")!!.pages.single().sections.flatMap { it.controls }
        assertEquals(18, controls.size)
        assertTrue(controls.all { it.binding.path != null && it.customRenderer == null })
        val disabled = original.updateLayer(target.id) { it.copy(strokeData = StrokeData(), shadowData = ShadowData()) }
        assertFalse(Appearance.strokeWidth.isVisible!!(disabled, target.id))
        assertFalse(Appearance.blur.isVisible!!(disabled, target.id))
        assertFalse(Appearance.dashLength.isVisible!!(original, target.id))
        val dashed = Appearance.strokeDashed.change(original, target.id, true).apply(original)
        assertTrue(Appearance.dashLength.isVisible!!(dashed, target.id))
        for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 101f)) {
            try { Appearance.strokeWidth.change(original, target.id, bad); fail("Must reject $bad") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun legacyShapeTextVectorStrokeMirrorsStaySynchronizedWithoutTouchingFill() {
        val layers = listOf(
            target,
            Layer(id = "text", type = LayerType.TEXT, textData = TextLayerData(text = "Keep\ntext", color = 0x80123456L)),
            Layer(id = "vector", type = LayerType.VECTOR, vectorPathData = VectorPathData(hasFill = true, fillColor = 0x40112233L)),
            Layer(id = "media", type = LayerType.MEDIA, mediaAssetPath = "assets/keep.png"),
            Layer(id = "group", type = LayerType.GROUP, children = listOf(target.copy(id = "child")))
        )
        for (layer in layers) {
            var doc = original.copy(layers = listOf(layer))
            val fill = layer.effectiveFill
            doc = Appearance.strokeEnabled.change(doc, layer.id, true).apply(doc)
            doc = Appearance.strokeWidth.change(doc, layer.id, 23.5f).apply(doc)
            doc = Appearance.strokeColor.change(doc, layer.id, 0xCC112233L).apply(doc)
            val result = doc.findLayerById(layer.id)!!
            assertEquals(fill, result.effectiveFill)
            assertEquals(layer.children, result.children)
            when (layer.type) {
                LayerType.SHAPE -> { assertTrue(result.shapeData!!.hasStroke); assertEquals(23.5f, result.shapeData!!.strokeWidth, 0f) }
                LayerType.TEXT -> { assertTrue(result.textData!!.hasOutline); assertEquals("Keep\ntext", result.textData!!.text); assertEquals(23.5f, result.textData!!.outlineWidth, 0f) }
                LayerType.VECTOR -> { assertTrue(result.vectorPathData!!.hasStroke); assertEquals(23.5f, result.vectorPathData!!.strokeWidth, 0f) }
                else -> Unit
            }
        }
    }

    @Test fun randomizedStalePropertyEventsSupportExactHistoryBranchingAndArchive() {
        val random = Random(1504)
        val tree = ActionTree(original)
        var expectedWidth = target.effectiveStroke.width
        var expectedBlur = target.effectiveShadow.blurRadius
        repeat(80) { index ->
            val value = random.nextInt(1, 90).toFloat() + .25f
            val event = if (index % 2 == 0) Appearance.strokeWidth.change(original, target.id, value).also { expectedWidth = value }
                else Appearance.blur.change(original, target.id, value).also { expectedBlur = value }
            tree.recordCommand(event.identity(tree.currentDocument), event::apply)
            val layer = tree.currentDocument.findLayerById(target.id)!!
            assertEquals(expectedWidth, layer.effectiveStroke.width, 0f)
            assertEquals(expectedBlur, layer.effectiveShadow.blurRadius, 0f)
            assertEquals(target.effectiveStroke.color, layer.effectiveStroke.color)
            assertEquals(target.effectiveStroke.opacity, layer.effectiveStroke.opacity, 0f)
            assertEquals(target.fillData, layer.fillData)
        }
        val end = tree.currentDocument
        assertTrue(tree.undoActionName!!.contains("Shadow"))
        assertTrue(tree.undo()); assertTrue(tree.redo()); assertEquals(end, tree.currentDocument)
        val out = ByteArrayOutputStream()
        SgProjectArchive.exportToStream(end, out)
        val restored = SgProjectArchive.importFromStream(ByteArrayInputStream(out.toByteArray())).first
        assertEquals(end.layers, restored.layers)
        assertTrue(tree.undo())
        val branch = Appearance.angle.change(tree.currentDocument, target.id, 270f)
        tree.recordCommand(branch.identity(tree.currentDocument), branch::apply)
        assertEquals(270f, tree.currentDocument.findLayerById(target.id)!!.effectiveShadow.lightAngleDegrees, 0f)
        assertEquals(82, tree.nodes.size)
    }
}
