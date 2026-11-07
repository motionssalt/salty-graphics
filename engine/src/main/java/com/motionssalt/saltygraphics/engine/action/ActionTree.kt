package com.motionssalt.saltygraphics.engine.action

import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class PasteMode(val displayName: String) {
    TRANSFORM_ONLY("Transform only"),
    EFFECTS_ONLY("Effects only"),
    BOTH("Both (Transform & Effects)"),
    SELECTIVE("Selective Properties")
}

data class SelectivePasteProperties(
    val transform: Boolean = true,
    val effects: Boolean = true,
    val fill: Boolean = true,
    val stroke: Boolean = true,
    val shadow: Boolean = true,
    val blendMode: Boolean = true,
    val opacity: Boolean = true,
    val mask: Boolean = true
)

/**
 * Global clipboard: holds copied layer properties and survives jumps across history branches.
 */
object GlobalClipboard {
    var copiedLayer: Layer? = null
        private set

    var copiedLayers: List<Layer> = emptyList()
        private set

    fun copy(layer: Layer) {
        copiedLayer = layer
        copiedLayers = listOf(layer)
    }

    fun copyMultiple(layers: List<Layer>) {
        copiedLayers = layers
        copiedLayer = if (layers.size == 1) layers.first() else null
    }

    fun hasContent(): Boolean = copiedLayer != null || copiedLayers.isNotEmpty()
    fun isMultiple(): Boolean = copiedLayers.size > 1

    fun clear() {
        copiedLayer = null
        copiedLayers = emptyList()
    }

    /**
     * Applies copied properties to a target layer according to PasteMode or SelectivePasteProperties.
     * Pasting REPLACES target's existing values as per section 3.9 and T24.
     */
    fun applyTo(target: Layer, mode: PasteMode): Layer {
        val source = copiedLayer ?: return target
        return when (mode) {
            PasteMode.TRANSFORM_ONLY -> target.copy(transform = source.transform)
            PasteMode.EFFECTS_ONLY -> target.copy(effects = source.effects)
            PasteMode.BOTH -> target.copy(transform = source.transform, effects = source.effects)
            PasteMode.SELECTIVE -> applySelective(target, source, SelectivePasteProperties())
        }
    }

    fun applySelective(target: Layer, source: Layer, props: SelectivePasteProperties): Layer {
        var result = target
        if (props.transform) result = result.copy(transform = source.transform)
        if (props.effects) result = result.copy(effects = source.effects)
        if (props.fill) result = result.copy(fillData = source.effectiveFill)
        if (props.stroke) result = result.copy(strokeData = source.effectiveStroke)
        if (props.shadow) result = result.copy(shadowData = source.effectiveShadow)
        if (props.blendMode) result = result.copy(blendMode = source.blendMode)
        if (props.opacity) result = result.copy(opacity = source.opacity)
        if (props.mask) result = result.copy(
            maskType = source.effectiveMaskType,
            maskSourceLayerId = source.effectiveMaskSourceLayerId,
            isMaskInverted = source.effectiveMaskInverted
        )
        return result
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
    val nodes: Map<String, ActionNode> = emptyMap(),
    val preferredChildren: Map<String, String> = emptyMap()
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
    private val preferredChildren = mutableMapOf<String, String>()

    // Legacy .sg documents may contain selection, but it is UI state, never history.
    private fun documentState(document: ProjectDocument) = document.copy(selectedLayerIds = emptySet())

    init {
        val initialState = documentState(initialDocument)
        val root = ActionNode(
            id = "root",
            description = "Initial Project",
            parentId = null,
            snapshot = initialState
        )
        rootNodeId = root.id
        currentNodeId = root.id
        _nodes[root.id] = root
        documentStates[root.id] = initialState
    }

    val currentDocument: ProjectDocument
        get() = documentStates[currentNodeId] ?: _nodes[rootNodeId]?.snapshot ?: ProjectDocument()

    /**
     * Records a new action command and transitions the document.
     * If currentNode is not a leaf (e.g. after undo), creates a new branch!
     */
    fun recordAction(description: String, newDocument: ProjectDocument): ActionNode {
        val state = documentState(newDocument)
        // Refuse selection-only/timestamp-only commands even if a future caller
        // accidentally routes a layer tap through the document command boundary.
        if (state.copy(updatedAt = currentDocument.updatedAt) == currentDocument) {
            return _nodes.getValue(currentNodeId)
        }
        val newNodeId = UUID.randomUUID().toString()
        val parent = _nodes[currentNodeId]

        val newNode = ActionNode(
            id = newNodeId,
            description = description,
            parentId = currentNodeId,
            childrenIds = emptyList(),
            timestamp = System.currentTimeMillis(),
            snapshot = state
        )

        _nodes[newNodeId] = newNode
        documentStates[newNodeId] = state

        // Update parent's children list
        if (parent != null) {
            _nodes[parent.id] = parent.copy(childrenIds = parent.childrenIds + newNodeId)
            preferredChildren[parent.id] = newNodeId
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
        preferredChildren[parentId] = current.id
        currentNodeId = parentId
        return true
    }

    /**
     * Redoes along the most recently active child branch (including explicit jumps).
     */
    fun redo(): Boolean {
        val current = _nodes[currentNodeId] ?: return false
        if (current.childrenIds.isEmpty()) return false
        val nextId = preferredChildren[current.id]?.takeIf { it in current.childrenIds }
            ?: current.childrenIds.last()
        if (nextId !in _nodes) return false
        preferredChildren[current.id] = nextId
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
        // Remember the entire visited path, not merely the target's direct parent.
        val visited = mutableSetOf<String>()
        var child = targetNode
        while (visited.add(child.id)) {
            val parentId = child.parentId ?: break
            val parent = _nodes[parentId] ?: break
            if (child.id in parent.childrenIds) preferredChildren[parentId] = child.id
            child = parent
        }
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
        val visited = mutableSetOf<String>()
        while (curr != null && visited.add(curr.id)) {
            path.add(0, curr)
            curr = curr.parentId?.let { _nodes[it] }
        }
        return path
    }

    fun toSnapshot(): ActionTreeSnapshot {
        return ActionTreeSnapshot(
            rootNodeId = rootNodeId,
            currentNodeId = currentNodeId,
            nodes = _nodes.toMap(),
            preferredChildren = preferredChildren.toMap()
        )
    }

    companion object {
        fun fromSnapshot(snapshot: ActionTreeSnapshot, fallbackDocument: ProjectDocument): ActionTree {
            val tree = ActionTree(fallbackDocument)
            if (snapshot.nodes.isNotEmpty()) {
                tree._nodes.clear()
                tree._nodes.putAll(snapshot.nodes.mapValues { (_, node) ->
                    node.copy(snapshot = node.snapshot?.let { tree.documentState(it) })
                })
                tree.rootNodeId = snapshot.rootNodeId
                tree.currentNodeId = snapshot.currentNodeId
                tree.preferredChildren.putAll(snapshot.preferredChildren.filter { (parent, child) ->
                    child in (tree._nodes[parent]?.childrenIds ?: emptyList())
                })
                for ((id, node) in tree._nodes) {
                    node.snapshot?.let { tree.documentStates[id] = it }
                }
                if (!tree.documentStates.containsKey(tree.currentNodeId)) {
                    tree.documentStates[tree.currentNodeId] = tree.documentState(fallbackDocument)
                }
            }
            return tree
        }
    }

}
