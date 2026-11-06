package com.motionssalt.saltygraphics.ui.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.media.MediaImporter
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.action.GlobalClipboard
import com.motionssalt.saltygraphics.engine.action.PasteMode
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
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
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
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
    var isProjectSettingsOpen by remember { mutableStateOf(false) }
    var isVectorMenuOpen by remember { mutableStateOf(false) }
    var isVectorSettingsOpen by remember { mutableStateOf(false) }
    var vectorTool by remember { mutableStateOf(VectorTool.SELECT) }
    var vectorPathIndex by remember { mutableIntStateOf(0) }
    var vectorPointIndex by remember { mutableStateOf<Int?>(null) }
    var vectorDraft by remember { mutableStateOf<Layer?>(null) }
    var isActionTreeOpen by remember { mutableStateOf(false) }

    // Load project and init action tree
    LaunchedEffect(projectId) {
        val loaded = repository.getProject(projectId)
        if (loaded != null) {
            previewQuality = loaded.editorSettings.defaultPreviewQuality
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

    val svgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) coroutineScope.launch {
            runCatching {
                val paths = context.contentResolver.openInputStream(uri)?.use { SvgVectorImporter.parse(it) }
                    ?: error("Cannot read SVG")
                val current = projectDocument ?: return@launch
                val newLayer = Layer(name = "Imported SVG", type = LayerType.VECTOR).withVectorPaths(paths)
                recordAndAutoSave("Import SVG", current.addLayer(newLayer, activeGroupId))
                selectedLayerIds = setOf(newLayer.id); vectorPathIndex = 0; vectorPointIndex = null
                isVectorMenuOpen = false; isVectorSettingsOpen = true
            }.onFailure { Toast.makeText(context, it.message ?: "Invalid SVG", Toast.LENGTH_LONG).show() }
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val currentDoc = projectDocument ?: return@rememberLauncherForActivityResult
        if (uris.isNotEmpty()) {
            val result = MediaImporter.importMediaFromUris(
                context = context,
                projectId = currentDoc.id,
                uris = uris,
                canvasWidth = currentDoc.canvasSize.width,
                canvasHeight = currentDoc.canvasSize.height
            )
            if (result.importedLayers.isNotEmpty()) {
                var updatedDoc = currentDoc
                for (layer in result.importedLayers) {
                    updatedDoc = updatedDoc.addLayer(layer, activeGroupId)
                }
                recordAndAutoSave("Import Media (${result.importedLayers.size})", updatedDoc)
                selectedLayerIds = setOf(result.importedLayers.last().id)
            }
            if (result.failureReasons.isNotEmpty()) {
                Toast.makeText(context, result.failureReasons.first(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val saveSgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        repository.exportProjectToStream(projectId, stream)
                    }
                    Toast.makeText(context, "Saved .sg project file successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save project: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Handle nested group Back navigation
    BackHandler {
        if (isVectorMenuOpen || isVectorSettingsOpen || isProjectSettingsOpen || isTransformPanelOpen || isEffectsSheetOpen || isShapeSettingsOpen || isTextSettingsOpen || isBlendingOpen || isMaskOpen) {
            isVectorMenuOpen = false; isVectorSettingsOpen = false; vectorDraft = null
            isProjectSettingsOpen = false; isTransformPanelOpen = false; isEffectsSheetOpen = false; isShapeSettingsOpen = false; isTextSettingsOpen = false; isBlendingOpen = false; isMaskOpen = false
        } else if (activeGroupId != null) {
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
                showGrid = doc.editorSettings.showGrid,
                gridSize = doc.editorSettings.gridSize,
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
                onSettingsClick = { isVectorMenuOpen = false; isVectorSettingsOpen = false; isTransformPanelOpen = false; isEffectsSheetOpen = false; isShapeSettingsOpen = false; isTextSettingsOpen = false; isBlendingOpen = false; isMaskOpen = false; isProjectSettingsOpen = true },
                onZoomIn = { zoom = (zoom * 1.25f).coerceAtMost(10f) },
                onZoomOut = { zoom = (zoom / 1.25f).coerceAtLeast(0.1f) },
                onFitToScreen = {
                    zoom = 1f
                    panOffset = Offset.Zero
                },
                onReset100Percent = {
                    zoom = 1.0f
                    panOffset = Offset.Zero
                },
                onToggleGrid = { recordAndAutoSave("Toggle grid", doc.copy(editorSettings = doc.editorSettings.copy(showGrid = !doc.editorSettings.showGrid))) },
                onGridSizeChange = { recordAndAutoSave("Grid size", doc.copy(editorSettings = doc.editorSettings.copy(gridSize = it))) },
                onSaveSgClick = {
                    val name = doc.name.ifBlank { "Project" }.replace(" ", "_")
                    saveSgLauncher.launch("$name.sg")
                }
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        EditorWorkspace(
            modifier = Modifier.padding(innerPadding).navigationBarsPadding().displayCutoutPadding(),
            preview = {
        Box(
            modifier = Modifier
                .fillMaxSize()

        ) {
            CanvasView(
                document = vectorDraft?.let { draft -> doc.updateLayer(draft.id) { draft } } ?: doc,
                selectedLayerIds = selectedLayerIds,
                zoom = zoom,
                panOffset = panOffset,
                showGrid = doc.editorSettings.showGrid,
                gridSize = doc.editorSettings.gridSize,
                onZoomPanChange = { newZoom, newPan ->
                    zoom = newZoom
                    panOffset = newPan
                },
                onSelectLayer = { id ->
                    selectedLayerIds = if (id != null) setOf(id) else emptySet()
                },
                onUpdateLayerTransform = { id, newTransform ->
                    projectDocument = doc.updateLayer(id) { it.copy(transform = newTransform) }
                },
                onCommitLayerTransform = { id, finalTransform ->
                    val updated = doc.updateLayer(id) { it.copy(transform = finalTransform) }
                    recordAndAutoSave("Transform Layer", updated)
                }
            )

            if (isVectorSettingsOpen && currentSelectedLayer?.type == LayerType.VECTOR) {
                VectorCanvasOverlay(currentSelectedLayer, doc.canvasSize, vectorPathIndex, vectorPointIndex, vectorTool,
                    zoom, panOffset, { vectorPointIndex = it }, { vectorDraft = it },
                    { changed -> recordAndAutoSave("Edit vector points", doc.updateLayer(changed.id) { changed }) },
                    { isVectorSettingsOpen = false; vectorDraft = null })
            }

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
                                var updated: ProjectDocument = currentDoc
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
                                var updated: ProjectDocument = currentDoc
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

        }
            },

            panel = {
                val pageOpen = isVectorMenuOpen || isVectorSettingsOpen || isProjectSettingsOpen || isTransformPanelOpen || isEffectsSheetOpen || isShapeSettingsOpen || isTextSettingsOpen || isBlendingOpen || isMaskOpen
                if (isVectorMenuOpen) {
                    VectorCreationPage(onChoose = { choice ->
                        if (choice == "Import SVG") svgLauncher.launch(arrayOf("image/svg+xml", "text/xml", "application/xml", "application/octet-stream"))
                        else {
                            val path = when (choice) {
                                "Rectangle" -> ShapeData(type = ShapeType.RECTANGLE).toVectorPath()
                                "Ellipse" -> ShapeData(type = ShapeType.CIRCLE).toVectorPath()
                                "Polygon" -> ShapeData(type = ShapeType.POLYGON).toVectorPath()
                                "Star" -> ShapeData(type = ShapeType.STAR).toVectorPath()
                                else -> VectorPathData()
                            }
                            val layer = Layer(name = choice, type = LayerType.VECTOR, vectorPathData = path)
                            recordAndAutoSave("Add vector", doc.addLayer(layer, activeGroupId))
                            selectedLayerIds = setOf(layer.id); vectorPathIndex = 0; vectorPointIndex = null
                            vectorTool = if (choice == "Draw with points") VectorTool.ADD else VectorTool.SELECT
                            isVectorMenuOpen = false; isVectorSettingsOpen = true
                        }
                    }, onBack = { isVectorMenuOpen = false })
                } else if (isVectorSettingsOpen && currentSelectedLayer?.type == LayerType.VECTOR) {
                    VectorPage(currentSelectedLayer, vectorPathIndex, vectorPointIndex, vectorTool,
                        { vectorPathIndex = it }, { vectorPointIndex = it }, { vectorTool = it },
                        { description, layer -> recordAndAutoSave(description, doc.updateLayer(layer.id) { layer }) },
                        { isVectorSettingsOpen = false })
                } else if (isProjectSettingsOpen) {
                    ProjectSettingsPage(doc, { recordAndAutoSave("Project settings", it) }, { isProjectSettingsOpen = false })
                } else if (!pageOpen) {

            BottomPanel(
                selectedLayer = currentSelectedLayer,
                onAddMedia = {
                    mediaPickerLauncher.launch("image/*")
                },
                onAddText = {
                    val newLayer = Layer(
                        name = "Text Layer ${doc.layers.size + 1}",
                        type = LayerType.TEXT,
                        textData = TextLayerData()
                    )
                    recordAndAutoSave("Add Text Layer", doc.addLayer(newLayer, activeGroupId))
                    selectedLayerIds = setOf(newLayer.id)
                    isTextSettingsOpen = true
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
                onAddVector = { isVectorMenuOpen = true },
                onEditLayer = {
                    when (currentSelectedLayer?.type) {
                        LayerType.TEXT -> isTextSettingsOpen = true
                        LayerType.SHAPE -> isShapeSettingsOpen = true
                        LayerType.VECTOR -> { vectorPathIndex = 0; vectorPointIndex = null; vectorTool = VectorTool.SELECT; isVectorSettingsOpen = true }
                        LayerType.GROUP -> activeGroupId = currentSelectedLayer.id
                        else -> {}
                    }
                },
                onOpenTransform = { isTransformPanelOpen = true },
                onOpenEffects = { isEffectsSheetOpen = true },
                onOpenBlending = { isBlendingOpen = true },
                onDelete = {
                    val updated = selectedLayerIds.fold(doc) { d, id -> d.removeLayer(id) }
                    selectedLayerIds = emptySet()
                    recordAndAutoSave("Delete Layers", updated)
                }
            )

                }
            // Transform Bottom Sheet
            if (isTransformPanelOpen && currentSelectedLayer != null) {
                PanelPage("Transform", onBack = { isTransformPanelOpen = false; isEffectsSheetOpen = false; isShapeSettingsOpen = false; isTextSettingsOpen = false; isBlendingOpen = false; isMaskOpen = false }) {
                    TransformPanel(
                        transform = currentSelectedLayer.transform,
                        onTransformChange = { previewT ->
                            projectDocument = doc.updateLayer(currentSelectedLayer.id) { it.copy(transform = previewT) }
                        },
                        onTransformCommit = { finalT ->
                            val updated = doc.updateLayer(currentSelectedLayer.id) { it.copy(transform = finalT) }
                            recordAndAutoSave("Transform Layer", updated)
                        },
                        onClose = { isTransformPanelOpen = false }
                    )
                }
            }

            // Effects Bottom Sheet
            if (isEffectsSheetOpen && currentSelectedLayer != null) {
                PanelPage("Effects", onBack = { isTransformPanelOpen = false; isEffectsSheetOpen = false; isShapeSettingsOpen = false; isTextSettingsOpen = false; isBlendingOpen = false; isMaskOpen = false }, scrollable = false) {
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
    // Dialogs
    val currentShape = currentSelectedLayer?.shapeData
    if (isShapeSettingsOpen && currentShape != null) {
        ShapeSettingsDialog(
            initialShape = currentShape,
            onShapeChange = { previewShape ->
                projectDocument = doc.updateLayer(currentSelectedLayer.id) { it.copy(shapeData = previewShape) }
            },
            onShapeCommit = { committedShape ->
                val updated = doc.updateLayer(currentSelectedLayer.id) { it.copy(shapeData = committedShape) }
                recordAndAutoSave("Update Shape", updated)
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
            },
            onDismiss = { isTextSettingsOpen = false }
        )
    }

    if (isBlendingOpen && currentSelectedLayer != null) {
        val selIdx = doc.activeLayers.indexOfFirst { it.id == currentSelectedLayer.id }
        val underlying = if (selIdx > 0) doc.activeLayers.subList(0, selIdx) else emptyList()
        BlendingPage(
            currentBlendMode = currentSelectedLayer.blendMode,
            currentOpacity = currentSelectedLayer.opacity,
            currentMaskMode = currentSelectedLayer.maskMode,
            affectsLayerIds = currentSelectedLayer.affectsLayerIds,
            allUnderlyingLayers = underlying,
            onBlendChange = { mode, op ->
                val updated = doc.updateLayer(currentSelectedLayer.id) { it.copy(blendMode = mode, opacity = op) }
                recordAndAutoSave("Update Blending", updated)
            },
            onMaskChange = { maskMode, affectsIds ->
                val updated = doc.updateLayer(currentSelectedLayer.id) { it.copy(maskMode = maskMode, affectsLayerIds = affectsIds) }
                recordAndAutoSave("Update Mask", updated)
            },
            onBack = { isBlendingOpen = false }
        )
    }


            }
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
