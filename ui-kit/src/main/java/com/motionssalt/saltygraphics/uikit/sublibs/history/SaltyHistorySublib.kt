package com.motionssalt.saltygraphics.uikit.sublibs.history

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.uikit.registry.ScaffoldLibraryComponent
import com.motionssalt.saltygraphics.uikit.registry.SaltyHistoryTreeAdapterState

@Composable
fun SaltyHistorySublib(
    actionTree: ActionTree,
    onJumpToNode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ScaffoldLibraryComponent(
        id = "history_tree",
        label = "Action History",
        current = SaltyHistoryTreeAdapterState(
            actionTree = actionTree,
            onJumpToNode = onJumpToNode,
            onDismiss = onDismiss
        ),
        onPreview = {},
        onCommit = {}
    )
}
