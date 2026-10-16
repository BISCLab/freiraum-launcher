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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.bisc.flauncher.util.setStatusBarHidden
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.Note
import com.bisc.flauncher.data.model.ContactInfo
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.WidgetCornerStyle
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.panelRows
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.ContactPickerSheet
import com.bisc.flauncher.ui.home.dock.AddWebsiteDialog
import com.bisc.flauncher.ui.home.dock.AppShortcutPickerDialog
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.ui.home.dock.DockDimens
import com.bisc.flauncher.ui.home.dock.DockPanelSurface
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet
import com.bisc.flauncher.ui.home.dock.resolveElementColors
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
    // Page-link entries (GridItemType.PAGE, including the dock's own) need the full list to resolve
    // an icon for the Search/App-overview singletons too — `pages` above is ROWS-only (the pager's own
    // list), which is why those two always fell back to the generic "unknown page" glyph otherwise.
    val allPages by viewModel.allPages.collectAsState()
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val appEntriesByRow by viewModel.appEntriesByRow.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val resolveLabel = viewModel::resolveLabel
    val handedness by viewModel.handedness.collectAsState()
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
    val shortcutAppPickerOpen by viewModel.shortcutAppPickerOpen.collectAsState()
    val shortcutPickerForApp by viewModel.shortcutPickerForApp.collectAsState()
    val availableShortcuts by viewModel.availableShortcuts.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    val railAppearance by viewModel.searchAlphabetRailAppearance.collectAsState()
    val chargingAnimationEnabled by viewModel.chargingAnimationEnabled.collectAsState()
    val chargingAnimationColor by viewModel.chargingAnimationColor.collectAsState()
    val searchAppearance by viewModel.searchAppearance.collectAsState()
    val searchKeyboardAutoOpen by viewModel.searchKeyboardAutoOpen.collectAsState()
    val appListDisplayMode by viewModel.appListDisplayMode.collectAsState()
    val appListLayout by viewModel.appListLayout.collectAsState()
    val appListSearchButtonAppearance by viewModel.appListSearchButtonAppearance.collectAsState()
    val appListShowSearchButton by viewModel.appListShowSearchButton.collectAsState()
    val appListContentAppearance by viewModel.appListContentAppearance.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val sidebarPanelAppearance by viewModel.sidebarPanelAppearance.collectAsState()
    val folderHideDock by viewModel.folderHideDock.collectAsState()
    val folderCardAppearance by viewModel.folderCardAppearance.collectAsState()
    val keyboardAppearance by viewModel.keyboardAppearance.collectAsState()
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
    val searchCategoryOrder by viewModel.searchCategoryOrder.collectAsState()
    val activeSearchCategories by viewModel.activeSearchCategories.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()
    val searchAiApp by viewModel.searchAiApp.collectAsState()
    val categoryColors by viewModel.categoryColors.collectAsState()
    val converterResult by viewModel.converterResult.collectAsState()

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
    // The Panel is part of the home page now (no separate "quick overview" appearance layer), so
    // it just inherits currentPageHideStatusBar like everything else on Home — no dedicated branch.
    val hideStatusBar = when {
        folderOpen -> folderHideStatusBar
        onAppOverviewPage -> appOverviewHideStatusBar
        searchOpen -> searchHideStatusBar
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
    // Search has no separate page-level backdrop any more (see searchAppearance) — its own single
    // full-bleed ElementAppearance is the "fill" layer here, blended against the current page's
    // background behind it exactly like any other translucent/glass surface would be.
    val backdropDark = if (searchOpen) {
        com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(searchAppearance, currentPageBackground)
    } else {
        val statusBarBackdrop = when {
            appListOpen -> appOverviewBackground
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

    // See the background-crossfade layer below for why this exists: a stable, append-only cache instead
    // of re-subscribing a Flow every time a page's role (lower/upper) in the crossfade changes.
    val pageBackgroundCache = remember { mutableStateMapOf<Long, BackgroundConfig>() }

    val homeIndex = pages.indexOfFirst { it.isHome }.coerceAtLeast(0)

    var didInitialPageScroll by remember { mutableStateOf(false) }
    LaunchedEffect(pages) {
        if (!didInitialPageScroll && pages.isNotEmpty()) {
            if (pagerState.currentPage != homeIndex) pagerState.scrollToPage(homeIndex)
            didInitialPageScroll = true
        }
    }

    // Hoisted above the pager (rather than declared down where the dock itself renders) so the
    // gesture-detector Box below (home-screen background taps) and the dock's own rendering block
    // further down share one dockExpanded state.
    val homeDockRow = pages.find { it.isHome }
        ?.let { rowsByPage[it.id] }
        ?.find { it.type == PageRowType.SEARCH_BAR }
    val homePanelRow = pages.find { it.isHome }
        ?.let { rowsByPage[it.id] }
        ?.find { it.type == PageRowType.PANEL }
    val dockVisibilityMode = homeDockRow?.dockVisibilityMode ?: com.bisc.flauncher.data.model.DockVisibilityMode.ALWAYS_VISIBLE
    // Ephemeral UI state only, deliberately not persisted — same ephemeral character as the
    // Favorites popup this look mirrors. Resets to its mode's own starting state whenever the
    // row id or the mode itself changes (e.g. the user just switched modes in Settings).
    var dockExpanded by remember(homeDockRow?.id, dockVisibilityMode) {
        mutableStateOf(dockVisibilityMode != com.bisc.flauncher.data.model.DockVisibilityMode.MANUAL)
    }
    val isOnHomePage = pagerState.currentPage == homeIndex
    LaunchedEffect(dockVisibilityMode, isOnHomePage) {
        if (dockVisibilityMode == com.bisc.flauncher.data.model.DockVisibilityMode.AUTO_EXPAND_ON_ARRIVAL && isOnHomePage) {
            dockExpanded = true
            kotlinx.coroutines.delay(2500)
            dockExpanded = false
        }
    }
    val dockCanCollapse = dockVisibilityMode != com.bisc.flauncher.data.model.DockVisibilityMode.ALWAYS_VISIBLE
    // Standard edge-gesture hot-zone size (not tied to the live system inset, which collapses to 0
    // once we hide the status bar — the whole point is keeping Android's own edge swipe reachable
    // even then), used to keep the Dock's/Panel's swipe-gesture claim from eating the top/bottom
    // edge strip Android itself reserves for the status bar reveal and the gesture-nav task switcher.
    val edgeSafeZonePx = with(LocalDensity.current) { 24.dp.toPx() }

    val openPageById: (Long) -> Unit = { pageId ->
        val index = pages.indexOfFirst { it.id == pageId }
        if (index >= 0) coroutineScope.launch { pagerState.animateScrollToPage(index) }
    }
    // Shared by every page-link entry (an AppsRowGrid cell, a pill-row cell, the floating dock) —
    // jumps the pager for a swipeable ROWS page, opens the matching overlay for the two singletons.
    val onOpenPageLink: (Long, com.bisc.flauncher.data.model.LauncherPageKind) -> Unit = { pageId, kind ->
        when (kind) {
            com.bisc.flauncher.data.model.LauncherPageKind.ROWS -> openPageById(pageId)
            com.bisc.flauncher.data.model.LauncherPageKind.APP_LIST -> viewModel.openAppList()
            com.bisc.flauncher.data.model.LauncherPageKind.SEARCH -> viewModel.openSearch()
            com.bisc.flauncher.data.model.LauncherPageKind.SIDEBAR -> viewModel.openOverview()
        }
    }
    // The one place "go back to home" is defined — closes every overlay and, crucially, also scrolls
    // the pager itself back to home. Closing an overlay alone used to leave the pager wherever it
    // already was, which is how the system home gesture (and back-swiping out of search/app overview)
    // could silently strand you on a non-home page instead of actually going home.
    val returnHome: () -> Unit = {
        viewModel.closeSearch()
        viewModel.closeAppList()
        viewModel.closeOverview()
        viewModel.closeAllFolders()
        if (pagerState.currentPage != homeIndex) coroutineScope.launch { pagerState.animateScrollToPage(homeIndex) }
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
    val overlayHidesPage = searchOpen || appListOpen || (isPanelOpen && !folderOpen)
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
                // full images sliding edge-to-edge). Pulled out here as one shared layer behind the pager.
                //
                // Backgrounds are read from pageBackgroundCache (filled below, per page slot) rather than
                // each collecting their own Flow — a page's background is fetched once and cached, then
                // just read back instantly regardless of how the transition logic below uses it.
                //
                // Frame-by-frame capture of an actual swipe (see git history for the two failed attempts
                // this replaced) confirmed the background has to track the *same* continuous position the
                // pager itself uses to lay out content — anything that waits for a discrete "arrived" or
                // "committed" signal (currentPage, targetPage) either lags behind the content's own motion
                // (a visible two-stage "content slides, then bg flashes in afterward") or over-fires on
                // every direction change during an indecisive drag (targetPage changes many times per
                // gesture, restarting a keyed animation each time). Reading currentPageOffsetFraction
                // directly, every frame, with no intermediate Animatable/LaunchedEffect, guarantees the
                // background is always exactly where the pager itself says the transition currently is.
                //
                // Frozen to the settled page alone whenever an overlay (app overview, search, quick
                // overview) covers the pager — a page reported flickering to a neighbor's background
                // specifically while leaving the app overview, and the pager underneath keeps existing
                // (composed, not disposed) the whole time an overlay sits on top of it, so anything that
                // nudged its position while hidden would only become visible right as that overlay's own
                // exit fade reveals it again. There's never a legitimate reason to crossfade while nothing
                // of the pager is even visible, so this sidesteps that class of bug outright rather than
                // chasing down whatever specifically causes the position to move while covered.
                //
                // Gated on pageContentAlpha, not the raw overlayHidesPage boolean — the boolean flips the
                // instant an overlay is told to close, but the overlay's own exit animation (its fadeOut
                // + slideOutVertically) keeps it visually on screen, gradually revealing the pager
                // underneath, for another 110-200ms after that. Unfreezing on the boolean alone reactivated
                // live crossfade math *during* that reveal instead of after it — exactly the window where
                // a transient glitch would read as "clearer"/more noticeable than the same thing happening
                // behind an instant cut (e.g. the system back gesture, which visually swaps far more
                // abruptly and gives a glitch nowhere near as much exposed time to be seen).
                val lowerIndex: Int
                val upperIndex: Int
                val crossfadeFraction: Float
                if (overlayHidesPage || pageContentAlpha < 0.999f) {
                    lowerIndex = pagerState.currentPage.coerceIn(0, pages.lastIndex)
                    upperIndex = lowerIndex
                    crossfadeFraction = 0f
                } else {
                    val continuousPage = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    lowerIndex = kotlin.math.floor(continuousPage).toInt().coerceIn(0, pages.lastIndex)
                    upperIndex = kotlin.math.ceil(continuousPage).toInt().coerceIn(0, pages.lastIndex)
                    crossfadeFraction = (continuousPage - lowerIndex).coerceIn(0f, 1f)
                }
                val lowerPageId = pages.getOrNull(lowerIndex)?.id
                val upperPageId = pages.getOrNull(upperIndex)?.id
                val lowerBackground = lowerPageId?.let { pageBackgroundCache[it] } ?: BackgroundConfig()
                BackgroundFill(lowerBackground, Modifier.fillMaxSize())
                if (upperPageId != null && upperPageId != lowerPageId && crossfadeFraction > 0f) {
                    val upperBackground = pageBackgroundCache[upperPageId] ?: BackgroundConfig()
                    BackgroundFill(upperBackground, Modifier.fillMaxSize().graphicsLayer { alpha = crossfadeFraction })
                }

                HorizontalPager(
                    state = pagerState,
                    // Was (pages.size - 1) — every page composed all the time, specifically to stop home
                    // from being disposed/recomposed from scratch when returning to it from a page 2+
                    // away. That traded a narrow, occasional hitch for a permanently heavier composition
                    // (every external widget host alive on every page, all the time), and that's what
                    // actually made *everything* feel worse afterward — the app-list transition, general
                    // scrolling, all of it — not just the one swipe this was meant to fix. Back to only
                    // keeping the immediate neighbor pre-warmed, matching how Pager is meant to be used.
                    beyondViewportPageCount = 1,
                    // Left/right swipe between Home and its Ergänzungsseiten — the carousel is finite
                    // (no wrap-around) for now, matching the masterplan's stated default; an opt-in
                    // endless mode is a later addition, not required for the default behavior. Overlays
                    // (search/app list/Panel/folder) each already render a full-size catch-all Box on
                    // top of the pager (see overlayHidesPage/folderOpen below), which intercepts the
                    // touch before it ever reaches the pager, so no extra gating is needed here for them.
                    userScrollEnabled = true,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val page = pages.getOrNull(pageIndex)
                    // A plain Box, not BoxWithConstraints — BoxWithConstraints is backed by SubcomposeLayout,
                    // and subcomposing every page's full widget content (each with its own state, icon
                    // loading, etc.) on first entering the pager's composition range is what made swiping
                    // onto a widget-heavy page feel janky, regardless of whether the widgets were external
                    // or built-in.
                    Box(
                        // The 4 rebindable HomeGestures (long-press/double-tap/swipe-up/swipe-down),
                        // restricted to the home page only — closing an overlay they open always lands
                        // you back on the page you actually started the gesture from (returnHome does
                        // the actual closing/scrolling). Swipe-up/-down double as the Dock's/Panel's own
                        // open gesture: when the Dock is in MANUAL mode (or the Panel is enabled), that
                        // gesture is claimed outright instead of running its normally-bound action —
                        // mirrored in SettingsGesturesScreen, which shows that slot locked with a reason
                        // instead of letting it be rebound. A top/bottom safe zone keeps Android's own
                        // edge gestures (hidden status bar reveal; gesture-nav task switcher) reachable.
                        modifier = Modifier.fillMaxSize().then(
                            if (page?.isHome == true) {
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
                                    bottomSafeZonePx = edgeSafeZonePx
                                )
                            } else {
                                Modifier
                            }
                        )
                    ) {
                    if (page != null) {
                        val pageRows = rowsByPage[page.id].orEmpty()
                        val pinnedSearchBarRow = pageRows.find { it.type == PageRowType.SEARCH_BAR }
                        // Neither the dock nor the Panel render inline — both float outside the pager
                        // as their own overlays (see the floating dock Box and Panel overlay below).
                        val withoutPinnedSearchBar = pageRows.filter { it.type != PageRowType.SEARCH_BAR && it.type != PageRowType.PANEL }
                        // Flush to the screen's bottom edge now (no floating margin of its own), so
                        // content reserves the panel's own height plus the system nav bar inset below it.
                        val reservedForDock = DockDimens.barHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + DockDimens.contentGap
                        val pageBackgroundConfigOrNull by remember(page.id) { viewModel.pageBackgroundConfig(page.id) }.collectAsState(initial = null)
                        LaunchedEffect(page.id, pageBackgroundConfigOrNull) {
                            pageBackgroundConfigOrNull?.let { pageBackgroundCache[page.id] = it }
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
                                if (pinnedSearchBarRow != null) Modifier.padding(bottom = reservedForDock)
                                else Modifier.navigationBarsPadding().padding(bottom = 16.dp)
                            )
                        CompositionLocalProvider(
                            LocalSurfaceIsDark provides pageSurfaceIsDark,
                            LocalSurfaceIsDarkLoading provides (loadedPageBackgroundConfig == null)
                        ) { PageRowsColumn(
                                rows = withoutPinnedSearchBar,
                                entriesByRow = appEntriesByRow,
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
                                handedness = handedness,
                                iconShape = iconShape,
                                accentColor = Color(accentColor),
                                modifier = contentModifier.graphicsLayer { alpha = pageContentAlpha }
                            ) }
                    }
                    }
                }
            }
        }

        // The dock is a bottom-anchored, full-width panel flush with the screen's own bottom edge —
        // the home page's own pinned SEARCH_BAR row, laid out full-width inside a DockPanelSurface
        // instead of floating as its own pill (that look is now exclusive to an ordinary *inline*
        // pillMode row, see PillRow). Mirrors the Panel's own shape/border treatment below, just
        // upside down: rounded only at the top (flush/square at the bottom), a border only on the
        // top edge (the row's own borderEnabled column now means exactly that for this row). No
        // permanent on-screen affordance any more — in MANUAL mode a swipe-up from the home content
        // (see detectHomeGestures above) is the only way to open it, never a visible notch. Placed
        // before the search/app-list overlays below so those naturally cover it when open;
        // folderHideDock keeps the existing "hide the dock while a folder is open" behavior.
        if (homeDockRow != null && homeDockRow.enabled && !(folderOpen && folderHideDock)) {
            // Tapping the open background collapses the dock, same as tapping outside any modal —
            // only live while there's actually something to collapse back to.
            if (dockExpanded && dockCanCollapse) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { dockExpanded = false }
                )
            }
            val dockAppearance = homeDockRow.appearance()
            val dockShape = if (homeDockRow.cornerStyle == WidgetCornerStyle.SQUARE) {
                RoundedCornerShape(0.dp)
            } else {
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            }
            val dockBorderColor = resolveElementColors(dockAppearance).borderColor
            androidx.compose.animation.AnimatedVisibility(
                visible = dockExpanded,
                enter = fadeIn(tween(160)) + androidx.compose.animation.expandVertically(tween(200), expandFrom = Alignment.Bottom),
                exit = fadeOut(tween(140)) + androidx.compose.animation.shrinkVertically(tween(180), shrinkTowards = Alignment.Bottom),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                DockPanelSurface(
                    appearance = dockAppearance.copy(borderEnabled = false),
                    shape = dockShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (dockAppearance.borderEnabled) {
                                Modifier.drawBehind {
                                    drawLine(dockBorderColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                                }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    PillRowEntries(
                        entries = appEntriesByRow[homeDockRow.id].orEmpty(),
                        folders = folders,
                        folderItemsByFolder = folderItemsByFolder,
                        savedMapTargets = savedMapTargets,
                        pinnedWebsites = pinnedWebsites,
                        pages = allPages,
                        loadIcon = viewModel::loadIcon,
                        loadPackIcon = viewModel::loadPackIcon,
                        resolveLabel = resolveLabel,
                        itemShape = pillItemShape(iconShape),
                        onFolderClick = viewModel::openFolder,
                        onShortcutClick = viewModel::launchFolderShortcut,
                        onOpenPageLink = onOpenPageLink,
                        // Live dock is display-only now — adding/removing entries only happens in
                        // Settings > Home screen > Layout > Dock > Elements.
                        onAddEmptySlot = null,
                        maxEntries = homeDockRow.pillMaxEntries,
                        showLabels = homeDockRow.showLabels,
                        labelColor = homeDockRow.appearance().contentColor?.let { Color(it) } ?: Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DockDimens.innerHorizontalPadding, vertical = DockDimens.innerVerticalPadding)
                            .navigationBarsPadding()
                    )
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

        // Dims the actual home screen behind the Panel instead of replacing it outright — a scrim keeps
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

        // The Panel is a box that slides down from the very top edge of the screen — full width.
        // Bottom corners rounded, top corners square since the panel is visually attached to the
        // screen's own top edge. Its content is the same AppsGridEntries grid an ordinary apps row
        // uses, just laid out full-width inside DockPanelSurface instead of a WidgetCard, mirroring
        // exactly how the Dock reuses PillRowEntries the same way for its own floating bar.
        //
        // BoxWithConstraints sits *outside* the AnimatedVisibility, and the AnimatedVisibility below
        // gets a fixed width/height instead of sizing to its content — expandVertically would otherwise
        // force a full remeasure (a subcompose pass, since BoxWithConstraints is backed by
        // SubcomposeLayout) on every single animation frame, which used to make opening the panel feel
        // choppy compared to text search's own, already pre-sized overlay.
        BoxWithConstraints(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
            // Top square — flush and seamless against the screen's own top edge, no gap at the device's
            // rounded corners. Bottom stays rounded (the panel's "resting" edge, not attached to anything).
            val panelShape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
            val panelRowShape = homePanelRow?.panelRows() ?: listOf(4, 4, 4)
            // A fixed shape the user picked up front (one column count per row), not an estimate
            // from content — predictable size that doesn't jump around as entries are added/removed,
            // same idea as the dock's own fixed pillMaxEntries width.
            val panelHeight = (96.dp * panelRowShape.size + 64.dp).coerceIn(140.dp, maxHeight)
            val panelAppearance = homePanelRow?.appearance() ?: sidebarPanelAppearance
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
                                Modifier.drawBehind {
                                    drawLine(panelBorderColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                                }
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
                                AppsGridEntries(
                                    entries = appEntriesByRow[row.id].orEmpty(),
                                    columns = 1,
                                    rowColumns = panelRowShape,
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
                                    showLabels = row.showLabels,
                                    labelColor = row.appearance().contentColor?.let { Color(it) } ?: Color.White
                                    // Live panel is display-only now — adding/removing entries only
                                    // happens in Settings > Home screen > Layout > Panel > Elements.
                                )
                                }
                            }
                        }
                    }
                }
            }
        }

        // TEMPORARY — Dock/Panel no longer open a settings-reachable long-press menu of their own
        // now that live "+" editing is gone from both; until a real way back into Settings from the
        // home screen is wired up, this plain center button stands in for it so Settings stays reachable.
        if (isOnHomePage && !isPanelOpen && !searchOpen && !appListOpen && !folderOpen) {
            androidx.compose.material3.Button(
                onClick = onNavigateToSettings,
                modifier = Modifier.align(Alignment.Center)
            ) { Text("Settings (temp)") }
        }
    }

    // All three cover the same "classic back-swipe/home gesture always lands you on the actual home
    // screen" contract: from either search surface, from plain app overview, and from any other page
    // with nothing open on top — not just closing whatever overlay happens to be up while leaving the
    // pager wherever it already was.
    BackHandler(enabled = appListOpen && !searchOpen) { returnHome() }
    BackHandler(enabled = searchOpen) { returnHome() }
    BackHandler(enabled = !appListOpen && !searchOpen && !folderOpen && pagerState.currentPage != homeIndex) { returnHome() }
    // Was missing entirely before — back while the Apps panel (sidebar overview) was open fell through
    // past the activity and out of the app instead of just closing the panel. Easy to miss while it was
    // only reachable via the dock's drag gesture; much more noticeable now that a notification-shade-style
    // swipe down opens it, since a physical/gesture back press is the expected way to dismiss that.
    BackHandler(enabled = isPanelOpen) { viewModel.closeOverview() }
    LaunchedEffect(homeButtonPressCount) { if (homeButtonPressCount > 0) returnHome() }
    AnimatedVisibility(
        visible = appListOpen,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(110)) + slideInVertically(animationSpec = tween(130)) { it / 8 },
        exit = fadeOut(tween(110)) + slideOutVertically(animationSpec = tween(130)) { it / 8 }
    ) {
        // Pulling out of the app overview this way used to only call closeAppList() — it never touched
        // the pager, so it could silently leave you on whatever page the pager happened to already be on
        // instead of home, exactly like the system-back path did before returnHome() was introduced.
        // rememberUpdatedState (not a plain closure over returnHome) because this connection object is
        // remembered once via `viewModel` and must keep calling the *current* returnHome — which closes
        // over pagerState/homeIndex/pages that are only valid for the composition that created them —
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
            converterResult = converterResult,
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
            categoryColors = categoryColors,
            aiAppPackage = searchAiApp,
            onToggleFilter = viewModel::toggleSearchFilter,
            keyboardAutoOpen = searchKeyboardAutoOpen,
            appearance = searchAppearance,
            keyboardAppearance = keyboardAppearance,
            loadIcon = viewModel::loadIcon,
            onQueryChange = viewModel::updateSearchQuery,
            onAppClick = { app ->
                launchApp(context, app.componentName)
                viewModel.closeSearch()
            },
            onContactClick = { contact -> contactActionsFor = contact },
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
                        GridItemType.FOLDER, GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET, GridItemType.PAGE, GridItemType.CONTACT -> Unit
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
