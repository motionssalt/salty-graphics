package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Sizes are calculated from usable space, after system insets and the top toolbar. */
object PanelSizing {
    fun sideDock(width: Float, height: Float) = width >= 840f || width > height
    fun bottomHeight(height: Float) = (height * .40f).coerceIn(260f, 340f).coerceAtMost(height * .45f)
    fun sideWidth(width: Float) = (width * .40f).coerceIn(280f, 360f).coerceAtMost(width * .45f)
}

@Composable
fun EditorWorkspace(
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
    panel: @Composable () -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val workspaceWidth = maxWidth.value
        val workspaceHeight = maxHeight.value
        if (PanelSizing.sideDock(workspaceWidth, workspaceHeight)) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight().testTag("preview")) { preview() }
                Surface(Modifier.width(PanelSizing.sideWidth(workspaceWidth).dp).fillMaxHeight().testTag("tools"),
                    color = MaterialTheme.colorScheme.surfaceContainer) { panel() }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().testTag("preview")) { preview() }
                Surface(Modifier.height(PanelSizing.bottomHeight(workspaceHeight).dp).fillMaxWidth().testTag("tools"),
                    color = MaterialTheme.colorScheme.surfaceContainer) { panel() }
            }
        }
    }
}

@Composable
fun PanelPage(title: String, onBack: () -> Unit, scrollable: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider()
        Column(Modifier.weight(1f).fillMaxWidth().then(
            if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
        ).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
