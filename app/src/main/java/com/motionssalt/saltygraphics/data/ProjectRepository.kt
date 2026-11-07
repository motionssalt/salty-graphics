package com.motionssalt.saltygraphics.data

import android.content.Context
import com.motionssalt.saltygraphics.engine.action.ActionTreeSnapshot
import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
import com.motionssalt.saltygraphics.engine.file.SgImportResult
import com.motionssalt.saltygraphics.engine.file.ProjectLoader
import com.motionssalt.saltygraphics.engine.file.ProjectLoadState
import com.motionssalt.saltygraphics.engine.file.AtomicFiles
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.QuarantinedLayerData
import com.motionssalt.saltygraphics.engine.model.SelectionGeometry
import com.motionssalt.saltygraphics.media.MediaCache
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.render.ProjectThumbnailRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@Serializable
data class ProjectSummary(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val layerCount: Int,
    val modifiedAt: Long
)

class ProjectRepository(private val context: Context,
    private val loader: ProjectLoader = ProjectLoader(),
    private val decodeMedia: (Context,String,String) -> Boolean = { c,id,path -> MediaCache.getOrLoad(c,id,path)!=null },
    private val renderThumbnail: (Context,ProjectDocument) -> ByteArray = ProjectThumbnailRenderer::renderToPng
) {
    private val loadingProjects=ConcurrentHashMap<String,CompletableDeferred<Unit>>()
    private val timedOutMedia=ConcurrentHashMap<String,String>()
    private val saveMutexes=ConcurrentHashMap<String,Mutex>()
    private val thumbnailJobs=ConcurrentHashMap<String,Job>()
    private val backgroundScope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val indexLock=Any()
    fun isProjectLoading(id: String): Boolean = loadingProjects.containsKey(id)

    private fun scheduleThumbnail(doc: ProjectDocument) {
        thumbnailJobs.remove(doc.id)?.cancel()
        if(isProjectLoading(doc.id)) return
        thumbnailJobs[doc.id]=backgroundScope.launch {
            try {
                if(isProjectLoading(doc.id)) return@launch
                val bytes=renderThumbnail(context,doc)
                ensureActive()
                if(!isProjectLoading(doc.id)) AtomicFiles.write(getThumbnailFile(doc.id)) {it.write(bytes)}
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) {LocalDiagnostics.record(context,"thumbnail failed ${e.javaClass.simpleName}")}
        }
    }

    /** Explicit foreground state machine; neither thumbnails nor autosave are awaited. */
    suspend fun openProject(id: String, safeMode: Boolean = false,
                            onProgress: (ProjectLoadState.Loading)->Unit = {}): ProjectLoadState {
        val gate=CompletableDeferred<Unit>()
        if(loadingProjects.putIfAbsent(id,gate)!=null) return ProjectLoadState.Error("This project is already opening. Please retry.")
        thumbnailJobs.remove(id)?.cancel()
        val decodingLayer=java.util.concurrent.atomic.AtomicReference<String?>(null)
        val start=LocalDiagnostics.clockMillis()
        var previous=start
        fun logPhase(label:String) {
            val now=LocalDiagnostics.clockMillis()
            LocalDiagnostics.record(context,"project_load phase=$label elapsed_ms=${now-start} previous_phase_ms=${now-previous}")
            previous=now
        }
        return try {
            val state=loader.load(safeMode,{ logPhase(it.phase);onProgress(it) }) { load ->
                load.phase("Reading archive",1)
                val file=getProjectFile(id)
                val backup=getBackupFile(id)
                if ((!file.isFile || file.length() == 0L) && backup.isFile && backup.length() > 0L) {
                    try {
                        backup.copyTo(file, overwrite = true)
                        LocalDiagnostics.record(context, "openProject restored $id from backup")
                    } catch (_: Throwable) {}
                }
                require(file.isFile) {"Project file is missing. Restore a saved .sg file or go back."}
                val imported = try {
                    SgProjectArchive.loadFromFile(file)
                } catch (e: Exception) {
                    if (backup.isFile && backup.length() > 0L) {
                        try {
                            val recovered = SgProjectArchive.loadFromFile(backup)
                            backup.copyTo(file, overwrite = true)
                            LocalDiagnostics.record(context, "openProject fallback to backup for $id")
                            recovered
                        } catch (_: Exception) {
                            throw e
                        }
                    } else {
                        throw e
                    }
                }
                load.phase("Restoring assets",2)
                val assetDir=getAssetsDir(id).canonicalFile
                for((path,bytes) in imported.assets) {
                    load.checkActive()
                    val target=File(assetDir,path).canonicalFile
                    require(target.path.startsWith(assetDir.path+File.separator)) {"Unsafe asset path in project"}
                    if(!target.exists()) AtomicFiles.write(target) {out->load.checkActive();out.write(bytes)}
                }
                load.phase("Checking layers and media",3)
                fun validate(layer:Layer):Layer {
                    load.checkActive()
                    return try {
                        require(layer.type!=LayerType.QUARANTINED) {layer.quarantinedData?.error ?: "Corrupt layer"}
                        require(SelectionGeometry.valid(layer.transform)) {"Invalid layer transform"}
                        if(layer.type==LayerType.MEDIA) {
                            val path=layer.mediaAssetPath ?: error("Missing media reference")
                            val asset=File(assetDir,path.removePrefix("assets/")).canonicalFile
                            require(asset.path.startsWith(assetDir.path+File.separator) && asset.isFile) {"Missing media asset"}
                            require(!safeMode || timedOutMedia[id]!=layer.id) {"Media timed out during the previous attempt"}
                            decodingLayer.set(layer.id)
                            val decoded=decodeMedia(context,id,path)
                            load.checkActive()
                            decodingLayer.set(null)
                            require(decoded) {"Cannot decode media asset"}
                        }
                        layer.copy(children=layer.children.map(::validate))
                    } catch(e: Exception) {
                        load.checkActive()
                        if(!safeMode) throw IllegalArgumentException("${layer.name}: ${e.message}. Try safe mode.",e)
                        LocalDiagnostics.record(context,"project_load quarantined_layer reason=${e.javaClass.simpleName}")
                        layer.copy(type=LayerType.QUARANTINED,isVisible=false,children=emptyList(),
                            quarantinedData=layer.quarantinedData ?: QuarantinedLayerData(json.encodeToString(layer),e.message ?: "Load failure",layer.id,layer.name))
                    }
                }
                require(imported.document.canvasSize.width>0 && imported.document.canvasSize.height>0) {"Invalid canvas dimensions"}
                val document=imported.document.copy(id=id,layers=imported.document.layers.map(::validate))
                load.phase("Ready",4)
                imported.copy(document=document,historySnapshot=if(safeMode) null else imported.historySnapshot)
            }
            logPhase(if(state is ProjectLoadState.Loaded) "Loaded" else "Error")
            if(state is ProjectLoadState.Error) {
                decodingLayer.get()?.let {timedOutMedia[id]=it}
                LocalDiagnostics.record(context,"project_load error=${state.message}")
            } else if(!safeMode) timedOutMedia.remove(id)
            state
        } finally {
            loadingProjects.remove(id,gate);gate.complete(Unit)
        }
    }

    private val projectsDir = File(context.filesDir, "projects").apply { mkdirs() }
    private val thumbnailsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
    private val indexFile = File(projectsDir, "index.json")

    private val _projects = MutableStateFlow<List<ProjectSummary>>(emptyList())
    val projects: StateFlow<List<ProjectSummary>> = _projects.asStateFlow()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun getProjectFile(id: String): File = File(projectsDir, "$id.sg")
    fun getBackupFile(id: String): File = File(projectsDir, "$id.sg.bak")
    fun getThumbnailFile(id: String): File = File(thumbnailsDir, "$id.png")
    fun getAssetsDir(id: String): File = File(projectsDir, "${id}_assets").apply { mkdirs() }

    fun getProjectAssets(id: String): Map<String, ByteArray> {
        val dir = File(projectsDir, "${id}_assets")
        if (!dir.exists() || !dir.isDirectory) return emptyMap()
        val result = mutableMapOf<String, ByteArray>()
        dir.walkTopDown().filter { it.isFile }.forEach { file ->
            val relPath = file.relativeTo(dir).path.replace('\\', '/')
            result[relPath] = file.readBytes()
        }
        return result
    }

    suspend fun loadProjects() = withContext(Dispatchers.IO) {
        try {
            val list = if (indexFile.exists()) {
                try {
                    val content = indexFile.readText()
                    json.decodeFromString<List<ProjectSummary>>(content)
                } catch (_: Throwable) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            // If main .sg is missing or truncated, restore from .sg.bak
            val bakFiles = projectsDir.listFiles { _, name -> name.endsWith(".sg.bak") && !name.startsWith(".") } ?: emptyArray()
            for (bak in bakFiles) {
                val projId = bak.name.removeSuffix(".sg.bak")
                val mainFile = getProjectFile(projId)
                if (!mainFile.exists() || mainFile.length() == 0L) {
                    try {
                        bak.copyTo(mainFile, overwrite = true)
                        LocalDiagnostics.record(context, "loadProjects restored $projId from backup")
                    } catch (_: Throwable) {}
                }
            }

            // Rebuild / merge index from project files on disk
            val knownIds = list.map { it.id }.toMutableSet()
            val recoveredSummaries = mutableListOf<ProjectSummary>()
            val allSgFiles = projectsDir.listFiles { _, name -> name.endsWith(".sg") && !name.startsWith(".") } ?: emptyArray()
            for (file in allSgFiles) {
                val projId = file.name.removeSuffix(".sg")
                if (projId !in knownIds) {
                    try {
                        val imported = SgProjectArchive.loadFromFile(file)
                        val doc = imported.document
                        val summary = ProjectSummary(
                            id = doc.id.ifBlank { projId },
                            name = if (doc.name.isNotBlank()) "Recovered - ${doc.name}" else "Recovered Project ($projId)",
                            width = doc.canvasSize.width,
                            height = doc.canvasSize.height,
                            layerCount = doc.layers.size,
                            modifiedAt = file.lastModified()
                        )
                        recoveredSummaries.add(summary)
                        knownIds.add(projId)
                    } catch (_: Throwable) {
                        val bak = getBackupFile(projId)
                        if (bak.exists() && bak.length() > 0L) {
                            try {
                                val imported = SgProjectArchive.loadFromFile(bak)
                                bak.copyTo(file, overwrite = true)
                                val doc = imported.document
                                val summary = ProjectSummary(
                                    id = doc.id.ifBlank { projId },
                                    name = if (doc.name.isNotBlank()) "Recovered - ${doc.name}" else "Recovered Project ($projId)",
                                    width = doc.canvasSize.width,
                                    height = doc.canvasSize.height,
                                    layerCount = doc.layers.size,
                                    modifiedAt = file.lastModified()
                                )
                                recoveredSummaries.add(summary)
                                knownIds.add(projId)
                            } catch (_: Throwable) {
                                recoveredSummaries.add(ProjectSummary(
                                    id = projId,
                                    name = "Recovered ($projId)",
                                    width = 1080,
                                    height = 1080,
                                    layerCount = 0,
                                    modifiedAt = file.lastModified()
                                ))
                                knownIds.add(projId)
                            }
                        } else {
                            recoveredSummaries.add(ProjectSummary(
                                id = projId,
                                name = "Recovered ($projId)",
                                width = 1080,
                                height = 1080,
                                layerCount = 0,
                                modifiedAt = file.lastModified()
                            ))
                            knownIds.add(projId)
                        }
                    }
                }
            }

            val merged = (list + recoveredSummaries).sortedByDescending { it.modifiedAt }
            _projects.value = merged
            if (recoveredSummaries.isNotEmpty() || !indexFile.exists()) {
                saveIndex(merged)
            }

            for(proj in _projects.value) {
                if(!getThumbnailFile(proj.id).exists() && !isProjectLoading(proj.id)) {
                    backgroundScope.launch {
                        if(!isProjectLoading(proj.id)) getProject(proj.id)?.let {scheduleThumbnail(it)}
                    }
                }
            }
        } catch (e: Throwable) {
            LocalDiagnostics.record(context, "loadProjects failed: ${e.message}")
        }
    }

    private fun saveIndex(list: List<ProjectSummary>) = synchronized(indexLock) {
        try {
            AtomicFiles.write(indexFile) { it.write(json.encodeToString(list).toByteArray()) }
            _projects.value = list.sortedByDescending { it.modifiedAt }
        } catch (e: Throwable) {
            _projects.value = list
        }
    }

    suspend fun createProject(name: String, canvasSize: CanvasSize): ProjectDocument = withContext(NonCancellable + Dispatchers.IO) {
        val doc = ProjectDocument(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Untitled Project" },
            canvasSize = canvasSize
        )
        val file = getProjectFile(doc.id)
        val previewBytes = ProjectThumbnailRenderer.renderToPng(context, doc)
        File(thumbnailsDir, "${doc.id}.png").writeBytes(previewBytes)

        SgProjectArchive.saveToFile(
            document = doc,
            file = file,
            assetFiles = emptyMap(),
            previewPng = previewBytes
        )

        val backupFile = getBackupFile(doc.id)
        if (file.exists() && file.length() > 0L) {
            try { file.copyTo(backupFile, overwrite = true) } catch (_: Throwable) {}
        }

        val summary = ProjectSummary(
            id = doc.id,
            name = doc.name,
            width = doc.canvasSize.width,
            height = doc.canvasSize.height,
            layerCount = doc.layers.size,
            modifiedAt = doc.updatedAt
        )
        synchronized(indexLock) {
            val current = _projects.value
            val updated = if (current.any { it.id == doc.id }) {
                current.map { if (it.id == doc.id) summary else it }
            } else {
                current + summary
            }
            saveIndex(updated)
        }
        doc
    }

    suspend fun getProject(id: String): ProjectDocument? = withContext(Dispatchers.IO) {
        val file = getProjectFile(id)
        val backup = getBackupFile(id)
        if (!file.exists() && backup.exists()) {
            try { backup.copyTo(file, overwrite = true) } catch (_: Throwable) {}
        }
        if (!file.exists()) return@withContext null
        try {
            val result = SgProjectArchive.loadFromFile(file)
            result.document
        } catch (e: Exception) {
            if (backup.exists() && backup.length() > 0L) {
                try {
                    val result = SgProjectArchive.loadFromFile(backup)
                    backup.copyTo(file, overwrite = true)
                    result.document
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    suspend fun getProjectFull(id: String): com.motionssalt.saltygraphics.engine.file.SgImportResult? = withContext(Dispatchers.IO) {
        val file = getProjectFile(id)
        val backup = getBackupFile(id)
        if (!file.exists() && backup.exists()) {
            try { backup.copyTo(file, overwrite = true) } catch (_: Throwable) {}
        }
        if (!file.exists()) return@withContext null
        try {
            SgProjectArchive.loadFromFile(file)
        } catch (_: Exception) {
            if (backup.exists() && backup.length() > 0L) {
                try {
                    val res = SgProjectArchive.loadFromFile(backup)
                    backup.copyTo(file, overwrite = true)
                    res
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    suspend fun saveProject(doc: ProjectDocument, historySnapshot: ActionTreeSnapshot? = null) = withContext(NonCancellable + Dispatchers.IO) {
        loadingProjects[doc.id]?.await()
        saveMutexes.getOrPut(doc.id) {Mutex()}.withLock {
            // A foreground open can begin while this save waited for an earlier save.
            loadingProjects[doc.id]?.await()
            val assets=getProjectAssets(doc.id)
            val thumbnail=getThumbnailFile(doc.id).let {if(it.exists()) it.readBytes() else null}
            val projFile = getProjectFile(doc.id)
            val backupFile = getBackupFile(doc.id)
            if (projFile.exists() && projFile.length() > 0L) {
                try {
                    projFile.copyTo(backupFile, overwrite = true)
                } catch (_: Throwable) {}
            }
            // Essential document storage comes first; thumbnail rendering is independent work.
            SgProjectArchive.saveToFile(doc, projFile, assets, historySnapshot, thumbnail)
            if (!backupFile.exists() && projFile.exists() && projFile.length() > 0L) {
                try { projFile.copyTo(backupFile, overwrite = true) } catch (_: Throwable) {}
            }
            synchronized(indexLock) {
                val current = _projects.value
                val summary = ProjectSummary(
                    id = doc.id,
                    name = doc.name,
                    width = doc.canvasSize.width,
                    height = doc.canvasSize.height,
                    layerCount = doc.layers.size,
                    modifiedAt = System.currentTimeMillis()
                )
                val updated = if (current.any { it.id == doc.id }) {
                    current.map { if (it.id == doc.id) summary else it }
                } else {
                    current + summary
                }
                saveIndex(updated)
            }
            scheduleThumbnail(doc)
        }
    }

    fun saveProjectInBackground(doc: ProjectDocument, historySnapshot: ActionTreeSnapshot? = null): Job =
        backgroundScope.launch(NonCancellable) {
            try {
                saveProject(doc, historySnapshot)
            } catch (e: Exception) {
                LocalDiagnostics.record(context, "background save failed ${e.javaClass.simpleName}")
            }
        }

    suspend fun duplicateProject(id: String) = withContext(Dispatchers.IO) {
        val existing = getProject(id) ?: return@withContext
        val newId = UUID.randomUUID().toString()
        val duplicatedDoc = existing.copy(
            id = newId,
            name = "${existing.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Duplicate asset folder
        val srcAssets = File(projectsDir, "${id}_assets")
        val dstAssets = File(projectsDir, "${newId}_assets")
        if (srcAssets.exists()) {
            srcAssets.copyRecursively(dstAssets, overwrite = true)
        }

        // Duplicate thumbnail
        ProjectThumbnailRenderer.duplicateThumbnail(context, id, newId)
        val previewBytes = File(thumbnailsDir, "$newId.png").let { if (it.exists()) it.readBytes() else null }

        val file = File(projectsDir, "$newId.sg")
        SgProjectArchive.saveToFile(
            document = duplicatedDoc,
            file = file,
            assetFiles = getProjectAssets(newId),
            previewPng = previewBytes
        )

        val summary = ProjectSummary(
            id = duplicatedDoc.id,
            name = duplicatedDoc.name,
            width = duplicatedDoc.canvasSize.width,
            height = duplicatedDoc.canvasSize.height,
            layerCount = duplicatedDoc.layers.size,
            modifiedAt = duplicatedDoc.updatedAt
        )
        saveIndex(_projects.value + summary)
    }

    suspend fun renameProject(id: String, newName: String) = withContext(Dispatchers.IO) {
        val existing = getProject(id) ?: return@withContext
        val updatedDoc = existing.copy(name = newName, updatedAt = System.currentTimeMillis())
        saveProject(updatedDoc)
    }

    suspend fun deleteProjects(ids: Set<String>) = withContext(Dispatchers.IO) {
        for (id in ids) {
            try { File(projectsDir, "$id.sg").delete() } catch (_: Throwable) {}
            try { File(projectsDir, "$id.sg.bak").delete() } catch (_: Throwable) {}
            try { File(projectsDir, "${id}_assets").deleteRecursively() } catch (_: Throwable) {}
            ProjectThumbnailRenderer.deleteThumbnail(context, id)
        }
        saveIndex(_projects.value.filter { it.id !in ids })
    }

    suspend fun importProject(inputStream: InputStream): ProjectDocument = withContext(Dispatchers.IO) {
        val result = SgProjectArchive.importFromStream(inputStream)
        val importedDoc = result.document
        val assets = result.assets

        val newId = UUID.randomUUID().toString()

        // S2: Importing the same project twice creates a copy with a new name, not an overwrite
        val existingNames = _projects.value.map { it.name.lowercase().trim() }
        val baseName = importedDoc.name.ifBlank { "Imported Project" }
        val uniqueName = if (existingNames.contains(baseName.lowercase().trim())) {
            var copyNum = 1
            var candidate = "$baseName (Copy $copyNum)"
            while (existingNames.contains(candidate.lowercase().trim())) {
                copyNum++
                candidate = "$baseName (Copy $copyNum)"
            }
            candidate
        } else {
            baseName
        }

        val doc = importedDoc.copy(
            id = newId,
            name = uniqueName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Extract assets to assets folder
        if (assets.isNotEmpty()) {
            val assetsDir = getAssetsDir(newId)
            for ((relPath, data) in assets) {
                val assetFile = File(assetsDir, relPath)
                assetFile.parentFile?.mkdirs()
                assetFile.writeBytes(data)
            }
        }

        // Save preview thumbnail
        val preview = result.previewPng
        if (preview != null && preview.isNotEmpty()) {
            File(thumbnailsDir, "$newId.png").writeBytes(preview)
        } else {
            ProjectThumbnailRenderer.saveThumbnail(context, doc)
        }

        val file = File(projectsDir, "$newId.sg")
        SgProjectArchive.saveToFile(
            document = doc,
            file = file,
            assetFiles = assets,
            historySnapshot = result.historySnapshot,
            previewPng = result.previewPng
        )

        val summary = ProjectSummary(
            id = doc.id,
            name = doc.name,
            width = doc.canvasSize.width,
            height = doc.canvasSize.height,
            layerCount = doc.layers.size,
            modifiedAt = doc.updatedAt
        )
        saveIndex(_projects.value + summary)
        doc
    }

    suspend fun exportProjectToStream(id: String, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val file = File(projectsDir, "$id.sg")
        if (file.exists()) {
            file.inputStream().use { it.copyTo(outputStream) }
        }
    }
}
