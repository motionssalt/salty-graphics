package com.motionssalt.saltygraphics.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.action.GlobalClipboard
import com.motionssalt.saltygraphics.engine.action.PasteMode
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import com.motionssalt.saltygraphics.theme.SaltyDarkBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    initialGroupId: String? = null,
    repository: ProjectRepository,
    onNavigateBack: () -> Unit,
    onNavigateToExport: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var projectDocument by remember { mutableStateOf<ProjectDocument?>(null) }
    var actionTree by remember { mutableStateOf<ActionTree?>(null) }

    var selectedLayerIds by remember { mutableStateOf(setOf<String>()) }
    var activeGroupId by remember { mutableStateOf(initialGroupId) }

    // View mode state
    var zoom by remember { mutableStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showGrid by remember { mutableStateOf(false) }
    var gridSize by remember { mutableStateOf(20) }

    // Preview quality state
    var previewQuality by remember { mutableStateOf(PreviewQuality.HIGH) }

    // Panel visibility states
    var isLayersPanelOpen by remember { mutableStateOf(false) }
    var isTransformPanelOpen by remember { mutableStateOf(false) }
    var isEffectsSheetOpen by remember { mutableStateOf(false) }
    var isShapeSettingsOpen by remember { mutableStateOf(false) }
    var isTextSettingsOpen by remember { mutableStateOf(false) }
    var isBlendingOpen by remember { mutableStateOf(false) }
    var isMaskOpen by remember { mutableStateOf(false) }
    var isActionTreeOpen by remember { mutableStateOf(false) }

    // Load project and init action tree
    LaunchedEffect(projectId) {
        val loaded = repository.getProject(projectId)
        if (loaded != null) {
            projectDocument = loaded
            actionTree = ActionTree(loaded)
        }
    }

    val doc = projectDocument
    val tree = actionTree

    fun recordAndAutoSave(description: String, newDoc: ProjectDocument) {
        val treeRef = actionTree ?: return
        treeRef.recordAction(description, newDoc)
        projectDocument = newDoc
        coroutineScope.launch {
            repository.saveProject(newDoc)
        }
    }

    // Handle nested group Back navigation
    BackHandler {
        if (activeGroupId != null) {
            activeGroupId = null // Return to parent document
        } else {
            onNavigateBack()
        }
    }

    if (doc == null || tree == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentSelectedLayer = doc.findLayerById(selectedLayerIds.firstOrNull() ?: "")

    Scaffold(
        topBar = {
            TopPanel(
                previewQuality = previewQuality,
                canUndo = tree.canUndo,
                canRedo = tree.canRedo,
                showGrid = showGrid,
                gridSize = gridSize,
                onNavigateBack = {
                    if (activeGroupId != null) {
                        activeGroupId = null
                    } else {
                        onNavigateBack()
                    }
                },
                onToggleLayers = { isLayersPanelOpen = !isLayersPanelOpen },
                onPreviewQualityClick = { previewQuality = previewQuality.next() },
                onUndoClick = {
                    if (tree.undo()) {
                        projectDocument = tree.currentDocument
                        coroutineScope.launch { repository.saveProject(tree.currentDocument) }
                    }
                },
                onRedoClick = {
                    if (tree.redo()) {
                        projectDocument = tree.currentDocument
                        coroutineScope.launch { repository.saveProject(tree.currentDocument) }
                    }
                },
                onOpenActionTree = { isActionTreeOpen = true },
                onExportClick = { onNavigateToExport(projectId) },
                onSettingsClick = onNavigateToSettings,
                onZoomIn = { zoom = (zoom * 1.25f).coerceAtMost(10f) },
                onZoomOut = { zoom = (zoom / 1.25f).coerceAtLeast(0.1f) },
                onFitToScreen = {
                    zoom = 0.5f
                    panOffset = Offset.Zero
                },
                onReset100Percent = {
                    zoom = 1.0f
                    panOffset = Offset.Zero
                },
                onToggleGrid = { showGrid = !showGrid },
                onGridSizeChange = { gridSize = it }
            )
        },
        bottomBar = {
            BottomPanel(
                selectedLayer = currentSelectedLayer,
                onAddMedia = {
                    val newLayer = Layer(
                        name = "Media Layer ${doc.layers.size + 1}",
                        type = LayerType.MEDIA
                    )
                    recordAndAutoSave("Add Media Layer", doc.addLayer(newLayer, activeGroupId))
                    selectedLayerIds = setOf(newLayer.id)
                },
                onAddText = {
                    val newLayer = Layer(
                        name = "Text Layer ${doc.layers.size + 1}",
                        type = LayerType.TEXT,
                        textData = TextLayerData()
                    )
                    recordAndAutoSave("Add Text Layer", doc.addLayer(newLayer, activeGroupId))
                    selectedLayerIds = setOf(newLayer.id)
                },
                onAddAdjustment = {
                    val newLayer = Layer(
                        name = "Adjustment Layer",
                        type = LayerType.ADJUSTMENT
                    )
                    recordAndAutoSave("Add Adjustment Layer", doc.addLayer(newLayer, activeGroupId))
                    selectedLayerIds = setOf(newLayer.id)
                },
                onAddShape = {
                    val newLayer = Layer(
                        name = "Shape Layer ${doc.layers.size + 1}",
                        type = LayerType.SHAPE,
                        shapeData = ShapeData()
                    )
                    recordAndAutoSave("Add Shape Layer", doc.addLayer(newLayer, activeGroupId))
                    selectedLayerIds = setOf(newLayer.id)
                },
                onAddVector = {
                    val newLayer = Layer(
                        name = "Vector Path ${doc.layers.size + 1}",
                        type = LayerType.VECTOR,
                        vectorPathData = VectorPathData()
                    )
                    recordAndAutoSave("Add Vector Layer", doc.addLayer(newLayer, activeGroupId))
                    selectedLayerIds = setOf(newLayer.id)
                },
                onEditLayer = {
                    when (currentSelectedLayer?.type) {
                        LayerType.TEXT -> isTextSettingsOpen = true
                        LayerType.SHAPE -> isShapeSettingsOpen = true
                        LayerType.GROUP -> activeGroupId = currentSelectedLayer.id
                        else -> {}
                    }
                },
                onOpenTransform = { isTransformPanelOpen = true },
                onOpenEffects = { isEffectsSheetOpen = true },
                onOpenBlending = { isBlendingOpen = true },
                onOpenMask = { isMaskOpen = true }
            )
        },
        containerColor = SaltyDarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CanvasView(
                document = doc,
                selectedLayerIds = selectedLayerIds,
                zoom = zoom,
                panOffset = panOffset,
                showGrid = showGrid,
                gridSize = gridSize,
                onZoomPanChange = { newZoom, newPan ->
                    zoom = newZoom
                    panOffset = newPan
                },
                onSelectLayer = { id ->
                    selectedLayerIds = if (id != null) setOf(id) else emptySet()
                },
                onUpdateLayerTransform = { id, newTransform ->
                    val updated = doc.updateLayer(id) { it.copy(transform = newTransform) }
                    recordAndAutoSave("Transform Layer", updated)
                }
            )

            // Right-side Layers Panel Drawer
            if (isLayersPanelOpen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                ) {
                    LayersPanel(
                        layers = doc.activeLayers,
                        selectedLayerIds = selectedLayerIds,
                        onSelectLayer = { selectedLayerIds = setOf(it) },
                        onToggleSelectMulti = { id ->
                            selectedLayerIds = if (selectedLayerIds.contains(id)) {
                                selectedLayerIds - id
                            } else {
                                selectedLayerIds + id
                            }
                        },
                        onSelectAll = {
                            selectedLayerIds = doc.activeLayers.map { it.id }.toSet()
                        },
                        onToggleVisibility = { id ->
                            val updated = doc.updateLayer(id) { it.copy(isVisible = !it.isVisible) }
                            recordAndAutoSave("Toggle Visibility", updated)
                        },
                        onToggleLock = { id ->
                            val updated = doc.updateLayer(id) { it.copy(isLocked = !it.isLocked) }
                            recordAndAutoSave("Toggle Lock", updated)
                        },
                        onDeleteLayers = { ids ->
                            val currentDoc = doc
                            if (currentDoc != null) {
                                var updated = currentDoc
                                for (id in ids) {
                                    updated = updated.removeLayer(id)
                                }
                                selectedLayerIds = emptySet()
                                recordAndAutoSave("Delete Layers", updated)
                            }
                        },
                        onRenameLayer = { id, newName ->
                            val updated = doc.updateLayer(id) { it.copy(name = newName) }
                            recordAndAutoSave("Rename Layer", updated)
                        },
                        onCopyLayer = { layer ->
                            GlobalClipboard.copy(layer)
                        },
                        onPasteProperties = { mode ->
                            val currentDoc = doc
                            if (currentDoc != null) {
                                var updated = currentDoc
                                for (id in selectedLayerIds) {
                                    updated = updated.updateLayer(id) { GlobalClipboard.applyTo(it, mode) }
                                }
                                recordAndAutoSave("Paste Properties (${mode.displayName})", updated)
                            }
                        },
                        onMoveLayer = { from, to ->
                            val list = doc.activeLayers.toMutableList()
                            val item = list.removeAt(from)
                            list.add(to, item)
                            val updated = if (activeGroupId == null) {
                                doc.copy(layers = list)
                            } else {
                                doc.updateLayer(activeGroupId!!) { it.copy(children = list) }
                            }
                            recordAndAutoSave("Reorder Layers", updated)
                        },
                        onEnterGroup = { groupId ->
                            activeGroupId = groupId
                        },
                        onClosePanel = { isLayersPanelOpen = false }
                    )
                }
            }

            // Transform Bottom Sheet
            if (isTransformPanelOpen && currentSelectedLayer != null) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    TransformPanel(
                        transform = currentSelectedLayer.transform,
                        onTransformChange = { newT ->
                            val updated = doc.updateLayer(currentSelectedLayer.id) { it.copy(transform = newT) }
                            recordAndAutoSave("Transform Layer", updated)
                        },
                        onClose = { isTransformPanelOpen = false }
                    )
                }
            }

            // Effects Bottom Sheet
            if (isEffectsSheetOpen && currentSelectedLayer != null) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    EffectSettingsSheet(
                        targetLayer = currentSelectedLayer,
                        allLayers = doc.activeLayers,
                        onAddEffect = { effect ->
                            val updated = doc.updateLayer(currentSelectedLayer.id) {
                                it.copy(effects = it.effects + effect)
                            }
                            recordAndAutoSave("Add Effect", updated)
                        },
                        onRemoveEffect = { effectId ->
                            val updated = doc.updateLayer(currentSelectedLayer.id) {
                                it.copy(effects = it.effects.filter { e -> e.effectId != effectId })
                            }
                            recordAndAutoSave("Remove Effect", updated)
                        },
                        onUpdateEffect = { updatedEffect ->
                            val updated = doc.updateLayer(currentSelectedLayer.id) {
                                it.copy(effects = it.effects.map { e -> if (e.effectId == updatedEffect.effectId) updatedEffect else e })
                            }
                            recordAndAutoSave("Update Effect", updated)
                        },
                        onClose = { isEffectsSheetOpen = false }
                    )
                }
            }
        }
    }

    // Dialogs
    val currentShape = currentSelectedLayer?.shapeData
    if (isShapeSettingsOpen && currentShape != null) {
        ShapeSettingsDialog(
            initialShape = currentShape,
            onSave = { newShape ->
                val updated = doc.updateLayer(currentSelectedLayer.id) { it.copy(shapeData = newShape) }
                recordAndAutoSave("Update Shape Settings", updated)
                isShapeSettingsOpen = false
            },
            onDismiss = { isShapeSettingsOpen = false }
        )
    }

    val currentText = currentSelectedLayer?.textData
    if (isTextSettingsOpen && currentText != null) {
        val candidates = doc.activeLayers.filter { it.type == LayerType.SHAPE || it.type == LayerType.VECTOR }
        TextSettingsDialog(
            initialData = currentText,
            candidatePathLayers = candidates,
            onSave = { newTextData ->
                val updated = doc.updateLayer(currentSelectedLayer.id) { it.copy(textData = newTextData) }
                recordAndAutoSave("Update Text Settings", updated)
                isTextSettingsOpen = false
            },
            onDismiss = { isTextSettingsOpen = false }
        )
    }

    if (isBlendingOpen && currentSelectedLayer != null) {
        BlendingDialog(
            currentBlendMode = currentSelectedLayer.blendMode,
            currentOpacity = currentSelectedLayer.opacity,
            onApply = { mode, op ->
                val updated = doc.updateLayer(currentSelectedLayer.id) { it.copy(blendMode = mode, opacity = op) }
                recordAndAutoSave("Update Blend Mode", updated)
                isBlendingOpen = false
            },
            onDismiss = { isBlendingOpen = false }
        )
    }

    if (isMaskOpen && currentSelectedLayer != null) {
        MaskDialog(
            currentMaskMode = currentSelectedLayer.maskMode,
            onApply = { mode ->
                val updated = doc.updateLayer(currentSelectedLayer.id) { it.copy(maskMode = mode) }
                recordAndAutoSave("Update Mask Mode", updated)
                isMaskOpen = false
            },
            onDismiss = { isMaskOpen = false }
        )
    }

    if (isActionTreeOpen) {
        ActionTreeDialog(
            actionTree = tree,
            onJumpToNode = { nodeId ->
                val restored = tree.jumpTo(nodeId)
                if (restored != null) {
                    projectDocument = restored
                    coroutineScope.launch { repository.saveProject(restored) }
                }
            },
            onDismiss = { isActionTreeOpen = false }
        )
    }
}
