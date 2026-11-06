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
        while (curr != null) {
            mainPathSet.add(curr.id)
            curr = curr.parentId?.let { allNodes[it] }
        }

        // 2. Identify Undone Nodes (descendants of nodes along main path that are NOT on main path)
        val undoneSet = mutableSetOf<String>()
        for (node in allNodes.values) {
            if (!mainPathSet.contains(node.id)) {
                undoneSet.add(node.id)
            }
        }

        // 3. Topological traversal from root to assign lanes and vertical Y order
        val laneAssignment = mutableMapOf<String, Int>()
        val yOrder = mutableMapOf<String, Int>()
        var nextLane = 1
        var currentY = 0

        // Queue for BFS traversal
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(rootNodeId)

        while (queue.isNotEmpty()) {
            val nodeId = queue.removeFirst()
            if (visited.contains(nodeId)) continue
            visited.add(nodeId)

            val node = allNodes[nodeId] ?: continue

            // Lane 0 is reserved for Main Path
            val lane = if (mainPathSet.contains(nodeId)) {
                0
            } else {
                laneAssignment[nodeId] ?: run {
                    val assigned = nextLane++
                    assigned
                }
            }
            laneAssignment[nodeId] = lane
            yOrder[nodeId] = currentY++

            // Process children: main path child first, then side branches
            val sortedChildren = node.childrenIds.sortedByDescending { mainPathSet.contains(it) }
            for (childId in sortedChildren) {
                if (!mainPathSet.contains(childId) && !laneAssignment.containsKey(childId)) {
                    laneAssignment[childId] = nextLane++
                }
                queue.add(childId)
            }
        }

        // Include any remaining orphan nodes
        for (nodeId in allNodes.keys) {
            if (!visited.contains(nodeId)) {
                laneAssignment[nodeId] = nextLane++
                yOrder[nodeId] = currentY++
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
