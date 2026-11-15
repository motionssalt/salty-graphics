package com.motionssalt.saltygraphics.uikit

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.GradientData
import com.motionssalt.saltygraphics.engine.model.GradientStop
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.registry.LibraryComponentRegistry
import com.motionssalt.saltygraphics.uikit.registry.SaltyBuiltInLibraryRegistry
import com.motionssalt.saltygraphics.uikit.registry.SaltyHistoryTreeAdapterState
import com.motionssalt.saltygraphics.uikit.registry.SaltyMediaPickerState
import com.motionssalt.saltygraphics.uikit.tokens.SaltyGraphicsTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h920dp-mdpi")
class BuiltInLibraryComponentsTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun transformTrackpadAdapterCommitsAccumulatedDragDelta() {
        SaltyBuiltInLibraryRegistry.ensureRegistered()
        val entry = LibraryComponentRegistry.get("transform_trackpad")!!
        var committed: Pair<Float, Float>? = null
        var previews = 0
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
                @Suppress("UNCHECKED_CAST")
                (entry.renderer as @androidx.compose.runtime.Composable (
                    String, Pair<Float, Float>, (Pair<Float, Float>) -> Unit, (Pair<Float, Float>) -> Unit
                ) -> Unit)(
                    "Position",
                    10f to 20f,
                    { previews++ },
                    { committed = it }
                )
            }
        }
        try {
            compose.onNodeWithTag("trackpad_surface").performTouchInput {
                down(center)
                moveBy(Offset(40f, 30f))
                up()
            }
            compose.runOnIdle {
                assertTrue("Trackpad drag should emit previews", previews > 0)
                assertNotNull("Trackpad drag end should commit final coordinate", committed)
                assertTrue("Committed X should advance from 10f (got ${committed!!.first})", committed!!.first > 10f)
                assertTrue("Committed Y should advance from 20f (got ${committed!!.second})", committed!!.second > 20f)
            }
        } finally {
            host.pause().stop().destroy()
        }
    }

    @Test
    fun mediaPickerGridAndLayerSelectorAndVectorToolbarAndHistoryTreeAdaptersWork() {
        SaltyBuiltInLibraryRegistry.ensureRegistered()
        var imported = false
        var committedMedia: SaltyMediaPickerState? = null
        var committedLayer: Pair<String, List<Layer>>? = null
        var committedVectorMode: String? = null
        var jumpedNodeId: String? = null

        val bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val layerA = Layer(id = "l-a", name = "Mask Target", type = LayerType.SHAPE)
        val doc = ProjectDocument(name = "Doc", layers = listOf(layerA))
        val tree = ActionTree(doc).apply {
            recordAction("Move Shape", doc.copy(name = "Doc 2"))
        }

        val mediaEntry = LibraryComponentRegistry.get("media_picker_grid")!!
        val layerEntry = LibraryComponentRegistry.get("layer_selector")!!
        val vectorEntry = LibraryComponentRegistry.get("vector_toolbar")!!
        val historyEntry = LibraryComponentRegistry.get("history_tree")!!
        val gradientEntry = LibraryComponentRegistry.get("gradient_bar")!!

        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
                androidx.compose.foundation.layout.Column {
                    mediaEntry.renderer(
                        "Media",
                        SaltyMediaPickerState(
                            media = listOf("asset_one.png" to bmp),
                            selectedPath = null,
                            onImportRequest = { imported = true }
                        ),
                        {},
                        { committedMedia = it as SaltyMediaPickerState }
                    )
                    layerEntry.renderer(
                        "Mask Source",
                        "" to listOf(layerA),
                        {},
                        {
                            @Suppress("UNCHECKED_CAST")
                            committedLayer = it as Pair<String, List<Layer>>
                        }
                    )
                    vectorEntry.renderer(
                        "Vector Mode",
                        "Select",
                        {},
                        { committedVectorMode = it as String }
                    )
                    gradientEntry.renderer(
                        "Gradient",
                        GradientData(stops = listOf(GradientStop(0f, 0xFFFF0000L), GradientStop(1f, 0xFF0000FFL))),
                        {},
                        {}
                    )
                    historyEntry.renderer(
                        "History",
                        SaltyHistoryTreeAdapterState(tree, { jumpedNodeId = it }, {}),
                        {},
                        {}
                    )
                }
            }
        }
        try {
            compose.onNodeWithText("Import from device").performClick()
            compose.onNodeWithText("asset_one.png").performClick()
            compose.onNodeWithText("None").performClick()
            compose.onNodeWithText("Mask Target").performClick()
            compose.onNodeWithText("Add point").performClick()
            compose.onNodeWithText("Action History Tree").assertIsDisplayed()
            compose.runOnIdle {
                assertTrue("Media import callback must fire", imported)
                assertEquals("asset_one.png", committedMedia?.selectedPath)
                assertEquals("l-a", committedLayer?.first)
                assertEquals("Add point", committedVectorMode)
                assertNull(jumpedNodeId)
            }
        } finally {
            host.pause().stop().destroy()
        }
    }
}
