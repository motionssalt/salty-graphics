package com.motionssalt.saltygraphics.ui.newproject

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.engine.model.AspectRatioPreset
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.QualityPreset
import com.motionssalt.saltygraphics.theme.*
import com.motionssalt.saltygraphics.ui.settings.SettingsSectionCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    repository: ProjectRepository,
    onNavigateBack: () -> Unit,
    onProjectCreated: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var projectName by remember { mutableStateOf("Untitled Project") }
    var selectedRatio by remember { mutableStateOf(AspectRatioPreset.STORY_9_16) }
    var selectedQuality by remember { mutableStateOf(QualityPreset.P1080) }

    var customWidthText by remember { mutableStateOf("1080") }
    var customHeightText by remember { mutableStateOf("1920") }

    val computedDims = remember(selectedRatio, selectedQuality, customWidthText, customHeightText) {
        if (selectedRatio == AspectRatioPreset.CUSTOM) {
            val w = customWidthText.toIntOrNull()?.coerceIn(100, 8192) ?: 1080
            val h = customHeightText.toIntOrNull()?.coerceIn(100, 8192) ?: 1920
            Pair(w, h)
        } else {
            selectedRatio.computeDimensions(selectedQuality)
        }
    }

    val canvasSize = remember(computedDims, selectedRatio, selectedQuality) {
        CanvasSize(
            width = computedDims.first,
            height = computedDims.second,
            aspectRatio = selectedRatio,
            quality = selectedQuality
        )
    }

    val isHeavyCanvas = canvasSize.isHeavyMemory || selectedQuality == QualityPreset.P8K

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Project", style = MaterialTheme.typography.titleLarge) },
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
        bottomBar = {
            Surface(
                color = SaltySurfaceDark,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "${canvasSize.width} x ${canvasSize.height} px",
                            style = MaterialTheme.typography.titleMedium,
                            color = SaltyCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${canvasSize.aspectRatio.displayName} • ${canvasSize.quality.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SaltyTextSecondary
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val doc = repository.createProject(projectName, canvasSize)
                                onProjectCreated(doc.id)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SaltyCyan,
                            contentColor = SaltyDarkBackground
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Text("Create Canvas", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = SaltyDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Name Input
            SettingsSectionCard(title = "Project Identity") {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Canvas Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SaltyCyan,
                        focusedLabelColor = SaltyCyan,
                        unfocusedBorderColor = SaltyOutlineDark
                    )
                )
            }

            // Aspect Ratio Templates (Section 3.4)
            SettingsSectionCard(title = "Aspect Ratio Template") {
                Text(
                    "Select standard design proportion or custom dimensions:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SaltyTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                val ratios = listOf(
                    AspectRatioPreset.STORY_9_16,
                    AspectRatioPreset.SQUARE,
                    AspectRatioPreset.PORTRAIT_4_5,
                    AspectRatioPreset.LANDSCAPE_16_9,
                    AspectRatioPreset.PHOTO_3_2,
                    AspectRatioPreset.A4,
                    AspectRatioPreset.CUSTOM
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ratios.chunked(2).forEach { rowRatios ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowRatios.forEach { ratio ->
                                val isSelected = selectedRatio == ratio
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
                                        .clickable { selectedRatio = ratio }
                                        .padding(vertical = 12.dp, horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ratio.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) SaltyCyan else SaltyTextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                            if (rowRatios.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (selectedRatio == AspectRatioPreset.CUSTOM) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customWidthText,
                            onValueChange = { customWidthText = it.filter { char -> char.isDigit() } },
                            label = { Text("Width (px)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SaltyCyan)
                        )
                        OutlinedTextField(
                            value = customHeightText,
                            onValueChange = { customHeightText = it.filter { char -> char.isDigit() } },
                            label = { Text("Height (px)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SaltyCyan)
                        )
                    }
                }
            }

            // Quality Presets (360p up to 8K)
            SettingsSectionCard(title = "Canvas Quality & Resolution") {
                Text(
                    "Quality determines the underlying render resolution:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SaltyTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                val qualities = listOf(
                    QualityPreset.P360,
                    QualityPreset.P720,
                    QualityPreset.P1080,
                    QualityPreset.P1440,
                    QualityPreset.P4K,
                    QualityPreset.P8K
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    qualities.forEach { quality ->
                        val isSelected = selectedQuality == quality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SaltySurfaceVariantDark else SaltyDarkBackground)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) SaltyCyan else SaltyOutlineDark,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedQuality = quality }
                                .padding(vertical = 12.dp, horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = quality.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) SaltyCyan else SaltyTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "Base Dimension: ${quality.baseDimension}px",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SaltyTextSecondary
                                )
                            }

                            if (quality.isHeavy) {
                                Surface(
                                    color = SaltyAmber.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "VRAM Heavy",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SaltyAmber,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 8K Safe Memory Warning Dialog (Section 3.4)
            if (isHeavyCanvas) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SaltyAmber.copy(alpha = 0.15f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SaltyAmber))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SaltyAmber, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "High Resolution Memory Notice",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SaltyAmber
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "An 8K or large canvas consumes approximately ~130 MB of GPU VRAM per full-size RGBA layer. Salty Graphics utilizes preview proxy downsampling and texture capping to maintain responsive editing and prevent device crashes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SaltyTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
