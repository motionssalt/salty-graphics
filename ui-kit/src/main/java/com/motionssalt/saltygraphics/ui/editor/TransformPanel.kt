package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.engine.model.SelectionBounds
import com.motionssalt.saltygraphics.engine.model.SmartGuideLine
import com.motionssalt.saltygraphics.engine.model.SmartSnapEngine
import com.motionssalt.saltygraphics.engine.model.Transform2D
import com.motionssalt.saltygraphics.theme.*
import com.motionssalt.saltygraphics.ui.controls.RulerSlider
import com.motionssalt.saltygraphics.ui.controls.ValueEditorDialog

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
    val colors = MaterialTheme.colorScheme
    var currentTransform by remember(transform) { mutableStateOf(transform) }
    var isTrackpadMode by remember { mutableStateOf(transform.isTrackpadMode) }

    var isTrackpadXEditorOpen by remember { mutableStateOf(false) }
    var isTrackpadYEditorOpen by remember { mutableStateOf(false) }

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

    Surface(
        color = colors.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "2D Transform",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.primary
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.onSurfaceVariant)
                }
            }

            // Scale Section (Linked / Unlinked Chain Toggle)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Scale", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    FilledIconToggleButton(
                        checked = currentTransform.isScaleLinked,
                        onCheckedChange = { linked ->
                            val updated = currentTransform.copy(isScaleLinked = linked)
                            currentTransform = updated
                            onTransformChange(updated)
                            onTransformCommit(updated)
                        },
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            containerColor = colors.surfaceVariant,
                            checkedContainerColor = colors.primary,
                            contentColor = colors.onSurfaceVariant,
                            checkedContentColor = colors.background
                        )
                    ) {
                        Icon(
                            if (currentTransform.isScaleLinked) Icons.Default.Link else Icons.Default.LinkOff,
                            contentDescription = "Link Scale X/Y"
                        )
                    }
                }

                if (currentTransform.isScaleLinked) {
                    val uniformScale = (currentTransform.scaleX + currentTransform.scaleY) / 2f
                    RulerSlider(
                        label = "Scale",
                        value = uniformScale * 100f,
                        onValueChange = {
                            val targetScale = it / 100f
                            // Snap scale to 100% (1.0f) if within 2%
                            val snappedScale = if (snapContext?.snapEnabled == true && kotlin.math.abs(targetScale - 1f) <= 0.02f) 1f else targetScale
                            val updated = currentTransform.setScaleUniform(snappedScale)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = {
                            clearGuides()
                            onTransformCommit(currentTransform)
                        },
                        minValue = 1f,
                        maxValue = 10000f,
                        defaultValue = 100f,
                        step = 1f,
                        unit = "%"
                    )
                } else {
                    RulerSlider(
                        label = "Scale X",
                        value = currentTransform.scaleX * 100f,
                        onValueChange = {
                            val targetScale = it / 100f
                            val snappedScale = if (snapContext?.snapEnabled == true && kotlin.math.abs(targetScale - 1f) <= 0.02f) 1f else targetScale
                            val updated = currentTransform.copy(scaleX = snappedScale)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = {
                            clearGuides()
                            onTransformCommit(currentTransform)
                        },
                        minValue = 1f,
                        maxValue = 10000f,
                        defaultValue = 100f,
                        step = 1f,
                        unit = "%"
                    )
                    RulerSlider(
                        label = "Scale Y",
                        value = currentTransform.scaleY * 100f,
                        onValueChange = {
                            val targetScale = it / 100f
                            val snappedScale = if (snapContext?.snapEnabled == true && kotlin.math.abs(targetScale - 1f) <= 0.02f) 1f else targetScale
                            val updated = currentTransform.copy(scaleY = snappedScale)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = {
                            clearGuides()
                            onTransformCommit(currentTransform)
                        },
                        minValue = 1f,
                        maxValue = 10000f,
                        defaultValue = 100f,
                        step = 1f,
                        unit = "%"
                    )
                }
            }

            // Move Section: Trackpad + Sensitivity OR Sliders
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Move", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    TextButton(onClick = {
                        val newMode = !isTrackpadMode
                        isTrackpadMode = newMode
                        val updated = currentTransform.copy(isTrackpadMode = newMode)
                        currentTransform = updated
                        onTransformChange(updated)
                        onTransformCommit(updated)
                    }) {
                        Text(if (isTrackpadMode) "Switch to Sliders" else "Switch to Trackpad", color = colors.primary)
                    }
                }

                if (isTrackpadMode) {
                    // Precision Trackpad with smooth in-flight dragging and commit on release
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.background)
                            .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .pointerInput(currentTransform.moveSensitivity, snapContext) {
                                detectDragGestures(
                                    onDragEnd = {
                                        clearGuides()
                                        onTransformCommit(currentTransform)
                                    },
                                    onDragCancel = {
                                        clearGuides()
                                        onTransformCommit(currentTransform)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val raw = currentTransform.translateBy(dragAmount.x, dragAmount.y)
                                        val snapped = applyTranslationSnapping(raw)
                                        currentTransform = snapped
                                        onTransformChange(snapped)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = colors.onSurfaceVariant)
                            Text(
                                "Drag here for precision translation",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { isTrackpadXEditorOpen = true },
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.surfaceVariant,
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        text = "X: ${currentTransform.translationX.toInt()} px",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    onClick = { isTrackpadYEditorOpen = true },
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.surfaceVariant,
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        text = "Y: ${currentTransform.translationY.toInt()} px",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    RulerSlider(
                        label = "Sensitivity",
                        value = currentTransform.moveSensitivity * 100f,
                        onValueChange = {
                            val updated = currentTransform.copy(moveSensitivity = it / 100f)
                            currentTransform = updated
                            onTransformChange(updated)
                        },
                        onValueChangeFinished = { onTransformCommit(currentTransform) },
                        minValue = 10f,
                        maxValue = 500f,
                        defaultValue = 100f,
                        step = 5f,
                        unit = "%"
                    )
                } else {
                    RulerSlider(
                        label = "Move X",
                        value = currentTransform.translationX,
                        onValueChange = {
                            val raw = currentTransform.copy(translationX = it)
                            val snapped = applyTranslationSnapping(raw)
                            currentTransform = snapped
                            onTransformChange(snapped)
                        },
                        onValueChangeFinished = {
                            clearGuides()
                            onTransformCommit(currentTransform)
                        },
                        minValue = -100000f,
                        maxValue = 100000f,
                        defaultValue = 0f,
                        step = 1f,
                        unit = "px"
                    )
                    RulerSlider(
                        label = "Move Y",
                        value = currentTransform.translationY,
                        onValueChange = {
                            val raw = currentTransform.copy(translationY = it)
                            val snapped = applyTranslationSnapping(raw)
                            currentTransform = snapped
                            onTransformChange(snapped)
                        },
                        onValueChangeFinished = {
                            clearGuides()
                            onTransformCommit(currentTransform)
                        },
                        minValue = -100000f,
                        maxValue = 100000f,
                        defaultValue = 0f,
                        step = 1f,
                        unit = "px"
                    )
                }
            }

            // Rotate Section (around layer center on flat canvas)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RulerSlider(
                    label = "Rotate",
                    value = currentTransform.rotationDegrees,
                    onValueChange = {
                        val snappedAngle = applyRotationSnapping(it)
                        val updated = currentTransform.copy(rotationDegrees = snappedAngle)
                        currentTransform = updated
                        onTransformChange(updated)
                    },
                    onValueChangeFinished = {
                        clearGuides()
                        onTransformCommit(currentTransform)
                    },
                    minValue = -36000f,
                    maxValue = 36000f,
                    defaultValue = 0f,
                    step = 1f,
                    unit = "°"
                )
            }
        }
    }

    if (isTrackpadXEditorOpen) {
        ValueEditorDialog(
            label = "Move X",
            currentValue = currentTransform.translationX,
            minValue = -100000f,
            maxValue = 100000f,
            step = 1f,
            unit = "px",
            onValueApplied = {
                val updated = currentTransform.copy(translationX = it)
                currentTransform = updated
                onTransformChange(updated)
                onTransformCommit(updated)
            },
            onDismiss = { isTrackpadXEditorOpen = false }
        )
    }

    if (isTrackpadYEditorOpen) {
        ValueEditorDialog(
            label = "Move Y",
            currentValue = currentTransform.translationY,
            minValue = -100000f,
            maxValue = 100000f,
            step = 1f,
            unit = "px",
            onValueApplied = {
                val updated = currentTransform.copy(translationY = it)
                currentTransform = updated
                onTransformChange(updated)
                onTransformCommit(updated)
            },
            onDismiss = { isTrackpadYEditorOpen = false }
        )
    }
}
