package com.motionssalt.saltygraphics.ui.editor

import com.motionssalt.saltygraphics.uikit.structure.SaltyToastManager
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.motionssalt.saltygraphics.data.FontItem
import com.motionssalt.saltygraphics.data.FontManager
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.TextLayerData
import com.motionssalt.saltygraphics.uikit.sublibs.text.SaltyFontEntry
import com.motionssalt.saltygraphics.uikit.sublibs.text.SaltyFontSelectionPage
import com.motionssalt.saltygraphics.uikit.sublibs.text.SaltyTextOptionsPage

@Composable
fun TextSettingsDialog(
    initialData: TextLayerData,
    candidatePathLayers: List<Layer>,
    onSave: (TextLayerData) -> Unit,
    onDismiss: () -> Unit
) {
    var textData by remember(initialData) { mutableStateOf(initialData) }
    LaunchedEffect(initialData) { textData = initialData }

    SaltyTextOptionsPage(
        initialData = textData,
        candidatePathLayers = candidatePathLayers,
        onSave = { updated ->
            textData = updated
            onSave(updated)
        },
        onDismiss = onDismiss,
        fontSelectionContent = { currentFontFamily, onBack ->
            FontSelectionPage(
                currentFontFamily = currentFontFamily,
                currentStyleName = textData.fontStyleName,
                onSelectFont = { fontItem ->
                    val updated = textData.copy(
                        fontFamily = fontItem.familyName,
                        fontStyleName = fontItem.styleName,
                        customFontAssetPath = fontItem.assetPath ?: fontItem.filePath
                    )
                    textData = updated
                    onSave(updated)
                },
                onBack = onBack
            )
        }
    )
}

@Composable
fun FontSelectionPage(
    currentFontFamily: String,
    currentStyleName: String,
    onSelectFont: (FontItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var fontList by remember { mutableStateOf(FontManager.getAllFonts(context)) }

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
            SaltyToastManager.show(msg)
        }
    }

    val entries = remember(fontList) {
        fontList.map { font ->
            val tf = try {
                FontManager.getTypeface(context, font)
            } catch (_: Throwable) {
                Typeface.DEFAULT
            }
            val ff = try {
                FontFamily(tf)
            } catch (_: Throwable) {
                FontFamily.Default
            }
            SaltyFontEntry(
                id = font.id,
                displayName = font.displayName,
                familyName = font.familyName,
                styleName = font.styleName,
                isBuiltIn = font.isBuiltIn,
                isImported = font.isImported,
                fontFamily = ff
            )
        }
    }

    SaltyFontSelectionPage(
        currentFontFamily = currentFontFamily,
        fonts = entries,
        onSelectFont = { id -> fontList.find { it.id == id }?.let(onSelectFont) },
        onDeleteFont = { id ->
            FontManager.deleteImportedFont(context, id)
            fontList = FontManager.getAllFonts(context)
        },
        onImportFont = { fontPickerLauncher.launch(arrayOf("*/*", "font/ttf", "font/otf")) },
        onBack = onBack
    )
}
