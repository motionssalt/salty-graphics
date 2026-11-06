package com.motionssalt.saltygraphics.ui.export

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

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val halfW = finalWidth / 2f
        val halfH = finalHeight / 2f

        for (layer in doc.layers) {
            if (!layer.isVisible) continue
            val lx = halfW + (layer.transform.translationX * resolutionScale)
            val ly = halfH + (layer.transform.translationY * resolutionScale)

            canvas.save()
            canvas.translate(lx, ly)
            canvas.rotate(layer.transform.rotationDegrees)
            canvas.scale(layer.transform.scaleX * resolutionScale, layer.transform.scaleY * resolutionScale)

            when (layer.type) {
                LayerType.SHAPE -> {
                    val s = layer.shapeData
                    if (s != null) {
                        paint.color = s.fillColor.toInt()
                        canvas.drawRect(-s.width / 2f, -s.height / 2f, s.width / 2f, s.height / 2f, paint)
                    }
                }
                LayerType.TEXT -> {
                    val t = layer.textData
                    paint.color = (t?.color ?: 0xFFFFFFFF).toInt()
                    paint.textSize = (t?.fontSize ?: 64f) * resolutionScale
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(t?.text ?: "Text", 0f, 0f, paint)
                }
                else -> {
                    paint.color = android.graphics.Color.DKGRAY
                    canvas.drawRect(-150f, -150f, 150f, 150f, paint)
                }
            }
            canvas.restore()
        }
        return bitmap
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
                    containerColor = SaltyDarkBackground,
                    titleContentColor = SaltyTextPrimary,
                    navigationIconContentColor = SaltyTextPrimary
                )
            )
        },
        containerColor = SaltyDarkBackground
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
                    color = SaltyCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Export always renders from the master document model at full quality (never downsampled preview proxy).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SaltyTextSecondary
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
                                .background(if (isSelected) SaltySurfaceVariantDark else SaltyDarkBackground)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) SaltyCyan else SaltyOutlineDark,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFormat = format }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = format.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) SaltyCyan else SaltyTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (selectedFormat != ExportFormat.PNG) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Quality: ${exportQualityPercent.toInt()}%", style = MaterialTheme.typography.bodyMedium, color = SaltyTextPrimary)
                    Slider(
                        value = exportQualityPercent,
                        onValueChange = { exportQualityPercent = it },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(thumbColor = SaltyCyan, activeTrackColor = SaltyCyan)
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
                                .background(if (isSelected) SaltySurfaceVariantDark else SaltyDarkBackground)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) SaltyCyan else SaltyOutlineDark,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { resolutionScale = s }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${(s * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) SaltyCyan else SaltyTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
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
                    colors = ButtonDefaults.buttonColors(containerColor = SaltyCyan, contentColor = SaltyDarkBackground)
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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SaltyCyan)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share via Share Sheet")
                }
            }

            if (isExporting) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SaltyCyan)
                }
            }
        }
    }
}
