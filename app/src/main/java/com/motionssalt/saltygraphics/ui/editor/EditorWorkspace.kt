package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.uikit.structure.SaltyPanelColumnScope
import com.motionssalt.saltygraphics.uikit.structure.SaltyPanelPage
import com.motionssalt.saltygraphics.uikit.structure.SaltyWorkspace

object PanelSizing {
    fun sideDock(width: Float, height: Float) =
        com.motionssalt.saltygraphics.uikit.structure.PanelSizing.sideDock(width, height)
    fun bottomHeight(height: Float) =
        com.motionssalt.saltygraphics.uikit.structure.PanelSizing.bottomHeight(height)
    fun sideWidth(width: Float) =
        com.motionssalt.saltygraphics.uikit.structure.PanelSizing.sideWidth(width)
}

@Composable
fun EditorWorkspace(
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
    panel: @Composable () -> Unit
) {
    SaltyWorkspace(
        modifier = modifier,
        preview = preview,
        panel = panel
    )
}

@Composable
fun PanelPage(
    title: String,
    onBack: () -> Unit,
    scrollable: Boolean = true,
    content: @Composable SaltyPanelColumnScope.() -> Unit
) {
    SaltyPanelPage(
        title = title,
        onBack = onBack,
        scrollable = scrollable,
        content = content
    )
}
