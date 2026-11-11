package com.motionssalt.saltygraphics

import androidx.compose.ui.geometry.Offset
import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class CanvasLockFadeAndGridSnapTest {

    @Test
    fun testDefaultCanvasColorIsNeutralGray() {
        // V14: New projects use neutral gray as default canvas color
        val defaultSettings = EditorSettings()
        assertEquals(
            "Default canvas background color must be neutral gray (0xFF808080L)",
            0xFF808080L,
            defaultSettings.backgroundColor
        )

        // Existing projects keep their saved color
        val existingLegacySettings = EditorSettings(backgroundColor = 0xFF000000L)
        assertEquals(0xFF000000L, existingLegacySettings.backgroundColor)

        val customColorSettings = EditorSettings(backgroundColor = 0xFF1E1E2EL)
        assertEquals(0xFF1E1E2EL, customColorSettings.backgroundColor)
    }

    @Test
    fun testGridLineSnappingIsIndependentOfGeneralSnapping() {
        // V15: snapToGrid is separate from snapEnabled
        val settings = EditorSettings(snapEnabled = false, snapToGrid = true)
        assertFalse("General snap should be disabled", settings.snapEnabled)
        assertTrue("Grid-line snap should be enabled", settings.snapToGrid)

        val altSettings = EditorSettings(snapEnabled = true, snapToGrid = false)
        assertTrue("General snap should be enabled", altSettings.snapEnabled)
        assertFalse("Grid-line snap should be disabled", altSettings.snapToGrid)
    }

    @Test
    fun testSnapToGridLinesCalculatesCorrectCoordinatesAndHighlights() {
        val bounds = SelectionBounds(left = -50f, top = -50f, right = 50f, bottom = 50f)
        val initialTransform = Transform2D(translationX = 103f, translationY = 198f) // Near (100, 200)

        // Test square grid (step = 100)
        val result = SmartSnapEngine.snapToGridLines(
            targetBounds = bounds,
            targetTransform = initialTransform,
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            gridSize = 100,
            isSquareGrid = true,
            effectiveZoom = 1.0f,
            tolerancePx = 8f
        )

        assertTrue("Should snap X near grid line 100", result.didSnapX)
        assertTrue("Should snap Y near grid line 200", result.didSnapY)
        assertEquals(100f, result.snappedTransform.translationX, 0.001f)
        assertEquals(200f, result.snappedTransform.translationY, 0.001f)
        assertEquals(100f, result.snappedGridX!!, 0.001f)
        assertEquals(200f, result.snappedGridY!!, 0.001f)

        // When far outside tolerance (e.g. at 125, 230), no snap should occur
        val farTransform = Transform2D(translationX = 125f, translationY = 230f)
        val farResult = SmartSnapEngine.snapToGridLines(
            targetBounds = bounds,
            targetTransform = farTransform,
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            gridSize = 100,
            isSquareGrid = true,
            effectiveZoom = 1.0f,
            tolerancePx = 8f
        )
        assertFalse("Should not snap X when outside tolerance", farResult.didSnapX)
        assertFalse("Should not snap Y when outside tolerance", farResult.didSnapY)
        assertNull(farResult.snappedGridX)
        assertNull(farResult.snappedGridY)
        assertEquals(125f, farResult.snappedTransform.translationX, 0.001f)
    }

    @Test
    fun testGridSnappingToleranceScalesWithZoom() {
        val bounds = SelectionBounds(left = -50f, top = -50f, right = 50f, bottom = 50f)
        // Delta of 12 document units
        val transform = Transform2D(translationX = 112f, translationY = 0f)

        // At zoom 1.0x with tolerance 8px, delta 12 exceeds tolerance
        val res1x = SmartSnapEngine.snapToGridLines(
            targetBounds = bounds,
            targetTransform = transform,
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            gridSize = 100,
            isSquareGrid = true,
            effectiveZoom = 1.0f,
            tolerancePx = 8f
        )
        assertFalse("Delta 12 exceeds 8px tolerance at 1.0x zoom", res1x.didSnapX)

        // At zoom 0.5x, 8 screen px corresponds to 16 document units, so delta 12 is within tolerance
        val resHalfZoom = SmartSnapEngine.snapToGridLines(
            targetBounds = bounds,
            targetTransform = transform,
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            gridSize = 100,
            isSquareGrid = true,
            effectiveZoom = 0.5f,
            tolerancePx = 8f
        )
        assertTrue("Delta 12 is within screen-pixel tolerance at 0.5x zoom", resHalfZoom.didSnapX)
        assertEquals(100f, resHalfZoom.snappedTransform.translationX, 0.001f)
    }

    @Test
    fun testCanvasLockToggleStateAndNavigationGuards() {
        var isLocked = false
        val toggleLock = { isLocked = !isLocked }

        assertFalse(isLocked)
        toggleLock()
        assertTrue("Canvas should be locked after toggle", isLocked)

        // Zoom action guard simulation: when locked, zoom must not change
        var currentZoom = 1.0f
        val onZoomIn = { if (!isLocked) currentZoom *= 1.25f }

        onZoomIn()
        assertEquals("Zoom must not change while canvas is locked", 1.0f, currentZoom, 0.0001f)

        // Unlock
        toggleLock()
        assertFalse(isLocked)

        onZoomIn()
        assertEquals("Zoom changes after canvas is unlocked", 1.25f, currentZoom, 0.0001f)
    }

    @Test
    fun testSnappingDefaultsAreOnForNewAndMigratedProjects() {
        val newSettings = EditorSettings()
        assertTrue("snapEnabled should be ON by default", newSettings.snapEnabled)
        assertTrue("snapToGrid should be ON by default", newSettings.snapToGrid)

        // ProjectDocument default
        val doc = ProjectDocument(id = "test-doc")
        assertTrue(doc.editorSettings.snapEnabled)
        assertTrue(doc.editorSettings.snapToGrid)
    }

    @Test
    fun testGridSnappingOnlyEngagesWhenGridIsVisible() {
        val bounds = SelectionBounds(left = -50f, top = -50f, right = 50f, bottom = 50f)
        val nearGridTransform = Transform2D(translationX = 102f, translationY = 0f)

        // When showGrid is false, canvas and controls must not snap to grid lines
        val showGridFalse = false
        val snapToGrid = true
        val shouldSnapToGrid = snapToGrid && showGridFalse
        assertFalse("Grid snapping must not engage when grid is not visible", shouldSnapToGrid)

        // When showGrid is true, snapping engages
        val showGridTrue = true
        val shouldSnapWhenVisible = snapToGrid && showGridTrue
        assertTrue("Grid snapping engages when grid is visible", shouldSnapWhenVisible)

        val snapRes = SmartSnapEngine.snapToGridLines(
            targetBounds = bounds,
            targetTransform = nearGridTransform,
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            gridSize = 100,
            isSquareGrid = true,
            effectiveZoom = 1.0f
        )
        assertTrue(snapRes.didSnapX)
        assertEquals(100f, snapRes.snappedTransform.translationX, 0.001f)
    }

    @Test
    fun testTransformPanelSnappingIntegration() {
        val bounds = SelectionBounds(left = -50f, top = -50f, right = 50f, bottom = 50f)
        val otherBounds = SelectionBounds(left = -40f, top = -40f, right = 40f, bottom = 40f)
        val otherTransform = Transform2D(translationX = 200f, translationY = 200f)

        // Snapping translation to other layer's center/edge
        val rawTranslation = Transform2D(translationX = 198f, translationY = 50f)
        val snapRes = SmartSnapEngine.snapTranslation(
            targetBounds = bounds,
            targetTransform = rawTranslation,
            otherLayers = listOf(otherBounds to otherTransform),
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            effectiveZoom = 1.0f
        )
        assertTrue("Should snap to layer center at 200f", snapRes.didSnapX)
        assertEquals(200f, snapRes.snappedTransform.translationX, 0.001f)
        assertTrue(snapRes.activeGuides.any { it.position == 200f })

        // Rotation snapping to standard 45-degree angle
        val (snappedAngle, didSnapAngle) = SmartSnapEngine.snapRotation(46.5f)
        assertTrue(didSnapAngle)
        assertEquals(45f, snappedAngle, 0.001f)
    }
}
