package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.uikit.structure.SaltyHistoryTree

/** Historical entry-point name retained for callers; rendered via shared :ui-kit SaltyHistoryTree. */
@Composable
fun ActionTreeDialog(
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
