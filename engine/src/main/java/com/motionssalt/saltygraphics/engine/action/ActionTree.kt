package com.motionssalt.saltygraphics.engine.action

import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class PasteMode(val displayName: String) {
    TRANSFORM_ONLY("Transform only"),
    EFFECTS_ONLY("Effects only"),
    BOTH("Both (Transform & Effects)")
}

/**
 * Global clipboard: holds copied layer properties and survives jumps across history branches.
 */
object GlobalClipboard {
    var copiedLayer: Layer? = null
        private set

    fun copy(layer: Layer) {
        copiedLayer = layer
    }

    fun hasContent(): Boolean = copiedLayer != null

    fun clear() {
        copiedLayer = null
    }

    /**
     * Applies copied properties to a target layer according to PasteMode.
     * Pasting REPLACES target's existing values as per section 3.9.
     */
    fun applyTo(target: Layer, mode: PasteMode): Layer {
        val source = copiedLayer ?: return target
        return when (mode) {
            PasteMode.TRANSFORM_ONLY -> target.copy(transform = source.transform)
            PasteMode.EFFECTS_ONLY -> target.copy(effects = source.effects)
            PasteMode.BOTH -> target.copy(transform = source.transform, effects = source.effects)
        }
    }
}

@Serializable
data class ActionNode(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val parentId: String? = null,
    val childrenIds: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val snapshot: ProjectDocument? = null // Periodic or state snapshot for fast O(1) jumps
)

/**
 * Tree-based action and history manager.
 */
@Serializable
data class ActionTreeSnapshot(
    val rootNodeId: String = "root",
    val currentNodeId: String = "root",
    val nodes: Map<String, ActionNode> = emptyMap()
)

class ActionTree(
    initialDocument: ProjectDocument
) {
    private val _nodes = mutableMapOf<String, ActionNode>()
    val nodes: Map<String, ActionNode> get() = _nodes

    var rootNodeId: String private set
    var currentNodeId: String private set

    // Cached states for each node
    private val documentStates = mutableMapOf<String, ProjectDocument>()

    init {
        val root = ActionNode(
            id = "root",
            description = "Initial Project",
            parentId = null,
            snapshot = initialDocument
        )
        rootNodeId = root.id
        currentNodeId = root.id
        _nodes[root.id] = root
        documentStates[root.id] = initialDocument
    }

    val currentDocument: ProjectDocument
        get() = documentStates[currentNodeId] ?: _nodes[rootNodeId]?.snapshot ?: ProjectDocument()

    /**
     * Records a new action command and transitions the document.
     * If currentNode is not a leaf (e.g. after undo), creates a new branch!
     */
    fun recordAction(description: String, newDocument: ProjectDocument): ActionNode {
        val newNodeId = UUID.randomUUID().toString()
        val parent = _nodes[currentNodeId]

        val newNode = ActionNode(
            id = newNodeId,
            description = description,
            parentId = currentNodeId,
            childrenIds = emptyList(),
            timestamp = System.currentTimeMillis(),
            snapshot = newDocument
        )

        _nodes[newNodeId] = newNode
        documentStates[newNodeId] = newDocument

        // Update parent's children list
        if (parent != null) {
            _nodes[parent.id] = parent.copy(childrenIds = parent.childrenIds + newNodeId)
        }

        currentNodeId = newNodeId
        return newNode
    }

    /**
     * Undoes to parent node on the active branch.
     */
    fun undo(): Boolean {
        val current = _nodes[currentNodeId] ?: return false
        val parentId = current.parentId ?: return false
        currentNodeId = parentId
        return true
    }

    /**
     * Redoes to the most recently added child branch.
     */
    fun redo(): Boolean {
        val current = _nodes[currentNodeId] ?: return false
        if (current.childrenIds.isEmpty()) return false
        // Pick the latest branch child
        val nextId = current.childrenIds.last()
        currentNodeId = nextId
        return true
    }

    val canUndo: Boolean
        get() = _nodes[currentNodeId]?.parentId != null

    val canRedo: Boolean
        get() = _nodes[currentNodeId]?.childrenIds?.isNotEmpty() == true

    /**
     * Jumps directly to ANY node across ANY branch in the tree.
     * Clipboard is untouched and survives!
     */
    fun jumpTo(nodeId: String): ProjectDocument? {
        val targetNode = _nodes[nodeId] ?: return null
        currentNodeId = nodeId
        return documentStates[nodeId] ?: targetNode.snapshot
    }

    /**
     * Returns list of all branch tips (leaves) for tree visualization.
     */
    fun getBranchTips(): List<ActionNode> {
        return _nodes.values.filter { it.childrenIds.isEmpty() }
    }

    /**
     * Reconstructs the path from root to current node.
     */
    fun getCurrentBranchPath(): List<ActionNode> {
        val path = mutableListOf<ActionNode>()
        var curr: ActionNode? = _nodes[currentNodeId]
        while (curr != null) {
            path.add(0, curr)
            curr = curr.parentId?.let { _nodes[it] }
        }
        return path
    }

    fun toSnapshot(): ActionTreeSnapshot {
        return ActionTreeSnapshot(
            rootNodeId = rootNodeId,
            currentNodeId = currentNodeId,
            nodes = _nodes.toMap()
        )
    }

    companion object {
        fun fromSnapshot(snapshot: ActionTreeSnapshot, fallbackDocument: ProjectDocument): ActionTree {
            val tree = ActionTree(fallbackDocument)
            if (snapshot.nodes.isNotEmpty()) {
                tree._nodes.clear()
                tree._nodes.putAll(snapshot.nodes)
                tree.rootNodeId = snapshot.rootNodeId
                tree.currentNodeId = snapshot.currentNodeId
                for ((id, node) in snapshot.nodes) {
                    node.snapshot?.let { tree.documentStates[id] = it }
                }
                if (!tree.documentStates.containsKey(tree.currentNodeId)) {
                    tree.documentStates[tree.currentNodeId] = fallbackDocument
                }
            }
            return tree
        }
    }

}
