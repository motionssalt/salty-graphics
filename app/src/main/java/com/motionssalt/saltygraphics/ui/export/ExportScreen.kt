package com.motionssalt.saltygraphics.ui.export

import com.motionssalt.saltygraphics.uikit.structure.SaltyToastManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.data.StorageManager
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import com.motionssalt.saltygraphics.media.MediaCache
import com.motionssalt.saltygraphics.uikit.sublibs.export.SaltyExportFormat
import com.motionssalt.saltygraphics.uikit.sublibs.export.SaltyExportScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ExportFormat(val displayName: String, val extension: String, val mimeType: String) {
    PNG("PNG (Lossless)", "png", "image/png"),
    JPEG("JPEG (Photo)", "jpg", "image/jpeg"),
    WEBP("WebP (Modern)", "webp", "image/webp")
}

@Composable
fun ExportScreen(
    projectId: String,
    repository: ProjectRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var projectDocument by remember { mutableStateOf<ProjectDocument?>(null) }
    var selectedFormat by remember { mutableStateOf(SaltyExportFormat.PNG) }
    var exportQualityPercent by remember { mutableStateOf(100f) }
    var resolutionScale by remember { mutableStateOf(1.0f) }
    var isExporting by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        projectDocument = repository.getProject(projectId)
    }

    val doc = projectDocument
    val finalWidth = ((doc?.canvasSize?.width ?: 1) * resolutionScale).toInt()
    val finalHeight = ((doc?.canvasSize?.height ?: 1) * resolutionScale).toInt()

    fun renderFullDocumentBitmap(activeDoc: ProjectDocument): Bitmap {
        val composite = LayerCompositor.compositeDocument(
            document = activeDoc,
            width = finalWidth,
            height = finalHeight,
            mediaResolver = { path -> MediaCache.getOrLoad(context, activeDoc.id, path) }
        )
        if (selectedFormat == SaltyExportFormat.JPEG && activeDoc.editorSettings.backgroundColor == 0L) {
            val flattened = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
            val c = Canvas(flattened)
            c.drawColor(android.graphics.Color.WHITE)
            c.drawBitmap(composite, 0f, 0f, null)
            return flattened
        }
        return composite
    }

    var currentFolderName by remember { mutableStateOf(StorageManager.getSavedFolderName(context)) }
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            StorageManager.setSavedFolder(context, treeUri)
            currentFolderName = StorageManager.getSavedFolderName(context)
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(selectedFormat.mimeType)
    ) { uri: Uri? ->
        val activeDoc = doc ?: return@rememberLauncherForActivityResult
        uri?.let {
            coroutineScope.launch {
                isExporting = true
                withContext(Dispatchers.IO) {
                    val bitmap = renderFullDocumentBitmap(activeDoc)
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        when (selectedFormat) {
                            SaltyExportFormat.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                            SaltyExportFormat.JPEG -> bitmap.compress(Bitmap.CompressFormat.JPEG, exportQualityPercent.toInt(), os)
                            SaltyExportFormat.WEBP -> bitmap.compress(Bitmap.CompressFormat.WEBP, exportQualityPercent.toInt(), os)
                        }
                    }
                }
                isExporting = false
                onNavigateBack()
            }
        }
    }

    val saveSgFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val currentDoc = projectDocument ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            coroutineScope.launch {
                isExporting = true
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            repository.exportProjectToStream(currentDoc.id, os)
                        }
                    } catch (_: Exception) {
                    }
                }
                isExporting = false
                SaltyToastManager.show("Saved .sg project file successfully")
                onNavigateBack()
            }
        }
    }

    SaltyExportScreen(
        isLoading = doc == null,
        finalWidth = finalWidth,
        finalHeight = finalHeight,
        selectedFormat = selectedFormat,
        onSelectFormat = { selectedFormat = it },
        exportQualityPercent = exportQualityPercent,
        onQualityChange = { exportQualityPercent = it },
        resolutionScale = resolutionScale,
        onResolutionScaleChange = { resolutionScale = it },
        currentFolderName = currentFolderName,
        onChangeFolder = { folderPickerLauncher.launch(null) },
        onExportSg = {
            val activeDoc = doc ?: return@SaltyExportScreen
            saveSgFileLauncher.launch("${activeDoc.name.replace(" ", "_")}.sg")
        },
        onSaveImage = {
            val activeDoc = doc ?: return@SaltyExportScreen
            saveFileLauncher.launch("${activeDoc.name.replace(" ", "_")}.${selectedFormat.extension}")
        },
        onShareImage = {
            val activeDoc = doc ?: return@SaltyExportScreen
            coroutineScope.launch {
                isExporting = true
                withContext(Dispatchers.IO) {
                    val bitmap = renderFullDocumentBitmap(activeDoc)
                    val cacheFile = File(context.cacheDir, "${activeDoc.name.replace(" ", "_")}.${selectedFormat.extension}")
                    FileOutputStream(cacheFile).use { fos ->
                        val format = when (selectedFormat) {
                            SaltyExportFormat.PNG -> Bitmap.CompressFormat.PNG
                            SaltyExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
                            SaltyExportFormat.WEBP -> Bitmap.CompressFormat.WEBP
                        }
                        bitmap.compress(format, exportQualityPercent.toInt(), fos)
                    }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = selectedFormat.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Image via"))
                }
                isExporting = false
            }
        },
        isExporting = isExporting,
        onNavigateBack = onNavigateBack
    )
}
