package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.engine.model.AspectRatioPreset
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.QualityPreset
import com.motionssalt.saltygraphics.ui.controls.ValueValidation
import com.motionssalt.saltygraphics.ui.newproject.NewProjectScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AspectRatioPickerTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun testAspectRatioDimensionCalculations() {
        // 9:16 at 1080p: baseDim = 1080 -> 1080 x 1920
        val storyDims = AspectRatioPreset.STORY_9_16.computeDimensions(QualityPreset.P1080, isRotated = false)
        assertEquals(Pair(1080, 1920), storyDims)

        // 9:16 rotated: 1920 x 1080
        val rotatedStoryDims = AspectRatioPreset.STORY_9_16.computeDimensions(QualityPreset.P1080, isRotated = true)
        assertEquals(Pair(1920, 1080), rotatedStoryDims)

        // 1:1 at 1080p: 1080 x 1080
        val squareDims = AspectRatioPreset.SQUARE.computeDimensions(QualityPreset.P1080, isRotated = false)
        assertEquals(Pair(1080, 1080), squareDims)

        // 16:9 at 1080p: 1920 x 1080
        val landscapeDims = AspectRatioPreset.LANDSCAPE_16_9.computeDimensions(QualityPreset.P1080, isRotated = false)
        assertEquals(Pair(1920, 1080), landscapeDims)

        // 4:5 at 1080p: 1080 x 1350
        val portraitDims = AspectRatioPreset.PORTRAIT_4_5.computeDimensions(QualityPreset.P1080, isRotated = false)
        assertEquals(Pair(1080, 1350), portraitDims)

        // YouTube Thumbnail (16:9) at 720p: 1280 x 720
        val ytDims = AspectRatioPreset.YOUTUBE_THUMBNAIL.computeDimensions(QualityPreset.P720, isRotated = false)
        assertEquals(Pair(1280, 720), ytDims)

        // Banner 3:1 at 1080p: 3240 x 1080
        val bannerDims = AspectRatioPreset.BANNER_3_1.computeDimensions(QualityPreset.P1080, isRotated = false)
        assertEquals(Pair(3240, 1080), bannerDims)
    }

    @Test
    fun testCustomDimensionValidation() {
        // Allowed range: 100 .. 8192 px
        val (valid1, _) = ValueValidation.validate("1920", 100f, 8192f, "px")
        assertTrue("1920 must be valid", valid1)

        val (validMin, _) = ValueValidation.validate("100", 100f, 8192f, "px")
        assertTrue("100 must be valid", validMin)

        val (validMax, _) = ValueValidation.validate("8192", 100f, 8192f, "px")
        assertTrue("8192 must be valid", validMax)

        val (invalidLow, errLow) = ValueValidation.validate("50", 100f, 8192f, "px")
        assertFalse("50 is below min and must be refused", invalidLow)
        assertNotNull(errLow)

        val (invalidHigh, errHigh) = ValueValidation.validate("10000", 100f, 8192f, "px")
        assertFalse("10000 is above max and must be refused", invalidHigh)
        assertNotNull(errHigh)

        val (invalidLetters, _) = ValueValidation.validate("bad", 100f, 8192f, "px")
        assertFalse("Text must be refused", invalidLetters)
    }

    @Test
    fun testNewProjectScreenRendersTilesAndCreatesCanvas() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            val repo = ProjectRepository(RuntimeEnvironment.getApplication())
            var createdProjectId: String? = null

            host.get().setContent {
                MaterialTheme {
                    NewProjectScreen(
                        repository = repo,
                        onNavigateBack = {},
                        onProjectCreated = { createdProjectId = it }
                    )
                }
            }

            // Verify title and most used ratio labels are visible
            compose.onNodeWithText("New Project").assertIsDisplayed()
            compose.onNodeWithText("Most Used").assertIsDisplayed()
            compose.onNodeWithText("9:16").assertIsDisplayed()
            compose.onNodeWithText("1:1").assertIsDisplayed()

            // Verify bottom bar displays initial size (1080 x 1920 px)
            compose.onNodeWithText("1080 x 1920 px").assertIsDisplayed()

            // Click Rotate button
            compose.onNodeWithText("Rotate").performClick()
            // Bottom bar updates to rotated dimensions: 1920 x 1080 px
            compose.onNodeWithText("1920 x 1080 px").assertIsDisplayed()

            // Select 1:1 tile
            compose.onNodeWithText("1:1").performClick()
            compose.onNodeWithText("1080 x 1080 px").assertIsDisplayed()

            // Tap Create Canvas
            compose.onNodeWithText("Create Canvas").performClick()
            compose.waitUntil(5000) { createdProjectId != null }

            assertNotNull("Project must be created", createdProjectId)
            val doc = runBlocking { repo.getProject(createdProjectId!!) }
            assertNotNull(doc)
            assertEquals(1080, doc?.canvasSize?.width)
            assertEquals(1080, doc?.canvasSize?.height)
        } finally {
            host.pause().stop().destroy()
        }
    }
}
