package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class DuplicateMultipleLayersTest {

    @Test
    fun duplicateMultipleSelectedLayersPreservesStackOrderAndProperties() {
        val layer1 = Layer(id = "l1", name = "Layer 1", type = LayerType.SHAPE, shapeData = ShapeData(width = 100f, height = 100f, fillColor = 0xFFFF0000L))
        val layer2 = Layer(id = "l2", name = "Layer 2", type = LayerType.TEXT, textData = TextLayerData(text = "Hello"))
        val child = Layer(id = "c1", name = "Child 1", type = LayerType.SHAPE)
        val group = Layer(id = "g1", name = "Group 1", type = LayerType.GROUP, children = listOf(child))
        val layer3 = Layer(id = "l3", name = "Layer 3", type = LayerType.VECTOR)

        val doc = ProjectDocument(
            id = "dup_test_doc",
            layers = listOf(layer1, layer2, group, layer3)
        )

        // Select layer 1, group 1, and layer 3 (3 layers selected)
        val selectedIds = setOf("l1", "g1", "l3")
        val panelLayers = doc.layers
        val targets = panelLayers.filter { it.id in selectedIds }
        assertEquals(3, targets.size)

        var currentList = panelLayers.toMutableList()
        val newSelectedIds = mutableSetOf<String>()
        targets.forEach { target ->
            val dup = target.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = "${target.name} Copy"
            )
            newSelectedIds.add(dup.id)
            val idx = currentList.indexOfFirst { it.id == target.id }
            if (idx >= 0) currentList.add(idx, dup) else currentList.add(0, dup)
        }

        val updatedDoc = doc.copy(layers = currentList, selectedLayerIds = newSelectedIds)

        // Total layers should now be 4 + 3 = 7
        assertEquals(7, updatedDoc.layers.size)
        assertEquals(3, newSelectedIds.size)
        assertEquals(newSelectedIds, updatedDoc.selectedLayerIds)

        // Check relative order: Each copy placed directly above original
        // Original list: [l1, l2, g1, l3]
        // l1 duplicate added at idx: [l1_copy, l1, l2, g1, l3]
        // g1 duplicate added at idx: [l1_copy, l1, l2, g1_copy, g1, l3]
        // l3 duplicate added at idx: [l1_copy, l1, l2, g1_copy, g1, l3_copy, l3]
        val names = updatedDoc.layers.map { it.name }
        assertEquals("Layer 1 Copy", names[0])
        assertEquals("Layer 1", names[1])
        assertEquals("Layer 2", names[2])
        assertEquals("Group 1 Copy", names[3])
        assertEquals("Group 1", names[4])
        assertEquals("Layer 3 Copy", names[5])
        assertEquals("Layer 3", names[6])

        // Verify group duplicate preserved children
        val dupGroup = updatedDoc.layers[3]
        assertEquals(1, dupGroup.children.size)
        assertEquals("Child 1", dupGroup.children[0].name)
    }
}
