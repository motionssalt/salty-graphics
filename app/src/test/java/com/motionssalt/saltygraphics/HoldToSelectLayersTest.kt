package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.ui.editor.LayersPanel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HoldToSelectLayersTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun layersPanelHasNoCheckboxesAndSupportsHoldToSelectAndMultiSelection() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            val layer1 = Layer(id = "layer_a", name = "Layer Alpha", type = LayerType.SHAPE)
            val layer2 = Layer(id = "layer_b", name = "Layer Beta", type = LayerType.TEXT)
            val layer3 = Layer(id = "layer_c", name = "Layer Gamma", type = LayerType.VECTOR)
            val layers = listOf(layer1, layer2, layer3)

            var selectedIds by mutableStateOf(setOf("layer_a"))

            host.get().setContent {
                MaterialTheme {
                    LayersPanel(
                        layers = layers,
                        selectedLayerIds = selectedIds,
                        onSelectLayer = { id -> selectedIds = if (id != null) setOf(id) else emptySet() },
                        onToggleSelectMulti = { id ->
                            selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                        },
                        onSelectAll = { selectedIds = layers.map { it.id }.toSet() },
                        onSelectNone = { selectedIds = emptySet() },
                        onToggleVisibility = {},
                        onToggleLock = {},
                        onDeleteLayers = {},
                        onRenameLayer = { _, _ -> },
                        onCopyLayer = {},
                        onPasteProperties = {},
                        onMoveLayer = { _, _ -> },
                        onEnterGroup = {},
                        onClosePanel = {}
                    )
                }
            }

            compose.waitForIdle()

            // Verify no Checkbox components exist in the layer list (V26 rule)
            compose.onAllNodes(isToggleable()).assertCountEquals(0)

            // Verify layer names exist
            compose.onNodeWithText("Layer Alpha").assertIsDisplayed()
            compose.onNodeWithText("Layer Beta").assertIsDisplayed()
            compose.onNodeWithText("Layer Gamma").assertIsDisplayed()

            // Multi-select toggle via long-press on Layer Beta
            compose.onNodeWithText("Layer Beta").performTouchInput {
                longClick()
            }
            compose.waitForIdle()

            // Both Layer Alpha and Layer Beta should now be selected
            assertTrue("Layer Alpha should remain selected", selectedIds.contains("layer_a"))
            assertTrue("Layer Beta should be added by longClick", selectedIds.contains("layer_b"))
            assertEquals(2, selectedIds.size)

            // In multi-select mode, tapping Layer Gamma adds it to selection
            compose.onNodeWithText("Layer Gamma").performClick()
            compose.waitForIdle()
            assertEquals(3, selectedIds.size)
            assertTrue(selectedIds.contains("layer_c"))

            // Tapping Layer Alpha removes it from selection
            compose.onNodeWithText("Layer Alpha").performClick()
            compose.waitForIdle()
            assertEquals(2, selectedIds.size)
            assertFalse(selectedIds.contains("layer_a"))
        } finally {
            host.pause().stop().destroy()
        }
    }
}
