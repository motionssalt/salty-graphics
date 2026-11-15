package com.motionssalt.saltygraphics.ui.editor

import com.motionssalt.saltygraphics.uikit.structure.SaltyToastManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.action.GlobalClipboard
import com.motionssalt.saltygraphics.engine.effect.EffectRegistry
import com.motionssalt.saltygraphics.engine.file.ProjectLoadState
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.media.MediaCache
import com.motionssalt.saltygraphics.media.MediaImporter
import com.motionssalt.saltygraphics.ui.controls.BorderShadowPanelPage
import com.motionssalt.saltygraphics.ui.controls.FillPanelPage
import android.content.Context
import com.motionssalt.saltygraphics.uikit.registry.ExampleTemplateFeature
import com.motionssalt.saltygraphics.uikit.registry.SaltyBuiltInLibraryRegistry
import com.motionssalt.saltygraphics.uikit.registry.ScaffoldFeaturePage
import com.motionssalt.saltygraphics.uikit.structure.SaltyConfirmDialog
import com.motionssalt.saltygraphics.uikit.structure.SaltyEditorScaffold
import com.motionssalt.saltygraphics.uikit.sublibs.bottompanel.SaltyEditorWorkspaceContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditorScreen(
    projectId: String,
    initialGroupId: String? = null,
    repository: ProjectRepository,
    onNavigateBack: () -> Unit,
    onNavigateToExport: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    SaltyBuiltInLibraryRegistry.ensureRegistered()
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
    var isCanvasLocked by rememberSaveable { mutableStateOf(false) }
    var externalSmartGuides by remember { mutableStateOf<List<SmartGuideLine>>(emptyList()) }
    var externalSnappedGridX by remember { mutableStateOf<Float?>(null) }
    var externalSnappedGridY by remember { mutableStateOf<Float?>(null) }

    // Panel visibility states
    var isLayersPanelOpen by remember { mutableStateOf(false) }
    var isTransformPanelOpen by remember { mutableStateOf(false) }
    var nudgeStepPx by remember { mutableFloatStateOf(1f) }
    var lockedNudgeNotified by remember { mutableStateOf(false) }
    var isEffectsSheetOpen by remember { mutableStateOf(false) }
    var isShapeSettingsOpen by remember { mutableStateOf(false) }
    var isTextSettingsOpen by remember { mutableStateOf(false) }
    var isBlendingOpen by remember { mutableStateOf(false) }
    var isFillPanelOpen by remember { mutableStateOf(false) }
    var isBorderShadowPanelOpen by remember { mutableStateOf(false) }

    var isProjectSettingsOpen by remember { mutableStateOf(false) }
    var isVectorMenuOpen by remember { mutableStateOf(false) }
    var isVectorSettingsOpen by remember { mutableStateOf(false) }
    var isShapePickerOpen by remember { mutableStateOf(false) }
    var vectorTool by remember { mutableStateOf(VectorTool.SELECT) }
    var vectorPathIndex by remember { mutableIntStateOf(0) }
    var vectorPointIndex by remember { mutableStateOf<Int?>(null) }
    var vectorDraft by remember { mutableStateOf<Layer?>(null) }
    var isActionTreeOpen by remember { mutableStateOf(false) }
    var isTemplateExampleOpen by remember { mutableStateOf(false) }
    val devPrefs = remember(context) { context.getSharedPreferences("salty_dev_prefs", Context.MODE_PRIVATE) }
    val devOptionsEnabled = remember(devPrefs) { devPrefs.getBoolean("dev_options", false) }
    val debugEffectsEnabled = remember(devPrefs) { devPrefs.getBoolean("debug_effects", false) }
    SideEffect { EditorToolFeatures.setDeveloperModeEnabled(devOptionsEnabled) }

    val loadState = remember(projectId) { MutableStateFlow<ProjectLoadState>(ProjectLoadState.Loading()) }
    val loading by loadState.collectAsState()
    var loadAttempt by remember(projectId) { mutableIntStateOf(0) }
    var safeMode by remember(projectId) { mutableStateOf(false) }
    LaunchedEffect(projectId, loadAttempt) {
        projectDocument = null; actionTree = null
        val result = repository.openProject(projectId, safeMode) { loadState.value = it }
        loadState.value = result
        if (result is ProjectLoadState.Loaded) {
            val loaded = result.result.document
            projectDocument = loaded
            actionTree = result.result.historySnapshot?.let { ActionTree.fromSnapshot(it, loaded) } ?: ActionTree(loaded)
        }
    }

    val doc = projectDocument
    val tree = remember(actionTree, historyVersion) { actionTree }

    var exitSaveError by remember { mutableStateOf<String?>(null) }

    fun requestSafeExit() {
        val currentDoc = projectDocument
        val snapshot = actionTree?.toSnapshot()
        if (currentDoc != null) {
            exitSaveError = null
            coroutineScope.launch {
                try {
                    repository.saveProject(currentDoc, snapshot)
                    onNavigateBack()
                } catch (e: Exception) {
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

    DisposableEffect(Unit) {
        onDispose { SaltyToastManager.clear() }
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
            }.onFailure { SaltyToastManager.show(it.message ?: "Invalid SVG") }
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val currentDoc = projectDocument ?: return@rememberLauncherForActivityResult
        if (uris.isNotEmpty()) {
            SaltyToastManager.show("Optimizing ${uris.size} image(s)...")
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
                SaltyToastManager.show("Image${if (count > 1) "s" else ""} optimized and imported")
            }
            if (result.failureReasons.isNotEmpty()) {
                SaltyToastManager.show(result.failureReasons.first())
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
            SaltyToastManager.show("Optimizing image…")
            val result = withContext(Dispatchers.IO) {
                MediaImporter.importMediaFromUris(context, current.id, listOf(uri), current.canvasSize.width, current.canvasSize.height)
            }
            val asset = result.importedLayers.firstOrNull()?.mediaAssetPath
            if (asset != null) {
                recordCommandAndAutoSave(ActionIdentity("Replace", "Layer", "media fill")) { live ->
                    live.updateLayer(targetId) { layer -> layer.copy(fillData = layer.effectiveFill.copy(type = FillType.MEDIA, mediaAssetPath = asset)) }
                }
                fillMediaRevision++
                SaltyToastManager.show("Image optimized and imported")
            } else SaltyToastManager.show(result.failureReasons.firstOrNull() ?: "Could not import image")
        }
    }
    fun mediaReferences(layers: List<Layer>): Set<String> = layers.flatMap { layer ->
        listOfNotNull(layer.mediaAssetPath, layer.fillData?.mediaAssetPath) + mediaReferences(layer.children)
    }.toSet()
    val referencedMedia = mediaReferences(projectDocument?.layers ?: emptyList())
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
                    SaltyToastManager.show("Saved .sg project file successfully")
                } catch (e: Exception) {
                    SaltyToastManager.show("Failed to save project: ${e.message}")
                }
            }
        }
    }

    BackHandler {
        if (exitSaveError != null) {
            exitSaveError = null
        } else if (isVectorMenuOpen || isVectorSettingsOpen || isProjectSettingsOpen || isTransformPanelOpen || isEffectsSheetOpen || isShapeSettingsOpen || isTextSettingsOpen || isBlendingOpen || isFillPanelOpen || isBorderShadowPanelOpen || isTemplateExampleOpen) {
            isVectorMenuOpen = false; isVectorSettingsOpen = false; vectorDraft = null
            isProjectSettingsOpen = false; isTransformPanelOpen = false; isEffectsSheetOpen = false; isShapeSettingsOpen = false; isTextSettingsOpen = false; isBlendingOpen = false; isFillPanelOpen = false; isBorderShadowPanelOpen = false; isTemplateExampleOpen = false
        } else if (activeGroupId != null) {
            val crumbs = groupBreadcrumbs
            activeGroupId = if (crumbs.size > 1) crumbs[crumbs.size - 2].first else null
        } else {
            requestSafeExit()
        }
    }

    if (doc == null || tree == null) {
        ProjectLoadingPanel(
            loading,
            onRetry = { safeMode = false; loadState.value = ProjectLoadState.Loading(); loadAttempt++ },
            onSafeMode = { safeMode = true; loadState.value = ProjectLoadState.Loading(); loadAttempt++ },
            onBack = onNavigateBack
        )
        return
    }

    val panelLayers = LayerStackCommands.layers(doc, activeGroupId)
    val currentSelectedLayer = doc.findLayerById(selectedLayerIds.firstOrNull() ?: "")

    LaunchedEffect(currentSelectedLayer?.id, currentSelectedLayer?.type) {
        if (currentSelectedLayer == null) {
            isTransformPanelOpen = false; isEffectsSheetOpen = false
            isBlendingOpen = false; isFillPanelOpen = false
        }
        if (currentSelectedLayer?.shapeData == null) isShapeSettingsOpen = false
        if (currentSelectedLayer?.textData == null) isTextSettingsOpen = false
        if (currentSelectedLayer?.type != LayerType.VECTOR) isVectorSettingsOpen = false
        vectorDraft = null; vectorPointIndex = null
    }

    SaltyEditorScaffold(
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
                        SaltyToastManager.show("Undid $actionName")
                    }
                },
                onRedoClick = {
                    val actionName = tree.redoActionName ?: "action"
                    if (tree.redo()) {
                        restoreHistoryDocument(tree.currentDocument)
                        SaltyToastManager.show("Redid $actionName")
                    }
                },
                onOpenActionTree = { isActionTreeOpen = true },
                onExportClick = { onNavigateToExport(projectId) },
                onSettingsClick = {
                    isVectorMenuOpen = false; isVectorSettingsOpen = false; isTransformPanelOpen = false
                    isEffectsSheetOpen = false; isShapeSettingsOpen = false; isTextSettingsOpen = false
                    isBlendingOpen = false; isFillPanelOpen = false
                    isBorderShadowPanelOpen = false; isTemplateExampleOpen = false; isProjectSettingsOpen = true
                },
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
        }
    ) { contentModifier ->
        SaltyEditorWorkspaceContainer(
            modifier = contentModifier,
            isLayersPanelOpen = isLayersPanelOpen,
            canvasContent = {
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
                            projectDocument?.let { projectDocument = SelectionGeometry.apply(it, id, newTransform) }
                        },
                        onCommitLayerTransform = { id, finalTransform ->
                            val live = projectDocument
                            if (live != null) recordAndAutoSave("Transform Layer", SelectionGeometry.apply(live, id, finalTransform))
                        },
                        onUpdateLayerTransforms = { transforms ->
                            val live = projectDocument
                            if (live != null && transforms.values.all { SelectionGeometry.valid(it) }) {
                                projectDocument = transforms.entries.fold(live) { d, (id, t) -> SelectionGeometry.apply(d, id, t) }
                            }
                        },
                        onCommitLayerTransforms = { transforms ->
                            val live = projectDocument
                            if (live != null && transforms.isNotEmpty() && transforms.values.all { SelectionGeometry.valid(it) }) {
                                val updated = transforms.entries.fold(live) { d, (id, t) -> SelectionGeometry.apply(d, id, t) }
                                if (updated.layers != actionTree?.currentDocument?.layers) recordAndAutoSave("Transform Layer", updated)
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

            },
            overlayContent = {
                    if (isFillPanelOpen && currentSelectedLayer?.effectiveFill?.type == FillType.GRADIENT) {
                        val targetId = currentSelectedLayer.id
                        GradientCanvasOverlay(
                            doc, currentSelectedLayer, zoom, panOffset,
                            onPreview = { fill -> projectDocument = projectDocument?.updateLayer(targetId) { it.withUniversalFill(fill) } },
                            onCommit = { identity, fill ->
                                recordCommandAndAutoSave(identity) { live ->
                                    live.updateLayer(targetId) { it.withUniversalFill(fill) }
                                }
                            },
                            onCancel = { projectDocument = actionTree?.currentDocument }
                        )
                    }

                    if (isVectorSettingsOpen && currentSelectedLayer?.type == LayerType.VECTOR) {
                        VectorCanvasOverlay(
                            currentSelectedLayer, doc.canvasSize, vectorPathIndex, vectorPointIndex, vectorTool,
                            zoom, panOffset, { vectorPointIndex = it }, { vectorDraft = it },
                            { changed -> recordAndAutoSave("Edit vector points", doc.updateLayer(changed.id) { changed }) },
                            { isVectorSettingsOpen = false; vectorDraft = null }
                        )
                    }

            },
            layersPanelContent = {
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
                                        SaltyToastManager.show(if (count == 1) "Deleted 1 layer" else "Deleted $count layers")
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
                                        val currentList = panelLayers.toMutableList()
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
            },
            panelContent = {
                val pageOpen = isVectorMenuOpen || isProjectSettingsOpen ||
                    (isVectorSettingsOpen && currentSelectedLayer?.type == LayerType.VECTOR) ||
                    (isShapeSettingsOpen && currentSelectedLayer?.shapeData != null) ||
                    (isTextSettingsOpen && currentSelectedLayer?.textData != null) ||
                    (currentSelectedLayer != null && (isTransformPanelOpen || isEffectsSheetOpen || isBlendingOpen || isFillPanelOpen || isBorderShadowPanelOpen || isTemplateExampleOpen))
                if (isVectorMenuOpen) {
                    VectorCreationPage(document = doc, onChoose = { choice ->
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
                    VectorPage(
                        currentSelectedLayer, vectorPathIndex, vectorPointIndex, vectorTool,
                        { vectorPathIndex = it }, { vectorPointIndex = it }, { vectorTool = it },
                        { description, layer -> recordAndAutoSave(description, doc.updateLayer(layer.id) { layer }) },
                        { isVectorSettingsOpen = false }
                    )
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
                    BorderShadowPanelPage(
                        document = doc,
                        layerId = currentSelectedLayer.id,
                        onCommit = { change ->
                            val live = tree.currentDocument
                            if (change.appliesTo(live)) recordCommandAndAutoSave(change.identity(live), change::apply)
                        },
                        onPreview = { change ->
                            val live = tree.currentDocument
                            projectDocument = change?.apply(live) ?: live
                        },
                        onBack = { isBorderShadowPanelOpen = false; isTemplateExampleOpen = false }
                    )
                } else if (isShapeSettingsOpen && currentSelectedLayer?.shapeData != null) {
                    ShapeSettingsDialog(
                        document = doc,
                        layerId = currentSelectedLayer.id,
                        onCommit = { change ->
                            val live = tree.currentDocument
                            if (change.appliesTo(live)) recordCommandAndAutoSave(change.identity(live), change::apply)
                        },
                        onPreview = { change ->
                            val live = tree.currentDocument
                            projectDocument = change?.apply(live) ?: live
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
                        document = doc,
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
                        onOpenTemplateExample = { isTemplateExampleOpen = true },
                        onOpenBorderShadow = {
                            if (currentSelectedLayer != null) {
                                isBorderShadowPanelOpen = true
                            }
                        },
                        onDuplicate = {
                            val targets = if (selectedLayerIds.isNotEmpty()) panelLayers.filter { it.id in selectedLayerIds } else listOfNotNull(currentSelectedLayer)
                            if (targets.isNotEmpty()) {
                                val currentList = panelLayers.toMutableList()
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
                                SaltyToastManager.show(if (count == 1) "Deleted 1 layer" else "Deleted $count layers")
                            }
                        }
                    )
                }

                if (isTransformPanelOpen && currentSelectedLayer != null) {
                    val targetId = currentSelectedLayer.id
                    val bounds = selectionBounds(context, doc, currentSelectedLayer)
                    val otherLayers = doc.activeLayers
                        .filter { it.id != targetId && it.isVisible && it.type != LayerType.ADJUSTMENT }
                        .map { selectionBounds(context, doc, it) to it.transform }
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
                    val targetIds = if (selectedLayerIds.isNotEmpty()) selectedLayerIds else setOf(targetId)
                    TransformPanel(
                        transform = currentSelectedLayer.transform,
                        snapContext = snapCtx,
                        onTransformChange = { previewT ->
                            val base = projectDocument ?: doc
                            projectDocument = base.updateLayer(targetId) { it.copy(transform = previewT) }
                        },
                        onTransformCommit = { finalT ->
                            val live = tree.currentDocument
                            val updated = SelectionGeometry.apply(live, targetId, finalT)
                            if (updated != live) {
                                recordCommandAndAutoSave(ActionIdentity("Transform", currentSelectedLayer.name)) { lDoc ->
                                    SelectionGeometry.apply(lDoc, targetId, finalT)
                                }
                            } else {
                                projectDocument = live
                            }
                        },
                        onClose = {
                            externalSmartGuides = emptyList()
                            externalSnappedGridX = null
                            externalSnappedGridY = null
                            isTransformPanelOpen = false
                        },
                        wrapInPanelPage = true,
                        nudgeStepPx = nudgeStepPx,
                        onNudgeStepChange = { nudgeStepPx = it },
                        onNudgeDelta = { dx, dy ->
                            val live = tree.currentDocument
                            val unlocked = targetIds.mapNotNull { id -> live.findLayerById(id) }
                                .filter { !it.isLocked && it.type != LayerType.ADJUSTMENT }
                            if (unlocked.isEmpty()) {
                                if (!lockedNudgeNotified) {
                                    lockedNudgeNotified = true
                                    SaltyToastManager.show("Layer is locked")
                                }
                            } else {
                                val base = projectDocument ?: live
                                projectDocument = CompositionTransforms.nudgeDocument(base, targetIds, dx, dy)
                            }
                        },
                        onNudgeCommit = { totalDx, totalDy ->
                            lockedNudgeNotified = false
                            val live = tree.currentDocument
                            val unlocked = targetIds.mapNotNull { id -> live.findLayerById(id) }
                                .filter { !it.isLocked && it.type != LayerType.ADJUSTMENT }
                            if (unlocked.isNotEmpty() && (totalDx != 0f || totalDy != 0f)) {
                                val targetLabel = if (unlocked.size > 1) "${unlocked.size} layers" else unlocked.first().name
                                recordCommandAndAutoSave(ActionIdentity("Nudge", targetLabel)) { lDoc ->
                                    CompositionTransforms.nudgeDocument(lDoc, targetIds, totalDx, totalDy)
                                }
                            } else {
                                projectDocument = live
                            }
                        },
                        onNudgeCancel = {
                            lockedNudgeNotified = false
                            projectDocument = tree.currentDocument
                        },
                        onQuickTransform = { action ->
                            externalSmartGuides = emptyList()
                            externalSnappedGridX = null
                            externalSnappedGridY = null
                            val live = tree.currentDocument
                            val unlocked = targetIds.mapNotNull { id -> live.findLayerById(id) }
                                .filter { !it.isLocked && it.type != LayerType.ADJUSTMENT }
                            if (unlocked.isEmpty()) {
                                SaltyToastManager.show("Layer is locked")
                            } else {
                                val targetLabel = if (unlocked.size > 1) "${unlocked.size} layers" else unlocked.first().name
                                val updated = CompositionTransforms.applyToDocument(
                                    live,
                                    targetIds,
                                    { l -> selectionBounds(context, live, l) },
                                    action
                                )
                                if (updated != live) {
                                    recordCommandAndAutoSave(ActionIdentity(action.label, targetLabel)) { lDoc ->
                                        CompositionTransforms.applyToDocument(
                                            lDoc,
                                            targetIds,
                                            { l -> selectionBounds(context, lDoc, l) },
                                            action
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                if (isEffectsSheetOpen && currentSelectedLayer != null) {
                    EffectSettingsSheet(
                        targetLayer = currentSelectedLayer,
                        allLayers = doc.activeLayers,
                        includeDebugEffects = debugEffectsEnabled,
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
                        onClose = { isEffectsSheetOpen = false },
                        wrapInPanelPage = true
                    )
                }

                if (isTemplateExampleOpen && currentSelectedLayer != null) {
                    val templateDef = remember { ExampleTemplateFeature.createDefinition() }
                    ScaffoldFeaturePage(
                        feature = templateDef,
                        pageId = "template_main",
                        document = doc,
                        layerId = currentSelectedLayer.id,
                        onPreview = { change ->
                            val base = tree.currentDocument
                            projectDocument = if (change != null && change.appliesTo(base)) change.apply(base) else base
                        },
                        onCommit = { change ->
                            val live = tree.currentDocument
                            if (change.appliesTo(live)) {
                                recordCommandAndAutoSave(change.identity(live), change::apply)
                            }
                        },
                        onBack = { isTemplateExampleOpen = false }
                    )
                }
            }
        )
    }

    val layerToRasterizeTarget = layerToRasterize
    if (layerToRasterizeTarget != null) {
        SaltyConfirmDialog(
            title = "Rasterize Layer",
            message = "Rasterizing '${layerToRasterizeTarget.name}' converts vector or text geometry into a bitmap graphic at canvas resolution.\n\n" +
                "Do you want to duplicate the original layer before rasterizing, or replace it directly?",
            confirmText = "Rasterize",
            dismissText = "Cancel",
            onConfirm = {
                val target = layerToRasterizeTarget
                layerToRasterize = null
                val currentDoc = projectDocument ?: return@SaltyConfirmDialog
                val updated = LayerRasterizer.rasterizeLayer(
                    context = context,
                    document = currentDoc,
                    layerId = target.id,
                    duplicateOriginal = false
                )
                val label = "Rasterize ${target.type.name.lowercase()}"
                recordAndAutoSave(label, updated)
                fillMediaRevision++
            },
            onDismiss = { layerToRasterize = null },
            secondaryConfirmText = "Duplicate & Rasterize",
            onSecondaryConfirm = {
                val target = layerToRasterizeTarget
                layerToRasterize = null
                val currentDoc = projectDocument ?: return@SaltyConfirmDialog
                val updated = LayerRasterizer.rasterizeLayer(
                    context = context,
                    document = currentDoc,
                    layerId = target.id,
                    duplicateOriginal = true
                )
                val label = "Duplicate and rasterize ${target.type.name.lowercase()}"
                recordAndAutoSave(label, updated)
                fillMediaRevision++
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
        SaltyConfirmDialog(
            title = "Save Error",
            message = "Could not save project before exit. Your changes are preserved in memory. Retry saving before leaving so you do not lose work.\n\n$exitSaveError",
            confirmText = "Retry Save",
            dismissText = "Keep Editing",
            onConfirm = { requestSafeExit() },
            onDismiss = { exitSaveError = null }
        )
    }
}
