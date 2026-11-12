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
class ProjectSettingsVisibilityTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun visibilityAndInlineQualityReflectTheSameDocument() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val tree = ActionTree(ProjectDocument())
        var shown by mutableStateOf(tree.currentDocument)
        host.get().setContent { MaterialTheme { Box(Modifier.requiredSize(412.dp, 450.dp)) {
            ProjectSettingsPage(shown, onCommit = { c -> tree.recordCommand(c.identity(tree.currentDocument), c::apply); shown = tree.currentDocument }, onBack = {})
        } } }
        try {
            compose.onNodeWithTag("control_snap_objects").performScrollTo()
            compose.onNode(isToggleable() and hasAnyAncestor(hasTestTag("control_snap_objects"))).performClick()
            compose.onNodeWithTag("control_snap_tolerance").assertDoesNotExist()
            compose.onNodeWithTag("control_preview_quality").performScrollTo()
            compose.onNodeWithText("Full").performClick()
            compose.onNodeWithText("1/4").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(PreviewQuality.QUARTER, tree.currentDocument.editorSettings.defaultPreviewQuality) }
            compose.onNodeWithTag("readout_preview_dimensions").performScrollTo()
                .assertTextContains("1/4", substring = true)
            compose.onAllNodes(isDialog()).assertCountEquals(0)
        } finally { host.pause().stop().destroy() }
    }

}
