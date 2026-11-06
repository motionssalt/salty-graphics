package com.motionssalt.saltygraphics

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.motionssalt.saltygraphics.data.DeviceInfoProvider
import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.data.SettingsManager
import com.motionssalt.saltygraphics.navigation.Screen
import com.motionssalt.saltygraphics.theme.*
import com.motionssalt.saltygraphics.ui.editor.EditorScreen
import com.motionssalt.saltygraphics.ui.export.ExportScreen
import com.motionssalt.saltygraphics.ui.home.HomeScreen
import com.motionssalt.saltygraphics.ui.newproject.NewProjectScreen
import com.motionssalt.saltygraphics.ui.settings.SettingsScreen
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install uncaught crash logger
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("SaltyGraphics", "Uncaught exception", throwable)
                val crashFile = File(applicationContext.filesDir, "last_crash.txt")
                crashFile.writeText(throwable.stackTraceToString())
            } catch (_: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Defensive Edge-to-Edge setup (handles OEM ROM quirks)
        runCatching {
            enableEdgeToEdge()
        }

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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SaltyDarkBackground
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
}
