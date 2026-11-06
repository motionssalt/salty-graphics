package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.theme.*

@Composable
fun BottomPanel(
    selectedLayer: Layer?,
    onAddMedia: () -> Unit,
    onAddText: () -> Unit,
    onAddAdjustment: () -> Unit,
    onAddShape: () -> Unit,
    onAddVector: () -> Unit,
    onEditLayer: () -> Unit,
    onOpenTransform: () -> Unit,
    onOpenEffects: () -> Unit,
    onOpenBlending: () -> Unit,
    onOpenMask: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SaltySurfaceDark,
        tonalElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedLayer == null) {
                // No layer selected options
                BottomPanelButton(
                    label = "Media",
                    icon = Icons.Default.AddPhotoAlternate,
                    onClick = onAddMedia
                )
                BottomPanelButton(
                    label = "Text",
                    icon = Icons.Default.TextFields,
                    onClick = onAddText
                )
                BottomPanelButton(
                    label = "Adjustment",
                    icon = Icons.Default.Tune,
                    onClick = onAddAdjustment
                )
                BottomPanelButton(
                    label = "Shape",
                    icon = Icons.Default.Category,
                    onClick = onAddShape
                )
                BottomPanelButton(
                    label = "Vector Pen",
                    icon = Icons.Default.Draw,
                    onClick = onAddVector
                )
            } else {
                // A layer is selected options
                val canEdit = selectedLayer.type != LayerType.ADJUSTMENT
                BottomPanelButton(
                    label = when (selectedLayer.type) {
                        LayerType.MEDIA -> "Replace"
                        LayerType.TEXT -> "Text Settings"
                        LayerType.SHAPE -> "Shape Settings"
                        LayerType.VECTOR -> "Edit Points"
                        LayerType.GROUP -> "Edit Group"
                        LayerType.ADJUSTMENT -> "Adjustment"
                    },
                    icon = Icons.Default.Edit,
                    enabled = canEdit,
                    onClick = onEditLayer
                )

                BottomPanelButton(
                    label = "Transform",
                    icon = Icons.Default.OpenWith,
                    onClick = onOpenTransform
                )

                BottomPanelButton(
                    label = "Effects (${selectedLayer.effects.size})",
                    icon = Icons.Default.AutoAwesome,
                    onClick = onOpenEffects
                )

                BottomPanelButton(
                    label = "Blending",
                    icon = Icons.Default.Layers,
                    onClick = onOpenBlending
                )

                BottomPanelButton(
                    label = "Mask",
                    icon = Icons.Default.FilterFrames,
                    onClick = onOpenMask
                )
            }
        }
    }
}

@Composable
fun BottomPanelButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = SaltySurfaceVariantDark,
            contentColor = SaltyCyan,
            disabledContainerColor = SaltySurfaceVariantDark.copy(alpha = 0.5f),
            disabledContentColor = SaltyTextTertiary
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
