package com.motionssalt.saltygraphics.uikit.sublibs.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.motionssalt.saltygraphics.uikit.controls.DropdownOption
import com.motionssalt.saltygraphics.uikit.controls.SaltyInlineDropdown
import com.motionssalt.saltygraphics.uikit.controls.SaltyProgressSlider
import com.motionssalt.saltygraphics.uikit.controls.SaltySwitch
import com.motionssalt.saltygraphics.uikit.controls.SaltyTextField
import com.motionssalt.saltygraphics.uikit.structure.SaltyActionRow
import com.motionssalt.saltygraphics.uikit.structure.SaltyButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyColumn
import com.motionssalt.saltygraphics.uikit.structure.SaltyDivider
import com.motionssalt.saltygraphics.uikit.structure.SaltyInfoRow
import com.motionssalt.saltygraphics.uikit.structure.SaltyJustify
import com.motionssalt.saltygraphics.uikit.structure.SaltyRow
import com.motionssalt.saltygraphics.uikit.structure.SaltyScreenScaffold
import com.motionssalt.saltygraphics.uikit.structure.SaltySectionCard
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltySpacer
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextStyle
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextTone
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextWeight
import com.motionssalt.saltygraphics.uikit.structure.saltyClickable
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxSize
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxWidth
import com.motionssalt.saltygraphics.uikit.structure.saltyPadding

data class SaltySettingsSocialLink(
    val label: String,
    val detail: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

data class SaltySettingsDeviceInfoItem(
    val label: String,
    val value: String
)

@Composable
fun <T> SaltySettingsScreen(
    themeSelected: T,
    themeOptions: List<DropdownOption<T>>,
    onThemeSelect: (T) -> Unit,
    versionText: String,
    onVersionTap: () -> Unit,
    savedFolderName: String,
    onChangeSaveFolder: () -> Unit,
    devOptionsEnabled: Boolean,
    debugEffectsEnabled: Boolean,
    onDebugEffectsChange: (Boolean) -> Unit,
    useSystemDefaultScale: Boolean,
    onUseSystemDefaultScaleChange: (Boolean) -> Unit,
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit,
    issueText: String,
    onIssueTextChange: (String) -> Unit,
    onShareReport: () -> Unit,
    socialLinks: List<SaltySettingsSocialLink>,
    deviceInfoItems: List<SaltySettingsDeviceInfoItem>,
    onNavigateBack: () -> Unit,
    extraSections: @Composable () -> Unit = {}
) {
    SaltyScreenScaffold(
        title = "Settings",
        onBack = onNavigateBack
    ) { innerPadding ->
        SaltyColumn(
            modifier = Modifier
                .saltyFillMaxSize()
                .saltyPadding(innerPadding)
                .saltyPadding(horizontal = SaltySpace.LG, vertical = SaltySpace.SM),
            spacing = SaltySpace.XL,
            scrollable = true
        ) {
            SaltySectionCard(title = "Appearance") {
                SaltyInlineDropdown(
                    label = "Theme",
                    selected = themeSelected,
                    options = themeOptions,
                    onSelect = onThemeSelect
                )
            }

            SaltySectionCard(title = "About Salty Graphics") {
                SaltyText(
                    text = "Salty Graphics is a hybrid raster and vector graphic design application built by Victor (solo developer and motion designer, brand MOTIONSALT). Designed for mobile creators with an offline-first GPU render engine and branching action tree history.",
                    style = SaltyTextStyle.BODY_MEDIUM,
                    tone = SaltyTextTone.SECONDARY
                )
                SaltySpacer(SaltySpace.SM)
                SaltyText(
                    text = versionText,
                    style = SaltyTextStyle.LABEL_SMALL,
                    tone = SaltyTextTone.ACCENT,
                    modifier = Modifier.saltyClickable { onVersionTap() }
                )
            }

            SaltySectionCard(title = "Save Location") {
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    justify = SaltyJustify.SPACE_BETWEEN,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaltyColumn(modifier = Modifier.weight(1f), spacing = SaltySpace.XXS) {
                        SaltyText(text = savedFolderName, style = SaltyTextStyle.BODY_LARGE)
                        SaltyText(
                            text = "Default directory for direct exports and project files.",
                            style = SaltyTextStyle.BODY_SMALL,
                            tone = SaltyTextTone.SECONDARY
                        )
                    }
                    SaltyTextButton(text = "Change", onClick = onChangeSaveFolder)
                }
            }

            if (devOptionsEnabled) {
                SaltySectionCard(title = "Developer Options") {
                    SaltyRow(
                        modifier = Modifier.saltyFillMaxWidth(),
                        justify = SaltyJustify.SPACE_BETWEEN,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SaltyColumn(modifier = Modifier.weight(1f), spacing = SaltySpace.XXS) {
                            SaltyText(text = "Enable Debug Test Effects", style = SaltyTextStyle.BODY_LARGE)
                            SaltyText(
                                text = "Exposes Invert, Brightness, and Tint test effects and the developer template feature in the editor.",
                                style = SaltyTextStyle.BODY_SMALL,
                                tone = SaltyTextTone.SECONDARY
                            )
                        }
                        SaltySwitch(
                            checked = debugEffectsEnabled,
                            onCheckedChange = onDebugEffectsChange
                        )
                    }
                }
            }

            SaltySectionCard(title = "Display & UI Scaling") {
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    justify = SaltyJustify.SPACE_BETWEEN,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaltyColumn(modifier = Modifier.weight(1f), spacing = SaltySpace.XXS) {
                        SaltyText(text = "Use system default scale", style = SaltyTextStyle.BODY_LARGE)
                        SaltyText(
                            text = if (useSystemDefaultScale) "Currently using system display scale" else "Custom scale: ${(uiScale * 100).toInt()}%",
                            style = SaltyTextStyle.BODY_SMALL,
                            tone = SaltyTextTone.SECONDARY
                        )
                    }
                    SaltySwitch(
                        checked = useSystemDefaultScale,
                        onCheckedChange = onUseSystemDefaultScaleChange
                    )
                }
                if (!useSystemDefaultScale) {
                    SaltySpacer(SaltySpace.MD)
                    SaltyProgressSlider(
                        label = "UI Scale",
                        value = uiScale,
                        onValueChange = onUiScaleChange,
                        range = 0.75f..1.50f,
                        step = 0.05f,
                        defaultValue = 1.0f,
                        unit = "%"
                    )
                }
            }

            extraSections()

            SaltySectionCard(title = "Report a Problem") {
                SaltyText(
                    text = "Describe any issue, unexpected behavior, or suggestion. Submitting generates a diagnostic report shared via the Android share sheet (works 100% offline with zero backend).",
                    style = SaltyTextStyle.BODY_MEDIUM,
                    tone = SaltyTextTone.SECONDARY
                )
                SaltySpacer(SaltySpace.SM)
                SaltyTextField(
                    value = issueText,
                    onValueChange = onIssueTextChange,
                    placeholder = "Write down issues or feedback here...",
                    singleLine = false,
                    minLines = 4,
                    maxLines = 6
                )
                SaltySpacer(SaltySpace.MD)
                SaltyButton(
                    text = "Share Diagnostic Report",
                    icon = Icons.Default.Share,
                    fillWidth = true,
                    onClick = onShareReport
                )
            }

            SaltySectionCard(title = "Connect & Support") {
                socialLinks.forEachIndexed { index, link ->
                    if (index > 0) SaltyDivider()
                    SaltyActionRow(
                        label = link.label,
                        detail = link.detail,
                        icon = link.icon,
                        onClick = link.onClick
                    )
                }
            }

            SaltySectionCard(title = "Device Info") {
                deviceInfoItems.forEach { info ->
                    SaltyInfoRow(info.label, info.value)
                }
            }

            SaltySpacer(SaltySpace.XXL)
        }
    }
}
