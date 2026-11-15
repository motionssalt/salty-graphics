package com.motionssalt.saltygraphics.uikit.structure

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.action.GlobalClipboard
import com.motionssalt.saltygraphics.engine.action.PasteMode
import com.motionssalt.saltygraphics.engine.action.SelectivePasteProperties
import com.motionssalt.saltygraphics.engine.file.ProjectLoadState
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.history.HistoryGeometry
import com.motionssalt.saltygraphics.history.HistoryGraphCalculator
import com.motionssalt.saltygraphics.history.HistoryViewport
import com.motionssalt.saltygraphics.uikit.controls.SaltyTextField
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Responsive sizing rules for the editor workspace.
 * Sizes are calculated from usable space after system insets and the top toolbar.
 */
object PanelSizing {
    fun sideDock(width: Float, height: Float): Boolean = width >= 840f || width > height
    fun bottomHeight(height: Float): Float = (height * 0.40f).coerceIn(260f, 340f).coerceAtMost(height * 0.45f)
    fun sideWidth(width: Float): Float = (width * 0.40f).coerceIn(280f, 360f).coerceAtMost(width * 0.45f)
}

/**
 * Responsive workspace shell placing the canvas preview above/left and the docked panel below/right.
 */
@Composable
fun SaltyWorkspace(
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
    panel: @Composable () -> Unit
) {
    val colors = SaltyTokens.colors
    BoxWithConstraints(modifier.fillMaxSize()) {
        val workspaceWidth = maxWidth.value
        val workspaceHeight = maxHeight.value
        if (PanelSizing.sideDock(workspaceWidth, workspaceHeight)) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight().testTag("preview")) { preview() }
                Surface(
                    modifier = Modifier
                        .width(PanelSizing.sideWidth(workspaceWidth).dp)
                        .fillMaxHeight()
                        .testTag("tools"),
                    color = colors.surface
                ) { panel() }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().testTag("preview")) { preview() }
                Surface(
                    modifier = Modifier
                        .height(PanelSizing.bottomHeight(workspaceHeight).dp)
                        .fillMaxWidth()
                        .testTag("tools"),
                    color = colors.surface
                ) { panel() }
            }
        }
    }
}

/**
 * Project load progress and error recovery surface.
 */
@Composable
fun SaltyLoadingSurface(
    state: ProjectLoadState,
    onRetry: () -> Unit,
    onSafeMode: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("project_load_surface"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(SaltyTokens.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.lg)
        ) {
            when (state) {
                is ProjectLoadState.Error -> {
                    SaltyText("Could not open project", style = SaltyTextStyle.TITLE_MEDIUM, weight = SaltyTextWeight.BOLD)
                    SaltyText(state.message, style = SaltyTextStyle.BODY_MEDIUM, tone = SaltyTextTone.SECONDARY)
                    SaltyButton(text = "Retry", onClick = onRetry)
                    SaltyOutlinedButton(text = "Open in safe mode", onClick = onSafeMode)
                    SaltyTextButton(text = "Back", onClick = onBack, tone = SaltyTextTone.SECONDARY)
                }
                is ProjectLoadState.Loading -> {
                    SaltyProgressBar(progress = state.step.toFloat() / state.total.coerceAtLeast(1))
                    SaltyText("Opening project: ${state.phase}", style = SaltyTextStyle.BODY_MEDIUM)
                    SaltyText("Step ${state.step} of ${state.total}", style = SaltyTextStyle.LABEL_MEDIUM, tone = SaltyTextTone.SECONDARY)
                    SaltyTextButton(text = "Back", onClick = onBack, tone = SaltyTextTone.SECONDARY)
                }
                is ProjectLoadState.Loaded -> {
                    SaltyText("Project ready", style = SaltyTextStyle.BODY_MEDIUM)
                }
            }
        }
    }
}

/**
 * Project gallery card used on the Home screen.
 * Uses tap/long-press selection indicator badge without any Checkbox.
 */
@Composable
fun SaltyProjectCard(
    name: String,
    width: Int,
    height: Int,
    layerCount: Int,
    thumbnailBitmap: Bitmap?,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes
    SaltyCard(
        modifier = modifier,
        isSelected = isSelected,
        padding = SaltySpace.MD,
        spacing = SaltySpace.SM,
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(shapes.md)
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnailBitmap != null) {
                SaltyBitmapImage(
                    bitmap = thumbnailBitmap,
                    contentDescription = "Project Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    fitCenter = true
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SaltyIcon(
                        icon = Icons.Default.Image,
                        tone = if (isSelected) SaltyTextTone.ACCENT else SaltyTextTone.SECONDARY,
                        size = SaltyIconSize.XL
                    )
                    Spacer(Modifier.height(SaltyTokens.spacing.xs))
                    SaltyText(
                        text = "$width x $height",
                        style = SaltyTextStyle.LABEL_SMALL,
                        tone = SaltyTextTone.SECONDARY
                    )
                }
            }

            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(SaltyTokens.spacing.sm)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primary else colors.surface.copy(alpha = 0.85f))
                        .border(1.5.dp, if (isSelected) colors.primary else colors.outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        SaltyText(
            text = name,
            style = SaltyTextStyle.TITLE_MEDIUM,
            weight = SaltyTextWeight.MEDIUM,
            maxLines = 1,
            ellipsis = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val layerCountText = when (layerCount) {
                0 -> "No layers"
                1 -> "1 layer"
                else -> "$layerCount layers"
            }
            SaltyText(layerCountText, style = SaltyTextStyle.BODY_SMALL, tone = SaltyTextTone.SECONDARY)
            SaltyText("${width}x${height}", style = SaltyTextStyle.LABEL_SMALL, tone = SaltyTextTone.SECONDARY)
        }
    }
}

/**
 * Drag-to-reorder layer list preserving pointer ownership at the container level.
 */
@Composable
fun SaltyReorderableLayerList(
    layers: List<Layer>,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (Layer, Boolean) -> Unit
) {
    val colors = SaltyTokens.colors
    val state = rememberLazyListState()
    val currentLayers by rememberUpdatedState(layers)
    val currentMove by rememberUpdatedState(onMove)
    var draggedId by remember { mutableStateOf<String?>(null) }
    var pointerY by remember { mutableFloatStateOf(0f) }
    var target by remember { mutableIntStateOf(-1) }
    val edge = with(LocalDensity.current) { 48.dp.toPx() }
    val maxSpeed = with(LocalDensity.current) { 14.dp.toPx() }

    fun updateTarget() {
        target = state.layoutInfo.visibleItemsInfo.minByOrNull {
            abs(pointerY - (it.offset + it.size / 2f))
        }?.index ?: -1
    }

    fun finish(commit: Boolean) {
        val from = currentLayers.indexOfFirst { it.id == draggedId }
        val to = target
        draggedId = null
        target = -1
        if (commit && from >= 0 && to in currentLayers.indices && from != to) currentMove(from, to)
    }

    LaunchedEffect(draggedId) {
        if (draggedId != null) {
            while (isActive) {
                val height = state.layoutInfo.viewportSize.height.toFloat()
                val delta = when {
                    pointerY < edge -> -maxSpeed * ((edge - pointerY) / edge).coerceIn(0f, 1f)
                    pointerY > height - edge -> maxSpeed * ((pointerY - height + edge) / edge).coerceIn(0f, 1f)
                    else -> 0f
                }
                if (delta != 0f) {
                    state.scrollBy(delta)
                    updateTarget()
                }
                delay(16)
            }
        }
    }

    Box(
        modifier
            .testTag("layer_list")
            .semantics {
                stateDescription = draggedId?.let { "Reordering $it to ${target + 1}" } ?: "Layer order"
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val handleZonePx = 48.dp.toPx()
                    if (down.position.x < size.width - handleZonePx) return@awaitEachGesture
                    val contentTopPx = SaltyTokens.spacing.xs.toPx()
                    val listY = down.position.y - contentTopPx
                    val item = state.layoutInfo.visibleItemsInfo.find {
                        listY >= it.offset && listY < it.offset + it.size
                    } ?: return@awaitEachGesture
                    down.consume()
                    val earlyEnd = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pointer = event.changes.find { it.id == down.id }
                            if (pointer == null || !pointer.pressed || event.changes.count { it.pressed } > 1 ||
                                (pointer.position - down.position).getDistance() > viewConfiguration.touchSlop
                            ) break
                        }
                        true
                    }
                    if (earlyEnd != null) return@awaitEachGesture
                    draggedId = currentLayers.getOrNull(item.index)?.id
                    pointerY = down.position.y
                    target = item.index
                    try {
                        while (draggedId != null) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pointer = event.changes.find { it.id == down.id }
                            val cancelled = pointer?.isConsumed == true
                            event.changes.forEach { it.consume() }
                            if (pointer == null || event.changes.count { it.pressed } > 1 || cancelled) break
                            pointerY = pointer.position.y
                            updateTarget()
                            if (!pointer.pressed) {
                                finish(true)
                                break
                            }
                        }
                    } finally {
                        finish(false)
                    }
                }
            }
    ) {
        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .testTag("layer_scroll"),
            contentPadding = PaddingValues(horizontal = SaltyTokens.spacing.sm, vertical = SaltyTokens.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)
        ) {
            itemsIndexed(layers, key = { _, layer -> layer.id }) { index, layer ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .testTag("layer_row_${layer.id}")
                        .alpha(if (draggedId == layer.id) 0.45f else 1f)
                ) {
                    row(layer, draggedId == layer.id)
                    if (draggedId != null && target == index) {
                        val from = layers.indexOfFirst { it.id == draggedId }
                        Box(
                            Modifier
                                .align(if (target > from) Alignment.BottomCenter else Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(colors.primary)
                                .testTag("layer_drop_indicator")
                        )
                    }
                }
            }
        }
        if (draggedId != null) {
            val layer = layers.find { it.id == draggedId }
            if (layer != null) {
                Surface(
                    color = colors.surfaceVariant,
                    shape = SaltyTokens.shapes.sm,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset(8, pointerY.toInt() - 20) }
                        .testTag("layer_drag_feedback")
                ) {
                    SaltyText(
                        text = layer.name,
                        style = SaltyTextStyle.LABEL_MEDIUM,
                        modifier = Modifier.padding(SaltyTokens.spacing.sm)
                    )
                }
            }
        }
    }
}

/**
 * Full panel-native Layers Browser.
 * Holds zero dialogs and zero Checkboxes (uses hold-to-select + inline rename/paste pages).
 */
@Composable
fun SaltyLayersPanel(
    layers: List<Layer>,
    selectedLayerIds: Set<String>,
    onSelectLayer: (String) -> Unit,
    onToggleSelectMulti: (String) -> Unit,
    onSelectAll: () -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDeleteLayers: (Set<String>) -> Unit,
    onRenameLayer: (String, String) -> Unit,
    onCopyLayer: (Layer) -> Unit,
    onPasteProperties: (PasteMode) -> Unit,
    onMoveLayer: (fromIndex: Int, toIndex: Int) -> Unit,
    onEnterGroup: (String) -> Unit,
    onClosePanel: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectNone: () -> Unit = {},
    onDuplicateLayer: ((String) -> Unit)? = null,
    onGroupSelected: (() -> Unit)? = null,
    onUngroupSelected: (() -> Unit)? = null,
    thumbnailContent: @Composable (Layer, Boolean) -> Unit = { layer, isSelected ->
        val fallback = when (layer.type) {
            LayerType.MEDIA -> Icons.Default.Image
            LayerType.TEXT -> Icons.Default.TextFields
            LayerType.ADJUSTMENT -> Icons.Default.Tune
            LayerType.SHAPE -> Icons.Default.Category
            LayerType.VECTOR -> Icons.Default.Draw
            LayerType.GROUP -> Icons.Default.Folder
            LayerType.QUARANTINED -> Icons.Default.Warning
        }
        SaltyIcon(fallback, tone = if (isSelected) SaltyTextTone.ACCENT else SaltyTextTone.SECONDARY, size = SaltyIconSize.MD)
    }
) {
    val colors = SaltyTokens.colors
    var subPage by remember { mutableStateOf("list") } // "list" | "rename" | "paste"
    var layerToRename by remember { mutableStateOf<Layer?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var multiSelectMode by remember { mutableStateOf(false) }
    LaunchedEffect(selectedLayerIds) {
        if (selectedLayerIds.isEmpty()) multiSelectMode = false
    }

    Surface(
        color = colors.surface,
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
    ) {
        when (subPage) {
            "rename" -> {
                val target = layerToRename
                SaltyPanelPage(
                    title = "Rename Layer",
                    onBack = { subPage = "list"; layerToRename = null }
                ) {
                    SaltyTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = "Layer Name"
                    )
                    SaltyRow(justify = SaltyJustify.END, spacing = SaltySpace.SM, modifier = Modifier.fillMaxWidth()) {
                        SaltyTextButton("Cancel", onClick = { subPage = "list"; layerToRename = null }, tone = SaltyTextTone.SECONDARY)
                        SaltyButton("Rename", onClick = {
                            if (target != null && renameInput.isNotBlank()) {
                                onRenameLayer(target.id, renameInput.trim())
                            }
                            subPage = "list"
                            layerToRename = null
                        })
                    }
                }
            }
            "paste" -> {
                val source = GlobalClipboard.copiedLayer
                var pasteTransform by remember { mutableStateOf(true) }
                var pasteEffects by remember { mutableStateOf(true) }
                var pasteFill by remember { mutableStateOf(true) }
                var pasteStroke by remember { mutableStateOf(true) }
                var pasteShadow by remember { mutableStateOf(true) }
                var pasteBlend by remember { mutableStateOf(true) }
                var pasteOpacity by remember { mutableStateOf(true) }
                var pasteMask by remember { mutableStateOf(true) }

                SaltyPanelPage(
                    title = "Paste Layer Properties",
                    onBack = { subPage = "list" }
                ) {
                    SaltyText(
                        text = "Select properties from '${source?.name ?: "Copied Layer"}':",
                        style = SaltyTextStyle.BODY_SMALL,
                        tone = SaltyTextTone.SECONDARY
                    )
                    val items = listOf(
                        Triple("Transform (Position, Scale, Rotation)", pasteTransform) { v: Boolean -> pasteTransform = v },
                        Triple("Effects Stack", pasteEffects) { v: Boolean -> pasteEffects = v },
                        Triple("Fill (Color, Gradient, Media)", pasteFill) { v: Boolean -> pasteFill = v },
                        Triple("Stroke / Border", pasteStroke) { v: Boolean -> pasteStroke = v },
                        Triple("Drop Shadow", pasteShadow) { v: Boolean -> pasteShadow = v },
                        Triple("Blend Mode", pasteBlend) { v: Boolean -> pasteBlend = v },
                        Triple("Opacity", pasteOpacity) { v: Boolean -> pasteOpacity = v },
                        Triple("Layer Mask", pasteMask) { v: Boolean -> pasteMask = v }
                    )
                    items.forEach { (label, checked, onChange) ->
                        SaltyListItem(
                            title = label,
                            isSelected = checked,
                            onClick = { onChange(!checked) },
                            trailingAction = {
                                if (checked) {
                                    SaltyIcon(Icons.Default.Check, tone = SaltyTextTone.ACCENT, size = SaltyIconSize.SM)
                                }
                            }
                        )
                    }
                    SaltyRow(justify = SaltyJustify.END, spacing = SaltySpace.SM, modifier = Modifier.fillMaxWidth()) {
                        SaltyTextButton("Cancel", onClick = { subPage = "list" }, tone = SaltyTextTone.SECONDARY)
                        SaltyButton("Paste Selected", onClick = {
                            @Suppress("UNUSED_VARIABLE")
                            val props = SelectivePasteProperties(
                                transform = pasteTransform,
                                effects = pasteEffects,
                                fill = pasteFill,
                                stroke = pasteStroke,
                                shadow = pasteShadow,
                                blendMode = pasteBlend,
                                opacity = pasteOpacity,
                                mask = pasteMask
                            )
                            onPasteProperties(PasteMode.SELECTIVE)
                            subPage = "list"
                        })
                    }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SaltyTokens.spacing.lg, vertical = SaltyTokens.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SaltyText(
                            text = "Layers (${layers.size})",
                            style = SaltyTextStyle.TITLE_MEDIUM,
                            tone = SaltyTextTone.ACCENT,
                            weight = SaltyTextWeight.BOLD
                        )
                        IconButton(onClick = onClosePanel) {
                            Icon(Icons.Default.Close, contentDescription = "Close Layers", tint = colors.textSecondary)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceVariant)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = SaltyTokens.spacing.sm, vertical = SaltyTokens.spacing.xs),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isAllSelected = layers.isNotEmpty() && selectedLayerIds.size >= layers.size
                        IconButton(
                            onClick = { if (isAllSelected) onSelectNone() else onSelectAll() },
                            modifier = Modifier.testTag("select_all_toggle")
                        ) {
                            Icon(
                                imageVector = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (isAllSelected) "Select None" else "Select All",
                                tint = if (isAllSelected) colors.primary else colors.textPrimary
                            )
                        }

                        IconButton(
                            onClick = {
                                val firstId = selectedLayerIds.firstOrNull()
                                val layer = layers.find { it.id == firstId }
                                if (layer != null) onCopyLayer(layer)
                            },
                            enabled = selectedLayerIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy Layer",
                                tint = if (selectedLayerIds.isNotEmpty()) colors.primary else colors.textDisabled
                            )
                        }

                        IconButton(
                            onClick = {
                                val firstId = selectedLayerIds.firstOrNull()
                                if (firstId != null) onDuplicateLayer?.invoke(firstId)
                            },
                            enabled = selectedLayerIds.isNotEmpty() && onDuplicateLayer != null
                        ) {
                            Icon(
                                Icons.Default.ControlPointDuplicate,
                                contentDescription = "Duplicate Layer",
                                tint = if (selectedLayerIds.isNotEmpty()) colors.primary else colors.textDisabled
                            )
                        }

                        if (selectedLayerIds.size == 1) {
                            val singleLayer = layers.find { it.id == selectedLayerIds.first() }
                            if (singleLayer != null) {
                                IconButton(onClick = {
                                    layerToRename = singleLayer
                                    renameInput = singleLayer.name
                                    subPage = "rename"
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename Layer", tint = colors.primary)
                                }
                            }
                        }

                        if (selectedLayerIds.size > 1 && onGroupSelected != null) {
                            IconButton(onClick = onGroupSelected) {
                                Icon(Icons.Default.Folder, contentDescription = "Group Layers", tint = colors.primary)
                            }
                        } else if (selectedLayerIds.size == 1 &&
                            layers.find { it.id == selectedLayerIds.first() }?.type == LayerType.GROUP &&
                            onUngroupSelected != null
                        ) {
                            IconButton(onClick = onUngroupSelected) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Ungroup Layers", tint = colors.primary)
                            }
                        }

                        IconButton(
                            onClick = { subPage = "paste" },
                            enabled = GlobalClipboard.hasContent() && selectedLayerIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = "Paste Properties",
                                tint = if (GlobalClipboard.hasContent()) colors.primary else colors.textDisabled
                            )
                        }

                        IconButton(
                            onClick = { onDeleteLayers(selectedLayerIds) },
                            enabled = selectedLayerIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = if (selectedLayerIds.isNotEmpty()) colors.error else colors.textDisabled
                            )
                        }
                    }

                    SaltyReorderableLayerList(
                        layers = layers,
                        onMove = onMoveLayer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { layer, dragging ->
                        val fallbackIcon = when (layer.type) {
                            LayerType.MEDIA -> Icons.Default.Image
                            LayerType.TEXT -> Icons.Default.TextFields
                            LayerType.ADJUSTMENT -> Icons.Default.Tune
                            LayerType.SHAPE -> Icons.Default.Category
                            LayerType.VECTOR -> Icons.Default.Draw
                            LayerType.GROUP -> Icons.Default.Folder
                            LayerType.QUARANTINED -> Icons.Default.Warning
                        }
                        SaltyLayerRow(
                            id = layer.id,
                            name = layer.name,
                            subtitle = "${layer.type.displayName} • ${layer.blendMode.displayName}",
                            isSelected = selectedLayerIds.contains(layer.id) || dragging,
                            isVisible = layer.isVisible,
                            isLocked = layer.isLocked,
                            showGroupButton = layer.type == LayerType.GROUP,
                            fallbackIcon = fallbackIcon,
                            thumbnail = { thumbnailContent(layer, selectedLayerIds.contains(layer.id) || dragging) },
                            onVisibilityToggle = { onToggleVisibility(layer.id) },
                            onLockToggle = { onToggleLock(layer.id) },
                            onEnterGroup = { onEnterGroup(layer.id) },
                            onRowClick = {
                                if (multiSelectMode || selectedLayerIds.size > 1) onToggleSelectMulti(layer.id)
                                else onSelectLayer(layer.id)
                            },
                            onRowLongClick = {
                                multiSelectMode = true
                                onToggleSelectMulti(layer.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Panel-hosted Action History Tree viewport. Never opens a Dialog or overlays the canvas.
 */
@Composable
fun SaltyHistoryTree(
    actionTree: ActionTree,
    onJumpToNode: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SaltyTokens.colors
    val density = LocalDensity.current
    val snapshot = actionTree.toSnapshot()
    val graph = remember(snapshot) {
        HistoryGraphCalculator.computeGraph(snapshot.nodes, snapshot.rootNodeId, snapshot.currentNodeId)
    }
    var viewport by remember { mutableStateOf(HistoryViewport()) }
    val current = graph.nodes.firstOrNull { it.isCurrent }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SaltyTokens.spacing.md)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountTree, null, tint = colors.primary)
                SaltyText(
                    text = "Action History Tree",
                    style = SaltyTextStyle.TITLE_MEDIUM,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = SaltyTokens.spacing.sm)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close history", tint = colors.textSecondary)
                }
            }
            SaltyText(
                text = "Active path highlighted; faded branches are retained. Tap a node to continue there.",
                style = SaltyTextStyle.BODY_SMALL,
                tone = SaltyTextTone.SECONDARY
            )
            SaltyText(
                text = "Actions: ${graph.nodes.size}",
                style = SaltyTextStyle.LABEL_SMALL,
                tone = SaltyTextTone.SECONDARY
            )

            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val width = maxWidth.value
                val height = maxHeight.value
                fun centered() = current?.let {
                    viewport.centered(HistoryGeometry.x(it) + 100f, HistoryGeometry.y(it) + 24f, width, height)
                } ?: viewport

                fun fit(): HistoryViewport {
                    val right = (graph.nodes.maxOfOrNull { HistoryGeometry.x(it) + HistoryGeometry.NODE_WIDTH } ?: 200f) + 24f
                    val bottom = (graph.nodes.maxOfOrNull { HistoryGeometry.y(it) + HistoryGeometry.NODE_HEIGHT } ?: 48f) + 24f
                    val scale = minOf(width / right, (height - 52f) / bottom, 1f).coerceIn(0.25f, 1f)
                    return HistoryViewport(scale).centered(right / 2f, bottom / 2f, width, height - 52f)
                }

                LaunchedEffect(width, height, snapshot.currentNodeId) { viewport = centered() }

                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { viewport = viewport.zoomAt(0.8f, width / 2, height / 2) }) {
                            Icon(Icons.Default.ZoomOut, "Zoom out history", tint = colors.textPrimary)
                        }
                        SaltyText("${(viewport.scale * 100).roundToInt()}%", style = SaltyTextStyle.LABEL_SMALL)
                        IconButton(onClick = { viewport = viewport.zoomAt(1.25f, width / 2, height / 2) }) {
                            Icon(Icons.Default.ZoomIn, "Zoom in history", tint = colors.textPrimary)
                        }
                        IconButton(onClick = { viewport = centered() }) {
                            Icon(Icons.Default.MyLocation, "Current action", tint = colors.primary)
                        }
                        IconButton(onClick = { viewport = fit() }) {
                            Icon(Icons.Default.FitScreen, "Fit tree", tint = colors.textPrimary)
                        }
                    }

                    val visible = graph.nodes.filter { HistoryGeometry.visible(it, viewport, width, height - 48f) }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .background(colors.background)
                            .testTag("history_viewport")
                            .semantics { stateDescription = "Zoom ${(viewport.scale * 100).roundToInt()} percent; ${visible.size} visible nodes" }
                            .pointerInput(Unit) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    viewport = viewport
                                        .zoomAt(zoom, centroid.x / density.density, centroid.y / density.density)
                                        .pan(pan.x / density.density, pan.y / density.density)
                                }
                            }
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            graph.edges.forEach { edge ->
                                if (HistoryGeometry.edgeVisible(edge, viewport, width, height - 48f)) {
                                    fun point(lane: Int, row: Int, bottom: Boolean) = Offset(
                                        viewport.screenX(lane * HistoryGeometry.LANE_STEP + 24f + HistoryGeometry.NODE_WIDTH / 2) * density.density,
                                        viewport.screenY(row * HistoryGeometry.ROW_STEP + 24f + (if (bottom) HistoryGeometry.NODE_HEIGHT else 0f)) * density.density
                                    )
                                    val a = point(edge.fromLane, edge.fromYIndex, true)
                                    val b = point(edge.toLane, edge.toYIndex, false)
                                    val middle = (a.y + b.y) / 2f
                                    val path = Path().apply {
                                        moveTo(a.x, a.y)
                                        cubicTo(a.x, middle, b.x, middle, b.x, b.y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = if (edge.isOnMainPath) colors.primary else colors.outline.copy(alpha = 0.45f),
                                        style = Stroke((if (edge.isOnMainPath) 2.5f else 1.5f) * density.density)
                                    )
                                }
                            }
                        }

                        visible.forEach { node ->
                            key(node.node.id) {
                                val scale = viewport.scale
                                val active = node.isOnMainPath
                                val outline = if (node.isCurrent) colors.primary else if (active) colors.outline else colors.outline.copy(alpha = 0.45f)
                                val icon = when {
                                    node.isCurrent -> Icons.Default.CheckCircle
                                    node.isBranchPoint -> Icons.Default.AccountTree
                                    node.node.description.contains("Delete", true) -> Icons.Default.Delete
                                    node.node.description.contains("Move", true) || node.node.description.contains("Transform", true) -> Icons.Default.OpenWith
                                    node.node.description.contains("Add", true) || node.node.description.contains("Create", true) -> Icons.Default.Add
                                    else -> Icons.Default.Edit
                                }
                                Row(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                (viewport.screenX(HistoryGeometry.x(node)) * density.density).roundToInt(),
                                                (viewport.screenY(HistoryGeometry.y(node)) * density.density).roundToInt()
                                            )
                                        }
                                        .size((HistoryGeometry.NODE_WIDTH * scale).dp, (HistoryGeometry.NODE_HEIGHT * scale).dp)
                                        .testTag("history_node_${node.node.id}")
                                        .semantics {
                                            selected = node.isCurrent
                                            stateDescription = if (node.isCurrent) "Current action" else if (active) "Active path" else "Retained branch"
                                        }
                                        .background(
                                            color = if (node.isCurrent) colors.primary.copy(alpha = 0.22f)
                                            else if (active) colors.surfaceVariant
                                            else colors.surface.copy(alpha = 0.7f),
                                            shape = SaltyTokens.shapes.sm
                                        )
                                        .border(if (node.isCurrent) 2.dp else 1.dp, outline, SaltyTokens.shapes.sm)
                                        .clickable {
                                            onJumpToNode(node.node.id)
                                            onDismiss()
                                        }
                                        .padding((6f * scale).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size((18f * scale).dp),
                                        tint = if (active) colors.primary else colors.textSecondary.copy(alpha = 0.65f)
                                    )
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .padding(start = (5f * scale).dp)
                                    ) {
                                        Text(
                                            text = node.node.description,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = (12f * scale).sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                            color = if (active) colors.textPrimary else colors.textSecondary.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = if (node.isCurrent) "Current" else if (node.isBranchPoint) "Branch" else if (!active) "Retained" else "Active",
                                            fontSize = (9f * scale).sp,
                                            maxLines = 1,
                                            color = if (active) colors.primary else colors.textSecondary.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
