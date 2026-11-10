package com.motionssalt.saltygraphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.file.ProjectLoadState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.min

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GradientPanelGestureTest : FillEditorFixture() {
    @Test fun centerHandleChangesRealFillInOneHistoryActionWithThePanelOpen() {
        val layer=Layer(id="gradient",name="Gradient target",type=LayerType.SHAPE,
            shapeData=ShapeData(width=400f,height=400f),fillData=FillData(type=FillType.GRADIENT))
        withEditorFixture(listOf(layer)) { id,repo ->
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText(layer.name).performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()
            compose.onNodeWithText("Fill").performClick()
            compose.onNodeWithText("Edit Gradient").performClick()
            compose.onNodeWithText("Gradient Settings").assertIsDisplayed()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            val node=compose.onNodeWithTag("gradient_handles")
            val rect=node.getUnclippedBoundsInRoot()
            val fit=min(rect.width.value/1000f,rect.height.value/1000f)*.94f
            node.performTouchInput {
                down(center)
                moveTo(center+Offset(40f*fit,20f*fit),100)
                up()
            }
            compose.waitUntil(10000) {runBlocking {repo.getProject(id)?.findLayerById(layer.id)?.effectiveFill?.gradient?.centerX?.let{it>.55f}==true}}
            val loaded=runBlocking {repo.openProject(id)} as ProjectLoadState.Loaded
            val changed=loaded.result.document.findLayerById(layer.id)!!
            assertEquals(.6f,changed.effectiveFill.gradient.centerX,.015f)
            assertEquals(.55f,changed.effectiveFill.gradient.centerY,.015f)
            assertEquals("A gesture commits one action",2,loaded.result.historySnapshot!!.nodes.size)
            assertEquals(layer.transform,changed.transform)
            compose.onNodeWithTag("tools").assertIsDisplayed()
            compose.onNodeWithTag("preview").assertIsDisplayed()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
        }
    }
}
