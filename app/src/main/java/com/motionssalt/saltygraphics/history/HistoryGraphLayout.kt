package com.motionssalt.saltygraphics.history

import com.motionssalt.saltygraphics.engine.action.ActionNode

data class GraphNodeLayout(
    val node: ActionNode,
    val lane: Int,
    val yIndex: Int,
    val isOnMainPath: Boolean,
    val isCurrent: Boolean,
    val isBranchPoint: Boolean,
    val isUndone: Boolean
)

data class GraphEdgeLayout(
    val fromNodeId: String,
    val toNodeId: String,
    val fromLane: Int,
    val fromYIndex: Int,
    val toLane: Int,
    val toYIndex: Int,
    val isOnMainPath: Boolean
)

data class HistoryGraphData(
    val nodes: List<GraphNodeLayout>,
    val edges: List<GraphEdgeLayout>,
    val currentNodeId: String,
    val rootNodeId: String,
    val totalLanes: Int
)

object HistoryGraphCalculator {

    fun computeGraph(
        allNodes: Map<String, ActionNode>,
        rootNodeId: String,
        currentNodeId: String
    ): HistoryGraphData {
        if (allNodes.isEmpty()) {
            return HistoryGraphData(emptyList(), emptyList(), currentNodeId, rootNodeId, 1)
        }

        // 1. Identify Main Path (from currentNode back to root)
        val mainPathSet = mutableSetOf<String>()
        var curr: ActionNode? = allNodes[currentNodeId]
        while (curr != null && mainPathSet.add(curr.id)) {
            curr = curr.parentId?.let { allNodes[it] }
        }

        // 2. Identify Undone Nodes (descendants of nodes along main path that are NOT on main path)
        val undoneSet = mutableSetOf<String>()
        for (node in allNodes.values) {
            if (!mainPathSet.contains(node.id)) {
                undoneSet.add(node.id)
            }
        }

        // 3. Topological traversal from root to assign lanes and depth-based vertical Y order
        val laneAssignment = mutableMapOf<String, Int>()
        val yOrder = mutableMapOf<String, Int>()
        var nextLane = 1

        // Assign main path: all nodes on main path get lane 0 and sequential depth
        val mainPathList = mutableListOf<String>()
        var pCurr: ActionNode? = allNodes[currentNodeId]
        while (pCurr != null) {
            mainPathList.add(0, pCurr.id)
            pCurr = pCurr.parentId?.let { allNodes[it] }
        }
        mainPathList.forEachIndexed { index, id ->
            laneAssignment[id] = 0
            yOrder[id] = index
        }

        // Recursive or queue-based DFS to lay out branches in dedicated lanes
        val visited = mutableSetOf<String>()
        visited.addAll(mainPathList)

        fun assignBranch(nodeId: String, lane: Int, depth: Int) {
            if (nodeId in visited) return
            visited.add(nodeId)
            laneAssignment[nodeId] = lane
            yOrder[nodeId] = depth

            val node = allNodes[nodeId] ?: return
            var continuationLane = lane
            var firstChild = true
            for (childId in node.childrenIds) {
                if (childId !in mainPathSet) {
                    val childLane = if (firstChild) continuationLane else nextLane++
                    firstChild = false
                    assignBranch(childId, childLane, depth + 1)
                }
            }
        }

        // Process side branches forking from main path nodes
        for (mainNodeId in mainPathList) {
            val node = allNodes[mainNodeId] ?: continue
            val parentDepth = yOrder[mainNodeId] ?: 0
            for (childId in node.childrenIds) {
                if (childId !in mainPathSet) {
                    val branchLane = nextLane++
                    assignBranch(childId, branchLane, parentDepth + 1)
                }
            }
        }

        // Include any remaining orphan nodes
        var orphanY = (yOrder.values.maxOrNull() ?: 0) + 1
        for (nodeId in allNodes.keys) {
            if (nodeId !in visited) {
                laneAssignment[nodeId] = nextLane++
                yOrder[nodeId] = orphanY++
                visited.add(nodeId)
            }
        }

        // 4. Build Node Layouts
        val layoutNodes = allNodes.values.map { node ->
            val isMain = mainPathSet.contains(node.id)
            GraphNodeLayout(
                node = node,
                lane = laneAssignment[node.id] ?: 0,
                yIndex = yOrder[node.id] ?: 0,
                isOnMainPath = isMain,
                isCurrent = node.id == currentNodeId,
                isBranchPoint = node.childrenIds.size > 1,
                isUndone = undoneSet.contains(node.id)
            )
        }.sortedBy { it.yIndex }

        // 5. Build Edge Layouts
        val edges = mutableListOf<GraphEdgeLayout>()
        for (node in allNodes.values) {
            val parentId = node.parentId ?: continue
            val parentNode = allNodes[parentId] ?: continue
            val fromLane = laneAssignment[parentId] ?: 0
            val fromY = yOrder[parentId] ?: 0
            val toLane = laneAssignment[node.id] ?: 0
            val toY = yOrder[node.id] ?: 0
            val edgeIsMain = mainPathSet.contains(node.id) && mainPathSet.contains(parentId)

            edges.add(
                GraphEdgeLayout(
                    fromNodeId = parentId,
                    toNodeId = node.id,
                    fromLane = fromLane,
                    fromYIndex = fromY,
                    toLane = toLane,
                    toYIndex = toY,
                    isOnMainPath = edgeIsMain
                )
            )
        }

        return HistoryGraphData(
            nodes = layoutNodes,
            edges = edges,
            currentNodeId = currentNodeId,
            rootNodeId = rootNodeId,
            totalLanes = maxOf(1, nextLane)
        )
    }
}
