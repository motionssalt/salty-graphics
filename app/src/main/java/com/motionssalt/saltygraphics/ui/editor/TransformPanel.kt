package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import com.motionssalt.saltygraphics.engine.model.QuickTransformAction
import com.motionssalt.saltygraphics.engine.model.SelectionBounds
import com.motionssalt.saltygraphics.engine.model.SmartGuideLine
import com.motionssalt.saltygraphics.engine.model.SmartSnapEngine
import com.motionssalt.saltygraphics.engine.model.Transform2D
import com.motionssalt.saltygraphics.uikit.controls.SaltyHaptics
import com.motionssalt.saltygraphics.uikit.controls.SaltyTransformPanel

data class TransformSnapContext(
    val bounds: SelectionBounds,
    val otherLayers: List<Pair<SelectionBounds, Transform2D>>,
    val otherAngles: List<Float>,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val effectiveZoom: Float,
    val snapEnabled: Boolean,
    val snapToGrid: Boolean,
    val showGrid: Boolean,
    val gridSize: Int,
    val isSquareGrid: Boolean,
    val tolerancePx: Float,
    val onGuidesChange: (List<SmartGuideLine>, Float?, Float?) -> Unit
)

@Composable
fun TransformPanel(
    transform: Transform2D,
    onTransformChange: (Transform2D) -> Unit,
    onTransformCommit: (Transform2D) -> Unit = onTransformChange,
    snapContext: TransformSnapContext? = null,
    onClose: () -> Unit,
    wrapInPanelPage: Boolean = false,
    nudgeStepPx: Float = 1f,
    onNudgeStepChange: (Float) -> Unit = {},
    onNudgeDelta: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    onNudgeCommit: (totalDx: Float, totalDy: Float) -> Unit = { _, _ -> },
    onNudgeCancel: () -> Unit = {},
    onQuickTransform: ((QuickTransformAction) -> Unit)? = null
) {
    val hostView = LocalView.current
    var prevSnappedX by remember { mutableStateOf(false) }
    var prevSnappedY by remember { mutableStateOf(false) }
    var prevSnappedCenterX by remember { mutableStateOf(false) }
    var prevSnappedCenterY by remember { mutableStateOf(false) }
    var prevGridX by remember { mutableStateOf<Float?>(null) }
    var prevGridY by remember { mutableStateOf<Float?>(null) }
    var prevGuides by remember { mutableStateOf<Set<Pair<Any, Float>>>(emptySet()) }

    fun applyTranslationSnapping(rawTransform: Transform2D): Transform2D {
        if (snapContext == null) return rawTransform
        var currentT = rawTransform
        var guides = emptyList<SmartGuideLine>()
        var snappedGridX: Float? = null
        var snappedGridY: Float? = null
        var centerLockedX = false
        var centerLockedY = false
        if (snapContext.snapEnabled) {
            val snapRes = SmartSnapEngine.snapTranslation(
                targetBounds = snapContext.bounds,
                targetTransform = currentT,
                otherLayers = snapContext.otherLayers,
                canvasWidth = snapContext.canvasWidth,
                canvasHeight = snapContext.canvasHeight,
                effectiveZoom = snapContext.effectiveZoom,
                tolerancePx = snapContext.tolerancePx,
                previouslySnappedX = prevSnappedX,
                previouslySnappedY = prevSnappedY,
                previouslySnappedCenterX = prevSnappedCenterX,
                previouslySnappedCenterY = prevSnappedCenterY
            )
            currentT = snapRes.snappedTransform
            guides = snapRes.activeGuides
            prevSnappedX = snapRes.didSnapX
            prevSnappedY = snapRes.didSnapY
            prevSnappedCenterX = snapRes.didSnapCenterX
            prevSnappedCenterY = snapRes.didSnapCenterY
            centerLockedX = snapRes.didSnapCenterX
            centerLockedY = snapRes.didSnapCenterY
        }
        if (snapContext.snapToGrid && snapContext.showGrid) {
            val gridSnap = SmartSnapEngine.snapToGridLines(
                targetBounds = snapContext.bounds,
                targetTransform = currentT,
                canvasWidth = snapContext.canvasWidth,
                canvasHeight = snapContext.canvasHeight,
                gridSize = snapContext.gridSize,
                isSquareGrid = snapContext.isSquareGrid,
                effectiveZoom = snapContext.effectiveZoom,
                tolerancePx = snapContext.tolerancePx,
                previouslySnappedX = prevGridX != null,
                previouslySnappedY = prevGridY != null
            )
            val resolvedX = if (centerLockedX) currentT.translationX else gridSnap.snappedTransform.translationX
            val resolvedY = if (centerLockedY) currentT.translationY else gridSnap.snappedTransform.translationY
            currentT = currentT.copy(translationX = resolvedX, translationY = resolvedY)
            snappedGridX = if (centerLockedX) null else gridSnap.snappedGridX
            snappedGridY = if (centerLockedY) null else gridSnap.snappedGridY
        }
        val nextGuides = guides.map { it.orientation to it.position }.toSet()
        val guideLocked = nextGuides.any { it !in prevGuides }
        val gridLocked = (snappedGridX != null && snappedGridX != prevGridX) ||
            (snappedGridY != null && snappedGridY != prevGridY)
        if (guideLocked) {
            SaltyHaptics.onGuideLocked(hostView)
        } else if (gridLocked) {
            SaltyHaptics.onSnapEngaged(hostView)
        }
        prevGuides = nextGuides
        prevGridX = snappedGridX
        prevGridY = snappedGridY
        snapContext.onGuidesChange(guides, snappedGridX, snappedGridY)
        return currentT
    }

    fun applyRotationSnapping(rawDegrees: Float): Float {
        if (snapContext == null || !snapContext.snapEnabled) return rawDegrees
        val (snapped, didSnap) = SmartSnapEngine.snapRotation(rawDegrees, snapContext.otherAngles)
        if (didSnap) SaltyHaptics.onSnapEngaged(hostView)
        return if (didSnap) snapped else rawDegrees
    }

    fun clearGuides() {
        prevSnappedX = false
        prevSnappedY = false
        prevSnappedCenterX = false
        prevSnappedCenterY = false
        prevGridX = null
        prevGridY = null
        prevGuides = emptySet()
        snapContext?.onGuidesChange(emptyList(), null, null)
    }

    SaltyTransformPanel(
        transform = transform,
        snapEnabled = snapContext?.snapEnabled == true,
        applyTranslationSnapping = ::applyTranslationSnapping,
        applyRotationSnapping = ::applyRotationSnapping,
        clearGuides = ::clearGuides,
        onTransformChange = onTransformChange,
        onTransformCommit = onTransformCommit,
        onClose = onClose,
        wrapInPanelPage = wrapInPanelPage,
        nudgeStepPx = nudgeStepPx,
        onNudgeStepChange = onNudgeStepChange,
        onNudgeDelta = onNudgeDelta,
        onNudgeCommit = onNudgeCommit,
        onNudgeCancel = onNudgeCancel,
        onQuickTransform = onQuickTransform
    )
}
