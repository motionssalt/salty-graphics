package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SegmentedChipsControl(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SaltySegmentedChips(
        options = options,
        selectedOption = selectedOption,
        onOptionSelected = onOptionSelected,
        modifier = modifier,
        label = label
    )
}

@Composable
fun ToggleControl(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SaltyToggleRow(
        label = label,
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier
    )
}

@Composable
fun DropdownControl(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SaltyInlineDropdown(
        selected = selectedOption,
        options = options.map { DropdownOption(value = it, label = it) },
        onSelect = onOptionSelected,
        modifier = modifier,
        label = label
    )
}
