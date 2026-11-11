package com.motionssalt.saltygraphics.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object NewProject : Screen("new_project")
    data object Editor : Screen("editor/{projectId}?groupId={groupId}") {
        fun createRoute(projectId: String, groupId: String? = null): String {
            return if (groupId != null) "editor/$projectId?groupId=$groupId" else "editor/$projectId"
        }
    }
    data object Export : Screen("export/{projectId}") {
        fun createRoute(projectId: String): String = "export/$projectId"
    }
}
