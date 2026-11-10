package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.engine.model.*

/**
 * Universal Fill control across all layers (T15 & T16).
 * Provides tabs for Solid Color, Gradient (T14), and Media fill (Fill, Fit, Stretch).
 * Supports transparency independently of stroke.
 */
@Composable
fun UniversalFillSection(
    fill: FillData,
    onFillChange: (FillData) -> Unit,
    onPickMedia: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var showColorPicker by remember { mutableStateOf(false) }
    var showGradientDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Fill",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )

            // Current Fill preview swatch
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, colors.outline, CircleShape)
            ) {
                CheckerboardPattern(Modifier.fillMaxSize())
                val swatchColor = when (fill.type) {
                    FillType.SOLID -> Color(fill.effectiveSolidArgb.toInt())
                    FillType.GRADIENT -> Color(fill.gradient.stops.firstOrNull()?.color?.toInt() ?: 0xFF00E5FF.toInt())
                    FillType.MEDIA -> Color(0xFF888888)
                }
                Box(modifier = Modifier.fillMaxSize().background(swatchColor))
            }
        }

        // Fill Type Tabs (Solid, Gradient, Media)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceVariant),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FillType.entries.forEach { fType ->
                val isSelected = fill.type == fType
                TextButton(
                    onClick = {
                        val updated = fill.copy(type = fType)
                        onFillChange(updated)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (isSelected) colors.primary else Color.Transparent,
                        contentColor = if (isSelected) colors.onPrimary else colors.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(fType.displayName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        when (fill.type) {
            FillType.SOLID -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Color", style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                    Button(
                        onClick = { showColorPicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(fill.solidColor.toInt()))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(String.format("#%08X", fill.effectiveSolidArgb), fontSize = 11.sp, color = colors.onSurface)
                    }
                }

                // Independent Fill Opacity (supports transparent fill with visible stroke!)
                RulerSlider(
                    label = "Fill Opacity",
                    value = fill.solidOpacity * 100f,
                    onValueChange = { pct ->
                        val updated = fill.copy(solidOpacity = (pct / 100f).coerceIn(0f, 1f))
                        onFillChange(updated)
                    },
                    minValue = 0f,
                    maxValue = 100f,
                    step = 1f,
                    unit = "%"
                )
            }
            FillType.GRADIENT -> {
                Button(
                    onClick = { showGradientDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Icon(Icons.Default.Gradient, contentDescription = "Edit Gradient", tint = colors.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit ${fill.gradient.type.displayName} Gradient", color = colors.onSurface)
                }
            }
            FillType.MEDIA -> {
                // T16 Media Fill modes: Fill, Fit, Stretch
                Text("Media Scale Mode", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MediaScaleMode.entries.forEach { mode ->
                        val isSelected = fill.mediaScaleMode == mode
                        TextButton(
                            onClick = {
                                val updated = fill.copy(mediaScaleMode = mode)
                                onFillChange(updated)
                            },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (isSelected) colors.primary else Color.Transparent,
                                contentColor = if (isSelected) colors.onPrimary else colors.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(mode.displayName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                if (onPickMedia != null) {
                    Button(
                        onClick = onPickMedia,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Select Media", tint = colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (fill.mediaAssetPath != null) "Change Media Image" else "Choose Media Image", color = colors.onSurface)
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            title = "Fill Color",
            initialColor = fill.solidColor,
            allowTransparent = true,
            onApply = { newColor ->
                val isAlphaZero = (newColor and 0xFF000000L) == 0L
                val updated = fill.copy(
                    solidColor = newColor,
                    solidOpacity = if (isAlphaZero) 0f else ((newColor shr 24 and 0xFF) / 255f)
                )
                onFillChange(updated)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showGradientDialog) {
        GradientSelectorDialog(
            gradient = fill.gradient,
            onGradientChange = { newGrad ->
                val updated = fill.copy(gradient = newGrad)
                onFillChange(updated)
            },
            onDismiss = { showGradientDialog = false }
        )
    }
}


/**
 * Modal bottom sheet dialog wrapping UniversalFillSection (V7).
 */
@Composable
fun UniversalFillDialog(
    fill: FillData,
    onFillChange: (FillData) -> Unit,
    onDismiss: () -> Unit,
    onPickMedia: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Fill Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close Fill", tint = colors.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        UniversalFillSection(
                            fill = fill,
                            onFillChange = onFillChange,
                            onPickMedia = onPickMedia
                        )
                    }
                }
            }
        }
    }
}
