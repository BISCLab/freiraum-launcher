package com.bisc.flauncher

import kotlinx.coroutines.flow.first
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bisc.flauncher.ui.home.HomeScreen
import com.bisc.flauncher.ui.settings.SettingsAppearanceScreen
import com.bisc.flauncher.ui.settings.SettingsAppsScreen
import com.bisc.flauncher.ui.settings.SettingsPermissionsScreen
import com.bisc.flauncher.ui.settings.SettingsFolderEditScreen
import com.bisc.flauncher.ui.settings.SettingsFolderIconScreen
import com.bisc.flauncher.ui.settings.SettingsFolderScreen
import com.bisc.flauncher.ui.settings.SettingsGesturesScreen
import com.bisc.flauncher.ui.settings.SettingsListScreen
import com.bisc.flauncher.ui.settings.SettingsPageRowsScreen
import com.bisc.flauncher.ui.settings.SettingsPageIconScreen
import com.bisc.flauncher.ui.settings.SettingsPagesScreen
import com.bisc.flauncher.ui.settings.SettingsHomeScreenHub
import com.bisc.flauncher.ui.settings.SettingsHomeAppearanceScreen
import com.bisc.flauncher.ui.settings.SettingsHomeLayoutScreen
import com.bisc.flauncher.ui.settings.SettingsHomePanelScreen
import com.bisc.flauncher.ui.settings.SettingsPanelBackgroundScreen
import com.bisc.flauncher.ui.settings.SettingsPanelBorderScreen
import com.bisc.flauncher.ui.settings.SettingsDockScreen
import com.bisc.flauncher.ui.settings.SettingsDockVisibilityScreen
import com.bisc.flauncher.ui.settings.SettingsDockBackgroundScreen
import com.bisc.flauncher.ui.settings.SettingsDockBorderScreen
import com.bisc.flauncher.ui.settings.SettingsDockElementsScreen
import com.bisc.flauncher.ui.settings.SettingsPanelElementsScreen
import com.bisc.flauncher.ui.settings.SettingsSearchFullScreen
import com.bisc.flauncher.ui.settings.SettingsViewModel
import com.bisc.flauncher.ui.settings.SettingsWidgetRowEditScreen
import com.bisc.flauncher.ui.theme.FLauncherTheme
import com.bisc.flauncher.util.SystemWidgetHostManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var widgetHostManager: SystemWidgetHostManager

    @Inject lateinit var appIconLoader: com.bisc.flauncher.util.AppIconLoader

    private var homeButtonPressCount by mutableIntStateOf(0)

    // Incremented when the system requests the settings screen (app info, launcher shortcut).
    private var openSettingsRequest by mutableIntStateOf(0)

    private fun handleSettingsIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_APPLICATION_PREFERENCES) openSettingsRequest++
    }

    // Applies the in-app language choice on Android 12 and older.
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.bisc.flauncher.util.AppLanguage.wrap(newBase))
    }

    override fun onStart() {
        super.onStart()
        widgetHostManager.startListening()
    }

    override fun onStop() {
        widgetHostManager.stopListening()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homeButtonPressCount++
        handleSettingsIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleSettingsIntent(intent)
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val theme by settingsVm.theme.collectAsState()
            val font by settingsVm.fontPreference.collectAsState()
            val navController = rememberNavController()
            androidx.compose.runtime.LaunchedEffect(openSettingsRequest) {
                if (openSettingsRequest > 0) {
                    val route = navController.currentBackStackEntryFlow.first().destination.route
                    if (route == null || !route.startsWith("settings")) navController.navigate("settings")
                }
            }

            val darkTheme = when (theme) {
                com.bisc.flauncher.ui.theme.ThemePreference.DARK -> true
                com.bisc.flauncher.ui.theme.ThemePreference.LIGHT -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            androidx.compose.runtime.DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = androidx.activity.SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = androidx.activity.SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { darkTheme }
                )
                onDispose { }
            }
            androidx.compose.runtime.CompositionLocalProvider(
                com.bisc.flauncher.ui.home.LocalAppIconLoader provides appIconLoader
            ) {
            FLauncherTheme(themePreference = theme, fontPreference = font) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onNavigateToSettings = { navController.navigate("settings") },
                                homeButtonPressCount = homeButtonPressCount
                            )
                        }
                        composable("settings") {
                            SettingsListScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { route -> navController.navigate("settings/$route") }
                            )
                        }
                        composable("settings/appearance") {
                            SettingsAppearanceScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { route -> navController.navigate("settings/$route") }
                            )
                        }
                        composable("settings/appearance/background") {
                            com.bisc.flauncher.ui.settings.SettingsGlobalBackgroundScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/search") {
                            SettingsSearchFullScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/pages") {
                            SettingsPagesScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onOpenPage = { page ->
                                    when {
                                        page.kind == com.bisc.flauncher.data.model.LauncherPageKind.ROWS && page.isHome ->
                                            navController.navigate("settings/pages/${page.id}/home")
                                        page.kind == com.bisc.flauncher.data.model.LauncherPageKind.ROWS ->
                                            navController.navigate("settings/pages/${page.id}")
                                        page.kind == com.bisc.flauncher.data.model.LauncherPageKind.APP_LIST ->
                                            navController.navigate("settings/app_overview")
                                        page.kind == com.bisc.flauncher.data.model.LauncherPageKind.SEARCH ->
                                            navController.navigate("settings/search")
                                        // SIDEBAR (the Panel's page identity) is filtered out of this
                                        // list — its settings live under Home > Panel instead, since
                                        // it's a Home-page bookend, not a standalone page to configure.
                                        page.kind == com.bisc.flauncher.data.model.LauncherPageKind.SIDEBAR -> Unit
                                    }
                                },
                                onOpenIcon = { pageId -> navController.navigate("settings/pages/$pageId/icon") }
                            )
                        }
                        composable(
                            "settings/pages/{pageId}",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsPageRowsScreen(
                                pageId = pageId,
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onOpenRow = { rowId -> navController.navigate("settings/pages/$pageId/rows/$rowId") }
                            )
                        }
                        composable(
                            "settings/pages/{pageId}/home",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsHomeScreenHub(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/pages/$pageId/home/$sub") }
                            )
                        }
                        composable(
                            "settings/pages/{pageId}/home/appearance",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsHomeAppearanceScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/pages/{pageId}/home/layout",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsHomeLayoutScreen(
                                pageId = pageId,
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onOpenRow = { rowId -> navController.navigate("settings/pages/$pageId/rows/$rowId") },
                                onOpenPanel = { navController.navigate("settings/pages/$pageId/home/panel") },
                                onOpenDock = { navController.navigate("settings/pages/$pageId/home/layout/dock") }
                            )
                        }
                        composable(
                            "settings/pages/{pageId}/home/panel",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsHomePanelScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/pages/$pageId/home/panel/$sub") }
                            )
                        }
                        composable(
                            "settings/pages/{pageId}/home/panel/background",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsPanelBackgroundScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/pages/{pageId}/home/panel/border",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsPanelBorderScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/pages/{pageId}/home/panel/elements",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsPanelElementsScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/pages/{pageId}/home/layout/dock",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsDockScreen(
                                pageId = pageId,
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/pages/$pageId/home/layout/dock/$sub") }
                            )
                        }
                        composable(
                            "settings/pages/{pageId}/home/layout/dock/visibility",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsDockVisibilityScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/pages/{pageId}/home/layout/dock/background",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsDockBackgroundScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/pages/{pageId}/home/layout/dock/border",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsDockBorderScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/pages/{pageId}/home/layout/dock/elements",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsDockElementsScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/pages/{pageId}/icon",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsPageIconScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/pages/{pageId}/rows/{rowId}",
                            arguments = listOf(
                                navArgument("pageId") { type = NavType.LongType },
                                navArgument("rowId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            val rowId = backStackEntry.arguments?.getLong("rowId") ?: return@composable
                            SettingsWidgetRowEditScreen(
                                pageId = pageId,
                                rowId = rowId,
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            "settings/pages/{pageId}/rows/{rowId}/apps",
                            arguments = listOf(
                                navArgument("pageId") { type = NavType.LongType },
                                navArgument("rowId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            val rowId = backStackEntry.arguments?.getLong("rowId") ?: return@composable
                            com.bisc.flauncher.ui.settings.SettingsAppsRowEditScreen(
                                pageId = pageId,
                                rowId = rowId,
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings/app_overview") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/permissions") {
                            SettingsPermissionsScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/gestures") {
                            SettingsGesturesScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/apps") {
                            SettingsAppsScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/folder") {
                            SettingsFolderScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/folder/{folderId}",
                            arguments = listOf(navArgument("folderId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
                            SettingsFolderEditScreen(folderId = folderId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/folder/{folderId}/icon",
                            arguments = listOf(navArgument("folderId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
                            SettingsFolderIconScreen(folderId = folderId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/elements") {
                            com.bisc.flauncher.ui.settings.SettingsElementsScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/elements/addresses") {
                            com.bisc.flauncher.ui.settings.SettingsAddressesScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/elements/addresses/{targetId}",
                            arguments = listOf(navArgument("targetId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val targetId = backStackEntry.arguments?.getLong("targetId") ?: return@composable
                            com.bisc.flauncher.ui.settings.SettingsAddressDetailScreen(targetId = targetId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/elements/links") {
                            com.bisc.flauncher.ui.settings.SettingsLinksScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/elements/links/{websiteId}",
                            arguments = listOf(navArgument("websiteId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val websiteId = backStackEntry.arguments?.getLong("websiteId") ?: return@composable
                            com.bisc.flauncher.ui.settings.SettingsLinkDetailScreen(websiteId = websiteId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/elements/powerbuttons") {
                            com.bisc.flauncher.ui.settings.SettingsPowerButtonsScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/elements/powerbuttons/{buttonId}",
                            arguments = listOf(navArgument("buttonId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val buttonId = backStackEntry.arguments?.getLong("buttonId") ?: return@composable
                            com.bisc.flauncher.ui.settings.SettingsPowerButtonDetailScreen(buttonId = buttonId, navController = navController, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
            }
        }
    }
}
