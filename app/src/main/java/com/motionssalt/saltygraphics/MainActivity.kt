package com.motionssalt.saltygraphics
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.motionssalt.saltygraphics.data.DeviceInfoProvider
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.data.SettingsManager
import com.motionssalt.saltygraphics.uikit.SaltyGraphicsApp
import java.io.File
/** Android lifecycle and dependency integration only. All UI lives in :ui-kit. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                Log.e("SaltyGraphics", "Uncaught exception", throwable)
                File(applicationContext.filesDir,"last_crash.txt").writeText(buildString {
                    appendLine("=== SALTY GRAPHICS CRASH LOG ===")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Exception: ${throwable.javaClass.name}: ${throwable.message}")
                    appendLine(throwable.stackTraceToString())
                })
            }
            previous?.uncaughtException(thread,throwable)
        }
        runCatching { enableEdgeToEdge() }
        val developer = applicationContext.getSharedPreferences("salty_dev_prefs",MODE_PRIVATE).getBoolean("debug_effects",false)
        setContent { SaltyGraphicsApp(ProjectRepository(applicationContext),SettingsManager(applicationContext),
            DeviceInfoProvider(applicationContext),SaltyFeatureRegistrations.registry(developer)) }
    }
}
