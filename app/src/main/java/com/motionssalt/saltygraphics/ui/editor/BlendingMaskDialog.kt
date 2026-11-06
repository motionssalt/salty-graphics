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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.motionssalt.saltygraphics.engine.model.MaskMode
import com.motionssalt.saltygraphics.engine.render.BlendMath
import com.motionssalt.saltygraphics.theme.*

object BlendThumbnailCache {
    private val cache = LruCache<BlendMode, Bitmap>(25)

    fun getThumbnail(mode: BlendMode): Bitmap {
        cache.get(mode)?.let { return it }

        // Render representative sample: gradient base + tinted circle blend layer
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Base gradient colors
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
    currentMaskMode: MaskMode,
    affectsLayerIds: List<String>,
    allUnderlyingLayers: List<Layer>,
    onBlendChange: (BlendMode, Float) -> Unit,
    onMaskChange: (MaskMode, List<String>) -> Unit,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var selectedCategory by remember { mutableStateOf<BlendCategory?>(null) }
    var activeBlendMode by remember(currentBlendMode) { mutableStateOf(currentBlendMode) }
    var activeOpacity by remember(currentOpacity) { mutableFloatStateOf(currentOpacity) }
    var activeMaskMode by remember(currentMaskMode) { mutableStateOf(currentMaskMode) }
    var activeAffectsIds by remember(affectsLayerIds) { mutableStateOf(affectsLayerIds.toSet()) }

    val displayedModes = remember(selectedCategory) {
        if (selectedCategory == null) BlendMode.entries
        else BlendMode.byCategory(selectedCategory!!)
    }

    PanelPage(title = "Blending & Mask", onBack = onBack, scrollable = true) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Opacity Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Opacity", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    Text("${(activeOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = colors.primary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = activeOpacity,
                    onValueChange = {
                        activeOpacity = it
                        onBlendChange(activeBlendMode, it)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                )
            }

            HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))

            // Blend Categories (S12: Category Tabs/Chips)
            Text("Blend Mode Categories", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val categories = listOf(null) + BlendCategory.entries
                categories.forEach { cat ->
                    val isCatSelected = selectedCategory == cat
                    val catName = cat?.displayName ?: "All"
                    FilterChip(
                        selected = isCatSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(catName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Grid of Blend Modes with Live Thumbnails
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

            // S12: Masking Section (integrated into blending)
            Text("Masking", style = MaterialTheme.typography.titleSmall, color = colors.primary)
            Text("Apply this layer as a clipping mask over underlying layers.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MaskMode.entries.forEach { mode ->
                    val isModeSelected = activeMaskMode == mode
                    FilterChip(
                        selected = isModeSelected,
                        onClick = {
                            activeMaskMode = mode
                            onMaskChange(mode, activeAffectsIds.toList())
                        },
                        label = { Text(mode.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (activeMaskMode != MaskMode.NONE && allUnderlyingLayers.isNotEmpty()) {
                Text("Layers affected by mask:", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    allUnderlyingLayers.forEach { layer ->
                        val isChecked = activeAffectsIds.contains(layer.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    activeAffectsIds = if (isChecked) activeAffectsIds - layer.id else activeAffectsIds + layer.id
                                    onMaskChange(activeMaskMode, activeAffectsIds.toList())
                                }
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    activeAffectsIds = if (checked) activeAffectsIds + layer.id else activeAffectsIds - layer.id
                                    onMaskChange(activeMaskMode, activeAffectsIds.toList())
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(layer.name, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                        }
                    }
                }
            }
        }
    }
}
