package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayOutputStream

class VectorEditingTest {
    @Test fun splitPreservesCubicAndClosure() {
        for (closed in listOf(false,true)) {
            val path = VectorPathData(points = listOf(BezierPoint(0f,0f,outHandleX=80f,outHandleY=100f),
                BezierPoint(200f,0f,inHandleX=120f,inHandleY=100f)), isClosed=closed)
            val (changed,index) = VectorEditing.split(path,0,.5f)
            assertEquals(1,index); assertEquals(closed,changed.isClosed)
            for (step in 0..100) {
                val t=step/100f
                val original=VectorEditing.pointAt(path.points[0],path.points[1],t)
                val split=if(t<=.5f) VectorEditing.pointAt(changed.points[0],changed.points[1],t*2)
                    else VectorEditing.pointAt(changed.points[1],changed.points[2],(t-.5f)*2)
                assertEquals(original.first,split.first,.001f); assertEquals(original.second,split.second,.001f)
            }
        }
    }
    @Test fun addMoveSmoothRemoveMultiplePathsRoundTripAndUndo() {
        val path=VectorPathData(points=listOf(BezierPoint(-100f,0f),BezierPoint(100f,0f)))
        val (added,index)=VectorEditing.addPoint(path,0f,1f,20f)
        assertEquals(1,index);assertEquals(3,added.points.size)
        val smooth=VectorEditing.toggleSmooth(BezierPoint(20f,30f))
        assertTrue(smooth.hasOutHandle)
        assertEquals(BezierPoint(20f,30f),VectorEditing.toggleSmooth(smooth))
        val moved=VectorEditing.movePoint(smooth,50f,70f)
        assertEquals(smooth.outHandleX+30,moved.outHandleX,.001f)
        val bent=VectorEditing.bend(moved,100f,120f)
        assertEquals(0f,bent.inHandleX,.001f)
        val layer=Layer(type=LayerType.VECTOR).withVectorPaths(listOf(added.copy(isClosed=true),path.copy(strokeCap=VectorCap.ROUND,strokeJoin=VectorJoin.BEVEL,dashLength=12f)))
        val doc=ProjectDocument(layers=listOf(layer,Layer(type=LayerType.TEXT,textData=TextLayerData(followPathLayerId=layer.id))))
        val tree=ActionTree(doc)
        val removed=doc.updateLayer(layer.id){ it.withVectorPaths(it.editablePaths.map { p -> p.copy(points=p.points.dropLast(1)) }) }
        tree.recordAction("Remove point",removed);assertTrue(tree.undo());assertEquals(doc,tree.currentDocument)
        val bytes=ByteArrayOutputStream();SgProjectArchive.exportToStream(doc,bytes)
        assertEquals(doc,SgProjectArchive.importFromStream(bytes.toByteArray().inputStream()).first)
        assertEquals(2,layer.editablePaths.size)
        assertTrue(layer.editablePaths.first().samplePath().size>2)
    }
    @Test fun cornerRemainsCornerAfterFractionalMove() {
        val corner = BezierPoint(-175.53192f, -79.78723f)
        val moved = VectorEditing.movePoint(corner,-95.74468f,-26.595743f)
        assertFalse(moved.hasInHandle); assertFalse(moved.hasOutHandle)
        assertTrue(VectorEditing.toggleSmooth(moved).hasOutHandle)
    }
    @Test fun inverseTransformSupportsRotationAndScale() {
        val t=Transform2D(translationX=10f,translationY=20f,scaleX=2f,scaleY=3f,rotationDegrees=90f)
        val (x,y)=VectorEditing.toLocal(-20f,40f,t)
        assertEquals(10f,x,.001f);assertEquals(10f,y,.001f)
    }
}
