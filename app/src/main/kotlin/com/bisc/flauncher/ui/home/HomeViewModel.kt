package com.bisc.flauncher.ui.home

import com.bisc.flauncher.util.matchesWordStart
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.AppOverride
import com.bisc.flauncher.data.model.AppRowEntry
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.ContactInfo
import com.bisc.flauncher.data.model.DockTapAction
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderIconMode
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.Note
import com.bisc.flauncher.data.model.NowPlayingInfo
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.PinnedContact
import com.bisc.flauncher.data.model.PinnedShortcut
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.RevealMode
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.data.model.SearchEngine
import com.bisc.flauncher.data.model.SearchKeyboardType
import com.bisc.flauncher.util.launchAiApp
import com.bisc.flauncher.data.model.SidebarCategoryViewMode
import com.bisc.flauncher.data.model.SidebarPinnedApp
import com.bisc.flauncher.data.model.WeatherSnapshot
import com.bisc.flauncher.data.model.AppListDisplayMode
import com.bisc.flauncher.data.model.AppListLayout
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.WidgetCornerStyle
import com.bisc.flauncher.data.repository.AppOverrideRepository
import com.bisc.flauncher.data.repository.ContactsRepository
import com.bisc.flauncher.data.repository.DockRepository
import com.bisc.flauncher.data.repository.FolderRepository
import com.bisc.flauncher.data.repository.InstalledAppsRepository
import com.bisc.flauncher.data.repository.MapsRepository
import com.bisc.flauncher.data.repository.NoteRepository
import com.bisc.flauncher.data.repository.NowPlayingRepository
import com.bisc.flauncher.data.repository.PageContentRepository
import com.bisc.flauncher.data.repository.ShortcutRepository
import com.bisc.flauncher.data.repository.SidebarAppsRepository
import com.bisc.flauncher.data.repository.SidebarRepository
import com.bisc.flauncher.data.repository.WeatherRepository
import com.bisc.flauncher.data.repository.WebsiteRepository
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.home.dock.QuickLaunchSlot
import com.bisc.flauncher.ui.home.dock.ResolvedSidebarItem
import com.bisc.flauncher.ui.home.dock.SidebarEntryType
import com.bisc.flauncher.ui.home.dock.defaultQuickLaunchSlots
import com.bisc.flauncher.ui.home.dock.resolveSidebarItems
import androidx.compose.runtime.snapshotFlow
import com.bisc.flauncher.util.AppIconLoader
import com.bisc.flauncher.util.BackgroundImageStore
import com.bisc.flauncher.util.ContactPhotoLoader
import com.bisc.flauncher.util.SystemActionsUtil
import com.bisc.flauncher.util.SystemWidgetHostManager
import com.bisc.flauncher.util.evaluateCalculatorExpression
import com.bisc.flauncher.util.formatCalculatorResult
import com.bisc.flauncher.util.launchApp
import com.bisc.flauncher.util.launchEmailTo
import com.bisc.flauncher.util.launchMapsQuery
import com.bisc.flauncher.util.launchWebSearch
import com.bisc.flauncher.util.looksLikeCalculatorExpression
import com.bisc.flauncher.util.looksLikeEmail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pageContentRepository: PageContentRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val settingsRepository: SettingsRepository,
    private val contactsRepository: ContactsRepository,
    private val folderRepository: FolderRepository,
    private val noteRepository: NoteRepository,
    private val mapsRepository: MapsRepository,
    private val websiteRepository: WebsiteRepository,
    private val shortcutRepository: ShortcutRepository,
    private val sidebarRepository: SidebarRepository,
    private val sidebarAppsRepository: SidebarAppsRepository,
    private val appOverrideRepository: AppOverrideRepository,
    private val weatherRepository: WeatherRepository,
    private val nowPlayingRepository: NowPlayingRepository,
    private val widgetHostManager: SystemWidgetHostManager,
    private val appIconLoader: AppIconLoader,
    private val contactPhotoLoader: ContactPhotoLoader,
    private val dockRepository: DockRepository
) : ViewModel() {
    // ---- Display settings ----

    fun loadIcon(componentName: ComponentName): ImageBitmap? = appIconLoader.load(componentName)

    val iconLoader: AppIconLoader get() = appIconLoader

    val appListLayout: StateFlow<AppListLayout> =
        settingsRepository.appListLayout.stateIn(viewModelScope, SharingStarted.Eagerly, AppListLayout.LIST)

    val handedness: StateFlow<Handedness> =
        settingsRepository.handedness.stateIn(viewModelScope, SharingStarted.Eagerly, Handedness.RIGHT)

    val dockSingleTapAction: StateFlow<DockTapAction> =
        settingsRepository.dockSingleTapAction.stateIn(viewModelScope, SharingStarted.Eagerly, DockTapAction.QUICK_OVERVIEW)

    val dockDoubleTapAction: StateFlow<DockTapAction> =
        settingsRepository.dockDoubleTapAction.stateIn(viewModelScope, SharingStarted.Eagerly, DockTapAction.FULL_SEARCH)

    val dockMatchWidgetCorners: StateFlow<Boolean> =
        settingsRepository.dockMatchWidgetCorners.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setDockMatchWidgetCorners(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDockMatchWidgetCorners(enabled) }
    }

    val chargingAnimationColor: StateFlow<Int> =
        settingsRepository.chargingAnimationColor.stateIn(viewModelScope, SharingStarted.Eagerly, 0xFF34D058.toInt())

    val chargingAnimationEnabled: StateFlow<Boolean> =
        settingsRepository.chargingAnimationEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun folderShowAddRow(folderId: Long): Flow<Boolean> = settingsRepository.folderShowAddRow(folderId)

    fun executeDockTapAction(action: DockTapAction) {
        when (action) {
            DockTapAction.NONE -> Unit
            DockTapAction.QUICK_OVERVIEW -> if (overviewEntry.value != null) closeOverview() else openOverview()
            DockTapAction.FULL_SEARCH -> openSearch()
        }
    }

    val iconShape: StateFlow<IconShape> =
        settingsRepository.iconShape.stateIn(viewModelScope, SharingStarted.Eagerly, IconShape.ROUNDED_SQUARE)

    val searchAlphabetRailAppearance: StateFlow<ElementAppearance?> =
        settingsRepository.searchAlphabetRailAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun pageHideStatusBar(pageId: Long): Flow<Boolean> = settingsRepository.pageHideStatusBar(pageId)

    fun setPageHideStatusBar(pageId: Long, hide: Boolean) {
        viewModelScope.launch { settingsRepository.setPageHideStatusBar(pageId, hide) }
    }

    val quickOverviewHideStatusBar: StateFlow<Boolean> =
        settingsRepository.quickOverviewHideStatusBar.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val folderHideStatusBar: StateFlow<Boolean> =
        settingsRepository.folderHideStatusBar.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val folderHideDock: StateFlow<Boolean> =
        settingsRepository.folderHideDock.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val dockSearchButtonAppearance: StateFlow<ElementAppearance?> =
        settingsRepository.dockSearchButtonAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setDockSearchButtonAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { settingsRepository.setDockSearchButtonAppearance(appearance) }
    }

    fun resetDockSearchButtonAppearance() {
        viewModelScope.launch { settingsRepository.resetDockSearchButtonAppearance() }
    }

    // One global Layer 1 shared by every surface — pages, app overview, both search surfaces — with an
    // optional per-surface color-only override falling back to it. See SettingsRepository for why images
    // are global-only now (a color-to-(cached) global-image crossfade never showed the flicker a
    // custom-image-to-custom-image one did).
    val globalBackground: StateFlow<BackgroundConfig> =
        settingsRepository.globalBackground.stateIn(viewModelScope, SharingStarted.Eagerly, BackgroundConfig())

    val searchOverlayBackground: StateFlow<BackgroundConfig> =
        combine(settingsRepository.searchOverlayBackgroundOverride, settingsRepository.globalBackground) { override, global ->
            override ?: global
        }.stateIn(viewModelScope, SharingStarted.Eagerly, BackgroundConfig())

    val quickOverviewBackground: StateFlow<BackgroundConfig> =
        combine(settingsRepository.quickOverviewBackgroundOverride, settingsRepository.globalBackground) { override, global ->
            override ?: global
        }.stateIn(viewModelScope, SharingStarted.Eagerly, BackgroundConfig())

    val appListDisplayMode: StateFlow<AppListDisplayMode> =
        settingsRepository.appListDisplayMode.stateIn(viewModelScope, SharingStarted.Eagerly, AppListDisplayMode.FULLSCREEN)

    val appListSearchButtonAppearance: StateFlow<ElementAppearance?> =
        settingsRepository.appListSearchButtonAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val appListShowSearchButton: StateFlow<Boolean> =
        settingsRepository.appListShowSearchButton.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val appListContentAppearance: StateFlow<ElementAppearance> =
        settingsRepository.appListContentAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    val accentColor: StateFlow<Int> =
        settingsRepository.accentColor.stateIn(viewModelScope, SharingStarted.Eagerly, 0xFF3B82F6.toInt())

    val homeScreenWidgetsEnabled: StateFlow<Boolean> =
        settingsRepository.homeScreenWidgetsEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val favoritesBottomBorderEnabled: StateFlow<Boolean> =
        settingsRepository.favoritesBottomBorderEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val favoritesGroupDividersEnabled: StateFlow<Boolean> =
        settingsRepository.favoritesGroupDividersEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val sidebarPanelAppearance: StateFlow<ElementAppearance> =
        settingsRepository.sidebarPanelAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    val folderCardAppearance: StateFlow<ElementAppearance> =
        settingsRepository.folderCardAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    val keyboardAppearance: StateFlow<ElementAppearance> =
        settingsRepository.keyboardAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    val searchResultCardAppearance: StateFlow<ElementAppearance> =
        settingsRepository.searchResultCardAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    // ---- Gestures ----

    val gestureActions: StateFlow<Map<HomeGesture, GestureAction>> =
        combine(HomeGesture.entries.map { settingsRepository.gestureAction(it) }) { actions ->
            HomeGesture.entries.zip(actions.toList()).toMap()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun executeGestureAction(gesture: HomeGesture, context: Context, onOpenPage: (Long) -> Unit, onOpenSettings: () -> Unit) {
        executeAction(gestureActions.value[gesture] ?: GestureAction.NONE, context, onOpenPage, onOpenSettings)
    }

    fun executeAction(action: GestureAction, context: Context, onOpenPage: (Long) -> Unit, onOpenSettings: () -> Unit) {
        when (action.type) {
            GestureActionType.NONE -> Unit
            GestureActionType.APP -> action.payload?.let { ComponentName.unflattenFromString(it) }?.let { launchApp(context, it) }
            GestureActionType.SHORTCUT -> {
                val parts = action.payload?.split("|")
                if (parts != null && parts.size >= 2) shortcutRepository.launch(parts[0], parts[1])
            }
            GestureActionType.PAGE -> action.payload?.toLongOrNull()?.let(onOpenPage)
            GestureActionType.SEARCH -> openSearch()
            GestureActionType.APP_LIST -> openAppList()
            GestureActionType.SETTINGS -> onOpenSettings()
            GestureActionType.NOTIFICATIONS -> SystemActionsUtil.expandNotifications(context)
            GestureActionType.QUICK_SETTINGS -> SystemActionsUtil.expandQuickSettings(context)
        }
    }

    // ---- Pages, rows and installed apps ----

    val pages: StateFlow<List<LauncherPage>> =
        pageContentRepository.pages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rowsByPage: StateFlow<Map<Long, List<PageRow>>> =
        pageContentRepository.rows
            .map { rows -> rows.groupBy { it.pageId }.mapValues { (_, v) -> v.sortedBy { it.orderIndex } } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val appEntriesByRow: StateFlow<Map<Long, List<AppRowEntry>>> =
        pageContentRepository.appRowEntries
            .map { entries -> entries.groupBy { it.pageRowId }.mapValues { (_, v) -> v.sortedBy { it.orderIndex } } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val appOverrides: StateFlow<Map<String, AppOverride>> =
        appOverrideRepository.overrides
            .map { list -> list.associateBy { it.componentName } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val labeledApps: StateFlow<List<AppInfo>> =
        combine(_installedApps, appOverrides) { apps, overrides ->
            apps.map { app ->
                val custom = overrides[app.componentName.flattenToString()]?.customLabel
                if (custom.isNullOrBlank()) app else app.copy(label = custom)
            }.sortedBy { it.label.lowercase() }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val browsableApps: StateFlow<List<AppInfo>> =
        combine(labeledApps, appOverrides) { apps, overrides ->
            apps.filter { overrides[it.componentName.flattenToString()]?.hidden != true }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- Dock ----

    val quickLaunchSlots: StateFlow<List<QuickLaunchSlot>> =
        combine(dockRepository.slots, labeledApps) { dbSlots, apps ->
            val byIndex = dbSlots.associateBy { it.slotIndex }
            defaultQuickLaunchSlots.map { placeholder ->
                val saved = byIndex[placeholder.index]?.componentName?.let(ComponentName::unflattenFromString)
                val app = saved?.let { cn -> apps.find { it.componentName == cn } }
                if (app != null) placeholder.copy(componentName = app.componentName, label = app.label) else placeholder
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, defaultQuickLaunchSlots)

    fun setDockSlot(index: Int, componentName: ComponentName?) {
        viewModelScope.launch { dockRepository.setSlot(index, componentName?.flattenToString()) }
    }

    fun setDockSingleTapAction(action: DockTapAction) {
        viewModelScope.launch { settingsRepository.setDockSingleTapAction(action) }
    }

    fun setDockDoubleTapAction(action: DockTapAction) {
        viewModelScope.launch { settingsRepository.setDockDoubleTapAction(action) }
    }

    fun resolveLabel(componentName: ComponentName): String {
        val custom = appOverrides.value[componentName.flattenToString()]?.customLabel
        return if (!custom.isNullOrBlank()) custom else installedAppsRepository.rawLabelFor(componentName)
    }

    // ---- Overlay state: quick overview, app overview, text search ----

    private val _overviewEntry = MutableStateFlow<SidebarEntryType?>(null)
    val overviewEntry: StateFlow<SidebarEntryType?> = _overviewEntry.asStateFlow()

    private val _searchOpen = MutableStateFlow(false)
    val searchOpen: StateFlow<Boolean> = _searchOpen.asStateFlow()

    private val _appListOpen = MutableStateFlow(false)
    val appListOpen: StateFlow<Boolean> = _appListOpen.asStateFlow()

    fun openAppList() {
        _overviewEntry.value = null
        _appListOpen.value = true
    }

    fun closeAppList() {
        _appListOpen.value = false
    }

    val appOverviewPageId: StateFlow<Long?> =
        pageContentRepository.appOverviewPageId.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ---- Text search settings and filters ----

    val searchShowAzIndex: StateFlow<Boolean> =
        settingsRepository.searchShowAzIndex.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val searchHideStatusBar: StateFlow<Boolean> =
        settingsRepository.searchHideStatusBar.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val searchKeyboardHaptics: StateFlow<Boolean> =
        settingsRepository.searchKeyboardHaptics.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val searchKeyboardSparks: StateFlow<Boolean> =
        settingsRepository.searchKeyboardSparks.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val searchKeyboardSparkColor: StateFlow<Int> =
        settingsRepository.searchKeyboardSparkColor.stateIn(viewModelScope, SharingStarted.Eagerly, 0xFF34D058.toInt())
    val searchKeyboardLayout: StateFlow<com.bisc.flauncher.data.model.KeyboardLayout> =
        settingsRepository.searchKeyboardLayout.stateIn(viewModelScope, SharingStarted.Eagerly, com.bisc.flauncher.data.model.KeyboardLayout.QWERTY)
    val searchKeyboardShowNumberRow: StateFlow<Boolean> =
        settingsRepository.searchKeyboardShowNumberRow.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    private val searchEngine: StateFlow<SearchEngine> =
        settingsRepository.searchEngine.stateIn(viewModelScope, SharingStarted.Eagerly, SearchEngine.DUCKDUCKGO)
    private val searchEngineCustomUrl: StateFlow<String> =
        settingsRepository.searchEngineCustomUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val searchIncluded: StateFlow<Set<SearchCategory>> =
        settingsRepository.searchIncluded.stateIn(viewModelScope, SharingStarted.Eagerly, SearchCategory.entries.toSet())
    val searchCategoryOrder: StateFlow<List<SearchCategory>> =
        settingsRepository.searchCategoryOrder.stateIn(viewModelScope, SharingStarted.Eagerly, SearchCategory.entries)
    val searchAiApp: StateFlow<String?> =
        settingsRepository.searchAiApp.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val searchKeyboardType: StateFlow<SearchKeyboardType> =
        settingsRepository.searchKeyboardType.stateIn(viewModelScope, SharingStarted.Eagerly, SearchKeyboardType.CUSTOM)

    /** The one category the results are narrowed to, or null to search every category enabled in the settings. */
    private val _searchFilter = MutableStateFlow<SearchCategory?>(null)
    val searchFilter: StateFlow<SearchCategory?> = _searchFilter.asStateFlow()

    val activeSearchCategories: StateFlow<Set<SearchCategory>> =
        combine(_searchFilter, searchIncluded) { filter, included -> filter?.let { setOf(it) } ?: included }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SearchCategory.entries.toSet())

    fun toggleSearchFilter(category: SearchCategory) {
        _searchFilter.value = if (_searchFilter.value == category) null else category
        refreshContactResults()
    }

    val searchIndexShowRailBar: StateFlow<Boolean> =
        settingsRepository.searchIndexShowRailBar.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val searchHeaderLetterAppearance: StateFlow<ElementAppearance> =
        settingsRepository.searchHeaderLetterAppearance.stateIn(
            viewModelScope, SharingStarted.Eagerly, ElementAppearance(fillMode = ElementFillMode.NONE, borderEnabled = true)
        )
    val searchAlphabetLetterAppearance: StateFlow<ElementAppearance> =
        settingsRepository.searchAlphabetLetterAppearance.stateIn(
            viewModelScope, SharingStarted.Eagerly, ElementAppearance(fillMode = ElementFillMode.NONE)
        )

    // ---- Search results ----

    val searchResults: StateFlow<List<AppInfo>> =
        combine(_searchQuery, labeledApps, appOverrides, activeSearchCategories) { query, apps, overrides, active ->
            val include = SearchCategory.APPS in active
            if (query.isBlank() || !include) {
                emptyList()
            } else {
                val visible = apps.filter { overrides[it.componentName.flattenToString()]?.hidden != true }
                val prefixMatches = visible.filter { matchesWordStart(it.label, query) }
                    .sortedByDescending { it.label.startsWith(query.trim(), ignoreCase = true) }
                val revealed = apps.filter { app ->
                    val override = overrides[app.componentName.flattenToString()]
                    override?.hidden == true && override.revealMode == RevealMode.SEARCH_CODE && override.revealCode == query
                }
                (prefixMatches + revealed).distinctBy { it.componentName }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val calculatorResult: StateFlow<String?> =
        _searchQuery.map { query ->
            if (looksLikeCalculatorExpression(query)) {
                evaluateCalculatorExpression(query)?.let(::formatCalculatorResult)
            } else {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _contactResults = MutableStateFlow<List<ContactInfo>>(emptyList())
    val contactResults: StateFlow<List<ContactInfo>> = _contactResults.asStateFlow()

    private val _contactsPermissionGranted = MutableStateFlow(contactsRepository.hasPermission())
    val contactsPermissionGranted: StateFlow<Boolean> = _contactsPermissionGranted.asStateFlow()

    // hasShortcutHostPermission() is a real Binder call to system_server; reading it here once (at
    // startup) instead of synchronously on every quick-overview open keeps that IPC off the opening
    // animation's frames, where an occasional slow call was enough to cause a visible stutter.
    private val _hasShortcutHostPermission = MutableStateFlow(shortcutRepository.hasPermission())
    val hasShortcutHostPermission: StateFlow<Boolean> = _hasShortcutHostPermission.asStateFlow()

    // ---- Content data: folders, notes, places, websites, shortcuts ----

    val folders: StateFlow<List<Folder>> =
        folderRepository.folders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val folderItemsByFolder: StateFlow<Map<Long, List<FolderItem>>> =
        folderRepository.folderItems
            .map { items -> items.groupBy { it.folderId } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _openFolderPath = MutableStateFlow<List<Long>>(emptyList())
    val openFolderPath: StateFlow<List<Long>> = _openFolderPath.asStateFlow()

    val notes: StateFlow<List<Note>> =
        noteRepository.notes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val savedMapTargets: StateFlow<List<SavedMapTarget>> =
        mapsRepository.targets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pinnedWebsites: StateFlow<List<PinnedWebsite>> =
        websiteRepository.websites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pinnedShortcuts: StateFlow<List<PinnedShortcut>> =
        shortcutRepository.pinnedShortcuts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sidebarPinnedApps: StateFlow<List<SidebarPinnedApp>> =
        sidebarAppsRepository.pinnedApps.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sidebarItems: StateFlow<List<ResolvedSidebarItem>> =
        combine(sidebarRepository.items, labeledApps, pinnedShortcuts, savedMapTargets) { items, apps, shortcuts, targets ->
            resolveSidebarItems(items, apps, shortcuts, targets)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _shortcutAppPickerOpen = MutableStateFlow(false)
    val shortcutAppPickerOpen: StateFlow<Boolean> = _shortcutAppPickerOpen.asStateFlow()

    private val _shortcutPickerForApp = MutableStateFlow<AppInfo?>(null)
    val shortcutPickerForApp: StateFlow<AppInfo?> = _shortcutPickerForApp.asStateFlow()

    private val _availableShortcuts = MutableStateFlow<List<ShortcutInfo>>(emptyList())
    val availableShortcuts: StateFlow<List<ShortcutInfo>> = _availableShortcuts.asStateFlow()

    // ---- Widget data: weather, music, system widgets ----

    val weather: StateFlow<WeatherSnapshot?> =
        weatherRepository.snapshot.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val nowPlaying: StateFlow<NowPlayingInfo?> = nowPlayingRepository.nowPlaying

    private val _notificationAccessGranted = MutableStateFlow(nowPlayingRepository.hasPermission())
    val notificationAccessGranted: StateFlow<Boolean> = _notificationAccessGranted.asStateFlow()

    private val _systemWidgetPickerOpenForPage = MutableStateFlow<Long?>(null)
    val systemWidgetPickerOpenForPage: StateFlow<Long?> = _systemWidgetPickerOpenForPage.asStateFlow()

    private val _pendingBindIntent = MutableStateFlow<Intent?>(null)
    val pendingBindIntent: StateFlow<Intent?> = _pendingBindIntent.asStateFlow()

    private val _pendingConfigureIntent = MutableStateFlow<Intent?>(null)
    val pendingConfigureIntent: StateFlow<Intent?> = _pendingConfigureIntent.asStateFlow()

    private var inFlightSystemWidget: InFlightSystemWidget? = null

    init {
        viewModelScope.launch { pageContentRepository.ensureSeeded() }
        viewModelScope.launch { sidebarRepository.ensureSeeded() }
        viewModelScope.launch {
            _installedApps.value = installedAppsRepository.getLaunchableApps()
            installedAppsRepository.packageChanges().collectLatest {
                _installedApps.value = installedAppsRepository.getLaunchableApps()
            }
        }
        viewModelScope.launch {
            snapshotFlow { appIconLoader.revision }
                .combine(_installedApps) { _, apps -> apps }
                .collectLatest { apps -> if (apps.isNotEmpty()) appIconLoader.warm(apps.map { it.componentName }) }
        }
    }

    fun refreshWidgetData() {
        nowPlayingRepository.refresh()
        _notificationAccessGranted.value = nowPlayingRepository.hasPermission()
    }

    fun toggleNowPlaying() = nowPlayingRepository.togglePlayPause()
    fun skipNext() = nowPlayingRepository.skipNext()
    fun skipPrevious() = nowPlayingRepository.skipPrevious()
    fun seekNowPlaying(positionMs: Long) = nowPlayingRepository.seekTo(positionMs)
    fun sendNowPlayingCustomAction(id: String) = nowPlayingRepository.sendCustomAction(id)
    fun selectNowPlayingPlayer(packageName: String) = nowPlayingRepository.selectPlayer(packageName)
    fun openNowPlayingPlayer() = nowPlayingRepository.openPlayer()

    // ---- Notes, places, websites, sidebar apps, shortcuts ----

    fun saveNote(note: Note, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch { onSaved(noteRepository.save(note)) }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { noteRepository.delete(note) }
    }

    fun createNote(onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(noteRepository.save(Note())) }
    }

    fun reorderNotes(ordered: List<Note>) {
        viewModelScope.launch { noteRepository.reorder(ordered) }
    }

    fun addMapTarget(label: String, lat: Double, lon: Double, address: String = "") {
        viewModelScope.launch { mapsRepository.add(label, lat, lon, address) }
    }

    fun removeMapTarget(target: SavedMapTarget) {
        viewModelScope.launch { mapsRepository.remove(target) }
    }

    fun addWebsite(label: String, url: String) {
        viewModelScope.launch { websiteRepository.add(label, url) }
    }

    fun removeWebsite(website: PinnedWebsite) {
        viewModelScope.launch { websiteRepository.remove(website) }
    }

    fun addSidebarApp(app: AppInfo) {
        viewModelScope.launch { sidebarAppsRepository.add(app.componentName.flattenToString()) }
    }

    fun addAllSidebarApps(apps: List<AppInfo>) {
        viewModelScope.launch { sidebarAppsRepository.addAll(apps.map { it.componentName.flattenToString() }) }
    }

    fun removeSidebarApp(app: SidebarPinnedApp) {
        viewModelScope.launch { sidebarAppsRepository.remove(app) }
    }

    fun sidebarCategoryViewMode(entry: SidebarEntryType): Flow<SidebarCategoryViewMode> =
        settingsRepository.sidebarCategoryViewMode(entry.name)

    fun sidebarCategoryShowAddRow(entry: SidebarEntryType): Flow<Boolean> =
        settingsRepository.sidebarCategoryShowAddRow(entry.name)

    fun loadShortcutIcon(info: ShortcutInfo) = shortcutRepository.loadIcon(info)

    fun openShortcutAppPicker() {
        _shortcutAppPickerOpen.value = true
    }

    fun dismissShortcutAppPicker() {
        _shortcutAppPickerOpen.value = false
    }

    fun selectShortcutSourceApp(app: AppInfo) {
        _shortcutAppPickerOpen.value = false
        _shortcutPickerForApp.value = app
        viewModelScope.launch { _availableShortcuts.value = shortcutRepository.shortcutsFor(app.componentName.packageName) }
    }

    fun dismissShortcutPicker() {
        _shortcutPickerForApp.value = null
        _availableShortcuts.value = emptyList()
    }

    fun pinShortcut(info: ShortcutInfo) {
        viewModelScope.launch {
            shortcutRepository.pin(info)
            dismissShortcutPicker()
        }
    }

    fun unpinShortcut(shortcut: PinnedShortcut) {
        viewModelScope.launch { shortcutRepository.unpin(shortcut) }
    }

    fun launchShortcut(shortcut: PinnedShortcut) {
        shortcutRepository.launch(shortcut)
    }

    // ---- Contacts ----

    fun onContactsPermissionResult(granted: Boolean) {
        _contactsPermissionGranted.value = granted
        if (granted) refreshContactResults()
    }

    fun refreshContactsPermission() {
        onContactsPermissionResult(contactsRepository.hasPermission())
    }

    private fun refreshContactResults() {
        val query = _searchQuery.value
        val active = _searchFilter.value?.let { setOf(it) } ?: searchIncluded.value
        if (query.length < 2 || SearchCategory.CONTACTS !in active) {
            _contactResults.value = emptyList()
            return
        }
        viewModelScope.launch { _contactResults.value = contactsRepository.search(query) }
    }

    // --- Sidebar Contacts category --- A curated, pinned list ([PinnedContact]).

    val pinnedContacts: StateFlow<List<PinnedContact>> =
        contactsRepository.pinnedContacts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _contactPickerOpen = MutableStateFlow(false)
    val contactPickerOpen: StateFlow<Boolean> = _contactPickerOpen.asStateFlow()

    private val _contactPickerList = MutableStateFlow<List<ContactInfo>?>(null)
    val contactPickerList: StateFlow<List<ContactInfo>?> = _contactPickerList.asStateFlow()

    fun openContactPicker() {
        _contactPickerOpen.value = true
        _contactPickerList.value = null
        viewModelScope.launch { _contactPickerList.value = contactsRepository.getAll() }
    }

    fun dismissContactPicker() {
        _contactPickerOpen.value = false
        _contactPickerList.value = null
    }

    fun pinContact(contact: ContactInfo) {
        viewModelScope.launch {
            contactsRepository.pin(contact)
            dismissContactPicker()
        }
    }

    fun unpinContact(contact: PinnedContact) {
        viewModelScope.launch { contactsRepository.unpin(contact) }
    }

    suspend fun loadContactPhoto(contactId: Long, photoUri: String?): ImageBitmap? =
        contactPhotoLoader.load(contactId, photoUri)

    val noteResults: StateFlow<List<Note>> =
        combine(_searchQuery, notes, activeSearchCategories) { query, list, active ->
            val include = SearchCategory.NOTES in active
            if (query.isBlank() || !include) emptyList() else list.filter { matchesWordStart(it.title, query) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val folderResults: StateFlow<List<Folder>> =
        combine(_searchQuery, folders, activeSearchCategories) { query, list, active ->
            val include = SearchCategory.FOLDERS in active
            if (query.isBlank() || !include) emptyList() else list.filter { matchesWordStart(it.name, query) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val mapTargetResults: StateFlow<List<SavedMapTarget>> =
        combine(_searchQuery, savedMapTargets, activeSearchCategories) { query, list, active ->
            val include = SearchCategory.PLACES in active
            if (query.isBlank() || !include) emptyList()
            else list.filter { it.label.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val websiteResults: StateFlow<List<PinnedWebsite>> =
        combine(_searchQuery, pinnedWebsites, activeSearchCategories) { query, list, active ->
            val include = SearchCategory.WEBSITES in active
            if (query.isBlank() || !include) emptyList() else list.filter { matchesWordStart(it.label, query) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val shortcutResults: StateFlow<List<PinnedShortcut>> =
        combine(_searchQuery, pinnedShortcuts, activeSearchCategories) { query, list, active ->
            val include = SearchCategory.SHORTCUTS in active
            if (query.isBlank() || !include) emptyList() else list.filter { matchesWordStart(it.label, query) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- Overlay controls and search commands ----

    fun openOverview(entry: SidebarEntryType = _overviewEntry.value ?: SidebarEntryType.APPS) {
        _overviewEntry.value = entry
        // Off the main thread and not awaited: keeps the cached value from going stale (e.g. after a
        // default-launcher change) without making the open animation wait on it.
        viewModelScope.launch(Dispatchers.Default) { _hasShortcutHostPermission.value = shortcutRepository.hasPermission() }
    }

    fun selectSidebarEntry(entry: SidebarEntryType) {
        _overviewEntry.value = entry
    }

    fun closeOverview() {
        _overviewEntry.value = null
    }

    fun openSearch() {
        _overviewEntry.value = null
        // The search backdrop is translucent, so the app overview must not stay open behind it.
        _appListOpen.value = false
        _searchOpen.value = true
        _searchFilter.value = null
    }

    fun closeSearch() {
        _searchOpen.value = false
        _searchQuery.value = ""
        _contactResults.value = emptyList()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        refreshContactResults()
    }

    fun runWebSearchCommand(context: Context) {
        val q = _searchQuery.value.trim()
        if (q.isEmpty()) return
        if (launchWebSearch(context, q, searchEngine.value, searchEngineCustomUrl.value)) closeSearch()
    }

    fun runMapsCommand(context: Context) {
        val q = _searchQuery.value.trim()
        if (q.isEmpty()) return
        if (launchMapsQuery(context, q)) closeSearch()
    }

    fun runAiCommand(context: Context) {
        val q = _searchQuery.value.trim()
        val app = searchAiApp.value ?: return
        if (q.isEmpty()) return
        launchAiApp(context, app, q)
        closeSearch()
    }

    fun runEmailCommand(context: Context) {
        val q = _searchQuery.value.trim()
        if (!looksLikeEmail(q)) return
        launchEmailTo(context, q)
        closeSearch()
    }

    // ---- Page and row editing ----

    fun addPage() {
        viewModelScope.launch { pageContentRepository.addPage() }
    }

    fun removePage(context: Context, page: LauncherPage) {
        viewModelScope.launch {
            if (pageContentRepository.removePageIfEmpty(page)) {
                settingsRepository.clearPageBackgroundConfig(page.id)
                BackgroundImageStore.delete(context, "page_${page.id}")
            }
        }
    }

    fun pageBackgroundConfig(pageId: Long): Flow<BackgroundConfig> =
        combine(settingsRepository.pageBackgroundOverride(pageId), settingsRepository.globalBackground) { override, global ->
            override ?: global
        }

    fun pageBackgroundOverride(pageId: Long): Flow<BackgroundConfig?> = settingsRepository.pageBackgroundOverride(pageId)

    fun setPageBackgroundConfig(pageId: Long, config: BackgroundConfig) {
        viewModelScope.launch { settingsRepository.setPageBackgroundConfig(pageId, config) }
    }

    fun clearPageBackgroundConfig(pageId: Long) {
        viewModelScope.launch { settingsRepository.clearPageBackgroundConfig(pageId) }
    }

    fun reorderPages(ordered: List<LauncherPage>) {
        viewModelScope.launch { pageContentRepository.reorderPages(ordered) }
    }

    fun renamePage(pageId: Long, name: String?) {
        viewModelScope.launch { pageContentRepository.renamePage(pageId, name) }
    }

    fun addWidgetRow(pageId: Long, kind: String, spanY: Int) {
        viewModelScope.launch { pageContentRepository.addWidgetRow(pageId, kind, spanY) }
    }

    fun addAppsRow(pageId: Long) {
        viewModelScope.launch { pageContentRepository.addAppsRow(pageId) }
    }

    fun addSearchBarRow(pageId: Long) {
        viewModelScope.launch {
            val relocated = pageContentRepository.addSearchBarRow(pageId)
            if (relocated > 0) _widgetRelocationWarningCount.value = relocated
        }
    }

    /** Non-zero while the "widget(s) moved to a new page" warning from [addSearchBarRow] should be shown. */
    val widgetRelocationWarningCount: StateFlow<Int> get() = _widgetRelocationWarningCount
    private val _widgetRelocationWarningCount = MutableStateFlow(0)

    fun dismissWidgetRelocationWarning() {
        _widgetRelocationWarningCount.value = 0
    }

    fun setRowSpanY(row: PageRow, spanY: Int) {
        viewModelScope.launch { pageContentRepository.setRowSpanY(row, spanY) }
    }

    fun setRowHeightPx(row: PageRow, heightPx: Int?) {
        viewModelScope.launch { pageContentRepository.setRowHeightPx(row, heightPx) }
    }

    fun setRowAppearance(row: PageRow, appearance: ElementAppearance) {
        viewModelScope.launch { pageContentRepository.setRowAppearance(row, appearance) }
    }

    fun setRowEdgeToEdge(row: PageRow, edgeToEdge: Boolean) {
        viewModelScope.launch { pageContentRepository.setRowEdgeToEdge(row, edgeToEdge) }
    }

    fun setRowCornerStyle(row: PageRow, cornerStyle: WidgetCornerStyle) {
        viewModelScope.launch { pageContentRepository.setRowCornerStyle(row, cornerStyle) }
    }

    fun setRowColumns(row: PageRow, columns: Int) {
        viewModelScope.launch { pageContentRepository.setRowColumns(row, columns) }
    }

    fun setRowClockStyle(row: PageRow, style: String) {
        viewModelScope.launch { pageContentRepository.setRowClockStyle(row, style) }
    }

    fun setRowClockDateAction(row: PageRow, action: GestureAction) {
        viewModelScope.launch { pageContentRepository.setRowClockDateAction(row, action) }
    }

    fun setRowClockTimeAction(row: PageRow, action: GestureAction) {
        viewModelScope.launch { pageContentRepository.setRowClockTimeAction(row, action) }
    }

    fun setRowClockTimeFormat(row: PageRow, format: String) {
        viewModelScope.launch { pageContentRepository.setRowClockTimeFormat(row, format) }
    }

    fun setRowClockDateFormat(row: PageRow, format: String) {
        viewModelScope.launch { pageContentRepository.setRowClockDateFormat(row, format) }
    }

    fun setRowClockShowNextAlarm(row: PageRow, show: Boolean) {
        viewModelScope.launch { pageContentRepository.setRowClockShowNextAlarm(row, show) }
    }

    fun setRowClockAlignment(row: PageRow, alignment: String) {
        viewModelScope.launch { pageContentRepository.setRowClockAlignment(row, alignment) }
    }

    fun removeRow(row: PageRow) {
        viewModelScope.launch {
            pageContentRepository.removeRow(row)
            if (row.type == PageRowType.SYSTEM_WIDGET) {
                row.payload.toIntOrNull()?.let { widgetHostManager.deleteId(it) }
            }
        }
    }

    fun reorderRows(orderedRows: List<PageRow>) {
        viewModelScope.launch { pageContentRepository.reorderRows(orderedRows) }
    }

    fun addAppEntryToRow(pageRowId: Long, app: AppInfo) {
        viewModelScope.launch { pageContentRepository.addAppEntry(pageRowId, app.componentName.flattenToString()) }
    }

    fun addExistingFolderToRow(pageRowId: Long, folderId: Long) {
        viewModelScope.launch { pageContentRepository.addFolderEntry(pageRowId, folderId) }
    }

    fun createFolderInRow(pageRowId: Long, name: String) {
        viewModelScope.launch {
            val folderId = folderRepository.createFolder(name)
            pageContentRepository.addFolderEntry(pageRowId, folderId)
        }
    }

    fun createStandaloneFolder(name: String, apps: List<AppInfo>) {
        viewModelScope.launch {
            val folderId = folderRepository.createFolder(name)
            apps.forEach { folderRepository.addItemToFolder(folderId, GridItemType.APP, it.componentName.flattenToString()) }
        }
    }

    fun addShortcutEntryToRow(pageRowId: Long, info: ShortcutInfo) {
        viewModelScope.launch {
            val label = (info.shortLabel ?: info.longLabel ?: info.id).toString()
            pageContentRepository.addShortcutEntry(pageRowId, info.`package`, info.id, label)
            dismissShortcutPicker()
        }
    }

    fun addMapTargetEntryToRow(pageRowId: Long, targetId: Long) {
        viewModelScope.launch { pageContentRepository.addMapTargetEntry(pageRowId, targetId) }
    }

    fun addWebsiteEntryToRow(pageRowId: Long, websiteId: Long) {
        viewModelScope.launch { pageContentRepository.addWebsiteEntry(pageRowId, websiteId) }
    }

    fun addNewMapTargetToRow(pageRowId: Long, label: String, lat: Double, lon: Double, address: String = "") {
        viewModelScope.launch {
            val targetId = mapsRepository.add(label, lat, lon, address)
            pageContentRepository.addMapTargetEntry(pageRowId, targetId)
        }
    }

    fun addNewWebsiteToRow(pageRowId: Long, label: String, url: String) {
        viewModelScope.launch {
            val websiteId = websiteRepository.add(label, url)
            pageContentRepository.addWebsiteEntry(pageRowId, websiteId)
        }
    }

    fun removeAppRowEntry(entry: AppRowEntry) {
        viewModelScope.launch {
            pageContentRepository.removeEntry(entry)
            if (entry.type == GridItemType.FOLDER) {
                entry.payload.toLongOrNull()?.let { folderRepository.deleteFolderById(it) }
            }
        }
    }

    fun reorderAppRowEntries(orderedEntries: List<AppRowEntry>) {
        viewModelScope.launch { pageContentRepository.reorderEntries(orderedEntries) }
    }

    // ---- System widgets ----

    fun installedSystemWidgetProviders(): List<AppWidgetProviderInfo> = widgetHostManager.installedProviders()

    fun getSystemWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? = widgetHostManager.getInfo(appWidgetId)

    fun createSystemWidgetView(appWidgetId: Int, info: AppWidgetProviderInfo, dark: Boolean): AppWidgetHostView =
        widgetHostManager.createHostView(appWidgetId, info, dark)

    fun updateSystemWidgetSize(appWidgetId: Int, hostView: AppWidgetHostView, widthDp: Int, heightDp: Int) =
        widgetHostManager.updateHostViewSize(appWidgetId, hostView, widthDp, heightDp)

    fun openSystemWidgetPicker(pageId: Long) {
        _systemWidgetPickerOpenForPage.value = pageId
    }

    fun dismissSystemWidgetPicker() {
        _systemWidgetPickerOpenForPage.value = null
    }

    fun startAddSystemWidget(pageId: Long, provider: AppWidgetProviderInfo) {
        _systemWidgetPickerOpenForPage.value = null
        val appWidgetId = widgetHostManager.allocateId()
        if (widgetHostManager.tryBindWithoutConfirmation(appWidgetId, provider)) {
            proceedAfterBind(pageId, appWidgetId, provider)
        } else {
            inFlightSystemWidget = InFlightSystemWidget(pageId, appWidgetId, provider)
            _pendingBindIntent.value = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            }
        }
    }

    fun onSystemWidgetBindResult(granted: Boolean) {
        _pendingBindIntent.value = null
        val pending = inFlightSystemWidget ?: return
        if (!granted) {
            widgetHostManager.deleteId(pending.appWidgetId)
            inFlightSystemWidget = null
            return
        }
        proceedAfterBind(pending.pageId, pending.appWidgetId, pending.provider)
    }

    private fun proceedAfterBind(pageId: Long, appWidgetId: Int, provider: AppWidgetProviderInfo) {
        val configureComponent = provider.configure
        if (configureComponent != null) {
            inFlightSystemWidget = InFlightSystemWidget(pageId, appWidgetId, provider)
            _pendingConfigureIntent.value = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = configureComponent
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
        } else {
            inFlightSystemWidget = null
            placeSystemWidget(pageId, appWidgetId)
        }
    }

    fun onSystemWidgetConfigureResult(success: Boolean) {
        _pendingConfigureIntent.value = null
        val pending = inFlightSystemWidget ?: return
        inFlightSystemWidget = null
        if (!success) {
            widgetHostManager.deleteId(pending.appWidgetId)
            return
        }
        placeSystemWidget(pending.pageId, pending.appWidgetId)
    }

    private fun placeSystemWidget(pageId: Long, appWidgetId: Int) {
        viewModelScope.launch { pageContentRepository.addSystemWidgetRow(pageId, appWidgetId, spanY = 2) }
    }

    private data class InFlightSystemWidget(
        val pageId: Long,
        val appWidgetId: Int,
        val provider: AppWidgetProviderInfo
    )

    // ---- Folders ----

    fun openFolder(folderId: Long) {
        _openFolderPath.value = _openFolderPath.value + folderId
    }

    fun closeFolder() {
        _openFolderPath.value = _openFolderPath.value.dropLast(1)
    }

    fun closeAllFolders() {
        _openFolderPath.value = emptyList()
    }

    fun renameFolder(folder: Folder, newName: String) {
        viewModelScope.launch { folderRepository.renameFolder(folder, newName) }
    }

    fun setFolderIcon(folder: Folder, mode: FolderIconMode, color: String? = null, packPackage: String? = null, packDrawable: String? = null) {
        viewModelScope.launch { folderRepository.setFolderIcon(folder, mode, color, packPackage, packDrawable) }
    }

    fun loadPackIcon(iconPackPackage: String, drawableName: String): ImageBitmap? =
        appIconLoader.loadPackIcon(iconPackPackage, drawableName)

    fun addWidgetToFolder(folderId: Long, kind: String, spanY: Int) {
        viewModelScope.launch { folderRepository.addWidgetToFolder(folderId, kind, spanY) }
    }

    fun addAppToFolder(folderId: Long, app: AppInfo) {
        viewModelScope.launch { folderRepository.addItemToFolder(folderId, GridItemType.APP, app.componentName.flattenToString()) }
    }

    fun addShortcutToFolder(folderId: Long, info: ShortcutInfo) {
        viewModelScope.launch {
            val label = (info.shortLabel ?: info.longLabel ?: info.id).toString()
            folderRepository.addShortcutToFolder(folderId, info.`package`, info.id, label)
            dismissShortcutPicker()
        }
    }

    fun addMapTargetToFolder(folderId: Long, targetId: Long) {
        viewModelScope.launch { folderRepository.addMapTargetToFolder(folderId, targetId) }
    }

    fun addWebsiteToFolder(folderId: Long, websiteId: Long) {
        viewModelScope.launch { folderRepository.addWebsiteToFolder(folderId, websiteId) }
    }

    fun addNewMapTargetToFolder(folderId: Long, label: String, lat: Double, lon: Double, address: String = "") {
        viewModelScope.launch {
            val targetId = mapsRepository.add(label, lat, lon, address)
            folderRepository.addMapTargetToFolder(folderId, targetId)
        }
    }

    fun addNewWebsiteToFolder(folderId: Long, label: String, url: String) {
        viewModelScope.launch {
            val websiteId = websiteRepository.add(label, url)
            folderRepository.addWebsiteToFolder(folderId, websiteId)
        }
    }

    fun launchFolderShortcut(payload: String) {
        val parts = payload.split("|")
        if (parts.size >= 2) shortcutRepository.launch(parts[0], parts[1])
    }

    fun addNestedFolder(parentFolderId: Long, name: String) {
        viewModelScope.launch {
            val childId = folderRepository.addNestedFolder(parentFolderId, name)
            openFolder(childId)
        }
    }

    fun reorderFolderItems(orderedItems: List<FolderItem>) {
        viewModelScope.launch { folderRepository.reorderItems(orderedItems) }
    }

    fun removeFromFolder(item: FolderItem) {
        viewModelScope.launch {
            val folderDeleted = folderRepository.removeFolderItem(item)
            if (folderDeleted) {
                closeFolder()
                val appRowEntry = appEntriesByRow.value.values.flatten()
                    .find { it.type == GridItemType.FOLDER && it.payload == item.folderId.toString() }
                if (appRowEntry != null) {
                    pageContentRepository.removeEntry(appRowEntry)
                } else {
                    val parentEntry = folderItemsByFolder.value.values.flatten()
                        .find { it.type == GridItemType.FOLDER && it.payload == item.folderId.toString() }
                    parentEntry?.let { folderRepository.removeFolderItem(it) }
                }
            }
        }
    }
}
