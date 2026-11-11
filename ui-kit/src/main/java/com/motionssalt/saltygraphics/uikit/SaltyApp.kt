package com.motionssalt.saltygraphics.uikit
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.motionssalt.saltygraphics.data.*
import com.motionssalt.saltygraphics.navigation.Screen
import com.motionssalt.saltygraphics.theme.*
import com.motionssalt.saltygraphics.ui.editor.EditorScreen
import com.motionssalt.saltygraphics.ui.export.ExportScreen
import com.motionssalt.saltygraphics.ui.home.HomeScreen
import com.motionssalt.saltygraphics.ui.newproject.NewProjectScreen
import com.motionssalt.saltygraphics.ui.settings.SettingsScreen
import com.motionssalt.saltygraphics.uikit.registry.*

@Composable
fun SaltyGraphicsApp(repository: ProjectRepository, settings: SettingsManager, devices: DeviceInfoProvider, registry: FeatureRegistry) {
    val theme by settings.theme.collectAsState()
    val uiScale by settings.uiScale.collectAsState()
    CompositionLocalProvider(LocalFeatureRegistry provides registry) {
        SaltyGraphicsTheme(theme.isDark(isSystemInDarkTheme()),true,uiScale) {
            Surface(Modifier.fillMaxSize().safeDrawingPadding(),color=MaterialTheme.colorScheme.background) {
                val nav = rememberNavController()
                NavHost(nav,Screen.Home.route) {
                    composable(Screen.Home.route) { RegisteredScreen(AppScreenId.HOME,repository,settings,devices,
                        onNew={nav.navigate(Screen.NewProject.route)},onEditor={nav.navigate(Screen.Editor.createRoute(it))},
                        onSettings={nav.navigate(Screen.Settings.route)},onBack={nav.popBackStack()}) }
                    composable(Screen.Settings.route) { RegisteredScreen(AppScreenId.SETTINGS,repository,settings,devices,
                        onNew={},onEditor={},onSettings={},onBack={nav.popBackStack()}) }
                    composable(Screen.NewProject.route) { RegisteredScreen(AppScreenId.NEW_PROJECT,repository,settings,devices,
                        onNew={},onEditor={nav.navigate(Screen.Editor.createRoute(it)){popUpTo(Screen.Home.route)}},
                        onSettings={},onBack={nav.popBackStack()}) }
                    composable(Screen.Editor.route,arguments=listOf(navArgument("projectId"){type=NavType.StringType},
                        navArgument("groupId"){type=NavType.StringType;nullable=true;defaultValue=null})) {
                        EditorScreen(it.arguments?.getString("projectId")?:"",it.arguments?.getString("groupId"),repository,
                            {nav.popBackStack()},{id->nav.navigate(Screen.Export.createRoute(id))},{nav.navigate(Screen.Settings.route)})
                    }
                    composable(Screen.Export.route,arguments=listOf(navArgument("projectId"){type=NavType.StringType})) {
                        RegisteredScreen(AppScreenId.EXPORT,repository,settings,devices,it.arguments?.getString("projectId")?:"",
                            onNew={},onEditor={},onSettings={},onBack={nav.popBackStack()})
                    }
                }
            }
        }
    }
}
@Composable
private fun RegisteredScreen(id: AppScreenId,repository: ProjectRepository,settings: SettingsManager,devices: DeviceInfoProvider,
    projectId: String="",onNew:()->Unit,onEditor:(String)->Unit,onSettings:()->Unit,onBack:()->Unit) {
    when(ScreenRegistry.get(id).componentId) {
        "screen.home" -> HomeScreen(repository,onNew,onEditor,onSettings)
        "screen.settings" -> SettingsScreen(settings,devices,onBack)
        "screen.new_project" -> NewProjectScreen(repository,onBack,onEditor)
        "screen.export" -> ExportScreen(projectId,repository,onBack)
        else -> error("Unregistered screen ${id.name}")
    }
}
