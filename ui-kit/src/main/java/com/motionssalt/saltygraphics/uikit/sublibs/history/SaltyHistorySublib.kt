package com.motionssalt.saltygraphics.uikit.sublibs.history

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.uikit.structure.SaltyHistoryTree

@Composable
fun SaltyHistorySublib(
    actionTree: ActionTree,
    onJumpToNode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    SaltyHistoryTree(
        actionTree = actionTree,
        onJumpToNode = onJumpToNode,
        onDismiss = onDismiss
    )
}
