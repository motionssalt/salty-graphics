package com.motionssalt.saltygraphics.uikit.sublibs.history

import androidx.compose.runtime.Composable
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.uikit.registry.LibraryComponentRegistry
import com.motionssalt.saltygraphics.uikit.registry.SaltyBuiltInLibraryRegistry
import com.motionssalt.saltygraphics.uikit.registry.SaltyHistoryTreeAdapterState

@Composable
fun SaltyHistorySublib(
    actionTree: ActionTree,
    onJumpToNode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    SaltyBuiltInLibraryRegistry.ensureRegistered()
    val entry = requireNotNull(LibraryComponentRegistry.get("history_tree")) {
        "history_tree library component must be registered"
    }
    entry.renderer(
        "Action History",
        SaltyHistoryTreeAdapterState(
            actionTree = actionTree,
            onJumpToNode = onJumpToNode,
            onDismiss = onDismiss
        ),
        {},
        {}
    )
}
