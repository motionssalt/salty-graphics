package com.motionssalt.saltygraphics.engine.model

import com.motionssalt.saltygraphics.engine.effect.EffectInstance
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class LayerType(val displayName: String) {
    MEDIA("Media"),
    TEXT("Text"),
    ADJUSTMENT("Adjustment"),
    SHAPE("Shape"),
    VECTOR("Vector"),
    GROUP("Group")
}

@Serializable
data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Layer",
    val type: LayerType = LayerType.SHAPE,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1.0f,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val maskMode: MaskMode = MaskMode.NONE,
    val affectsLayerIds: List<String> = emptyList(), // For masks and adjustment layers
    val transform: Transform2D = Transform2D(),
    val shapeData: ShapeData? = null,
    val vectorPathData: VectorPathData? = null,
    val textData: TextLayerData? = null,
    val mediaAssetPath: String? = null,
    val effects: List<EffectInstance> = emptyList(),
    val children: List<Layer> = emptyList() // For Group layers
) {
    /**
     * Approximate bounds in local coordinate space (before transform).
     */
    val localWidth: Float
        get() = when (type) {
            LayerType.SHAPE -> shapeData?.width ?: 200f
            LayerType.TEXT -> 400f
            LayerType.MEDIA -> 400f
            LayerType.VECTOR -> 300f
            LayerType.ADJUSTMENT -> 1000f
            LayerType.GROUP -> 400f
        }

    val localHeight: Float
        get() = when (type) {
            LayerType.SHAPE -> shapeData?.height ?: 200f
            LayerType.TEXT -> (textData?.fontSize ?: 64f) * 1.5f
            LayerType.MEDIA -> 400f
            LayerType.VECTOR -> 300f
            LayerType.ADJUSTMENT -> 1000f
            LayerType.GROUP -> 400f
        }
}

@Serializable
data class ProjectDocument(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Project",
    val canvasSize: CanvasSize = CanvasSize(),
    val layers: List<Layer> = emptyList(),
    val selectedLayerIds: Set<String> = emptySet(),
    val activeGroupId: String? = null, // When inside a nested group editing screen
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val activeLayers: List<Layer>
        get() {
            if (activeGroupId == null) return layers
            val group = findLayerById(activeGroupId)
            return group?.children ?: layers
        }

    fun findLayerById(id: String, layerList: List<Layer> = layers): Layer? {
        for (layer in layerList) {
            if (layer.id == id) return layer
            if (layer.type == LayerType.GROUP) {
                val found = findLayerById(id, layer.children)
                if (found != null) return found
            }
        }
        return null
    }

    fun updateLayer(targetId: String, transform: (Layer) -> Layer): ProjectDocument {
        fun updateRecursive(list: List<Layer>): List<Layer> {
            return list.map { layer ->
                if (layer.id == targetId) {
                    transform(layer)
                } else if (layer.type == LayerType.GROUP) {
                    layer.copy(children = updateRecursive(layer.children))
                } else {
                    layer
                }
            }
        }
        return copy(layers = updateRecursive(layers), updatedAt = System.currentTimeMillis())
    }

    fun removeLayer(targetId: String): ProjectDocument {
        fun removeRecursive(list: List<Layer>): List<Layer> {
            return list.filter { it.id != targetId }.map { layer ->
                if (layer.type == LayerType.GROUP) {
                    layer.copy(children = removeRecursive(layer.children))
                } else {
                    layer
                }
            }
        }
        return copy(
            layers = removeRecursive(layers),
            selectedLayerIds = selectedLayerIds - targetId,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun addLayer(layer: Layer, targetGroupId: String? = activeGroupId): ProjectDocument {
        if (targetGroupId == null) {
            return copy(layers = listOf(layer) + layers, updatedAt = System.currentTimeMillis())
        }
        return updateLayer(targetGroupId) { group ->
            group.copy(children = listOf(layer) + group.children)
        }
    }
}
