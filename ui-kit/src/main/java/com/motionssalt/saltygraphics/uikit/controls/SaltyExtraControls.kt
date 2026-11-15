package com.motionssalt.saltygraphics.uikit.controls

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.uikit.registry.LibraryComponentRegistry
import com.motionssalt.saltygraphics.uikit.registry.SaltyBuiltInLibraryRegistry
import com.motionssalt.saltygraphics.uikit.structure.*
import kotlin.math.abs

@Composable
fun SaltyToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SaltyRow(
        modifier = modifier.saltyFillMaxWidth(),
        padding = SaltySpace.XS,
        justify = SaltyJustify.SPACE_BETWEEN,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SaltyText(
            text = label,
            style = SaltyTextStyle.LABEL_MEDIUM
        )
        SaltySwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SaltyColorPickerRow(
    label: String,
    color: Long,
    onColorChange: (Long) -> Unit,
    allowTransparent: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    SaltyColumn(modifier = modifier, spacing = SaltySpace.XS) {
        SaltyColorSwatchRow(
            label = label,
            colorArgb = color,
            onClick = { expanded = !expanded }
        )
        SaltyAnimatedSection(visible = expanded) {
            SaltyCard(padding = SaltySpace.SM, spacing = SaltySpace.SM) {
                SaltyColorPicker(
                    colorArgb = color,
                    onColorChange = onColorChange,
                    supportsAlpha = true,
                    supportsTransparent = allowTransparent,
                    applyLabel = "Apply",
                    presets = SaltyRecentColorsRegistry.recentColors,
                    onColorCommit = {
                        onColorChange(it)
                        if (it != 0L) SaltyRecentColorsRegistry.recordColor(it)
                        expanded = false
                    },
                    onColorCancel = { expanded = false }
                )
            }
        }
    }
}

@Composable
fun SaltyFillPanelPage(
    fill: FillData,
    media: List<Pair<String, Bitmap?>>,
    onPreview: (FillData) -> Unit,
    onCommit: (String, FillData) -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit
) {
    var page by remember { mutableStateOf("fill") }
    var stopIndex by remember { mutableIntStateOf(0) }
    var pointIndex by remember { mutableIntStateOf(0) }
    val live by rememberUpdatedState(fill)
    val preview by rememberUpdatedState(onPreview)
    val commit by rememberUpdatedState(onCommit)

    fun back() {
        page = when (page) {
            "stopColor", "pointColor" -> "gradient"
            else -> "fill"
        }
    }

    BackHandler(page != "fill") { back() }

    fun stop(g: GradientData) = stopIndex.coerceIn(0, (g.stops.size - 1).coerceAtLeast(0))
    fun point(g: GradientData) = pointIndex.coerceIn(0, (g.freeformPoints.size - 1).coerceAtLeast(0))

    fun setStopColor(c: Long, save: Boolean) {
        val f = live
        val i = stop(f.gradient)
        val updated = f.copy(
            gradient = f.gradient.copy(
                stops = f.gradient.stops.mapIndexed { idx, s ->
                    if (idx == i) s.copy(color = c, opacity = 1f) else s
                }
            )
        )
        if (save) commit("Change gradient stop color", updated) else preview(updated)
    }

    fun setPointColor(c: Long, save: Boolean) {
        val f = live
        val i = point(f.gradient)
        val updated = f.copy(
            gradient = f.gradient.copy(
                freeformPoints = f.gradient.freeformPoints.mapIndexed { idx, p ->
                    if (idx == i) p.copy(color = c, opacity = 1f) else p
                }
            )
        )
        if (save) commit("Change freeform point color", updated) else preview(updated)
    }

    when (page) {
        "solidColor" -> SaltyColorPickerPage(
            title = "Fill Color",
            colorArgb = fill.effectiveSolidArgb,
            onPreview = { preview(live.copy(solidColor = it, solidOpacity = 1f)) },
            onCommit = {
                SaltyRecentColorsRegistry.recordColor(it)
                commit("Change fill color", live.copy(solidColor = it, solidOpacity = 1f))
            },
            onBack = { back() },
            presets = SaltyRecentColorsRegistry.recentColors,
            applyLabel = "Set hex"
        )
        "stopColor" -> SaltyColorPickerPage(
            title = "Gradient Stop Color",
            colorArgb = fill.gradient.stops.getOrNull(stop(fill.gradient))?.effectiveArgb ?: 0L,
            onPreview = { setStopColor(it, false) },
            onCommit = {
                SaltyRecentColorsRegistry.recordColor(it)
                setStopColor(it, true)
            },
            onBack = { back() },
            presets = SaltyRecentColorsRegistry.recentColors,
            applyLabel = "Set hex"
        )
        "pointColor" -> SaltyColorPickerPage(
            title = "Freeform Point Color",
            colorArgb = fill.gradient.freeformPoints.getOrNull(point(fill.gradient))?.effectiveArgb ?: 0L,
            onPreview = { setPointColor(it, false) },
            onCommit = {
                SaltyRecentColorsRegistry.recordColor(it)
                setPointColor(it, true)
            },
            onBack = { back() },
            presets = SaltyRecentColorsRegistry.recentColors,
            applyLabel = "Set hex"
        )
        "gradient" -> SaltyPanelPage(title = "Gradient Settings", onBack = { page = "fill" }) {
            val g = fill.gradient
            SaltySegmentedChips(
                options = GradientType.entries.map { it.displayName },
                selectedOption = g.type.displayName,
                onOptionSelected = { label ->
                    commit("Change gradient type", live.copy(gradient = live.gradient.copy(type = GradientType.entries.first { it.displayName == label })))
                },
                label = "Gradient type"
            )
            if (g.type != GradientType.FREEFORM) {
                SaltyGradientStopEditor(
                    gradient = g,
                    selectedStopIndex = stop(g),
                    onSelectStop = { stopIndex = it },
                    onGradientPreview = { preview(live.copy(gradient = it)) },
                    onGradientCommit = { label, updatedGrad -> commit(label, live.copy(gradient = updatedGrad)) }
                )
                SaltyRow(modifier = Modifier.saltyFillMaxWidth(), spacing = SaltySpace.SM) {
                    SaltyTextButton(text = "Add stop", onClick = {
                        val f = live
                        stopIndex = f.gradient.stops.size
                        commit("Add gradient stop", f.copy(gradient = f.gradient.copy(stops = f.gradient.stops + GradientStop(position = 0.5f))))
                    })
                    SaltyTextButton(
                        text = "Delete stop",
                        enabled = g.stops.size > 2,
                        onClick = {
                            val f = live
                            val i = stop(f.gradient)
                            stopIndex = (i - 1).coerceAtLeast(0)
                            commit("Delete gradient stop", f.copy(gradient = f.gradient.copy(stops = f.gradient.stops.filterIndexed { idx, _ -> idx != i })))
                        }
                    )
                }
                g.stops.getOrNull(stop(g))?.let { s ->
                    SaltyTextButton(text = "Stop color and alpha", onClick = { page = "stopColor" })
                    SaltyGradientNumber("Stop position", s.position, 0f, 1f, fill, onPreview, onCommit) { grad, v ->
                        grad.copy(stops = grad.stops.mapIndexed { i, it -> if (i == stop(grad)) it.copy(position = v) else it })
                    }
                    SaltyGradientNumber("Stop opacity", s.opacity, 0f, 1f, fill, onPreview, onCommit) { grad, v ->
                        grad.copy(stops = grad.stops.mapIndexed { i, it -> if (i == stop(grad)) it.copy(opacity = v) else it })
                    }
                    SaltyGradientNumber("Midpoint", s.midpoint, 0.01f, 0.99f, fill, onPreview, onCommit) { grad, v ->
                        grad.copy(stops = grad.stops.mapIndexed { i, it -> if (i == stop(grad)) it.copy(midpoint = v) else it })
                    }
                }
                SaltyRow(modifier = Modifier.saltyFillMaxWidth(), spacing = SaltySpace.SM) {
                    SaltyTextButton(text = "Reverse", onClick = { commit("Reverse gradient", live.copy(gradient = live.gradient.reversed())) })
                    SaltyTextButton(text = "Distribute evenly", onClick = { commit("Distribute gradient stops", live.copy(gradient = live.gradient.distributeEvenly())) })
                }
            } else {
                SaltyRow(modifier = Modifier.saltyFillMaxWidth(), spacing = SaltySpace.SM) {
                    SaltyTextButton(text = "Add point", onClick = {
                        val f = live
                        pointIndex = f.gradient.freeformPoints.size
                        commit("Add freeform point", f.copy(gradient = f.gradient.copy(freeformPoints = f.gradient.freeformPoints + FreeformPoint())))
                    })
                    SaltyTextButton(
                        text = "Delete point",
                        enabled = g.freeformPoints.size > 1,
                        onClick = {
                            val f = live
                            val i = point(f.gradient)
                            pointIndex = (i - 1).coerceAtLeast(0)
                            commit("Delete freeform point", f.copy(gradient = f.gradient.copy(freeformPoints = f.gradient.freeformPoints.filterIndexed { idx, _ -> idx != i })))
                        }
                    )
                }
                SaltyTextButton(text = "Point color and alpha", onClick = { page = "pointColor" })
                g.freeformPoints.getOrNull(point(g))?.let { p ->
                    listOf("Point X" to p.x, "Point Y" to p.y, "Point radius" to p.radius).forEach { (label, value) ->
                        SaltyGradientNumber(label, value, 0f, 1f, fill, onPreview, onCommit) { grad, v ->
                            grad.copy(freeformPoints = grad.freeformPoints.mapIndexed { i, it ->
                                if (i != point(grad)) it else when (label) {
                                    "Point X" -> it.copy(x = v)
                                    "Point Y" -> it.copy(y = v)
                                    else -> it.copy(radius = v.coerceAtLeast(0.01f))
                                }
                            })
                        }
                    }
                }
            }
            SaltyGradientNumber("Angle", g.angleDegrees, -360f, 360f, fill, onPreview, onCommit) { it, v -> it.copy(angleDegrees = v) }
            SaltyGradientNumber("Center X", g.centerX, 0f, 1f, fill, onPreview, onCommit) { it, v -> it.copy(centerX = v) }
            SaltyGradientNumber("Center Y", g.centerY, 0f, 1f, fill, onPreview, onCommit) { it, v -> it.copy(centerY = v) }
            SaltyGradientNumber("Radius", g.radius, 0.01f, 2f, fill, onPreview, onCommit) { it, v -> it.copy(radius = v) }
            SaltyGradientNumber("Aspect", g.aspect, 0.05f, 10f, fill, onPreview, onCommit) { it, v -> it.copy(aspect = v) }
            SaltySegmentedChips(
                options = GradientRepeatMode.entries.map { it.displayName },
                selectedOption = g.repeatMode.displayName,
                onOptionSelected = { label ->
                    commit("Change gradient repeat", live.copy(gradient = live.gradient.copy(repeatMode = GradientRepeatMode.entries.first { it.displayName == label })))
                },
                label = "Repeat"
            )
            SaltySegmentedChips(
                options = GradientInterpolation.entries.map { it.displayName },
                selectedOption = g.interpolation.displayName,
                onOptionSelected = { label ->
                    commit("Change gradient interpolation", live.copy(gradient = live.gradient.copy(interpolation = GradientInterpolation.entries.first { it.displayName == label })))
                },
                label = "Interpolation"
            )
            SaltyRow(label = "Dither") {
                SaltySwitch(
                    checked = g.dither,
                    onCheckedChange = { commit("Change gradient dithering", live.copy(gradient = live.gradient.copy(dither = it))) }
                )
            }
        }
        "media" -> SaltyPanelPage(title = "Project Images", onBack = { page = "fill" }) {
            SaltyMediaPickerGrid(
                media = media,
                onSelectMedia = { path ->
                    commit("Choose media fill", live.copy(type = FillType.MEDIA, mediaAssetPath = path))
                    page = "fill"
                },
                onImport = onImport
            )
        }
        else -> SaltyPanelPage(title = "Fill Settings", onBack = onBack) {
            SaltySegmentedChips(
                options = FillType.entries.map { it.displayName },
                selectedOption = fill.type.displayName,
                onOptionSelected = { label ->
                    commit("Change fill type", live.copy(type = FillType.entries.first { it.displayName == label }))
                },
                label = "Fill type"
            )
            when (fill.type) {
                FillType.SOLID -> {
                    SaltyTextButton(text = "Color and alpha", onClick = { page = "solidColor" })
                    SaltyTextButton(text = "Transparent", onClick = { commit("Make fill transparent", live.copy(solidOpacity = 0f)) })
                    SaltyNumberControl(
                        label = "Fill Opacity",
                        value = fill.solidOpacity * 100f,
                        min = 0f,
                        max = 100f,
                        unit = "%",
                        onPreview = { preview(live.copy(solidOpacity = it / 100f)) },
                        onCommit = { commit("Change fill opacity", live.copy(solidOpacity = it / 100f)) }
                    )
                }
                FillType.GRADIENT -> SaltyButton(text = "Edit Gradient", onClick = { page = "gradient" })
                FillType.MEDIA -> {
                    SaltyButton(
                        text = if (fill.mediaAssetPath == null) "Choose Media Image" else "Replace",
                        onClick = { page = "media" }
                    )
                    SaltySegmentedChips(
                        options = MediaScaleMode.entries.map { it.displayName },
                        selectedOption = fill.mediaScaleMode.displayName,
                        onOptionSelected = { label ->
                            commit("Change media fit", live.copy(mediaScaleMode = MediaScaleMode.entries.first { it.displayName == label }))
                        },
                        label = "Media Scale Mode"
                    )
                    listOf(
                        "Media scale" to fill.mediaScale,
                        "Media offset X" to fill.mediaOffsetX,
                        "Media offset Y" to fill.mediaOffsetY,
                        "Media rotation" to fill.mediaRotation
                    ).forEach { (label, value) ->
                        val range = when (label) {
                            "Media scale" -> 0.05f..20f
                            "Media rotation" -> -360f..360f
                            else -> -2f..2f
                        }
                        fun set(v: Float) = when (label) {
                            "Media scale" -> live.copy(mediaScale = v)
                            "Media offset X" -> live.copy(mediaOffsetX = v)
                            "Media offset Y" -> live.copy(mediaOffsetY = v)
                            else -> live.copy(mediaRotation = v)
                        }
                        SaltyNumberControl(
                            label = label,
                            value = value,
                            min = range.start,
                            max = range.endInclusive,
                            step = 0.01f,
                            onPreview = { preview(set(it)) },
                            onCommit = { commit("Change ${label.lowercase()}", set(it)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaltyGradientNumber(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    fill: FillData,
    preview: (FillData) -> Unit,
    commit: (String, FillData) -> Unit,
    change: (GradientData, Float) -> GradientData
) {
    val live by rememberUpdatedState(fill)
    SaltyNumberControl(
        label = label,
        value = value,
        min = min,
        max = max,
        step = if (max > 100f) 1f else 0.01f,
        onPreview = { preview(live.copy(gradient = change(live.gradient, it))) },
        onCommit = { commit("Change gradient ${label.lowercase()}", live.copy(gradient = change(live.gradient, it))) }
    )
}

@Composable
fun SaltyTransformPanel(
    transform: Transform2D,
    snapEnabled: Boolean,
    applyTranslationSnapping: (Transform2D) -> Transform2D,
    applyRotationSnapping: (Float) -> Float,
    clearGuides: () -> Unit,
    onTransformChange: (Transform2D) -> Unit,
    onTransformCommit: (Transform2D) -> Unit,
    onClose: () -> Unit,
    wrapInPanelPage: Boolean = false
) {
    SaltyBuiltInLibraryRegistry.ensureRegistered()
    var currentTransform by remember(transform) { mutableStateOf(transform) }
    var isTrackpadMode by remember { mutableStateOf(transform.isTrackpadMode) }

    val content: @Composable () -> Unit = {
        SaltySectionCard {
            SaltyColumn(modifier = Modifier.saltyFillMaxWidth(), spacing = SaltySpace.MD) {
                if (!wrapInPanelPage) {
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
                            onClick = {
                                clearGuides()
                                onClose()
                            }
                        )
                    }
                }

                // Scale Section
                SaltyColumn(spacing = SaltySpace.SM) {
                    SaltyRow(
                        modifier = Modifier.saltyFillMaxWidth(),
                        justify = SaltyJustify.SPACE_BETWEEN,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SaltyText(text = "Scale", style = SaltyTextStyle.BODY_LARGE)
                        SaltyToggleIconButton(
                            checked = currentTransform.linkScale,
                            icon = if (currentTransform.linkScale) Icons.Default.Link else Icons.Default.LinkOff,
                            contentDescription = if (currentTransform.linkScale) "Linked Scale" else "Unlinked Scale",
                            onClick = {
                                val updated = currentTransform.copy(linkScale = !currentTransform.linkScale)
                                currentTransform = updated
                                onTransformChange(updated)
                                onTransformCommit(updated)
                            }
                        )
                    }

                    if (currentTransform.linkScale) {
                        SaltyNumberControl(
                            label = "Scale",
                            value = currentTransform.scaleX * 100f,
                            min = 1f,
                            max = 10000f,
                            defaultValue = 100f,
                            step = 1f,
                            unit = "%",
                            onPreview = {
                                val targetScale = it / 100f
                                val snappedScale = if (snapEnabled && abs(targetScale - 1f) <= 0.02f) 1f else targetScale
                                val ratio = if (currentTransform.scaleX != 0f) currentTransform.scaleY / currentTransform.scaleX else 1f
                                val updated = currentTransform.copy(scaleX = snappedScale, scaleY = snappedScale * ratio)
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
                                val snappedScale = if (snapEnabled && abs(targetScale - 1f) <= 0.02f) 1f else targetScale
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
                                val snappedScale = if (snapEnabled && abs(targetScale - 1f) <= 0.02f) 1f else targetScale
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

    if (wrapInPanelPage) {
        SaltyPanelPage(
            title = "Transform",
            onBack = {
                clearGuides()
                onClose()
            }
        ) {
            content()
        }
    } else {
        content()
    }
}
