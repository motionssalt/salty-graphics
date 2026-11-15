package com.motionssalt.saltygraphics.ui.controls

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorPickerPage
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorSwatchRow
import com.motionssalt.saltygraphics.uikit.controls.SaltyNumberControl
import com.motionssalt.saltygraphics.uikit.controls.SaltyRecentColorsRegistry
import com.motionssalt.saltygraphics.uikit.controls.SaltySegmentedChips
import com.motionssalt.saltygraphics.uikit.controls.SaltySwitch
import com.motionssalt.saltygraphics.uikit.structure.SaltyJustify
import com.motionssalt.saltygraphics.uikit.structure.SaltyPanelPage
import com.motionssalt.saltygraphics.uikit.structure.SaltyRow
import com.motionssalt.saltygraphics.uikit.structure.SaltySectionCard
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextStyle
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextWeight
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxWidth

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
        "strokeColor" -> SaltyColorPickerPage(
            title = "Stroke Color",
            colorArgb = liveStroke.effectiveArgb,
            onPreview = { c -> onStrokePreview(liveStroke.copy(color = c, opacity = 1f)) },
            onCommit = { c ->
                SaltyRecentColorsRegistry.recordColor(c)
                onStrokeCommit("Change stroke color", liveStroke.copy(color = c, opacity = 1f))
            },
            onBack = { page = "main" },
            presets = SaltyRecentColorsRegistry.recentColors,
            applyLabel = "Set hex"
        )
        "shadowColor" -> SaltyColorPickerPage(
            title = "Shadow Color",
            colorArgb = liveShadow.effectiveArgb,
            onPreview = { c -> onShadowPreview(liveShadow.copy(color = c, opacity = 1f)) },
            onCommit = { c ->
                SaltyRecentColorsRegistry.recordColor(c)
                onShadowCommit("Change shadow color", liveShadow.copy(color = c, opacity = 1f))
            },
            onBack = { page = "main" },
            presets = SaltyRecentColorsRegistry.recentColors,
            applyLabel = "Set hex"
        )
        else -> SaltyPanelPage(title = "Border & Shadow", onBack = onBack) {
            // --- SECTION 1: STROKE ---
            SaltySectionCard {
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    justify = SaltyJustify.SPACE_BETWEEN,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaltyText(
                        text = "Stroke / Outline",
                        style = SaltyTextStyle.TITLE_SMALL,
                        weight = SaltyTextWeight.SEMI_BOLD
                    )
                    SaltySwitch(
                        checked = liveStroke.isEnabled,
                        modifier = Modifier.testTag("stroke_switch"),
                        onCheckedChange = { en ->
                            onStrokeCommit(
                                if (en) "Enable stroke" else "Disable stroke",
                                liveStroke.copy(isEnabled = en)
                            )
                        }
                    )
                }

                if (liveStroke.isEnabled) {
                    SaltySegmentedChips(
                        options = StrokeAlignment.entries.map { it.displayName },
                        selectedOption = liveStroke.alignment.displayName,
                        onOptionSelected = { alignName ->
                            val align = StrokeAlignment.entries.firstOrNull { it.displayName == alignName } ?: StrokeAlignment.CENTER
                            onStrokeCommit("Change stroke alignment", liveStroke.copy(alignment = align))
                        },
                        label = "Alignment"
                    )
                    SaltyNumberControl(
                        label = "Stroke Width",
                        value = liveStroke.width,
                        min = 0f,
                        max = 100f,
                        step = 0.5f,
                        unit = "px",
                        onPreview = { onStrokePreview(liveStroke.copy(width = it)) },
                        onCommit = { onStrokeCommit("Change stroke width", liveStroke.copy(width = it)) }
                    )
                    SaltyNumberControl(
                        label = "Stroke Opacity",
                        value = liveStroke.opacity * 100f,
                        min = 0f,
                        max = 100f,
                        step = 1f,
                        unit = "%",
                        onPreview = { onStrokePreview(liveStroke.copy(opacity = it / 100f)) },
                        onCommit = { onStrokeCommit("Change stroke opacity", liveStroke.copy(opacity = it / 100f)) }
                    )
                    SaltyColorSwatchRow(
                        label = "Stroke Color",
                        colorArgb = liveStroke.effectiveArgb,
                        onClick = { page = "strokeColor" },
                        buttonTag = "stroke_color_btn"
                    )
                    SaltyRow(
                        modifier = Modifier.saltyFillMaxWidth(),
                        justify = SaltyJustify.SPACE_BETWEEN,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SaltyText(text = "Dashed Stroke", style = SaltyTextStyle.BODY_SMALL)
                        SaltySwitch(
                            checked = liveStroke.isDashed,
                            onCheckedChange = { dashed ->
                                onStrokeCommit(
                                    if (dashed) "Enable dashed stroke" else "Disable dashed stroke",
                                    liveStroke.copy(isDashed = dashed)
                                )
                            }
                        )
                    }
                    if (liveStroke.isDashed) {
                        SaltyNumberControl(
                            label = "Dash Length",
                            value = liveStroke.dashLength,
                            min = 1f,
                            max = 100f,
                            step = 1f,
                            unit = "px",
                            onPreview = { onStrokePreview(liveStroke.copy(dashLength = it)) },
                            onCommit = { onStrokeCommit("Change dash length", liveStroke.copy(dashLength = it)) }
                        )
                        SaltyNumberControl(
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
                    SaltySegmentedChips(
                        options = VectorCap.entries.map { it.name },
                        selectedOption = liveStroke.cap.name,
                        onOptionSelected = { capName ->
                            val cap = VectorCap.entries.firstOrNull { it.name == capName } ?: VectorCap.ROUND
                            onStrokeCommit("Change stroke cap", liveStroke.copy(cap = cap))
                        },
                        label = "Line Cap"
                    )
                    SaltySegmentedChips(
                        options = VectorJoin.entries.map { it.name },
                        selectedOption = liveStroke.join.name,
                        onOptionSelected = { joinName ->
                            val join = VectorJoin.entries.firstOrNull { it.name == joinName } ?: VectorJoin.ROUND
                            onStrokeCommit("Change stroke join", liveStroke.copy(join = join))
                        },
                        label = "Line Join"
                    )
                }
            }

            // --- SECTION 2: DROP SHADOW ---
            SaltySectionCard {
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    justify = SaltyJustify.SPACE_BETWEEN,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaltyText(
                        text = "Drop Shadow",
                        style = SaltyTextStyle.TITLE_SMALL,
                        weight = SaltyTextWeight.SEMI_BOLD
                    )
                    SaltySwitch(
                        checked = liveShadow.isEnabled,
                        modifier = Modifier.testTag("shadow_switch"),
                        onCheckedChange = { en ->
                            onShadowCommit(
                                if (en) "Enable shadow" else "Disable shadow",
                                liveShadow.copy(isEnabled = en)
                            )
                        }
                    )
                }

                if (liveShadow.isEnabled) {
                    SaltyColorSwatchRow(
                        label = "Shadow Color",
                        colorArgb = liveShadow.effectiveArgb,
                        onClick = { page = "shadowColor" },
                        buttonTag = "shadow_color_btn"
                    )
                    val shadowModes = listOf(
                        BlendMode.NORMAL, BlendMode.MULTIPLY, BlendMode.DARKEN,
                        BlendMode.LIGHTEN, BlendMode.SCREEN, BlendMode.OVERLAY
                    )
                    SaltySegmentedChips(
                        options = shadowModes.map { it.displayName },
                        selectedOption = liveShadow.blendMode.displayName,
                        onOptionSelected = { modeName ->
                            val mode = shadowModes.firstOrNull { it.displayName == modeName } ?: BlendMode.NORMAL
                            onShadowCommit("Change shadow blend", liveShadow.copy(blendMode = mode))
                        },
                        label = "Shadow Blend"
                    )
                    SaltyNumberControl(
                        label = "Shadow Opacity",
                        value = liveShadow.opacity * 100f,
                        min = 0f,
                        max = 100f,
                        step = 1f,
                        unit = "%",
                        onPreview = { onShadowPreview(liveShadow.copy(opacity = it / 100f)) },
                        onCommit = { onShadowCommit("Change shadow opacity", liveShadow.copy(opacity = it / 100f)) }
                    )
                    SaltyNumberControl(
                        label = "Light Angle",
                        value = liveShadow.lightAngleDegrees,
                        min = 0f,
                        max = 360f,
                        step = 1f,
                        unit = "°",
                        onPreview = { onShadowPreview(liveShadow.copy(lightAngleDegrees = it)) },
                        onCommit = { onShadowCommit("Change shadow angle", liveShadow.copy(lightAngleDegrees = it)) }
                    )
                    SaltyNumberControl(
                        label = "Distance",
                        value = liveShadow.distance,
                        min = 0f,
                        max = 100f,
                        step = 1f,
                        unit = "px",
                        onPreview = { onShadowPreview(liveShadow.copy(distance = it)) },
                        onCommit = { onShadowCommit("Change shadow distance", liveShadow.copy(distance = it)) }
                    )
                    SaltyNumberControl(
                        label = "Blur / Softness",
                        value = liveShadow.blurRadius,
                        min = 0f,
                        max = 100f,
                        step = 1f,
                        unit = "px",
                        onPreview = { onShadowPreview(liveShadow.copy(blurRadius = it)) },
                        onCommit = { onShadowCommit("Change shadow blur", liveShadow.copy(blurRadius = it)) }
                    )
                    SaltyNumberControl(
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
