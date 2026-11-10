package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class LayerStackCommandsTest {
    @Test fun randomReorderGroupUndoRedoKeepsOneTopFirstStackAndAllLayers() {
        val random = Random(9081)
        val original = (0..19).map { Layer(id="l$it", name="Layer $it", shapeData=ShapeData()) }
        val tree = ActionTree(ProjectDocument(layers=original))
        var groupNumber = 0
        repeat(1200) {
            val doc = tree.currentDocument
            val list = doc.layers
            when (random.nextInt(6)) {
                0 -> if (list.size > 1) {
                    val from = list.random(random).id
                    val to = list.random(random).id
                    tree.recordCommand(ActionIdentity("Reorder","Layers")) { LayerStackCommands.reorder(it,null,from,to) }
                }
                1 -> if (list.size > 1) {
                    val selected=list.shuffled(random).take(2).map { it.id }.toSet()
                    val id="g${groupNumber++}"
                    tree.recordCommand(ActionIdentity("Group","Layers")) { LayerStackCommands.group(it,null,selected,id) }
                }
                2 -> list.firstOrNull { it.type==LayerType.GROUP }?.let { group ->
                    tree.recordCommand(ActionIdentity("Ungroup","Layers")) { LayerStackCommands.ungroup(it,null,group.id) }
                }
                3 -> tree.undo()
                4 -> tree.redo()
                else -> tree.jumpTo(tree.nodes.keys.random(random))
            }
            fun leaves(list: List<Layer>): List<String> = list.flatMap { if(it.type==LayerType.GROUP) leaves(it.children) else listOf(it.id) }
            assertEquals(original.map { it.id }.toSet(), leaves(tree.currentDocument.layers).toSet())
            assertEquals(original.size, leaves(tree.currentDocument.layers).size)
            // Panel list is the one renderer consumes, with its order reversed only for painter traversal.
            assertEquals(tree.currentDocument.layers, LayerStackCommands.layers(tree.currentDocument,null))
        }
    }

    @Test fun nestedGroupCommandsPreserveSiblingsAndUndoAsOneAction() {
        val a=Layer(id="a"); val b=Layer(id="b"); val c=Layer(id="c")
        val outside=Layer(id="outside")
        val doc=ProjectDocument(layers=listOf(outside,Layer(id="g",type=LayerType.GROUP,children=listOf(a,b,c))))
        val tree=ActionTree(doc)
        tree.recordCommand(ActionIdentity("Reorder","Layers")) { LayerStackCommands.reorder(it,"g","a","c") }
        assertEquals(listOf("b","c","a"), LayerStackCommands.layers(tree.currentDocument,"g").map { it.id })
        assertEquals(outside,tree.currentDocument.layers.first())
        assertEquals(2,tree.nodes.size)
        tree.undo(); assertEquals(doc.layers,tree.currentDocument.layers)
        tree.redo()
        val grouped=LayerStackCommands.group(tree.currentDocument,"g",setOf("c","a"),"nested")
        assertEquals(listOf("b","nested"),LayerStackCommands.layers(grouped,"g").map{it.id})
        assertEquals(listOf("c","a"),grouped.findLayerById("nested")!!.children.map{it.id})
        assertEquals(tree.currentDocument.layers,LayerStackCommands.ungroup(grouped,"g","nested").layers)
        assertEquals(doc,LayerStackCommands.reorder(doc,null,"missing","outside"))
    }
}
