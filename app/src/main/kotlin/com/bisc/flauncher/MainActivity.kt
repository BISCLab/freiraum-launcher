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
import androidx.navigation.compose.currentBackStackEntryAsState
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
import com.bisc.flauncher.ui.settings.SettingsDockScreen
import com.bisc.flauncher.ui.settings.SettingsDockVisibilityScreen
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
    // Remembers where each touch starts, so apps and overlays can zoom out of that spot.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
            com.bisc.flauncher.ui.TouchOrigin.x = ev.rawX
            com.bisc.flauncher.ui.TouchOrigin.y = ev.rawY
        }
        return super.dispatchTouchEvent(ev)
    }

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
            val lockPortrait by settingsVm.lockPortrait.collectAsState()
            val settingsThemeMode by settingsVm.settingsThemeMode.collectAsState()
            val categoryIcons by settingsVm.categoryIcons.collectAsState()
            val chosenMotion by settingsVm.motionLevel.collectAsState()
            val motionPrefs by settingsVm.motionPrefs.collectAsState()
            val designEffects by settingsVm.designEffects.collectAsState()
            // Android's "Remove animations" wins over the launcher's own choice; re-read on every return.
            var systemAnimationsOff by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.bisc.flauncher.ui.systemAnimationsOff(this)) }
            androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
                systemAnimationsOff = com.bisc.flauncher.ui.systemAnimationsOff(this@MainActivity)
                onPauseOrDispose { }
            }
            val motion = if (systemAnimationsOff) com.bisc.flauncher.ui.MotionLevel.OFF else chosenMotion
            com.bisc.flauncher.ui.TouchOrigin.launchZoom = motion != com.bisc.flauncher.ui.MotionLevel.OFF && motionPrefs.appZoom
            androidx.compose.runtime.LaunchedEffect(lockPortrait) {
                requestedOrientation = if (lockPortrait) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
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
            // The Home button leaves the launcher's own settings too, like any other app.
            androidx.compose.runtime.LaunchedEffect(homeButtonPressCount) {
                if (homeButtonPressCount > 0) navController.popBackStack("home", inclusive = false)
            }

            val launcherDark = when (theme) {
                com.bisc.flauncher.ui.theme.ThemePreference.DARK -> true
                com.bisc.flauncher.ui.theme.ThemePreference.LIGHT -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            // In the settings, the bar icons follow the settings' own color mode.
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            val darkTheme = if (currentRoute?.startsWith("settings") == true)
                com.bisc.flauncher.ui.theme.settingsDark(settingsThemeMode, launcherDark) else launcherDark
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
                com.bisc.flauncher.ui.settings.LocalShowSettingsTips provides showSettingsTips,
                com.bisc.flauncher.ui.LocalMotion provides motion,
                com.bisc.flauncher.ui.LocalMotionPrefs provides motionPrefs,
                com.bisc.flauncher.ui.theme.LocalDesignEffects provides designEffects,
                com.bisc.flauncher.ui.theme.LocalSettingsThemeMode provides settingsThemeMode,
                com.bisc.flauncher.ui.home.search.LocalCategoryIcons provides categoryIcons
            ) {
            FLauncherTheme(themePreference = theme, fontPreference = font, accentColor = accentColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navMotion = if (motionPrefs.settingsSlide) motion else com.bisc.flauncher.ui.MotionLevel.OFF
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = {
                            if (navMotion == com.bisc.flauncher.ui.MotionLevel.OFF) androidx.compose.animation.EnterTransition.None
                            else androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(320, easing = com.bisc.flauncher.ui.EmphasizedEasing)) { it / 5 } +
                                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220))
                        },
                        exitTransition = {
                            if (navMotion == com.bisc.flauncher.ui.MotionLevel.OFF) androidx.compose.animation.ExitTransition.None
                            else androidx.compose.animation.scaleOut(androidx.compose.animation.core.tween(320, easing = com.bisc.flauncher.ui.EmphasizedEasing), targetScale = 0.96f) +
                                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220))
                        },
                        popEnterTransition = {
                            if (navMotion == com.bisc.flauncher.ui.MotionLevel.OFF) androidx.compose.animation.EnterTransition.None
                            else androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(320, easing = com.bisc.flauncher.ui.EmphasizedEasing), initialScale = 0.96f) +
                                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220))
                        },
                        popExitTransition = {
                            if (navMotion == com.bisc.flauncher.ui.MotionLevel.OFF) androidx.compose.animation.ExitTransition.None
                            else androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(280, easing = com.bisc.flauncher.ui.EmphasizedEasing)) { it / 5 } +
                                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))
                        }
                    ) {
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
                        settingsComposable("settings") {
                            SettingsListScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { route -> navController.navigate("settings/$route") }
                            )
                        }
                        settingsComposable("settings/appearance") {
                            SettingsAppearanceScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { route -> navController.navigate("settings/$route") }
                            )
                        }
                        settingsComposable("settings/appearance/background") {
                            com.bisc.flauncher.ui.settings.SettingsGlobalBackgroundScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/search") {
                            SettingsSearchFullScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/search/$sub") }
                            )
                        }
                        com.bisc.flauncher.ui.settings.SearchElement.entries.forEach { element ->
                            settingsComposable("settings/search/${element.route}") {
                                com.bisc.flauncher.ui.settings.SettingsSearchElementScreen(
                                    element = element,
                                    navController = navController,
                                    onBack = { navController.popBackStack() },
                                    onNavigate = { sub -> navController.navigate("settings/search/${element.route}/$sub") }
                                )
                            }
                        }
                        settingsComposable("settings/search/appearance") {
                            com.bisc.flauncher.ui.settings.SettingsSearchAppearanceScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/search/$sub") }
                            )
                        }
                        settingsComposable("settings/search/page") {
                            com.bisc.flauncher.ui.settings.SettingsSearchPageScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/search/keyboard") {
                            com.bisc.flauncher.ui.settings.SettingsSearchKeyboardScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/search/favorites") {
                            com.bisc.flauncher.ui.settings.SettingsSearchFavoritesScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/search/web") {
                            com.bisc.flauncher.ui.settings.SettingsSearchWebScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/search/ai") {
                            com.bisc.flauncher.ui.settings.SettingsSearchWebScreen(navController = navController, onBack = { navController.popBackStack() }, part = com.bisc.flauncher.ui.settings.CommandPart.AI)
                        }
                        settingsComposable("settings/search/behavior") {
                            com.bisc.flauncher.ui.settings.SettingsSearchBehaviorScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/pages") {
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
                        settingsComposable(
                            "settings/pages/{pageId}",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@settingsComposable
                            SettingsPageRowsScreen(
                                pageId = pageId,
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onOpenRow = { rowId -> navController.navigate("settings/pages/$pageId/rows/$rowId") }
                            )
                        }
                        settingsComposable(
                            "settings/pages/{pageId}/home",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@settingsComposable
                            SettingsHomeScreenHub(
                                pageId = pageId,
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/pages/$pageId/home/$sub") }
                            )
                        }
                        settingsComposable(
                            "settings/pages/{pageId}/appearance",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@settingsComposable
                            SettingsPageAppearanceScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable(
                            "settings/pages/{pageId}/home/appearance",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@settingsComposable
                            SettingsPageAppearanceScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable(
                            "settings/pages/{pageId}/home/layout",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@settingsComposable
                            SettingsHomeLayoutScreen(
                                pageId = pageId,
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onOpenRow = { rowId -> navController.navigate("settings/pages/$pageId/rows/$rowId") },
                                onOpenPanel = { navController.navigate("settings/pages/$pageId/home/panel") },
                                onOpenDock = { navController.navigate("settings/pages/$pageId/home/layout/dock") }
                            )
                        }
                        settingsComposable(
                            "settings/pages/{pageId}/home/panel",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@settingsComposable
                            SettingsHomePanelScreen(
                                pageId = pageId,
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/pages/$pageId/home/panel/$sub") }
                            )
                        }
                        settingsComposable(
                            "settings/pages/{pageId}/home/panel/elements",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@settingsComposable
                            SettingsPanelElementsScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable(
                            "settings/pages/{pageId}/home/layout/dock",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@settingsComposable
                            SettingsDockScreen(
                                pageId = pageId,
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/pages/$pageId/home/layout/dock/$sub") }
                            )
                        }
                        settingsComposable(
                            "settings/pages/{pageId}/home/layout/dock/visibility",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@settingsComposable
                            SettingsDockVisibilityScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable(
                            "settings/pages/{pageId}/home/layout/dock/elements",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@settingsComposable
                            SettingsDockElementsScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable(
                            "settings/pages/{pageId}/icon",
                            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@settingsComposable
                            SettingsPageIconScreen(pageId = pageId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable(
                            "settings/pages/{pageId}/rows/{rowId}",
                            arguments = listOf(
                                navArgument("pageId") { type = NavType.LongType },
                                navArgument("rowId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@settingsComposable
                            val rowId = backStackEntry.arguments?.getLong("rowId") ?: return@settingsComposable
                            SettingsWidgetRowEditScreen(
                                pageId = pageId,
                                rowId = rowId,
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        settingsComposable(
                            "settings/pages/{pageId}/rows/{rowId}/elementbox",
                            arguments = listOf(
                                navArgument("pageId") { type = NavType.LongType },
                                navArgument("rowId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@settingsComposable
                            val rowId = backStackEntry.arguments?.getLong("rowId") ?: return@settingsComposable
                            com.bisc.flauncher.ui.settings.SettingsElementBoxEditScreen(
                                pageId = pageId,
                                rowId = rowId,
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        settingsComposable("settings/app_overview") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/app_overview/$sub") }
                            )
                        }
                        settingsComposable("settings/app_overview/appearance") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewAppearanceHubScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/app_overview/$sub") }
                            )
                        }
                        settingsComposable("settings/app_overview/page") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewPageScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/app_overview/search_button") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewSearchButtonScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/app_overview/search_field") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewSearchFieldScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/app_overview/behavior") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewBehaviorScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/app_overview/section_letters") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewSectionLettersScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/app_overview/az_index") {
                            com.bisc.flauncher.ui.settings.SettingsAppOverviewAzIndexScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/appearance/designs") {
                            com.bisc.flauncher.ui.settings.SettingsDesignsScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/appearance/widget_paging") {
                            com.bisc.flauncher.ui.settings.SettingsWidgetPagingScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/app_grid") {
                            com.bisc.flauncher.ui.settings.SettingsAppGridScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/app_grid/$sub") }
                            )
                        }
                        settingsComposable("settings/app_grid/appearance") {
                            com.bisc.flauncher.ui.settings.SettingsAppGridAppearanceHubScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/app_grid/$sub") }
                            )
                        }
                        settingsComposable("settings/app_grid/page") {
                            com.bisc.flauncher.ui.settings.SettingsAppGridPageScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/app_grid/grid") {
                            com.bisc.flauncher.ui.settings.SettingsAppGridGridScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/app_grid/search_field") {
                            com.bisc.flauncher.ui.settings.SettingsAppGridSearchFieldScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/app_grid/behavior") {
                            com.bisc.flauncher.ui.settings.SettingsAppGridBehaviorScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/permissions") {
                            SettingsPermissionsScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/gestures") {
                            SettingsGesturesScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/apps") {
                            SettingsAppsScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { route -> navController.navigate("settings/apps/$route") }
                            )
                        }
                        settingsComposable("settings/appearance/icons") {
                            com.bisc.flauncher.ui.settings.SettingsAppsGlobalScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/about") {
                            com.bisc.flauncher.ui.settings.SettingsAboutScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/system") {
                            com.bisc.flauncher.ui.settings.SettingsSystemScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { route -> navController.navigate("settings/$route") }
                            )
                        }
                        settingsComposable("settings/appearance/colors") {
                            com.bisc.flauncher.ui.settings.SettingsColorsScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/appearance/corners") {
                            com.bisc.flauncher.ui.settings.SettingsCornersScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/wallpaper_packs") {
                            com.bisc.flauncher.ui.settings.SettingsWallpaperPacksScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/appearance/statusbar") {
                            com.bisc.flauncher.ui.settings.SettingsStatusBarScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/appearance/animations") {
                            com.bisc.flauncher.ui.settings.SettingsAnimationsScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/folder") {
                            SettingsFolderScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/folder/global") {
                            com.bisc.flauncher.ui.settings.SettingsFolderGlobalScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { sub -> navController.navigate("settings/folder/global/$sub") }
                            )
                        }
                        settingsComposable("settings/folder/global/visibility") {
                            com.bisc.flauncher.ui.settings.SettingsFolderGlobalVisibilityScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable(
                            "settings/folder/{folderId}",
                            arguments = listOf(navArgument("folderId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@settingsComposable
                            SettingsFolderEditScreen(folderId = folderId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable(
                            "settings/folder/{folderId}/widget/{itemId}",
                            arguments = listOf(navArgument("folderId") { type = NavType.LongType }, navArgument("itemId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@settingsComposable
                            val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@settingsComposable
                            com.bisc.flauncher.ui.settings.SettingsFolderWidgetEditScreen(folderId = folderId, itemId = itemId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable(
                            "settings/folder/{folderId}/icon",
                            arguments = listOf(navArgument("folderId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@settingsComposable
                            SettingsFolderIconScreen(folderId = folderId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/elements") {
                            com.bisc.flauncher.ui.settings.SettingsElementsScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/elements/addresses") {
                            com.bisc.flauncher.ui.settings.SettingsAddressesScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable(
                            "settings/elements/addresses/{targetId}",
                            arguments = listOf(navArgument("targetId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val targetId = backStackEntry.arguments?.getLong("targetId") ?: return@settingsComposable
                            com.bisc.flauncher.ui.settings.SettingsAddressDetailScreen(targetId = targetId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/elements/links") {
                            com.bisc.flauncher.ui.settings.SettingsLinksScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable(
                            "settings/elements/links/{websiteId}",
                            arguments = listOf(navArgument("websiteId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val websiteId = backStackEntry.arguments?.getLong("websiteId") ?: return@settingsComposable
                            com.bisc.flauncher.ui.settings.SettingsLinkDetailScreen(websiteId = websiteId, navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable("settings/elements/actionbuttons") {
                            com.bisc.flauncher.ui.settings.SettingsActionButtonsScreen(navController = navController, onBack = { navController.popBackStack() })
                        }
                        settingsComposable(
                            "settings/elements/actionbuttons/{buttonId}",
                            arguments = listOf(navArgument("buttonId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val buttonId = backStackEntry.arguments?.getLong("buttonId") ?: return@settingsComposable
                            com.bisc.flauncher.ui.settings.SettingsActionButtonDetailScreen(buttonId = buttonId, navController = navController, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
            }
        }
    }
}

/** A settings destination: drawn in the settings' own color mode. */
private fun androidx.navigation.NavGraphBuilder.settingsComposable(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    content: @androidx.compose.runtime.Composable androidx.compose.animation.AnimatedContentScope.(androidx.navigation.NavBackStackEntry) -> Unit
) = composable(route, arguments) { entry -> com.bisc.flauncher.ui.theme.SettingsThemed { content(entry) } }
