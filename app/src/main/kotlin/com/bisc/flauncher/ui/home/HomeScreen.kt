package com.bisc.flauncher.ui.home

import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.luminance
import android.app.Activity
import android.content.ComponentName
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.ceil
import kotlin.math.floor
import com.bisc.flauncher.data.model.SearchKeyboardType
import com.bisc.flauncher.util.setStatusBarHidden
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.Note
import com.bisc.flauncher.data.model.ContactInfo
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.SidebarCategoryViewMode
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.AddSidebarAppsChooserSheet
import com.bisc.flauncher.ui.home.dock.ContactPickerSheet
import com.bisc.flauncher.ui.home.dock.AddWebsiteDialog
import com.bisc.flauncher.ui.home.dock.AppShortcutPickerDialog
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.ui.home.dock.DockDimens
import com.bisc.flauncher.ui.home.dock.ResolvedSidebarItem
import com.bisc.flauncher.ui.home.dock.SearchBar
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet
import com.bisc.flauncher.ui.home.dock.SidebarOverviewPanel
import com.bisc.flauncher.ui.home.search.AppOverviewContent
import com.bisc.flauncher.ui.home.search.automaticRailAppearance
import com.bisc.flauncher.ui.home.search.SearchOverlay
import com.bisc.flauncher.ui.settings.FolderCreatorSheet
import com.bisc.flauncher.ui.settings.SavedMapTargetPickerSheet
import com.bisc.flauncher.ui.settings.SavedWebsitePickerSheet
import com.bisc.flauncher.ui.home.widgets.NoteEditorDialog
import com.bisc.flauncher.util.launchApp
import com.bisc.flauncher.util.launchMapTarget
import com.bisc.flauncher.util.launchSettingsShortcut
import com.bisc.flauncher.util.openContact
import com.bisc.flauncher.util.openWebsite
import kotlinx.coroutines.launch

/** Hosts the paged home screen ([PageRowsColumn] per page). */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    homeButtonPressCount: Int = 0,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pages by viewModel.pages.collectAsState()
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val appEntriesByRow by viewModel.appEntriesByRow.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val resolveLabel = viewModel::resolveLabel
    val handedness by viewModel.handedness.collectAsState()
    val overviewEntry by viewModel.overviewEntry.collectAsState()
    var lastOverviewEntry by remember { mutableStateOf(overviewEntry) }
    LaunchedEffect(overviewEntry) { if (overviewEntry != null) lastOverviewEntry = overviewEntry }
    val searchOpen by viewModel.searchOpen.collectAsState()
    val appListOpen by viewModel.appListOpen.collectAsState()
    val appOverviewPageId by viewModel.appOverviewPageId.collectAsState()
    val appOverviewBackground by remember(appOverviewPageId) { viewModel.pageBackgroundConfig(appOverviewPageId ?: -1L) }
        .collectAsState(initial = BackgroundConfig())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val calculatorResult by viewModel.calculatorResult.collectAsState()
    val contactResults by viewModel.contactResults.collectAsState()
    val contactsPermissionGranted by viewModel.contactsPermissionGranted.collectAsState()
    val hasShortcutHostPermission by viewModel.hasShortcutHostPermission.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val folderItemsByFolder by viewModel.folderItemsByFolder.collectAsState()
    val openFolderPath by viewModel.openFolderPath.collectAsState()
    val folderOpen = openFolderPath.isNotEmpty()
    val folderHideStatusBar by viewModel.folderHideStatusBar.collectAsState()
    val folderHideDock by viewModel.folderHideDock.collectAsState()
    val dockSearchButtonAppearance by viewModel.dockSearchButtonAppearance.collectAsState()
    val quickHideStatusBar by viewModel.quickOverviewHideStatusBar.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val savedMapTargets by viewModel.savedMapTargets.collectAsState()
    val pinnedWebsites by viewModel.pinnedWebsites.collectAsState()
    val pinnedShortcuts by viewModel.pinnedShortcuts.collectAsState()
    val sidebarPinnedApps by viewModel.sidebarPinnedApps.collectAsState()
    val pinnedContacts by viewModel.pinnedContacts.collectAsState()
    val contactPickerOpen by viewModel.contactPickerOpen.collectAsState()
    val contactPickerList by viewModel.contactPickerList.collectAsState()
    val shortcutAppPickerOpen by viewModel.shortcutAppPickerOpen.collectAsState()
    val shortcutPickerForApp by viewModel.shortcutPickerForApp.collectAsState()
    val availableShortcuts by viewModel.availableShortcuts.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    val railAppearance by viewModel.searchAlphabetRailAppearance.collectAsState()
    val quickLaunchSlots by viewModel.quickLaunchSlots.collectAsState()
    val dockSingleTapAction by viewModel.dockSingleTapAction.collectAsState()
    val dockDoubleTapAction by viewModel.dockDoubleTapAction.collectAsState()
    val chargingAnimationEnabled by viewModel.chargingAnimationEnabled.collectAsState()
    val chargingAnimationColor by viewModel.chargingAnimationColor.collectAsState()
    val searchOverlayBackground by viewModel.searchOverlayBackground.collectAsState()
    val quickOverviewBackground by viewModel.quickOverviewBackground.collectAsState()
    val appListDisplayMode by viewModel.appListDisplayMode.collectAsState()
    val appListLayout by viewModel.appListLayout.collectAsState()
    val appListSearchButtonAppearance by viewModel.appListSearchButtonAppearance.collectAsState()
    val appListShowSearchButton by viewModel.appListShowSearchButton.collectAsState()
    val appListContentAppearance by viewModel.appListContentAppearance.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val sidebarPanelAppearance by viewModel.sidebarPanelAppearance.collectAsState()
    val folderCardAppearance by viewModel.folderCardAppearance.collectAsState()
    val keyboardAppearance by viewModel.keyboardAppearance.collectAsState()
    val searchResultCardAppearance by viewModel.searchResultCardAppearance.collectAsState()
    val sidebarItems by viewModel.sidebarItems.collectAsState()
    val searchShowAzIndex by viewModel.searchShowAzIndex.collectAsState()
    val searchHideStatusBar by viewModel.searchHideStatusBar.collectAsState()
    val noteResults by viewModel.noteResults.collectAsState()
    val websiteResults by viewModel.websiteResults.collectAsState()
    val shortcutResults by viewModel.shortcutResults.collectAsState()
    val folderResults by viewModel.folderResults.collectAsState()
    val mapTargetResults by viewModel.mapTargetResults.collectAsState()
    val searchIndexShowRailBar by viewModel.searchIndexShowRailBar.collectAsState()
    val searchHeaderLetterAppearance by viewModel.searchHeaderLetterAppearance.collectAsState()
    val searchAlphabetLetterAppearance by viewModel.searchAlphabetLetterAppearance.collectAsState()
    val searchKeyboardShowNumberRow by viewModel.searchKeyboardShowNumberRow.collectAsState()
    val searchKeyboardLayout by viewModel.searchKeyboardLayout.collectAsState()
    val searchKeyboardType by viewModel.searchKeyboardType.collectAsState()
    val searchCategoryOrder by viewModel.searchCategoryOrder.collectAsState()
    val activeSearchCategories by viewModel.activeSearchCategories.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()
    val searchAiApp by viewModel.searchAiApp.collectAsState()
    val searchKeyboardSparks by viewModel.searchKeyboardSparks.collectAsState()
    val searchKeyboardSparkColor by viewModel.searchKeyboardSparkColor.collectAsState()
    val searchKeyboardHaptics by viewModel.searchKeyboardHaptics.collectAsState()

    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    val onAppOverviewPage = appListOpen
    val overviewSurfaceDark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(appListContentAppearance, appOverviewBackground)
    val currentPageId = pages.getOrNull(pagerState.currentPage)?.id ?: -1L
    val currentPageHideStatusBar by remember(currentPageId) { viewModel.pageHideStatusBar(currentPageId) }
        .collectAsState(initial = false)
    // Its own page-scoped flag, independent of text search's — previously it silently borrowed
    // search's setting, with no way to configure it separately from the app overview's own screen.
    val appOverviewHideStatusBar by remember(appOverviewPageId) { viewModel.pageHideStatusBar(appOverviewPageId ?: -1L) }
        .collectAsState(initial = false)
    val hideStatusBar = when {
        folderOpen -> folderHideStatusBar
        onAppOverviewPage -> appOverviewHideStatusBar
        searchOpen -> searchHideStatusBar
        overviewEntry != null -> quickHideStatusBar
        else -> currentPageHideStatusBar
    }
    val immersiveSearch = hideStatusBar && !folderOpen
    // Real inset values only ever arrive once WindowInsets are actually delivered, and are cached here
    // (never reset back to 0) so our own top-padding animation below always has a real, stable target
    // to animate toward — instead of tracking the *live*, mid-transition system inset while the OS
    // separately animates the real status bar in/out. Two independently-timed animations racing each
    // other is what caused content (e.g. the clock) to visibly slide and "find its place" instead of
    // moving once, cleanly, whenever hideStatusBar differs between two things the user navigates
    // between (e.g. one page hides it, another doesn't).
    var cachedStatusBarInset by remember { mutableStateOf(0.dp) }
    var cachedCutoutInset by remember { mutableStateOf(0.dp) }
    val liveStatusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val liveCutoutInset = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    if (liveStatusBarInset > 0.dp) cachedStatusBarInset = liveStatusBarInset
    if (liveCutoutInset > 0.dp) cachedCutoutInset = liveCutoutInset
    val targetTopInset = if (immersiveSearch) cachedCutoutInset else maxOf(cachedStatusBarInset, cachedCutoutInset)
    val animatedTopInset by animateDpAsState(targetTopInset, animationSpec = tween(200), label = "topInset")
    val view = LocalView.current
    DisposableEffect(hideStatusBar) {
        val window = (view.context as? Activity)?.window
        window?.setStatusBarHidden(view, hideStatusBar)
        onDispose { }
    }
    val currentPageBackground by remember(currentPageId) { viewModel.pageBackgroundConfig(currentPageId) }
        .collectAsState(initial = BackgroundConfig())
    val statusBarBackdrop = when {
        searchOpen -> searchOverlayBackground
        appListOpen -> appOverviewBackground
        overviewEntry != null && !folderOpen -> quickOverviewBackground
        else -> currentPageBackground
    }
    val backdropDark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(
        com.bisc.flauncher.data.model.ElementAppearance(fillMode = com.bisc.flauncher.data.model.ElementFillMode.NONE),
        statusBarBackdrop
    )
    val appThemeDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    val lightIcons = folderOpen || backdropDark
    DisposableEffect(lightIcons, appThemeDark) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        controller?.isAppearanceLightStatusBars = !lightIcons
        onDispose { controller?.isAppearanceLightStatusBars = !appThemeDark }
    }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var showAddMapTargetDialog by remember { mutableStateOf(false) }
    var showAddWebsiteDialog by remember { mutableStateOf(false) }
    var showFolderCreator by remember { mutableStateOf(false) }
    var showAddSidebarAppsChooser by remember { mutableStateOf(false) }
    var showSidebarAppPicker by remember { mutableStateOf(false) }

    var addingAppToFolderId by remember { mutableStateOf<Long?>(null) }
    var addingWidgetKindToFolderId by remember { mutableStateOf<Long?>(null) }
    var sizingWidgetForFolder by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var sizingFolderWidgetSpanY by remember { mutableStateOf(1) }
    var addingSubfolderToId by remember { mutableStateOf<Long?>(null) }
    var pendingShortcutFolderId by remember { mutableStateOf<Long?>(null) }
    var mapTargetPickerForFolderId by remember { mutableStateOf<Long?>(null) }
    var websitePickerForFolderId by remember { mutableStateOf<Long?>(null) }
    var addNewMapTargetForFolderId by remember { mutableStateOf<Long?>(null) }
    var addNewWebsiteForFolderId by remember { mutableStateOf<Long?>(null) }

    var didInitialPageScroll by remember { mutableStateOf(false) }
    LaunchedEffect(pages) {
        if (!didInitialPageScroll && pages.isNotEmpty()) {
            val homeIndex = pages.indexOfFirst { it.isHome }.coerceAtLeast(0)
            if (pagerState.currentPage != homeIndex) pagerState.scrollToPage(homeIndex)
            didInitialPageScroll = true
        }
    }

    val openPageById: (Long) -> Unit = { pageId ->
        val index = pages.indexOfFirst { it.id == pageId }
        if (index >= 0) coroutineScope.launch { pagerState.animateScrollToPage(index) }
    }
    val openSettingsFromGesture: () -> Unit = {
        viewModel.closeOverview()
        onNavigateToSettings()
    }

    // Whether *any* level of the folder stack is open.
    val blurRadius by animateDpAsState(
        targetValue = if (folderOpen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 20.dp else 0.dp,
        animationSpec = tween(220),
        label = "folderBackdropBlur"
    )
    val overlayHidesPage = searchOpen || appListOpen || (overviewEntry != null && !folderOpen)
    val pageContentAlpha by animateFloatAsState(
        targetValue = if (overlayHidesPage) 0f else 1f,
        animationSpec = tween(200),
        label = "pageContentAlpha"
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (folderOpen) 0.45f else 0f,
        animationSpec = tween(220),
        label = "folderBackdropScrim"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (pages.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize().then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)) {
                // Layer 1 used to live *inside* each page's pager slot, so it physically slid along with
                // that page's content — fine when neighboring pages share the same background, but a hard,
                // sudden seam wherever two pages carry genuinely different images/colors (you'd see both
                // full images sliding edge-to-edge). Pulled out here as one shared layer behind the pager,
                // crossfading between the settling and target page's background based on the pager's own
                // continuous drag position, so the backdrop dissolves smoothly while the content on top
                // still slides normally — the effect the user asked for without giving up per-page Ebene 1.
                val continuousPage = pagerState.currentPage + pagerState.currentPageOffsetFraction
                val lowerIndex = floor(continuousPage).toInt().coerceIn(0, pages.lastIndex)
                val upperIndex = ceil(continuousPage).toInt().coerceIn(0, pages.lastIndex)
                val crossfadeFraction = (continuousPage - lowerIndex).coerceIn(0f, 1f)
                val lowerPageId = pages.getOrNull(lowerIndex)?.id
                val upperPageId = pages.getOrNull(upperIndex)?.id
                val lowerBackground by remember(lowerPageId) { viewModel.pageBackgroundConfig(lowerPageId ?: -1L) }
                    .collectAsState(initial = BackgroundConfig())
                BackgroundFill(lowerBackground, Modifier.fillMaxSize())
                if (upperPageId != null && upperPageId != lowerPageId && crossfadeFraction > 0f) {
                    val upperBackground by remember(upperPageId) { viewModel.pageBackgroundConfig(upperPageId) }
                        .collectAsState(initial = BackgroundConfig())
                    BackgroundFill(upperBackground, Modifier.fillMaxSize().graphicsLayer { alpha = crossfadeFraction })
                }

                HorizontalPager(
                    state = pagerState,
                    // Composes the page directly left/right of the current one ahead of time, while idle,
                    // instead of only once the user actually starts dragging onto it — that first-ever
                    // composition of a widget-heavy page (own or external widgets alike) is what causes the
                    // one-time hitch on a page's first visit; pre-warming its neighbors moves that cost to
                    // before the swipe instead of during it.
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val page = pages.getOrNull(pageIndex)
                    // A plain Box, not BoxWithConstraints — BoxWithConstraints is backed by SubcomposeLayout,
                    // and subcomposing every page's full widget content (each with its own state, icon
                    // loading, etc.) on first entering the pager's composition range is what made swiping
                    // onto a widget-heavy page feel janky, regardless of whether the widgets were external
                    // or built-in. The one thing this used maxWidth for (innerWidth, for the dock's reserved
                    // space) is read from LocalConfiguration instead, which costs nothing to recompute.
                    val innerWidth = (LocalConfiguration.current.screenWidthDp.dp - DockDimens.horizontalMargin * 2).coerceAtLeast(0.dp)
                    Box(
                        // Restricted to the home page only — recognizing these on every page meant a
                        // swipe-up from e.g. page 2 could open the app overview, and closing it again left
                        // you wherever the pager already was instead of back on home, which read as a bug
                        // ("closing app overview drops me on page 1 instead of home"). Home is the one page
                        // guaranteed to be where the user actually started from.
                        modifier = Modifier.fillMaxSize().then(
                            if (page?.isHome == true) {
                                Modifier.detectHomeGestures(
                                    onLongPress = { viewModel.executeGestureAction(HomeGesture.LONG_PRESS, context, openPageById, openSettingsFromGesture) },
                                    onDoubleTap = { viewModel.executeGestureAction(HomeGesture.DOUBLE_TAP, context, openPageById, openSettingsFromGesture) },
                                    onSwipeUp = { viewModel.executeGestureAction(HomeGesture.SWIPE_UP, context, openPageById, openSettingsFromGesture) },
                                    onSwipeDown = { viewModel.executeGestureAction(HomeGesture.SWIPE_DOWN, context, openPageById, openSettingsFromGesture) }
                                )
                            } else {
                                Modifier
                            }
                        )
                    ) {
                    if (page != null) {
                        val pageRows = rowsByPage[page.id].orEmpty()
                        val pinnedSearchBarRow = pageRows.lastOrNull()?.takeIf { it.type == PageRowType.SEARCH_BAR }
                        val inlineRows = if (pinnedSearchBarRow != null) pageRows.dropLast(1) else pageRows
                        val reservedForDock = DockDimens.scaled(innerWidth)
                            .let { it.barHeight + DockDimens.topMargin + DockDimens.bottomMargin } + DockDimens.contentGap
                        val pageBackgroundConfigOrNull by remember(page.id) { viewModel.pageBackgroundConfig(page.id) }.collectAsState(initial = null)
                        val loadedPageBackgroundConfig = pageBackgroundConfigOrNull
                        // Null (not yet loaded from disk) until proven otherwise, so a hosted system widget
                        // never mounts guessing at dark/light and then gets stuck once the real value arrives.
                        val pageSurfaceIsDark: Boolean? = if (loadedPageBackgroundConfig != null) {
                            com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(
                                com.bisc.flauncher.data.model.ElementAppearance(fillMode = com.bisc.flauncher.data.model.ElementFillMode.NONE),
                                loadedPageBackgroundConfig
                            )
                        } else {
                            null
                        }
                        val contentModifier = Modifier
                            .padding(top = animatedTopInset)
                            .padding(top = 16.dp)
                            .then(
                                if (pinnedSearchBarRow != null) Modifier.padding(bottom = reservedForDock)
                                else Modifier.navigationBarsPadding().padding(bottom = 16.dp)
                            )
                        run { CompositionLocalProvider(
                            LocalSurfaceIsDark provides pageSurfaceIsDark,
                            LocalSurfaceIsDarkLoading provides (loadedPageBackgroundConfig == null)
                        ) { PageRowsColumn(
                                rows = inlineRows,
                                entriesByRow = appEntriesByRow,
                                folders = folders,
                                folderItemsByFolder = folderItemsByFolder,
                                savedMapTargets = savedMapTargets,
                                pinnedWebsites = pinnedWebsites,
                                loadIcon = viewModel::loadIcon,
                                loadPackIcon = viewModel::loadPackIcon,
                                resolveLabel = resolveLabel,
                                onFolderClick = viewModel::openFolder,
                                onShortcutClick = viewModel::launchFolderShortcut,
                                onExecuteAction = { action -> viewModel.executeAction(action, context, openPageById, openSettingsFromGesture) },
                                handedness = handedness,
                                iconShape = iconShape,
                                onSearchBarTap = { viewModel.executeDockTapAction(dockSingleTapAction) },
                                onSearchBarDoubleTap = { viewModel.executeDockTapAction(dockDoubleTapAction) },
                                quickLaunchSlots = quickLaunchSlots,
                                loadQuickLaunchIcon = viewModel::loadIcon,
                                onQuickLaunchClick = { componentName -> launchApp(context, componentName) },
                                modifier = contentModifier.graphicsLayer { alpha = pageContentAlpha }
                            ) } }
                    }
                    }
                }
            }
        }

        if (overlayHidesPage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
            )
        }

        if (folderOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = viewModel::closeAllFolders
                    )
            )
        }

        // Charging animation above the pages, below overlays and dock.
        ChargingGlow(enabled = chargingAnimationEnabled, color = Color(chargingAnimationColor))

        val currentPageRows = pages.getOrNull(pagerState.currentPage)?.let { rowsByPage[it.id] }.orEmpty()
        val pinnedSearchBarRow = currentPageRows.lastOrNull()?.takeIf { it.type == PageRowType.SEARCH_BAR }
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val pinnedDockFootprint = DockDimens.scaled((screenWidth - DockDimens.horizontalMargin * 2).coerceAtLeast(0.dp))
            .let { it.barHeight + DockDimens.topMargin + DockDimens.bottomMargin }

        // Matches text search's own open/close timing exactly, so both feel equally instant.
        AnimatedVisibility(
            visible = overviewEntry != null && !folderOpen,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.fillMaxSize()
        ) {
            BackgroundFill(quickOverviewBackground, Modifier.fillMaxSize())
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            val overviewPivotX = if (handedness == Handedness.LEFT) 0.12f else 0.88f

            // BoxWithConstraints sits *outside* the AnimatedVisibility, and the AnimatedVisibility
            // below gets a fixed width/height instead of sizing to its content — expandVertically
            // would otherwise force a full remeasure (a subcompose pass, since BoxWithConstraints is
            // backed by SubcomposeLayout) on every single animation frame, which used to make opening
            // the quick overview feel choppy compared to text search's own, already pre-sized overlay.
            //
            // A previous version of this also kept the panel composed permanently (movableContentOf,
            // moved between a hidden zero-size holder and this visible slot) to avoid rebuilding it on
            // every open. That made *unrelated* screens (app overview, background sliders) noticeably
            // laggier — an always-mounted panel recomposing on every change to any of the many lists it
            // reads (apps, folders, notes, …) competed for frame time even while fully closed — so it's
            // back to disposing normally. The plain-composition cost turned out not to be the actual
            // cause of the felt delay anyway; that was the single/double-tap timing fixed separately.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DockDimens.horizontalMargin)
                    .then(
                        if (immersiveSearch) {
                            Modifier
                                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                                .padding(top = 16.dp)
                        } else {
                            Modifier.statusBarsPadding()
                        }
                    )
                    .padding(bottom = if (pinnedSearchBarRow != null) DockDimens.contentGap else DockDimens.bottomMargin),
                contentAlignment = Alignment.BottomCenter
            ) {
                val d = com.bisc.flauncher.ui.home.dock.dockDimens(maxWidth)
                val availableHeight = (if (pinnedSearchBarRow != null) maxHeight - pinnedDockFootprint else maxHeight)
                    .coerceAtLeast(200.dp)
                val panelShape = RoundedCornerShape(d.cornerRadius)

                androidx.compose.animation.AnimatedVisibility(
                    visible = overviewEntry != null && !folderOpen,
                    enter = expandVertically(animationSpec = tween(120), expandFrom = Alignment.Bottom) +
                        scaleIn(animationSpec = tween(120), initialScale = 0.94f, transformOrigin = TransformOrigin(overviewPivotX, 1f)) +
                        fadeIn(tween(90)),
                    exit = shrinkVertically(animationSpec = tween(220), shrinkTowards = Alignment.Bottom) +
                        scaleOut(animationSpec = tween(220), targetScale = 0.94f, transformOrigin = TransformOrigin(overviewPivotX, 1f)) +
                        fadeOut(tween(160)),
                    modifier = Modifier.width(d.pillWidth).height(availableHeight)
                ) {
                    val currentOverviewEntry = lastOverviewEntry
                    if (currentOverviewEntry != null) {
                        val sidebarViewMode by remember(currentOverviewEntry) { viewModel.sidebarCategoryViewMode(currentOverviewEntry) }
                            .collectAsState(initial = SidebarCategoryViewMode.LIST)
                        val sidebarShowAddRow by remember(currentOverviewEntry) { viewModel.sidebarCategoryShowAddRow(currentOverviewEntry) }
                            .collectAsState(initial = true)

                        CompositionLocalProvider(com.bisc.flauncher.ui.home.search.LocalRailAppearance provides railAppearance) { SidebarOverviewPanel(
                            entry = currentOverviewEntry,
                            items = sidebarItems,
                            pages = pages,
                            installedApps = browsableApps,
                            sidebarPinnedApps = sidebarPinnedApps,
                            folders = folders,
                            folderItemsByFolder = folderItemsByFolder,
                            notes = notes,
                            savedMapTargets = savedMapTargets,
                            pinnedWebsites = pinnedWebsites,
                            pinnedShortcuts = pinnedShortcuts,
                            hasShortcutPermission = hasShortcutHostPermission,
                            viewMode = sidebarViewMode,
                            showAddRow = sidebarShowAddRow,
                            loadIcon = viewModel::loadIcon,
                            loadPackIcon = viewModel::loadPackIcon,
                            onAppClick = { app ->
                                launchApp(context, app.componentName)
                                viewModel.closeOverview()
                            },
                            onPageClick = { page ->
                                val index = pages.indexOf(page)
                                if (index >= 0) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                }
                                viewModel.closeOverview()
                            },
                            onFolderClick = { folder -> viewModel.openFolder(folder.id) },
                            onAddFolderClick = { showFolderCreator = true },
                            onSaveNote = viewModel::saveNote,
                            onDeleteNote = viewModel::deleteNote,
                            onCreateNote = viewModel::createNote,
                            onReorderNotes = viewModel::reorderNotes,
                            onMapTargetClick = { target ->
                                launchMapTarget(context, target)
                                viewModel.closeOverview()
                            },
                            onRemoveMapTarget = viewModel::removeMapTarget,
                            onAddMapTargetClick = { showAddMapTargetDialog = true },
                            onWebsiteClick = { website ->
                                openWebsite(context, website)
                                viewModel.closeOverview()
                            },
                            onRemoveWebsite = viewModel::removeWebsite,
                            onAddWebsiteClick = { showAddWebsiteDialog = true },
                            onShortcutClick = { shortcut ->
                                viewModel.launchShortcut(shortcut)
                                viewModel.closeOverview()
                            },
                            onRemoveShortcut = viewModel::unpinShortcut,
                            onAddShortcutClick = viewModel::openShortcutAppPicker,
                            onAddSidebarAppsClick = { showAddSidebarAppsChooser = true },
                            onRemoveSidebarApp = viewModel::removeSidebarApp,
                            pinnedContacts = pinnedContacts,
                            hasContactsPermission = contactsPermissionGranted,
                            onContactClick = { pinned ->
                                openContact(context, ContactInfo(pinned.contactId, pinned.lookupKey, pinned.displayName, pinned.photoUri))
                                viewModel.closeOverview()
                            },
                            onRemoveContact = viewModel::unpinContact,
                            onAddContactClick = viewModel::openContactPicker,
                            onContactsPermissionResult = viewModel::onContactsPermissionResult,
                            loadContactPhoto = viewModel::loadContactPhoto,
                            onSidebarItemClick = { item ->
                                when (item) {
                                    is ResolvedSidebarItem.Category -> viewModel.selectSidebarEntry(item.entryType)
                                    is ResolvedSidebarItem.App -> {
                                        launchApp(context, item.app.componentName)
                                        viewModel.closeOverview()
                                    }
                                    is ResolvedSidebarItem.Shortcut -> {
                                        viewModel.launchShortcut(item.shortcut)
                                        viewModel.closeOverview()
                                    }
                                    is ResolvedSidebarItem.MapTarget -> {
                                        launchMapTarget(context, item.target)
                                        viewModel.closeOverview()
                                    }
                                }
                            },
                            onSettingsClick = {
                                viewModel.closeOverview()
                                onNavigateToSettings()
                            },
                            panelAppearance = sidebarPanelAppearance,
                            shape = panelShape,
                            handedness = handedness,
                            dock = d,
                            modifier = Modifier.fillMaxSize()
                        ) }
                    }
                }
            }
            if (pinnedSearchBarRow != null) {
                val dockAlpha by animateFloatAsState(
                    targetValue = if ((searchOpen && searchKeyboardType == SearchKeyboardType.SYSTEM) || (folderOpen && folderHideDock)) 0f else 1f,
                    animationSpec = tween(200),
                    label = "dockAlpha"
                )
                SearchBar(
                    modifier = Modifier.graphicsLayer { alpha = dockAlpha },
                    onSingleTap = { viewModel.executeDockTapAction(dockSingleTapAction) },
                    onDoubleTap = { viewModel.executeDockTapAction(dockDoubleTapAction) },
                    handedness = handedness,
                    iconShape = iconShape,
                    appearance = pinnedSearchBarRow.appearance(),
                    searchButtonAppearance = dockSearchButtonAppearance,
                    quickLaunchSlots = quickLaunchSlots,
                    loadQuickLaunchIcon = viewModel::loadIcon,
                    onQuickLaunchClick = { componentName -> launchApp(context, componentName) }
                )
            }
        }
    }

    BackHandler(enabled = appListOpen && !searchOpen) { viewModel.closeAppList() }
    BackHandler(enabled = searchOpen) { viewModel.closeSearch() }
    LaunchedEffect(homeButtonPressCount) { if (appListOpen) viewModel.closeAppList() }
    AnimatedVisibility(
        visible = appListOpen,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(110)) + slideInVertically(animationSpec = tween(130)) { it / 8 },
        exit = fadeOut(tween(110)) + slideOutVertically(animationSpec = tween(130)) { it / 8 }
    ) {
        val pullDownToClose = remember(viewModel) {
            object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                private var pulled = 0f
                override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                    pulled = if (available.y > 0f && source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) pulled + available.y else 0f
                    if (pulled > 260f) { pulled = 0f; viewModel.closeAppList() }
                    return androidx.compose.ui.geometry.Offset.Zero
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                .nestedScroll(pullDownToClose)
        ) {
            BackgroundFill(appOverviewBackground, Modifier.fillMaxSize())
            CompositionLocalProvider(LocalAppIconLoader provides viewModel.iconLoader) {
                AppOverviewContent(
                    layout = appListLayout,
                    showSearchButton = appListShowSearchButton,
                    allApps = browsableApps,
                    showAzIndex = searchShowAzIndex,
                    indexShowRailBar = searchIndexShowRailBar,
                    headerLetterAppearance = searchHeaderLetterAppearance,
                    alphabetLetterAppearance = searchAlphabetLetterAppearance,
                    displayMode = appListDisplayMode,
                    contentAppearance = appListContentAppearance,
                    pageBackground = appOverviewBackground,
                    railAppearance = railAppearance,
                    accentColor = accentColor,
                    handedness = handedness,
                    loadIcon = viewModel::loadIcon,
                    onAppClick = { app ->
                        launchApp(context, app.componentName)
                        viewModel.closeAppList()
                    },
                    // With a hidden status bar only the display cutout inset remains.
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (immersiveSearch) Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                            else Modifier.statusBarsPadding()
                        )
                        .padding(top = 16.dp)
                )
                if (appListShowSearchButton) {
                    com.bisc.flauncher.ui.home.dock.FloatingSearchButton(
                        onClick = { viewModel.executeDockTapAction(com.bisc.flauncher.data.model.DockTapAction.FULL_SEARCH) },
                        handedness = handedness,
                        iconShape = iconShape,
                        appearance = appListSearchButtonAppearance ?: railAppearance ?: automaticRailAppearance(overviewSurfaceDark)
                    )
                }
            }
        }
    }

    val searchPivotX = if (handedness == Handedness.LEFT) 0.12f else 0.88f
    AnimatedVisibility(
        visible = searchOpen,
        modifier = Modifier.fillMaxSize(),
        enter = expandVertically(
            animationSpec = tween(120),
            expandFrom = Alignment.Bottom
        ) + scaleIn(
            animationSpec = tween(120),
            initialScale = 0.94f,
            transformOrigin = TransformOrigin(searchPivotX, 1f)
        ) + fadeIn(tween(90)),
        exit = shrinkVertically(
            animationSpec = tween(220),
            shrinkTowards = Alignment.Bottom
        ) + scaleOut(
            animationSpec = tween(220),
            targetScale = 0.94f,
            transformOrigin = TransformOrigin(searchPivotX, 1f)
        ) + fadeOut(tween(160))
    ) {
        SearchOverlay(
            query = searchQuery,
            appResults = searchResults,
            calculatorResult = calculatorResult,
            contactResults = contactResults,
            noteResults = noteResults,
            websiteResults = websiteResults,
            shortcutResults = shortcutResults,
            folderResults = folderResults,
            mapTargetResults = mapTargetResults,
            contactsPermissionGranted = contactsPermissionGranted,
            statusBarHidden = immersiveSearch,
            categoryOrder = searchCategoryOrder,
            activeCategories = activeSearchCategories,
            categoryFilter = searchFilter,
            aiAppPackage = searchAiApp,
            onToggleFilter = viewModel::toggleSearchFilter,
            keyboardType = searchKeyboardType,
            backdrop = searchOverlayBackground,
            keyboardAppearance = keyboardAppearance,
            searchResultCardAppearance = searchResultCardAppearance,
            showKeyboardNumberRow = searchKeyboardShowNumberRow,
            keyboardLayout = searchKeyboardLayout,
            keyboardSparks = searchKeyboardSparks,
            keyboardSparkColor = Color(searchKeyboardSparkColor),
            keyboardHaptics = searchKeyboardHaptics,
            handedness = handedness,
            iconShape = iconShape,
            loadIcon = viewModel::loadIcon,
            onQueryChange = viewModel::updateSearchQuery,
            onAppClick = { app ->
                launchApp(context, app.componentName)
                viewModel.closeSearch()
            },
            onContactClick = { contact ->
                openContact(context, contact)
                viewModel.closeSearch()
            },
            onSettingsShortcutClick = { shortcut ->
                launchSettingsShortcut(context, shortcut)
                viewModel.closeSearch()
            },
            onNoteClick = { note ->
                editingNote = note
                showNoteEditor = true
                viewModel.closeSearch()
            },
            onFolderClick = { folder ->
                viewModel.closeSearch()
                viewModel.openFolder(folder.id)
            },
            onMapTargetClick = { target ->
                com.bisc.flauncher.util.launchMapTarget(context, target)
                viewModel.closeSearch()
            },
            onWebsiteClick = { website ->
                openWebsite(context, website)
                viewModel.closeSearch()
            },
            onShortcutClick = { shortcut ->
                viewModel.launchShortcut(shortcut)
                viewModel.closeSearch()
            },
            onRefreshContactsPermission = viewModel::refreshContactsPermission,
            onHome = viewModel::closeSearch,
            onWebCommand = { viewModel.runWebSearchCommand(context) },
            onMapsCommand = { viewModel.runMapsCommand(context) },
            onEmailCommand = { viewModel.runEmailCommand(context) },
            onAiCommand = { viewModel.runAiCommand(context) }
        )
    }

    if (showNoteEditor) {
        NoteEditorDialog(
            note = editingNote,
            onSave = { note, onSaved -> viewModel.saveNote(note, onSaved) },
            onDelete = { note ->
                viewModel.deleteNote(note)
                showNoteEditor = false
                viewModel.closeOverview()
            },
            onDismiss = { showNoteEditor = false }
        )
    }

    if (showAddMapTargetDialog) {
        AddMapTargetDialog(
            onSave = { label, lat, lon, address ->
                viewModel.addMapTarget(label, lat, lon, address)
                showAddMapTargetDialog = false
            },
            onDismiss = { showAddMapTargetDialog = false }
        )
    }

    if (showAddWebsiteDialog) {
        AddWebsiteDialog(
            onSave = { label, url ->
                viewModel.addWebsite(label, url)
                showAddWebsiteDialog = false
            },
            onDismiss = { showAddWebsiteDialog = false }
        )
    }

    if (showAddSidebarAppsChooser) {
        AddSidebarAppsChooserSheet(
            onPickOne = {
                showAddSidebarAppsChooser = false
                showSidebarAppPicker = true
            },
            onAddAll = {
                showAddSidebarAppsChooser = false
                viewModel.addAllSidebarApps(browsableApps)
            },
            onDismiss = { showAddSidebarAppsChooser = false }
        )
    }

    if (showSidebarAppPicker) {
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app ->
                viewModel.addSidebarApp(app)
                showSidebarAppPicker = false
            },
            onDismiss = { showSidebarAppPicker = false }
        )
    }

    if (contactPickerOpen) {
        ContactPickerSheet(
            contacts = contactPickerList,
            loadPhoto = viewModel::loadContactPhoto,
            onSelected = viewModel::pinContact,
            onDismiss = viewModel::dismissContactPicker
        )
    }

    if (showFolderCreator) {
        FolderCreatorSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onCreate = { name, apps ->
                viewModel.createStandaloneFolder(name, apps)
                showFolderCreator = false
            },
            onDismiss = { showFolderCreator = false }
        )
    }

    if (shortcutAppPickerOpen) {
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = viewModel::selectShortcutSourceApp,
            onDismiss = viewModel::dismissShortcutAppPicker
        )
    }

    shortcutPickerForApp?.let { app ->
        AppShortcutPickerDialog(
            appLabel = app.label,
            shortcuts = availableShortcuts,
            loadIcon = viewModel::loadShortcutIcon,
            onShortcutSelected = { info ->
                val folderId = pendingShortcutFolderId
                if (folderId != null) {
                    viewModel.addShortcutToFolder(folderId, info)
                    pendingShortcutFolderId = null
                } else {
                    viewModel.pinShortcut(info)
                }
            },
            onDismiss = {
                pendingShortcutFolderId = null
                viewModel.dismissShortcutPicker()
            }
        )
    }

    openFolderPath.lastOrNull()?.let { folderId ->
        val folder = folders.find { it.id == folderId }
        if (folder != null) {
            val folderShowAddRow by remember(folderId) { viewModel.folderShowAddRow(folderId) }.collectAsState(initial = true)
            FolderView(
                folder = folder,
                items = folderItemsByFolder[folderId].orEmpty(),
                folders = folders,
                folderItemsByFolder = folderItemsByFolder,
                savedMapTargets = savedMapTargets,
                pinnedWebsites = pinnedWebsites,
                showAddRow = folderShowAddRow,
                loadIcon = viewModel::loadIcon,
                loadPackIcon = viewModel::loadPackIcon,
                resolveLabel = resolveLabel,
                onAppClick = { item ->
                    when (item.type) {
                        GridItemType.APP -> ComponentName.unflattenFromString(item.payload)?.let { launchApp(context, it) }
                        GridItemType.SHORTCUT -> viewModel.launchFolderShortcut(item.payload)
                        GridItemType.MAP_TARGET -> item.payload.toLongOrNull()
                            ?.let { id -> savedMapTargets.find { it.id == id } }
                            ?.let { target -> launchMapTarget(context, target) }
                        GridItemType.WEBSITE -> item.payload.toLongOrNull()
                            ?.let { id -> pinnedWebsites.find { it.id == id } }
                            ?.let { website -> openWebsite(context, website) }
                        GridItemType.FOLDER, GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET -> Unit
                    }
                    viewModel.closeAllFolders()
                    viewModel.closeOverview()
                },
                onFolderClick = { item -> item.payload.toLongOrNull()?.let(viewModel::openFolder) },
                onRemoveItem = viewModel::removeFromFolder,
                onReorderItems = viewModel::reorderFolderItems,
                onAddAppClick = { addingAppToFolderId = folderId },
                onAddShortcutClick = {
                    pendingShortcutFolderId = folderId
                    viewModel.openShortcutAppPicker()
                },
                onAddWidgetClick = { addingWidgetKindToFolderId = folderId },
                onAddSubfolderClick = { addingSubfolderToId = folderId },
                onAddMapTargetClick = { mapTargetPickerForFolderId = folderId },
                onAddWebsiteClick = { websitePickerForFolderId = folderId },
                onBack = viewModel::closeFolder,
                cardAppearance = folderCardAppearance
            )
        }
    }

    addingAppToFolderId?.let { folderId ->
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app ->
                viewModel.addAppToFolder(folderId, app)
                addingAppToFolderId = null
            },
            onDismiss = { addingAppToFolderId = null }
        )
    }

    addingWidgetKindToFolderId?.let { folderId ->
        FolderWidgetKindSheet(
            onKindSelected = { kind ->
                addingWidgetKindToFolderId = null
                sizingFolderWidgetSpanY = WidgetKind.defaultSpan(kind)
                sizingWidgetForFolder = folderId to kind
            },
            onDismiss = { addingWidgetKindToFolderId = null }
        )
    }

    sizingWidgetForFolder?.let { (folderId, kind) ->
        FolderWidgetSizeSheet(
            row = PageRow(pageId = 0, orderIndex = 0, type = PageRowType.WIDGET, payload = kind, spanY = sizingFolderWidgetSpanY),
            spanY = sizingFolderWidgetSpanY,
            minSpan = WidgetKind.minSpan(kind),
            maxSpan = WidgetKind.maxSpan(kind),
            onSpanChange = { sizingFolderWidgetSpanY = it },
            onSave = {
                viewModel.addWidgetToFolder(folderId, kind, sizingFolderWidgetSpanY)
                sizingWidgetForFolder = null
            },
            onDismiss = { sizingWidgetForFolder = null }
        )
    }

    addingSubfolderToId?.let { folderId ->
        NameSubfolderDialog(
            onConfirm = { name ->
                viewModel.addNestedFolder(folderId, name)
                addingSubfolderToId = null
            },
            onDismiss = { addingSubfolderToId = null }
        )
    }

    mapTargetPickerForFolderId?.let { folderId ->
        SavedMapTargetPickerSheet(
            targets = savedMapTargets,
            onSelect = { target ->
                viewModel.addMapTargetToFolder(folderId, target.id)
                mapTargetPickerForFolderId = null
            },
            onAddNew = {
                mapTargetPickerForFolderId = null
                addNewMapTargetForFolderId = folderId
            },
            onDismiss = { mapTargetPickerForFolderId = null }
        )
    }

    websitePickerForFolderId?.let { folderId ->
        SavedWebsitePickerSheet(
            websites = pinnedWebsites,
            onSelect = { website ->
                viewModel.addWebsiteToFolder(folderId, website.id)
                websitePickerForFolderId = null
            },
            onAddNew = {
                websitePickerForFolderId = null
                addNewWebsiteForFolderId = folderId
            },
            onDismiss = { websitePickerForFolderId = null }
        )
    }

    addNewMapTargetForFolderId?.let { folderId ->
        AddMapTargetDialog(
            onSave = { label, lat, lon, address ->
                viewModel.addNewMapTargetToFolder(folderId, label, lat, lon, address)
                addNewMapTargetForFolderId = null
            },
            onDismiss = { addNewMapTargetForFolderId = null }
        )
    }

    addNewWebsiteForFolderId?.let { folderId ->
        AddWebsiteDialog(
            onSave = { label, url ->
                viewModel.addNewWebsiteToFolder(folderId, label, url)
                addNewWebsiteForFolderId = null
            },
            onDismiss = { addNewWebsiteForFolderId = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalMaterial3Api
@Composable
private fun FolderWidgetKindSheet(onKindSelected: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(WidgetKind.MUSIC, WidgetKind.WEATHER, WidgetKind.NOTES).forEach { kind ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onKindSelected(kind) }
                        .padding(horizontal = 4.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = folderWidgetKindIcon(kind), contentDescription = null)
                    Text(folderWidgetKindLabel(kind), modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun FolderWidgetSizeSheet(
    row: PageRow,
    spanY: Int,
    minSpan: Int,
    maxSpan: Int,
    onSpanChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.settings_rows_size_title), style = MaterialTheme.typography.titleLarge)
            WidgetSizeEditor(row = row.copy(spanY = spanY), minSpan = minSpan, maxSpan = maxSpan, onSpanChange = onSpanChange)
            TextButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_rows_save))
            }
        }
    }
}

@Composable
private fun NameSubfolderDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_add_subfolder)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.settings_folder_creator_name_hint)) }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.settings_folder_creator_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.widget_notes_cancel)) }
        }
    )
}

private fun folderWidgetKindIcon(kind: String) = when (kind) {
    WidgetKind.MUSIC -> Icons.Filled.MusicNote
    WidgetKind.WEATHER -> Icons.Filled.Cloud
    WidgetKind.NOTES -> Icons.Filled.EditNote
    else -> Icons.Filled.MusicNote
}

@Composable
private fun folderWidgetKindLabel(kind: String): String = when (kind) {
    WidgetKind.MUSIC -> stringResource(R.string.picker_widget_music)
    WidgetKind.WEATHER -> stringResource(R.string.picker_widget_weather)
    WidgetKind.NOTES -> stringResource(R.string.picker_widget_notes)
    else -> kind
}
