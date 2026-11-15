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
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.motionssalt.saltygraphics.BuildConfig
import com.motionssalt.saltygraphics.config.SocialConfig
import com.motionssalt.saltygraphics.data.DeviceInfoProvider
import com.motionssalt.saltygraphics.data.SettingsManager
import com.motionssalt.saltygraphics.data.StorageManager
import com.motionssalt.saltygraphics.data.ThemeChoice
import com.motionssalt.saltygraphics.uikit.controls.DropdownOption
import com.motionssalt.saltygraphics.uikit.sublibs.settings.SaltySettingsDeviceInfoItem
import com.motionssalt.saltygraphics.uikit.sublibs.settings.SaltySettingsScreen
import com.motionssalt.saltygraphics.uikit.sublibs.settings.SaltySettingsSocialLink

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    deviceInfoProvider: DeviceInfoProvider,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val theme by settingsManager.theme.collectAsState()
    val useSystemDefault by settingsManager.useSystemDefaultScale.collectAsState()
    val uiScale by settingsManager.uiScale.collectAsState()

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

    val socialLinks = remember(context) {
        listOf(
            SaltySettingsSocialLink("WhatsApp (Direct Contact)", SocialConfig.WHATSAPP_PHONE, Icons.Default.Chat) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.WHATSAPP_URL)))
            },
            SaltySettingsSocialLink("Instagram", SocialConfig.INSTAGRAM_HANDLE, Icons.Default.CameraAlt) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.INSTAGRAM_URL)))
            },
            SaltySettingsSocialLink("Threads", SocialConfig.THREADS_HANDLE, Icons.Default.AlternateEmail) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.THREADS_URL)))
            },
            SaltySettingsSocialLink("X / Twitter", SocialConfig.X_HANDLE, Icons.Default.Tag) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.X_URL)))
            },
            SaltySettingsSocialLink("YouTube", SocialConfig.YOUTUBE_HANDLE, Icons.Default.PlayCircle) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.YOUTUBE_URL)))
            },
            SaltySettingsSocialLink("TikTok", SocialConfig.TIKTOK_HANDLE, Icons.Default.Movie) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.TIKTOK_URL)))
            }
        )
    }

    val deviceItems = remember(deviceInfo) {
        listOf(
            SaltySettingsDeviceInfoItem("Device Model", "${deviceInfo.manufacturer} ${deviceInfo.model}"),
            SaltySettingsDeviceInfoItem("Android Version", "${deviceInfo.androidVersion} (API ${deviceInfo.sdkInt})"),
            SaltySettingsDeviceInfoItem("Supported ABIs", deviceInfo.supportedAbis),
            SaltySettingsDeviceInfoItem("Screen Resolution", deviceInfo.screenSize),
            SaltySettingsDeviceInfoItem("Memory (RAM)", "${deviceInfo.availableRamMB} MB free / ${deviceInfo.totalRamMB} MB total"),
            SaltySettingsDeviceInfoItem("GPU Renderer", deviceInfo.gpuRenderer),
            SaltySettingsDeviceInfoItem("Max Texture Limit", "${deviceInfo.maxTextureSize} x ${deviceInfo.maxTextureSize} px")
        )
    }

    SaltySettingsScreen(
        themeSelected = theme,
        themeOptions = ThemeChoice.entries.map { DropdownOption(it, it.displayName) },
        onThemeSelect = { settingsManager.setTheme(it) },
        versionText = "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) | Developer: ${SocialConfig.DEVELOPER_NAME} (${SocialConfig.BRAND_NAME})",
        onVersionTap = {
            versionTapCount++
            if (versionTapCount >= 7 && !devOptionsEnabled) {
                devOptionsEnabled = true
                context.getSharedPreferences("salty_dev_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("dev_options", true)
                    .apply()
                Toast.makeText(context, "Developer options unlocked!", Toast.LENGTH_SHORT).show()
            }
        },
        savedFolderName = currentSavedFolderName,
        onChangeSaveFolder = { folderPickerLauncher.launch(null) },
        devOptionsEnabled = devOptionsEnabled,
        debugEffectsEnabled = debugEffectsEnabled,
        onDebugEffectsChange = { enabled ->
            debugEffectsEnabled = enabled
            context.getSharedPreferences("salty_dev_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("debug_effects", enabled)
                .apply()
        },
        useSystemDefaultScale = useSystemDefault,
        onUseSystemDefaultScaleChange = { settingsManager.setUseSystemDefault(it) },
        uiScale = uiScale,
        onUiScaleChange = { settingsManager.setUiScale(it) },
        issueText = issueText,
        onIssueTextChange = { issueText = it },
        onShareReport = {
            val shareIntent = deviceInfoProvider.composeProblemReport(issueText)
            val chooser = Intent.createChooser(shareIntent, "Send Problem Report via")
            context.startActivity(chooser)
        },
        socialLinks = socialLinks,
        deviceInfoItems = deviceItems,
        onNavigateBack = onNavigateBack
    )
}
