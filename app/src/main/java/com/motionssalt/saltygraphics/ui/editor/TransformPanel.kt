package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.model.SelectionBounds
import com.motionssalt.saltygraphics.engine.model.SmartGuideLine
import com.motionssalt.saltygraphics.engine.model.SmartSnapEngine
import com.motionssalt.saltygraphics.engine.model.Transform2D
import com.motionssalt.saltygraphics.uikit.controls.SaltyTransformPanel

data class TransformSnapContext(
    val bounds: SelectionBounds,
    val otherLayers: List<Pair<SelectionBounds, Transform2D>> = emptyList(),
    val otherAngles: List<Float> = emptyList(),
    val canvasWidth: Float,
    val canvasHeight: Float,
    val effectiveZoom: Float = 1.0f,
    val snapEnabled: Boolean = true,
    val snapToGrid: Boolean = true,
    val showGrid: Boolean = false,
    val gridSize: Int = 50,
    val isSquareGrid: Boolean = true,
    val tolerancePx: Float = 8f,
    val onGuidesChange: (List<SmartGuideLine>, Float?, Float?) -> Unit = { _, _, _ -> }
)

@Composable
fun TransformPanel(
    transform: Transform2D,
    onTransformChange: (Transform2D) -> Unit,
    onTransformCommit: (Transform2D) -> Unit = onTransformChange,
    snapContext: TransformSnapContext? = null,
    onClose: () -> Unit,
    wrapInPanelPage: Boolean = false
) {
    fun applyTranslationSnapping(rawTransform: Transform2D): Transform2D {
        if (snapContext == null) return rawTransform
        var currentT = rawTransform
        var guides = emptyList<SmartGuideLine>()
        var snappedGridX: Float? = null
        var snappedGridY: Float? = null
        if (snapContext.snapEnabled) {
            val snapRes = SmartSnapEngine.snapTranslation(
                targetBounds = snapContext.bounds,
                targetTransform = currentT,
                otherLayers = snapContext.otherLayers,
                canvasWidth = snapContext.canvasWidth,
                canvasHeight = snapContext.canvasHeight,
                effectiveZoom = snapContext.effectiveZoom,
                tolerancePx = snapContext.tolerancePx
            )
            currentT = snapRes.snappedTransform
            guides = snapRes.activeGuides
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
                tolerancePx = snapContext.tolerancePx
            )
            currentT = gridSnap.snappedTransform
            snappedGridX = gridSnap.snappedGridX
            snappedGridY = gridSnap.snappedGridY
        }
        snapContext.onGuidesChange(guides, snappedGridX, snappedGridY)
        return currentT
    }

    fun applyRotationSnapping(rawDegrees: Float): Float {
        if (snapContext == null || !snapContext.snapEnabled) return rawDegrees
        val (snapped, didSnap) = SmartSnapEngine.snapRotation(rawDegrees, snapContext.otherAngles)
        return if (didSnap) snapped else rawDegrees
    }

    fun clearGuides() {
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
        wrapInPanelPage = wrapInPanelPage
    )
}
