package com.motionssalt.saltygraphics.ui.editor

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.data.FontItem
import com.motionssalt.saltygraphics.data.FontManager
import com.motionssalt.saltygraphics.data.FontOrigin
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.theme.*
import com.motionssalt.saltygraphics.ui.controls.ColorPickerDialog
import com.motionssalt.saltygraphics.ui.controls.RulerSlider
import com.motionssalt.saltygraphics.ui.controls.SegmentedChipsControl
import com.motionssalt.saltygraphics.ui.controls.ValueEditorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSettingsDialog(
    initialData: TextLayerData,
    candidatePathLayers: List<Layer>,
    onSave: (TextLayerData) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var textData by remember { mutableStateOf(initialData) }

    // Sub-pages inside bottom panel: "main" (Text Options) or "fonts" (Font Page)
    var currentSubPage by remember { mutableStateOf("main") }

    // Dialog states for modal pickers (R4 color picker & R5 value/text editor)
    var isEditingContentText by remember { mutableStateOf(false) }
    var isColorPickerOpen by remember { mutableStateOf(false) }
    var isOutlineColorPickerOpen by remember { mutableStateOf(false) }

    // Helper to update state and trigger live canvas update
    fun updateData(newData: TextLayerData) {
        textData = newData
        onSave(newData)
    }

    if (currentSubPage == "fonts") {
        FontSelectionPage(
            currentFontFamily = textData.fontFamily,
            currentStyleName = textData.fontStyleName,
            onSelectFont = { fontItem ->
                updateData(textData.copy(
                    fontFamily = fontItem.familyName,
                    fontStyleName = fontItem.styleName,
                    customFontAssetPath = fontItem.assetPath ?: fontItem.filePath
                ))
                currentSubPage = "main"
            },
            onBack = { currentSubPage = "main" }
        )
        return
    }

    PanelPage(
        title = "Text Options",
        onBack = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Text input preview box (tap to edit text)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceVariant,
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.outline)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditingContentText = true }
                    .padding(vertical = 12.dp, horizontal = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Text Content (tap to edit)",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = textData.text.ifEmpty { "(Empty text)" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                            maxLines = 2
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Text",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Font Row
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceVariant,
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.outline)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currentSubPage = "fonts" }
                    .padding(vertical = 12.dp, horizontal = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Font Family", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        Text(
                            text = "${textData.fontFamily} (${textData.fontStyleName})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Select Font",
                        tint = colors.onSurfaceVariant
                    )
                }
            }

            // 3. Size: Ruler slider (px)
            RulerSlider(
                label = "Size",
                value = textData.fontSize,
                onValueChange = { updateData(textData.copy(fontSize = it)) },
                minValue = 8f,
                maxValue = 400f,
                defaultValue = 64f,
                step = 1f,
                unit = "px"
            )

            // 4. Color Swatch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Color", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(safeColor(textData.color))
                        .border(1.5.dp, colors.outline, CircleShape)
                        .clickable { isColorPickerOpen = true }
                )
            }

            // 5. Alignment Segmented Chips (Left, Center, Right, Justify)
            // 5. Alignment Segmented Chips (Left, Center, Right, Justify)
            SegmentedChipsControl(
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

            // 6. Box Mode Chips: Free, Paragraph, Fixed size, Shrink to fit
            SegmentedChipsControl(
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

            // 7. Line spacing (Ruler slider in %, default 120%)
            RulerSlider(
                label = "Line spacing",
                value = textData.lineSpacing,
                onValueChange = { updateData(textData.copy(lineSpacing = it)) },
                minValue = 50f,
                maxValue = 300f,
                defaultValue = 120f,
                step = 1f,
                unit = "%"
            )

            // 8. Letter spacing (Ruler slider in ‰ em with reset button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    RulerSlider(
                        label = "Letter spacing",
                        value = textData.letterSpacing,
                        onValueChange = { updateData(textData.copy(letterSpacing = it)) },
                        minValue = -100f,
                        maxValue = 800f,
                        defaultValue = 0f,
                        step = 5f,
                        unit = "‰ em"
                    )
                }
                if (textData.letterSpacing != 0f) {
                    IconButton(
                        onClick = { updateData(textData.copy(letterSpacing = 0f)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset letter spacing", tint = colors.primary)
                    }
                }
            }

            // 9. Outline Section (swatch + ruler slider px)
            HorizontalDivider(color = colors.outline.copy(alpha = 0.5f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = textData.hasOutline,
                        onCheckedChange = { updateData(textData.copy(hasOutline = it)) }
                    )
                    Text("Outline Stroke", style = MaterialTheme.typography.titleSmall)
                }
                if (textData.hasOutline) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(safeColor(textData.outlineColor))
                            .border(1.dp, colors.outline, CircleShape)
                            .clickable { isOutlineColorPickerOpen = true }
                    )
                }
            }

            if (textData.hasOutline) {
                RulerSlider(
                    label = "Outline Width",
                    value = textData.outlineWidth,
                    onValueChange = { updateData(textData.copy(outlineWidth = it)) },
                    minValue = 1f,
                    maxValue = 40f,
                    defaultValue = 2f,
                    step = 1f,
                    unit = "px"
                )
            }

            // 10. Move Along Path
            HorizontalDivider(color = colors.outline.copy(alpha = 0.5f))
            Text(
                "Move Along Path",
                style = MaterialTheme.typography.titleSmall,
                color = colors.primary
            )

            val pathOptions = listOf("None (Flat text)") + candidatePathLayers.map { it.name }
            val selectedPathIndex = candidatePathLayers.indexOfFirst { it.id == textData.followPathLayerId } + 1
            com.motionssalt.saltygraphics.ui.controls.DropdownControl(
                label = "Path layer", options = pathOptions, selectedOption = pathOptions[selectedPathIndex],
                onOptionSelected = { name -> updateData(textData.copy(followPathLayerId = candidatePathLayers.getOrNull(pathOptions.indexOf(name) - 1)?.id)) }
            )

            if (textData.followPathLayerId != null) {
                RulerSlider(
                    label = "Path Offset",
                    value = (textData.pathOffsetPercent * 100f),
                    onValueChange = { updateData(textData.copy(pathOffsetPercent = (it / 100f).coerceIn(0f, 1f))) },
                    minValue = 0f,
                    maxValue = 100f,
                    defaultValue = 0f,
                    step = 1f,
                    unit = "%"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Color Pickers
    if (isColorPickerOpen) {
        ColorPickerDialog(
            title = "Text Color",
            initialColor = textData.color,
            allowTransparent = false,
            onApply = {
                updateData(textData.copy(color = it))
                isColorPickerOpen = false
            },
            onDismiss = { isColorPickerOpen = false }
        )
    }

    if (isOutlineColorPickerOpen) {
        ColorPickerDialog(
            title = "Outline Color",
            initialColor = textData.outlineColor,
            allowTransparent = false,
            onApply = {
                updateData(textData.copy(outlineColor = it))
                isOutlineColorPickerOpen = false
            },
            onDismiss = { isOutlineColorPickerOpen = false }
        )
    }

    // Text Editor Dialog (Floating above keyboard per R5 rules)
    if (isEditingContentText) {
        var tempText by remember { mutableStateOf(textData.text) }
        AlertDialog(
            onDismissRequest = { isEditingContentText = false },
            title = { Text("Edit Text", fontWeight = FontWeight.Bold, color = colors.primary) },
            text = {
                OutlinedTextField(
                    value = tempText,
                    onValueChange = { tempText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 220.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        updateData(textData.copy(text = tempText))
                        isEditingContentText = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingContentText = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FontSelectionPage(
    currentFontFamily: String,
    currentStyleName: String,
    onSelectFont: (FontItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    var selectedStyleFilter by remember { mutableStateOf<String?>(null) }

    var fontList by remember { mutableStateOf(FontManager.getAllFonts(context)) }

    // Multi-font picker launcher per R12
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val summary = FontManager.importFontsFromUris(context, uris)
            fontList = FontManager.getAllFonts(context)
            val msg = if (summary.skippedCount > 0) {
                "Imported ${summary.importedCount} font(s). Skipped ${summary.skippedCount}."
            } else {
                "Successfully imported ${summary.importedCount} font(s)."
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val filteredFonts = remember(fontList, searchQuery, selectedStyleFilter) {
        fontList.filter { font ->
            val matchQuery = searchQuery.isBlank() || font.displayName.contains(searchQuery, ignoreCase = true) || font.familyName.contains(searchQuery, ignoreCase = true)
            val matchStyle = selectedStyleFilter == null || font.styleName.equals(selectedStyleFilter, ignoreCase = true)
            matchQuery && matchStyle
        }
    }

    PanelPage(title = "Fonts", onBack = onBack, scrollable = false) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Bar & Import Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search fonts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary)
                )

                Button(
                    onClick = {
                        fontPickerLauncher.launch(arrayOf("*/*", "font/ttf", "font/otf"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import", fontWeight = FontWeight.Bold)
                }
            }

            // Style Chips (Regular, Bold, Italic, Condensed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val styles = listOf("All", "Regular", "Bold", "Italic", "Condensed")
                styles.forEach { style ->
                    val isSelected = (style == "All" && selectedStyleFilter == null) || (selectedStyleFilter == style)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedStyleFilter = if (style == "All") null else style },
                        label = { Text(style, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Font List
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredFonts, key = { it.id }) { font ->
                    val isSelected = font.familyName.equals(currentFontFamily, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surface,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) colors.primary else colors.outline.copy(alpha = 0.4f))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFont(font) }
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = font.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.primary else colors.onSurface
                                )
                                Text(
                                    text = "${font.familyName} • ${font.styleName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Badge: built-in or imported
                                if (font.isBuiltIn) {
                                    Surface(
                                        color = colors.secondary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "built-in",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.secondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (font.isImported) {
                                    Surface(
                                        color = colors.tertiary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "imported",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.tertiary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            FontManager.deleteImportedFont(context, font.id)
                                            fontList = FontManager.getAllFonts(context)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete font", tint = colors.error, modifier = Modifier.size(16.dp))
                                    }
                                }

                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
