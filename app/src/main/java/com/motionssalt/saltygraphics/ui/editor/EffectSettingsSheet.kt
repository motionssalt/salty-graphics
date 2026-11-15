package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.effect.*
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.uikit.controls.DropdownOption
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorPickerPage
import com.motionssalt.saltygraphics.uikit.controls.SaltyColorSwatchRow
import com.motionssalt.saltygraphics.uikit.controls.SaltyInlineDropdown
import com.motionssalt.saltygraphics.uikit.controls.SaltyNumberControl
import com.motionssalt.saltygraphics.uikit.controls.SaltyProgressNumberControl
import com.motionssalt.saltygraphics.uikit.controls.SaltyRecentColorsRegistry
import com.motionssalt.saltygraphics.uikit.controls.SaltySegmentedChips
import com.motionssalt.saltygraphics.uikit.controls.SaltySwitch
import com.motionssalt.saltygraphics.uikit.structure.SaltyBox
import com.motionssalt.saltygraphics.uikit.structure.SaltyCard
import com.motionssalt.saltygraphics.uikit.structure.SaltyColumn
import com.motionssalt.saltygraphics.uikit.structure.SaltyIcon
import com.motionssalt.saltygraphics.uikit.structure.SaltyIconButton
import com.motionssalt.saltygraphics.uikit.structure.SaltyIconSize
import com.motionssalt.saltygraphics.uikit.structure.SaltyJustify
import com.motionssalt.saltygraphics.uikit.structure.SaltyLazyColumn
import com.motionssalt.saltygraphics.uikit.structure.SaltyRow
import com.motionssalt.saltygraphics.uikit.structure.SaltySectionCard
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltySpacer
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextStyle
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextTone
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextWeight
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxWidth
import com.motionssalt.saltygraphics.uikit.structure.saltyListItems

@Composable
fun EffectSettingsSheet(
    targetLayer: Layer,
    allLayers: List<Layer>,
    onAddEffect: (EffectInstance) -> Unit,
    onRemoveEffect: (String) -> Unit,
    onUpdateEffect: (ActionIdentity, EffectInstance) -> Unit,
    onClose: () -> Unit
) {
    val availableEffects = remember { EffectRegistry.getAvailableEffects(includeDebug = false) }
    var activeColorEdit by remember { mutableStateOf<Triple<ControlDefinition, EffectInstance, String>?>(null) }

    val colorEdit = activeColorEdit
    if (colorEdit != null) {
        val (control, instance, effectName) = colorEdit
        val currentColor = instance.getColor(control.id, control.defaultColor)
        SaltyColorPickerPage(
            title = control.label,
            colorArgb = currentColor,
            onPreview = { c ->
                val identity = ActionIdentity("Change", effectName, "${control.label} color")
                val updated = instance.copy(colorParams = instance.colorParams + (control.id to c))
                activeColorEdit = Triple(control, updated, effectName)
                onUpdateEffect(identity, updated)
            },
            onCommit = { c ->
                SaltyRecentColorsRegistry.recordColor(c)
                val identity = ActionIdentity("Change", effectName, "${control.label} color")
                val updated = instance.copy(colorParams = instance.colorParams + (control.id to c))
                onUpdateEffect(identity, updated)
                activeColorEdit = null
            },
            onBack = { activeColorEdit = null },
            presets = SaltyRecentColorsRegistry.recentColors
        )
        return
    }

    SaltySectionCard {
        SaltyColumn(
            modifier = Modifier.saltyFillMaxWidth(),
            spacing = SaltySpace.MD
        ) {
            SaltyRow(
                modifier = Modifier.saltyFillMaxWidth(),
                justify = SaltyJustify.SPACE_BETWEEN,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SaltyRow(spacing = SaltySpace.SM, verticalAlignment = Alignment.CenterVertically) {
                    SaltyIcon(Icons.Default.AutoAwesome, tone = SaltyTextTone.ACCENT)
                    SaltyText(
                        text = "Effects Engine (${targetLayer.effects.size})",
                        style = SaltyTextStyle.TITLE_MEDIUM,
                        tone = SaltyTextTone.ACCENT,
                        weight = SaltyTextWeight.BOLD
                    )
                }
                SaltyIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    tone = SaltyTextTone.SECONDARY,
                    onClick = onClose
                )
            }

            if (availableEffects.isEmpty() && targetLayer.effects.isEmpty()) {
                SaltyBox(
                    modifier = Modifier.saltyFillMaxWidth(),
                    padding = SaltySpace.LG,
                    contentAlignment = Alignment.Center
                ) {
                    SaltyColumn(horizontalAlignment = Alignment.CenterHorizontally, spacing = SaltySpace.SM) {
                        SaltyIcon(
                            Icons.Default.AutoAwesome,
                            tone = SaltyTextTone.SECONDARY,
                            size = SaltyIconSize.XL
                        )
                        SaltyText(
                            text = "No effects available in Beta",
                            style = SaltyTextStyle.TITLE_MEDIUM,
                            tone = SaltyTextTone.SECONDARY,
                            weight = SaltyTextWeight.SEMI_BOLD
                        )
                        SaltyText(
                            text = "The underlying GPU effect engine & multi-input shader pipeline is fully wired. Real effects will ship in subsequent releases.",
                            style = SaltyTextStyle.BODY_MEDIUM,
                            tone = SaltyTextTone.SECONDARY
                        )
                    }
                }
            } else {
                SaltyLazyColumn(
                    modifier = Modifier.saltyFillMaxWidth(),
                    spacing = SaltySpace.MD
                ) {
                    saltyListItems(targetLayer.effects, key = { it.effectId }) { effectInstance ->
                        val definition = EffectRegistry.getEffectDefinition(effectInstance.effectId)
                        if (definition != null) {
                            SaltyCard(padding = SaltySpace.MD, spacing = SaltySpace.SM) {
                                SaltyRow(
                                    modifier = Modifier.saltyFillMaxWidth(),
                                    justify = SaltyJustify.SPACE_BETWEEN,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SaltyText(
                                        text = definition.name,
                                        style = SaltyTextStyle.TITLE_MEDIUM,
                                        tone = SaltyTextTone.ACCENT
                                    )
                                    SaltyIconButton(
                                        icon = Icons.Default.Close,
                                        contentDescription = "Remove Effect",
                                        tone = SaltyTextTone.ERROR,
                                        onClick = { onRemoveEffect(effectInstance.effectId) }
                                    )
                                }

                                definition.controls.forEach { control ->
                                    AutoGeneratedControl(
                                        control = control,
                                        instance = effectInstance,
                                        allLayers = allLayers,
                                        effectName = definition.name,
                                        onOpenColorPicker = {
                                            activeColorEdit = Triple(control, effectInstance, definition.name)
                                        },
                                        onValueChange = { identity, updated ->
                                            onUpdateEffect(identity, updated)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EffectSettingsSheet(
    targetLayer: Layer,
    allLayers: List<Layer>,
    onAddEffect: (EffectInstance) -> Unit,
    onRemoveEffect: (String) -> Unit,
    onUpdateEffect: (EffectInstance) -> Unit,
    onClose: () -> Unit
) {
    EffectSettingsSheet(
        targetLayer = targetLayer,
        allLayers = allLayers,
        onAddEffect = onAddEffect,
        onRemoveEffect = onRemoveEffect,
        onUpdateEffect = { _, instance -> onUpdateEffect(instance) },
        onClose = onClose
    )
}

/**
 * Reusable auto-generated control widget supporting all effect control types via :ui-kit.
 */
@Composable
fun AutoGeneratedControl(
    control: ControlDefinition,
    instance: EffectInstance,
    allLayers: List<Layer>,
    effectName: String = "Effect",
    onOpenColorPicker: (() -> Unit)? = null,
    onValueChange: (ActionIdentity, EffectInstance) -> Unit
) {
    SaltyColumn(padding = SaltySpace.XXS) {
        when (control.type) {
            ControlType.TOGGLE -> {
                val current = instance.getBool(control.id, control.defaultToggle)
                SaltyRow(
                    modifier = Modifier.saltyFillMaxWidth(),
                    padding = SaltySpace.XS,
                    justify = SaltyJustify.SPACE_BETWEEN,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaltyText(text = control.label, style = SaltyTextStyle.LABEL_MEDIUM)
                    SaltySwitch(
                        checked = current,
                        onCheckedChange = {
                            val identity = ActionIdentity.forBooleanChange(effectName, control.label, it)
                            onValueChange(identity, instance.copy(boolParams = instance.boolParams + (control.id to it)))
                        }
                    )
                }
            }
            ControlType.SLIDER -> {
                val current = instance.getFloat(control.id, control.defaultValue)
                if (control.sliderStyle == SliderStyle.PROGRESS) {
                    SaltyProgressNumberControl(
                        label = control.label,
                        value = current,
                        min = control.minValue,
                        max = control.maxValue,
                        defaultValue = control.defaultValue,
                        step = control.step,
                        unit = control.unit,
                        onPreview = {
                            val identity = ActionIdentity.forNumericChange(effectName, control.label, current, it)
                            onValueChange(identity, instance.copy(floatParams = instance.floatParams + (control.id to it)))
                        },
                        onCommit = {
                            val identity = ActionIdentity.forNumericChange(effectName, control.label, current, it)
                            onValueChange(identity, instance.copy(floatParams = instance.floatParams + (control.id to it)))
                        }
                    )
                } else {
                    SaltyNumberControl(
                        label = control.label,
                        value = current,
                        min = control.minValue,
                        max = control.maxValue,
                        defaultValue = control.defaultValue,
                        step = control.step,
                        unit = control.unit,
                        onPreview = {
                            val identity = ActionIdentity.forNumericChange(effectName, control.label, current, it)
                            onValueChange(identity, instance.copy(floatParams = instance.floatParams + (control.id to it)))
                        },
                        onCommit = {
                            val identity = ActionIdentity.forNumericChange(effectName, control.label, current, it)
                            onValueChange(identity, instance.copy(floatParams = instance.floatParams + (control.id to it)))
                        }
                    )
                }
            }
            ControlType.PROGRESS_SLIDER -> {
                val current = instance.getFloat(control.id, control.defaultValue)
                SaltyProgressNumberControl(
                    label = control.label,
                    value = current,
                    min = control.minValue,
                    max = control.maxValue,
                    defaultValue = control.defaultValue,
                    step = control.step,
                    unit = control.unit.ifBlank { "%" },
                    onPreview = {
                        val identity = ActionIdentity.forNumericChange(effectName, control.label, current, it)
                        onValueChange(identity, instance.copy(floatParams = instance.floatParams + (control.id to it)))
                    },
                    onCommit = {
                        val identity = ActionIdentity.forNumericChange(effectName, control.label, current, it)
                        onValueChange(identity, instance.copy(floatParams = instance.floatParams + (control.id to it)))
                    }
                )
            }
            ControlType.COLOR_PICKER -> {
                val current = instance.getColor(control.id, control.defaultColor)
                SaltyColorSwatchRow(
                    label = control.label,
                    colorArgb = current,
                    onClick = { onOpenColorPicker?.invoke() }
                )
            }
            ControlType.DROPDOWN -> {
                val current = instance.getString(control.id, control.options.firstOrNull() ?: "")
                SaltyInlineDropdown(
                    label = control.label,
                    selected = current,
                    options = control.options.map { DropdownOption(it, it) },
                    onSelect = {
                        val identity = ActionIdentity("Set", effectName, "${control.label} to $it")
                        onValueChange(identity, instance.copy(stringParams = instance.stringParams + (control.id to it)))
                    }
                )
            }
            ControlType.SEGMENTED_CHIPS -> {
                val current = instance.getString(control.id, control.options.firstOrNull() ?: "")
                SaltySegmentedChips(
                    label = control.label,
                    options = control.options,
                    selectedOption = current,
                    onOptionSelected = {
                        val identity = ActionIdentity("Set", effectName, "${control.label} to $it")
                        onValueChange(identity, instance.copy(stringParams = instance.stringParams + (control.id to it)))
                    }
                )
            }
            ControlType.LAYER_SELECTOR, ControlType.TEXT_FIELD -> {
                val current = instance.getString(control.id, control.defaultText)
                val layerOptions = listOf("None") + allLayers.map { it.name }
                SaltyInlineDropdown(
                    label = control.label,
                    selected = current.ifBlank { "None" },
                    options = layerOptions.map { DropdownOption(it, it) },
                    onSelect = {
                        val identity = ActionIdentity("Set", effectName, "${control.label} to $it")
                        onValueChange(identity, instance.copy(stringParams = instance.stringParams + (control.id to it)))
                    }
                )
            }
        }
    }
}
