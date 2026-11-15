package com.motionssalt.saltygraphics.uikit.sublibs.vector

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.uikit.controls.DropdownOption
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorPickerPage
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorSwatchRow
import com.motionssalt.saltygraphics.uikit.controls.SaltyInlineDropdown
import com.motionssalt.saltygraphics.uikit.controls.SaltyNumberControl
import com.motionssalt.saltygraphics.uikit.controls.SaltyProgressNumberControl
import com.motionssalt.saltygraphics.uikit.controls.SaltyRecentColorsRegistry
import com.motionssalt.saltygraphics.uikit.controls.SaltyToggleRow
import com.motionssalt.saltygraphics.uikit.controls.SaltyTransformPanel
import com.motionssalt.saltygraphics.uikit.overlays.SaltyVectorToolMode
import com.motionssalt.saltygraphics.uikit.structure.SaltyChip
import com.motionssalt.saltygraphics.uikit.structure.SaltyColumn
import com.motionssalt.saltygraphics.uikit.structure.SaltyOutlinedButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyPanelPage
import com.motionssalt.saltygraphics.uikit.structure.SaltyRow
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltyTabRow
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextStyle
import com.motionssalt.saltygraphics.uikit.structure.SaltyTonalButton
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxWidth

@Composable
fun SaltyVectorPage(
    layer: Layer,
    pathIndex: Int,
    selectedPoint: Int?,
    tool: SaltyVectorToolMode,
    onPathIndex: (Int) -> Unit,
    onPoint: (Int?) -> Unit,
    onTool: (SaltyVectorToolMode) -> Unit,
    onUpdate: (String, Layer) -> Unit,
    onBack: () -> Unit
) {
    var tab by remember { mutableStateOf("Shape") }
    var colorSubPage by remember { mutableStateOf<String?>(null) }
    val paths = layer.editablePaths.ifEmpty { listOf(VectorPathData()) }
    val index = pathIndex.coerceIn(0, paths.lastIndex)
    val path = paths[index]

    fun updatePath(label: String, next: VectorPathData) {
        val list = paths.toMutableList().apply { this[index] = next }
        onUpdate(label, layer.withVectorPaths(list))
    }

    if (colorSubPage != null) {
        val isFill = colorSubPage == "fillColor"
        SaltyColorPickerPage(
            title = if (isFill) "Fill Colour" else "Border Colour",
            colorArgb = if (isFill) path.fillColor else path.strokeColor,
            onPreview = { c ->
                val updated = if (isFill) path.copy(fillColor = c) else path.copy(strokeColor = c)
                updatePath(if (isFill) "Vector fill color" else "Vector border color", updated)
            },
            onCommit = { c ->
                SaltyRecentColorsRegistry.recordColor(c)
                val updated = if (isFill) path.copy(fillColor = c) else path.copy(strokeColor = c)
                updatePath(if (isFill) "Vector fill color" else "Vector border color", updated)
                colorSubPage = null
            },
            onBack = { colorSubPage = null },
            supportsTransparent = true,
            presets = SaltyRecentColorsRegistry.recentColors
        )
        return
    }

    SaltyPanelPage("Vector", onBack, scrollable = false) {
        SaltyTabRow(
            tabs = listOf("Shape", "Fill", "Border", "Path", "Transform", "Opacity"),
            selectedTab = tab,
            onSelectTab = { tab = it }
        )
        SaltyColumn(
            modifier = Modifier.weight(1f).saltyFillMaxWidth(),
            spacing = SaltySpace.XS,
            scrollable = true
        ) {
            SaltyRow(
                modifier = Modifier.saltyFillMaxWidth(),
                spacing = SaltySpace.XS,
                scrollable = true
            ) {
                paths.forEachIndexed { i, _ ->
                    SaltyChip(
                        label = "Path ${i + 1}",
                        selected = i == index,
                        onClick = { onPathIndex(i); onPoint(null) }
                    )
                }
                SaltyChip(
                    label = "+ Add",
                    selected = false,
                    onClick = {
                        onUpdate("Add vector path", layer.withVectorPaths(paths + VectorPathData()))
                        onPathIndex(paths.size)
                        onPoint(null)
                    }
                )
            }
            when (tab) {
                "Shape" -> {
                    SaltyVectorToolMode.entries.chunked(2).forEach { row ->
                        SaltyRow(modifier = Modifier.saltyFillMaxWidth(), spacing = SaltySpace.XS) {
                            row.forEach { item ->
                                SaltyTonalButton(
                                    text = item.label,
                                    onClick = { onTool(item) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    SaltyText(tool.hint, style = SaltyTextStyle.BODY_SMALL)
                    SaltyOutlinedButton(
                        text = if (path.isClosed) "Open path" else "Close path",
                        onClick = {
                            updatePath(
                                if (path.isClosed) "Open path" else "Close path",
                                path.copy(isClosed = !path.isClosed)
                            )
                        }
                    )
                    SaltyText("${path.points.size} points", style = SaltyTextStyle.LABEL_SMALL)
                }
                "Fill" -> {
                    SaltyToggleRow("Solid fill", path.hasFill, { updatePath("Vector fill", path.copy(hasFill = it)) })
                    SaltyColorSwatchRow(
                        label = "Fill colour",
                        colorArgb = path.fillColor,
                        onClick = { colorSubPage = "fillColor" }
                    )
                }
                "Border" -> {
                    SaltyToggleRow("Border", path.hasStroke, { updatePath("Vector border", path.copy(hasStroke = it)) })
                    SaltyColorSwatchRow(
                        label = "Border colour",
                        colorArgb = path.strokeColor,
                        onClick = { colorSubPage = "strokeColor" }
                    )
                    SaltyNumberControl(
                        label = "Width",
                        value = path.strokeWidth,
                        min = 0f,
                        max = 200f,
                        defaultValue = 4f,
                        step = 1f,
                        unit = "px",
                        onPreview = { updatePath("Border width", path.copy(strokeWidth = it)) },
                        onCommit = { updatePath("Border width", path.copy(strokeWidth = it)) }
                    )
                    SaltyInlineDropdown(
                        selected = path.strokeCap,
                        options = VectorCap.entries.map { DropdownOption(it, it.name) },
                        onSelect = { updatePath("Border cap", path.copy(strokeCap = it)) },
                        label = "Cap"
                    )
                    SaltyInlineDropdown(
                        selected = path.strokeJoin,
                        options = VectorJoin.entries.map { DropdownOption(it, it.name) },
                        onSelect = { updatePath("Border join", path.copy(strokeJoin = it)) },
                        label = "Join"
                    )
                    SaltyNumberControl(
                        label = "Dash",
                        value = path.dashLength,
                        min = 0f,
                        max = 200f,
                        defaultValue = 0f,
                        step = 1f,
                        unit = "px",
                        onPreview = { updatePath("Border dash", path.copy(dashLength = it)) },
                        onCommit = { updatePath("Border dash", path.copy(dashLength = it)) }
                    )
                    SaltyNumberControl(
                        label = "Gap",
                        value = path.dashGap,
                        min = 1f,
                        max = 200f,
                        defaultValue = 8f,
                        step = 1f,
                        unit = "px",
                        onPreview = { updatePath("Border gap", path.copy(dashGap = it)) },
                        onCommit = { updatePath("Border gap", path.copy(dashGap = it)) }
                    )
                }
                "Path" -> {
                    val point = path.points.getOrNull(selectedPoint ?: -1)
                    if (point == null) {
                        SaltyText("Select a point on the canvas to edit its coordinates")
                    } else {
                        fun change(p: BezierPoint) = updatePath(
                            "Point coordinates",
                            path.copy(points = path.points.mapIndexed { i, v -> if (i == selectedPoint) p else v })
                        )
                        SaltyNumberControl("Point X", point.x, -32768f, 32768f, { change(VectorEditing.movePoint(point, it, point.y)) }, { change(VectorEditing.movePoint(point, it, point.y)) }, step = 1f, unit = "px", defaultValue = 0f)
                        SaltyNumberControl("Point Y", point.y, -32768f, 32768f, { change(VectorEditing.movePoint(point, point.x, it)) }, { change(VectorEditing.movePoint(point, point.x, it)) }, step = 1f, unit = "px", defaultValue = 0f)
                        SaltyNumberControl("In X", point.inHandleX, -32768f, 32768f, { change(point.copy(inHandleX = it)) }, { change(point.copy(inHandleX = it)) }, step = 1f, unit = "px", defaultValue = point.x)
                        SaltyNumberControl("In Y", point.inHandleY, -32768f, 32768f, { change(point.copy(inHandleY = it)) }, { change(point.copy(inHandleY = it)) }, step = 1f, unit = "px", defaultValue = point.y)
                        SaltyNumberControl("Out X", point.outHandleX, -32768f, 32768f, { change(point.copy(outHandleX = it)) }, { change(point.copy(outHandleX = it)) }, step = 1f, unit = "px", defaultValue = point.x)
                        SaltyNumberControl("Out Y", point.outHandleY, -32768f, 32768f, { change(point.copy(outHandleY = it)) }, { change(point.copy(outHandleY = it)) }, step = 1f, unit = "px", defaultValue = point.y)
                    }
                }
                "Transform" -> SaltyTransformPanel(
                    transform = layer.transform,
                    snapEnabled = false,
                    applyTranslationSnapping = { it },
                    applyRotationSnapping = { it },
                    clearGuides = {},
                    onTransformChange = { onUpdate("Vector transform", layer.copy(transform = it)) },
                    onTransformCommit = { onUpdate("Vector transform", layer.copy(transform = it)) },
                    onClose = onBack
                )
                "Opacity" -> SaltyProgressNumberControl(
                    label = "Opacity",
                    value = layer.opacity * 100f,
                    min = 0f,
                    max = 100f,
                    onPreview = { onUpdate("Vector opacity", layer.copy(opacity = it / 100f)) },
                    onCommit = { onUpdate("Vector opacity", layer.copy(opacity = it / 100f)) },
                    step = 1f,
                    unit = "%",
                    defaultValue = 100f
                )
            }
        }
    }
}
