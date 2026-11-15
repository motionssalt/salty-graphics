package com.motionssalt.saltygraphics.ui.editor

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.MaskMode
import com.motionssalt.saltygraphics.engine.model.MaskType
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import com.motionssalt.saltygraphics.uikit.controls.SaltyBlendAndMaskPanel
import com.motionssalt.saltygraphics.uikit.controls.SaltyBlendThumbnailCache

object BlendThumbnailCache {
    fun getThumbnail(mode: com.motionssalt.saltygraphics.engine.model.BlendMode): Bitmap =
        SaltyBlendThumbnailCache.getThumbnail(mode)
}

@Composable
fun BlendingPage(
    currentBlendMode: com.motionssalt.saltygraphics.engine.model.BlendMode,
    currentOpacity: Float,
    currentMaskType: MaskType,
    currentMaskSourceLayerId: String?,
    isMaskInverted: Boolean,
    currentMaskMode: MaskMode = MaskMode.NONE,
    allAvailableLayers: List<Layer>,
    onBlendChange: (com.motionssalt.saltygraphics.engine.model.BlendMode, Float) -> Unit,
    onMaskChange: (MaskType, String?, Boolean, MaskMode) -> Unit,
    onBack: () -> Unit
) {
    val activeSourceId = currentMaskSourceLayerId ?: allAvailableLayers.firstOrNull()?.id
    val selectedSourceLayer = remember(activeSourceId, allAvailableLayers) {
        allAvailableLayers.find { it.id == activeSourceId }
    }
    val maskPreview = remember(selectedSourceLayer, currentMaskType, isMaskInverted) {
        if (selectedSourceLayer != null && currentMaskType != MaskType.NONE) {
            val bmp = LayerCompositor.renderLayerContent(selectedSourceLayer, 64, 64)
            if (isMaskInverted) {
                val px = IntArray(64 * 64)
                bmp.getPixels(px, 0, 64, 0, 0, 64, 64)
                for (i in px.indices) {
                    val p = px[i]
                    val a = 255 - ((p ushr 24) and 0xFF)
                    px[i] = (a shl 24) or (p and 0x00FFFFFF)
                }
                bmp.setPixels(px, 0, 64, 0, 0, 64, 64)
            }
            bmp
        } else null
    }

    SaltyBlendAndMaskPanel(
        currentBlendMode = currentBlendMode,
        currentOpacity = currentOpacity,
        currentMaskType = currentMaskType,
        currentMaskSourceLayerId = currentMaskSourceLayerId,
        isMaskInverted = isMaskInverted,
        currentMaskMode = currentMaskMode,
        allAvailableLayers = allAvailableLayers,
        maskPreviewBitmap = maskPreview,
        onBlendChange = onBlendChange,
        onMaskChange = onMaskChange,
        onBack = onBack
    )
}
