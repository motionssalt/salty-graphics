package com.motionssalt.saltygraphics.ui.export

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.data.StorageManager
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import com.motionssalt.saltygraphics.media.MediaCache
import com.motionssalt.saltygraphics.uikit.controls.SaltyProgressSlider
import com.motionssalt.saltygraphics.uikit.structure.SaltyBox
import com.motionssalt.saltygraphics.uikit.structure.SaltyButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyColumn
import com.motionssalt.saltygraphics.uikit.structure.SaltyOutlinedButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyRow
import com.motionssalt.saltygraphics.uikit.structure.SaltyScreenScaffold
import com.motionssalt.saltygraphics.uikit.structure.SaltySectionCard
import com.motionssalt.saltygraphics.uikit.structure.SaltySelectableTile
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltySpacer
import com.motionssalt.saltygraphics.uikit.structure.SaltySpinner
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextStyle
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextTone
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextWeight
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxSize
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxWidth
import com.motionssalt.saltygraphics.uikit.structure.saltyNavigationBarsPadding
import com.motionssalt.saltygraphics.uikit.structure.saltyPadding
import com.motionssalt.saltygraphics.uikit.structure.SaltyRow as SharedRow
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
    var selectedFormat by remember { mutableStateOf(ExportFormat.PNG) }
    var exportQualityPercent by remember { mutableStateOf(100f) }
    var resolutionScale by remember { mutableStateOf(1.0f) }
    var isExporting by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        projectDocument = repository.getProject(projectId)
    }

    val doc = projectDocument
    if (doc == null) {
        SaltyBox(
            modifier = Modifier.saltyFillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            SaltySpinner()
        }
        return
    }

    val finalWidth = (doc.canvasSize.width * resolutionScale).toInt()
    val finalHeight = (doc.canvasSize.height * resolutionScale).toInt()

    fun renderFullDocumentBitmap(): Bitmap {
        val composite = LayerCompositor.compositeDocument(
            document = doc,
            width = finalWidth,
            height = finalHeight,
            mediaResolver = { path -> MediaCache.getOrLoad(context, doc.id, path) }
        )
        if (selectedFormat == ExportFormat.JPEG && doc.editorSettings.backgroundColor == 0L) {
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
        uri?.let {
            coroutineScope.launch {
                isExporting = true
                withContext(Dispatchers.IO) {
                    val bitmap = renderFullDocumentBitmap()
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        when (selectedFormat) {
                            ExportFormat.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                            ExportFormat.JPEG -> bitmap.compress(Bitmap.CompressFormat.JPEG, exportQualityPercent.toInt(), os)
                            ExportFormat.WEBP -> bitmap.compress(Bitmap.CompressFormat.WEBP, exportQualityPercent.toInt(), os)
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
                Toast.makeText(context, "Saved .sg project file successfully", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
        }
    }

    SaltyScreenScaffold(
        title = "Export Image",
        onBack = onNavigateBack
    ) { innerPadding ->
        SaltyColumn(
            modifier = Modifier
                .saltyFillMaxSize()
                .saltyPadding(innerPadding)
                .saltyPadding(all = SaltySpace.LG),
            spacing = SaltySpace.LG,
            scrollable = true
        ) {
            SaltySectionCard(title = "Full Document Output") {
                SaltyText(
                    text = "Resolution: $finalWidth x $finalHeight px",
                    style = SaltyTextStyle.TITLE_MEDIUM,
                    tone = SaltyTextTone.ACCENT,
                    weight = SaltyTextWeight.BOLD
                )
                SaltyText(
                    text = "Export always renders from the master document model at full quality (never downsampled preview proxy).",
                    style = SaltyTextStyle.BODY_MEDIUM,
                    tone = SaltyTextTone.SECONDARY
                )
            }

            SaltySectionCard(title = "Export Format") {
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    spacing = SaltySpace.SM
                ) {
                    ExportFormat.entries.forEach { format ->
                        SaltySelectableTile(
                            title = format.displayName,
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (selectedFormat != ExportFormat.PNG) {
                    SaltySpacer(SaltySpace.MD)
                    SaltyProgressSlider(
                        label = "Quality",
                        value = exportQualityPercent,
                        onValueChange = { exportQualityPercent = it },
                        range = 10f..100f,
                        step = 1f,
                        defaultValue = 90f,
                        unit = "%"
                    )
                }
            }

            SaltySectionCard(title = "Output Scale") {
                val scales = listOf(0.5f, 0.75f, 1.0f)
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    spacing = SaltySpace.SM
                ) {
                    scales.forEach { s ->
                        SaltySelectableTile(
                            title = "${(s * 100).toInt()}%",
                            selected = resolutionScale == s,
                            onClick = { resolutionScale = s },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            SaltySectionCard(title = "Save Location") {
                SharedRow(
                    label = "Save to: $currentFolderName",
                    subtitle = "Direct export into this folder without prompting each time."
                ) {
                    SaltyTextButton(
                        text = "Change",
                        onClick = { folderPickerLauncher.launch(null) }
                    )
                }
            }

            SaltySectionCard(title = "Save Project File (.sg)") {
                SaltyText(
                    text = "Export the entire editable project archive including all layers, vectors, typography, history, and embedded assets.",
                    style = SaltyTextStyle.BODY_MEDIUM,
                    tone = SaltyTextTone.SECONDARY
                )
                SaltySpacer(SaltySpace.MD)
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    spacing = SaltySpace.SM
                ) {
                    SaltyButton(
                        text = "Save .sg",
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            val fileName = "${doc.name.replace(" ", "_")}.sg"
                            saveSgFileLauncher.launch(fileName)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SaltyOutlinedButton(
                        text = "Share .sg",
                        icon = Icons.Default.Share,
                        onClick = {
                            coroutineScope.launch {
                                isExporting = true
                                withContext(Dispatchers.IO) {
                                    val cacheFile = File(context.cacheDir, "${doc.name.replace(" ", "_")}.sg")
                                    FileOutputStream(cacheFile).use { fos ->
                                        repository.exportProjectToStream(doc.id, fos)
                                    }
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/octet-stream"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Project (.sg) via"))
                                }
                                isExporting = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SaltyColumn(
                modifier = Modifier.saltyNavigationBarsPadding(),
                spacing = SaltySpace.SM
            ) {
                SaltyButton(
                    text = "Save to Device (SAF)",
                    icon = Icons.Default.FileDownload,
                    fillWidth = true,
                    onClick = {
                        val fileName = "${doc.name.replace(" ", "_")}.${selectedFormat.extension}"
                        saveFileLauncher.launch(fileName)
                    }
                )
                SaltyOutlinedButton(
                    text = "Share via Share Sheet",
                    icon = Icons.Default.Share,
                    fillWidth = true,
                    onClick = {
                        coroutineScope.launch {
                            isExporting = true
                            withContext(Dispatchers.IO) {
                                val bitmap = renderFullDocumentBitmap()
                                val cacheFile = File(context.cacheDir, "${doc.name.replace(" ", "_")}.${selectedFormat.extension}")
                                FileOutputStream(cacheFile).use { fos ->
                                    val format = when (selectedFormat) {
                                        ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                                        ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
                                        ExportFormat.WEBP -> Bitmap.CompressFormat.WEBP
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
                    }
                )
            }

            if (isExporting) {
                SaltyBox(
                    modifier = Modifier.saltyFillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    SaltySpinner()
                }
            }
        }
    }
}
