package com.motionssalt.saltygraphics.ui.export

import com.motionssalt.saltygraphics.data.StorageManager
import com.motionssalt.saltygraphics.media.MediaCache
import com.motionssalt.saltygraphics.engine.model.ShapeType
import android.graphics.RectF
import android.os.Build

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.theme.*
import com.motionssalt.saltygraphics.ui.settings.SettingsSectionCard
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    projectId: String,
    repository: ProjectRepository,
    onNavigateBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val finalWidth = (doc.canvasSize.width * resolutionScale).toInt()
    val finalHeight = (doc.canvasSize.height * resolutionScale).toInt()

    fun renderFullDocumentBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawExportBackground(canvas, doc.editorSettings.backgroundColor, selectedFormat == ExportFormat.JPEG)

        val halfW = finalWidth / 2f
        val halfH = finalHeight / 2f

        for (layer in doc.layers) {
            if (!layer.isVisible) continue
            val lx = halfW + (layer.transform.translationX * resolutionScale)
            val ly = halfH + (layer.transform.translationY * resolutionScale)

            val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                layerPaint.blendMode = when (layer.blendMode) {
                    com.motionssalt.saltygraphics.engine.model.BlendMode.DARKEN -> android.graphics.BlendMode.DARKEN
                    com.motionssalt.saltygraphics.engine.model.BlendMode.MULTIPLY -> android.graphics.BlendMode.MULTIPLY
                    com.motionssalt.saltygraphics.engine.model.BlendMode.COLOR_BURN -> android.graphics.BlendMode.COLOR_BURN
                    com.motionssalt.saltygraphics.engine.model.BlendMode.LIGHTEN -> android.graphics.BlendMode.LIGHTEN
                    com.motionssalt.saltygraphics.engine.model.BlendMode.SCREEN -> android.graphics.BlendMode.SCREEN
                    com.motionssalt.saltygraphics.engine.model.BlendMode.COLOR_DODGE -> android.graphics.BlendMode.COLOR_DODGE
                    com.motionssalt.saltygraphics.engine.model.BlendMode.OVERLAY -> android.graphics.BlendMode.OVERLAY
                    com.motionssalt.saltygraphics.engine.model.BlendMode.SOFT_LIGHT -> android.graphics.BlendMode.SOFT_LIGHT
                    com.motionssalt.saltygraphics.engine.model.BlendMode.HARD_LIGHT -> android.graphics.BlendMode.HARD_LIGHT
                    com.motionssalt.saltygraphics.engine.model.BlendMode.DIFFERENCE -> android.graphics.BlendMode.DIFFERENCE
                    com.motionssalt.saltygraphics.engine.model.BlendMode.EXCLUSION -> android.graphics.BlendMode.EXCLUSION
                    com.motionssalt.saltygraphics.engine.model.BlendMode.HUE -> android.graphics.BlendMode.HUE
                    com.motionssalt.saltygraphics.engine.model.BlendMode.SATURATION -> android.graphics.BlendMode.SATURATION
                    com.motionssalt.saltygraphics.engine.model.BlendMode.COLOR -> android.graphics.BlendMode.COLOR
                    com.motionssalt.saltygraphics.engine.model.BlendMode.LUMINOSITY -> android.graphics.BlendMode.LUMINOSITY
                    else -> android.graphics.BlendMode.SRC_OVER
                }
            }

            canvas.saveLayer(null, layerPaint)
            canvas.translate(lx, ly)
            canvas.rotate(layer.transform.rotationDegrees)
            canvas.scale(layer.transform.scaleX * resolutionScale, layer.transform.scaleY * resolutionScale)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            when (layer.type) {
                LayerType.SHAPE -> {
                    val s = layer.shapeData
                    if (s != null) {
                        val rect = RectF(-s.width / 2f, -s.height / 2f, s.width / 2f, s.height / 2f)
                        if (s.hasFill) {
                            paint.style = Paint.Style.FILL
                            paint.color = s.fillColor.toInt()
                            when (s.type) {
                                ShapeType.CIRCLE -> canvas.drawOval(rect, paint)
                                ShapeType.ROUNDED_RECTANGLE -> canvas.drawRoundRect(rect, s.cornerRadius, s.cornerRadius, paint)
                                else -> canvas.drawRect(rect, paint)
                            }
                        }
                        if (s.hasStroke && s.strokeWidth > 0) {
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = s.strokeWidth
                            paint.color = s.strokeColor.toInt()
                            when (s.type) {
                                ShapeType.CIRCLE -> canvas.drawOval(rect, paint)
                                ShapeType.ROUNDED_RECTANGLE -> canvas.drawRoundRect(rect, s.cornerRadius, s.cornerRadius, paint)
                                else -> canvas.drawRect(rect, paint)
                            }
                        }
                    }
                }
                LayerType.VECTOR -> {
                    layer.editablePaths.forEach { path -> com.motionssalt.saltygraphics.ui.editor.drawNativeVector(canvas, path, 1f) }
                }
                LayerType.TEXT -> {
                    val t = layer.textData
                    if (t != null) {
                        paint.color = t.textColor.toInt()
                        paint.textSize = t.fontSize
                        paint.textAlign = Paint.Align.CENTER
                        paint.isFakeBoldText = t.isBold
                        if (t.isItalic) paint.textSkewX = -0.25f
                        val y = (paint.descent() + paint.ascent()) / -2f
                        if (t.hasOutline) {
                            val outlinePaint = Paint(paint).apply {
                                style = Paint.Style.STROKE
                                strokeWidth = t.outlineWidth
                                color = t.outlineColor.toInt()
                            }
                            canvas.drawText(t.text, 0f, y, outlinePaint)
                        }
                        canvas.drawText(t.text, 0f, y, paint)
                    }
                }
                LayerType.MEDIA -> {
                    val mediaPath = layer.mediaAssetPath
                    val bmp = if (mediaPath != null) MediaCache.getOrLoad(context, doc.id, mediaPath) else null
                    val mw = layer.mediaWidth ?: 300f
                    val mh = layer.mediaHeight ?: 300f
                    if (bmp != null) {
                        val dst = RectF(-mw / 2f, -mh / 2f, mw / 2f, mh / 2f)
                        canvas.drawBitmap(bmp, null, dst, paint)
                    } else {
                        paint.color = android.graphics.Color.DKGRAY
                        canvas.drawRect(-mw / 2f, -mh / 2f, mw / 2f, mh / 2f, paint)
                    }
                }
                LayerType.ADJUSTMENT -> {
                    paint.color = 0x80808080.toInt()
                    canvas.drawRect(-finalWidth / 2f, -finalHeight / 2f, finalWidth / 2f, finalHeight / 2f, paint)
                }
                else -> {}
            }
            canvas.restore()
        }
        return bitmap
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
                    } catch (_: Exception) {}
                }
                isExporting = false
                android.widget.Toast.makeText(context, "Saved .sg project file successfully", android.widget.Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Image", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.onSurface,
                    navigationIconContentColor = colors.onSurface
                )
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resolution and Dimensions Overview
            SettingsSectionCard(title = "Full Document Output") {
                Text(
                    text = "Resolution: $finalWidth x $finalHeight px",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Export always renders from the master document model at full quality (never downsampled preview proxy).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }

            // Format Selection
            SettingsSectionCard(title = "Export Format") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.entries.forEach { format ->
                        val isSelected = selectedFormat == format
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.surfaceVariant else colors.background)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) colors.primary else colors.outline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFormat = format }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = format.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) colors.primary else colors.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (selectedFormat != ExportFormat.PNG) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Quality: ${exportQualityPercent.toInt()}%", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    Slider(
                        value = exportQualityPercent,
                        onValueChange = { exportQualityPercent = it },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )
                }
            }

            // Resolution Scale Selection
            SettingsSectionCard(title = "Output Scale") {
                val scales = listOf(0.5f, 0.75f, 1.0f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    scales.forEach { s ->
                        val isSelected = resolutionScale == s
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.surfaceVariant else colors.background)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) colors.primary else colors.outline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { resolutionScale = s }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${(s * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) colors.primary else colors.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // S16: Save Location card
            SettingsSectionCard(title = "Save Location") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Save to: $currentFolderName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                        Text(
                            text = "Direct export into this folder without prompting each time.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { folderPickerLauncher.launch(null) }) {
                        Text("Change", color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // S2: Save project (.sg) on Export screen
            SettingsSectionCard(title = "Save Project File (.sg)") {
                Text(
                    text = "Export the entire editable project archive including all layers, vectors, typography, history, and embedded assets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val fileName = "${doc.name.replace(" ", "_")}.sg"
                            saveSgFileLauncher.launch(fileName)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save .sg", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
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
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share .sg")
                    }
                }
            }

            // Export Actions (SAF or Share Sheet)
            Column(
                modifier = Modifier.navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val fileName = "${doc.name.replace(" ", "_")}.${selectedFormat.extension}"
                        saveFileLauncher.launch(fileName)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Device (SAF)", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
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
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share via Share Sheet")
                }
            }

            if (isExporting) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            }
        }
    }
}
