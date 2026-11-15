package com.motionssalt.saltygraphics.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.motionssalt.saltygraphics.BuildConfig
import com.motionssalt.saltygraphics.config.SocialConfig
import com.motionssalt.saltygraphics.data.DeviceInfoProvider
import com.motionssalt.saltygraphics.data.SettingsManager
import com.motionssalt.saltygraphics.data.StorageManager
import com.motionssalt.saltygraphics.data.ThemeChoice
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
import com.motionssalt.saltygraphics.uikit.structure.SaltyRow
import com.motionssalt.saltygraphics.uikit.structure.SaltyScreenScaffold
import com.motionssalt.saltygraphics.uikit.structure.SaltySectionCard
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltySpacer
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextStyle
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextTone
import com.motionssalt.saltygraphics.uikit.structure.saltyClickable
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxSize
import com.motionssalt.saltygraphics.uikit.structure.saltyPadding
import com.motionssalt.saltygraphics.uikit.structure.SaltyRow as SharedRow

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    deviceInfoProvider: DeviceInfoProvider,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val theme by settingsManager.theme.collectAsState()
    val uiScale by settingsManager.uiScale.collectAsState()
    val useSystemDefault by settingsManager.useSystemDefault.collectAsState()
    var issueText by remember { mutableStateOf("") }
    var versionTapCount by remember { mutableIntStateOf(0) }
    var devOptionsEnabled by remember {
        mutableStateOf(
            context.getSharedPreferences("salty_dev_prefs", Context.MODE_PRIVATE)
                .getBoolean("dev_options", false)
        )
    }
    var debugEffectsEnabled by remember {
        mutableStateOf(
            context.getSharedPreferences("salty_dev_prefs", Context.MODE_PRIVATE)
                .getBoolean("debug_effects", false)
        )
    }
    var currentSavedFolderName by remember { mutableStateOf(StorageManager.getSavedFolderName(context)) }
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            StorageManager.setSavedFolder(context, treeUri)
            currentSavedFolderName = StorageManager.getSavedFolderName(context)
            Toast.makeText(context, "Save folder updated", Toast.LENGTH_SHORT).show()
        }
    }
    val deviceInfo = remember { deviceInfoProvider.getDeviceInfo() }

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
                    selected = theme,
                    options = ThemeChoice.entries.map { DropdownOption(it, it.displayName) },
                    onSelect = { settingsManager.setTheme(it) }
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
                    text = "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) | Developer: ${SocialConfig.DEVELOPER_NAME} (${SocialConfig.BRAND_NAME})",
                    style = SaltyTextStyle.LABEL_SMALL,
                    tone = SaltyTextTone.ACCENT,
                    modifier = Modifier.saltyClickable {
                        versionTapCount++
                        if (versionTapCount >= 7 && !devOptionsEnabled) {
                            devOptionsEnabled = true
                            context.getSharedPreferences("salty_dev_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("dev_options", true)
                                .apply()
                            Toast.makeText(context, "Developer options unlocked!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            SaltySectionCard(title = "Save Location") {
                SharedRow(
                    label = currentSavedFolderName,
                    subtitle = "Default directory for direct exports and project files."
                ) {
                    SaltyTextButton(
                        text = "Change",
                        onClick = { folderPickerLauncher.launch(null) }
                    )
                }
            }

            if (devOptionsEnabled) {
                SaltySectionCard(title = "Developer Options") {
                    SharedRow(
                        label = "Enable Debug Test Effects",
                        subtitle = "Exposes Invert, Brightness, and Tint test effects in the effects engine to verify adjustment layer compositing."
                    ) {
                        SaltySwitch(
                            checked = debugEffectsEnabled,
                            onCheckedChange = { enabled ->
                                debugEffectsEnabled = enabled
                                context.getSharedPreferences("salty_dev_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("debug_effects", enabled)
                                    .apply()
                            }
                        )
                    }
                }
            }

            SaltySectionCard(title = "Display & UI Scaling") {
                SharedRow(
                    label = "Use system default scale",
                    subtitle = if (useSystemDefault) "Currently using system display scale" else "Custom scale: ${(uiScale * 100).toInt()}%"
                ) {
                    SaltySwitch(
                        checked = useSystemDefault,
                        onCheckedChange = { settingsManager.setUseSystemDefault(it) }
                    )
                }
                if (!useSystemDefault) {
                    SaltySpacer(SaltySpace.MD)
                    SaltyProgressSlider(
                        label = "UI Scale",
                        value = uiScale,
                        onValueChange = { settingsManager.setUiScale(it) },
                        range = 0.75f..1.50f,
                        step = 0.05f,
                        defaultValue = 1.0f,
                        unit = "%"
                    )
                }
            }

            SaltySectionCard(title = "Report a Problem") {
                SaltyText(
                    text = "Describe any issue, unexpected behavior, or suggestion. Submitting generates a diagnostic report shared via the Android share sheet (works 100% offline with zero backend).",
                    style = SaltyTextStyle.BODY_MEDIUM,
                    tone = SaltyTextTone.SECONDARY
                )
                SaltySpacer(SaltySpace.SM)
                SaltyTextField(
                    value = issueText,
                    onValueChange = { issueText = it },
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
                    onClick = {
                        val shareIntent = deviceInfoProvider.composeProblemReport(issueText)
                        val chooser = Intent.createChooser(shareIntent, "Send Problem Report via")
                        context.startActivity(chooser)
                    }
                )
            }

            SaltySectionCard(title = "Connect & Support") {
                SaltyActionRow(
                    label = "WhatsApp (Direct Contact)",
                    detail = SocialConfig.WHATSAPP_PHONE,
                    icon = Icons.Default.Chat,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.WHATSAPP_URL))) }
                )
                SaltyDivider()
                SaltyActionRow(
                    label = "Instagram",
                    detail = SocialConfig.INSTAGRAM_HANDLE,
                    icon = Icons.Default.CameraAlt,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.INSTAGRAM_URL))) }
                )
                SaltyDivider()
                SaltyActionRow(
                    label = "Threads",
                    detail = SocialConfig.THREADS_HANDLE,
                    icon = Icons.Default.AlternateEmail,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.THREADS_URL))) }
                )
                SaltyDivider()
                SaltyActionRow(
                    label = "X / Twitter",
                    detail = SocialConfig.X_HANDLE,
                    icon = Icons.Default.Tag,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.X_URL))) }
                )
                SaltyDivider()
                SaltyActionRow(
                    label = "YouTube",
                    detail = SocialConfig.YOUTUBE_HANDLE,
                    icon = Icons.Default.PlayCircle,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.YOUTUBE_URL))) }
                )
                SaltyDivider()
                SaltyActionRow(
                    label = "TikTok",
                    detail = SocialConfig.TIKTOK_HANDLE,
                    icon = Icons.Default.Movie,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.TIKTOK_URL))) }
                )
            }

            SaltySectionCard(title = "Device Info") {
                SaltyInfoRow("Device Model", "${deviceInfo.manufacturer} ${deviceInfo.model}")
                SaltyInfoRow("Android Version", "${deviceInfo.androidVersion} (API ${deviceInfo.sdkInt})")
                SaltyInfoRow("Supported ABIs", deviceInfo.supportedAbis)
                SaltyInfoRow("Screen Resolution", deviceInfo.screenSize)
                SaltyInfoRow("Memory (RAM)", "${deviceInfo.availableRamMB} MB free / ${deviceInfo.totalRamMB} MB total")
                SaltyInfoRow("GPU Renderer", deviceInfo.gpuRenderer)
                SaltyInfoRow("Max Texture Limit", "${deviceInfo.maxTextureSize} x ${deviceInfo.maxTextureSize} px")
            }

            SaltySpacer(SaltySpace.XXL)
        }
    }
}
