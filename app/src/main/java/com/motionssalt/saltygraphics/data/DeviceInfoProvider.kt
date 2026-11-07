package com.motionssalt.saltygraphics.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.opengl.GLES30
import android.os.Build
import com.motionssalt.saltygraphics.config.SocialConfig

data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val supportedAbis: String,
    val screenSize: String,
    val totalRamMB: Long,
    val availableRamMB: Long,
    val gpuRenderer: String,
    val maxTextureSize: Int
)

class DeviceInfoProvider(private val context: Context) {
    fun getDeviceInfo(maxTextureSize: Int = 4096): DeviceInfo {
        val dm = context.resources.displayMetrics
        val actMan = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actMan?.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem / (1024 * 1024)
        val availRam = memInfo.availMem / (1024 * 1024)

        return DeviceInfo(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS.joinToString(", "),
            screenSize = "${dm.widthPixels} x ${dm.heightPixels} (${dm.densityDpi} dpi)",
            totalRamMB = totalRam,
            availableRamMB = availRam,
            gpuRenderer = "OpenGL ES 3.x",
            maxTextureSize = maxTextureSize
        )
    }

    fun composeProblemReport(writtenIssue: String, maxTextureSize: Int = 4096): Intent {
        val info = getDeviceInfo(maxTextureSize)
        val crashFile = java.io.File(context.filesDir, "last_crash.txt")
        val crashLogContent = if (crashFile.exists()) {
            try { crashFile.readText().takeLast(16000) } catch (_: Throwable) { null }
        } else null

        val reportBody = buildString {
            appendLine("=== SALTY GRAPHICS PROBLEM REPORT ===")
            appendLine()
            appendLine("Issue Description:")
            appendLine(writtenIssue.ifBlank { "No description entered." })
            appendLine()
            appendLine("--- Device Diagnostics ---")
            appendLine("Device: ${info.manufacturer} ${info.model}")
            appendLine("Android: ${info.androidVersion} (API ${info.sdkInt})")
            appendLine("Supported ABIs: ${info.supportedAbis}")
            appendLine("Screen: ${info.screenSize}")
            appendLine("RAM: ${info.availableRamMB} MB free / ${info.totalRamMB} MB total")
            appendLine("GPU Renderer: ${info.gpuRenderer}")
            appendLine("Max Texture Size: ${info.maxTextureSize}px")
            appendLine("App Version: ${com.motionssalt.saltygraphics.BuildConfig.VERSION_NAME}")
            if (!crashLogContent.isNullOrBlank()) {
                appendLine()
                appendLine("--- Recent Crash Log ---")
                appendLine(crashLogContent)
            }
            appendLine()
            appendLine("Submitted to: ${SocialConfig.DEVELOPER_NAME} (${SocialConfig.BRAND_NAME})")
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Salty Graphics Bug Report - ${info.model}")
            putExtra(Intent.EXTRA_TEXT, reportBody)
        }
    }
}
