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
import com.bisc.flauncher.ui.settings.SettingsSearchFullScreen
import com.bisc.flauncher.ui.settings.SettingsSidebarScreen
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
                        composable("settings/sidebar") {
                            SettingsSidebarScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/search") {
                            SettingsSearchFullScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/pages") {
                            SettingsPagesScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onOpenPage = { page ->
                                    when (page.kind) {
                                        com.bisc.flauncher.data.model.LauncherPageKind.ROWS -> navController.navigate("settings/pages/${page.id}")
                                        com.bisc.flauncher.data.model.LauncherPageKind.APP_LIST -> navController.navigate("settings/app_overview")
                                        com.bisc.flauncher.data.model.LauncherPageKind.SEARCH -> navController.navigate("settings/search")
                                        com.bisc.flauncher.data.model.LauncherPageKind.SIDEBAR -> navController.navigate("settings/sidebar")
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
                    }
                }
            }
            }
        }
    }
}
