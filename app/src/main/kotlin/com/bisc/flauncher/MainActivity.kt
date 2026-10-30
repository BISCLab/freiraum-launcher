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
import com.bisc.flauncher.ui.settings.SettingsPageAppearanceScreen
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
import com.bisc.flauncher.ui.theme.ThemePreference
import com.bisc.flauncher.util.SystemWidgetHostManager
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var widgetHostManager: SystemWidgetHostManager

    @Inject lateinit var appIconLoader: com.bisc.flauncher.util.AppIconLoader

    @Inject lateinit var themeRepository: com.bisc.flauncher.data.repository.ThemeRepository

    @Inject lateinit var settingsRepository: com.bisc.flauncher.data.settings.SettingsRepository

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

    // uiMode is declared in configChanges (manifest), so a device dark/light flip lands here
    // instead of recreating the Activity. Only matters for ThemePreference.SYSTEM - Light/Dark
    // ignore the device setting entirely, so reapplying for them here would just be a no-op
    // rewrite. See ThemeRepository.reapplyTheme for what "System follows the device live" means
    // in practice: it re-stamps the background and every box/card/widget/dock/panel's default
    // fill, the same as a manual Light/Dark/System switch in Settings does.
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        lifecycleScope.launch {
            if (settingsRepository.theme.first() == ThemePreference.SYSTEM) themeRepository.reapplyTheme()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleSettingsIntent(intent)
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val theme by settingsVm.theme.collectAsState()
            val font by settingsVm.fontPreference.collectAsState()
            val globalIconSizePercent by settingsVm.iconSizePercent.collectAsState()
            val globalIconShape by settingsVm.iconShape.collectAsState()
            val appOverridesForIconSize by settingsVm.appOverrides.collectAsState()
            val showSettingsTips by settingsVm.showSettingsTips.collectAsState()
            val accentColor by settingsVm.accentColor.collectAsState()
            // New lambda identity whenever either input changes, so LocalIconSizeResolver consumers
            // (AppIconCell) recompose - a StateFlow's plain .value read wouldn't do that on its own.
            val iconSizeResolver = androidx.compose.runtime.remember(globalIconSizePercent, appOverridesForIconSize) {
                { componentName: String ->
                    val percent = appOverridesForIconSize[componentName]?.iconSizePercent ?: globalIconSizePercent
                    percent / 100f
                }
            }
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
                com.bisc.flauncher.ui.home.LocalAppIconLoader provides appIconLoader,
                com.bisc.flauncher.ui.home.LocalIconSizeResolver provides iconSizeResolver,
                com.bisc.flauncher.ui.home.LocalIconShape provides globalIconShape,
                com.bisc.flauncher.ui.settings.LocalShowSettingsTips provides showSettingsTips
            ) {
            FLauncherTheme(themePreference = theme, fontPreference = font, accentColor = accentColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onNavigateToSettings = { route ->
                                    // Deeper screens open on top of the settings start page, so back
                                    // and the breadcrumbs lead through it as usual.
                                    if (route != "settings") navController.navigate("settings")
                                    navController.navigate(route)
                                },
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
                            SettingsSearchFullScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/search/$sub") }
                            )
                        }
                        com.bisc.flauncher.ui.settings.SearchElement.entries.forEach { element ->
                            composable("settings/search/${element.route}") {
                                com.bisc.flauncher.ui.settings.SettingsSearchElementScreen(
                                    element = element,
                                    navController = navController,
                                    onBack = { navController.popBackStack() },
                                    onNavigate = { sub -> navController.navigate("settings/search/${element.route}/$sub") }
                                )
                            }
                            composable("settings/search/${element.route}/background") {
                                com.bisc.flauncher.ui.settings.SettingsSearchElementBackgroundScreen(element = element, navController = navController, onBack = { navController.popBackStack() })
                            }
                            composable("settings/search/${element.route}/border") {
                                com.bisc.flauncher.ui.settings.SettingsSearchElementBorderScreen(element = element, navController = navController, onBack = { navController.popBackStack() })
                            }
                        }
                        composable("settings/search/appearance") {
                            com.bisc.flauncher.ui.settings.SettingsSearchAppearanceScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/search/$sub") }
                            )
                        }
                        composable("settings/search/page") {
                            com.bisc.flauncher.ui.settings.SettingsSearchPageScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/search/keyboard") {
                            com.bisc.flauncher.ui.settings.SettingsSearchKeyboardScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/search/behavior") {
                            com.bisc.flauncher.ui.settings.SettingsSearchBehaviorScreen(navController = navController, onBack = { navController.popBackStack() })
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
                                        page.kind == com.bisc.flauncher.data.model.LauncherPageKind.APP_GRID ->
                                            navController.navigate("settings/app_grid")
                                    }
                                }
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
                                pageId = pageId,
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/pages/$pageId/home/$sub") }
                            )
                        }
                        composable(
                            "settings/pages/{pageId}/appearance",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsPageAppearanceScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/pages/{pageId}/home/appearance",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            SettingsPageAppearanceScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
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
                            "settings/pages/{pageId}/rows/{rowId}/elementbox",
                            arguments = listOf(
                                navArgument("pageId") { type = NavType.LongType },
                                navArgument("rowId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                            val rowId = backStackEntry.arguments?.getLong("rowId") ?: return@composable
                            com.bisc.flauncher.ui.settings.SettingsElementBoxEditScreen(
                                pageId = pageId,
                                rowId = rowId,
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings/app_overview") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/app_overview/$sub") }
                            )
                        }
                        composable("settings/app_overview/appearance") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewAppearanceHubScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/app_overview/$sub") }
                            )
                        }
                        composable("settings/app_overview/page") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewPageScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/app_overview/search_button") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewSearchButtonScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/app_overview/behavior") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewBehaviorScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/app_overview/section_letters") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewSectionLettersScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/app_overview/az_index") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewAzIndexScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/app_grid") {
                            com.bisc.flauncher.ui.settings.SettingsAppGridScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/app_grid/$sub") }
                            )
                        }
                        composable("settings/app_grid/appearance") {
                            com.bisc.flauncher.ui.settings.SettingsAppGridAppearanceHubScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/app_grid/$sub") }
                            )
                        }
                        composable("settings/app_grid/page") {
                            com.bisc.flauncher.ui.settings.SettingsAppGridPageScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/app_grid/grid") {
                            com.bisc.flauncher.ui.settings.SettingsAppGridGridScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/app_grid/search_field") {
                            com.bisc.flauncher.ui.settings.SettingsAppGridSearchFieldScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/app_grid/behavior") {
                            com.bisc.flauncher.ui.settings.SettingsAppGridBehaviorScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/permissions") {
                            SettingsPermissionsScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/gestures") {
                            SettingsGesturesScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/apps") {
                            SettingsAppsScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { route -> navController.navigate("settings/apps/$route") }
                            )
                        }
                        composable("settings/appearance/icons") {
                            com.bisc.flauncher.ui.settings.SettingsAppsGlobalScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/about") {
                            com.bisc.flauncher.ui.settings.SettingsAboutScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/system") {
                            com.bisc.flauncher.ui.settings.SettingsSystemScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { route -> navController.navigate("settings/$route") }
                            )
                        }
                        composable("settings/folder") {
                            SettingsFolderScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/folder/global") {
                            com.bisc.flauncher.ui.settings.SettingsFolderGlobalScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/folder/global/$sub") }
                            )
                        }
                        composable("settings/folder/global/appearance") {
                            com.bisc.flauncher.ui.settings.SettingsFolderGlobalAppearanceScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/folder/global/border") {
                            com.bisc.flauncher.ui.settings.SettingsFolderGlobalBorderScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable("settings/folder/global/visibility") {
                            com.bisc.flauncher.ui.settings.SettingsFolderGlobalVisibilityScreen(navController = navController, onBack = { navController.popBackStack() })
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
                        composable("settings/elements/actionbuttons") {
                            com.bisc.flauncher.ui.settings.SettingsActionButtonsScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "settings/elements/actionbuttons/{buttonId}",
                            arguments = listOf(navArgument("buttonId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val buttonId = backStackEntry.arguments?.getLong("buttonId") ?: return@composable
                            com.bisc.flauncher.ui.settings.SettingsActionButtonDetailScreen(buttonId = buttonId, navController = navController, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
            }
        }
    }
}
