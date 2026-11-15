package com.motionssalt.saltygraphics.uikit

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.uikit.registry.*
import com.motionssalt.saltygraphics.uikit.tokens.SaltyGraphicsTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Operates the same registered host used by the real Fill page, not an entry in isolation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RegisteredMediaPickerHostTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun realGridUsesCurrentImportAndSelectionBindingsWithoutMutatingAssetOrder() {
        var items by mutableStateOf(listOf("one.png" to null, "two.png" to null))
        var selected by mutableStateOf<String?>(null)
        var replacementBinding by mutableStateOf(false)
        var firstImports = 0
        var latestImports = 0
        var firstCommits = 0
        var latestCommits = 0
        var previews = 0
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            host.get().setContent {
                SaltyGraphicsTheme(darkTheme = true, dynamicColor = false) {
                    // Capture this render's callback intentionally: recomposition must
                    // replace it, not retain a registry-global session callback.
                    val latest = replacementBinding
                    ScaffoldLibraryComponent(
                        id = "media_picker_grid",
                        label = "Project Images",
                        current = SaltyMediaPickerState(items, selected,
                            onImportRequest = { if (latest) latestImports++ else firstImports++ }),
                        onPreview = { previews++ },
                        onCommit = { value ->
                            assertEquals(items, value.media)
                            selected = value.selectedPath
                            if (latest) latestCommits++ else firstCommits++
                        }
                    )
                }
            }
            compose.onNodeWithTag("registered_component_media_picker_grid").assertIsDisplayed()
            compose.onNodeWithText("Import from device").performClick()
            compose.onNodeWithTag("fill_media_two.png").performClick()
            compose.runOnIdle {
                assertEquals("two.png", selected)
                assertEquals(1, firstImports)
                assertEquals(1, firstCommits)
                assertEquals(0, previews)
                assertEquals(listOf("one.png", "two.png"), items.map { it.first })
                items = listOf("three.png" to null)
                replacementBinding = true
            }
            compose.onNodeWithTag("fill_media_one.png").assertDoesNotExist()
            compose.onNodeWithText("Import from device").performClick()
            compose.onNodeWithTag("fill_media_three.png").performClick()
            compose.runOnIdle {
                assertEquals("three.png", selected)
                assertEquals(1, latestImports)
                assertEquals(1, latestCommits)
                assertEquals(1, firstImports)
                assertEquals(1, firstCommits)
                assertEquals(0, previews)
            }
            compose.onAllNodes(isDialog()).assertCountEquals(0)
        } finally {
            host.pause().stop().destroy()
        }
    }
}
