package com.motionssalt.saltygraphics.engine.file

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
    val appVersion: String = "1.0.0-beta1",
    val appName: String = "Salty Graphics",
    val author: String = "MOTIONSALT",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

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
        assetFiles: Map<String, ByteArray> = emptyMap()
    ) {
        val zipOut = ZipOutputStream(BufferedOutputStream(outputStream))

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

        // 3. Write assets/
        for ((assetName, data) in assetFiles) {
            zipOut.putNextEntry(ZipEntry("assets/$assetName"))
            zipOut.write(data)
            zipOut.closeEntry()
        }

        zipOut.finish()
        zipOut.flush()
    }

    /**
     * Reads a .sg ZIP container stream and returns the reconstructed ProjectDocument and asset map.
     * Guaranteed crash-proof: individual corrupted layers are quarantined rather than failing the document.
     */
    fun importFromStream(inputStream: InputStream): Pair<ProjectDocument, Map<String, ByteArray>> {
        val zipIn = ZipInputStream(BufferedInputStream(inputStream))
        var projectJson: String? = null
        val assets = mutableMapOf<String, ByteArray>()

        var entry = zipIn.nextEntry
        while (entry != null) {
            val name = entry.name
            if (name == "project.json") {
                val bytes = zipIn.readBytes()
                projectJson = String(bytes, Charsets.UTF_8)
            } else if (name.startsWith("assets/") && !entry.isDirectory) {
                val assetKey = name.removePrefix("assets/")
                assets[assetKey] = zipIn.readBytes()
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }

        if (projectJson == null) {
            throw IllegalArgumentException("Invalid .sg file: missing project.json document model")
        }

        val document = try {
            json.decodeFromString<ProjectDocument>(projectJson)
        } catch (e: Exception) {
            recoverProjectDocument(projectJson, e)
        }

        return Pair(document, assets)
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
    fun saveToFile(document: ProjectDocument, file: File, assetFiles: Map<String, ByteArray> = emptyMap()) {
        FileOutputStream(file).use { fos ->
            exportToStream(document, fos, assetFiles)
        }
    }

    /**
     * Loads project from local file.
     */
    fun loadFromFile(file: File): Pair<ProjectDocument, Map<String, ByteArray>> {
        FileInputStream(file).use { fis ->
            return importFromStream(fis)
        }
    }
}
