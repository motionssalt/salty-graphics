package com.motionssalt.saltygraphics.uikit.structure

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.registry.FeatureCatalog
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

/**
 * Reusable panel-native chooser. Features supply registered choices and callbacks only;
 * the same shared header, fixed three-column tool grid and token system own all layout.
 */
@Composable
fun ScaffoldToolPicker(
    title: String,
    catalog: FeatureCatalog,
    document: ProjectDocument,
    availableActions: Set<String>,
    onChoose: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    selectedLayerIds: Set<String> = emptySet()
) {
    SaltyPanelPage(title, onBack, modifier.testTag("scaffold_tool_picker"), scrollable = false) {
        ScaffoldToolPanel(catalog, document, selectedLayerIds, availableActions, onChoose)
    }
}

/** Fixed three-column tool surface. All navigation and eligibility come from registrations. */
@Composable
fun ScaffoldToolPanel(
    catalog: FeatureCatalog,
    document: ProjectDocument,
    selectedLayerIds: Set<String>,
    availableActions: Set<String>,
    onToolClick: (String) -> Unit,
    groupBreadcrumbs: List<Pair<String, String>> = emptyList(),
    onPopGroup: (() -> Unit)? = null,
    onJumpToGroup: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val tiles = catalog.queryTools(document, selectedLayerIds)
        .filter { it.id in availableActions }
        .map { TileItem(it.id, it.title, it.icon, isEnabled = it.enabledCondition(document, selectedLayerIds)) }
    Column(modifier.testTag("scaffold_tool_panel").fillMaxSize().padding(horizontal = SaltyTokens.spacing.sm, vertical = SaltyTokens.spacing.xs), verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)) {
        if (groupBreadcrumbs.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = SaltyTokens.spacing.xxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onPopGroup?.invoke() },
                    modifier = Modifier.size(SaltyTokens.spacing.breadcrumbButton)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Exit Group", tint = colors.primary)
                }
                Spacer(modifier = Modifier.width(SaltyTokens.spacing.xs))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Root",
                        style = SaltyTokens.typography.labelSmall,
                        color = colors.primary,
                        modifier = Modifier.clickable { onJumpToGroup?.invoke(-1) }
                    )
                    groupBreadcrumbs.forEachIndexed { idx, crumb ->
                        Text(" / ", style = SaltyTokens.typography.labelSmall, color = colors.onSurfaceVariant)
                        Text(
                            text = crumb.second,
                            style = SaltyTokens.typography.labelSmall,
                            fontWeight = if (idx == groupBreadcrumbs.lastIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (idx == groupBreadcrumbs.lastIndex) colors.onSurface else colors.primary,
                            modifier = Modifier.clickable { onJumpToGroup?.invoke(idx) }
                        )
                    }
                }
            }
        }
        tiles.chunked(3).forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)) {
                row.forEach { tool ->
                    FilledTonalButton(onClick = { onToolClick(tool.id) }, enabled = tool.isEnabled, modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(SaltyTokens.spacing.xxs)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(tool.icon, null)
                            Text(tool.title, style = SaltyTokens.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
