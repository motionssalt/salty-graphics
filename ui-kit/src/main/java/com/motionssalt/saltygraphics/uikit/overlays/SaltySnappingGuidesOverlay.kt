package com.motionssalt.saltygraphics.uikit.overlays

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

data class SnapGuideLine(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val isGridLine: Boolean = false,
    val isSnappedActive: Boolean = true
)

/**
 * On-canvas snapping guides overlay per V15 requirements.
 * Renders active smart guides (red) and grid snapping guides (highlighted cyan).
 */
@Composable
fun SaltySnappingGuidesOverlay(
    guides: List<SnapGuideLine>,
    modifier: Modifier = Modifier
) {
    val colors = SaltyTokens.colors

    Canvas(modifier = modifier.fillMaxSize()) {
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)

        guides.forEach { guide ->
            val guideColor = when {
                guide.isGridLine && guide.isSnappedActive -> colors.snapGuideGrid
                guide.isSnappedActive -> colors.snapGuideSmart
                else -> colors.outline
            }

            drawLine(
                color = guideColor,
                start = Offset(guide.x1, guide.y1),
                end = Offset(guide.x2, guide.y2),
                strokeWidth = if (guide.isSnappedActive) 2.dp.toPx() else 1.dp.toPx(),
                pathEffect = if (guide.isGridLine) null else dashEffect
            )
        }
    }
}
