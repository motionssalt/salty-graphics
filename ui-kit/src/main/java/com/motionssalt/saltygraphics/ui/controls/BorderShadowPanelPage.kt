package com.motionssalt.saltygraphics.ui.controls

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.ui.editor.PanelPage

/**
 * Universal Border & Shadow panel page (V8).
 * All editing lives strictly inside the bottom panel with zero dialogs or overlays.
 */
@Composable
fun BorderShadowPanelPage(
    stroke: StrokeData,
    shadow: ShadowData,
    onStrokePreview: (StrokeData) -> Unit,
    onStrokeCommit: (String, StrokeData) -> Unit,
    onShadowPreview: (ShadowData) -> Unit,
    onShadowCommit: (String, ShadowData) -> Unit,
    onBack: () -> Unit
) {
    var page by remember { mutableStateOf("main") }
    val liveStroke by rememberUpdatedState(stroke)
    val liveShadow by rememberUpdatedState(shadow)

    fun back() {
        if (page != "main") page = "main" else onBack()
    }
    BackHandler(page != "main") { back() }

    when (page) {
        "strokeColor" -> {
            PanelColorPage(
                title = "Stroke Color",
                color = liveStroke.effectiveArgb,
                onPreview = { c -> onStrokePreview(liveStroke.copy(color = c, opacity = 1f)) },
                onCommit = { c -> onStrokeCommit("Change stroke color", liveStroke.copy(color = c, opacity = 1f)) },
                onBack = { page = "main" }
            )
        }
        "shadowColor" -> {
            PanelColorPage(
                title = "Shadow Color",
                color = liveShadow.effectiveArgb,
                onPreview = { c -> onShadowPreview(liveShadow.copy(color = c, opacity = 1f)) },
                onCommit = { c -> onShadowCommit("Change shadow color", liveShadow.copy(color = c, opacity = 1f)) },
                onBack = { page = "main" }
            )
        }
        else -> {
            PanelPage(title = "Border & Shadow", onBack = onBack) {
                val colors = MaterialTheme.colorScheme

                // --- SECTION 1: STROKE ---
                Column(
                    modifier = Modifier
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
                        Text("Stroke / Outline", style = MaterialTheme.typography.titleSmall)
                        SaltySwitch(
                            checked = liveStroke.isEnabled,
                            modifier = Modifier.testTag("stroke_switch"),
                            onCheckedChange = { en ->
                                onStrokeCommit(if (en) "Enable stroke" else "Disable stroke", liveStroke.copy(isEnabled = en))
                            }
                        )
                    }

                    if (liveStroke.isEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Stroke Color", style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { page = "strokeColor" },
                                modifier = Modifier.testTag("stroke_color_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                                border = ButtonDefaults.outlinedButtonBorder
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(liveStroke.effectiveArgb.toInt()))
                                        .border(1.dp, colors.outline, CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    String.format(java.util.Locale.US, "#%08X", liveStroke.effectiveArgb),
                                    fontSize = 11.sp,
                                    color = colors.onSurface
                                )
                            }
                        }

                        PanelNumberControl(
                            label = "Stroke Width",
                            value = liveStroke.width,
                            min = 0.5f,
                            max = 200f,
                            step = 1f,
                            unit = "px",
                            onPreview = { onStrokePreview(liveStroke.copy(width = it)) },
                            onCommit = { onStrokeCommit("Change stroke width", liveStroke.copy(width = it)) }
                        )

                        PanelNumberControl(
                            label = "Stroke Opacity",
                            value = liveStroke.opacity * 100f,
                            min = 0f,
                            max = 100f,
                            step = 1f,
                            unit = "%",
                            onPreview = { onStrokePreview(liveStroke.copy(opacity = it / 100f)) },
                            onCommit = { onStrokeCommit("Change stroke opacity", liveStroke.copy(opacity = it / 100f)) }
                        )

                        SegmentedChipsControl(
                            label = "Alignment",
                            options = StrokeAlignment.entries.map { it.displayName },
                            selectedOption = liveStroke.alignment.displayName,
                            onOptionSelected = { name ->
                                val align = StrokeAlignment.entries.firstOrNull { it.displayName == name } ?: StrokeAlignment.CENTER
                                onStrokeCommit("Change stroke alignment", liveStroke.copy(alignment = align))
                            }
                        )

                        SegmentedChipsControl(
                            label = "Line Style",
                            options = listOf("Solid", "Dashed"),
                            selectedOption = if (liveStroke.isDashed) "Dashed" else "Solid",
                            onOptionSelected = { style ->
                                onStrokeCommit("Change stroke style", liveStroke.copy(isDashed = style == "Dashed"))
                            }
                        )

                        if (liveStroke.isDashed) {
                            PanelNumberControl(
                                label = "Dash Length",
                                value = liveStroke.dashLength,
                                min = 1f,
                                max = 100f,
                                step = 1f,
                                unit = "px",
                                onPreview = { onStrokePreview(liveStroke.copy(dashLength = it)) },
                                onCommit = { onStrokeCommit("Change dash length", liveStroke.copy(dashLength = it)) }
                            )
                            PanelNumberControl(
                                label = "Dash Gap",
                                value = liveStroke.dashGap,
                                min = 1f,
                                max = 100f,
                                step = 1f,
                                unit = "px",
                                onPreview = { onStrokePreview(liveStroke.copy(dashGap = it)) },
                                onCommit = { onStrokeCommit("Change dash gap", liveStroke.copy(dashGap = it)) }
                            )
                        }

                        SegmentedChipsControl(
                            label = "Line Cap",
                            options = VectorCap.entries.map { it.name },
                            selectedOption = liveStroke.cap.name,
                            onOptionSelected = { capName ->
                                val cap = VectorCap.entries.firstOrNull { it.name == capName } ?: VectorCap.ROUND
                                onStrokeCommit("Change stroke cap", liveStroke.copy(cap = cap))
                            }
                        )

                        SegmentedChipsControl(
                            label = "Line Join",
                            options = VectorJoin.entries.map { it.name },
                            selectedOption = liveStroke.join.name,
                            onOptionSelected = { joinName ->
                                val join = VectorJoin.entries.firstOrNull { it.name == joinName } ?: VectorJoin.ROUND
                                onStrokeCommit("Change stroke join", liveStroke.copy(join = join))
                            }
                        )
                    }
                }

                // --- SECTION 2: DROP SHADOW ---
                Column(
                    modifier = Modifier
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
                        Text("Drop Shadow", style = MaterialTheme.typography.titleSmall)
                        SaltySwitch(
                            checked = liveShadow.isEnabled,
                            modifier = Modifier.testTag("shadow_switch"),
                            onCheckedChange = { en ->
                                onShadowCommit(if (en) "Enable shadow" else "Disable shadow", liveShadow.copy(isEnabled = en))
                            }
                        )
                    }

                    if (liveShadow.isEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Shadow Color", style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { page = "shadowColor" },
                                modifier = Modifier.testTag("shadow_color_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                                border = ButtonDefaults.outlinedButtonBorder
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(liveShadow.effectiveArgb.toInt()))
                                        .border(1.dp, colors.outline, CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    String.format(java.util.Locale.US, "#%08X", liveShadow.effectiveArgb),
                                    fontSize = 11.sp,
                                    color = colors.onSurface
                                )
                            }
                        }

                        val shadowModes = listOf(
                            BlendMode.NORMAL, BlendMode.MULTIPLY, BlendMode.DARKEN,
                            BlendMode.LIGHTEN, BlendMode.SCREEN, BlendMode.OVERLAY
                        )
                        SegmentedChipsControl(
                            label = "Shadow Blend",
                            options = shadowModes.map { it.displayName },
                            selectedOption = liveShadow.blendMode.displayName,
                            onOptionSelected = { modeName ->
                                val mode = shadowModes.firstOrNull { it.displayName == modeName } ?: BlendMode.NORMAL
                                onShadowCommit("Change shadow blend", liveShadow.copy(blendMode = mode))
                            }
                        )

                        PanelNumberControl(
                            label = "Shadow Opacity",
                            value = liveShadow.opacity * 100f,
                            min = 0f,
                            max = 100f,
                            step = 1f,
                            unit = "%",
                            onPreview = { onShadowPreview(liveShadow.copy(opacity = it / 100f)) },
                            onCommit = { onShadowCommit("Change shadow opacity", liveShadow.copy(opacity = it / 100f)) }
                        )

                        PanelNumberControl(
                            label = "Light Angle",
                            value = liveShadow.lightAngleDegrees,
                            min = 0f,
                            max = 360f,
                            step = 1f,
                            unit = "°",
                            onPreview = { onShadowPreview(liveShadow.copy(lightAngleDegrees = it)) },
                            onCommit = { onShadowCommit("Change shadow angle", liveShadow.copy(lightAngleDegrees = it)) }
                        )

                        PanelNumberControl(
                            label = "Distance",
                            value = liveShadow.distance,
                            min = 0f,
                            max = 100f,
                            step = 1f,
                            unit = "px",
                            onPreview = { onShadowPreview(liveShadow.copy(distance = it)) },
                            onCommit = { onShadowCommit("Change shadow distance", liveShadow.copy(distance = it)) }
                        )

                        PanelNumberControl(
                            label = "Blur / Softness",
                            value = liveShadow.blurRadius,
                            min = 0f,
                            max = 100f,
                            step = 1f,
                            unit = "px",
                            onPreview = { onShadowPreview(liveShadow.copy(blurRadius = it)) },
                            onCommit = { onShadowCommit("Change shadow blur", liveShadow.copy(blurRadius = it)) }
                        )

                        PanelNumberControl(
                            label = "Spread",
                            value = liveShadow.spread,
                            min = 0f,
                            max = 50f,
                            step = 1f,
                            unit = "px",
                            onPreview = { onShadowPreview(liveShadow.copy(spread = it)) },
                            onCommit = { onShadowCommit("Change shadow spread", liveShadow.copy(spread = it)) }
                        )
                    }
                }
            }
        }
    }
}
