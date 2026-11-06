package com.motionssalt.saltygraphics.data

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

enum class FontOrigin {
    DEVICE_DEFAULT,
    BUILT_IN,
    SYSTEM,
    IMPORTED
}

@Serializable
data class FontMetadata(
    val id: String,
    val displayName: String,
    val familyName: String,
    val styleName: String = "Regular",
    val fileName: String,
    val isBuiltIn: Boolean = false,
    val isImported: Boolean = false
)

data class FontItem(
    val id: String,
    val displayName: String,
    val familyName: String,
    val styleName: String = "Regular",
    val origin: FontOrigin,
    val assetPath: String? = null,
    val filePath: String? = null,
    val isBuiltIn: Boolean = (origin == FontOrigin.BUILT_IN),
    val isImported: Boolean = (origin == FontOrigin.IMPORTED)
)

data class FontImportSummary(
    val totalProcessed: Int,
    val importedCount: Int,
    val skippedCount: Int,
    val skippedReasons: List<String>
)

object FontManager {

    private val typefaceCache = mutableMapOf<String, Typeface>()

    val DEVICE_DEFAULT = FontItem(
        id = "device_default",
        displayName = "Device Default",
        familyName = "sans-serif",
        styleName = "Regular",
        origin = FontOrigin.DEVICE_DEFAULT
    )

    private val BUILT_IN_FONTS = listOf(
        FontItem("builtin_inter", "Inter", "Inter", "Regular", FontOrigin.BUILT_IN, assetPath = "Inter-Regular.ttf"),
        FontItem("builtin_montserrat", "Montserrat", "Montserrat", "Regular", FontOrigin.BUILT_IN, assetPath = "Montserrat-Regular.ttf"),
        FontItem("builtin_playfair", "Playfair Display", "Playfair Display", "Regular", FontOrigin.BUILT_IN, assetPath = "PlayfairDisplay-Regular.ttf"),
        FontItem("builtin_oswald", "Oswald", "Oswald", "Regular", FontOrigin.BUILT_IN, assetPath = "Oswald-Regular.ttf"),
        FontItem("builtin_dancing", "Dancing Script", "Dancing Script", "Regular", FontOrigin.BUILT_IN, assetPath = "DancingScript-Regular.ttf"),
        FontItem("builtin_jetbrains", "JetBrains Mono", "JetBrains Mono", "Regular", FontOrigin.BUILT_IN, assetPath = "JetBrainsMono-Regular.ttf"),
        FontItem("builtin_dejavu", "DejaVu Serif", "DejaVu Serif", "Regular", FontOrigin.BUILT_IN, assetPath = "DejaVuSerif.ttf")
    )

    fun getBuiltInFonts(): List<FontItem> = BUILT_IN_FONTS

    fun getAllFonts(context: Context): List<FontItem> {
        val list = mutableListOf<FontItem>()
        list.add(DEVICE_DEFAULT)
        list.addAll(BUILT_IN_FONTS)
        list.addAll(getImportedFonts(context))
        return list
    }

    fun getFontsDir(context: Context): File {
        val dir = File(context.filesDir, "custom_fonts")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getMetaFile(context: Context): File {
        return File(getFontsDir(context), "fonts_index.json")
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun getImportedFonts(context: Context): List<FontItem> {
        val metaFile = getMetaFile(context)
        if (!metaFile.exists()) return emptyList()
        return try {
            val content = metaFile.readText()
            val list = json.decodeFromString<List<FontMetadata>>(content)
            list.mapNotNull { meta ->
                val fontFile = File(getFontsDir(context), meta.fileName)
                if (fontFile.exists()) {
                    FontItem(
                        id = meta.id,
                        displayName = meta.displayName,
                        familyName = meta.familyName,
                        styleName = meta.styleName,
                        origin = FontOrigin.IMPORTED,
                        filePath = fontFile.absolutePath
                    )
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveImportedMetadata(context: Context, items: List<FontMetadata>) {
        try {
            val content = json.encodeToString(items)
            getMetaFile(context).writeText(content)
        } catch (_: Exception) {}
    }

    /**
     * Validates and imports multiple font files into the app's font library.
     */
    fun importFontsFromUris(context: Context, uris: List<Uri>): FontImportSummary {
        val existingImported = getImportedFonts(context).toMutableList()
        val existingMeta = existingImported.map {
            FontMetadata(it.id, it.displayName, it.familyName, it.styleName, File(it.filePath ?: "").name, isImported = true)
        }.toMutableList()

        var imported = 0
        var skipped = 0
        val reasons = mutableListOf<String>()

        for (uri in uris) {
            val fileName = queryFileName(context, uri) ?: "font_${UUID.randomUUID().toString().take(8)}.ttf"
            val lowerName = fileName.lowercase()

            if (!lowerName.endsWith(".ttf") && !lowerName.endsWith(".otf")) {
                skipped++
                reasons.add("$fileName: Unsupported format (must be .ttf or .otf)")
                continue
            }

            // Copy to temp file for validation
            val tempFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}_$fileName")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempFile.length() < 1024L) {
                    skipped++
                    reasons.add("$fileName: Corrupt font file (too small)")
                    tempFile.delete()
                    continue
                }

                // Check magic bytes for TrueType / OpenType
                val magic = ByteArray(4)
                FileInputStream(tempFile).use { it.read(magic) }
                val isTrueType = (magic[0] == 0x00.toByte() && magic[1] == 0x01.toByte() && magic[2] == 0x00.toByte() && magic[3] == 0x00.toByte()) ||
                        (magic[0] == 't'.code.toByte() && magic[1] == 'r'.code.toByte() && magic[2] == 'u'.code.toByte() && magic[3] == 'e'.code.toByte())
                val isOpenType = magic[0] == 'O'.code.toByte() && magic[1] == 'T'.code.toByte() && magic[2] == 'T'.code.toByte() && magic[3] == 'O'.code.toByte()

                if (!isTrueType && !isOpenType) {
                    skipped++
                    reasons.add("$fileName: Not a valid TrueType or OpenType font")
                    tempFile.delete()
                    continue
                }

                // Test Typeface creation
                val tf = try {
                    Typeface.createFromFile(tempFile)
                } catch (e: Exception) {
                    null
                }

                if (tf == null) {
                    skipped++
                    reasons.add("$fileName: Failed to parse font typeface")
                    tempFile.delete()
                    continue
                }

                // Determine font family name from file name
                val rawName = fileName.substringBeforeLast('.')
                val cleanFamilyName = rawName.replace('_', ' ').replace('-', ' ').trim()
                val style = if (rawName.contains("Bold", ignoreCase = true)) "Bold"
                else if (rawName.contains("Italic", ignoreCase = true)) "Italic"
                else if (rawName.contains("Condensed", ignoreCase = true)) "Condensed"
                else "Regular"

                // Check for duplicates
                val isDuplicate = existingMeta.any {
                    it.familyName.equals(cleanFamilyName, ignoreCase = true) && it.styleName.equals(style, ignoreCase = true)
                } || BUILT_IN_FONTS.any {
                    it.familyName.equals(cleanFamilyName, ignoreCase = true) && it.styleName.equals(style, ignoreCase = true)
                }

                if (isDuplicate) {
                    skipped++
                    reasons.add("$cleanFamilyName ($style): Already installed")
                    tempFile.delete()
                    continue
                }

                // Store in custom_fonts
                val targetFileName = "${UUID.randomUUID()}_$fileName"
                val destFile = File(getFontsDir(context), targetFileName)
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()

                val newMeta = FontMetadata(
                    id = "imported_${UUID.randomUUID()}",
                    displayName = "$cleanFamilyName $style".trim(),
                    familyName = cleanFamilyName,
                    styleName = style,
                    fileName = targetFileName,
                    isImported = true
                )
                existingMeta.add(newMeta)
                imported++
            } catch (e: Exception) {
                skipped++
                reasons.add("$fileName: Error ${e.message}")
                tempFile.delete()
            }
        }

        saveImportedMetadata(context, existingMeta)
        return FontImportSummary(
            totalProcessed = uris.size,
            importedCount = imported,
            skippedCount = skipped,
            skippedReasons = reasons
        )
    }

    fun deleteImportedFont(context: Context, fontId: String): Boolean {
        val existing = getImportedFonts(context).toMutableList()
        val target = existing.find { it.id == fontId } ?: return false
        existing.remove(target)
        if (target.filePath != null) {
            File(target.filePath).delete()
        }
        val meta = existing.map {
            FontMetadata(it.id, it.displayName, it.familyName, it.styleName, File(it.filePath ?: "").name, isImported = true)
        }
        saveImportedMetadata(context, meta)
        typefaceCache.remove(fontId)
        return true
    }

    fun getTypeface(context: Context, font: FontItem?): Typeface {
        if (font == null || font.origin == FontOrigin.DEVICE_DEFAULT) {
            return Typeface.DEFAULT
        }
        val cacheKey = font.id
        typefaceCache[cacheKey]?.let { return it }

        val created: Typeface? = try {
            when (font.origin) {
                FontOrigin.BUILT_IN -> {
                    if (font.assetPath != null) {
                        Typeface.createFromAsset(context.assets, "fonts/${font.assetPath}")
                    } else null
                }
                FontOrigin.IMPORTED -> {
                    if (font.filePath != null && File(font.filePath).exists()) {
                        Typeface.createFromFile(File(font.filePath))
                    } else null
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }

        val result = created ?: Typeface.DEFAULT
        typefaceCache[cacheKey] = result
        return result
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIdx)
                    }
                }
            } catch (_: Exception) {}
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }
}
