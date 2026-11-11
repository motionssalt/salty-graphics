package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random
import kotlin.math.*

class SelectionGeometryTest {
    private val box=SelectionBounds(-100f,-60f,100f,60f)
    @Test fun objectCenterIsFixedAndLinkedScalePreservesExistingRatio() {
        val start = Transform2D(translationX = 123f, translationY = -40f, scaleX = 2f, scaleY = 3f, rotationDegrees = 37f)
        val center = SelectionGeometry.world(box.center, start)
        val down = SelectionGeometry.world(SelectionPoint(box.right, box.bottom), start)
        val t = SelectionGeometry.drag(start, box, SelectionHandle.CORNER_BOTTOM_RIGHT, down, down + SelectionPoint(90f, 130f))
        val after = SelectionGeometry.world(box.center, t)
        assertEquals("Center X must remain identical after corner drag", center.x, after.x, .001f)
        assertEquals("Center Y must remain identical after corner drag", center.y, after.y, .001f)
        assertEquals("Linked scale preserves aspect ratio", start.scaleX / start.scaleY, t.scaleX / t.scaleY, .0001f)
    }

    @Test fun scaleAroundCenterForAllHandlesOnUnrotatedAndRotatedLayers() {
        val handles = listOf(
            SelectionHandle.CORNER_TOP_LEFT, SelectionHandle.CORNER_TOP_RIGHT,
            SelectionHandle.CORNER_BOTTOM_RIGHT, SelectionHandle.CORNER_BOTTOM_LEFT,
            SelectionHandle.EDGE_TOP, SelectionHandle.EDGE_BOTTOM,
            SelectionHandle.EDGE_LEFT, SelectionHandle.EDGE_RIGHT
        )
        val rotations = listOf(0f, 30f, 45f, 90f, 180f, 275f)
        val linkedStates = listOf(true, false)

        for (rot in rotations) {
            for (linked in linkedStates) {
                val start = Transform2D(translationX = 150f, translationY = 220f, scaleX = 1.5f, scaleY = 2.0f, rotationDegrees = rot, isScaleLinked = linked)
                val expectedCenter = SelectionGeometry.world(box.center, start)

                for (h in handles) {
                    val handlePoint = SelectionPoint(
                        if (h.x < 0) box.left else if (h.x > 0) box.right else box.center.x,
                        if (h.y < 0) box.top else if (h.y > 0) box.bottom else box.center.y
                    )
                    val down = SelectionGeometry.world(handlePoint, start)
                    val dragVector = SelectionPoint(30f, -40f)
                    val result = SelectionGeometry.drag(start, box, h, down, down + dragVector)

                    val actualCenter = SelectionGeometry.world(box.center, result)
                    assertEquals("Center X must stay fixed for handle $h at rot $rot linked $linked", expectedCenter.x, actualCenter.x, .001f)
                    assertEquals("Center Y must stay fixed for handle $h at rot $rot linked $linked", expectedCenter.y, actualCenter.y, .001f)
                    assertTrue("Resulting scale must be valid", SelectionGeometry.valid(result))
                }
            }
        }
    }

    @Test fun freeCornerAndEdgesScaleOnlyIntendedAxesAroundCenter() {
        val start = Transform2D(rotationDegrees = 90f, isScaleLinked = false)
        val center = SelectionGeometry.world(box.center, start)
        val down = SelectionGeometry.world(SelectionPoint(100f, 0f), start)
        val t = SelectionGeometry.drag(start, box, SelectionHandle.EDGE_RIGHT, down, down + SelectionPoint(0f, 100f))
        assertEquals(2.0f, t.scaleX, .0001f)
        assertEquals(1f, t.scaleY, 0f)
        val after = SelectionGeometry.world(box.center, t)
        assertEquals(center.x, after.x, .001f)
        assertEquals(center.y, after.y, .001f)

        val free = SelectionGeometry.drag(
            Transform2D(isScaleLinked = false), box, SelectionHandle.CORNER_BOTTOM_RIGHT,
            SelectionPoint(100f, 60f), SelectionPoint(200f, 60f)
        )
        assertEquals(2.0f, free.scaleX, .0001f)
        assertEquals(1f, free.scaleY, 0f)
    }
    @Test fun rotationUsesPointerDeltaAndVisibleCenterWithoutJumpingFromTouchSlop() {
        val b=SelectionBounds(20f,10f,120f,90f)
        val start=Transform2D(rotationDegrees=32f,scaleX=2f,scaleY=3f)
        val center=SelectionGeometry.world(b.center,start)
        val down=center+SelectionPoint(80f,-120f)
        assertEquals(start,SelectionGeometry.drag(start,b,SelectionHandle.ROTATE,down,down))
        val pointer=center+(down-center).rotated(13f)
        val t=SelectionGeometry.drag(start,b,SelectionHandle.ROTATE,down,pointer)
        assertEquals(45f,t.rotationDegrees,.001f)
        val after=SelectionGeometry.world(b.center,t)
        assertEquals(center.x,after.x,.001f);assertEquals(center.y,after.y,.001f)
    }
    @Test fun tenThousandRandomDragsCannotDeleteOrCorruptLayers() {
        val random=Random(739102)
        val handles=SelectionHandle.entries.filter { it!=SelectionHandle.NONE }
        val nested=Layer(id="nested",type=LayerType.GROUP,children=listOf(Layer(id="child",shapeData=ShapeData())))
        var doc=ProjectDocument(layers=listOf(Layer(id="target",shapeData=ShapeData()),nested))
        val originalIds=doc.layers.flatMap { listOf(it.id)+it.children.map { child -> child.id } }
        repeat(10000) { i ->
            val start=Transform2D(translationX=random.nextFloat()*1e5f,translationY=random.nextFloat()*-1e5f,
                scaleX=.01f+random.nextFloat()*100f,scaleY=.01f+random.nextFloat()*100f,
                rotationDegrees=random.nextFloat()*360f,isScaleLinked=random.nextBoolean())
            val zoom=10f.pow(random.nextFloat()*6-3) // tiny/huge zoom and pointer deltas.
            val down=SelectionPoint(random.nextFloat()*1000f,random.nextFloat()*1000f)
            val delta=when(i%37) { 0 -> Float.NaN;1 -> Float.POSITIVE_INFINITY;2 -> Float.MAX_VALUE;else -> (random.nextFloat()-.5f)*1e6f/zoom }
            val end=down+SelectionPoint(delta,if(random.nextBoolean()) -delta else delta*.001f)
            val transformed=SelectionGeometry.drag(start,box,handles[i%handles.size],down,end)
            assertTrue("Invalid at $i: $transformed",SelectionGeometry.valid(transformed))
            doc=SelectionGeometry.apply(doc,"target",transformed)
            // Simulated cancellation/multi-touch restores transform only, not an old document.
            if(i%13==0) doc=SelectionGeometry.apply(doc,"target",start)
            assertEquals(originalIds,doc.layers.flatMap { listOf(it.id)+it.children.map { child -> child.id } })
            assertTrue(SelectionGeometry.valid(doc.layers.first().transform))
        }
    }
    @Test fun transformBoundaryRejectsBadOrMissingTargetsAndNeverResurrectsRemovedLayers() {
        val layer=Layer(id="target");val doc=ProjectDocument(layers=listOf(layer,Layer(id="new")))
        for(t in listOf(Transform2D(scaleX=0f),Transform2D(scaleY=-1f),Transform2D(translationX=Float.NaN),Transform2D(rotationDegrees=Float.POSITIVE_INFINITY))) {
            assertSame(doc,SelectionGeometry.apply(doc,layer.id,t))
        }
        val removed=doc.removeLayer(layer.id)
        assertSame(removed,SelectionGeometry.apply(removed,layer.id,Transform2D(scaleX=2f)))
        val changed=SelectionGeometry.apply(doc,layer.id,Transform2D(scaleX=2f))
        assertEquals(2,changed.layers.size);assertEquals("new",changed.layers.last().id)
        val locked=doc.updateLayer(layer.id){it.copy(isLocked=true)}
        assertSame(locked,SelectionGeometry.apply(locked,layer.id,Transform2D(scaleX=2f)))
    }
}
