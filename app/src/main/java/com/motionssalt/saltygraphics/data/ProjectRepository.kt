package com.motionssalt.saltygraphics.data

import android.content.Context
import com.motionssalt.saltygraphics.engine.action.ActionTreeSnapshot
import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
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

class ProjectRepository(private val context: Context) {
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
            if (!indexFile.exists()) {
                _projects.value = emptyList()
                return@withContext
            }
            val content = indexFile.readText()
            val list = json.decodeFromString<List<ProjectSummary>>(content)

            // R3 Migration: on upgrade, remove the seeded sample project if it is still unmodified
            val migrated = list.filterNot { it.name == "My First Salty Design" && it.layerCount == 2 }
            if (migrated.size != list.size) {
                list.filter { it.name == "My First Salty Design" && it.layerCount == 2 }.forEach {
                    try { File(projectsDir, "${it.id}.sg").delete() } catch (_: Throwable) {}
                    try { File(thumbnailsDir, "${it.id}.png").delete() } catch (_: Throwable) {}
                }
                saveIndex(migrated)
            } else {
                _projects.value = list.sortedByDescending { it.modifiedAt }
            }

            // S2b: Background check for missing thumbnails on old projects
            CoroutineScope(Dispatchers.IO).launch {
                for (proj in _projects.value) {
                    val thumb = File(thumbnailsDir, "${proj.id}.png")
                    if (!thumb.exists()) {
                        try {
                            val doc = getProject(proj.id)
                            if (doc != null) {
                                ProjectThumbnailRenderer.saveThumbnail(context, doc)
                            }
                        } catch (_: Throwable) {}
                    }
                }
            }
        } catch (e: Throwable) {
            _projects.value = emptyList()
        }
    }

    private fun saveIndex(list: List<ProjectSummary>) {
        try {
            indexFile.writeText(json.encodeToString(list))
            _projects.value = list.sortedByDescending { it.modifiedAt }
        } catch (e: Throwable) {
            _projects.value = list
        }
    }

    suspend fun createProject(name: String, canvasSize: CanvasSize): ProjectDocument = withContext(Dispatchers.IO) {
        val doc = ProjectDocument(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Untitled Project" },
            canvasSize = canvasSize
        )
        val file = File(projectsDir, "${doc.id}.sg")
        val previewBytes = ProjectThumbnailRenderer.renderToPng(context, doc)
        File(thumbnailsDir, "${doc.id}.png").writeBytes(previewBytes)

        SgProjectArchive.saveToFile(
            document = doc,
            file = file,
            assetFiles = emptyMap(),
            previewPng = previewBytes
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

    suspend fun getProject(id: String): ProjectDocument? = withContext(Dispatchers.IO) {
        val file = File(projectsDir, "$id.sg")
        if (!file.exists()) return@withContext null
        try {
            val result = SgProjectArchive.loadFromFile(file)
            result.document
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getProjectFull(id: String): com.motionssalt.saltygraphics.engine.file.SgImportResult? = withContext(Dispatchers.IO) {
        val file = File(projectsDir, "$id.sg")
        if (!file.exists()) return@withContext null
        try {
            SgProjectArchive.loadFromFile(file)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveProject(doc: ProjectDocument, historySnapshot: ActionTreeSnapshot? = null) = withContext(Dispatchers.IO) {
        try {
            val file = File(projectsDir, "${doc.id}.sg")
            val assets = getProjectAssets(doc.id)
            val previewBytes = try {
                val thumbFile = ProjectThumbnailRenderer.saveThumbnail(context, doc)
                thumbFile.readBytes()
            } catch (_: Throwable) {
                null
            }

            SgProjectArchive.saveToFile(
                document = doc,
                file = file,
                assetFiles = assets,
                historySnapshot = historySnapshot,
                previewPng = previewBytes
            )

            val updated = _projects.value.map {
                if (it.id == doc.id) {
                    it.copy(
                        name = doc.name,
                        width = doc.canvasSize.width,
                        height = doc.canvasSize.height,
                        layerCount = doc.layers.size,
                        modifiedAt = System.currentTimeMillis()
                    )
                } else it
            }
            saveIndex(updated)
        } catch (_: Throwable) {}
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
