package com.motionssalt.saltygraphics.engine.file

import com.motionssalt.saltygraphics.engine.action.ActionTreeSnapshot
import com.motionssalt.saltygraphics.engine.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.*
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class SgManifest(
    val formatVersion: String = "1.0.0",
    val appVersion: String = "1.0.0",
    val appName: String = "Salty Graphics",
    val author: String = "MOTIONSALT",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

data class SgImportResult(
    val document: ProjectDocument,
    val assets: Map<String, ByteArray> = emptyMap(),
    val manifest: SgManifest = SgManifest(),
    val historySnapshot: ActionTreeSnapshot? = null,
    val previewPng: ByteArray? = null
) {
    val first: ProjectDocument get() = document
    val second: Map<String, ByteArray> get() = assets
}

object SgProjectArchive {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Packs the project document and assets into a .sg ZIP container stream.
     */
    fun exportToStream(
        document: ProjectDocument,
        outputStream: OutputStream,
        assetFiles: Map<String, ByteArray> = emptyMap(),
        historySnapshot: ActionTreeSnapshot? = null,
        previewPng: ByteArray? = null
    ) {
        val zipOut = ZipOutputStream(BufferedOutputStream(outputStream))
        try {
            // 1. Write manifest.json
            val manifest = SgManifest(
                createdAt = document.createdAt,
                modifiedAt = System.currentTimeMillis()
            )
            val manifestBytes = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)
            zipOut.putNextEntry(ZipEntry("manifest.json"))
            zipOut.write(manifestBytes)
            zipOut.closeEntry()

            // 2. Write project.json
            val projectBytes = json.encodeToString(document).toByteArray(Charsets.UTF_8)
            zipOut.putNextEntry(ZipEntry("project.json"))
            zipOut.write(projectBytes)
            zipOut.closeEntry()

            // 3. Write history.json if available
            if (historySnapshot != null) {
                val historyBytes = json.encodeToString(historySnapshot).toByteArray(Charsets.UTF_8)
                zipOut.putNextEntry(ZipEntry("history.json"))
                zipOut.write(historyBytes)
                zipOut.closeEntry()
            }

            // 4. Write preview.png if available
            if (previewPng != null && previewPng.isNotEmpty()) {
                zipOut.putNextEntry(ZipEntry("preview.png"))
                zipOut.write(previewPng)
                zipOut.closeEntry()
            }

            // 5. Write assets/
            for ((assetName, data) in assetFiles) {
                val path = if (assetName.startsWith("assets/")) assetName else "assets/$assetName"
                zipOut.putNextEntry(ZipEntry(path))
                zipOut.write(data)
                zipOut.closeEntry()
            }

            zipOut.finish()
            zipOut.flush()
        } catch (e: Exception) {
            throw IOException("Failed to export .sg project archive: ${e.message}", e)
        }
    }

    /**
     * Reads a .sg ZIP container stream and returns SgImportResult.
     * Guaranteed crash-proof: individual corrupted layers are quarantined rather than failing the document.
     */
    fun importFromStream(inputStream: InputStream): SgImportResult {
        var projectJson: String? = null
        var manifestJson: String? = null
        var historyJson: String? = null
        var previewPng: ByteArray? = null
        val assets = mutableMapOf<String, ByteArray>()
        var entryCount = 0

        try {
            val zipIn = ZipInputStream(BufferedInputStream(inputStream))
            var entry = zipIn.nextEntry
            while (entry != null) {
                entryCount++
                val name = entry.name
                when {
                    name == "manifest.json" -> {
                        manifestJson = String(zipIn.readBytes(), Charsets.UTF_8)
                    }
                    name == "project.json" -> {
                        projectJson = String(zipIn.readBytes(), Charsets.UTF_8)
                    }
                    name == "history.json" -> {
                        historyJson = String(zipIn.readBytes(), Charsets.UTF_8)
                    }
                    name == "preview.png" -> {
                        previewPng = zipIn.readBytes()
                    }
                    name.startsWith("assets/") && !entry.isDirectory -> {
                        val assetKey = name.removePrefix("assets/")
                        assets[assetKey] = zipIn.readBytes()
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Corrupted .sg file: cannot read archive contents (${e.message})", e)
        }
        if (entryCount == 0) {
            throw IllegalArgumentException("Corrupted .sg file: archive is empty or invalid format")
        }

        // Validate manifest version if present
        val manifest = manifestJson?.let {
            try {
                json.decodeFromString<SgManifest>(it)
            } catch (_: Exception) {
                null
            }
        } ?: SgManifest()

        val majorVersion = manifest.formatVersion.split('.').firstOrNull()?.toIntOrNull() ?: 1
        if (majorVersion > 1) {
            throw IllegalArgumentException("Unsupported .sg format version '${manifest.formatVersion}'. This project was created with a newer version of Salty Graphics. Please update the app.")
        }

        if (projectJson == null) {
            throw IllegalArgumentException("Invalid .sg file: missing project.json document model")
        }

        val document = try {
            json.decodeFromString<ProjectDocument>(projectJson)
        } catch (e: Exception) {
            recoverProjectDocument(projectJson, e)
        }

        val historySnapshot = historyJson?.let {
            try {
                json.decodeFromString<ActionTreeSnapshot>(it)
            } catch (_: Exception) {
                null
            }
        }

        return SgImportResult(
            document = document,
            assets = assets,
            manifest = manifest,
            historySnapshot = historySnapshot,
            previewPng = previewPng
        )
    }

    /**
     * Rescues a project document if full serialization fails due to one or more broken layers.
     * Quarantines broken layers with error descriptions while preserving healthy layers.
     */
    fun recoverProjectDocument(rawJson: String, originalError: Exception): ProjectDocument {
        return try {
            val jsonTree = json.parseToJsonElement(rawJson).jsonObject
            val id = jsonTree["id"]?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString()
            val name = jsonTree["name"]?.jsonPrimitive?.contentOrNull ?: "Recovered Project"
            val canvasSize = jsonTree["canvasSize"]?.let {
                runCatching { json.decodeFromJsonElement<CanvasSize>(it) }.getOrNull()
            } ?: CanvasSize()
            val rawLayers = jsonTree["layers"]?.jsonArray ?: emptyList()
            val recoveredLayers = rawLayers.mapIndexed { index, layerElement ->
                try {
                    json.decodeFromJsonElement<Layer>(layerElement)
                } catch (layerEx: Exception) {
                    val layerObj = layerElement as? JsonObject
                    val layerId = layerObj?.get("id")?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString()
                    val layerName = layerObj?.get("name")?.jsonPrimitive?.contentOrNull ?: "Layer ${index + 1}"
                    Layer(
                        id = layerId,
                        name = "$layerName [Quarantined]",
                        type = LayerType.QUARANTINED,
                        quarantinedData = QuarantinedLayerData(
                            rawJson = layerElement.toString(),
                            error = layerEx.message ?: layerEx.javaClass.simpleName,
                            originalLayerId = layerId,
                            originalLayerName = layerName
                        )
                    )
                }
            }
            val selectedIds = jsonTree["selectedLayerIds"]?.let {
                runCatching { json.decodeFromJsonElement<Set<String>>(it) }.getOrNull()
            } ?: emptySet()
            val activeGroupId = jsonTree["activeGroupId"]?.jsonPrimitive?.contentOrNull

            ProjectDocument(
                id = id,
                name = name,
                canvasSize = canvasSize,
                editorSettings = jsonTree["editorSettings"]?.let {
                    runCatching { json.decodeFromJsonElement<EditorSettings>(it) }.getOrDefault(EditorSettings())
                } ?: EditorSettings(),
                layers = recoveredLayers,
                selectedLayerIds = selectedIds,
                activeGroupId = activeGroupId,
                createdAt = jsonTree["createdAt"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        } catch (_: Exception) {
            ProjectDocument(
                name = "Corrupted Project (Quarantined)",
                layers = listOf(
                    Layer(
                        name = "Corrupted Document Data",
                        type = LayerType.QUARANTINED,
                        quarantinedData = QuarantinedLayerData(
                            rawJson = rawJson,
                            error = originalError.message ?: originalError.javaClass.simpleName
                        )
                    )
                )
            )
        }
    }

    /**
     * Saves project to local file (e.g. for auto-save).
     */
    fun saveToFile(
        document: ProjectDocument,
        file: File,
        assetFiles: Map<String, ByteArray> = emptyMap(),
        historySnapshot: ActionTreeSnapshot? = null,
        previewPng: ByteArray? = null
    ) {
        FileOutputStream(file).use { fos ->
            exportToStream(
                document = document,
                outputStream = fos,
                assetFiles = assetFiles,
                historySnapshot = historySnapshot,
                previewPng = previewPng
            )
        }
    }

    /**
     * Loads project from local file.
     */
    fun loadFromFile(file: File): SgImportResult {
        FileInputStream(file).use { fis ->
            return importFromStream(fis)
        }
    }
}
