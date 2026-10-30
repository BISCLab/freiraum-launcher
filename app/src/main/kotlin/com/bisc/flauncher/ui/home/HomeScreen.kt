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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.bisc.flauncher.util.setStatusBarHidden
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.Note
import com.bisc.flauncher.data.model.ContactInfo
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.effectiveAppearance
import com.bisc.flauncher.data.model.dockRows
import com.bisc.flauncher.data.model.panelRows
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.ContactPickerSheet
import com.bisc.flauncher.ui.home.dock.AddWebsiteDialog
import com.bisc.flauncher.ui.home.dock.AppShortcutPickerDialog
import com.bisc.flauncher.ui.home.dock.BackgroundCrossfade
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.ui.home.dock.FreeEdge
import com.bisc.flauncher.ui.home.dock.freeEdgeBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.bisc.flauncher.ui.home.dock.DockDimens
import com.bisc.flauncher.ui.home.dock.DockPanelSurface
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.home.search.AppOverviewContent
import com.bisc.flauncher.ui.home.search.automaticRailAppearance
import com.bisc.flauncher.ui.home.search.SearchOverlay
import com.bisc.flauncher.ui.settings.FolderCreatorSheet
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
    /** Opens the settings at a navigation route, "settings" being their start page. */
    onNavigateToSettings: (String) -> Unit,
    homeButtonPressCount: Int = 0,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pages by viewModel.pages.collectAsState()
    // Page-link entries (GridItemType.PAGE, including the dock's own) need the full list to resolve
    // an icon for the Search/App-overview singletons too - `pages` above is ROWS-only (the pager's own
    // list), which is why those two always fell back to the generic "unknown page" glyph otherwise.
    val allPages by viewModel.allPages.collectAsState()
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val rowEntriesByRow by viewModel.rowEntriesByRow.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val resolveLabel = viewModel::resolveLabel
    val appOverviewHandedness by viewModel.appOverviewHandedness.collectAsState()
    val isPanelOpen by viewModel.isPanelOpen.collectAsState()
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
    val notes by viewModel.notes.collectAsState()
    val savedMapTargets by viewModel.savedMapTargets.collectAsState()
    val pinnedWebsites by viewModel.pinnedWebsites.collectAsState()
    val pinnedShortcuts by viewModel.pinnedShortcuts.collectAsState()
    val pinnedContacts by viewModel.pinnedContacts.collectAsState()
    val contactPickerOpen by viewModel.contactPickerOpen.collectAsState()
    val contactPickerList by viewModel.contactPickerList.collectAsState()
    val actionButtons by viewModel.actionButtons.collectAsState()
    val shortcutAppPickerOpen by viewModel.shortcutAppPickerOpen.collectAsState()
    val shortcutPickerForApp by viewModel.shortcutPickerForApp.collectAsState()
    val availableShortcuts by viewModel.availableShortcuts.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    val iconSizeScale by viewModel.iconSizeScale.collectAsState()
    val railAppearance by viewModel.appListRailAppearance.collectAsState()
    val chargingAnimationEnabled by viewModel.chargingAnimationEnabled.collectAsState()
    val chargingAnimationColor by viewModel.chargingAnimationColor.collectAsState()
    val searchAppearance by viewModel.searchAppearance.collectAsState()
    val searchBarAppearance by viewModel.searchBarAppearance.collectAsState()
    val searchBackground by viewModel.searchBackground.collectAsState()
    val searchBarCornerRadiusPercent by viewModel.searchBarCornerRadiusPercent.collectAsState()
    val searchResultsAppearance by viewModel.searchResultsAppearance.collectAsState()
    val searchResultsCornerRadiusPercent by viewModel.searchResultsCornerRadiusPercent.collectAsState()
    val searchResultsEdgeToEdge by viewModel.searchResultsEdgeToEdge.collectAsState()
    val searchBarAtBottom by viewModel.searchBarAtBottom.collectAsState()
    val searchBarShowCategories by viewModel.searchBarShowCategories.collectAsState()
    val searchKeyboardCapEnabled by viewModel.searchKeyboardCapEnabled.collectAsState()
    val searchKeyboardCapColor by viewModel.searchKeyboardCapColor.collectAsState()
    val searchKeyboardCapCornerRadiusPercent by viewModel.searchKeyboardCapCornerRadiusPercent.collectAsState()
    val searchKeyboardCapArrow by viewModel.searchKeyboardCapArrow.collectAsState()
    val searchKeyboardAutoOpen by viewModel.searchKeyboardAutoOpen.collectAsState()
    val appListSearchButtonAppearance by viewModel.appListSearchButtonAppearance.collectAsState()
    val appListShowSearchButton by viewModel.appListShowSearchButton.collectAsState()
    val appListAccentColor by viewModel.appListAccentColor.collectAsState()
    val folderHideDock by viewModel.folderHideDock.collectAsState()
    val folderCardAppearance by viewModel.folderCardAppearance.collectAsState()
    val folderShowLabels by viewModel.folderShowLabels.collectAsState()
    val cornerRadiusPercent by viewModel.cornerRadiusPercent.collectAsState()
    val appListShowAzIndex by viewModel.appListShowAzIndex.collectAsState()
    val searchHideStatusBar by viewModel.searchHideStatusBar.collectAsState()
    val websiteResults by viewModel.websiteResults.collectAsState()
    val shortcutResults by viewModel.shortcutResults.collectAsState()
    val folderResults by viewModel.folderResults.collectAsState()
    val mapTargetResults by viewModel.mapTargetResults.collectAsState()
    val appListHeaderLetterAppearance by viewModel.appListHeaderLetterAppearance.collectAsState()
    val searchCategoryOrder by viewModel.searchCategoryOrder.collectAsState()
    val activeSearchCategories by viewModel.activeSearchCategories.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()
    val searchAiApp by viewModel.searchAiApp.collectAsState()
    val categoryColors by viewModel.categoryColors.collectAsState()
    val converterResult by viewModel.converterResult.collectAsState()
    val appGridOpen by viewModel.appGridOpen.collectAsState()
    val appGridPageId by viewModel.appGridPageId.collectAsState()
    val appGridBackground by remember(appGridPageId) { viewModel.pageBackgroundConfig(appGridPageId ?: -1L) }
        .collectAsState(initial = BackgroundConfig())
    val appGridHideStatusBar by remember(appGridPageId) { viewModel.pageHideStatusBar(appGridPageId ?: -1L) }
        .collectAsState(initial = false)
    val appGridColumns by viewModel.appGridColumns.collectAsState()
    val appGridIconSizePercent by viewModel.appGridIconSizePercent.collectAsState()
    val appGridShowLabels by viewModel.appGridShowLabels.collectAsState()
    val appGridBarPosition by viewModel.appGridBarPosition.collectAsState()
    val appGridBarOpensTextSearch by viewModel.appGridBarOpensTextSearch.collectAsState()
    val appGridKeyboardAutoOpen by viewModel.appGridKeyboardAutoOpen.collectAsState()
    val appGridBarAppearance by viewModel.appGridBarAppearance.collectAsState()
    val appGridFolderIds by viewModel.appGridFolderIds.collectAsState()
    val appGridFoldersAlphabetical by viewModel.appGridFoldersAlphabetical.collectAsState()
    val appGridHideFolderApps by viewModel.appGridHideFolderApps.collectAsState()
    val appListOwnSearch by viewModel.appListOwnSearch.collectAsState()
    val appQuery by viewModel.appQuery.collectAsState()
    val appQueryResults by viewModel.appQueryResults.collectAsState()
    // The app list's own app search (search button with "own search" on).
    var appListSearching by remember { mutableStateOf(false) }
    LaunchedEffect(appListOpen) { if (!appListOpen) appListSearching = false }

    // Endless carousel: the pager runs over many virtual pages that cycle through the real ones
    // (virtual index mod page count), started in the middle so it can wrap both ways.
    val carouselEndless by viewModel.carouselEndless.collectAsState()
    val endless = carouselEndless && pages.size > 1
    val virtualPageCount = if (endless) pages.size * ENDLESS_CAROUSEL_LOOPS else pages.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { virtualPageCount })
    fun realIndex(virtual: Int): Int =
        if (endless) virtual.mod(pages.size) else virtual.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
    // The virtual index of real page [real] nearest to the current one, so jumps take the short way round.
    fun virtualIndexOf(real: Int): Int {
        if (!endless) return real
        val current = pagerState.currentPage
        val base = current - current.mod(pages.size)
        return listOf(base + real - pages.size, base + real, base + real + pages.size).minBy { kotlin.math.abs(it - current) }
    }

    val onAppOverviewPage = appListOpen
    val overviewSurfaceDark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), appOverviewBackground)
    val currentPageId = pages.getOrNull(realIndex(pagerState.currentPage))?.id ?: -1L
    val currentPageHideStatusBar by remember(currentPageId) { viewModel.pageHideStatusBar(currentPageId) }
        .collectAsState(initial = false)
    // Its own page-scoped flag, independent of text search's.
    val appOverviewHideStatusBar by remember(appOverviewPageId) { viewModel.pageHideStatusBar(appOverviewPageId ?: -1L) }
        .collectAsState(initial = false)
    // The Panel is part of the home page, so it inherits currentPageHideStatusBar like everything
    // else on Home - no dedicated branch.
    val hideStatusBar = when {
        folderOpen -> folderHideStatusBar
        onAppOverviewPage -> appOverviewHideStatusBar
        appGridOpen -> appGridHideStatusBar
        searchOpen -> searchHideStatusBar
        else -> currentPageHideStatusBar
    }
    val immersiveSearch = hideStatusBar && !folderOpen
    // Real inset values only ever arrive once WindowInsets are actually delivered, and are cached here
    // (never reset back to 0) so our own top-padding animation below always has a real, stable target
    // to animate toward - instead of tracking the *live*, mid-transition system inset while the OS
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
    // Search draws its own backdrop (searchBackground); its ElementAppearance only adds opacity and text color.
    val backdropDark = if (searchOpen) {
        com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(searchAppearance.copy(fillMode = com.bisc.flauncher.data.model.ElementFillMode.NONE), searchBackground)
    } else {
        val statusBarBackdrop = when {
            appListOpen -> appOverviewBackground
            appGridOpen -> appGridBackground
            else -> currentPageBackground
        }
        com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(
            com.bisc.flauncher.data.model.ElementAppearance(fillMode = com.bisc.flauncher.data.model.ElementFillMode.NONE),
            statusBarBackdrop
        )
    }
    val appThemeDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    val lightIcons = folderOpen || backdropDark
    DisposableEffect(lightIcons, appThemeDark) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        controller?.isAppearanceLightStatusBars = !lightIcons
        onDispose { controller?.isAppearanceLightStatusBars = !appThemeDark }
    }
    var contactActionsFor by remember { mutableStateOf<com.bisc.flauncher.data.model.ContactInfo?>(null) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var showAddMapTargetDialog by remember { mutableStateOf(false) }
    var showAddWebsiteDialog by remember { mutableStateOf(false) }
    var showFolderCreator by remember { mutableStateOf(false) }

    // See the background-crossfade layer below for why this exists: a stable, append-only cache instead
    // of re-subscribing a Flow every time a page's role (lower/upper) in the crossfade changes.
    val pageBackgroundCache = remember { mutableStateMapOf<Long, BackgroundConfig>() }

    val homeIndex = pages.indexOfFirst { it.isHome }.coerceAtLeast(0)

    // Starts on Home; afterwards keeps showing the same page when the page list or the endless mode
    // changes (both remap virtual indices), and keeps an endless pager away from its outer ends.
    var didInitialPageScroll by remember { mutableStateOf(false) }
    var lastShownPageId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(pages, endless) {
        if (pages.isEmpty()) return@LaunchedEffect
        val wantedReal = if (!didInitialPageScroll) homeIndex
            else pages.indexOfFirst { it.id == lastShownPageId }.takeIf { it >= 0 } ?: homeIndex
        val current = pagerState.currentPage
        val offCenter = if (endless) current < pages.size || current >= virtualPageCount - pages.size else current >= pages.size
        if (!didInitialPageScroll || offCenter || realIndex(current) != wantedReal) {
            pagerState.scrollToPage(if (endless) (ENDLESS_CAROUSEL_LOOPS / 2) * pages.size + wantedReal else wantedReal)
        }
        didInitialPageScroll = true
    }
    LaunchedEffect(pages, endless) {
        snapshotFlow { pagerState.currentPage }.collect { virtual ->
            if (didInitialPageScroll) pages.getOrNull(realIndex(virtual))?.id?.let { lastShownPageId = it }
        }
    }

    // Hoisted above the pager (rather than declared down where the dock itself renders) so the
    // gesture-detector Box below (home-screen background taps) and the dock's own rendering block
    // further down share one dockExpanded state.
    val rowUnitCounts by viewModel.rowUnitCounts.collectAsState()
    val homeGestureExclusion = remember { HomeGestureExclusion() }
    var pagerHeightPx by remember { mutableStateOf(0) }
    val homeDockRow = pages.find { it.isHome }
        ?.let { rowsByPage[it.id] }
        ?.find { it.type == PageRowType.SEARCH_BAR }
    val homePanelRow = pages.find { it.isHome }
        ?.let { rowsByPage[it.id] }
        ?.find { it.type == PageRowType.PANEL }
    val dockVisibilityMode = homeDockRow?.dockVisibilityMode ?: com.bisc.flauncher.data.model.DockVisibilityMode.ALWAYS_VISIBLE
    // Ephemeral UI state only, deliberately not persisted - same ephemeral character as the
    // Favorites popup this look mirrors. Resets to its mode's own starting state whenever the
    // row id or the mode itself changes (e.g. the user just switched modes in Settings).
    var dockExpanded by remember(homeDockRow?.id, dockVisibilityMode) {
        mutableStateOf(dockVisibilityMode != com.bisc.flauncher.data.model.DockVisibilityMode.MANUAL)
    }
    val isOnHomePage = realIndex(pagerState.currentPage) == homeIndex
    LaunchedEffect(dockVisibilityMode, isOnHomePage) {
        if (dockVisibilityMode == com.bisc.flauncher.data.model.DockVisibilityMode.AUTO_EXPAND_ON_ARRIVAL && isOnHomePage) {
            dockExpanded = true
            kotlinx.coroutines.delay(2500)
            dockExpanded = false
        }
    }
    val dockCanCollapse = dockVisibilityMode != com.bisc.flauncher.data.model.DockVisibilityMode.ALWAYS_VISIBLE
    // Standard edge-gesture hot-zone size (not tied to the live system inset, which collapses to 0
    // once we hide the status bar - the whole point is keeping Android's own edge swipe reachable
    // even then), used to keep the Dock's/Panel's swipe-gesture claim from eating the top/bottom
    // edge strip Android itself reserves for the status bar reveal and the gesture-nav task switcher.
    val edgeSafeZonePx = with(LocalDensity.current) { 24.dp.toPx() }

    val openPageById: (Long) -> Unit = { pageId ->
        val index = pages.indexOfFirst { it.id == pageId }
        if (index >= 0) coroutineScope.launch { pagerState.animateScrollToPage(virtualIndexOf(index)) }
    }
    // Shared by every page-link entry (an ElementBoxGrid cell, a pill-row cell, the floating dock) - 
    // jumps the pager for a swipeable ROWS page, opens the matching overlay for the two singletons.
    val onOpenPageLink: (Long, com.bisc.flauncher.data.model.LauncherPageKind) -> Unit = { pageId, kind ->
        when (kind) {
            com.bisc.flauncher.data.model.LauncherPageKind.ROWS -> openPageById(pageId)
            com.bisc.flauncher.data.model.LauncherPageKind.APP_LIST -> viewModel.openAppList()
            com.bisc.flauncher.data.model.LauncherPageKind.SEARCH -> viewModel.openSearch()
            com.bisc.flauncher.data.model.LauncherPageKind.APP_GRID -> viewModel.openAppGrid()
        }
    }
    // An Action button's action: open a page (same dispatch as a plain page-link entry), trigger
    // the Panel/Dock directly (the one thing a page-link can't express), jump into a curated
    // Android settings screen, or open FLauncher's own Settings - see ActionButtonActionType.
    val onActionButtonClick: (com.bisc.flauncher.data.model.ActionButton) -> Unit = { button ->
        when (button.actionType) {
            com.bisc.flauncher.data.model.ActionButtonActionType.PAGE -> {
                val target = allPages.find { it.id == button.actionPageId }
                if (target != null) onOpenPageLink(target.id, target.kind)
            }
            com.bisc.flauncher.data.model.ActionButtonActionType.PANEL -> viewModel.openOverview()
            com.bisc.flauncher.data.model.ActionButtonActionType.DOCK -> dockExpanded = true
            com.bisc.flauncher.data.model.ActionButtonActionType.ANDROID_SETTING -> {
                button.androidSettingAction?.let { action -> launchSettingsShortcut(context, action) }
            }
            com.bisc.flauncher.data.model.ActionButtonActionType.LAUNCHER_SETTINGS -> onNavigateToSettings("settings")
        }
    }
    // The one place "go back to home" is defined: closes every overlay and also scrolls the pager
    // back to home, so the system home gesture and back-swipes never leave you on another page.
    val returnHome: () -> Unit = {
        viewModel.closeSearch()
        viewModel.closeAppList()
        viewModel.closeAppGrid()
        viewModel.closeOverview()
        viewModel.closeAllFolders()
        if (realIndex(pagerState.currentPage) != homeIndex) coroutineScope.launch { pagerState.animateScrollToPage(virtualIndexOf(homeIndex)) }
    }
    val openSettingsFromGesture: () -> Unit = {
        viewModel.closeOverview()
        onNavigateToSettings("settings")
    }

    // Whether *any* level of the folder stack is open.
    val blurRadius by animateDpAsState(
        targetValue = if (folderOpen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 20.dp else 0.dp,
        animationSpec = tween(220),
        label = "folderBackdropBlur"
    )
    val overlayHidesPage = searchOpen || appListOpen || appGridOpen || (isPanelOpen && !folderOpen)
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { pagerHeightPx = it.height }
                    .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
            ) {
                // Layer 1 (the background) is one shared layer behind the pager rather than part of
                // each page, so pages with different images/colors blend instead of showing a seam.
                //
                // Backgrounds are read from pageBackgroundCache (filled below, per page slot) rather than
                // each collecting their own Flow - a page's background is fetched once and cached, then
                // just read back instantly regardless of how the transition logic below uses it.
                //
                // Frame-by-frame capture of an actual swipe (see git history for the two failed attempts
                // this replaced) confirmed the background has to track the *same* continuous position the
                // pager itself uses to lay out content - anything that waits for a discrete "arrived" or
                // "committed" signal (currentPage, targetPage) either lags behind the content's own motion
                // (a visible two-stage "content slides, then bg flashes in afterward") or over-fires on
                // every direction change during an indecisive drag (targetPage changes many times per
                // gesture, restarting a keyed animation each time). Reading currentPageOffsetFraction
                // directly, every frame, with no intermediate Animatable/LaunchedEffect, guarantees the
                // background is always exactly where the pager itself says the transition currently is.
                //
                // Frozen to the settled page alone whenever an overlay (app overview, search, quick
                // overview) covers the pager - a page reported flickering to a neighbor's background
                // specifically while leaving the app overview, and the pager underneath keeps existing
                // (composed, not disposed) the whole time an overlay sits on top of it, so anything that
                // nudged its position while hidden would only become visible right as that overlay's own
                // exit fade reveals it again. There's never a legitimate reason to crossfade while nothing
                // of the pager is even visible, so this sidesteps that class of bug outright rather than
                // chasing down whatever specifically causes the position to move while covered.
                //
                // Gated on pageContentAlpha, not the raw overlayHidesPage boolean - the boolean flips the
                // instant an overlay is told to close, but the overlay's own exit animation (its fadeOut
                // + slideOutVertically) keeps it visually on screen, gradually revealing the pager
                // underneath, for another 110-200ms after that. Unfreezing on the boolean alone reactivated
                // live crossfade math *during* that reveal instead of after it - exactly the window where
                // a transient glitch would read as "clearer"/more noticeable than the same thing happening
                // behind an instant cut (e.g. the system back gesture, which visually swaps far more
                // abruptly and gives a glitch nowhere near as much exposed time to be seen).
                val lowerIndex: Int
                val upperIndex: Int
                val crossfadeFraction: Float
                if (overlayHidesPage || pageContentAlpha < 0.999f) {
                    lowerIndex = realIndex(pagerState.currentPage)
                    upperIndex = lowerIndex
                    crossfadeFraction = 0f
                } else {
                    val continuousPage = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    val lowerVirtual = kotlin.math.floor(continuousPage).toInt()
                    lowerIndex = realIndex(lowerVirtual)
                    upperIndex = realIndex(kotlin.math.ceil(continuousPage).toInt())
                    crossfadeFraction = (continuousPage - lowerVirtual).coerceIn(0f, 1f)
                }
                val lowerPageId = pages.getOrNull(lowerIndex)?.id
                val upperPageId = pages.getOrNull(upperIndex)?.id
                val lowerBackground = lowerPageId?.let { pageBackgroundCache[it] } ?: BackgroundConfig()
                val upperBackground = if (upperPageId != null && upperPageId != lowerPageId) {
                    pageBackgroundCache[upperPageId] ?: BackgroundConfig()
                } else null
                BackgroundCrossfade(lowerBackground, upperBackground, crossfadeFraction, Modifier.fillMaxSize())

                HorizontalPager(
                    state = pagerState,
                    // Was (pages.size - 1) - every page composed all the time, specifically to stop home
                    // from being disposed/recomposed from scratch when returning to it from a page 2+
                    // away. That traded a narrow, occasional hitch for a permanently heavier composition
                    // (every external widget host alive on every page, all the time), and that's what
                    // actually made *everything* feel worse afterward - the app-list transition, general
                    // scrolling, all of it - not just the one swipe this was meant to fix. Back to only
                    // keeping the immediate neighbor pre-warmed, matching how Pager is meant to be used.
                    // While swiping, up to four consecutive virtual pages are composed (the two in view
                    // plus one neighbor each side), so an endless carousel of three pages composes one
                    // page twice - only one copy hosts its app widgets (see hostsAppWidgets below). With
                    // two pages the copies would both be in view at times, so nothing is pre-composed.
                    beyondViewportPageCount = if (endless && pages.size < 3) 0 else 1,
                    // Left/right swipe between Home and its other pages; wraps around when the
                    // carousel is endless. Overlays
                    // (search/app list/Panel/folder) each already render a full-size catch-all Box on
                    // top of the pager (see overlayHidesPage/folderOpen below), which intercepts the
                    // touch before it ever reaches the pager, so no extra gating is needed here for them.
                    userScrollEnabled = true,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val page = pages.getOrNull(realIndex(pageIndex))
                    // A plain Box, not BoxWithConstraints - BoxWithConstraints is backed by SubcomposeLayout,
                    // and subcomposing every page's full widget content (each with its own state, icon
                    // loading, etc.) on first entering the pager's composition range is what made swiping
                    // onto a widget-heavy page feel janky, regardless of whether the widgets were external
                    // or built-in.
                    Box(
                        // The 4 rebindable HomeGestures (long-press/double-tap/swipe-up/swipe-down), on
                        // every page - the Dock and the Panel are Home's and show on every page, so their
                        // open gestures work everywhere too. Swipe-up/-down double as the Dock's/Panel's own
                        // open gesture: when the Dock is in MANUAL mode (or the Panel is enabled), that
                        // gesture is claimed outright instead of running its normally-bound action - 
                        // mirrored in SettingsGesturesScreen, which shows that slot locked with a reason
                        // instead of letting it be rebound. A top/bottom safe zone keeps Android's own
                        // edge gestures (hidden status bar reveal; gesture-nav task switcher) reachable.
                        modifier = Modifier.fillMaxSize().then(
                            if (page != null) {
                                Modifier.detectHomeGestures(
                                    onLongPress = { viewModel.executeGestureAction(com.bisc.flauncher.data.model.HomeGesture.LONG_PRESS, context, openPageById, openSettingsFromGesture) },
                                    onDoubleTap = { viewModel.executeGestureAction(com.bisc.flauncher.data.model.HomeGesture.DOUBLE_TAP, context, openPageById, openSettingsFromGesture) },
                                    onSwipeUp = {
                                        if (dockVisibilityMode == com.bisc.flauncher.data.model.DockVisibilityMode.MANUAL && homeDockRow?.enabled == true) {
                                            dockExpanded = true
                                        } else {
                                            viewModel.executeGestureAction(com.bisc.flauncher.data.model.HomeGesture.SWIPE_UP, context, openPageById, openSettingsFromGesture)
                                        }
                                    },
                                    onSwipeDown = {
                                        if (homePanelRow?.enabled == true) {
                                            viewModel.openOverview()
                                        } else {
                                            viewModel.executeGestureAction(com.bisc.flauncher.data.model.HomeGesture.SWIPE_DOWN, context, openPageById, openSettingsFromGesture)
                                        }
                                    },
                                    topSafeZonePx = edgeSafeZonePx,
                                    bottomSafeZonePx = edgeSafeZonePx,
                                    exclusion = homeGestureExclusion
                                )
                            } else {
                                Modifier
                            }
                        )
                    ) {
                    if (page != null) {
                        val pageRows = rowsByPage[page.id].orEmpty()
                        // Neither the dock nor the Panel render inline - both float outside the pager
                        // as their own overlays (see the floating dock Box and Panel overlay below).
                        val withoutPinnedSearchBar = pageRows.filter { it.type != PageRowType.SEARCH_BAR && it.type != PageRowType.PANEL }
                        // Home's Dock floats over every page, flush to the screen's bottom edge: content
                        // reserves its exact height (1 or 2 rows, captions) plus the nav bar inset, and
                        // keeps the same gap above it as between its own rows.
                        val shownDock = homeDockRow?.takeIf { it.enabled }
                        val reservedForDock = shownDock?.let { DockDimens.height(it) + PageRowDimens.rowSpacing }?.plus(
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        )
                        // Starts from the last known config, so a page re-entering composition while swiping
                        // doesn't flash its widgets' loading state first.
                        val pageBackgroundConfigOrNull by remember(page.id) { viewModel.pageBackgroundConfig(page.id) }
                            .collectAsState(initial = pageBackgroundCache[page.id])
                        LaunchedEffect(page.id, pageBackgroundConfigOrNull) {
                            pageBackgroundConfigOrNull?.let { config ->
                                // Decode a page's own image before the shared layer ever crossfades to
                                // it - a still-loading upper layer is what read as flicker.
                                config.imagePath?.takeIf { config.source == com.bisc.flauncher.data.model.BackgroundSource.IMAGE }?.let { path ->
                                    withContext(Dispatchers.Default) { com.bisc.flauncher.util.BackgroundImageStore.load(context, path) }
                                }
                                pageBackgroundCache[page.id] = config
                            }
                        }
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
                                if (reservedForDock != null) Modifier.padding(bottom = reservedForDock)
                                else Modifier.navigationBarsPadding().padding(bottom = 16.dp)
                            )
                        // Row units stretch so that a full page (its unit count, see PageGrid) ends exactly
                        // one row gap above the Dock, the same gap as between rows.
                        val bottomReserve = reservedForDock
                            ?: (WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                        val rowUnitHeight = with(LocalDensity.current) {
                            PageRowDimens.stretchedUnit(pagerHeightPx.toDp() - animatedTopInset - 16.dp - bottomReserve, rowUnitCounts[page.id] ?: 0)
                        }
                        CompositionLocalProvider(
                            LocalSurfaceIsDark provides pageSurfaceIsDark,
                            LocalSurfaceIsDarkLoading provides (loadedPageBackgroundConfig == null),
                            LocalRowUnitHeight provides rowUnitHeight,
                            LocalHomeGestureExclusion provides homeGestureExclusion,
                            com.bisc.flauncher.ui.home.widgets.LocalReuseWidgetViews provides true,
                            // An app widget has one view: of a page composed twice (three-page endless
                            // carousel), only the copy in view or right next to it hosts it - the
                            // other one, off-screen, shows an empty card.
                            com.bisc.flauncher.ui.home.widgets.LocalHostAppWidgets provides
                                (!(endless && pages.size == 3) || (pageIndex - pagerState.currentPage) in -1..1)
                        ) { PageRowsColumn(
                                rows = withoutPinnedSearchBar,
                                entriesByRow = rowEntriesByRow,
                                folders = folders,
                                folderItemsByFolder = folderItemsByFolder,
                                savedMapTargets = savedMapTargets,
                                pinnedWebsites = pinnedWebsites,
                                pages = allPages,
                                loadIcon = viewModel::loadIcon,
                                loadPackIcon = viewModel::loadPackIcon,
                                resolveLabel = resolveLabel,
                                onFolderClick = viewModel::openFolder,
                                onShortcutClick = viewModel::launchFolderShortcut,
                                onOpenPageLink = onOpenPageLink,
                                onExecuteAction = { action -> viewModel.executeAction(action, context, openPageById, openSettingsFromGesture) },
                                iconShape = iconShape,
                                iconSizeScale = iconSizeScale,
                                modifier = contentModifier.graphicsLayer { alpha = pageContentAlpha }
                            ) }
                    }
                    }
                }
            }
        }

        // The dock is a bottom-anchored, full-width panel flush with the screen's own bottom edge - 
        // the home page's own pinned SEARCH_BAR row, laid out full-width inside a DockPanelSurface
        // instead of floating as its own pill (that look is now exclusive to an ordinary *inline*
        // pillMode row, see PillRow). Mirrors the Panel's own shape/border treatment below, just
        // upside down: rounded only at the top (flush/square at the bottom), a border only on the
        // top edge (the row's own borderEnabled column means exactly that for this row). No
        // permanent on-screen affordance - in MANUAL mode a swipe-up from the home content
        // (see detectHomeGestures above) is the only way to open it, never a visible notch. Placed
        // before the search/app-list overlays below so those naturally cover it when open;
        // folderHideDock keeps the existing "hide the dock while a folder is open" behavior.
        if (homeDockRow != null && homeDockRow.enabled && !(folderOpen && folderHideDock)) {
            // Tapping the open background collapses the dock, same as tapping outside any modal - 
            // only live while there's actually something to collapse back to.
            if (dockExpanded && dockCanCollapse) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { dockExpanded = false }
                )
            }
            val dockAppearance = homeDockRow.appearance()
            // Only the top corners round (the dock sits flush with the screen's bottom edge) - the
            // percent scales how far towards the 28dp max each one goes, 0 staying perfectly flat.
            val dockCornerRadius = 28.dp * (homeDockRow.cornerRadiusPercent.coerceIn(0, 100) / 100f)
            val dockShape = RoundedCornerShape(topStart = dockCornerRadius, topEnd = dockCornerRadius)
            val dockBorderColor = resolveElementColors(dockAppearance).borderColor
            // Mirrors the Panel's own BoxWithConstraints+fixed-height treatment just above (see its
            // comment) - the Dock can now be 1 or 2 rows tall (dockRowShape), so its size needs to be
            // predictable up front rather than estimated from content, same reasoning as the Panel.
            // Computed exactly from the same padding/rowGap/row-height constants the content itself
            // uses below, rather than a flat generous guess - any slack here is slack the Column's
            // Arrangement.Top has to center into, pushing the top row down from the top edge.
            val dockRowShape = homeDockRow.dockRows()
            val dockHeight = DockDimens.height(homeDockRow)
            androidx.compose.animation.AnimatedVisibility(
                visible = dockExpanded,
                enter = fadeIn(tween(160)) + androidx.compose.animation.expandVertically(tween(200), expandFrom = Alignment.Bottom),
                exit = fadeOut(tween(140)) + androidx.compose.animation.shrinkVertically(tween(180), shrinkTowards = Alignment.Bottom),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(dockHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                DockPanelSurface(
                    appearance = dockAppearance.copy(borderEnabled = false),
                    shape = dockShape,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (dockAppearance.borderEnabled) {
                                Modifier.freeEdgeBorder(1.dp, dockBorderColor, dockCornerRadius, FreeEdge.TOP)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Column(
                        // Top-anchored, not centered - dockHeight is now computed to match the content
                        // exactly, so Center would just be an identity no-op when the estimate is right
                        // and silently reintroduce a gap above the top row if it's ever slightly off.
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier
                            .fillMaxSize()
                            // Top gap matches the full icon-to-phone-edge gap (DockDimens.topPadding,
                            // i.e. the pill's own outer horizontalMargin plus innerHorizontalPadding)
                            // rather than innerHorizontalPadding alone, which undershoots it - see
                            // DockDimens.rowGap for the (separate) gap this creates *between* the
                            // Dock's own sub-rows.
                            .padding(
                                start = DockDimens.innerHorizontalPadding,
                                end = DockDimens.innerHorizontalPadding,
                                top = DockDimens.topPadding,
                                bottom = DockDimens.innerVerticalPadding
                            )
                            .navigationBarsPadding()
                    ) {
                        GridEntries(
                            entries = rowEntriesByRow[homeDockRow.id].orEmpty(),
                            columns = 1,
                            rowColumns = dockRowShape,
                            fixedSlots = true,
                            referenceColumnCount = DockDimens.REFERENCE_SLOT_COUNT,
                            rowGap = DockDimens.rowGap,
                            folders = folders,
                            folderItemsByFolder = folderItemsByFolder,
                            savedMapTargets = savedMapTargets,
                            pinnedWebsites = pinnedWebsites,
                            pages = allPages,
                            loadIcon = viewModel::loadIcon,
                            loadPackIcon = viewModel::loadPackIcon,
                            resolveLabel = resolveLabel,
                            onFolderClick = viewModel::openFolder,
                            onShortcutClick = viewModel::launchFolderShortcut,
                            onOpenPageLink = onOpenPageLink,
                            pinnedContacts = pinnedContacts,
                            loadContactPhoto = viewModel::loadContactPhoto,
                            onContactClick = { contactId ->
                                pinnedContacts.find { it.contactId == contactId }?.let { contact ->
                                    com.bisc.flauncher.util.openContact(
                                        context,
                                        com.bisc.flauncher.data.model.ContactInfo(contact.contactId, contact.lookupKey, contact.displayName, contact.photoUri)
                                    )
                                }
                            },
                            actionButtons = actionButtons,
                            onActionButtonClick = onActionButtonClick,
                            showLabels = homeDockRow.showLabels,
                            labelColor = homeDockRow.appearance().contentColor?.let { Color(it) } ?: Color.White,
                            iconSizeScale = iconSizeScale
                            // Live dock is display-only now - adding/removing entries only happens in
                            // Settings > Home screen > Layout > Dock > Elements.
                        )
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

        // Dims the actual home screen behind the Panel instead of replacing it outright - a scrim keeps
        // the real home screen (and its wallpaper) visible but dimmed underneath, like a standard modal
        // backdrop, instead of painting over the entire screen with a flat color.
        AnimatedVisibility(
            visible = isPanelOpen && !folderOpen && homePanelRow?.enabled != false,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.fillMaxSize()
        ) {
            // Tapping the dimmed area outside the panel closes it, same as tapping outside any modal.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = viewModel::closeOverview
                    )
            )
        }

        // The Panel is a box that slides down from the very top edge of the screen - full width.
        // Bottom corners rounded, top corners square since the panel is visually attached to the
        // screen's own top edge. Its content is the same GridEntries grid an ordinary element box
        // uses, just laid out full-width inside DockPanelSurface instead of a WidgetCard, mirroring
        // exactly how the Dock now uses the same GridEntries grid for its own floating bar - 
        // the Dock is a mirrored Panel (only-top-rounded corners, capped at 2 rows instead of 6).
        //
        // BoxWithConstraints sits *outside* the AnimatedVisibility, and the AnimatedVisibility below
        // gets a fixed width/height instead of sizing to its content - expandVertically would otherwise
        // force a full remeasure (a subcompose pass, since BoxWithConstraints is backed by
        // SubcomposeLayout) on every animation frame, which makes opening the panel choppy.
        BoxWithConstraints(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
            // Top square - flush and seamless against the screen's own top edge, no gap at the device's
            // rounded corners. Bottom stays rounded (the panel's "resting" edge, not attached to
            // anything) - the percent scales how far towards the 28dp max it goes, same as the Dock.
            val panelCornerRadius = 28.dp * ((homePanelRow?.cornerRadiusPercent ?: com.bisc.flauncher.ui.theme.DEFAULT_CORNER_RADIUS_PERCENT).coerceIn(0, 100) / 100f)
            val panelShape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = panelCornerRadius, bottomEnd = panelCornerRadius)
            val panelRowShape = homePanelRow?.panelRows() ?: listOf(4, 4, 4)
            // A fixed shape the user picked up front (one column count per row), not an estimate
            // from content - predictable size that doesn't jump around as entries are added/removed,
            // same idea as the dock's own fixed pillMaxEntries width.
            val panelHeight = (96.dp * panelRowShape.size + 64.dp).coerceIn(140.dp, maxHeight)
            val panelAppearance = homePanelRow?.appearance() ?: com.bisc.flauncher.data.model.ElementAppearance(borderEnabled = true)
            val panelBorderColor = resolveElementColors(panelAppearance).borderColor

            AnimatedVisibility(
                visible = isPanelOpen && !folderOpen && homePanelRow?.enabled != false,
                enter = expandVertically(animationSpec = tween(160), expandFrom = Alignment.Top) + fadeIn(tween(100)),
                exit = shrinkVertically(animationSpec = tween(200), shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
                modifier = Modifier.fillMaxWidth().height(panelHeight)
            ) {
                DockPanelSurface(
                    appearance = panelAppearance.copy(borderEnabled = false),
                    shape = panelShape,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (panelAppearance.borderEnabled) {
                                Modifier.freeEdgeBorder(1.dp, panelBorderColor, panelCornerRadius, FreeEdge.BOTTOM)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = maxOf(cachedStatusBarInset, cachedCutoutInset))
                            .padding(16.dp)
                    ) {
                        item {
                            homePanelRow?.let { row ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                GridEntries(
                                    entries = rowEntriesByRow[row.id].orEmpty(),
                                    columns = 1,
                                    rowColumns = panelRowShape,
                                    fixedSlots = true,
                                    folders = folders,
                                    folderItemsByFolder = folderItemsByFolder,
                                    savedMapTargets = savedMapTargets,
                                    pinnedWebsites = pinnedWebsites,
                                    pages = allPages,
                                    loadIcon = viewModel::loadIcon,
                                    loadPackIcon = viewModel::loadPackIcon,
                                    resolveLabel = resolveLabel,
                                    onFolderClick = { folderId -> viewModel.openFolder(folderId) },
                                    onShortcutClick = viewModel::launchFolderShortcut,
                                    onOpenPageLink = onOpenPageLink,
                                    pinnedContacts = pinnedContacts,
                                    loadContactPhoto = viewModel::loadContactPhoto,
                                    onContactClick = { contactId ->
                                        pinnedContacts.find { it.contactId == contactId }?.let { contact ->
                                            com.bisc.flauncher.util.openContact(
                                                context,
                                                com.bisc.flauncher.data.model.ContactInfo(contact.contactId, contact.lookupKey, contact.displayName, contact.photoUri)
                                            )
                                        }
                                    },
                                    actionButtons = actionButtons,
                                    onActionButtonClick = onActionButtonClick,
                                    showLabels = row.showLabels,
                                    labelColor = row.appearance().contentColor?.let { Color(it) } ?: Color.White,
                                    iconSizeScale = iconSizeScale
                                    // Live panel is display-only now - adding/removing entries only
                                    // happens in Settings > Home screen > Layout > Panel > Elements.
                                )
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    // All three cover the same "classic back-swipe/home gesture always lands you on the actual home
    // screen" contract: from either search surface, from plain app overview, and from any other page
    // with nothing open on top - not just closing whatever overlay happens to be up while leaving the
    // pager wherever it already was.
    BackHandler(enabled = (appListOpen || appGridOpen) && !searchOpen) { returnHome() }
    BackHandler(enabled = searchOpen) { returnHome() }
    BackHandler(enabled = !appListOpen && !appGridOpen && !searchOpen && !folderOpen && realIndex(pagerState.currentPage) != homeIndex) { returnHome() }
    // Back first leaves the app list's own search, then the list.
    BackHandler(enabled = appListOpen && appListSearching && !searchOpen && !folderOpen) {
        appListSearching = false
        viewModel.updateAppQuery("")
    }
    // Back closes an open Panel rather than leaving the app.
    BackHandler(enabled = isPanelOpen) { viewModel.closeOverview() }
    LaunchedEffect(homeButtonPressCount) { if (homeButtonPressCount > 0) returnHome() }
    AnimatedVisibility(
        visible = appListOpen,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(110)) + slideInVertically(animationSpec = tween(130)) { it / 8 },
        exit = fadeOut(tween(110)) + slideOutVertically(animationSpec = tween(130)) { it / 8 }
    ) {
        // Pulling out of the app overview goes through returnHome(), so it always lands on home.
        // rememberUpdatedState (not a plain closure over returnHome) because this connection object is
        // remembered once via `viewModel` and must keep calling the *current* returnHome - which closes
        // over pagerState/homeIndex/pages that are only valid for the composition that created them - 
        // rather than whatever stale one existed the first time this was built.
        val currentReturnHome = rememberUpdatedState(returnHome)
        val pullDownToClose = remember(viewModel) {
            object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                private var pulled = 0f
                override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                    pulled = if (available.y > 0f && source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) pulled + available.y else 0f
                    if (pulled > 260f) { pulled = 0f; currentReturnHome.value() }
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
              Column(
                  modifier = Modifier
                      .fillMaxSize()
                      .then(if (appListSearching) Modifier.windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)) else Modifier)
              ) {
                AppOverviewContent(
                    showSearchButton = appListShowSearchButton && !appListSearching,
                    allApps = if (appListSearching && appQuery.isNotBlank()) appQueryResults.sortedBy { it.label.lowercase() } else browsableApps,
                    showAzIndex = appListShowAzIndex,
                    headerLetterAppearance = appListHeaderLetterAppearance,
                    pageBackground = appOverviewBackground,
                    railAppearance = railAppearance,
                    accentColor = appListAccentColor,
                    handedness = appOverviewHandedness,
                    cornerRadiusPercent = cornerRadiusPercent,
                    loadIcon = viewModel::loadIcon,
                    onAppClick = { app ->
                        launchApp(context, app.componentName)
                        viewModel.closeAppList()
                    },
                    // With a hidden status bar only the display cutout inset remains.
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(
                            if (immersiveSearch) Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                            else Modifier.statusBarsPadding()
                        )
                        .padding(top = 16.dp)
                )
                if (appListSearching) {
                    com.bisc.flauncher.ui.home.search.AppSearchField(
                        query = appQuery,
                        onQueryChange = viewModel::updateAppQuery,
                        onSubmit = {
                            appQueryResults.firstOrNull()?.let { app ->
                                launchApp(context, app.componentName)
                                viewModel.closeAppList()
                            }
                        },
                        appearance = appListSearchButtonAppearance ?: railAppearance ?: automaticRailAppearance(overviewSurfaceDark),
                        cornerRadiusPercent = cornerRadiusPercent,
                        autoOpen = true,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
                    )
                    if (searchKeyboardCapEnabled) {
                        com.bisc.flauncher.ui.home.search.KeyboardCap(searchKeyboardCapColor, searchKeyboardCapCornerRadiusPercent, searchKeyboardCapArrow)
                    }
                }
              }
                if (appListShowSearchButton && !appListSearching) {
                    com.bisc.flauncher.ui.home.search.AppOverviewSearchButton(
                        onClick = { if (appListOwnSearch) appListSearching = true else viewModel.openSearch() },
                        handedness = appOverviewHandedness,
                        cornerRadiusPercent = cornerRadiusPercent,
                        appearance = appListSearchButtonAppearance ?: railAppearance ?: automaticRailAppearance(overviewSurfaceDark)
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = appGridOpen,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(110)) + slideInVertically(animationSpec = tween(130)) { it / 8 },
        exit = fadeOut(tween(110)) + slideOutVertically(animationSpec = tween(130)) { it / 8 }
    ) {
        val gridSurfaceDark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), appGridBackground)
        val gridFolders = remember(folders, appGridFolderIds) { appGridFolderIds.mapNotNull { id -> folders.find { it.id == id } } }
        val gridApps = remember(browsableApps, gridFolders, folderItemsByFolder, appGridHideFolderApps) {
            if (!appGridHideFolderApps) browsableApps
            else {
                val inFolders = gridFolders.flatMap { folderItemsByFolder[it.id].orEmpty() }
                    .filter { it.type == GridItemType.APP }.map { it.payload }.toSet()
                browsableApps.filter { it.componentName.flattenToString() !in inFolders }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
        ) {
            BackgroundFill(appGridBackground, Modifier.fillMaxSize())
            CompositionLocalProvider(LocalAppIconLoader provides viewModel.iconLoader) {
                com.bisc.flauncher.ui.home.search.AppGridContent(
                    apps = gridApps,
                    folders = gridFolders,
                    foldersAlphabetical = appGridFoldersAlphabetical,
                    folderItemsByFolder = folderItemsByFolder,
                    query = appQuery,
                    queryResults = appQueryResults,
                    columns = appGridColumns,
                    globalIconScale = iconSizeScale,
                    gridIconScale = appGridIconSizePercent / 100f,
                    showLabels = appGridShowLabels,
                    labelColor = com.bisc.flauncher.ui.home.dock.resolveElementColors(
                        ElementAppearance(fillMode = ElementFillMode.NONE), gridSurfaceDark
                    ).contentColor,
                    barPosition = appGridBarPosition,
                    barOpensTextSearch = appGridBarOpensTextSearch,
                    keyboardAutoOpen = appGridKeyboardAutoOpen,
                    barAppearance = appGridBarAppearance ?: automaticRailAppearance(gridSurfaceDark),
                    cornerRadiusPercent = cornerRadiusPercent,
                    keyboardCapColor = if (searchKeyboardCapEnabled) searchKeyboardCapColor else null,
                    keyboardCapCornerRadiusPercent = searchKeyboardCapCornerRadiusPercent,
                    keyboardCapArrow = searchKeyboardCapArrow,
                    loadIcon = viewModel::loadIcon,
                    loadPackIcon = viewModel::loadPackIcon,
                    onQueryChange = viewModel::updateAppQuery,
                    onAppClick = { app ->
                        launchApp(context, app.componentName)
                        viewModel.closeAppGrid()
                    },
                    onFolderClick = viewModel::openFolder,
                    onOpenTextSearch = viewModel::openSearch,
                    modifier = Modifier
                        .then(
                            if (appGridHideStatusBar) Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                            else Modifier.statusBarsPadding()
                        )
                        .padding(top = 16.dp)
                )
            }
        }
    }

    val searchPivotX = if (appOverviewHandedness == Handedness.LEFT) 0.12f else 0.88f
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
            converterResult = converterResult,
            contactResults = contactResults,
            websiteResults = websiteResults,
            shortcutResults = shortcutResults,
            folderResults = folderResults,
            mapTargetResults = mapTargetResults,
            contactsPermissionGranted = contactsPermissionGranted,
            statusBarHidden = immersiveSearch,
            categoryOrder = searchCategoryOrder,
            activeCategories = activeSearchCategories,
            categoryFilter = searchFilter,
            categoryColors = categoryColors,
            aiAppPackage = searchAiApp,
            onToggleFilter = viewModel::toggleSearchFilter,
            keyboardAutoOpen = searchKeyboardAutoOpen,
            appearance = searchAppearance,
            cornerRadiusPercent = cornerRadiusPercent,
            background = searchBackground,
            barAppearance = searchBarAppearance,
            barCornerRadiusPercent = searchBarCornerRadiusPercent,
            resultsAppearance = searchResultsAppearance,
            resultsCornerRadiusPercent = searchResultsCornerRadiusPercent,
            resultsEdgeToEdge = searchResultsEdgeToEdge,
            barAtBottom = searchBarAtBottom,
            barShowCategories = searchBarShowCategories,
            keyboardCapColor = if (searchKeyboardCapEnabled) searchKeyboardCapColor else null,
            keyboardCapCornerRadiusPercent = searchKeyboardCapCornerRadiusPercent,
            keyboardCapArrow = searchKeyboardCapArrow,
            loadIcon = viewModel::loadIcon,
            onQueryChange = viewModel::updateSearchQuery,
            onAppClick = { app ->
                launchApp(context, app.componentName)
                viewModel.closeSearch()
            },
            onContactClick = { contact -> contactActionsFor = contact },
            onSettingsShortcutClick = { shortcut ->
                launchSettingsShortcut(context, shortcut.action)
                viewModel.closeSearch()
            },
            onLauncherSettingsClick = { target ->
                viewModel.closeSearch()
                val homeId = pages.find { it.isHome }?.id
                if (homeId != null || com.bisc.flauncher.data.model.LauncherSettingsTarget.HOME !in target.route) {
                    onNavigateToSettings(target.route.replace(com.bisc.flauncher.data.model.LauncherSettingsTarget.HOME, homeId.toString()))
                }
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
            onWebCommand = { viewModel.runWebSearchCommand(context) },
            onMapsCommand = { viewModel.runMapsCommand(context) },
            onAiCommand = { viewModel.runAiCommand(context) }
        )
    }

    contactActionsFor?.let { contact ->
        com.bisc.flauncher.ui.home.search.ContactActionsSheet(
            contact = contact,
            loadPhoto = viewModel::loadContactPhoto,
            onDismiss = { contactActionsFor = null }
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
            onShortcutSelected = { info -> viewModel.pinShortcut(info) },
            onDismiss = viewModel::dismissShortcutPicker
        )
    }

    openFolderPath.lastOrNull()?.let { folderId ->
        val folder = folders.find { it.id == folderId }
        if (folder != null) {
            FolderView(
                folder = folder,
                items = folderItemsByFolder[folderId].orEmpty(),
                folders = folders,
                folderItemsByFolder = folderItemsByFolder,
                savedMapTargets = savedMapTargets,
                pinnedWebsites = pinnedWebsites,
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
                        GridItemType.FOLDER, GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET, GridItemType.PAGE, GridItemType.CONTACT, GridItemType.ACTION_BUTTON -> Unit
                    }
                    viewModel.closeAllFolders()
                    viewModel.closeOverview()
                },
                onFolderClick = { item -> item.payload.toLongOrNull()?.let(viewModel::openFolder) },
                onRemoveItem = viewModel::removeFromFolder,
                onReorderItems = viewModel::reorderFolderItems,
                onBack = viewModel::closeFolder,
                cardAppearance = folder.effectiveAppearance(folderCardAppearance),
                showLabels = if (folder.showLabelsOverrideEnabled) folder.showLabels else folderShowLabels
            )
        }
    }

}

/** Virtual rounds an endless carousel spans (it starts in the middle) - far more than anyone swipes. */
private const val ENDLESS_CAROUSEL_LOOPS = 1000
