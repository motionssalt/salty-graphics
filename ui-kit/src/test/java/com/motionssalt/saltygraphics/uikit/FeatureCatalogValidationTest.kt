package com.motionssalt.saltygraphics.uikit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.registry.*
import org.junit.Assert.*
import org.junit.Test

class FeatureCatalogValidationTest {
    private fun feature(id: String = "test") = FeatureDefinition(id, "Feature", Icons.Default.Star, ToolPlacement.ALWAYS)
    private fun withControl(control: FeatureControlDef<*>) = feature().copy(pages = listOf(
        FeaturePageDef("main", "Main", listOf(FeatureSectionDef("section", controls = listOf(control))))
    ))
    private fun number(range: ClosedFloatingPointRange<Float> = 0f..100f, step: Float = 1f) = FeatureControlDef(
        "size", "Size", BoundControlType.RULER_SLIDER,
        PropertyBinding<Float>({ d, _ -> d.editorSettings.gridSize.toFloat() }, { d, _, v ->
            d.copy(editorSettings = d.editorSettings.copy(gridSize = v.toInt())) to "Change Grid size"
        }), range = range, step = step, defaultValue = 20f
    )
    private fun rejected(definition: FeatureDefinition) {
        try { FeatureCatalog().register(definition); fail("Invalid definition was accepted") }
        catch (_: IllegalArgumentException) { /* correct: fail before UI can reach an invalid declaration */ }
    }

    @Test fun replacementKeepsDeclarationOrderAndCatalogsAreIndependent() {
        val catalog = FeatureCatalog()
        catalog.register(feature("first")); catalog.register(feature("second"))
        catalog.register(feature("first").copy(title = "Updated"))
        assertEquals(listOf("first", "second"), catalog.queryTools(ProjectDocument(), emptySet()).map { it.id })
        assertEquals("Updated", catalog.getFeature("first")?.title)
        catalog.unregister("first")
        assertEquals(listOf("second"), catalog.getAllFeatures().map { it.id })
        assertTrue(FeatureCatalog().getAllFeatures().isEmpty())
    }

    @Test fun malformedMetadataRangesStepsAndDefaultsAreRejectedBeforeRendering() {
        rejected(feature("")); rejected(feature().copy(title = ""))
        rejected(feature().copy(defaultPageId = "missing"))
        rejected(feature().copy(pages = listOf(FeaturePageDef("", "Main", emptyList()))))
        rejected(withControl(number(100f..0f)))
        rejected(withControl(number(Float.NaN..100f)))
        rejected(withControl(number(step = 0f)))
        rejected(withControl(number(step = Float.POSITIVE_INFINITY)))
        rejected(withControl(FeatureControlDef("wrong", "Wrong", BoundControlType.SWITCH,
            PropertyBinding<String>({ _, _ -> "not boolean" }, { d, _, _ -> d to "Invalid" }), defaultValue = "not boolean")))
        val repeated = withControl(number())
        val page = repeated.pages.single()
        rejected(repeated.copy(pages = listOf(page, page)))
        val section = page.sections.single()
        rejected(repeated.copy(pages = listOf(page.copy(sections = listOf(section.copy(controls = listOf(number(), number())))))))
        FeatureCatalog().register(withControl(number()))
    }
}
