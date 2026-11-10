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
    GROUP("Group"),
    QUARANTINED("Quarantined")
}

@Serializable
data class QuarantinedLayerData(
    val rawJson: String,
    val error: String,
    val originalLayerId: String? = null,
    val originalLayerName: String? = null
)

@Serializable
data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Layer",
    val type: LayerType = LayerType.SHAPE,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1.0f,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val maskType: MaskType = MaskType.NONE,
    val maskSourceLayerId: String? = null,
    val isMaskInverted: Boolean = false,
    val maskMode: MaskMode = MaskMode.NONE,
    val affectsLayerIds: List<String> = emptyList(), // For masks and adjustment layers
    val transform: Transform2D = Transform2D(),
    val shapeData: ShapeData? = null,
    val vectorPathData: VectorPathData? = null,
    val additionalVectorPaths: List<VectorPathData> = emptyList(),
    val textData: TextLayerData? = null,
    val mediaAssetPath: String? = null,
    val mediaWidth: Float? = null,
    val mediaHeight: Float? = null,
    val effects: List<EffectInstance> = emptyList(),
    val children: List<Layer> = emptyList(), // For Group layers
    val quarantinedData: QuarantinedLayerData? = null,
    val fillData: FillData? = null,
    val strokeData: StrokeData? = null,
    val shadowData: ShadowData? = null
) {
    /**
     * Universal Fill resolution with backward-compatibility for legacy layer-specific fills.
     */
    val effectiveFill: FillData
        get() = fillData ?: when (type) {
            LayerType.SHAPE -> FillData(
                type = FillType.SOLID,
                solidColor = shapeData?.fillColor ?: 0xFF00E5FFL,
                solidOpacity = if (shapeData?.hasFill == false) 0f else 1f
            )
            LayerType.TEXT -> FillData(
                type = FillType.SOLID,
                solidColor = textData?.color ?: 0xFFFFFFFFL,
                solidOpacity = 1f
            )
            LayerType.VECTOR -> FillData(
                type = FillType.SOLID,
                solidColor = vectorPathData?.fillColor ?: 0x00000000L,
                solidOpacity = if (vectorPathData?.hasFill == true) 1f else 0f
            )
            LayerType.MEDIA -> FillData(
                type = FillType.MEDIA,
                mediaAssetPath = mediaAssetPath,
                mediaScaleMode = MediaScaleMode.FILL
            )
            else -> FillData()
        }

    /**
     * Universal Stroke resolution with backward-compatibility for legacy shape/vector/text strokes.
     */
    val effectiveStroke: StrokeData
        get() = strokeData ?: when (type) {
            LayerType.SHAPE -> StrokeData(
                isEnabled = shapeData?.hasStroke == true,
                color = shapeData?.strokeColor ?: 0xFFFFFFFFL,
                width = shapeData?.strokeWidth ?: 4f
            )
            LayerType.VECTOR -> StrokeData(
                isEnabled = vectorPathData?.hasStroke == true,
                color = vectorPathData?.strokeColor ?: 0xFFFFFFFFL,
                width = vectorPathData?.strokeWidth ?: 4f,
                cap = vectorPathData?.strokeCap ?: VectorCap.ROUND,
                join = vectorPathData?.strokeJoin ?: VectorJoin.ROUND,
                isDashed = (vectorPathData?.dashLength ?: 0f) > 0f,
                dashLength = vectorPathData?.dashLength ?: 12f,
                dashGap = vectorPathData?.dashGap ?: 8f
            )
            LayerType.TEXT -> StrokeData(
                isEnabled = textData?.hasOutline == true,
                color = textData?.outlineColor ?: 0xFF000000L,
                width = textData?.outlineWidth ?: 2f
            )
            else -> StrokeData()
        }

    val effectiveShadow: ShadowData
        get() = shadowData ?: ShadowData()

    val effectiveMaskType: MaskType
        get() = when {
            maskType != MaskType.NONE -> maskType
            maskMode == MaskMode.IN_MASK -> if (type == LayerType.VECTOR) MaskType.VECTOR else MaskType.ALPHA
            maskMode == MaskMode.OUT_MASK -> if (type == LayerType.VECTOR) MaskType.VECTOR else MaskType.ALPHA
            else -> MaskType.NONE
        }

    val effectiveMaskInverted: Boolean
        get() = when {
            maskType != MaskType.NONE -> isMaskInverted
            maskMode == MaskMode.OUT_MASK -> true
            else -> false
        }

    val effectiveMaskSourceLayerId: String?
        get() = maskSourceLayerId ?: affectsLayerIds.firstOrNull()

    val editablePaths: List<VectorPathData> get() = listOfNotNull(vectorPathData) + additionalVectorPaths

    fun withVectorPaths(paths: List<VectorPathData>): Layer = copy(
        vectorPathData = paths.firstOrNull(), additionalVectorPaths = paths.drop(1))

    /**
     * Approximate bounds in local coordinate space (before transform).
     */
    val localWidth: Float
        get() = when (type) {
            LayerType.SHAPE -> shapeData?.width ?: 200f
            LayerType.TEXT -> {
                textData?.let {
                    val longestLineLen = it.text.split("\r\n", "\n").maxOfOrNull { l -> l.length } ?: 1
                    val charW = it.fontSize * 0.6f
                    when (it.boxMode) {
                        TextBoxMode.FREE -> maxOf(100f, longestLineLen * charW)
                        else -> maxOf(100f, it.boxWidth)
                    }
                } ?: 400f
            }
            LayerType.MEDIA -> mediaWidth ?: 400f
            LayerType.VECTOR -> {
                val pts = editablePaths.flatMap { it.points }
                if (pts.isNotEmpty()) {
                    val minX = pts.minOf { it.x }
                    val maxX = pts.maxOf { it.x }
                    maxOf(40f, maxX - minX)
                } else 300f
            }
            LayerType.ADJUSTMENT -> 1000f
            LayerType.GROUP -> 400f
            LayerType.QUARANTINED -> 240f
        }

    val localHeight: Float
        get() = when (type) {
            LayerType.SHAPE -> shapeData?.height ?: 200f
            LayerType.TEXT -> {
                textData?.let {
                    val lineCount = it.text.split("\r\n", "\n").size.coerceAtLeast(1)
                    val singleLineH = it.fontSize * (it.lineSpacing / 100f)
                    when (it.boxMode) {
                        TextBoxMode.FREE -> maxOf(singleLineH, lineCount * singleLineH)
                        else -> maxOf(it.boxHeight, lineCount * singleLineH)
                    }
                } ?: 90f
            }
            LayerType.MEDIA -> mediaHeight ?: 400f
            LayerType.VECTOR -> {
                val pts = editablePaths.flatMap { it.points }
                if (pts.isNotEmpty()) {
                    val minY = pts.minOf { it.y }
                    val maxY = pts.maxOf { it.y }
                    maxOf(40f, maxY - minY)
                } else 300f
            }
            LayerType.ADJUSTMENT -> 1000f
            LayerType.GROUP -> 400f
            LayerType.QUARANTINED -> 120f
        }
}

@Serializable
data class EditorSettings(
    val backgroundColor: Long = 0xFF808080L, // Neutral gray default canvas color (V14)
    val showGrid: Boolean = false,
    val gridSize: Int = 20,
    val gridColor: Long = 0x66808080L,
    val isSquareGrid: Boolean = false, // When false (default), grid matches canvas aspect ratio so last row/col is not cropped (T26)
    val snapEnabled: Boolean = false, // General object / Smart Guides snapping
    val snapToGrid: Boolean = false, // V15: Optional grid-line snapping separate from general snapping
    val snapTolerancePx: Float = 8f,
    val defaultPreviewQuality: com.motionssalt.saltygraphics.engine.render.PreviewQuality = com.motionssalt.saltygraphics.engine.render.PreviewQuality.HIGH
)

@Serializable
data class ProjectDocument(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Project",
    val canvasSize: CanvasSize = CanvasSize(),
    val layers: List<Layer> = emptyList(),
    val editorSettings: EditorSettings = EditorSettings(),
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

    fun updateEditorSettings(transform: (EditorSettings) -> EditorSettings): ProjectDocument {
        return copy(editorSettings = transform(editorSettings), updatedAt = System.currentTimeMillis())
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
