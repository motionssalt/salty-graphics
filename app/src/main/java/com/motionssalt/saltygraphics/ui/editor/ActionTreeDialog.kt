package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.uikit.sublibs.history.SaltyHistorySublib

/** Historical entry-point name retained for callers; rendered via shared :ui-kit history sub-library. */
@Composable
fun ActionTreeDialog(
    actionTree: ActionTree,
    onJumpToNode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    SaltyHistorySublib(
        actionTree = actionTree,
        onJumpToNode = onJumpToNode,
        onDismiss = onDismiss
    )
}
