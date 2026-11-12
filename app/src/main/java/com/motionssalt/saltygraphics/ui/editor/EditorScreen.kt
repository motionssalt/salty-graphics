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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.media.MediaImporter
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.action.GlobalClipboard
import com.motionssalt.saltygraphics.engine.action.PasteMode
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.effect.EffectRegistry
import com.motionssalt.saltygraphics.ui.controls.BorderShadowPanelPage
import com.motionssalt.saltygraphics.ui.controls.FillPanelPage
import com.motionssalt.saltygraphics.media.MediaCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import com.motionssalt.saltygraphics.engine.file.ProjectLoadState
import androidx.compose.ui.platform.testTag

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
    var historyVersion by remember { mutableIntStateOf(0) }

    var selectedLayerIds by remember { mutableStateOf(setOf<String>()) }
    var activeGroupId by remember { mutableStateOf(initialGroupId) }
    var layerToRasterize by remember { mutableStateOf<Layer?>(null) }

    fun findGroupPath(layers: List<Layer>, targetId: String): List<Pair<String, String>>? {
        for (layer in layers) {
            if (layer.id == targetId && layer.type == LayerType.GROUP) {
                return listOf(layer.id to layer.name)
            }
            if (layer.children.isNotEmpty()) {
                val sub = findGroupPath(layer.children, targetId)
                if (sub != null) {
                    return listOf(layer.id to layer.name) + sub
                }
            }
        }
        return null
    }

    val groupBreadcrumbs = remember(projectDocument, activeGroupId) {
        val target = activeGroupId
        val docLayers = projectDocument?.layers ?: emptyList()
        if (target == null) emptyList() else findGroupPath(docLayers, target) ?: emptyList()
    }

    // View mode state
    var zoom by remember { mutableStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showGrid by remember { mutableStateOf(false) }
    var gridSize by remember { mutableStateOf(20) }
    var isCanvasLocked by rememberSaveable { mutableStateOf(false) }
    var externalSmartGuides by remember { mutableStateOf<List<com.motionssalt.saltygraphics.engine.model.SmartGuideLine>>(emptyList()) }
    var externalSnappedGridX by remember { mutableStateOf<Float?>(null) }
    var externalSnappedGridY by remember { mutableStateOf<Float?>(null) }

    // Panel visibility states
    var isLayersPanelOpen by remember { mutableStateOf(false) }
    var isTransformPanelOpen by remember { mutableStateOf(false) }
    var isEffectsSheetOpen by remember { mutableStateOf(false) }
    var isShapeSettingsOpen by remember { mutableStateOf(false) }
    var isTextSettingsOpen by remember { mutableStateOf(false) }
    var isBlendingOpen by remember { mutableStateOf(false) }
    var isFillPanelOpen by remember { mutableStateOf(false) }
    var isBorderShadowPanelOpen by remember { mutableStateOf(false) }

    var isMaskOpen by remember { mutableStateOf(false) }
    var isProjectSettingsOpen by remember { mutableStateOf(false) }
    var isVectorMenuOpen by remember { mutableStateOf(false) }
    var isVectorSettingsOpen by remember { mutableStateOf(false) }
    var isShapePickerOpen by remember { mutableStateOf(false) }
    var vectorTool by remember { mutableStateOf(VectorTool.SELECT) }
    var vectorPathIndex by remember { mutableIntStateOf(0) }
    var vectorPointIndex by remember { mutableStateOf<Int?>(null) }
    var vectorDraft by remember { mutableStateOf<Layer?>(null) }
    var isActionTreeOpen by remember { mutableStateOf(false) }

    val loadState=remember(projectId) {MutableStateFlow<ProjectLoadState>(ProjectLoadState.Loading())}
    val loading by loadState.collectAsState()
    var loadAttempt by remember(projectId) {mutableIntStateOf(0)}
    var safeMode by remember(projectId) {mutableStateOf(false)}
    LaunchedEffect(projectId,loadAttempt) {
        projectDocument=null;actionTree=null
        val result=repository.openProject(projectId,safeMode) {loadState.value=it}
        loadState.value=result
        if(result is ProjectLoadState.Loaded) {
            val loaded=result.result.document
            projectDocument=loaded
            actionTree=result.result.historySnapshot?.let {ActionTree.fromSnapshot(it,loaded)} ?: ActionTree(loaded)
        }
    }

    val doc = projectDocument
    // The engine tree is intentionally not Compose-dependent. A pointer change
    // must invalidate the UI even when two history nodes have equal documents.
    val tree = remember(actionTree, historyVersion) { actionTree }

    var isSavingBeforeExit by remember { mutableStateOf(false) }
    var exitSaveError by remember { mutableStateOf<String?>(null) }

    fun requestSafeExit() {
        val currentDoc = projectDocument
        val snapshot = actionTree?.toSnapshot()
        if (currentDoc != null) {
            isSavingBeforeExit = true
            exitSaveError = null
            coroutineScope.launch {
                try {
                    repository.saveProject(currentDoc, snapshot)
                    isSavingBeforeExit = false
                    onNavigateBack()
                } catch (e: Exception) {
                    isSavingBeforeExit = false
                    exitSaveError = e.message ?: e.javaClass.simpleName
                }
            }
        } else {
            onNavigateBack()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP || event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                val currentDoc = projectDocument
                val snapshot = actionTree?.toSnapshot()
                if (currentDoc != null) {
                    repository.saveProjectInBackground(currentDoc, snapshot)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun recordCommandAndAutoSave(identity: ActionIdentity, mutation: (ProjectDocument) -> ProjectDocument) {
        val treeRef = actionTree ?: return
        treeRef.recordCommand(identity, mutation)
        historyVersion++
        projectDocument = treeRef.currentDocument
        val snapshot = treeRef.toSnapshot()
        repository.saveProjectInBackground(treeRef.currentDocument, snapshot)
    }

    fun recordSettingChange(identity: ActionIdentity, transform: (EditorSettings) -> EditorSettings) {
        recordCommandAndAutoSave(identity) { it.updateEditorSettings(transform) }
    }

    fun recordAndAutoSave(description: String, newDoc: ProjectDocument) {
        val treeRef = actionTree ?: return
        treeRef.recordAction(description, newDoc)
        historyVersion++
        projectDocument = treeRef.currentDocument
        val snapshot = treeRef.toSnapshot()
        repository.saveProjectInBackground(treeRef.currentDocument, snapshot)
    }

    fun restoreHistoryDocument(restored: ProjectDocument) {
        projectDocument = restored
        selectedLayerIds = selectedLayerIds.filter { restored.findLayerById(it) != null }.toSet()
        historyVersion++
        val snapshot = actionTree?.toSnapshot()
        repository.saveProjectInBackground(restored, snapshot)
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
            Toast.makeText(context, "Optimizing ${uris.size} image(s)...", Toast.LENGTH_SHORT).show()
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
                val count = result.importedLayers.size
                Toast.makeText(context, "Image${if (count > 1) "s" else ""} optimized and imported", Toast.LENGTH_SHORT).show()
            }
            if (result.failureReasons.isNotEmpty()) {
                Toast.makeText(context, result.failureReasons.first(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    var fillMediaRevision by remember { mutableIntStateOf(0) }
    var fillImportTargetId by remember { mutableStateOf<String?>(null) }
    val fillMediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val targetId = fillImportTargetId
        fillImportTargetId = null
        if (uri != null && targetId != null) coroutineScope.launch {
            val current = projectDocument ?: return@launch
            Toast.makeText(context, "Optimizing image…", Toast.LENGTH_SHORT).show()
            val result = withContext(Dispatchers.IO) {
                MediaImporter.importMediaFromUris(context, current.id, listOf(uri), current.canvasSize.width, current.canvasSize.height)
            }
            val asset = result.importedLayers.firstOrNull()?.mediaAssetPath
            if (asset != null) {
                recordCommandAndAutoSave(ActionIdentity("Replace", "Layer", "media fill")) { live ->
                    live.updateLayer(targetId) { layer -> layer.copy(fillData = layer.effectiveFill.copy(type = FillType.MEDIA, mediaAssetPath = asset)) }
                }
                fillMediaRevision++
                Toast.makeText(context, "Image optimized and imported", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(context, result.failureReasons.firstOrNull() ?: "Could not import image", Toast.LENGTH_LONG).show()
        }
    }
    fun mediaReferences(layers: List<Layer>): Set<String> = layers.flatMap { layer ->
        listOfNotNull(layer.mediaAssetPath,layer.fillData?.mediaAssetPath)+mediaReferences(layer.children)
    }.toSet()
    val referencedMedia=mediaReferences(projectDocument?.layers ?: emptyList())
    val fillMediaPaths by produceState(emptyList<String>(), projectId, fillMediaRevision, referencedMedia) {
        value = withContext(Dispatchers.IO) {
            repository.getAssetsDir(projectId).listFiles()?.filter { it.isFile && it.extension.lowercase() in setOf("png", "jpg", "jpeg", "webp") }
                ?.sortedBy { it.name }?.map { "assets/${it.name}" } ?: emptyList()
        }
    }
    val mediaCacheRevision by MediaCache.revision.collectAsState()

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
        if (exitSaveError != null) {
            exitSaveError = null
        } else if (isVectorMenuOpen || isVectorSettingsOpen || isProjectSettingsOpen || isTransformPanelOpen || isEffectsSheetOpen || isShapeSettingsOpen || isTextSettingsOpen || isBlendingOpen || isMaskOpen || isFillPanelOpen || isBorderShadowPanelOpen) {
            isVectorMenuOpen = false; isVectorSettingsOpen = false; vectorDraft = null
            isProjectSettingsOpen = false; isTransformPanelOpen = false; isEffectsSheetOpen = false; isShapeSettingsOpen = false; isTextSettingsOpen = false; isBlendingOpen = false; isMaskOpen = false; isFillPanelOpen = false; isBorderShadowPanelOpen = false
        } else if (activeGroupId != null) {
            val crumbs = groupBreadcrumbs
            activeGroupId = if (crumbs.size > 1) crumbs[crumbs.size - 2].first else null
        } else {
            requestSafeExit()
        }
    }

    if (doc == null || tree == null) {
        ProjectLoadingPanel(loading,
            onRetry={safeMode=false;loadState.value=ProjectLoadState.Loading();loadAttempt++},
            onSafeMode={safeMode=true;loadState.value=ProjectLoadState.Loading();loadAttempt++},onBack=onNavigateBack)
        return
    }

    val panelLayers = LayerStackCommands.layers(doc, activeGroupId)
    val currentSelectedLayer = doc.findLayerById(selectedLayerIds.firstOrNull() ?: "")

    // A fixed pane must always have content. Requested pages are not eligible without
    // their target, and stale requests must not resurrect when a later layer is selected.
    LaunchedEffect(currentSelectedLayer?.id, currentSelectedLayer?.type) {
        if (currentSelectedLayer == null) {
            isTransformPanelOpen = false; isEffectsSheetOpen = false
            isBlendingOpen = false; isMaskOpen = false; isFillPanelOpen = false
        }
        if (currentSelectedLayer?.shapeData == null) isShapeSettingsOpen = false
        if (currentSelectedLayer?.textData == null) isTextSettingsOpen = false
        if (currentSelectedLayer?.type != LayerType.VECTOR) isVectorSettingsOpen = false
        vectorDraft = null; vectorPointIndex = null
    }

    Scaffold(
        topBar = {
            TopPanel(
                previewQuality = doc.editorSettings.defaultPreviewQuality,
                canUndo = tree.canUndo,
                canRedo = tree.canRedo,
                showGrid = doc.editorSettings.showGrid,
                gridSize = doc.editorSettings.gridSize,
                isCanvasLocked = isCanvasLocked,
                onToggleCanvasLock = { isCanvasLocked = !isCanvasLocked },
                onNavigateBack = {
                    if (activeGroupId != null) {
                        activeGroupId = null
                    } else {
                        requestSafeExit()
                    }
                },
                onToggleLayers = { isLayersPanelOpen = !isLayersPanelOpen },
                onPreviewQualityClick = {
                    val next = doc.editorSettings.defaultPreviewQuality.next()
                    recordSettingChange(ActionIdentity("Change", "Preview Quality")) {
                        it.copy(defaultPreviewQuality = next)
                    }
                },
                onUndoClick = {
                    val actionName = tree.undoActionName ?: "action"
                    if (tree.undo()) {
                        restoreHistoryDocument(tree.currentDocument)
                        Toast.makeText(context, "Undid $actionName", Toast.LENGTH_SHORT).show()
                    }
                },
                onRedoClick = {
                    val actionName = tree.redoActionName ?: "action"
                    if (tree.redo()) {
                        restoreHistoryDocument(tree.currentDocument)
                        Toast.makeText(context, "Redid $actionName", Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenActionTree = { isActionTreeOpen = true },
                onExportClick = { onNavigateToExport(projectId) },
                onSettingsClick = { isVectorMenuOpen = false; isVectorSettingsOpen = false; isTransformPanelOpen = false; isEffectsSheetOpen = false; isShapeSettingsOpen = false; isTextSettingsOpen = false; isBlendingOpen = false; isMaskOpen = false; isFillPanelOpen = false; isBorderShadowPanelOpen = false; isProjectSettingsOpen = true },
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
                onToggleGrid = {
                    val show = !(doc.editorSettings.showGrid)
                    recordSettingChange(ActionIdentity(if (show) "Show" else "Hide", "Gridlines")) { it.copy(showGrid = show) }
                },
                onGridSizeChange = { nextSize ->
                    val currentSize = doc.editorSettings.gridSize
                    val verb = if (nextSize >= currentSize) "Increase" else "Decrease"
                    recordSettingChange(ActionIdentity(verb, "Grid", "size")) { it.copy(gridSize = nextSize) }
                },
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
                document = (vectorDraft?.let { draft -> doc.updateLayer(draft.id) { draft } } ?: doc).copy(activeGroupId = activeGroupId),
                selectedLayerIds = selectedLayerIds,
                zoom = zoom,
                panOffset = panOffset,
                previewQuality = doc.editorSettings.defaultPreviewQuality,
                isCanvasLocked = isCanvasLocked,
                isTextEditing = isTextSettingsOpen && currentSelectedLayer?.type == LayerType.TEXT,
                showGrid = doc.editorSettings.showGrid,
                gridSize = doc.editorSettings.gridSize,
                externalSmartGuides = externalSmartGuides,
                externalSnappedGridX = externalSnappedGridX,
                externalSnappedGridY = externalSnappedGridY,
                onZoomPanChange = { newZoom, newPan ->
                    zoom = newZoom
                    panOffset = newPan
                },
                onSelectLayer = { id ->
                    selectedLayerIds = if (id != null) setOf(id) else emptySet()
                },
                onUpdateLayerTransform = { id, newTransform ->
                    projectDocument?.let { projectDocument=SelectionGeometry.apply(it,id,newTransform) }
                },
                onCommitLayerTransform = { id, finalTransform ->
                    val live=projectDocument
                    if(live!=null) recordAndAutoSave("Transform Layer",SelectionGeometry.apply(live,id,finalTransform))
                },
                onUpdateLayerTransforms = { transforms ->
                    val live=projectDocument
                    if(live!=null && transforms.values.all { SelectionGeometry.valid(it) }) {
                        projectDocument=transforms.entries.fold(live) { d,(id,t) -> SelectionGeometry.apply(d,id,t) }
                    }
                },
                onCommitLayerTransforms = { transforms ->
                    val live=projectDocument
                    if(live!=null && transforms.isNotEmpty() && transforms.values.all { SelectionGeometry.valid(it) }) {
                        val updated=transforms.entries.fold(live) { d,(id,t) -> SelectionGeometry.apply(d,id,t) }
                        // Compare to the committed history state, not the last preview frame.
                        if(updated.layers!=actionTree?.currentDocument?.layers) recordAndAutoSave("Transform Layer",updated)
                    }
                },
                onUpdateTextData = { layerId, newTextData ->
                    projectDocument?.let { cur ->
                        projectDocument = cur.updateLayer(layerId) {
                            val base = it.copy(textData = newTextData)
                            val fd = it.fillData
                            if (fd == null || fd.type == FillType.SOLID) {
                                base.copy(fillData = FillData(type = FillType.SOLID, solidColor = newTextData.color, solidOpacity = 1f))
                            } else base
                        }
                    }
                },
                onCommitTextData = { layerId, finalTextData ->
                    val live = projectDocument
                    if (live != null) {
                        val updated = live.updateLayer(layerId) {
                            val base = it.copy(textData = finalTextData)
                            val fd = it.fillData
                            if (fd == null || fd.type == FillType.SOLID) {
                                base.copy(fillData = FillData(type = FillType.SOLID, solidColor = finalTextData.color, solidOpacity = 1f))
                            } else base
                        }
                        recordAndAutoSave("Resize Text Box", updated)
                    }
                }
            )

            if (isFillPanelOpen && currentSelectedLayer?.effectiveFill?.type == FillType.GRADIENT) {
                val targetId=currentSelectedLayer.id
                GradientCanvasOverlay(currentSelectedLayer,doc.canvasSize,zoom,panOffset,
                    onPreview={ fill -> projectDocument=projectDocument?.updateLayer(targetId){it.copy(fillData=fill)} },
                    onCommit={ fill -> recordCommandAndAutoSave(ActionIdentity("Change","Layer","gradient geometry")) { live ->
                        live.updateLayer(targetId){it.withUniversalFill(fill)}
                    } })
            }

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
                        layers = panelLayers,
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
                            selectedLayerIds = panelLayers.map { it.id }.toSet()
                        },
                        onSelectNone = { selectedLayerIds = emptySet() },
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
                            if (currentDoc != null && ids.isNotEmpty()) {
                                var updated: ProjectDocument = currentDoc
                                for (id in ids) {
                                    updated = updated.removeLayer(id)
                                }
                                selectedLayerIds = emptySet()
                                recordAndAutoSave("Delete Layers", updated)
                                val count = ids.size
                                Toast.makeText(context, if (count == 1) "Deleted 1 layer" else "Deleted $count layers", Toast.LENGTH_SHORT).show()
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
                            val source = panelLayers.getOrNull(from)?.id
                            val destination = panelLayers.getOrNull(to)?.id
                            if (source != null && destination != null && source != destination) {
                                recordCommandAndAutoSave(ActionIdentity("Reorder", "Layers")) {
                                    LayerStackCommands.reorder(it, activeGroupId, source, destination)
                                }
                            }
                        },
                        onEnterGroup = { groupId ->
                            activeGroupId = groupId
                        },
                        onClosePanel = { isLayersPanelOpen = false },
                        onDuplicateLayer = { id ->
                            val targets = if (selectedLayerIds.isNotEmpty()) panelLayers.filter { it.id in selectedLayerIds } else listOfNotNull(doc.findLayerById(id))
                            if (targets.isNotEmpty()) {
                                var currentList = panelLayers.toMutableList()
                                val newSelectedIds = mutableSetOf<String>()
                                targets.forEach { target ->
                                    val dup = target.copy(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = "${target.name} Copy"
                                    )
                                    newSelectedIds.add(dup.id)
                                    val idx = currentList.indexOfFirst { it.id == target.id }
                                    if (idx >= 0) currentList.add(idx, dup) else currentList.add(0, dup)
                                }
                                val updated = if (activeGroupId == null) {
                                    doc.copy(layers = currentList)
                                } else {
                                    doc.updateLayer(activeGroupId!!) { it.copy(children = currentList) }
                                }
                                selectedLayerIds = newSelectedIds
                                val label = if (targets.size == 1) "Duplicate Layer" else "Duplicate ${targets.size} layers"
                                recordAndAutoSave(label, updated)
                            }
                        },
                        onGroupSelected = {
                            val ids = selectedLayerIds
                            if (panelLayers.count { it.id in ids } > 1) {
                                val id = java.util.UUID.randomUUID().toString()
                                recordCommandAndAutoSave(ActionIdentity("Group", "Layers")) {
                                    LayerStackCommands.group(it, activeGroupId, ids, id)
                                }
                                selectedLayerIds = setOf(id)
                            }
                        },
                        onUngroupSelected = {
                            val id = selectedLayerIds.singleOrNull()
                            val group = panelLayers.find { it.id == id && it.type == LayerType.GROUP }
                            if (group != null) {
                                recordCommandAndAutoSave(ActionIdentity("Ungroup", "Layers")) {
                                    LayerStackCommands.ungroup(it, activeGroupId, group.id)
                                }
                                selectedLayerIds = group.children.map { it.id }.toSet()
                            }
                        }
                    )
                }
            }

        }
            },

            panel = {
                // Derive eligibility in this composition (not just in the effect above)
                // so no blank frame occurs on deselection or an incompatible layer tap.
                val pageOpen = isVectorMenuOpen || isProjectSettingsOpen ||
                    (isVectorSettingsOpen && currentSelectedLayer?.type == LayerType.VECTOR) ||
                    (isShapeSettingsOpen && currentSelectedLayer?.shapeData != null) ||
                    (isTextSettingsOpen && currentSelectedLayer?.textData != null) ||
                    (currentSelectedLayer != null && (isTransformPanelOpen || isEffectsSheetOpen || isBlendingOpen || isFillPanelOpen || isBorderShadowPanelOpen))
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
                    ProjectSettingsPage(
                        document = doc,
                        onCommit = { change ->
                            val live = tree.currentDocument
                            if (change.appliesTo(live)) recordCommandAndAutoSave(change.identity(live), change::apply)
                        },
                        onPreview = { change ->
                            val live = tree.currentDocument
                            projectDocument = change?.apply(live) ?: live
                        },
                        onBack = { isProjectSettingsOpen = false }
                    )
                } else if (isFillPanelOpen && currentSelectedLayer != null) {
                    val targetId = currentSelectedLayer.id
                    // The revision observation invalidates cached null image cells after decoding.
                    val media = remember(fillMediaPaths, mediaCacheRevision) {
                        fillMediaPaths.map { path -> path to MediaCache.getOrRequest(context, doc.id, path) }
                    }
                    FillPanelPage(
                        fill = currentSelectedLayer.effectiveFill,
                        media = media,
                        onPreview = { fill ->
                            projectDocument = projectDocument?.updateLayer(targetId) { it.copy(fillData = fill) }
                        },
                        onCommit = { label, fill ->
                            recordCommandAndAutoSave(ActionIdentity(label.substringBefore(' '), "Layer", label.substringAfter(' '))) { live ->
                                live.updateLayer(targetId) { it.withUniversalFill(fill) }
                            }
                        },
                        onImport = { fillImportTargetId = targetId; fillMediaPicker.launch("image/*") },
                        onBack = { isFillPanelOpen = false }
                    )
                } else if (isBorderShadowPanelOpen && currentSelectedLayer != null) {
                    val targetLayer = currentSelectedLayer
                    val targetId = targetLayer.id
                    BorderShadowPanelPage(
                        stroke = targetLayer.effectiveStroke,
                        shadow = targetLayer.effectiveShadow,
                        onStrokePreview = { newStroke ->
                            projectDocument = projectDocument?.updateLayer(targetId) { layer ->
                                layer.copy(strokeData = newStroke)
                            }
                        },
                        onStrokeCommit = { desc, newStroke ->
                            val updated = doc.updateLayer(targetId) { layer ->
                                val base = layer.copy(strokeData = newStroke)
                                when (layer.type) {
                                    LayerType.SHAPE -> {
                                        val shape = layer.shapeData ?: ShapeData()
                                        base.copy(shapeData = shape.copy(
                                            hasStroke = newStroke.isEnabled,
                                            strokeColor = newStroke.color,
                                            strokeWidth = newStroke.width
                                        ))
                                    }
                                    LayerType.TEXT -> {
                                        val text = layer.textData ?: TextLayerData()
                                        base.copy(textData = text.copy(
                                            hasOutline = newStroke.isEnabled,
                                            outlineColor = newStroke.color,
                                            outlineWidth = newStroke.width
                                        ))
                                    }
                                    LayerType.VECTOR -> {
                                        val path = layer.vectorPathData
                                        if (path != null) {
                                            base.copy(vectorPathData = path.copy(
                                                hasStroke = newStroke.isEnabled,
                                                strokeColor = newStroke.color,
                                                strokeWidth = newStroke.width,
                                                strokeCap = newStroke.cap,
                                                strokeJoin = newStroke.join,
                                                dashLength = if (newStroke.isDashed) newStroke.dashLength else 0f,
                                                dashGap = if (newStroke.isDashed) newStroke.dashGap else 0f
                                            ))
                                        } else base
                                    }
                                    else -> base
                                }
                            }
                            projectDocument = updated
                            recordAndAutoSave(desc, updated)
                        },
                        onShadowPreview = { newShadow ->
                            projectDocument = projectDocument?.updateLayer(targetId) { layer ->
                                layer.copy(shadowData = newShadow)
                            }
                        },
                        onShadowCommit = { desc, newShadow ->
                            val updated = doc.updateLayer(targetId) { layer ->
                                layer.copy(shadowData = newShadow)
                            }
                            projectDocument = updated
                            recordAndAutoSave(desc, updated)
                        },
                        onBack = { isBorderShadowPanelOpen = false }
                    )
                } else if (isShapeSettingsOpen && currentSelectedLayer?.shapeData != null) {
                    val currentShape = currentSelectedLayer.shapeData!!
                    ShapeSettingsDialog(
                        initialShape = currentShape,
                        onShapeChange = { previewShape ->
                            projectDocument = doc.updateLayer(currentSelectedLayer.id) {
                                val base = it.copy(shapeData = previewShape)
                                val fd = it.fillData
                                if (fd == null || fd.type == FillType.SOLID) {
                                    base.copy(fillData = FillData(type = FillType.SOLID, solidColor = previewShape.fillColor, solidOpacity = if (previewShape.hasFill) 1f else 0f))
                                } else base
                            }
                        },
                        onShapeCommit = { committedShape ->
                            val updated = doc.updateLayer(currentSelectedLayer.id) {
                                val base = it.copy(shapeData = committedShape)
                                val fd = it.fillData
                                if (fd == null || fd.type == FillType.SOLID) {
                                    base.copy(fillData = FillData(type = FillType.SOLID, solidColor = committedShape.fillColor, solidOpacity = if (committedShape.hasFill) 1f else 0f))
                                } else base
                            }
                            recordAndAutoSave("Update Shape", updated)
                        },
                        onDismiss = { isShapeSettingsOpen = false }
                    )
                } else if (isTextSettingsOpen && currentSelectedLayer?.textData != null) {
                    val currentText = currentSelectedLayer.textData!!
                    val candidates = doc.activeLayers.filter { it.type == LayerType.SHAPE || it.type == LayerType.VECTOR }
                    TextSettingsDialog(
                        initialData = currentText,
                        candidatePathLayers = candidates,
                        onSave = { newTextData ->
                            val updated = doc.updateLayer(currentSelectedLayer.id) {
                                val base = it.copy(textData = newTextData)
                                val fd = it.fillData
                                if (fd == null || fd.type == FillType.SOLID) {
                                    base.copy(fillData = FillData(type = FillType.SOLID, solidColor = newTextData.color, solidOpacity = 1f))
                                } else base
                            }
                            recordAndAutoSave("Update Text Settings", updated)
                        },
                        onDismiss = { isTextSettingsOpen = false }
                    )
                } else if (isBlendingOpen && currentSelectedLayer != null) {
                    val otherLayers = doc.activeLayers.filter { it.id != currentSelectedLayer.id }
                    BlendingPage(
                        currentBlendMode = currentSelectedLayer.blendMode,
                        currentOpacity = currentSelectedLayer.opacity,
                        currentMaskType = currentSelectedLayer.effectiveMaskType,
                        currentMaskSourceLayerId = currentSelectedLayer.effectiveMaskSourceLayerId,
                        isMaskInverted = currentSelectedLayer.effectiveMaskInverted,
                        currentMaskMode = currentSelectedLayer.maskMode,
                        allAvailableLayers = otherLayers,
                        onBlendChange = { mode, op ->
                            val updated = doc.updateLayer(currentSelectedLayer.id) { it.copy(blendMode = mode, opacity = op) }
                            recordAndAutoSave("Update Blending", updated)
                        },
                        onMaskChange = { maskType, sourceId, isInverted, maskMode ->
                            val updated = doc.updateLayer(currentSelectedLayer.id) {
                                it.copy(
                                    maskType = maskType,
                                    maskSourceLayerId = sourceId,
                                    isMaskInverted = isInverted,
                                    maskMode = maskMode,
                                    affectsLayerIds = listOfNotNull(sourceId)
                                )
                            }
                            recordAndAutoSave("Update Mask", updated)
                        },
                        onBack = { isBlendingOpen = false }
                    )
                } else if (isShapePickerOpen) {
                    ShapePickerDialog(
                        onSelectShape = { chosenShape ->
                            val newLayer = Layer(
                                name = "${chosenShape.type.displayName} Layer ${doc.layers.size + 1}",
                                type = LayerType.SHAPE,
                                shapeData = chosenShape
                            )
                            recordAndAutoSave("Add ${chosenShape.type.displayName}", doc.addLayer(newLayer, activeGroupId))
                            selectedLayerIds = setOf(newLayer.id)
                            isShapePickerOpen = false
                            isShapeSettingsOpen = true
                        },
                        onDismiss = { isShapePickerOpen = false }
                    )
                } else if (!pageOpen) {

                    BottomPanel(
                        document = doc,
                        selectedLayer = currentSelectedLayer,
                        groupBreadcrumbs = groupBreadcrumbs,
                        onPopGroup = {
                            val crumbs = groupBreadcrumbs
                            activeGroupId = if (crumbs.size > 1) crumbs[crumbs.size - 2].first else null
                        },
                        onJumpToGroup = { idx ->
                            activeGroupId = if (idx < 0) null else groupBreadcrumbs.getOrNull(idx)?.first
                        },
                        onRasterize = {
                            layerToRasterize = currentSelectedLayer
                        },
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
                            isShapePickerOpen = true
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
                        onOpenFill = {
                            if (currentSelectedLayer != null) {
                                isFillPanelOpen = true
                            }
                        },
                        onOpenBorderShadow = {
                            if (currentSelectedLayer != null) {
                                isBorderShadowPanelOpen = true
                            }
                        },
                        onDuplicate = {
                            val targets = if (selectedLayerIds.isNotEmpty()) panelLayers.filter { it.id in selectedLayerIds } else listOfNotNull(currentSelectedLayer)
                            if (targets.isNotEmpty()) {
                                var currentList = panelLayers.toMutableList()
                                val newSelectedIds = mutableSetOf<String>()
                                targets.forEach { target ->
                                    val dup = target.copy(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = "${target.name} Copy"
                                    )
                                    newSelectedIds.add(dup.id)
                                    val idx = currentList.indexOfFirst { it.id == target.id }
                                    if (idx >= 0) currentList.add(idx, dup) else currentList.add(0, dup)
                                }
                                val updated = if (activeGroupId == null) {
                                    doc.copy(layers = currentList)
                                } else {
                                    doc.updateLayer(activeGroupId!!) { it.copy(children = currentList) }
                                }
                                selectedLayerIds = newSelectedIds
                                val label = if (targets.size == 1) "Duplicate Layer" else "Duplicate ${targets.size} layers"
                                recordAndAutoSave(label, updated)
                            }
                        },
                        onDelete = {
                            val count = selectedLayerIds.size
                            val updated = selectedLayerIds.fold(doc) { d, id -> d.removeLayer(id) }
                            selectedLayerIds = emptySet()
                            recordAndAutoSave("Delete Layers", updated)
                            if (count > 0) {
                                Toast.makeText(context, if (count == 1) "Deleted 1 layer" else "Deleted $count layers", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Transform Bottom Sheet
                if (isTransformPanelOpen && currentSelectedLayer != null) {
                    PanelPage("Transform", onBack = {
                        externalSmartGuides = emptyList()
                        externalSnappedGridX = null
                        externalSnappedGridY = null
                        isTransformPanelOpen = false
                    }) {
                        val targetId = currentSelectedLayer.id
                        val bounds = selectionBounds(context, doc, currentSelectedLayer)
                        val otherLayers = doc.activeLayers
                            .filter { it.id != targetId && it.isVisible && it.type != LayerType.ADJUSTMENT }
                            .map { (selectionBounds(context, doc, it)) to it.transform }
                        val otherAngles = doc.activeLayers.filter { it.id != targetId }.map { it.transform.rotationDegrees }
                        val snapCtx = TransformSnapContext(
                            bounds = bounds,
                            otherLayers = otherLayers,
                            otherAngles = otherAngles,
                            canvasWidth = doc.canvasSize.width.toFloat(),
                            canvasHeight = doc.canvasSize.height.toFloat(),
                            effectiveZoom = zoom,
                            snapEnabled = doc.editorSettings.snapEnabled,
                            snapToGrid = doc.editorSettings.snapToGrid,
                            showGrid = doc.editorSettings.showGrid,
                            gridSize = doc.editorSettings.gridSize,
                            isSquareGrid = doc.editorSettings.isSquareGrid,
                            tolerancePx = doc.editorSettings.snapTolerancePx,
                            onGuidesChange = { guides, gridX, gridY ->
                                externalSmartGuides = guides
                                externalSnappedGridX = gridX
                                externalSnappedGridY = gridY
                            }
                        )
                        TransformPanel(
                            transform = currentSelectedLayer.transform,
                            snapContext = snapCtx,
                            onTransformChange = { previewT ->
                                projectDocument = doc.updateLayer(currentSelectedLayer.id) { it.copy(transform = previewT) }
                            },
                            onTransformCommit = { finalT ->
                                val updated = doc.updateLayer(currentSelectedLayer.id) { it.copy(transform = finalT) }
                                recordAndAutoSave("Transform Layer", updated)
                            },
                            onClose = {
                                externalSmartGuides = emptyList()
                                externalSnappedGridX = null
                                externalSnappedGridY = null
                                isTransformPanelOpen = false
                            }
                        )
                    }
                }

                // Effects Bottom Sheet
                if (isEffectsSheetOpen && currentSelectedLayer != null) {
                    PanelPage("Effects", onBack = { isEffectsSheetOpen = false }, scrollable = false) {
                        EffectSettingsSheet(
                            targetLayer = currentSelectedLayer,
                            allLayers = doc.activeLayers,
                            onAddEffect = { effect ->
                                val def = EffectRegistry.getEffectDefinition(effect.effectId)
                                val identity = ActionIdentity("Add", def?.name ?: "Effect")
                                recordCommandAndAutoSave(identity) { latestDoc ->
                                    latestDoc.updateLayer(currentSelectedLayer.id) {
                                        it.copy(effects = it.effects + effect)
                                    }
                                }
                            },
                            onRemoveEffect = { effectId ->
                                val def = EffectRegistry.getEffectDefinition(effectId)
                                val identity = ActionIdentity("Remove", def?.name ?: "Effect")
                                recordCommandAndAutoSave(identity) { latestDoc ->
                                    latestDoc.updateLayer(currentSelectedLayer.id) {
                                        it.copy(effects = it.effects.filter { e -> e.effectId != effectId })
                                    }
                                }
                            },
                            onUpdateEffect = { identity, updatedEffect ->
                                recordCommandAndAutoSave(identity) { latestDoc ->
                                    latestDoc.updateLayer(currentSelectedLayer.id) {
                                        it.copy(effects = it.effects.map { e -> if (e.effectId == updatedEffect.effectId) updatedEffect else e })
                                    }
                                }
                            },
                            onClose = { isEffectsSheetOpen = false }
                        )
                    }
                }
            }
        )
    }

    // Rasterize Confirmation Dialog (destructive/irreversible operation allowed exception)
    val layerToRasterizeTarget = layerToRasterize
    if (layerToRasterizeTarget != null) {
        AlertDialog(
            onDismissRequest = { layerToRasterize = null },
            title = { Text("Rasterize Layer") },
            text = {
                Text(
                    "Rasterizing '${layerToRasterizeTarget.name}' converts vector or text geometry into a bitmap graphic at canvas resolution.\n\n" +
                    "Do you want to duplicate the original layer before rasterizing, or replace it directly?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    val target = layerToRasterizeTarget
                    layerToRasterize = null
                    val currentDoc = projectDocument ?: return@Button
                    val updated = com.motionssalt.saltygraphics.engine.model.LayerRasterizer.rasterizeLayer(
                        context = context,
                        document = currentDoc,
                        layerId = target.id,
                        duplicateOriginal = false
                    )
                    val label = "Rasterize ${target.type.name.lowercase()}"
                    recordAndAutoSave(label, updated)
                    fillMediaRevision++
                }) {
                    Text("Rasterize")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { layerToRasterize = null }) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        val target = layerToRasterizeTarget
                        layerToRasterize = null
                        val currentDoc = projectDocument ?: return@Button
                        val updated = com.motionssalt.saltygraphics.engine.model.LayerRasterizer.rasterizeLayer(
                            context = context,
                            document = currentDoc,
                            layerId = target.id,
                            duplicateOriginal = true
                        )
                        val label = "Duplicate and rasterize ${target.type.name.lowercase()}"
                        recordAndAutoSave(label, updated)
                        fillMediaRevision++
                    }) {
                        Text("Duplicate & Rasterize")
                    }
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
                    restoreHistoryDocument(restored)
                }
            },
            onDismiss = { isActionTreeOpen = false }
        )
    }

    if (exitSaveError != null) {
        AlertDialog(
            onDismissRequest = { exitSaveError = null },
            title = { Text("Save Error") },
            text = { Text("Could not save project before exit. Your changes are preserved in memory. Retry saving before leaving so you do not lose work.\n\n$exitSaveError") },
            confirmButton = {
                Button(onClick = { requestSafeExit() }) {
                    Text("Retry Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { exitSaveError = null }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}
