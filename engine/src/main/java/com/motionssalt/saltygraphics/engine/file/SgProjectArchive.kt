package com.motionssalt.saltygraphics.engine.file

import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
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

        val document = json.decodeFromString<ProjectDocument>(projectJson)
        return Pair(document, assets)
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
