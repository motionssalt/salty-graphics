package com.motionssalt.saltygraphics

import android.graphics.Color
import androidx.compose.ui.test.*
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FillPanelAlphaRegressionTest : FillEditorFixture() {
    @Test fun realFillColorPagePreservesHalfAlphaAndVisibleStrokeWithoutWindows() {
        val layer=Layer(id="alpha",name="Alpha target",type=LayerType.SHAPE,
            shapeData=ShapeData(width=80f,height=80f),
            strokeData=StrokeData(isEnabled=true,width=8f,color=0xFFFF0000L))
        withEditorFixture(listOf(layer)) { id,repo ->
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText(layer.name).performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()
            compose.onNodeWithText("Fill").performClick()
            compose.onNodeWithText("Color and alpha").performClick()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            compose.onNodeWithTag("color_hex").performScrollTo().performTextReplacement("8000FF00")
            compose.onNodeWithText("Set hex").performScrollTo().performClick()
            compose.waitUntil(10000) { runBlocking {repo.getProject(id)?.findLayerById(layer.id)?.effectiveFill?.effectiveSolidArgb==0x8000FF00L} }
            val saved=runBlocking {repo.getProject(id)!!}.findLayerById(layer.id)!!
            val bitmap=LayerCompositor.renderLayerContent(saved,200,200)
            assertEquals("Alpha must not be multiplied twice",128,Color.alpha(bitmap.getPixel(100,100)))
            assertTrue(Color.red(bitmap.getPixel(140,100))>200)
            compose.onNodeWithText("Transparent").performScrollTo().performClick()
            compose.waitUntil(10000) {runBlocking {repo.getProject(id)?.findLayerById(layer.id)?.effectiveFill?.effectiveSolidArgb?.ushr(24)==0L}}
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            compose.onNodeWithTag("preview").assertIsDisplayed()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FillPanelMediaRegressionTest : FillEditorFixture() {
    @Test fun projectImagePickerReplacesFillNotLayerAndRemainsInsideTools() {
        val layer=Layer(id="mediafill",name="Media fill target",type=LayerType.SHAPE,shapeData=ShapeData())
        withEditorFixture(listOf(layer)) { id,repo ->
            compose.onNodeWithContentDescription("Layers").performClick()
            compose.onNodeWithText(layer.name).performClick()
            compose.onNodeWithContentDescription("Close Layers").performClick()
            compose.onNodeWithText("Fill").performClick()
            compose.onNodeWithText("Media Fill").performScrollTo().performClick()
            compose.onNodeWithText("Choose Media Image").performScrollTo().performClick()
            compose.onNodeWithText("Import from device").assertExists()
            compose.onNodeWithTag("fill_media_assets/test.png").performScrollTo().performClick()
            compose.waitUntil(10000) {runBlocking {repo.getProject(id)?.findLayerById(layer.id)?.effectiveFill?.mediaAssetPath=="assets/test.png"}}
            val saved=runBlocking {repo.getProject(id)!!}
            assertEquals(listOf(layer.id),saved.layers.map{it.id})
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            compose.onNodeWithTag("preview").assertIsDisplayed()
        }
    }
}
