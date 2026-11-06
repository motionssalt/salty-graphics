package com.motionssalt.saltygraphics

import android.graphics.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.ui.editor.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VectorEditorTest {
    @get:Rule val compose=createEmptyComposeRule()

    @Test fun touchAddMoveSmoothRemoveAndMultiplePathsAreUndoableAndPersist() {
        val host=Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val initial=Layer(type=LayerType.VECTOR,vectorPathData=VectorPathData())
        val tree=ActionTree(ProjectDocument(layers=listOf(initial)))
        var layer by mutableStateOf(initial)
        var tool by mutableStateOf(VectorTool.ADD)
        var selected by mutableStateOf<Int?>(null)
        var pathIndex by mutableIntStateOf(0)
        fun commit(description:String,changed:Layer) {
            layer=changed;tree.recordAction(description,tree.currentDocument.updateLayer(initial.id){changed})
        }
        host.get().setContent { MaterialTheme {
            Column(Modifier.requiredSize(412.dp,820.dp)) {
                Box(Modifier.height(400.dp).fillMaxWidth()) {
                    VectorCanvasOverlay(layer,CanvasSize(1000,1000),pathIndex,selected,tool,1f,Offset.Zero,
                        {selected=it},{},{commit("Gesture",it)},{})
                }
                Box(Modifier.weight(1f)) {
                    VectorPage(layer,pathIndex,selected,tool,{pathIndex=it},{selected=it},{tool=it},::commit,{})
                }
            }
        } }
        try {
            val handles=compose.onNodeWithTag("vector_handles")
            handles.performTouchInput { click(Offset(140f,170f));click(Offset(260f,220f)) }
            compose.runOnIdle{assertEquals(2,layer.editablePaths[0].points.size);assertEquals(3,tree.nodes.size);tool=VectorTool.SELECT}
            val xBefore=layer.editablePaths[0].points[0].x
            handles.performTouchInput { down(Offset(140f,170f));moveTo(Offset(170f,190f),200);up() }
            compose.runOnIdle{assertTrue(layer.editablePaths[0].points[0].x>xBefore);assertEquals(4,tree.nodes.size)}
            compose.onNodeWithText("Corner/Smooth").performClick()
            handles.performTouchInput{click(Offset(170f,190f))}
            compose.runOnIdle{assertTrue(layer.editablePaths[0].points[0].hasOutHandle)}
            compose.onNodeWithText("Close path").performScrollTo().performClick()
            compose.runOnIdle{assertTrue(layer.editablePaths[0].isClosed)}
            compose.onNodeWithText("Open path").performClick()
            compose.runOnIdle{assertFalse(layer.editablePaths[0].isClosed)}
            compose.onNodeWithText("+ Add").performScrollTo().performClick()
            compose.runOnIdle{assertEquals(2,layer.editablePaths.size);assertEquals(1,pathIndex)}
            compose.onNodeWithText("Add point").performScrollTo().performClick()
            handles.performTouchInput{click(Offset(250f,250f))}
            compose.onNodeWithText("Remove point").performClick()
            handles.performTouchInput{click(Offset(250f,250f))}
            compose.runOnIdle{
                assertTrue(layer.editablePaths[1].points.isEmpty())
                assertTrue(tree.undo());layer=tree.currentDocument.layers.single()
                assertEquals(1,layer.editablePaths[1].points.size)
                val bytes=ByteArrayOutputStream();SgProjectArchive.exportToStream(tree.currentDocument,bytes)
                assertEquals(tree.currentDocument,SgProjectArchive.importFromStream(bytes.toByteArray().inputStream()).first)
            }
        } finally{host.pause().stop().destroy()}
    }

    @Test fun svgBasicShapesCurvesMultipleContoursAndUnsafeInputs() {
        val svg="""<svg xmlns="http://www.w3.org/2000/svg"><g transform="translate(10 20)" fill="#ff0000"><rect width="100" height="60"/><circle cx="150" cy="30" r="20"/><path d="M 0 100 C 20 40 70 150 100 100 M 120 100 L 160 100" fill="none" stroke="#00ff00"/></g></svg>"""
        val paths=SvgVectorImporter.parse(svg.byteInputStream())
        assertEquals(4,paths.size);assertTrue(paths[0].isClosed);assertFalse(paths.last().isClosed)
        assertEquals(0xFFFF0000L,paths[0].fillColor)
        assertEquals(0xFF00FF00L,paths.last().strokeColor)
        assertTrue(paths[2].points.size>2)
        for (bad in listOf("<svg/>","<!DOCTYPE svg><svg/>","<svg><image href='https://example.org'/></svg>")) {
            assertTrue(runCatching{SvgVectorImporter.parse(bad.byteInputStream())}.isFailure)
        }
    }

    @Test fun vectorStrokeAndFillRenderAndPathCanCarryText() {
        val data=ShapeData(type=ShapeType.RECTANGLE,width=100f,height=100f).toVectorPath()
            .copy(hasFill=true,fillColor=0xFFFF0000L,hasStroke=true,strokeColor=0xFF00FF00L,strokeWidth=8f,strokeCap=VectorCap.ROUND,strokeJoin=VectorJoin.ROUND)
        val bitmap=Bitmap.createBitmap(200,200,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap);canvas.translate(100f,100f)
        drawNativeVector(canvas,data)
        assertEquals(Color.RED,bitmap.getPixel(100,100));assertEquals(Color.GREEN,bitmap.getPixel(50,100))
        val measure=PathMeasure(nativeVectorPath(data),false)
        assertTrue(measure.length>300f)
        canvas.drawTextOnPath("Salty",nativeVectorPath(data),0f,0f,Paint().apply{textSize=16f;color=Color.BLUE})
        bitmap.recycle()
    }
}
