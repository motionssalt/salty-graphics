package com.motionssalt.saltygraphics.uikit.sublibs.text

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.TextBoxMode
import com.motionssalt.saltygraphics.engine.model.TextJustification
import com.motionssalt.saltygraphics.engine.model.TextLayerData
import com.motionssalt.saltygraphics.uikit.controls.DropdownOption
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorPickerPage
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorSwatchRow
import com.motionssalt.saltygraphics.uikit.controls.SaltyInlineDropdown
import com.motionssalt.saltygraphics.uikit.controls.SaltyNumberControl
import com.motionssalt.saltygraphics.uikit.controls.SaltyRecentColorsRegistry
import com.motionssalt.saltygraphics.uikit.controls.SaltySegmentedChips
import com.motionssalt.saltygraphics.uikit.controls.SaltySwitch
import com.motionssalt.saltygraphics.uikit.controls.SaltyTextField
import com.motionssalt.saltygraphics.uikit.structure.SaltyBadge
import com.motionssalt.saltygraphics.uikit.structure.SaltyButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyCard
import com.motionssalt.saltygraphics.uikit.structure.SaltyChip
import com.motionssalt.saltygraphics.uikit.structure.SaltyColorSwatch
import com.motionssalt.saltygraphics.uikit.structure.SaltyColumn
import com.motionssalt.saltygraphics.uikit.structure.SaltyDivider
import com.motionssalt.saltygraphics.uikit.structure.SaltyIcon
import com.motionssalt.saltygraphics.uikit.structure.SaltyIconButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyIconSize
import com.motionssalt.saltygraphics.uikit.structure.SaltyJustify
import com.motionssalt.saltygraphics.uikit.structure.SaltyLazyColumn
import com.motionssalt.saltygraphics.uikit.structure.SaltyPanelPage
import com.motionssalt.saltygraphics.uikit.structure.SaltyRow
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltySpacer
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextStyle
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextTone
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextWeight
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxSize
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxWidth
import com.motionssalt.saltygraphics.uikit.structure.saltyListItems

data class SaltyFontEntry(
    val id: String,
    val displayName: String,
    val familyName: String,
    val styleName: String,
    val isBuiltIn: Boolean,
    val isImported: Boolean,
    val fontFamily: FontFamily = FontFamily.Default
)

@Composable
fun SaltyTextOptionsPage(
    initialData: TextLayerData,
    candidatePathLayers: List<Layer>,
    onSave: (TextLayerData) -> Unit,
    onDismiss: () -> Unit,
    fontSelectionContent: @Composable (currentFontFamily: String, onBack: () -> Unit) -> Unit
) {
    var textData by remember(initialData) { mutableStateOf(initialData) }
    LaunchedEffect(initialData) { textData = initialData }
    var currentSubPage by remember { mutableStateOf("main") }

    fun updateData(newData: TextLayerData) {
        textData = newData
        onSave(newData)
    }

    when (currentSubPage) {
        "text_edit" -> {
            var tempText by remember { mutableStateOf(textData.text) }
            SaltyPanelPage(title = "Edit Text", onBack = { currentSubPage = "main" }) {
                SaltyColumn(
                    modifier = Modifier.saltyFillMaxWidth(),
                    padding = SaltySpace.XS,
                    spacing = SaltySpace.MD
                ) {
                    SaltyTextField(
                        value = tempText,
                        onValueChange = { tempText = it },
                        singleLine = false,
                        minLines = 4,
                        maxLines = 10
                    )
                    SaltyRow(
                        modifier = Modifier.saltyFillMaxWidth(),
                        justify = SaltyJustify.END,
                        spacing = SaltySpace.SM,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SaltyTextButton(
                            text = "Cancel",
                            tone = SaltyTextTone.SECONDARY,
                            onClick = { currentSubPage = "main" }
                        )
                        SaltyButton(
                            text = "Apply",
                            onClick = {
                                updateData(textData.copy(text = tempText))
                                currentSubPage = "main"
                            }
                        )
                    }
                }
            }
            return
        }
        "fonts" -> {
            fontSelectionContent(textData.fontFamily) { currentSubPage = "main" }
            return
        }
        "text_color" -> {
            SaltyColorPickerPage(
                title = "Text Color",
                colorArgb = textData.color,
                onPreview = { updateData(textData.copy(color = it)) },
                onCommit = {
                    SaltyRecentColorsRegistry.recordColor(it)
                    updateData(textData.copy(color = it))
                    currentSubPage = "main"
                },
                onBack = { currentSubPage = "main" },
                supportsTransparent = false,
                applyLabel = "Apply",
                presets = SaltyRecentColorsRegistry.recentColors
            )
            return
        }
        "outline_color" -> {
            SaltyColorPickerPage(
                title = "Outline Color",
                colorArgb = textData.outlineColor,
                onPreview = { updateData(textData.copy(outlineColor = it)) },
                onCommit = {
                    SaltyRecentColorsRegistry.recordColor(it)
                    updateData(textData.copy(outlineColor = it))
                    currentSubPage = "main"
                },
                onBack = { currentSubPage = "main" },
                supportsTransparent = false,
                applyLabel = "Apply",
                presets = SaltyRecentColorsRegistry.recentColors
            )
            return
        }
    }

    SaltyPanelPage(
        title = "Text Options",
        onBack = onDismiss
    ) {
        SaltyColumn(
            modifier = Modifier.saltyFillMaxWidth(),
            spacing = SaltySpace.MD
        ) {
            SaltyCard(
                padding = SaltySpace.MD,
                spacing = SaltySpace.XXS,
                onClick = { currentSubPage = "text_edit" }
            ) {
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    justify = SaltyJustify.SPACE_BETWEEN,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaltyColumn(modifier = Modifier.weight(1f), spacing = SaltySpace.XXS) {
                        SaltyText(
                            text = "Text Content",
                            style = SaltyTextStyle.LABEL_SMALL,
                            tone = SaltyTextTone.SECONDARY
                        )
                        SaltyText(
                            text = textData.text.ifBlank { "(Empty text)" },
                            style = SaltyTextStyle.BODY_LARGE,
                            maxLines = 2
                        )
                    }
                    SaltyIcon(
                        icon = Icons.Default.Edit,
                        contentDescription = "Edit Text",
                        tone = SaltyTextTone.ACCENT,
                        size = SaltyIconSize.MD
                    )
                }
            }

            SaltyCard(
                padding = SaltySpace.MD,
                spacing = SaltySpace.XXS,
                onClick = { currentSubPage = "fonts" }
            ) {
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    justify = SaltyJustify.SPACE_BETWEEN,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaltyColumn(spacing = SaltySpace.XXS) {
                        SaltyText(
                            text = "Font Family",
                            style = SaltyTextStyle.LABEL_SMALL,
                            tone = SaltyTextTone.SECONDARY
                        )
                        SaltyText(
                            text = "${textData.fontFamily} (${textData.fontStyleName})",
                            style = SaltyTextStyle.TITLE_MEDIUM,
                            weight = SaltyTextWeight.BOLD,
                            tone = SaltyTextTone.ACCENT
                        )
                    }
                    SaltyIcon(
                        icon = Icons.Default.ChevronRight,
                        contentDescription = "Select Font",
                        tone = SaltyTextTone.SECONDARY
                    )
                }
            }

            SaltyNumberControl(
                label = "Size",
                value = textData.fontSize,
                min = 8f,
                max = 400f,
                defaultValue = 64f,
                step = 1f,
                unit = "px",
                onPreview = { updateData(textData.copy(fontSize = it)) },
                onCommit = { updateData(textData.copy(fontSize = it)) }
            )

            SaltyColorSwatchRow(
                label = "Color",
                colorArgb = textData.color,
                onClick = { currentSubPage = "text_color" }
            )

            SaltySegmentedChips(
                label = "Alignment",
                options = listOf("Left", "Center", "Right", "Justify"),
                selectedOption = when (textData.justification) {
                    TextJustification.LEFT -> "Left"
                    TextJustification.CENTER -> "Center"
                    TextJustification.RIGHT -> "Right"
                    TextJustification.JUSTIFY -> "Justify"
                },
                onOptionSelected = { option ->
                    val j = when (option) {
                        "Left" -> TextJustification.LEFT
                        "Center" -> TextJustification.CENTER
                        "Right" -> TextJustification.RIGHT
                        else -> TextJustification.JUSTIFY
                    }
                    updateData(textData.copy(justification = j))
                }
            )

            SaltySegmentedChips(
                label = "Box Framing",
                options = listOf("Free", "Paragraph", "Fixed", "Shrink fit"),
                selectedOption = when (textData.boxMode) {
                    TextBoxMode.FREE -> "Free"
                    TextBoxMode.PARAGRAPH -> "Paragraph"
                    TextBoxMode.FIXED_SIZE -> "Fixed"
                    TextBoxMode.SHRINK_TO_FIT -> "Shrink fit"
                },
                onOptionSelected = { opt ->
                    val mode = when (opt) {
                        "Paragraph" -> TextBoxMode.PARAGRAPH
                        "Fixed" -> TextBoxMode.FIXED_SIZE
                        "Shrink fit" -> TextBoxMode.SHRINK_TO_FIT
                        else -> TextBoxMode.FREE
                    }
                    updateData(textData.copy(boxMode = mode))
                }
            )

            SaltyNumberControl(
                label = "Line spacing",
                value = textData.lineSpacing,
                min = 50f,
                max = 300f,
                defaultValue = 120f,
                step = 1f,
                unit = "%",
                onPreview = { updateData(textData.copy(lineSpacing = it)) },
                onCommit = { updateData(textData.copy(lineSpacing = it)) }
            )

            SaltyRow(
                modifier = Modifier.saltyFillMaxWidth(),
                justify = SaltyJustify.SPACE_BETWEEN,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SaltyNumberControl(
                    label = "Letter spacing",
                    value = textData.letterSpacing,
                    min = -100f,
                    max = 800f,
                    defaultValue = 0f,
                    step = 5f,
                    unit = "‰ em",
                    onPreview = { updateData(textData.copy(letterSpacing = it)) },
                    onCommit = { updateData(textData.copy(letterSpacing = it)) },
                    modifier = Modifier.weight(1f)
                )
                if (textData.letterSpacing != 0f) {
                    SaltyIconButton(
                        icon = Icons.Default.RestartAlt,
                        contentDescription = "Reset letter spacing",
                        tone = SaltyTextTone.ACCENT,
                        compact = true,
                        onClick = { updateData(textData.copy(letterSpacing = 0f)) }
                    )
                }
            }

            SaltyDivider()
            SaltyRow(
                modifier = Modifier.saltyFillMaxWidth(),
                justify = SaltyJustify.SPACE_BETWEEN,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SaltyText(text = "Outline Stroke", style = SaltyTextStyle.TITLE_SMALL)
                SaltyRow(spacing = SaltySpace.SM, verticalAlignment = Alignment.CenterVertically) {
                    if (textData.hasOutline) {
                        SaltyColorSwatch(
                            colorArgb = textData.outlineColor,
                            circular = true,
                            onClick = { currentSubPage = "outline_color" }
                        )
                    }
                    SaltySwitch(
                        checked = textData.hasOutline,
                        onCheckedChange = { updateData(textData.copy(hasOutline = it)) }
                    )
                }
            }
            if (textData.hasOutline) {
                SaltyNumberControl(
                    label = "Outline Width",
                    value = textData.outlineWidth,
                    min = 1f,
                    max = 40f,
                    defaultValue = 2f,
                    step = 1f,
                    unit = "px",
                    onPreview = { updateData(textData.copy(outlineWidth = it)) },
                    onCommit = { updateData(textData.copy(outlineWidth = it)) }
                )
            }

            SaltyDivider()
            SaltyText(
                text = "Move Along Path",
                style = SaltyTextStyle.TITLE_SMALL,
                tone = SaltyTextTone.ACCENT
            )
            val pathOptions = listOf("None (Flat text)") + candidatePathLayers.map { it.name }
            val selectedPathIndex = candidatePathLayers.indexOfFirst { it.id == textData.followPathLayerId } + 1
            SaltyInlineDropdown(
                label = "Path layer",
                selected = pathOptions[selectedPathIndex],
                options = pathOptions.map { DropdownOption(it, it) },
                onSelect = { name ->
                    updateData(textData.copy(followPathLayerId = candidatePathLayers.getOrNull(pathOptions.indexOf(name) - 1)?.id))
                }
            )
            if (textData.followPathLayerId != null) {
                SaltyNumberControl(
                    label = "Path Offset",
                    value = textData.pathOffsetPercent * 100f,
                    min = 0f,
                    max = 100f,
                    defaultValue = 0f,
                    step = 1f,
                    unit = "%",
                    onPreview = { updateData(textData.copy(pathOffsetPercent = (it / 100f).coerceIn(0f, 1f))) },
                    onCommit = { updateData(textData.copy(pathOffsetPercent = (it / 100f).coerceIn(0f, 1f))) }
                )
            }

            SaltySpacer(SaltySpace.LG)
        }
    }
}

@Composable
fun SaltyFontSelectionPage(
    currentFontFamily: String,
    fonts: List<SaltyFontEntry>,
    onSelectFont: (String) -> Unit,
    onDeleteFont: (String) -> Unit,
    onImportFont: () -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStyleFilter by remember { mutableStateOf<String?>(null) }

    val filteredFonts = remember(fonts, searchQuery, selectedStyleFilter) {
        fonts.filter { font ->
            val matchQuery = searchQuery.isBlank() ||
                font.displayName.contains(searchQuery, ignoreCase = true) ||
                font.familyName.contains(searchQuery, ignoreCase = true)
            val matchStyle = selectedStyleFilter == null ||
                font.styleName.equals(selectedStyleFilter, ignoreCase = true)
            matchQuery && matchStyle
        }
    }

    SaltyPanelPage(title = "Fonts", onBack = onBack, scrollable = false) {
        SaltyColumn(
            modifier = Modifier.saltyFillMaxSize(),
            padding = SaltySpace.XS,
            spacing = SaltySpace.SM
        ) {
            SaltyRow(
                modifier = Modifier.saltyFillMaxWidth(),
                spacing = SaltySpace.SM,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SaltyTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search fonts...",
                    leadingIcon = { SaltyIcon(Icons.Default.Search, tone = SaltyTextTone.ACCENT, size = SaltyIconSize.MD) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            SaltyIconButton(
                                icon = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tone = SaltyTextTone.SECONDARY,
                                compact = true,
                                onClick = { searchQuery = "" }
                            )
                        }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                SaltyButton(
                    text = "Import",
                    icon = Icons.Default.Add,
                    onClick = onImportFont
                )
            }

            SaltyRow(
                modifier = Modifier.saltyFillMaxWidth(),
                spacing = SaltySpace.XS,
                scrollable = true
            ) {
                val styles = listOf("All", "Regular", "Bold", "Italic", "Condensed")
                styles.forEach { style ->
                    val isSelected = (style == "All" && selectedStyleFilter == null) || (selectedStyleFilter == style)
                    SaltyChip(
                        label = style,
                        selected = isSelected,
                        onClick = { selectedStyleFilter = if (style == "All") null else style }
                    )
                }
            }

            SaltyLazyColumn(
                modifier = Modifier.saltyFillMaxSize().weight(1f),
                spacing = SaltySpace.XS
            ) {
                saltyListItems(filteredFonts, key = { it.id }) { font ->
                    val isSelected = font.familyName.equals(currentFontFamily, ignoreCase = true)
                    SaltyCard(
                        isSelected = isSelected,
                        padding = SaltySpace.SM,
                        spacing = SaltySpace.XXS,
                        onClick = { onSelectFont(font.id) }
                    ) {
                        SaltyRow(
                            modifier = Modifier.saltyFillMaxWidth(),
                            justify = SaltyJustify.SPACE_BETWEEN,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SaltyColumn(modifier = Modifier.weight(1f), spacing = SaltySpace.XXS) {
                                SaltyText(
                                    text = font.displayName,
                                    fontFamily = font.fontFamily,
                                    style = SaltyTextStyle.BODY_LARGE,
                                    weight = if (isSelected) SaltyTextWeight.BOLD else SaltyTextWeight.NORMAL,
                                    tone = if (isSelected) SaltyTextTone.ACCENT else SaltyTextTone.PRIMARY
                                )
                                SaltyText(
                                    text = "${font.familyName} • ${font.styleName}",
                                    style = SaltyTextStyle.LABEL_SMALL,
                                    tone = SaltyTextTone.SECONDARY
                                )
                            }
                            SaltyRow(spacing = SaltySpace.XS, verticalAlignment = Alignment.CenterVertically) {
                                if (font.isBuiltIn) {
                                    SaltyBadge(text = "built-in", tone = SaltyTextTone.SECONDARY)
                                } else if (font.isImported) {
                                    SaltyBadge(text = "imported", tone = SaltyTextTone.ACCENT)
                                    SaltyIconButton(
                                        icon = Icons.Default.Delete,
                                        contentDescription = "Delete font",
                                        tone = SaltyTextTone.ERROR,
                                        compact = true,
                                        onClick = { onDeleteFont(font.id) }
                                    )
                                }
                                if (isSelected) {
                                    SaltyIcon(
                                        icon = Icons.Default.Check,
                                        tone = SaltyTextTone.ACCENT,
                                        size = SaltyIconSize.MD
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
