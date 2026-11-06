package com.motionssalt.saltygraphics.data

import android.content.Context
import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

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
    private val indexFile = File(projectsDir, "index.json")

    private val _projects = MutableStateFlow<List<ProjectSummary>>(emptyList())
    val projects: StateFlow<List<ProjectSummary>> = _projects.asStateFlow()

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun loadProjects() = withContext(Dispatchers.IO) {
        if (!indexFile.exists()) {
            // Seed with an initial sample project for Victor
            val initial = ProjectSummary(
                id = UUID.randomUUID().toString(),
                name = "My First Salty Design",
                width = 1080,
                height = 1920,
                layerCount = 2,
                modifiedAt = System.currentTimeMillis()
            )
            saveIndex(listOf(initial))
            _projects.value = listOf(initial)
            return@withContext
        }
        try {
            val content = indexFile.readText()
            val list = json.decodeFromString<List<ProjectSummary>>(content)
            _projects.value = list.sortedByDescending { it.modifiedAt }
        } catch (e: Exception) {
            _projects.value = emptyList()
        }
    }

    private fun saveIndex(list: List<ProjectSummary>) {
        indexFile.writeText(json.encodeToString(list))
        _projects.value = list.sortedByDescending { it.modifiedAt }
    }

    suspend fun createProject(name: String, canvasSize: CanvasSize): ProjectDocument = withContext(Dispatchers.IO) {
        val doc = ProjectDocument(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Untitled Project" },
            canvasSize = canvasSize
        )
        val file = File(projectsDir, "${doc.id}.sg")
        SgProjectArchive.saveToFile(doc, file)

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
            val (doc, _) = SgProjectArchive.loadFromFile(file)
            doc
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveProject(doc: ProjectDocument) = withContext(Dispatchers.IO) {
        val file = File(projectsDir, "${doc.id}.sg")
        SgProjectArchive.saveToFile(doc, file)

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
        val file = File(projectsDir, "$newId.sg")
        SgProjectArchive.saveToFile(duplicatedDoc, file)

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
            File(projectsDir, "$id.sg").delete()
        }
        saveIndex(_projects.value.filter { it.id !in ids })
    }

    suspend fun importProject(inputStream: InputStream): ProjectDocument = withContext(Dispatchers.IO) {
        val (importedDoc, assets) = SgProjectArchive.importFromStream(inputStream)
        val newId = UUID.randomUUID().toString()
        val doc = importedDoc.copy(id = newId, updatedAt = System.currentTimeMillis())
        val file = File(projectsDir, "$newId.sg")
        SgProjectArchive.saveToFile(doc, file, assets)

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
