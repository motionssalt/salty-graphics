package com.motionssalt.saltygraphics

import android.graphics.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.ui.editor.EditorScreen
import com.motionssalt.saltygraphics.ui.export.drawExportBackground
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectSettingsTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun editorGearOpensProjectSettingsNotAppSettings() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            val repo = ProjectRepository(RuntimeEnvironment.getApplication())
            val doc = runBlocking { repo.createProject("Settings fixture", CanvasSize()) }
            var openedAppSettings = false
            host.get().setContent {
                MaterialTheme { EditorScreen(doc.id, repository = repo, onNavigateBack = {},
                    onNavigateToExport = {}, onNavigateToSettings = { openedAppSettings = true }) }
            }
            compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Settings").assertIsDisplayed().performClick()
            compose.onNodeWithText("Project settings").assertIsDisplayed()
            compose.onNodeWithText("Canvas background").performScrollTo().assertIsDisplayed()
            assertFalse(openedAppSettings)
        } finally { host.pause().stop().destroy() }
    }

    @Test fun backgroundAndPreferencesPersistAndUndo() {
        val original = ProjectDocument()
        for (argb in listOf(0L, 0xFF274ABC, 0x804080C0)) {
            val changed = original.copy(editorSettings = EditorSettings(backgroundColor = argb, showGrid = true, gridSize = 48, snapEnabled = true))
            val tree = ActionTree(original)
            tree.recordAction("Canvas background", changed)
            val bytes = ByteArrayOutputStream().also { SgProjectArchive.exportToStream(changed, it) }.toByteArray()
            val loaded = SgProjectArchive.importFromStream(ByteArrayInputStream(bytes)).first
            assertEquals(changed, loaded)
            assertTrue(tree.undo())
            assertEquals(original, tree.currentDocument)
        }
    }

    @Test fun pngAndWebpKeepRealTransparencyAndJpegFlattens() {
        for (format in listOf(Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.WEBP_LOSSLESS)) {
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            drawExportBackground(Canvas(bitmap), 0L, false)
            val bytes = ByteArrayOutputStream()
            assertTrue(bitmap.compress(format, 100, bytes))
            val restored = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size())
            assertEquals(0, Color.alpha(restored.getPixel(3, 3)))
            drawExportBackground(Canvas(bitmap), 0xFF274ABCL, false)
            assertEquals(0xFF274ABC.toInt(), bitmap.getPixel(3, 3))
            drawExportBackground(Canvas(bitmap), 0L, true)
            assertEquals(Color.WHITE, bitmap.getPixel(3, 3))
            bitmap.recycle(); restored.recycle()
        }
    }
}
