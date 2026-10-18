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
import com.bisc.flauncher.data.model.RowEntry
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.ContactInfo
import com.bisc.flauncher.data.model.DockTapAction
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderIconMode
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.GridConstants
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.data.model.LauncherPageKind
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
import com.bisc.flauncher.util.launchAiApp
import com.bisc.flauncher.data.model.WeatherSnapshot
import com.bisc.flauncher.data.model.AppListDisplayMode
import com.bisc.flauncher.data.model.AppListLayout
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.repository.AppOverrideRepository
import com.bisc.flauncher.data.repository.ContactsRepository
import com.bisc.flauncher.data.repository.FolderRepository
import com.bisc.flauncher.data.repository.InstalledAppsRepository
import com.bisc.flauncher.data.repository.MapsRepository
import com.bisc.flauncher.data.repository.NoteRepository
import com.bisc.flauncher.data.repository.NowPlayingRepository
import com.bisc.flauncher.data.repository.PageContentRepository
import com.bisc.flauncher.data.repository.ShortcutRepository
import com.bisc.flauncher.data.repository.WeatherRepository
import com.bisc.flauncher.data.repository.WebsiteRepository
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.home.dock.PageIconGlyph
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
import kotlinx.coroutines.flow.first
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
    private val actionButtonRepository: com.bisc.flauncher.data.repository.ActionButtonRepository,
    private val shortcutRepository: ShortcutRepository,
    private val appOverrideRepository: AppOverrideRepository,
    private val weatherRepository: WeatherRepository,
    private val nowPlayingRepository: NowPlayingRepository,
    private val widgetHostManager: SystemWidgetHostManager,
    private val appIconLoader: AppIconLoader,
    private val contactPhotoLoader: ContactPhotoLoader
) : ViewModel() {
    // ---- Display settings ----

    fun loadIcon(componentName: ComponentName): ImageBitmap? = appIconLoader.load(componentName)

    val iconLoader: AppIconLoader get() = appIconLoader

    val appListLayout: StateFlow<AppListLayout> =
        settingsRepository.appListLayout.stateIn(viewModelScope, SharingStarted.Eagerly, AppListLayout.LIST)

    val appOverviewHandedness: StateFlow<Handedness> =
        settingsRepository.appOverviewHandedness.stateIn(viewModelScope, SharingStarted.Eagerly, Handedness.RIGHT)

    val chargingAnimationColor: StateFlow<Int> =
        settingsRepository.chargingAnimationColor.stateIn(viewModelScope, SharingStarted.Eagerly, 0xFF34D058.toInt())

    val chargingAnimationEnabled: StateFlow<Boolean> =
        settingsRepository.chargingAnimationEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun folderShowAddRow(folderId: Long): Flow<Boolean> = settingsRepository.folderShowAddRow(folderId)

    fun executeDockTapAction(action: DockTapAction) {
        when (action) {
            DockTapAction.NONE -> Unit
            DockTapAction.QUICK_OVERVIEW -> if (isPanelOpen.value) closeOverview() else openOverview()
            DockTapAction.FULL_SEARCH -> openSearch()
        }
    }

    val iconShape: StateFlow<IconShape> =
        settingsRepository.iconShape.stateIn(viewModelScope, SharingStarted.Eagerly, IconShape.ROUNDED_SQUARE)

    val iconSizeScale: StateFlow<Float> =
        settingsRepository.iconSizePercent.map { 1f + it / 100f }.stateIn(viewModelScope, SharingStarted.Eagerly, 1f)

    val searchAlphabetRailAppearance: StateFlow<ElementAppearance?> =
        settingsRepository.searchAlphabetRailAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun pageHideStatusBar(pageId: Long): Flow<Boolean> = settingsRepository.pageHideStatusBar(pageId)

    fun setPageHideStatusBar(pageId: Long, hide: Boolean) {
        viewModelScope.launch { settingsRepository.setPageHideStatusBar(pageId, hide) }
    }

    val folderHideStatusBar: StateFlow<Boolean> =
        settingsRepository.folderHideStatusBar.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val folderHideDock: StateFlow<Boolean> =
        settingsRepository.folderHideDock.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // One global Layer 1 shared by every surface — pages, app overview, both search surfaces — with an
    // optional per-surface color-only override falling back to it. See SettingsRepository for why images
    // are global-only now (a color-to-(cached) global-image crossfade never showed the flicker a
    // custom-image-to-custom-image one did).
    val globalBackground: StateFlow<BackgroundConfig> =
        settingsRepository.globalBackground.stateIn(viewModelScope, SharingStarted.Eagerly, BackgroundConfig())

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

    val sidebarPanelAppearance: StateFlow<ElementAppearance> =
        settingsRepository.sidebarPanelAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    fun setSidebarPanelAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { settingsRepository.setSidebarPanelAppearance(appearance) }
    }

    val folderCardAppearance: StateFlow<ElementAppearance> =
        settingsRepository.folderCardAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    val keyboardAppearance: StateFlow<ElementAppearance> =
        settingsRepository.keyboardAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    val searchAppearance: StateFlow<ElementAppearance> =
        settingsRepository.searchAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance())

    val searchKeyboardAutoOpen: StateFlow<Boolean> =
        settingsRepository.searchKeyboardAutoOpen.stateIn(viewModelScope, SharingStarted.Eagerly, true)

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
            // The sole way to reference a page now — ROWS pages jump the pager via onOpenPage,
            // the singleton overlays (App overview/Search/Sidebar) open their own overlay directly,
            // same dispatch PageRowContent's onOpenPageLink already does for a dock/element-box page-link tap.
            GestureActionType.PAGE -> action.payload?.toLongOrNull()?.let { id ->
                when (allPages.value.find { it.id == id }?.kind) {
                    LauncherPageKind.APP_LIST -> openAppList()
                    LauncherPageKind.SEARCH -> openSearch()
                    LauncherPageKind.SIDEBAR -> openOverview()
                    else -> onOpenPage(id)
                }
            }
            GestureActionType.SETTINGS -> onOpenSettings()
            GestureActionType.NOTIFICATIONS -> SystemActionsUtil.expandNotifications(context)
            GestureActionType.QUICK_SETTINGS -> SystemActionsUtil.expandQuickSettings(context)
        }
    }

    fun setGestureAction(gesture: HomeGesture, action: GestureAction) {
        viewModelScope.launch { settingsRepository.setGestureAction(gesture, action) }
    }

    // ---- Pages, rows and installed apps ----

    val pages: StateFlow<List<LauncherPage>> =
        pageContentRepository.pages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Every page, including the singleton App overview/Search pages (unlike [pages], which only
     *  has the swipeable ROWS ones) — used by the Pages settings list so those two are no longer
     *  hidden/implicit. */
    val allPages: StateFlow<List<LauncherPage>> =
        pageContentRepository.allPages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rowsByPage: StateFlow<Map<Long, List<PageRow>>> =
        pageContentRepository.rows
            .map { rows -> rows.groupBy { it.pageId }.mapValues { (_, v) -> v.sortedBy { it.orderIndex } } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val rowEntriesByRow: StateFlow<Map<Long, List<RowEntry>>> =
        pageContentRepository.rowEntries
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

    fun resolveLabel(componentName: ComponentName): String {
        val custom = appOverrides.value[componentName.flattenToString()]?.customLabel
        return if (!custom.isNullOrBlank()) custom else installedAppsRepository.rawLabelFor(componentName)
    }

    // ---- Overlay state: quick overview, app overview, text search ----

    private val _isPanelOpen = MutableStateFlow(false)
    val isPanelOpen: StateFlow<Boolean> = _isPanelOpen.asStateFlow()

    private val _searchOpen = MutableStateFlow(false)
    val searchOpen: StateFlow<Boolean> = _searchOpen.asStateFlow()

    private val _appListOpen = MutableStateFlow(false)
    val appListOpen: StateFlow<Boolean> = _appListOpen.asStateFlow()

    fun openAppList() {
        _isPanelOpen.value = false
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
    private val searchEngine: StateFlow<SearchEngine> =
        settingsRepository.searchEngine.stateIn(viewModelScope, SharingStarted.Eagerly, SearchEngine.DUCKDUCKGO)
    private val searchEngineCustomUrl: StateFlow<String> =
        settingsRepository.searchEngineCustomUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val searchIncluded: StateFlow<Set<SearchCategory>> =
        settingsRepository.searchIncluded.stateIn(viewModelScope, SharingStarted.Eagerly, SearchCategory.entries.toSet())
    val searchCategoryOrder: StateFlow<List<SearchCategory>> =
        settingsRepository.searchCategoryOrder.stateIn(viewModelScope, SharingStarted.Eagerly, SearchCategory.entries)

    /** Per-category icon color overrides (null entries fall back to [com.bisc.flauncher.ui.home.search.defaultColor]). */
    val categoryColors: StateFlow<Map<SearchCategory, Int?>> =
        combine(SearchCategory.entries.map { c -> settingsRepository.categoryColor(c).map { color -> c to color } }) { pairs -> pairs.toMap() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun setCategoryColor(category: SearchCategory, color: Int) {
        viewModelScope.launch { settingsRepository.setCategoryColor(category, color) }
    }
    val searchAiApp: StateFlow<String?> =
        settingsRepository.searchAiApp.stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

    val converterResult: StateFlow<String?> =
        _searchQuery.map { query -> com.bisc.flauncher.util.tryConvert(query) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

    val actionButtons: StateFlow<List<com.bisc.flauncher.data.model.ActionButton>> =
        actionButtonRepository.actionButtons.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pinnedShortcuts: StateFlow<List<PinnedShortcut>> =
        shortcutRepository.pinnedShortcuts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    // ---- Notes, places, websites, shortcuts ----

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

    fun openOverview() {
        _isPanelOpen.value = true
        // Off the main thread and not awaited: keeps the cached value from going stale (e.g. after a
        // default-launcher change) without making the open animation wait on it.
        viewModelScope.launch(Dispatchers.Default) { _hasShortcutHostPermission.value = shortcutRepository.hasPermission() }
    }

    fun closeOverview() {
        _isPanelOpen.value = false
    }

    fun openSearch() {
        _isPanelOpen.value = false
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

    /** Suspends until the new page's id is known, so the caller (the carousel's own "+" tiles) can
     *  navigate straight to it rather than firing-and-forgetting like [addPage] does. */
    suspend fun addPageInDirection(direction: com.bisc.flauncher.data.model.PageDirection): Long =
        pageContentRepository.addPageInDirection(direction)

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

    fun setPageIcon(pageId: Long, glyph: PageIconGlyph?, backgroundColor: Int?, iconColor: Int?) {
        viewModelScope.launch { pageContentRepository.setPageIcon(pageId, glyph?.name, backgroundColor, iconColor) }
    }

    fun addWidgetRow(pageId: Long, kind: String, spanY: Int) {
        val candidate = PageRow(pageId = pageId, orderIndex = 0, type = PageRowType.WIDGET, payload = kind, spanY = spanY)
        viewModelScope.launch {
            if (pageContentRepository.wouldOverflow(pageId, candidate)) {
                _pendingOverflowAdd.value = PendingOverflowAdd.WidgetRow(pageId, kind, spanY)
            } else {
                pageContentRepository.addWidgetRow(pageId, kind, spanY)
            }
        }
    }

    fun addElementBox(pageId: Long) {
        val candidate = PageRow(pageId = pageId, orderIndex = 0, type = PageRowType.ELEMENT_BOX, columns = GridConstants.DEFAULT_ELEMENT_BOX_COLUMNS)
        viewModelScope.launch {
            if (pageContentRepository.wouldOverflow(pageId, candidate)) {
                _pendingOverflowAdd.value = PendingOverflowAdd.ElementBox(pageId)
            } else {
                pageContentRepository.addElementBox(pageId)
            }
        }
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

    /** A would-be apps/widget row that doesn't fit this page as-is — see masterplan Phase 7: offer a
     *  new page for it instead of silently relocating something there, [wouldOverflow]-gated in
     *  [addElementBox]/[addWidgetRow]. Resolved by [confirmPendingOverflowAdd]/[cancelPendingOverflowAdd]. */
    sealed class PendingOverflowAdd {
        data class ElementBox(val pageId: Long) : PendingOverflowAdd()
        data class WidgetRow(val pageId: Long, val kind: String, val spanY: Int) : PendingOverflowAdd()
    }

    val pendingOverflowAdd: StateFlow<PendingOverflowAdd?> get() = _pendingOverflowAdd
    private val _pendingOverflowAdd = MutableStateFlow<PendingOverflowAdd?>(null)

    fun confirmPendingOverflowAdd() {
        val pending = _pendingOverflowAdd.value ?: return
        _pendingOverflowAdd.value = null
        viewModelScope.launch {
            when (pending) {
                is PendingOverflowAdd.ElementBox -> pageContentRepository.addElementBox(pending.pageId)
                is PendingOverflowAdd.WidgetRow -> pageContentRepository.addWidgetRow(pending.pageId, pending.kind, pending.spanY)
            }
        }
    }

    fun cancelPendingOverflowAdd() {
        _pendingOverflowAdd.value = null
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

    fun setRowCornerRadiusPercent(row: PageRow, percent: Int) {
        viewModelScope.launch { pageContentRepository.setRowCornerRadiusPercent(row, percent) }
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

    fun addPageLinkEntryToRow(pageRowId: Long, pageId: Long) {
        viewModelScope.launch { pageContentRepository.addPageLinkEntry(pageRowId, pageId) }
    }

    /** Pinning a contact into a row also ensures a [PinnedContact] snapshot exists for it (name/photo
     *  cached, not a live content-resolver lookup on every render) — the same mechanism search's own
     *  contact pinning already uses, just reused here instead of invented twice. */
    fun addContactEntryToRow(pageRowId: Long, contact: ContactInfo) {
        viewModelScope.launch {
            contactsRepository.pin(contact)
            pageContentRepository.addContactEntry(pageRowId, contact.id)
        }
    }

    fun addActionButtonEntryToRow(pageRowId: Long, actionButtonId: Long) {
        viewModelScope.launch { pageContentRepository.addActionButtonEntry(pageRowId, actionButtonId) }
    }

    fun setRowPillMode(row: PageRow, pillMode: Boolean) {
        viewModelScope.launch { pageContentRepository.setRowPillMode(row, pillMode) }
    }

    fun setPanelRowColumns(row: PageRow, rows: List<Int>) {
        viewModelScope.launch { pageContentRepository.setPanelRowColumns(row, rows) }
    }

    fun setDockRowColumns(row: PageRow, rows: List<Int>) {
        viewModelScope.launch { pageContentRepository.setDockRowColumns(row, rows) }
    }

    fun setRowDockVisibilityMode(row: PageRow, mode: com.bisc.flauncher.data.model.DockVisibilityMode) {
        viewModelScope.launch { pageContentRepository.setRowDockVisibilityMode(row, mode) }
    }

    fun setRowEnabled(row: PageRow, enabled: Boolean) {
        viewModelScope.launch { pageContentRepository.setRowEnabled(row, enabled) }
    }

    fun setRowShowLabels(row: PageRow, showLabels: Boolean) {
        viewModelScope.launch { pageContentRepository.setRowShowLabels(row, showLabels) }
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

    fun removeRowEntry(entry: RowEntry) {
        viewModelScope.launch {
            pageContentRepository.removeEntry(entry)
            if (entry.type == GridItemType.FOLDER) {
                entry.payload.toLongOrNull()?.let { folderRepository.deleteFolderById(it) }
            }
        }
    }

    fun reorderRowEntries(orderedEntries: List<RowEntry>) {
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
                val rowEntry = rowEntriesByRow.value.values.flatten()
                    .find { it.type == GridItemType.FOLDER && it.payload == item.folderId.toString() }
                if (rowEntry != null) {
                    pageContentRepository.removeEntry(rowEntry)
                } else {
                    val parentEntry = folderItemsByFolder.value.values.flatten()
                        .find { it.type == GridItemType.FOLDER && it.payload == item.folderId.toString() }
                    parentEntry?.let { folderRepository.removeFolderItem(it) }
                }
            }
        }
    }
}
