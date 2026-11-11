package com.motionssalt.saltygraphics.ui.newproject

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.engine.model.AspectRatioCategory
import com.motionssalt.saltygraphics.engine.model.AspectRatioPreset
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.QualityPreset
import com.motionssalt.saltygraphics.ui.controls.ValueEditorDialog
import com.motionssalt.saltygraphics.ui.settings.SettingsSectionCard
import kotlinx.coroutines.launch

@Composable
fun AspectRatioTile(
    preset: AspectRatioPreset,
    isSelected: Boolean,
    isRotated: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val rw = if (isRotated && preset != AspectRatioPreset.CUSTOM && preset != AspectRatioPreset.SQUARE) preset.ratioHeight else preset.ratioWidth
    val rh = if (isRotated && preset != AspectRatioPreset.CUSTOM && preset != AspectRatioPreset.SQUARE) preset.ratioWidth else preset.ratioHeight

    val maxBox = 34f
    val (boxW, boxH) = if (rw >= rh) {
        Pair(maxBox, (maxBox * (rh / rw)).coerceIn(8f, maxBox))
    } else {
        Pair((maxBox * (rw / rh)).coerceIn(8f, maxBox), maxBox)
    }

    val displayRatio = if (isRotated && preset != AspectRatioPreset.CUSTOM && preset != AspectRatioPreset.SQUARE) {
        val wStr = if (preset.ratioHeight % 1f == 0f) preset.ratioHeight.toInt().toString() else preset.ratioHeight.toString()
        val hStr = if (preset.ratioWidth % 1f == 0f) preset.ratioWidth.toInt().toString() else preset.ratioWidth.toString()
        "$wStr:$hStr"
    } else {
        preset.displayName
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colors.primary else colors.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(width = 42.dp, height = 42.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = boxW.dp, height = boxH.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) colors.primary else colors.onSurface.copy(alpha = 0.35f))
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = displayRatio,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) colors.primary else colors.onSurface,
                maxLines = 1
            )
            Text(
                text = preset.shortName,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) colors.primary.copy(alpha = 0.85f) else colors.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CustomRatioTile(
    width: Int,
    height: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val rw = width.toFloat().coerceAtLeast(1f)
    val rh = height.toFloat().coerceAtLeast(1f)
    val maxBox = 34f
    val (boxW, boxH) = if (rw >= rh) {
        Pair(maxBox, (maxBox * (rh / rw)).coerceIn(8f, maxBox))
    } else {
        Pair((maxBox * (rw / rh)).coerceIn(8f, maxBox), maxBox)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colors.primary else colors.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(width = 42.dp, height = 42.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = boxW.dp, height = boxH.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) colors.primary else colors.onSurface.copy(alpha = 0.35f))
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Custom",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) colors.primary else colors.onSurface,
                maxLines = 1
            )
            Text(
                text = "${width}x${height}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) colors.primary.copy(alpha = 0.85f) else colors.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    repository: ProjectRepository,
    onNavigateBack: () -> Unit,
    onProjectCreated: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()

    var projectName by remember { mutableStateOf("Untitled Project") }
    var selectedRatio by remember { mutableStateOf(AspectRatioPreset.STORY_9_16) }
    var selectedQuality by remember { mutableStateOf(QualityPreset.P1080) }
    var isRotated by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    var customWidth by remember { mutableIntStateOf(1080) }
    var customHeight by remember { mutableIntStateOf(1920) }

    var editingDimension by remember { mutableStateOf<String?>(null) } // "width" or "height"

    val computedDims = remember(selectedRatio, selectedQuality, customWidth, customHeight, isRotated) {
        if (selectedRatio == AspectRatioPreset.CUSTOM) {
            Pair(customWidth, customHeight)
        } else {
            selectedRatio.computeDimensions(selectedQuality, isRotated)
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

    // Primary most-used ratios
    val primaryRatios = remember {
        listOf(
            AspectRatioPreset.SQUARE,
            AspectRatioPreset.PORTRAIT_4_5,
            AspectRatioPreset.STORY_9_16,
            AspectRatioPreset.LANDSCAPE_16_9,
            AspectRatioPreset.PHOTO_3_2,
            AspectRatioPreset.A4
        )
    }

    // Categories for expanded grid
    val standardRatios = remember {
        listOf(
            AspectRatioPreset.PORTRAIT_3_4,
            AspectRatioPreset.LANDSCAPE_4_3,
            AspectRatioPreset.LANDSCAPE_5_4,
            AspectRatioPreset.PORTRAIT_2_3,
            AspectRatioPreset.ULTRAWIDE_21_9,
            AspectRatioPreset.LANDSCAPE_2_1,
            AspectRatioPreset.PORTRAIT_1_2,
            AspectRatioPreset.BANNER_3_1,
            AspectRatioPreset.SKYSCRAPER_1_3,
            AspectRatioPreset.SMARTPHONE_9_19_5,
            AspectRatioPreset.SMARTPHONE_19_5_9
        )
    }

    val socialRatios = remember {
        listOf(
            AspectRatioPreset.INSTAGRAM_POST,
            AspectRatioPreset.INSTAGRAM_PORTRAIT,
            AspectRatioPreset.INSTAGRAM_STORY,
            AspectRatioPreset.YOUTUBE_THUMBNAIL,
            AspectRatioPreset.FACEBOOK_COVER,
            AspectRatioPreset.X_POST,
            AspectRatioPreset.X_HEADER,
            AspectRatioPreset.LINKEDIN_BANNER,
            AspectRatioPreset.PINTEREST_PIN
        )
    }

    val paperRatios = remember {
        listOf(
            AspectRatioPreset.A3_PORTRAIT,
            AspectRatioPreset.A3_LANDSCAPE,
            AspectRatioPreset.A4_LANDSCAPE,
            AspectRatioPreset.A5_PORTRAIT,
            AspectRatioPreset.A5_LANDSCAPE,
            AspectRatioPreset.US_LETTER_PORTRAIT,
            AspectRatioPreset.US_LETTER_LANDSCAPE,
            AspectRatioPreset.BUSINESS_CARD_LANDSCAPE,
            AspectRatioPreset.BUSINESS_CARD_PORTRAIT
        )
    }

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
                    containerColor = colors.background,
                    titleContentColor = colors.onSurface,
                    navigationIconContentColor = colors.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = colors.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
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
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        val rotSuffix = if (isRotated && selectedRatio != AspectRatioPreset.CUSTOM && selectedRatio != AspectRatioPreset.SQUARE) " (Rotated)" else ""
                        Text(
                            "${canvasSize.aspectRatio.displayName}$rotSuffix • ${canvasSize.quality.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
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
                            containerColor = colors.primary,
                            contentColor = colors.background
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Text("Create Canvas", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Identity
            SettingsSectionCard(title = "Project Identity") {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Canvas Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        focusedLabelColor = colors.primary,
                        unfocusedBorderColor = colors.outline
                    )
                )
            }

            // Aspect Ratio Templates (Section 3.4 & R9)
            SettingsSectionCard(title = "Aspect Ratio") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Visual proportion shapes:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { isRotated = !isRotated },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.ScreenRotation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isRotated) "Rotated" else "Rotate",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Popular / Most Used Row (Scrollable or Row of Tiles)
                Text(
                    "Most Used",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    primaryRatios.forEach { ratio ->
                        AspectRatioTile(
                            preset = ratio,
                            isSelected = selectedRatio == ratio,
                            isRotated = isRotated,
                            onClick = { selectedRatio = ratio },
                            modifier = Modifier.width(82.dp)
                        )
                    }

                    CustomRatioTile(
                        width = customWidth,
                        height = customHeight,
                        isSelected = selectedRatio == AspectRatioPreset.CUSTOM,
                        onClick = { selectedRatio = AspectRatioPreset.CUSTOM },
                        modifier = Modifier.width(82.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Expand / Show More Control
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isExpanded) "Hide Expanded Ratios" else "Show All Ratios & Formats (${standardRatios.size + socialRatios.size + paperRatios.size}+)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Standard Ratios
                        Text(
                            "Display & Standard",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        standardRatios.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { ratio ->
                                    AspectRatioTile(
                                        preset = ratio,
                                        isSelected = selectedRatio == ratio,
                                        isRotated = isRotated,
                                        onClick = { selectedRatio = ratio },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }

                        // Social Media Formats
                        Text(
                            "Social Media",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        socialRatios.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { ratio ->
                                    AspectRatioTile(
                                        preset = ratio,
                                        isSelected = selectedRatio == ratio,
                                        isRotated = isRotated,
                                        onClick = { selectedRatio = ratio },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }

                        // Paper & Print
                        Text(
                            "Paper & Print",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        paperRatios.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { ratio ->
                                    AspectRatioTile(
                                        preset = ratio,
                                        isSelected = selectedRatio == ratio,
                                        isRotated = isRotated,
                                        onClick = { selectedRatio = ratio },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                // Custom Dimensions Input (R5 tap-to-edit values with proper validation)
                if (selectedRatio == AspectRatioPreset.CUSTOM) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Custom Dimensions (tap value pill to edit with keyboard):",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Width Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceVariant,
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.outline)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { editingDimension = "width" }
                                .padding(vertical = 12.dp, horizontal = 14.dp)
                        ) {
                            Column {
                                Text("Width", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                Text("${customWidth} px", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.primary)
                            }
                        }

                        // Height Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceVariant,
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.outline)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { editingDimension = "height" }
                                .padding(vertical = 12.dp, horizontal = 14.dp)
                        ) {
                            Column {
                                Text("Height", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                Text("${customHeight} px", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.primary)
                            }
                        }
                    }
                }
            }

            // Quality Presets (360p up to 8K)
            SettingsSectionCard(title = "Canvas Quality & Resolution") {
                Text(
                    "Quality determines the base render resolution:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
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
                        val dims = if (selectedRatio == AspectRatioPreset.CUSTOM) {
                            Pair(customWidth, customHeight)
                        } else {
                            selectedRatio.computeDimensions(quality, isRotated)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.surfaceVariant else colors.background)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) colors.primary else colors.outline,
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
                                    color = if (isSelected) colors.primary else colors.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "Canvas Dimension: ${dims.first} x ${dims.second} px",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }

                            if (quality.isHeavy) {
                                Surface(
                                    color = colors.secondary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "VRAM Heavy",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.secondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 8K Safe Memory Warning Dialog
            if (isHeavyCanvas) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.secondary.copy(alpha = 0.15f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.secondary))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "High Resolution Memory Notice",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "An 8K or large canvas consumes approximately ~130 MB of GPU VRAM per full-size RGBA layer. Salty Graphics utilizes preview proxy downsampling and texture capping to maintain responsive editing and prevent device crashes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Tap-to-edit Custom Dimension Dialogs (R5 compliant)
    if (editingDimension == "width") {
        ValueEditorDialog(
            label = "Canvas Width",
            currentValue = customWidth.toFloat(),
            minValue = 100f,
            maxValue = 8192f,
            step = 1f,
            unit = "px",
            onValueApplied = { customWidth = it.toInt() },
            onDismiss = { editingDimension = null }
        )
    } else if (editingDimension == "height") {
        ValueEditorDialog(
            label = "Canvas Height",
            currentValue = customHeight.toFloat(),
            minValue = 100f,
            maxValue = 8192f,
            step = 1f,
            unit = "px",
            onValueApplied = { customHeight = it.toInt() },
            onDismiss = { editingDimension = null }
        )
    }
}
