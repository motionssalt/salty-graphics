package com.motionssalt.saltygraphics.uikit.controls

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.BlendMath
import com.motionssalt.saltygraphics.engine.render.FillRenderer
import com.motionssalt.saltygraphics.uikit.structure.*
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens
import kotlin.math.abs

/**
 * Shared Gradient stop preview + draggable stop rail rendered strictly inside the bottom panel.
 */
@Composable
fun SaltyGradientStopEditor(
    gradient: GradientData,
    selectedStopIndex: Int,
    onSelectStop: (Int) -> Unit,
    onGradientPreview: (GradientData) -> Unit,
    onGradientCommit: (String, GradientData) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SaltyTokens.colors
    val liveGradient by rememberUpdatedState(gradient)
    val selectStopCallback by rememberUpdatedState(onSelectStop)
    val previewCallback by rememberUpdatedState(onGradientPreview)
    val commitCallback by rememberUpdatedState(onGradientCommit)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(SaltyTokens.shapes.sm)
                .border(1.dp, colors.outlineVariant, SaltyTokens.shapes.sm)
                .testTag("gradient_preview")
        ) {
            drawIntoCanvas { c ->
                val bounds = RectF(0f, 0f, size.width, size.height)
                val paint = android.graphics.Paint().apply {
                    shader = FillRenderer.createGradientShader(gradient, bounds)
                    isDither = gradient.dither
                }
                c.nativeCanvas.drawRect(bounds, paint)
            }
        }

        if (gradient.type != GradientType.FREEFORM) {
            SaltyText("Color Stops (${gradient.stops.size})", style = SaltyTextStyle.LABEL_MEDIUM)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("gradient_stop_rail")
                    .pointerInput(Unit) {
                        var dragged = -1
                        var initial: GradientData? = null
                        var draft: GradientData? = null
                        var inFlight = false
                        try {
                            detectDragGestures(
                                onDragStart = { p ->
                                    initial = liveGradient
                                    draft = liveGradient
                                    dragged = liveGradient.stops.indices.minByOrNull {
                                        abs(liveGradient.stops[it].position - p.x / size.width.toFloat().coerceAtLeast(1f))
                                    } ?: -1
                                    inFlight = dragged >= 0
                                    if (dragged >= 0) selectStopCallback(dragged)
                                },
                                onDragEnd = {
                                    // Use the gesture's final value, not a possibly one-frame-old
                                    // recomposed gradient. Preview frames never add history nodes.
                                    val final = draft
                                    inFlight = false
                                    if (dragged >= 0 && final != null && final != initial) {
                                        commitCallback("Move gradient stop", final)
                                    }
                                    dragged = -1
                                    initial = null
                                    draft = null
                                },
                                onDragCancel = {
                                    val restore = initial
                                    inFlight = false
                                    dragged = -1
                                    initial = null
                                    draft = null
                                    if (restore != null) previewCallback(restore)
                                }
                            ) { change, _ ->
                                if (dragged >= 0) {
                                    val base = draft ?: liveGradient
                                    val updated = base.copy(stops = base.stops.mapIndexed { i, stop ->
                                        if (i == dragged) stop.copy(position =
                                            (change.position.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f))
                                        else stop
                                    })
                                    draft = updated
                                    previewCallback(updated)
                                    change.consume()
                                }
                            }
                        } finally {
                            // Closing the page or removing its target also discards a draft.
                            if (inFlight) initial?.let { previewCallback(it) }
                        }
                    }
            ) {
                drawLine(colors.outline, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 3f)
                gradient.stops.forEachIndexed { i, s ->
                    drawCircle(
                        color = if (i == selectedStopIndex) colors.primary else colors.outline,
                        radius = 12f,
                        center = Offset(s.position * size.width, size.height / 2f)
                    )
                    drawCircle(
                        color = Color(s.effectiveArgb.toInt()),
                        radius = 8f,
                        center = Offset(s.position * size.width, size.height / 2f)
                    )
                }
            }
        }
    }
}

/**
 * Project media picker grid embedded directly inside bottom-panel Fill/Media pages.
 */
@Composable
fun SaltyMediaPickerGrid(
    media: List<Pair<String, Bitmap?>>,
    onSelectMedia: (String) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.md)
    ) {
        SaltyButton(text = "Import from device", onClick = onImport)
        if (media.isEmpty()) {
            SaltyText("No images in this project yet", style = SaltyTextStyle.BODY_MEDIUM, tone = SaltyTextTone.SECONDARY)
        } else {
            media.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm)
                ) {
                    row.forEach { (path, bitmap) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SaltyTokens.shapes.sm)
                                .background(SaltyTokens.colors.surfaceVariant)
                                .border(1.dp, SaltyTokens.colors.outlineVariant, SaltyTokens.shapes.sm)
                                .clickable { onSelectMedia(path) }
                                .padding(SaltyTokens.spacing.xs)
                                .testTag("fill_media_$path"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (bitmap != null) {
                                SaltyBitmapImage(bitmap, contentDescription = path, modifier = Modifier.size(72.dp))
                            } else {
                                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                    SaltyText("Image", style = SaltyTextStyle.LABEL_SMALL, tone = SaltyTextTone.SECONDARY)
                                }
                            }
                            SaltyText(
                                text = path.substringAfterLast('/'),
                                style = SaltyTextStyle.LABEL_SMALL,
                                maxLines = 1,
                                ellipsis = true
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * Shared Blend thumbnail cache in :ui-kit using BlendMath from :engine.
 */
object SaltyBlendThumbnailCache {
    private val cache = LruCache<BlendMode, Bitmap>(40)

    fun getThumbnail(mode: BlendMode): Bitmap {
        cache.get(mode)?.let { return it }
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val topColor = 0xFF3366FFL
        val bottomColor = 0xFFFF6633L
        for (y in 0 until size) {
            val ratio = y.toFloat() / size
            val baseR = (((topColor shr 16 and 0xFF) * (1f - ratio) + (bottomColor shr 16 and 0xFF) * ratio)).toInt()
            val baseG = (((topColor shr 8 and 0xFF) * (1f - ratio) + (bottomColor shr 8 and 0xFF) * ratio)).toInt()
            val baseB = (((topColor and 0xFF) * (1f - ratio) + (bottomColor and 0xFF) * ratio)).toInt()
            val baseArgb = (0xFFL shl 24) or (baseR.toLong() shl 16) or (baseG.toLong() shl 8) or baseB.toLong()
            for (x in 0 until size) {
                val dx = x - size * 0.5f
                val dy = y - size * 0.5f
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                val pixelColor = if (dist <= size * 0.4f) {
                    BlendMath.blendColor(mode, baseArgb, 0xFFFFFF00L, opacity = 0.85f).toInt()
                } else {
                    baseArgb.toInt()
                }
                bitmap.setPixel(x, y, pixelColor)
            }
        }
        cache.put(mode, bitmap)
        return bitmap
    }
}

/**
 * Full Blending Mode thumbnail grid & Layer Mask configuration rendered inside bottom panel.
 */
@Composable
fun SaltyBlendAndMaskPanel(
    currentBlendMode: BlendMode,
    currentOpacity: Float,
    currentMaskType: MaskType,
    currentMaskSourceLayerId: String?,
    isMaskInverted: Boolean,
    currentMaskMode: MaskMode = MaskMode.NONE,
    allAvailableLayers: List<Layer>,
    maskPreviewBitmap: Bitmap?,
    onBlendChange: (BlendMode, Float) -> Unit,
    onMaskChange: (MaskType, String?, Boolean, MaskMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SaltyTokens.colors
    var selectedCategory by remember { mutableStateOf<BlendCategory?>(null) }
    var activeBlendMode by remember(currentBlendMode) { mutableStateOf(currentBlendMode) }
    var activeOpacity by remember(currentOpacity) { mutableFloatStateOf(currentOpacity) }
    var activeMaskType by remember(currentMaskType) { mutableStateOf(currentMaskType) }
    var activeSourceId by remember(currentMaskSourceLayerId, allAvailableLayers) {
        mutableStateOf(currentMaskSourceLayerId ?: allAvailableLayers.firstOrNull()?.id)
    }
    var activeInverted by remember(isMaskInverted) { mutableStateOf(isMaskInverted) }
    var isSourceDropdownOpen by remember { mutableStateOf(false) }

    val displayedModes = remember(selectedCategory) {
        if (selectedCategory == null) BlendMode.entries else BlendMode.byCategory(selectedCategory!!)
    }
    val selectedSourceLayer = remember(activeSourceId, allAvailableLayers) {
        allAvailableLayers.find { it.id == activeSourceId }
    }

    SaltyPanelPage(title = "Blending & Mask", onBack = onBack, modifier = modifier, scrollable = true) {
        SaltyProgressSlider(
            value = activeOpacity,
            onValueChange = {
                activeOpacity = it
                onBlendChange(activeBlendMode, it)
            },
            range = 0f..1f,
            label = "Opacity",
            unit = "%",
            defaultValue = 1f
        )

        SaltyDivider()

        SaltyText("Blend Mode Categories", style = SaltyTextStyle.LABEL_MEDIUM, tone = SaltyTextTone.SECONDARY)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)
        ) {
            val categories = listOf(null) + BlendCategory.entries
            items(categories) { cat ->
                SaltyChip(
                    label = cat?.displayName ?: "All",
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(85.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm)
        ) {
            items(displayedModes, key = { it.name }) { mode ->
                val isSelected = activeBlendMode == mode
                val thumb = remember(mode) { SaltyBlendThumbnailCache.getThumbnail(mode) }
                Surface(
                    shape = SaltyTokens.shapes.tile,
                    color = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surface,
                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) colors.primary else colors.outlineVariant),
                    modifier = Modifier
                        .clip(SaltyTokens.shapes.tile)
                        .clickable {
                            activeBlendMode = mode
                            onBlendChange(mode, activeOpacity)
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(SaltyTokens.spacing.xs),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = thumb.asImageBitmap(),
                            contentDescription = mode.displayName,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(SaltyTokens.shapes.sm)
                        )
                        Spacer(Modifier.height(SaltyTokens.spacing.xs))
                        SaltyText(
                            text = mode.displayName,
                            style = SaltyTextStyle.LABEL_SMALL,
                            tone = if (isSelected) SaltyTextTone.ACCENT else SaltyTextTone.PRIMARY,
                            weight = if (isSelected) SaltyTextWeight.BOLD else SaltyTextWeight.NORMAL,
                            maxLines = 1,
                            ellipsis = true,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        SaltyDivider()

        SaltyText("Layer Mask Source", style = SaltyTextStyle.TITLE_SMALL, tone = SaltyTextTone.ACCENT)
        SaltyText(
            "Select another layer to use as an alpha or luminance mask.",
            style = SaltyTextStyle.BODY_SMALL,
            tone = SaltyTextTone.SECONDARY
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)
        ) {
            MaskType.entries.forEach { type ->
                SaltyChip(
                    label = type.displayName,
                    selected = activeMaskType == type,
                    onClick = {
                        activeMaskType = type
                        onMaskChange(type, activeSourceId, activeInverted, currentMaskMode)
                    }
                )
            }
        }

        if (activeMaskType != MaskType.NONE) {
            Column(verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)) {
                SaltyText("Mask Source Layer:", style = SaltyTextStyle.LABEL_SMALL, tone = SaltyTextTone.SECONDARY)
                OutlinedButton(
                    onClick = { isSourceDropdownOpen = !isSourceDropdownOpen },
                    modifier = Modifier.fillMaxWidth(),
                    shape = SaltyTokens.shapes.button
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SaltyText(
                            selectedSourceLayer?.name ?: "Select a mask layer",
                            style = SaltyTextStyle.BODY_MEDIUM
                        )
                        Icon(
                            imageVector = if (isSourceDropdownOpen) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Select Layer",
                            tint = colors.textPrimary
                        )
                    }
                }

                if (isSourceDropdownOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceVariant, SaltyTokens.shapes.md)
                            .border(1.dp, colors.outlineVariant, SaltyTokens.shapes.md)
                            .padding(SaltyTokens.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xxs)
                    ) {
                        if (allAvailableLayers.isEmpty()) {
                            SaltyText(
                                "No other layers in project",
                                style = SaltyTextStyle.BODY_SMALL,
                                tone = SaltyTextTone.SECONDARY,
                                modifier = Modifier.padding(SaltyTokens.spacing.sm)
                            )
                        } else {
                            allAvailableLayers.forEach { layer ->
                                val isChosen = layer.id == activeSourceId
                                SaltyListItem(
                                    title = "${layer.name} (${layer.type.displayName})",
                                    isSelected = isChosen,
                                    onClick = {
                                        activeSourceId = layer.id
                                        isSourceDropdownOpen = false
                                        onMaskChange(activeMaskType, layer.id, activeInverted, currentMaskMode)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            SaltyRow(label = "Invert Mask") {
                SaltySwitch(
                    checked = activeInverted,
                    onCheckedChange = { checked ->
                        activeInverted = checked
                        onMaskChange(activeMaskType, activeSourceId, checked, currentMaskMode)
                    }
                )
            }

            if (maskPreviewBitmap != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceVariant.copy(alpha = 0.5f), SaltyTokens.shapes.md)
                        .padding(SaltyTokens.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.md)
                ) {
                    SaltyBitmapImage(
                        bitmap = maskPreviewBitmap,
                        contentDescription = "Mask Preview",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(SaltyTokens.shapes.sm)
                            .border(1.dp, colors.outlineVariant, SaltyTokens.shapes.sm)
                    )
                    Column {
                        SaltyText(
                            text = "Active Mask Preview (${activeMaskType.displayName}${if (activeInverted) " Inverted" else ""})",
                            style = SaltyTextStyle.LABEL_SMALL,
                            weight = SaltyTextWeight.BOLD
                        )
                        SaltyText(
                            text = "Source: ${selectedSourceLayer?.name ?: "None"}",
                            style = SaltyTextStyle.BODY_SMALL,
                            tone = SaltyTextTone.SECONDARY
                        )
                    }
                }
            }
        }
    }
}

/**
 * Aspect ratio preset visual tile using proportional indicator boxes.
 */
@Composable
fun SaltyAspectRatioTile(
    ratioWidth: Float,
    ratioHeight: Float,
    displayRatio: String,
    shortName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compactWidth: Boolean = false
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes
    val maxBox = 34f
    val rw = ratioWidth.coerceAtLeast(0.1f)
    val rh = ratioHeight.coerceAtLeast(0.1f)
    val (boxW, boxH) = if (rw >= rh) {
        maxBox to (maxBox * (rh / rw)).coerceIn(8f, maxBox)
    } else {
        (maxBox * (rw / rh)).coerceIn(8f, maxBox) to maxBox
    }

    Box(
        modifier = modifier
            .then(if (compactWidth) Modifier.width(82.dp) else Modifier)
            .clip(shapes.lg)
            .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colors.primary else colors.outline.copy(alpha = 0.5f),
                shape = shapes.lg
            )
            .clickable(onClick = onClick)
            .padding(vertical = SaltyTokens.spacing.md, horizontal = SaltyTokens.spacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = boxW.dp, height = boxH.dp)
                        .clip(shapes.xs)
                        .background(if (isSelected) colors.primary else colors.textPrimary.copy(alpha = 0.35f))
                )
            }
            Spacer(Modifier.height(SaltyTokens.spacing.xs))
            SaltyText(
                text = displayRatio,
                style = SaltyTextStyle.LABEL_MEDIUM,
                tone = if (isSelected) SaltyTextTone.ACCENT else SaltyTextTone.PRIMARY,
                weight = if (isSelected) SaltyTextWeight.BOLD else SaltyTextWeight.MEDIUM,
                maxLines = 1
            )
            SaltyText(
                text = shortName,
                style = SaltyTextStyle.LABEL_SMALL,
                tone = if (isSelected) SaltyTextTone.ACCENT else SaltyTextTone.SECONDARY,
                maxLines = 1
            )
        }
    }
}

/**
 * Font entry card used in bottom-panel Font Selection page.
 */
@Composable
fun SaltyFontItemCard(
    displayName: String,
    subtitle: String,
    fontFamily: FontFamily,
    isSelected: Boolean,
    badgeLabel: String?,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SaltyTokens.colors
    Surface(
        shape = SaltyTokens.shapes.md,
        color = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surface,
        border = BorderStroke(1.dp, if (isSelected) colors.primary else colors.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SaltyTokens.spacing.md, vertical = SaltyTokens.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SaltyText(
                    text = displayName,
                    style = SaltyTextStyle.BODY_LARGE,
                    fontFamily = fontFamily,
                    tone = if (isSelected) SaltyTextTone.ACCENT else SaltyTextTone.PRIMARY,
                    weight = if (isSelected) SaltyTextWeight.BOLD else SaltyTextWeight.NORMAL
                )
                SaltyText(subtitle, style = SaltyTextStyle.LABEL_SMALL, tone = SaltyTextTone.SECONDARY)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)) {
                if (!badgeLabel.isNullOrBlank()) {
                    SaltyBadge(badgeLabel, tone = SaltyTextTone.SECONDARY)
                }
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete font", tint = colors.error, modifier = Modifier.size(16.dp))
                    }
                }
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
