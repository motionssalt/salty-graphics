package com.motionssalt.saltygraphics.ui.controls

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.model.FillData

/** Pure binding facade over :ui-kit SaltyFillPanelPage. */
@Composable
fun FillPanelPage(
    fill: FillData,
    media: List<Pair<String, Bitmap?>>,
    onPreview: (FillData) -> Unit,
    onCommit: (String, FillData) -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit
) {
    SaltyFillPanelPage(
        fill = fill,
        media = media,
        onPreview = onPreview,
        onCommit = onCommit,
        onImport = onImport,
        onBack = onBack
    )
}
