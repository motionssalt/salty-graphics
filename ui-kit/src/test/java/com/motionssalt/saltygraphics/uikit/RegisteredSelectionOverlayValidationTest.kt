package com.motionssalt.saltygraphics.uikit

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.model.ShapeData
import com.motionssalt.saltygraphics.uikit.registry.OverlayRegistry
import com.motionssalt.saltygraphics.uikit.registry.OverlayRenderContext
import com.motionssalt.saltygraphics.uikit.registry.SaltyBuiltInLibraryRegistry
import com.motionssalt.saltygraphics.uikit.tokens.SaltyGraphicsTheme
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h412dp-mdpi")
class RegisteredSelectionOverlayValidationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun missingSelectionBindingsFailsLoudly() {
        SaltyBuiltInLibraryRegistry.ensureRegistered()
        val entry = OverlayRegistry.get("selection_box")!!
        val doc = ProjectDocument(canvasSize = CanvasSize(400, 400))
        val layer = Layer(name = "Box", type = LayerType.SHAPE, shapeData = ShapeData(width = 200f, height = 100f))
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var thrown: Throwable? = null
        try {
            host.get().setContent {
                SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
                    entry.renderer(OverlayRenderContext(doc, layer, doc.canvasSize, 1f, Offset.Zero))
                }
            }
            compose.waitForIdle()
        } catch (t: Throwable) {
            thrown = t
        }
        try {
            assertNotNull("selection_box must reject missing SelectionOverlayBindings", thrown)
        } finally {
            host.pause().stop().destroy()
        }
    }
}
