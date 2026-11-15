package com.motionssalt.saltygraphics.ui.newproject

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.engine.model.AspectRatioPreset
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.QualityPreset
import com.motionssalt.saltygraphics.uikit.controls.SaltyAspectRatioTile
import com.motionssalt.saltygraphics.uikit.controls.SaltyTextField
import com.motionssalt.saltygraphics.uikit.controls.SaltyValueEntryStrip
import com.motionssalt.saltygraphics.uikit.structure.SaltyAnimatedSection
import com.motionssalt.saltygraphics.uikit.structure.SaltyBox
import com.motionssalt.saltygraphics.uikit.structure.SaltyButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyCard
import com.motionssalt.saltygraphics.uikit.structure.SaltyColumn
import com.motionssalt.saltygraphics.uikit.structure.SaltyJustify
import com.motionssalt.saltygraphics.uikit.structure.SaltyOutlinedButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyRow
import com.motionssalt.saltygraphics.uikit.structure.SaltyScreenScaffold
import com.motionssalt.saltygraphics.uikit.structure.SaltySectionCard
import com.motionssalt.saltygraphics.uikit.structure.SaltySelectableTile
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltySpacer
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextStyle
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextTone
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextWeight
import com.motionssalt.saltygraphics.uikit.structure.SaltyWarningCard
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxSize
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxWidth
import com.motionssalt.saltygraphics.uikit.structure.saltyPadding
import kotlinx.coroutines.launch

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
    var isRotated by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var customWidth by remember { mutableIntStateOf(1080) }
    var customHeight by remember { mutableIntStateOf(1920) }
    var editingDimension by remember { mutableStateOf<String?>(null) }

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

    @Composable
    fun RatioPresetTile(
        preset: AspectRatioPreset,
        modifier: Modifier = Modifier,
        compactWidth: Boolean = false
    ) {
        val ratioW = if (isRotated && preset != AspectRatioPreset.CUSTOM && preset != AspectRatioPreset.SQUARE) preset.ratioHeight else preset.ratioWidth
        val ratioH = if (isRotated && preset != AspectRatioPreset.CUSTOM && preset != AspectRatioPreset.SQUARE) preset.ratioWidth else preset.ratioHeight
        val displayRatio = if (isRotated && preset != AspectRatioPreset.CUSTOM && preset != AspectRatioPreset.SQUARE) {
            val wStr = if (preset.ratioHeight % 1f == 0f) preset.ratioHeight.toInt().toString() else preset.ratioHeight.toString()
            val hStr = if (preset.ratioWidth % 1f == 0f) preset.ratioWidth.toInt().toString() else preset.ratioWidth.toString()
            "$wStr:$hStr"
        } else {
            preset.displayName
        }
        SaltyAspectRatioTile(
            ratioWidth = ratioW,
            ratioHeight = ratioH,
            displayRatio = displayRatio,
            shortName = preset.shortName,
            isSelected = selectedRatio == preset,
            onClick = { selectedRatio = preset },
            modifier = modifier,
            compactWidth = compactWidth
        )
    }

    @Composable
    fun RatioCategoryGrid(title: String, presets: List<AspectRatioPreset>) {
        SaltyText(
            text = title,
            style = SaltyTextStyle.LABEL_MEDIUM,
            tone = SaltyTextTone.ACCENT,
            weight = SaltyTextWeight.SEMI_BOLD
        )
        presets.chunked(3).forEach { row ->
            SaltyRow(
                modifier = Modifier.saltyFillMaxWidth(),
                spacing = SaltySpace.SM
            ) {
                row.forEach { ratio ->
                    RatioPresetTile(preset = ratio, modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) {
                    SaltyBox(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }

    SaltyScreenScaffold(
        title = "New Project",
        onBack = onNavigateBack,
        bottomBar = {
            SaltyColumn(modifier = Modifier.saltyFillMaxWidth()) {
                if (editingDimension != null) {
                    val isWidth = editingDimension == "width"
                    SaltyValueEntryStrip(
                        label = if (isWidth) "Canvas Width" else "Canvas Height",
                        currentValue = (if (isWidth) customWidth else customHeight).toFloat(),
                        range = 100f..8192f,
                        unit = "px",
                        onConfirm = { v ->
                            if (isWidth) customWidth = v.toInt() else customHeight = v.toInt()
                            editingDimension = null
                        },
                        onDismiss = { editingDimension = null }
                    )
                }
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    padding = SaltySpace.LG,
                    justify = SaltyJustify.SPACE_BETWEEN,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaltyColumn(spacing = SaltySpace.XXS) {
                        SaltyText(
                            text = "${canvasSize.width} x ${canvasSize.height} px",
                            style = SaltyTextStyle.TITLE_MEDIUM,
                            tone = SaltyTextTone.ACCENT,
                            weight = SaltyTextWeight.BOLD
                        )
                        val rotSuffix = if (isRotated && selectedRatio != AspectRatioPreset.CUSTOM && selectedRatio != AspectRatioPreset.SQUARE) " (Rotated)" else ""
                        SaltyText(
                            text = "${canvasSize.aspectRatio.displayName}$rotSuffix • ${canvasSize.quality.displayName}",
                            style = SaltyTextStyle.BODY_MEDIUM,
                            tone = SaltyTextTone.SECONDARY
                        )
                    }
                    SaltyButton(
                        text = "Create Canvas",
                        onClick = {
                            coroutineScope.launch {
                                val doc = repository.createProject(projectName, canvasSize)
                                onProjectCreated(doc.id)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        SaltyColumn(
            modifier = Modifier
                .saltyFillMaxSize()
                .saltyPadding(innerPadding)
                .saltyPadding(horizontal = SaltySpace.LG, vertical = SaltySpace.SM),
            spacing = SaltySpace.LG,
            scrollable = true
        ) {
            SaltySectionCard(title = "Project Identity") {
                SaltyTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = "Canvas Title"
                )
            }

            SaltySectionCard(title = "Aspect Ratio") {
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    justify = SaltyJustify.SPACE_BETWEEN,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaltyText(
                        text = "Visual proportion shapes:",
                        style = SaltyTextStyle.BODY_MEDIUM,
                        tone = SaltyTextTone.SECONDARY
                    )
                    SaltyOutlinedButton(
                        text = if (isRotated) "Rotated" else "Rotate",
                        icon = Icons.Default.ScreenRotation,
                        onClick = { isRotated = !isRotated }
                    )
                }
                SaltySpacer(SaltySpace.SM)
                SaltyText(
                    text = "Most Used",
                    style = SaltyTextStyle.LABEL_MEDIUM,
                    tone = SaltyTextTone.ACCENT,
                    weight = SaltyTextWeight.SEMI_BOLD
                )
                SaltySpacer(SaltySpace.XS)
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    spacing = SaltySpace.SM,
                    scrollable = true
                ) {
                    primaryRatios.forEach { ratio ->
                        RatioPresetTile(preset = ratio, compactWidth = true)
                    }
                    SaltyAspectRatioTile(
                        ratioWidth = customWidth.toFloat(),
                        ratioHeight = customHeight.toFloat(),
                        displayRatio = "Custom",
                        shortName = "${customWidth}x${customHeight}",
                        isSelected = selectedRatio == AspectRatioPreset.CUSTOM,
                        onClick = { selectedRatio = AspectRatioPreset.CUSTOM },
                        compactWidth = true
                    )
                }
                SaltySpacer(SaltySpace.SM)
                SaltyTextButton(
                    text = if (isExpanded) "Hide Expanded Ratios" else "Show All Ratios & Formats (${standardRatios.size + socialRatios.size + paperRatios.size}+)",
                    icon = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    onClick = { isExpanded = !isExpanded }
                )

                SaltyAnimatedSection(visible = isExpanded) {
                    SaltyColumn(spacing = SaltySpace.MD) {
                        RatioCategoryGrid("Display & Standard", standardRatios)
                        RatioCategoryGrid("Social Media", socialRatios)
                        RatioCategoryGrid("Paper & Print", paperRatios)
                    }
                }

                if (selectedRatio == AspectRatioPreset.CUSTOM) {
                    SaltySpacer(SaltySpace.MD)
                    SaltyText(
                        text = "Custom Dimensions (tap value pill to edit with keyboard):",
                        style = SaltyTextStyle.BODY_SMALL,
                        tone = SaltyTextTone.SECONDARY
                    )
                    SaltySpacer(SaltySpace.XS)
                    SaltyRow(
                        modifier = Modifier.saltyFillMaxWidth(),
                        spacing = SaltySpace.MD
                    ) {
                        SaltyCard(
                            modifier = Modifier.weight(1f),
                            isSelected = editingDimension == "width",
                            padding = SaltySpace.MD,
                            spacing = SaltySpace.XXS,
                            onClick = { editingDimension = "width" }
                        ) {
                            SaltyText("Width", style = SaltyTextStyle.LABEL_SMALL, tone = SaltyTextTone.SECONDARY)
                            SaltyText("$customWidth px", style = SaltyTextStyle.TITLE_MEDIUM, tone = SaltyTextTone.ACCENT, weight = SaltyTextWeight.BOLD)
                        }
                        SaltyCard(
                            modifier = Modifier.weight(1f),
                            isSelected = editingDimension == "height",
                            padding = SaltySpace.MD,
                            spacing = SaltySpace.XXS,
                            onClick = { editingDimension = "height" }
                        ) {
                            SaltyText("Height", style = SaltyTextStyle.LABEL_SMALL, tone = SaltyTextTone.SECONDARY)
                            SaltyText("$customHeight px", style = SaltyTextStyle.TITLE_MEDIUM, tone = SaltyTextTone.ACCENT, weight = SaltyTextWeight.BOLD)
                        }
                    }
                }
            }

            SaltySectionCard(title = "Canvas Quality & Resolution") {
                SaltyText(
                    text = "Quality determines the base render resolution:",
                    style = SaltyTextStyle.BODY_MEDIUM,
                    tone = SaltyTextTone.SECONDARY
                )
                SaltySpacer(SaltySpace.SM)
                val qualities = listOf(
                    QualityPreset.P360,
                    QualityPreset.P720,
                    QualityPreset.P1080,
                    QualityPreset.P1440,
                    QualityPreset.P4K,
                    QualityPreset.P8K
                )
                SaltyColumn(spacing = SaltySpace.SM) {
                    qualities.forEach { quality ->
                        val isSelected = selectedQuality == quality
                        val dims = if (selectedRatio == AspectRatioPreset.CUSTOM) {
                            Pair(customWidth, customHeight)
                        } else {
                            selectedRatio.computeDimensions(quality, isRotated)
                        }
                        SaltySelectableTile(
                            title = quality.displayName,
                            subtitle = "Canvas Dimension: ${dims.first} x ${dims.second} px",
                            badge = if (quality == QualityPreset.P8K) "HEAVY" else null,
                            selected = isSelected,
                            onClick = { selectedQuality = quality }
                        )
                    }
                }
            }

            if (isHeavyCanvas) {
                SaltyWarningCard(
                    title = "High Resolution Memory Notice",
                    message = "An 8K or large canvas consumes approximately ~130 MB of GPU VRAM per full-size RGBA layer. Salty Graphics utilizes preview proxy downsampling and texture capping to maintain responsive editing and prevent device crashes."
                )
            }

            SaltySpacer(SaltySpace.XXL)
        }
    }
}
