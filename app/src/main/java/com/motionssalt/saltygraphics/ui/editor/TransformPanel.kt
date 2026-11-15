package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.engine.model.SelectionBounds
import com.motionssalt.saltygraphics.engine.model.SmartGuideLine
import com.motionssalt.saltygraphics.engine.model.SmartSnapEngine
import com.motionssalt.saltygraphics.engine.model.Transform2D
import com.motionssalt.saltygraphics.uikit.controls.SaltyNumberControl
import com.motionssalt.saltygraphics.uikit.controls.SaltyPositionTrackpad
import com.motionssalt.saltygraphics.uikit.structure.SaltyColumn
import com.motionssalt.saltygraphics.uikit.structure.SaltyIconButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyJustify
import com.motionssalt.saltygraphics.uikit.structure.SaltyRow
import com.motionssalt.saltygraphics.uikit.structure.SaltySectionCard
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextStyle
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextTone
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextWeight
import com.motionssalt.saltygraphics.uikit.structure.SaltyToggleIconButton
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxWidth

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
    onClose: () -> Unit
) {
    var currentTransform by remember(transform) { mutableStateOf(transform) }
    var isTrackpadMode by remember { mutableStateOf(transform.isTrackpadMode) }

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

    SaltySectionCard {
        SaltyColumn(modifier = Modifier.saltyFillMaxWidth(), spacing = SaltySpace.MD) {
            SaltyRow(
                modifier = Modifier.saltyFillMaxWidth(),
                justify = SaltyJustify.SPACE_BETWEEN,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SaltyText(
                    text = "2D Transform",
                    style = SaltyTextStyle.TITLE_MEDIUM,
                    tone = SaltyTextTone.ACCENT,
                    weight = SaltyTextWeight.BOLD
                )
                SaltyIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    tone = SaltyTextTone.SECONDARY,
                    onClick = onClose
                )
            }

            // Scale Section (Linked / Unlinked Chain Toggle)
            SaltyColumn(spacing = SaltySpace.SM) {
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    justify = SaltyJustify.SPACE_BETWEEN,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaltyText(text = "Scale", style = SaltyTextStyle.BODY_LARGE)
                    SaltyToggleIconButton(
                        checked = currentTransform.isScaleLinked,
                        onCheckedChange = { linked ->
                            val updated = currentTransform.copy(isScaleLinked = linked)
                            currentTransform = updated
                            onTransformChange(updated)
                            onTransformCommit(updated)
                        },
                        checkedIcon = Icons.Default.Link,
                        uncheckedIcon = Icons.Default.LinkOff,
                        contentDescription = "Link Scale X/Y"
                    )
                }

                if (currentTransform.isScaleLinked) {
                    val uniformScale = (currentTransform.scaleX + currentTransform.scaleY) / 2f
                    SaltyNumberControl(
                        label = "Scale",
                        value = uniformScale * 100f,
                        min = 1f,
                        max = 10000f,
                        defaultValue = 100f,
                        step = 1f,
                        unit = "%",
                        onPreview = {
                            val targetScale = it / 100f
                            val snappedScale = if (snapContext?.snapEnabled == true && kotlin.math.abs(targetScale - 1f) <= 0.02f) 1f else targetScale
                            val updated = currentTransform.setScaleUniform(snappedScale)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onCommit = {
                            clearGuides()
                            onTransformCommit(currentTransform)
                        }
                    )
                } else {
                    SaltyNumberControl(
                        label = "Scale X",
                        value = currentTransform.scaleX * 100f,
                        min = 1f,
                        max = 10000f,
                        defaultValue = 100f,
                        step = 1f,
                        unit = "%",
                        onPreview = {
                            val targetScale = it / 100f
                            val snappedScale = if (snapContext?.snapEnabled == true && kotlin.math.abs(targetScale - 1f) <= 0.02f) 1f else targetScale
                            val updated = currentTransform.copy(scaleX = snappedScale)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onCommit = {
                            clearGuides()
                            onTransformCommit(currentTransform)
                        }
                    )
                    SaltyNumberControl(
                        label = "Scale Y",
                        value = currentTransform.scaleY * 100f,
                        min = 1f,
                        max = 10000f,
                        defaultValue = 100f,
                        step = 1f,
                        unit = "%",
                        onPreview = {
                            val targetScale = it / 100f
                            val snappedScale = if (snapContext?.snapEnabled == true && kotlin.math.abs(targetScale - 1f) <= 0.02f) 1f else targetScale
                            val updated = currentTransform.copy(scaleY = snappedScale)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onCommit = {
                            clearGuides()
                            onTransformCommit(currentTransform)
                        }
                    )
                }
            }

            // Move Section: Trackpad + Sensitivity OR Sliders
            SaltyColumn(spacing = SaltySpace.SM) {
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    justify = SaltyJustify.SPACE_BETWEEN,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaltyText(text = "Move", style = SaltyTextStyle.BODY_LARGE)
                    SaltyTextButton(
                        text = if (isTrackpadMode) "Switch to Sliders" else "Switch to Trackpad",
                        onClick = {
                            val newMode = !isTrackpadMode
                            isTrackpadMode = newMode
                            val updated = currentTransform.copy(isTrackpadMode = newMode)
                            currentTransform = updated
                            onTransformChange(updated)
                            onTransformCommit(updated)
                        }
                    )
                }

                if (isTrackpadMode) {
                    SaltyPositionTrackpad(
                        x = currentTransform.translationX,
                        y = currentTransform.translationY,
                        sensitivity = currentTransform.moveSensitivity,
                        onDelta = { dx, dy ->
                            val raw = currentTransform.copy(
                                translationX = currentTransform.translationX + dx,
                                translationY = currentTransform.translationY + dy
                            )
                            val snapped = applyTranslationSnapping(raw)
                            currentTransform = snapped
                            onTransformChange(snapped)
                        },
                        onDragEnd = {
                            clearGuides()
                            onTransformCommit(currentTransform)
                        },
                        onCommitX = {
                            val updated = currentTransform.copy(translationX = it)
                            currentTransform = updated
                            onTransformChange(updated)
                            onTransformCommit(updated)
                        },
                        onCommitY = {
                            val updated = currentTransform.copy(translationY = it)
                            currentTransform = updated
                            onTransformChange(updated)
                            onTransformCommit(updated)
                        },
                        range = -100000f..100000f
                    )

                    SaltyNumberControl(
                        label = "Sensitivity",
                        value = currentTransform.moveSensitivity * 100f,
                        min = 10f,
                        max = 500f,
                        defaultValue = 100f,
                        step = 5f,
                        unit = "%",
                        onPreview = {
                            val updated = currentTransform.copy(moveSensitivity = it / 100f)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onCommit = { onTransformCommit(currentTransform) }
                    )
                } else {
                    SaltyNumberControl(
                        label = "Move X",
                        value = currentTransform.translationX,
                        min = -100000f,
                        max = 100000f,
                        defaultValue = 0f,
                        step = 1f,
                        unit = "px",
                        onPreview = {
                            val raw = currentTransform.copy(translationX = it)
                            val snapped = applyTranslationSnapping(raw)
                            currentTransform = snapped
                            onTransformChange(snapped)
                        },
                        onCommit = {
                            clearGuides()
                            onTransformCommit(currentTransform)
                        }
                    )
                    SaltyNumberControl(
                        label = "Move Y",
                        value = currentTransform.translationY,
                        min = -100000f,
                        max = 100000f,
                        defaultValue = 0f,
                        step = 1f,
                        unit = "px",
                        onPreview = {
                            val raw = currentTransform.copy(translationY = it)
                            val snapped = applyTranslationSnapping(raw)
                            currentTransform = snapped
                            onTransformChange(snapped)
                        },
                        onCommit = {
                            clearGuides()
                            onTransformCommit(currentTransform)
                        }
                    )
                }
            }

            // Rotate Section
            SaltyColumn(spacing = SaltySpace.SM) {
                SaltyNumberControl(
                    label = "Rotate",
                    value = currentTransform.rotationDegrees,
                    min = -36000f,
                    max = 36000f,
                    defaultValue = 0f,
                    step = 1f,
                    unit = "°",
                    onPreview = {
                        val snappedAngle = applyRotationSnapping(it)
                        val updated = currentTransform.copy(rotationDegrees = snappedAngle)
                        currentTransform = updated
                        onTransformChange(updated)
                    },
                    onCommit = {
                        clearGuides()
                        onTransformCommit(currentTransform)
                    }
                )
            }
        }
    }
}
