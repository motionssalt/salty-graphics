package com.motionssalt.saltygraphics.ui.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.uikit.controls.DropdownOption
import com.motionssalt.saltygraphics.uikit.controls.SaltyInlineDropdown
import com.motionssalt.saltygraphics.uikit.controls.SaltySegmentedChips
import com.motionssalt.saltygraphics.uikit.structure.SaltyJustify
import com.motionssalt.saltygraphics.uikit.structure.SaltyRow
import com.motionssalt.saltygraphics.uikit.structure.SaltySpace
import com.motionssalt.saltygraphics.uikit.structure.SaltyText
import com.motionssalt.saltygraphics.uikit.structure.SaltyTextStyle
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxWidth

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
    SaltyRow(
        modifier = modifier.saltyFillMaxWidth(),
        padding = SaltySpace.XS,
        justify = SaltyJustify.SPACE_BETWEEN,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SaltyText(
            text = label,
            style = SaltyTextStyle.LABEL_MEDIUM
        )
        com.motionssalt.saltygraphics.uikit.controls.SaltySwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
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
