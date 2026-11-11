package com.motionssalt.saltygraphics

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
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.ui.editor.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SelectionHandleRegressionTest {
    @get:Rule val compose=createEmptyComposeRule()

    @Test fun everyHandleUsesFreshCallbacksAndRecordsExactlyOneActionAfterLayerCreation() {
        val host=Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var doc by mutableStateOf(ProjectDocument(canvasSize=CanvasSize(1000,1000)))
        var selected by mutableStateOf(emptySet<String>())
        val tree=ActionTree(doc)
        host.get().setContent { MaterialTheme {
            // Deliberately capture a composition-local snapshot, as EditorScreen originally did.
            // With stale pointerInput callbacks this restores the initially empty document.
            val snapshot=doc
            Box(Modifier.requiredSize(412.dp,400.dp)) {
                CanvasView(doc,selected,1f,Offset.Zero,false,20,{_,_->},{ selected=it?.let{setOf(it)} ?: emptySet() },
                    { id,t -> doc=SelectionGeometry.apply(snapshot,id,t) },
                    { id,t -> doc=SelectionGeometry.apply(snapshot,id,t);tree.recordAction("Transform",doc) })
            }
        } }
        try {
            compose.waitForIdle() // Install handler while document still has no layers.
            val shape=Layer(id="new",shapeData=ShapeData(width=500f,height=400f))
            val neighbor=Layer(id="neighbor",shapeData=ShapeData(width=50f,height=50f),transform=Transform2D(translationX=-400f))
            compose.runOnIdle {doc=doc.copy(layers=listOf(shape,neighbor));selected=setOf(shape.id)}
            val canvas=compose.onNodeWithTag("editor_canvas")
            for(h in SelectionHandle.entries.filter {it!=SelectionHandle.NONE && it!=SelectionHandle.BODY}) {
                compose.runOnIdle { doc=doc.updateLayer(shape.id){it.copy(transform=Transform2D(rotationDegrees=27f))} }
                val dimensions=canvas.fetchSemanticsNode().size
                val fit=min(dimensions.width/1000f,dimensions.height/1000f)*.94f
                val center=Offset(dimensions.width/2f,dimensions.height/2f)
                fun screen(local:SelectionPoint):Offset {
                    val p=local.rotated(27f);return center+Offset(p.x*fit,p.y*fit)
                }
                val start=if(h==SelectionHandle.ROTATE) screen(SelectionPoint(0f,-200f-32f/fit))
                    else screen(SelectionPoint(h.x*250f,h.y*200f))
                val end=if(h==SelectionHandle.ROTATE) {
                    val p=SelectionPoint(start.x-center.x,start.y-center.y).rotated(40f)
                    center+Offset(p.x,p.y)
                } else start+Offset(if(h.x==0) 0f else h.x*35f,if(h.y==0) 0f else h.y*35f)
                val beforeNodes=tree.nodes.size
                canvas.performTouchInput {down(start);moveTo(end,200);up()}
                compose.runOnIdle {
                    assertEquals("Layer loss on $h",listOf("new","neighbor"),doc.layers.map{it.id})
                    val t=doc.layers.first().transform
                    assertTrue(SelectionGeometry.valid(t))
                    assertNotEquals("No transform on $h",Transform2D(rotationDegrees=27f),t)
                    assertEquals("History on $h",beforeNodes+1,tree.nodes.size)
                }
            }
            // Layer taps do not commit transform actions.
            val before=tree.nodes.size
            canvas.performTouchInput {click(center)}
            compose.runOnIdle {assertEquals(before,tree.nodes.size)}
        } finally {host.pause().stop().destroy()}
    }

    @Test fun secondPointerAndCancellationRestorePreviewWithoutHistoryOrLayerLoss() {
        val host=Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val layer=Layer(id="shape",shapeData=ShapeData(width=400f,height=400f))
        val initial=ProjectDocument(canvasSize=CanvasSize(1000,1000),layers=listOf(layer))
        var doc by mutableStateOf(initial)
        var commits=0
        host.get().setContent {MaterialTheme {Box(Modifier.requiredSize(412.dp,400.dp)) {
            CanvasView(doc,setOf(layer.id),1f,Offset.Zero,false,20,{_,_->},{},
                {id,t->doc=SelectionGeometry.apply(doc,id,t)},{_,_->commits++})
        }}}
        try {
            val canvas=compose.onNodeWithTag("editor_canvas")
            canvas.performTouchInput {
                down(0,Offset(281f,200f));moveTo(0,Offset(315f,200f),100)
                down(1,Offset(120f,150f));moveTo(1,Offset(100f,130f),100);up(1);up(0)
            }
            compose.runOnIdle {assertEquals(0,commits);assertEquals(initial.layers,doc.layers)}
            canvas.performTouchInput {down(Offset(281f,200f));moveTo(Offset(310f,200f),100);cancel()}
            compose.runOnIdle {assertEquals(0,commits);assertEquals(initial.layers,doc.layers)}
        } finally {host.pause().stop().destroy()}
    }

    @Test fun measuredTextGroupAndCombinedBoundsMatchVisibleGeometry() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val text=Layer(type=LayerType.TEXT,textData=TextLayerData(text="WWW iii",fontSize=80f,justification=TextJustification.LEFT))
        val child=Layer(type=LayerType.SHAPE,shapeData=ShapeData(width=100f,height=40f),transform=Transform2D(translationX=300f,rotationDegrees=90f))
        val group=Layer(type=LayerType.GROUP,children=listOf(child))
        val doc=ProjectDocument(layers=listOf(text,group))
        val b=layerSelectionBounds(context,doc,text)
        assertEquals(selectionTextPaint(context,text.textData!!).measureText("WWW iii"),b.width,.01f)
        assertEquals(-200f,b.left,.01f)
        val g=layerSelectionBounds(context,doc,group)
        assertEquals(280f,g.left,.01f);assertEquals(320f,g.right,.01f)
        assertEquals(-50f,g.top,.01f);assertEquals(50f,g.bottom,.01f)
        val proxy=selectionProxy(context,doc,setOf(text.id,group.id))!!
        val combined=selectionBounds(context,doc,proxy)
        assertEquals(520f,combined.width,.01f)
        assertTrue(selectionProxy(context,doc.copy(layers=listOf(text.copy(isLocked=true),group)),setOf(text.id,group.id))!!.isLocked)
    }
}
