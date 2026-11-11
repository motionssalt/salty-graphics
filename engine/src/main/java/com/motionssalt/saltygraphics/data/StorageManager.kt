package com.motionssalt.saltygraphics.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object StorageManager {
    private const val PREFS_NAME = "salty_storage_prefs"
    private const val KEY_FOLDER_URI = "saved_folder_tree_uri"
    private const val KEY_FOLDER_NAME = "saved_folder_name"

    fun getSavedFolderUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriStr = prefs.getString(KEY_FOLDER_URI, null) ?: return null
        val uri = Uri.parse(uriStr)

        val hasPermission = try {
            context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isWritePermission
            }
        } catch (_: Throwable) {
            false
        }
        return if (hasPermission) uri else null
    }

    fun getSavedFolderName(context: Context): String {
        val uri = getSavedFolderUri(context) ?: return "Pictures & Documents (Default)"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FOLDER_NAME, null) ?: "Selected Folder"
    }

    fun setSavedFolder(context: Context, treeUri: Uri, folderName: String? = null) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (_: Throwable) {}

        val name = folderName ?: treeUri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/') ?: "External Folder"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_FOLDER_URI, treeUri.toString())
            .putString(KEY_FOLDER_NAME, name)
            .apply()
    }

    fun clearSavedFolder(context: Context) {
        val uri = getSavedFolderUri(context)
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.releasePersistableUriPermission(uri, flags)
            } catch (_: Throwable) {}
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_FOLDER_URI)
            .remove(KEY_FOLDER_NAME)
            .apply()
    }

    fun resolveUniqueFileName(existingNames: Set<String>, targetName: String): String {
        val lowerNames = existingNames.map { it.lowercase() }.toSet()
        if (!lowerNames.contains(targetName.lowercase())) return targetName
        val existingMatch = existingNames.firstOrNull { it.equals(targetName, ignoreCase = true) }
        val nameToUse = existingMatch ?: targetName
        val base = nameToUse.substringBeforeLast('.')
        val ext = nameToUse.substringAfterLast('.', "")
        val extSuffix = if (ext.isNotEmpty()) ".$ext" else ""
        var counter = 1
        var candidate = "${base}_$counter$extSuffix"
        while (lowerNames.contains(candidate.lowercase())) {
            counter++
            candidate = "${base}_$counter$extSuffix"
        }
        return candidate
    }

    /**
     * Opens an output stream to save directly to the configured folder (or MediaStore default).
     */
    fun openImageOutputStream(context: Context, fileName: String, mimeType: String): Pair<Uri?, OutputStream?> {
        val savedTree = getSavedFolderUri(context)
        if (savedTree != null) {
            try {
                val treeDocUri = DocumentsContract.buildDocumentUriUsingTree(
                    savedTree,
                    DocumentsContract.getTreeDocumentId(savedTree)
                )
                val targetDoc = DocumentsContract.createDocument(context.contentResolver, treeDocUri, mimeType, fileName)
                if (targetDoc != null) {
                    val stream = context.contentResolver.openOutputStream(targetDoc)
                    if (stream != null) return Pair(targetDoc, stream)
                }
            } catch (_: Throwable) {
                // If permission was lost or folder missing, gracefully fall through to default
            }
        }

        // MediaStore default into Pictures/Salty Graphics
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Salty Graphics")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val stream = uri?.let { context.contentResolver.openOutputStream(it) }
        return Pair(uri, stream)
    }

    fun openProjectOutputStream(context: Context, fileName: String): Pair<Uri?, OutputStream?> {
        val savedTree = getSavedFolderUri(context)
        if (savedTree != null) {
            try {
                val treeDocUri = DocumentsContract.buildDocumentUriUsingTree(
                    savedTree,
                    DocumentsContract.getTreeDocumentId(savedTree)
                )
                val targetDoc = DocumentsContract.createDocument(context.contentResolver, treeDocUri, "application/octet-stream", fileName)
                if (targetDoc != null) {
                    val stream = context.contentResolver.openOutputStream(targetDoc)
                    if (stream != null) return Pair(targetDoc, stream)
                }
            } catch (_: Throwable) {}
        }

        // MediaStore default into Documents/Salty Graphics
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Salty Graphics")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
        val stream = uri?.let { context.contentResolver.openOutputStream(it) }
        return Pair(uri, stream)
    }
}
