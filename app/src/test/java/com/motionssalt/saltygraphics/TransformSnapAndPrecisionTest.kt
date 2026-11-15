package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.ui.editor.EditorScreen
import com.motionssalt.saltygraphics.uikit.structure.SaltyToastManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TransformSnapAndPrecisionTest {
    @get:Rule
    val compose = createEmptyComposeRule()
    private val context get() = RuntimeEnvironment.getApplication()

    @After
    fun cleanup() {
        SaltyToastManager.clear()
    }

    // --- V38: Canvas center snapping priority + zoom-invariant tolerance + hysteresis ---

    @Test
    fun canvasCenterTakesPriorityOverNearbySiblingEdgesAcrossZoomsAndHysteresisHolds() {
        val targetBounds = SelectionBounds(-50f, -50f, 50f, 50f)
        // Sibling positioned so its right edge sits at x = +4f (even closer to x = 4f than canvas center 0f!)
        val siblingBounds = SelectionBounds(-50f, -50f, 50f, 50f)
        val siblingTransform = Transform2D(translationX = -46f, translationY = -46f) // right/bottom edge at +4f
        val others = listOf(siblingBounds to siblingTransform)

        for (zoom in listOf(0.5f, 1.0f, 2.5f)) {
            val rawX = 4f / zoom
            val res = SmartSnapEngine.snapTranslation(
                targetBounds = targetBounds,
                targetTransform = Transform2D(translationX = rawX, translationY = rawX),
                otherLayers = others,
                canvasWidth = 1000f,
                canvasHeight = 1000f,
                effectiveZoom = zoom,
                tolerancePx = 8f
            )
            assertTrue("Canvas center X must win over sibling edge at zoom=$zoom", res.didSnapCenterX)
            assertTrue("Canvas center Y must win over sibling edge at zoom=$zoom", res.didSnapCenterY)
            assertEquals(0f, res.snappedTransform.translationX, 0.001f)
            assertEquals(0f, res.snappedTransform.translationY, 0.001f)
            assertTrue(res.activeGuides.any { it.orientation == GuideOrientation.VERTICAL && abs(it.position) < 0.01f })
            assertTrue(res.activeGuides.any { it.orientation == GuideOrientation.HORIZONTAL && abs(it.position) < 0.01f })
        }

        // Hysteresis break-away at zoom = 1f: base center tol = 8 * 1.25 = 10f, hysteresis = 10 * 1.85 = 18.5f
        val held = SmartSnapEngine.snapTranslation(
            targetBounds = targetBounds,
            targetTransform = Transform2D(translationX = 14f, translationY = 14f),
            otherLayers = others,
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            effectiveZoom = 1f,
            tolerancePx = 8f,
            previouslySnappedX = true,
            previouslySnappedY = true,
            previouslySnappedCenterX = true,
            previouslySnappedCenterY = true
        )
        assertTrue("Center lock must hold inside hysteresis threshold", held.didSnapCenterX)
        assertEquals(0f, held.snappedTransform.translationX, 0.001f)

        val broken = SmartSnapEngine.snapTranslation(
            targetBounds = targetBounds,
            targetTransform = Transform2D(translationX = 22f, translationY = 22f),
            otherLayers = others,
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            effectiveZoom = 1f,
            tolerancePx = 8f,
            previouslySnappedX = true,
            previouslySnappedY = true,
            previouslySnappedCenterX = true,
            previouslySnappedCenterY = true
        )
        assertFalse("Center lock must break away cleanly beyond hysteresis", broken.didSnapCenterX)
        assertEquals(22f, broken.snappedTransform.translationX, 0.001f)
    }

    @Test
    fun scaleHandleEdgeSnapsToCanvasCenterWithHysteresis() {
        val bounds = SelectionBounds(-50f, -50f, 50f, 50f)
        val start = Transform2D(translationX = 100f, translationY = 0f, scaleX = 1.8f, scaleY = 1.8f)
        // Left edge at 100 - 50 * 1.92 = 4f (within canvas center 0f tolerance)
        val raw = start.copy(scaleX = 1.92f)
        val snapRes = SmartSnapEngine.snapScaleHandleToCanvasCenter(
            targetBounds = bounds,
            startTransform = start,
            rawTransform = raw,
            handle = SelectionHandle.EDGE_LEFT,
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            effectiveZoom = 1f,
            tolerancePx = 8f
        )
        assertTrue(snapRes.didSnapCenterX)
        assertEquals(2.0f, snapRes.snappedTransform.scaleX, 0.001f)
    }

    // --- V39: Pure composition transforms (10 actions, wide/tall/rotated/group/multi/locked) ---

    @Test
    fun compositionTransformsCoverAllTenActionsRotatedAndLockedGuards() {
        val wide = Layer(
            id = "wide",
            name = "Wide",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 400f, height = 100f),
            transform = Transform2D(translationX = 120f, translationY = -80f, scaleX = 1.5f, scaleY = 0.8f)
        )
        val bounds = SelectionBounds(-200f, -50f, 200f, 50f)
        val canvas = CanvasSize(1000, 600)

        fun runAction(layer: Layer, action: QuickTransformAction): Transform2D =
            CompositionTransforms.apply(layer, bounds, canvas, action)

        // Align edges & centers (visual bounds of wide at scale 1.5x0.8 are width=600, height=80)
        val left = runAction(wide, QuickTransformAction.ALIGN_LEFT)
        assertEquals(-500f + 300f, left.translationX, 0.01f)

        val hCenter = runAction(wide, QuickTransformAction.ALIGN_H_CENTER)
        assertEquals(0f, hCenter.translationX, 0.01f)

        val right = runAction(wide, QuickTransformAction.ALIGN_RIGHT)
        assertEquals(500f - 300f, right.translationX, 0.01f)

        val top = runAction(wide, QuickTransformAction.ALIGN_TOP)
        assertEquals(-300f + 40f, top.translationY, 0.01f)

        val vCenter = runAction(wide, QuickTransformAction.ALIGN_V_CENTER)
        assertEquals(0f, vCenter.translationY, 0.01f)

        val bottom = runAction(wide, QuickTransformAction.ALIGN_BOTTOM)
        assertEquals(300f - 40f, bottom.translationY, 0.01f)

        // Fit: uniform scale so 400x100 fits 1000x600 -> min(1000/400=2.5, 600/100=6.0) = 2.5f
        val fit = runAction(wide, QuickTransformAction.FIT_TO_COMPOSITION)
        assertEquals(2.5f, fit.scaleX, 0.01f)
        assertEquals(2.5f, fit.scaleY, 0.01f)
        assertEquals(0f, fit.translationX, 0.01f)
        assertEquals(0f, fit.translationY, 0.01f)

        // Fill: uniform scale so 400x100 covers 1000x600 -> max(2.5, 6.0) = 6.0f
        val fill = runAction(wide, QuickTransformAction.FILL_COMPOSITION)
        assertEquals(6.0f, fill.scaleX, 0.01f)
        assertEquals(6.0f, fill.scaleY, 0.01f)

        // Stretch: 400 -> 1000 (2.5f), 100 -> 600 (6.0f)
        val stretch = runAction(wide, QuickTransformAction.STRETCH_TO_COMPOSITION)
        assertEquals(2.5f, stretch.scaleX, 0.01f)
        assertEquals(6.0f, stretch.scaleY, 0.01f)

        // Reset to actual size keeps visual center
        val reset = runAction(wide, QuickTransformAction.RESET_ACTUAL_SIZE)
        assertEquals(1f, reset.scaleX, 0.001f)
        assertEquals(1f, reset.scaleY, 0.001f)
        assertEquals(120f, reset.translationX, 0.01f)
        assertEquals(-80f, reset.translationY, 0.01f)

        // Rotated 90° Fit matches 1000 / 100 = 10f, 600 / 400 = 1.5f -> 1.5f
        val rot90 = wide.copy(transform = wide.transform.copy(rotationDegrees = 90f))
        val fitRot = runAction(rot90, QuickTransformAction.FIT_TO_COMPOSITION)
        assertEquals(1.5f, fitRot.scaleX, 0.01f)
        assertEquals(1.5f, fitRot.scaleY, 0.01f)

        // Locked layer never changes
        val locked = wide.copy(isLocked = true)
        assertEquals(locked.transform, runAction(locked, QuickTransformAction.FIT_TO_COMPOSITION))
    }

    // --- V39 + V43 Real EditorScreen Integration (UI, Undo/Redo, Hold repeat, Multi-selection, Locked) ---

    @Test
    fun editorTransformQuickActionsNudgeHoldRepeatAndExactEntryCommitCleanHistory() {
        val repo = ProjectRepository(context)
        val l1 = Layer(
            id = "l1",
            name = "Hero Box",
            type = LayerType.SHAPE,
            shapeData = ShapeData(width = 200f, height = 100f),
            transform = Transform2D(translationX = 0f, translationY = 0f, scaleX = 2f, scaleY = 2f)
        )
        val l2 = Layer(
            id = "l2",
            name = "Locked Box",
            type = LayerType.SHAPE,
            isLocked = true,
            shapeData = ShapeData(width = 100f, height = 100f),
            transform = Transform2D(translationX = 50f, translationY = 50f)
        )
        val initialDoc = runBlocking {
            val base = repo.createProject("Transform Precision", CanvasSize(1000, 1000))
            base.copy(layers = listOf(l1, l2)).also { repo.saveProject(it) }
        }
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var latestDoc = initialDoc
        host.get().setContent {
            MaterialTheme {
                EditorScreen(
                    projectId = initialDoc.id,
                    repository = repo,
                    onNavigateBack = {},
                    onNavigateToExport = {},
                    onNavigateToSettings = {}
                )
            }
        }
        try {
            compose.waitUntil(5000) { compose.onAllNodesWithTag("editor_canvas").fetchSemanticsNodes().isNotEmpty() }

            // Select Hero Box via Layers drawer so click is exact
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText("Hero Box").performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()

            // Open Transform
            compose.onNodeWithText("Transform", useUnmergedTree = true).performClick()
            compose.onNodeWithText("2D Transform").assertIsDisplayed()

            // 1. Tap "Reset to Actual Size" -> scaleX/Y becomes 1.0, history node recorded
            compose.onNodeWithTag("quick_transform_reset_actual_size").performScrollTo().performClick()
            compose.waitUntil(10000) { runBlocking { repo.getProject(initialDoc.id)?.findLayerById("l1")?.transform?.scaleX?.let { kotlin.math.abs(it - 1.0f) < 0.01f } == true } }
            latestDoc = runBlocking { repo.getProject(initialDoc.id)!! }
            assertEquals(1.0f, latestDoc.findLayerById("l1")!!.transform.scaleX, 0.01f)

            // 2. Tap "Left" -> translates box left edge to -500 (-500 + 100 = -400)
            compose.onNodeWithTag("quick_transform_align_left").performScrollTo().performClick()
            compose.waitUntil(10000) { runBlocking { repo.getProject(initialDoc.id)?.findLayerById("l1")?.transform?.translationX?.let { kotlin.math.abs(it - (-400f)) < 0.5f } == true } }
            latestDoc = runBlocking { repo.getProject(initialDoc.id)!! }
            assertEquals(-400f, latestDoc.findLayerById("l1")!!.transform.translationX, 0.5f)

            // Undo returns to previous centered X
            compose.onNodeWithContentDescription("Undo").performClick()
            compose.waitUntil(10000) { runBlocking { repo.getProject(initialDoc.id)?.findLayerById("l1")?.transform?.translationX?.let { kotlin.math.abs(it) < 0.5f } == true } }
            latestDoc = runBlocking { repo.getProject(initialDoc.id)!! }
            assertEquals(0f, latestDoc.findLayerById("l1")!!.transform.translationX, 0.5f)

            // 3. V43 Nudge mode: switch segmented chips to "Nudge"
            compose.onNodeWithText("Nudge").performScrollTo().performClick()
            compose.onNodeWithTag("nudge_pad").assertIsDisplayed()

            // Single tap right (+1px default)
            compose.onNodeWithTag("nudge_right").performScrollTo().performClick()
            compose.waitUntil(10000) { runBlocking { repo.getProject(initialDoc.id)?.findLayerById("l1")?.transform?.translationX?.let { kotlin.math.abs(it - 1f) < 0.01f } == true } }
            latestDoc = runBlocking { repo.getProject(initialDoc.id)!! }
            assertEquals(1f, latestDoc.findLayerById("l1")!!.transform.translationX, 0.01f)

            // Hold right (~600ms -> repeats multiple steps, commits ONE single undoable history node)
            compose.onNodeWithTag("nudge_right").performTouchInput {
                down(center)
            }
            compose.mainClock.advanceTimeBy(650L)
            compose.onNodeWithTag("nudge_right").performTouchInput {
                up()
            }
            compose.waitUntil(10000) { runBlocking { (repo.getProject(initialDoc.id)?.findLayerById("l1")?.transform?.translationX ?: 0f) >= 3f } }
            latestDoc = runBlocking { repo.getProject(initialDoc.id)!! }
            val afterHoldX = latestDoc.findLayerById("l1")!!.transform.translationX
            assertTrue("Hold must move multiple steps beyond 1px (got $afterHoldX)", afterHoldX >= 3f)

            // One single Undo reverts the ENTIRE hold back to 1f!
            compose.onNodeWithContentDescription("Undo").performClick()
            compose.waitUntil(10000) { runBlocking { repo.getProject(initialDoc.id)?.findLayerById("l1")?.transform?.translationX?.let { kotlin.math.abs(it - 1f) < 0.01f } == true } }
            latestDoc = runBlocking { repo.getProject(initialDoc.id)!! }
            assertEquals("Single hold must revert in one Undo step", 1f, latestDoc.findLayerById("l1")!!.transform.translationX, 0.01f)

            // 4. Switch to Sliders and test exact numeric entry on Move X
            compose.onNodeWithText("Sliders").performScrollTo().performClick()
            compose.onNodeWithText("1 px", useUnmergedTree = true).performScrollTo().performClick()
            compose.onNodeWithTag("entry_Move X").performTextReplacement("250")
            compose.onNodeWithText("Done").performClick()
            compose.waitUntil(10000) { runBlocking { repo.getProject(initialDoc.id)?.findLayerById("l1")?.transform?.translationX?.let { kotlin.math.abs(it - 250f) < 0.01f } == true } }
            latestDoc = runBlocking { repo.getProject(initialDoc.id)!! }
            assertEquals(250f, latestDoc.findLayerById("l1")!!.transform.translationX, 0.01f)

            // 5. Locked layer guard: select Locked Box and tap Nudge Up -> stays at (50, 50) + shows toast
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText("Locked Box").performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()
            compose.onNodeWithText("Nudge").performScrollTo().performClick()
            compose.onNodeWithTag("nudge_up").performScrollTo().performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Layer is locked").assertIsDisplayed()
            latestDoc = runBlocking { repo.getProject(initialDoc.id)!! }
            assertEquals(50f, latestDoc.findLayerById("l2")!!.transform.translationY, 0.01f)
        } finally {
            host.pause().stop().destroy()
        }
    }
}
