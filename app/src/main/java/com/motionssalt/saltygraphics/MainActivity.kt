package com.motionssalt.saltygraphics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.motionssalt.saltygraphics.data.DeviceInfoProvider
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.data.SettingsManager
import com.motionssalt.saltygraphics.navigation.Screen
import com.motionssalt.saltygraphics.theme.SaltyGraphicsTheme
import com.motionssalt.saltygraphics.ui.editor.EditorScreen
import com.motionssalt.saltygraphics.ui.export.ExportScreen
import com.motionssalt.saltygraphics.ui.home.HomeScreen
import com.motionssalt.saltygraphics.ui.newproject.NewProjectScreen
import com.motionssalt.saltygraphics.ui.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = ProjectRepository(applicationContext)
        val settingsManager = SettingsManager(applicationContext)
        val deviceInfoProvider = DeviceInfoProvider(applicationContext)

        setContent {
            val uiScale by settingsManager.uiScale.collectAsState()

            SaltyGraphicsTheme(
                darkTheme = true,
                dynamicColor = true,
                uiScale = uiScale
            ) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            repository = repository,
                            onNavigateToNewProject = { navController.navigate(Screen.NewProject.route) },
                            onNavigateToEditor = { projectId ->
                                navController.navigate(Screen.Editor.createRoute(projectId))
                            },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            settingsManager = settingsManager,
                            deviceInfoProvider = deviceInfoProvider,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.NewProject.route) {
                        NewProjectScreen(
                            repository = repository,
                            onNavigateBack = { navController.popBackStack() },
                            onProjectCreated = { projectId ->
                                navController.navigate(Screen.Editor.createRoute(projectId)) {
                                    popUpTo(Screen.Home.route)
                                }
                            }
                        )
                    }

                    composable(
                        route = Screen.Editor.route,
                        arguments = listOf(
                            navArgument("projectId") { type = NavType.StringType },
                            navArgument("groupId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                        val groupId = backStackEntry.arguments?.getString("groupId")

                        EditorScreen(
                            projectId = projectId,
                            initialGroupId = groupId,
                            repository = repository,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToExport = { id ->
                                navController.navigate(Screen.Export.createRoute(id))
                            },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                        )
                    }

                    composable(
                        route = Screen.Export.route,
                        arguments = listOf(
                            navArgument("projectId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                        ExportScreen(
                            projectId = projectId,
                            repository = repository,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
