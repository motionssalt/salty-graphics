import com.motionssalt.saltygraphics.data.StorageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.motionssalt.saltygraphics.ui.controls.SaltySwitch
package com.motionssalt.saltygraphics.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.config.SocialConfig
import com.motionssalt.saltygraphics.data.DeviceInfoProvider
import com.motionssalt.saltygraphics.data.SettingsManager
import com.motionssalt.saltygraphics.data.ThemeChoice
import com.motionssalt.saltygraphics.ui.controls.DropdownControl
import com.motionssalt.saltygraphics.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    deviceInfoProvider: DeviceInfoProvider,
    onNavigateBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val theme by settingsManager.theme.collectAsState()
    val uiScale by settingsManager.uiScale.collectAsState()
    val useSystemDefault by settingsManager.useSystemDefault.collectAsState()

    var issueText by remember { mutableStateOf("") }
    var versionTapCount by remember { mutableIntStateOf(0) }
    var devOptionsEnabled by remember {
        mutableStateOf(context.getSharedPreferences("salty_dev_prefs", android.content.Context.MODE_PRIVATE).getBoolean("dev_options", false))
    }
    var debugEffectsEnabled by remember {
        mutableStateOf(context.getSharedPreferences("salty_dev_prefs", android.content.Context.MODE_PRIVATE).getBoolean("debug_effects", false))
    }
    var currentSavedFolderName by remember { mutableStateOf(StorageManager.getSavedFolderName(context)) }
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: android.net.Uri? ->
        if (treeUri != null) {
            StorageManager.setSavedFolder(context, treeUri)
            currentSavedFolderName = StorageManager.getSavedFolderName(context)
            Toast.makeText(context, "Save folder updated", Toast.LENGTH_SHORT).show()
        }
    }
    val deviceInfo = remember { deviceInfoProvider.getDeviceInfo() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSectionCard(title = "Appearance") {
                DropdownControl("Theme", ThemeChoice.entries.map { it.displayName }, theme.displayName,
                    { label -> settingsManager.setTheme(ThemeChoice.entries.first { it.displayName == label }) })
            }
            // About Section
            SettingsSectionCard(title = "About Salty Graphics") {
                Text(
                    text = "Salty Graphics is a hybrid raster and vector graphic design application built by Victor (solo developer and motion designer, brand MOTIONSALT). Designed for mobile creators with an offline-first GPU render engine and branching action tree history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version: ${com.motionssalt.saltygraphics.BuildConfig.VERSION_NAME} (${com.motionssalt.saltygraphics.BuildConfig.VERSION_CODE}) | Developer: ${SocialConfig.DEVELOPER_NAME} (${SocialConfig.BRAND_NAME})",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    modifier = Modifier.clickable {
                        versionTapCount++
                        if (versionTapCount >= 7 && !devOptionsEnabled) {
                            devOptionsEnabled = true
                            context.getSharedPreferences("salty_dev_prefs", android.content.Context.MODE_PRIVATE).edit()
                                .putBoolean("dev_options", true).apply()
                            Toast.makeText(context, "Developer options unlocked!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // S16: Save Location setting
            SettingsSectionCard(title = "Save Location") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSavedFolderName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Default directory for direct exports and project files.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { folderPickerLauncher.launch(null) }) {
                        Text("Change", color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // S11: Developer Options
            if (devOptionsEnabled) {
                SettingsSectionCard(title = "Developer Options") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Enable Debug Test Effects",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurface
                            )
                            Text(
                                "Exposes Invert, Brightness, and Tint test effects in the effects engine to verify adjustment layer compositing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        SaltySwitch(
                            checked = debugEffectsEnabled,
                            onCheckedChange = { enabled ->
                                debugEffectsEnabled = enabled
                                context.getSharedPreferences("salty_dev_prefs", android.content.Context.MODE_PRIVATE).edit()
                                    .putBoolean("debug_effects", enabled).apply()
                            }
                        )
                    }
                }
            }
            // UI Scale Slider
            SettingsSectionCard(title = "Display & UI Scaling") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Use system default scale",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurface
                        )
                        Text(
                            if (useSystemDefault) "Currently using system display scale" else "Custom scale: ${(uiScale * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useSystemDefault,
                        onCheckedChange = { settingsManager.setUseSystemDefault(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primary,
                            checkedTrackColor = colors.surfaceVariant
                        )
                    )
                }

                if (!useSystemDefault) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "UI Scale: ${(uiScale * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface
                    )
                    Slider(
                        value = uiScale,
                        onValueChange = { settingsManager.setUiScale(it) },
                        valueRange = 0.75f..1.50f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.surfaceVariant
                        )
                    )
                }
            }

            // Report a Problem / Issues
            SettingsSectionCard(title = "Report a Problem") {
                Text(
                    "Describe any issue, unexpected behavior, or suggestion. Submitting generates a diagnostic report shared via the Android share sheet (works 100% offline with zero backend).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = issueText,
                    onValueChange = { issueText = it },
                    placeholder = { Text("Write down issues or feedback here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val shareIntent = deviceInfoProvider.composeProblemReport(issueText)
                        val chooser = Intent.createChooser(shareIntent, "Send Problem Report via")
                        context.startActivity(chooser)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.background
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Diagnostic Report", fontWeight = FontWeight.Bold)
                }
            }

            // Social & Contact Links (Section 3.3)
            SettingsSectionCard(title = "Connect & Support") {
                SocialLinkItem(
                    label = "WhatsApp (Direct Contact)",
                    detail = SocialConfig.WHATSAPP_PHONE,
                    icon = Icons.Default.Chat,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.WHATSAPP_URL))
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.5f))
                SocialLinkItem(
                    label = "Instagram",
                    detail = SocialConfig.INSTAGRAM_HANDLE,
                    icon = Icons.Default.CameraAlt,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.INSTAGRAM_URL))
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.5f))
                SocialLinkItem(
                    label = "Threads",
                    detail = SocialConfig.THREADS_HANDLE,
                    icon = Icons.Default.AlternateEmail,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.THREADS_URL))
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.5f))
                SocialLinkItem(
                    label = "X / Twitter",
                    detail = SocialConfig.X_HANDLE,
                    icon = Icons.Default.Tag,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.X_URL))
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.5f))
                SocialLinkItem(
                    label = "YouTube",
                    detail = SocialConfig.YOUTUBE_HANDLE,
                    icon = Icons.Default.PlayCircle,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.YOUTUBE_URL))
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.5f))
                SocialLinkItem(
                    label = "TikTok",
                    detail = SocialConfig.TIKTOK_HANDLE,
                    icon = Icons.Default.Movie,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SocialConfig.TIKTOK_URL))
                        context.startActivity(intent)
                    }
                )
            }

            // Device Diagnostics Section
            SettingsSectionCard(title = "Device Info") {
                DeviceInfoRow("Device Model", "${deviceInfo.manufacturer} ${deviceInfo.model}")
                DeviceInfoRow("Android Version", "${deviceInfo.androidVersion} (API ${deviceInfo.sdkInt})")
                DeviceInfoRow("Supported ABIs", deviceInfo.supportedAbis)
                DeviceInfoRow("Screen Resolution", deviceInfo.screenSize)
                DeviceInfoRow("Memory (RAM)", "${deviceInfo.availableRamMB} MB free / ${deviceInfo.totalRamMB} MB total")
                DeviceInfoRow("GPU Renderer", deviceInfo.gpuRenderer)
                DeviceInfoRow("Max Texture Limit", "${deviceInfo.maxTextureSize} x ${deviceInfo.maxTextureSize} px")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SocialLinkItem(
    label: String,
    detail: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        }
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.primary)
    }
}

@Composable
fun DeviceInfoRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, fontWeight = FontWeight.Medium)
    }
}
