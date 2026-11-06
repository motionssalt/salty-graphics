package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.engine.action.ActionNode
import com.motionssalt.saltygraphics.history.HistoryGraphCalculator
import org.junit.Assert.*
import org.junit.Test

class HistoryGraphTest {

    @Test
    fun testHistoryGraphWithBranchesAndUndoneNodes() {
        // Build a history tree:
        // root -> n1 -> n2 (current)
        //            \-> n3 (undone branch) -> n4
        val root = ActionNode(id = "root", description = "Initial", childrenIds = listOf("n1"))
        val n1 = ActionNode(id = "n1", description = "Step 1", parentId = "root", childrenIds = listOf("n2", "n3"))
        val n2 = ActionNode(id = "n2", description = "Step 2 Active", parentId = "n1")
        val n3 = ActionNode(id = "n3", description = "Step 3 Alt", parentId = "n1", childrenIds = listOf("n4"))
        val n4 = ActionNode(id = "n4", description = "Step 4 Alt Leaf", parentId = "n3")

        val allNodes = mapOf(
            "root" to root,
            "n1" to n1,
            "n2" to n2,
            "n3" to n3,
            "n4" to n4
        )

        val graph = HistoryGraphCalculator.computeGraph(allNodes, rootNodeId = "root", currentNodeId = "n2")

        // 1. Verify Node Count
        assertEquals(5, graph.nodes.size)

        // 2. Main Path: root, n1, n2 must be on main path and lane 0
        val rootLayout = graph.nodes.find { it.node.id == "root" }
        val n1Layout = graph.nodes.find { it.node.id == "n1" }
        val n2Layout = graph.nodes.find { it.node.id == "n2" }
        val n3Layout = graph.nodes.find { it.node.id == "n3" }
        val n4Layout = graph.nodes.find { it.node.id == "n4" }

        assertNotNull(rootLayout)
        assertNotNull(n1Layout)
        assertNotNull(n2Layout)
        assertNotNull(n3Layout)
        assertNotNull(n4Layout)

        assertTrue("Root must be on main path", rootLayout!!.isOnMainPath)
        assertTrue("N1 must be on main path", n1Layout!!.isOnMainPath)
        assertTrue("N2 must be on main path", n2Layout!!.isOnMainPath)
        assertEquals(0, rootLayout.lane)
        assertEquals(0, n1Layout.lane)
        assertEquals(0, n2Layout.lane)

        // Current Node
        assertTrue("N2 must be current node", n2Layout.isCurrent)
        assertFalse("N1 is not current node", n1Layout.isCurrent)

        // Branch Point
        assertTrue("N1 has 2 children and must be flagged as branch point", n1Layout.isBranchPoint)
        assertFalse("N2 has no children and is not a branch point", n2Layout.isBranchPoint)

        // Undone / Alt Branch: n3 and n4
        assertFalse("N3 is not on main path", n3Layout!!.isOnMainPath)
        assertFalse("N4 is not on main path", n4Layout!!.isOnMainPath)
        assertTrue("N3 is undone branch", n3Layout.isUndone)
        assertTrue("N4 is undone branch", n4Layout.isUndone)
        assertTrue("N3 must be on secondary lane > 0", n3Layout.lane > 0)
        assertTrue("N4 must be on secondary lane > 0", n4Layout.lane > 0)

        // 3. Verify Edges
        assertEquals(4, graph.edges.size)
        val mainEdges = graph.edges.filter { it.isOnMainPath }
        assertEquals("Two edges on main path: root->n1, n1->n2", 2, mainEdges.size)
    }
}
