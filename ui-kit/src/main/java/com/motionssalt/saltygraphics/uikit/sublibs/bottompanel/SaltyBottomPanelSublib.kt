package com.motionssalt.saltygraphics.uikit.sublibs.bottompanel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.motionssalt.saltygraphics.engine.model.ProjectDocument
import com.motionssalt.saltygraphics.uikit.registry.FeatureCatalog
import com.motionssalt.saltygraphics.uikit.registry.FeatureDefinition
import com.motionssalt.saltygraphics.uikit.registry.SaltyBuiltInLibraryRegistry
import com.motionssalt.saltygraphics.uikit.structure.SaltyBox
import com.motionssalt.saltygraphics.uikit.structure.SaltyWorkspace
import com.motionssalt.saltygraphics.uikit.structure.ScaffoldToolPanel
import com.motionssalt.saltygraphics.uikit.structure.ScaffoldToolPicker
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxHeight
import com.motionssalt.saltygraphics.uikit.structure.saltyFillMaxSize

@Composable
fun SaltyBottomToolGridSublib(
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
    SaltyBuiltInLibraryRegistry.ensureRegistered()
    ScaffoldToolPanel(
        catalog = catalog,
        document = document,
        selectedLayerIds = selectedLayerIds,
        availableActions = availableActions,
        onToolClick = onToolClick,
        groupBreadcrumbs = groupBreadcrumbs,
        onPopGroup = onPopGroup,
        onJumpToGroup = onJumpToGroup,
        modifier = modifier
    )
}

@Composable
fun SaltyToolPickerSublib(
    title: String,
    catalog: FeatureCatalog,
    document: ProjectDocument,
    availableActions: Set<String>,
    onChoose: (FeatureDefinition) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScaffoldToolPicker(
        title = title,
        catalog = catalog,
        document = document,
        availableActions = availableActions,
        onChoose = onChoose,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun SaltyEditorWorkspaceContainer(
    modifier: Modifier = Modifier,
    isLayersPanelOpen: Boolean,
    canvasContent: @Composable () -> Unit,
    overlayContent: @Composable () -> Unit,
    layersPanelContent: @Composable () -> Unit,
    panelContent: @Composable () -> Unit
) {
    SaltyBuiltInLibraryRegistry.ensureRegistered()
    SaltyWorkspace(
        modifier = modifier,
        preview = {
            SaltyBox(modifier = Modifier.saltyFillMaxSize()) {
                canvasContent()
                overlayContent()
                if (isLayersPanelOpen) {
                    SaltyBox(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .saltyFillMaxHeight()
                    ) {
                        layersPanelContent()
                    }
                }
            }
        },
        panel = panelContent
    )
}
