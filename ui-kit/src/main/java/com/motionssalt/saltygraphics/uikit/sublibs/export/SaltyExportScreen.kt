package com.motionssalt.saltygraphics.uikit.sublibs.export

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.uikit.controls.SaltyProgressSlider
import com.motionssalt.saltygraphics.uikit.structure.SaltyBox
import com.motionssalt.saltygraphics.uikit.structure.SaltyButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyColumn
import com.motionssalt.saltygraphics.uikit.structure.SaltyJustify
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

enum class SaltyExportFormat(val displayName: String, val extension: String, val mimeType: String) {
    PNG("PNG (Lossless)", "png", "image/png"),
    JPEG("JPEG (Photo)", "jpg", "image/jpeg"),
    WEBP("WebP (Modern)", "webp", "image/webp")
}

@Composable
fun SaltyExportScreen(
    isLoading: Boolean,
    finalWidth: Int,
    finalHeight: Int,
    selectedFormat: SaltyExportFormat,
    onSelectFormat: (SaltyExportFormat) -> Unit,
    exportQualityPercent: Float,
    onQualityChange: (Float) -> Unit,
    resolutionScale: Float,
    onResolutionScaleChange: (Float) -> Unit,
    currentFolderName: String,
    onChangeFolder: () -> Unit,
    onExportSg: () -> Unit,
    onSaveImage: () -> Unit,
    onShareImage: () -> Unit,
    isExporting: Boolean,
    onNavigateBack: () -> Unit
) {
    if (isLoading) {
        SaltyBox(
            modifier = Modifier.saltyFillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            SaltySpinner()
        }
        return
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
                    SaltyExportFormat.entries.forEach { format ->
                        SaltySelectableTile(
                            title = format.displayName,
                            selected = selectedFormat == format,
                            onClick = { onSelectFormat(format) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (selectedFormat != SaltyExportFormat.PNG) {
                    SaltySpacer(SaltySpace.MD)
                    SaltyProgressSlider(
                        label = "Quality",
                        value = exportQualityPercent,
                        onValueChange = onQualityChange,
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
                            onClick = { onResolutionScaleChange(s) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            SaltySectionCard(title = "Save Location") {
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    justify = SaltyJustify.SPACE_BETWEEN,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaltyText(
                        text = currentFolderName,
                        style = SaltyTextStyle.BODY_MEDIUM,
                        tone = SaltyTextTone.SECONDARY,
                        modifier = Modifier.weight(1f)
                    )
                    SaltyTextButton(text = "Change", onClick = onChangeFolder)
                }
            }

            SaltySectionCard(title = "Project Archive (.sg)") {
                SaltyOutlinedButton(
                    text = "Export .sg Project File",
                    icon = Icons.Default.Archive,
                    fillWidth = true,
                    onClick = onExportSg
                )
            }

            SaltyColumn(
                modifier = Modifier
                    .saltyFillMaxWidth()
                    .saltyNavigationBarsPadding(),
                spacing = SaltySpace.SM
            ) {
                SaltyButton(
                    text = "Save to Device (SAF)",
                    icon = Icons.Default.FileDownload,
                    fillWidth = true,
                    onClick = onSaveImage
                )
                SaltyOutlinedButton(
                    text = "Share via Share Sheet",
                    icon = Icons.Default.Share,
                    fillWidth = true,
                    onClick = onShareImage
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
