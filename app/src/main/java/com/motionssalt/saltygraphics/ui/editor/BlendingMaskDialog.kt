package com.motionssalt.saltygraphics.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import com.motionssalt.saltygraphics.ui.controls.ProgressSliderControl
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.engine.model.BlendCategory
import com.motionssalt.saltygraphics.engine.model.BlendMode
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.MaskType
import com.motionssalt.saltygraphics.engine.render.BlendMath
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import com.motionssalt.saltygraphics.theme.*
import com.motionssalt.saltygraphics.ui.controls.SaltySwitch

object BlendThumbnailCache {
    private val cache = LruCache<BlendMode, Bitmap>(30)

    fun getThumbnail(mode: BlendMode): Bitmap {
        cache.get(mode)?.let { return it }

        // Render representative sample: gradient base + tinted circle blend layer
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        // Base gradient colors (Blue to Orange)
        val topColor = 0xFF3366FFL
        val bottomColor = 0xFFFF6633L

        for (y in 0 until size) {
            val ratio = y.toFloat() / size
            val baseR = (((topColor shr 16 and 0xFF) * (1f - ratio) + (bottomColor shr 16 and 0xFF) * ratio)).toInt()
            val baseG = (((topColor shr 8 and 0xFF) * (1f - ratio) + (bottomColor shr 8 and 0xFF) * ratio)).toInt()
            val baseB = (((topColor and 0xFF) * (1f - ratio) + (bottomColor and 0xFF) * ratio)).toInt()
            val baseArgb = (0xFFL shl 24) or (baseR.toLong() shl 16) or (baseG.toLong() shl 8) or baseB.toLong()

            for (x in 0 until size) {
                // Circle blend shape on top right
                val dx = x - size * 0.5f
                val dy = y - size * 0.5f
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                val pixelColor = if (dist <= size * 0.4f) {
                    val srcArgb = 0xFFFFFF00L // Bright Yellow circle
                    BlendMath.blendColor(mode, baseArgb, srcArgb, opacity = 0.85f).toInt()
                } else {
                    baseArgb.toInt()
                }
                bitmap.setPixel(x, y, pixelColor)
            }
        }

        cache.put(mode, bitmap)
        return bitmap
    }
}

@Composable
fun BlendingPage(
    currentBlendMode: BlendMode,
    currentOpacity: Float,
    currentMaskType: MaskType,
    currentMaskSourceLayerId: String?,
    isMaskInverted: Boolean,
    currentMaskMode: com.motionssalt.saltygraphics.engine.model.MaskMode = com.motionssalt.saltygraphics.engine.model.MaskMode.NONE,
    allAvailableLayers: List<Layer>,
    onBlendChange: (BlendMode, Float) -> Unit,
    onMaskChange: (MaskType, String?, Boolean, com.motionssalt.saltygraphics.engine.model.MaskMode) -> Unit,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var selectedCategory by remember { mutableStateOf<BlendCategory?>(null) }
    var activeBlendMode by remember(currentBlendMode) { mutableStateOf(currentBlendMode) }
    var activeOpacity by remember(currentOpacity) { mutableFloatStateOf(currentOpacity) }
    var activeMaskType by remember(currentMaskType) { mutableStateOf(currentMaskType) }
    var activeSourceId by remember(currentMaskSourceLayerId) {
        mutableStateOf(currentMaskSourceLayerId ?: allAvailableLayers.firstOrNull()?.id)
    }
    var activeInverted by remember(isMaskInverted) { mutableStateOf(isMaskInverted) }
    var activeCutoutMode by remember(currentMaskMode) { mutableStateOf(currentMaskMode) }
    var isSourceDropdownOpen by remember { mutableStateOf(false) }

    val displayedModes = remember(selectedCategory) {
        if (selectedCategory == null) BlendMode.entries
        else BlendMode.byCategory(selectedCategory!!)
    }

    val selectedSourceLayer = remember(activeSourceId, allAvailableLayers) {
        allAvailableLayers.find { it.id == activeSourceId }
    }

    // Mask preview thumbnail
    val maskPreview = remember(selectedSourceLayer, activeMaskType, activeInverted) {
        if (selectedSourceLayer != null && activeMaskType != MaskType.NONE) {
            val bmp = LayerCompositor.renderLayerContent(selectedSourceLayer, 64, 64)
            if (activeInverted) {
                // Invert preview pixels
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

    PanelPage(title = "Blending & Mask", onBack = onBack, scrollable = true) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Opacity Slider
            Column {
                ProgressSliderControl(
                    label = "Opacity",
                    value = activeOpacity,
                    onValueChange = {
                        activeOpacity = it
                        onBlendChange(activeBlendMode, it)
                    },
                    minValue = 0f,
                    maxValue = 1f,
                    defaultValue = 1f,
                    step = 0.01f,
                    unit = "%"
                )
            }

            HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))

            // 2. Blend Mode Categories (Tabs)
            Text("Blend Mode Categories", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val categories = listOf(null) + BlendCategory.entries
                items(categories) { cat ->
                    val isCatSelected = selectedCategory == cat
                    val catName = cat?.displayName ?: "All"
                    FilterChip(
                        selected = isCatSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(catName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // 3. Grid of Blend Modes with Live Thumbnails
            LazyVerticalGrid(
                columns = GridCells.Adaptive(85.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedModes, key = { it.name }) { mode ->
                    val isSelected = activeBlendMode == mode
                    val thumb = remember(mode) { BlendThumbnailCache.getThumbnail(mode) }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surface,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) colors.primary else colors.outline.copy(alpha = 0.3f))
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                activeBlendMode = mode
                                onBlendChange(mode, activeOpacity)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = mode.displayName,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isSelected) colors.primary else colors.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))

            // 4. Non-Destructive Masking Section (T10 & T11)
            Text("Layer Masking", style = MaterialTheme.typography.titleSmall, color = colors.primary)
            Text(
                "Non-destructive compositing mask applied while blend mode is active.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            // Mask Type Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MaskType.entries.forEach { type ->
                    val isSelected = activeMaskType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            activeMaskType = type
                            onMaskChange(type, activeSourceId, activeInverted, activeCutoutMode)
                        },
                        label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Alpha Cutout Mode Chips (V23)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Alpha Cutout (Punch-Through)", style = MaterialTheme.typography.titleSmall, color = colors.primary)
                Text(
                    "Punches through layers beneath. 'Inward' cuts out the shape; 'Outward' keeps only inside the shape.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        com.motionssalt.saltygraphics.engine.model.MaskMode.NONE to "Off",
                        com.motionssalt.saltygraphics.engine.model.MaskMode.IN_MASK to "Inward (Punch Hole)",
                        com.motionssalt.saltygraphics.engine.model.MaskMode.OUT_MASK to "Outward (Matte)"
                    )
                    modes.forEach { (mode, label) ->
                        val isSelected = activeCutoutMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                activeCutoutMode = mode
                                onMaskChange(activeMaskType, activeSourceId, activeInverted, mode)
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            if (activeMaskType != MaskType.NONE) {
                // Source Layer Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mask Source Layer:", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { isSourceDropdownOpen = !isSourceDropdownOpen },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedSourceLayer?.name ?: "Select a mask layer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurface
                                )
                                Icon(
                                    imageVector = if (isSourceDropdownOpen) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Layer"
                                )
                            }
                        }

                        if (isSourceDropdownOpen) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceVariant, RoundedCornerShape(8.dp))
                                    .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (allAvailableLayers.isEmpty()) {
                                    Text(
                                        text = "No other layers in project",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                } else {
                                    allAvailableLayers.forEach { layer ->
                                        val isChosen = layer.id == activeSourceId
                                        Surface(
                                            onClick = {
                                                activeSourceId = layer.id
                                                isSourceDropdownOpen = false
                                                onMaskChange(activeMaskType, layer.id, activeInverted, activeCutoutMode)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isChosen) colors.primary.copy(alpha = 0.15f) else Color.Transparent,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "${layer.name} (${layer.type.displayName})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isChosen) colors.primary else colors.onSurface,
                                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Invert Mask Toggle & Live Mask Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Invert Mask", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                        Text("Reverses mask opacity", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    SaltySwitch(
                        checked = activeInverted,
                        onCheckedChange = { checked ->
                            activeInverted = checked
                            onMaskChange(activeMaskType, activeSourceId, checked, activeCutoutMode)
                        }
                    )
                }

                // Mask Preview Tile
                if (maskPreview != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            bitmap = maskPreview.asImageBitmap(),
                            contentDescription = "Mask Preview",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        )
                        Column {
                            Text(
                                text = "Active Mask Preview (${activeMaskType.displayName}${if (activeInverted) " Inverted" else ""})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Source: ${selectedSourceLayer?.name ?: "None"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
