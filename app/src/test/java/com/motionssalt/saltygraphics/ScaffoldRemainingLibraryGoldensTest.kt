package com.motionssalt.saltygraphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.file.ProjectLoadState
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import com.motionssalt.saltygraphics.theme.SaltyGraphicsTheme
import com.motionssalt.saltygraphics.uikit.controls.SaltyAspectRatioTile
import com.motionssalt.saltygraphics.uikit.controls.SaltyValueEntryStrip
import com.motionssalt.saltygraphics.uikit.structure.SaltyEditorTopBar
import com.motionssalt.saltygraphics.uikit.structure.SaltyEmptyState
import com.motionssalt.saltygraphics.uikit.structure.SaltyLayerRow
import com.motionssalt.saltygraphics.uikit.structure.SaltyLoadingSurface
import com.motionssalt.saltygraphics.uikit.structure.SaltyProjectCard
import com.motionssalt.saltygraphics.uikit.structure.SaltySelectableTile
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltyWarningCard
import com.motionssalt.saltygraphics.uikit.structure.saltyPadding
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Static dark/light native golden coverage for the remaining shared :ui-kit components
 * (TopBar, ProjectCard, LayerRow, WarningCard, SelectableTile, AspectRatioTile,
 * ValueEntryStrip, EmptyState, LoadingSurface).
 */
abstract class ScaffoldRemainingLibraryGoldensFixture(private val dark: Boolean) {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun remainingLibraryComponentsMatchReviewedThemeAndDifferAcrossThemes() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var expectedSurfaceColor = 0
        host.get().setContent {
            SaltyGraphicsTheme(darkTheme = dark, dynamicColor = false) {
                expectedSurfaceColor = MaterialTheme.colorScheme.surface.toArgb()
                Surface(Modifier.requiredSize(412.dp, 1180.dp).testTag("matrix_root")) {
                    Column(
                        modifier = Modifier.fillMaxSize().saltyPadding(SaltySpace.MD),
                        verticalArrangement = Arrangement.spacedBy(SaltySpace.SM)
                    ) {
                        SaltyEditorTopBar(
                            previewQuality = PreviewQuality.HALF,
                            canUndo = true,
                            canRedo = false,
                            showGrid = true,
                            gridSize = 50,
                            onNavigateBack = {},
                            onToggleLayers = {},
                            onPreviewQualityClick = {},
                            onUndoClick = {},
                            onRedoClick = {},
                            onOpenActionTree = {},
                            onExportClick = {},
                            onSettingsClick = {},
                            onZoomIn = {},
                            onZoomOut = {},
                            onFitToScreen = {},
                            onReset100Percent = {},
                            onToggleGrid = {},
                            onGridSizeChange = {}
                        )
                        SaltyLayerRow(
                            id = "l1",
                            name = "Headline Group",
                            subtitle = "Group • Normal",
                            isSelected = true,
                            isVisible = true,
                            isLocked = false,
                            showGroupButton = true,
                            onEnterGroup = {},
                            onVisibilityToggle = {},
                            onLockToggle = {}
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SaltySpace.SM)
                        ) {
                            Box(Modifier.weight(1f)) {
                                SaltyProjectCard(
                                    name = "Poster",
                                    width = 1080,
                                    height = 1350,
                                    layerCount = 4,
                                    thumbnailBitmap = null,
                                    isSelected = true,
                                    isSelectionMode = false,
                                    onClick = {},
                                    onLongClick = {}
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                SaltyAspectRatioTile(
                                    ratioWidth = 16f,
                                    ratioHeight = 9f,
                                    displayRatio = "16:9",
                                    shortName = "Wide",
                                    isSelected = true,
                                    onClick = {}
                                )
                            }
                        }
                        SaltySelectableTile(
                            title = "PNG Transparent",
                            subtitle = "Lossless RGBA",
                            badge = "Default",
                            selected = true,
                            onClick = {}
                        )
                        SaltyWarningCard(
                            title = "Safe Mode",
                            message = "Effects temporarily paused."
                        )
                        SaltyValueEntryStrip(
                            label = "Opacity",
                            currentValue = 75f,
                            range = 0f..100f,
                            unit = "%",
                            onConfirm = {},
                            onDismiss = {}
                        )
                        SaltyEmptyState(
                            message = "No layers yet",
                            icon = Icons.Default.Layers,
                            actionText = "Add Layer",
                            onAction = {}
                        )
                        Box(Modifier.fillMaxWidth().height(210.dp)) {
                            SaltyLoadingSurface(
                                state = ProjectLoadState.Error("Archive mismatch"),
                                onRetry = {},
                                onSafeMode = {},
                                onBack = {}
                            )
                        }
                    }
                }
            }
        }
        try {
            compose.mainClock.advanceTimeBy(1000)
            compose.waitForIdle()
            val node = compose.onNodeWithTag("matrix_root").fetchSemanticsNode()
            val actual = Bitmap.createBitmap(node.size.width, node.size.height, Bitmap.Config.ARGB_8888)
            compose.runOnIdle {
                val canvas = android.graphics.Canvas(actual)
                canvas.translate(-node.boundsInWindow.left, -node.boundsInWindow.top)
                host.get().window.decorView.draw(canvas)
            }
            val pixels = IntArray(actual.width * actual.height)
            actual.getPixels(pixels, 0, actual.width, 0, 0, actual.width, actual.height)
            assertTrue("Library golden must contain real controls", pixels.distinct().size > 20)
            assertEquals("Rendered pixels must match static theme surface", expectedSurfaceColor, actual.getPixel(2, 2))
            val name = "components_library_${if (dark) "dark" else "light"}.png"
            File("build/outputs/scaffold-screenshots", name).apply { parentFile.mkdirs() }
                .outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val expected = File("src/test/goldens/scaffold", name)
            val opposite = File("src/test/goldens/scaffold", "components_library_${if (dark) "light" else "dark"}.png")
            assertTrue("Missing reviewed golden: $name", expected.isFile)
            assertTrue("Missing reviewed opposite-theme golden: ${opposite.name}", opposite.isFile)
            val baseline = BitmapFactory.decodeFile(expected.absolutePath)
            val otherTheme = BitmapFactory.decodeFile(opposite.absolutePath)
            assertEquals(baseline.width, actual.width)
            assertEquals(baseline.height, actual.height)
            assertTrue("Changed reviewed golden: $name", baseline.sameAs(actual))
            assertFalse("Light and dark library themes must differ", otherTheme.sameAs(actual))
        } finally {
            host.pause().stop().destroy()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h1220dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScaffoldRemainingLibraryDarkGoldenTest : ScaffoldRemainingLibraryGoldensFixture(true)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h1220dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScaffoldRemainingLibraryLightGoldenTest : ScaffoldRemainingLibraryGoldensFixture(false)
