package com.motionssalt.saltygraphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.engine.render.PreviewQuality
import com.motionssalt.saltygraphics.ui.editor.ProjectSettingsFeature
import com.motionssalt.saltygraphics.ui.editor.ProjectSettingsPage
import com.motionssalt.saltygraphics.uikit.registry.change
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectSettingsGoldenTest {
    @get:Rule val compose = createEmptyComposeRule()

    /** These are screenshots of shared scaffold/library components, not app/device screenshot loops. */
    @Test fun settingsLibraryPageHasReviewedLightAndDarkGoldens() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var dark by mutableStateOf(true)
        val fixture = ProjectDocument(id = "scaffold-fixture", name = "Scaffold fixture", createdAt = 0, updatedAt = 0)
        host.get().setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.requiredSize(412.dp, 450.dp)) { ProjectSettingsPage(fixture, {}, {}) }
            }
        }
        val failures = mutableListOf<String>()
        try {
            for (isDark in listOf(true, false)) {
                compose.runOnIdle { dark = isDark }
                compose.mainClock.advanceTimeBy(1000); compose.waitForIdle()
                val node = compose.onNodeWithTag("scaffold_feature_page").fetchSemanticsNode()
                val actual = Bitmap.createBitmap(node.size.width, node.size.height, Bitmap.Config.ARGB_8888)
                compose.runOnIdle {
                    val canvas = android.graphics.Canvas(actual)
                    canvas.translate(-node.boundsInWindow.left, -node.boundsInWindow.top)
                    host.get().window.decorView.draw(canvas)
                }
                val pixels = IntArray(actual.width * actual.height)
                actual.getPixels(pixels, 0, actual.width, 0, 0, actual.width, actual.height)
                assertTrue("Golden must contain real rendered controls", pixels.distinct().size > 20)
                val name = "settings_${if (isDark) "dark" else "light"}.png"
                File("build/outputs/scaffold-screenshots", name).apply { parentFile.mkdirs() }
                    .outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val expected = File("src/test/goldens/scaffold", name)
                if (!expected.isFile) { failures += "Missing reviewed golden: $name"; continue }
                val baseline = BitmapFactory.decodeFile(expected.absolutePath)
                assertEquals(baseline.width, actual.width); assertEquals(baseline.height, actual.height)
                assertTrue("Changed reviewed golden: $name", baseline.sameAs(actual))
            }
            assertTrue(failures.joinToString("\n"), failures.isEmpty())
        } finally { host.pause().stop().destroy() }
    }

}
