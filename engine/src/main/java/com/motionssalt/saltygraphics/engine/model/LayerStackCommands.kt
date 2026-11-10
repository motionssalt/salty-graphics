package com.motionssalt.saltygraphics.engine.model

/** All lists are top-first, including nested groups. Commands resolve against the live document. */
object LayerStackCommands {
    fun layers(document: ProjectDocument, groupId: String?): List<Layer> =
        if (groupId == null) document.layers else document.findLayerById(groupId)?.children.orEmpty()

    private fun replace(document: ProjectDocument, groupId: String?, layers: List<Layer>): ProjectDocument =
        if (groupId == null) document.copy(layers = layers) else document.updateLayer(groupId) { it.copy(children = layers) }

    fun reorder(document: ProjectDocument, groupId: String?, fromId: String, toId: String): ProjectDocument {
        val list = layers(document, groupId).toMutableList()
        val from = list.indexOfFirst { it.id == fromId }
        val to = list.indexOfFirst { it.id == toId }
        if (from < 0 || to < 0 || from == to) return document
        list.add(to, list.removeAt(from))
        return replace(document, groupId, list)
    }

    fun group(document: ProjectDocument, groupId: String?, selected: Set<String>, newId: String): ProjectDocument {
        val list = layers(document, groupId)
        val children = list.filter { it.id in selected }
        if (children.size < 2) return document
        val first = list.indexOfFirst { it.id in selected }
        val group = Layer(id = newId, name = "Group", type = LayerType.GROUP, children = children)
        val result = list.filterNot { it.id in selected }.toMutableList().apply { add(first, group) }
        return replace(document, groupId, result)
    }

    fun ungroup(document: ProjectDocument, groupId: String?, selectedId: String): ProjectDocument {
        val list = layers(document, groupId)
        val group = list.find { it.id == selectedId && it.type == LayerType.GROUP } ?: return document
        return replace(document, groupId, list.flatMap { if (it.id == group.id) group.children else listOf(it) })
    }
}
