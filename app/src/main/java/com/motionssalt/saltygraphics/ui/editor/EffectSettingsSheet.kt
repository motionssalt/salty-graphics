package com.motionssalt.saltygraphics.ui.editor

import com.motionssalt.saltygraphics.ui.controls.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.engine.effect.*
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectSettingsSheet(
    targetLayer: Layer,
    allLayers: List<Layer>,
    onAddEffect: (EffectInstance) -> Unit,
    onRemoveEffect: (String) -> Unit,
    onUpdateEffect: (EffectInstance) -> Unit,
    onClose: () -> Unit
) {
    // In Beta: ships zero real effects (Section 4.2)
    val availableEffects = remember { EffectRegistry.getAvailableEffects(includeDebug = false) }

    Surface(
        color = SaltySurfaceDark,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SaltyCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Effects Engine (${targetLayer.effects.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SaltyCyan
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SaltyTextSecondary)
                }
            }

            if (availableEffects.isEmpty() && targetLayer.effects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SaltyDarkBackground)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SaltyTextTertiary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No effects available in Beta",
                            style = MaterialTheme.typography.titleMedium,
                            color = SaltyTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "The underlying GPU effect engine & multi-input shader pipeline is fully wired. Real effects will ship in subsequent releases.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SaltyTextTertiary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(targetLayer.effects, key = { it.effectId }) { effectInstance ->
                        val definition = EffectRegistry.getEffectDefinition(effectInstance.effectId)
                        if (definition != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = SaltySurfaceVariantDark)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(definition.name, style = MaterialTheme.typography.titleMedium, color = SaltyCyan)
                                        IconButton(onClick = { onRemoveEffect(effectInstance.effectId) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove Effect", tint = SaltyError)
                                        }
                                    }

                                    // Automatically generate controls based on definition (Section 4.2)
                                    definition.controls.forEach { control ->
                                        AutoGeneratedControl(
                                            control = control,
                                            instance = effectInstance,
                                            allLayers = allLayers,
                                            onValueChange = { updated ->
                                                onUpdateEffect(updated)
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
}

/**
 * Reusable auto-generated control widget supporting all 6 control types.
 */
@Composable
fun AutoGeneratedControl(
    control: ControlDefinition,
    instance: EffectInstance,
    allLayers: List<Layer>,
    onValueChange: (EffectInstance) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        when (control.type) {
            ControlType.TOGGLE -> {
                val current = instance.getBool(control.id, control.defaultToggle)
                ToggleControl(
                    label = control.label,
                    checked = current,
                    onCheckedChange = {
                        onValueChange(instance.copy(boolParams = instance.boolParams + (control.id to it)))
                    }
                )
            }
            ControlType.SLIDER -> {
                val current = instance.getFloat(control.id, control.defaultValue)
                if (control.sliderStyle == SliderStyle.PROGRESS) {
                    ProgressSliderControl(
                        label = control.label,
                        value = current,
                        onValueChange = {
                            onValueChange(instance.copy(floatParams = instance.floatParams + (control.id to it)))
                        },
                        minValue = control.minValue,
                        maxValue = control.maxValue,
                        defaultValue = control.defaultValue,
                        step = control.step,
                        unit = control.unit
                    )
                } else {
                    RulerSlider(
                        label = control.label,
                        value = current,
                        onValueChange = {
                            onValueChange(instance.copy(floatParams = instance.floatParams + (control.id to it)))
                        },
                        minValue = control.minValue,
                        maxValue = control.maxValue,
                        defaultValue = control.defaultValue,
                        step = control.step,
                        unit = control.unit
                    )
                }
            }
            ControlType.PROGRESS_SLIDER -> {
                val current = instance.getFloat(control.id, control.defaultValue)
                ProgressSliderControl(
                    label = control.label,
                    value = current,
                    onValueChange = {
                        onValueChange(instance.copy(floatParams = instance.floatParams + (control.id to it)))
                    },
                    minValue = control.minValue,
                    maxValue = control.maxValue,
                    defaultValue = control.defaultValue,
                    step = control.step,
                    unit = control.unit.ifBlank { "%" }
                )
            }
            ControlType.COLOR_PICKER -> {
                val current = instance.getColor(control.id, control.defaultColor)
                ColorPickerRow(
                    label = control.label,
                    color = current,
                    onColorChange = {
                        onValueChange(instance.copy(colorParams = instance.colorParams + (control.id to it)))
                    }
                )
            }
            ControlType.DROPDOWN -> {
                val current = instance.getString(control.id, control.options.firstOrNull() ?: "")
                DropdownControl(
                    label = control.label,
                    options = control.options,
                    selectedOption = current,
                    onOptionSelected = {
                        onValueChange(instance.copy(stringParams = instance.stringParams + (control.id to it)))
                    }
                )
            }
            ControlType.SEGMENTED_CHIPS -> {
                val current = instance.getString(control.id, control.options.firstOrNull() ?: "")
                SegmentedChipsControl(
                    label = control.label,
                    options = control.options,
                    selectedOption = current,
                    onOptionSelected = {
                        onValueChange(instance.copy(stringParams = instance.stringParams + (control.id to it)))
                    }
                )
            }
            ControlType.LAYER_SELECTOR, ControlType.TEXT_FIELD -> {
                val current = instance.getString(control.id, control.defaultText)
                val layerOptions = listOf("None") + allLayers.map { it.name }
                DropdownControl(
                    label = control.label,
                    options = layerOptions,
                    selectedOption = current.ifBlank { "None" },
                    onOptionSelected = {
                        onValueChange(instance.copy(stringParams = instance.stringParams + (control.id to it)))
                    }
                )
            }
        }
    }
}
