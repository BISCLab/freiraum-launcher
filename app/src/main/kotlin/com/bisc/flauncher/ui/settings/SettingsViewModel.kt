package com.bisc.flauncher.ui.settings

import android.content.ComponentName
import android.content.pm.ShortcutInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.AppOverride
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.data.model.IconPackInfo
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.AppGridBarPosition
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.RevealMode
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.data.model.SearchEngine
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.repository.AppOverrideRepository
import com.bisc.flauncher.data.repository.FolderRepository
import com.bisc.flauncher.data.repository.IconPackRepository
import com.bisc.flauncher.data.repository.InstalledAppsRepository
import com.bisc.flauncher.data.repository.MapsRepository
import com.bisc.flauncher.data.repository.PageContentRepository
import com.bisc.flauncher.data.repository.ThemeRepository
import com.bisc.flauncher.data.repository.ShortcutRepository
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.theme.FontPreference
import com.bisc.flauncher.ui.theme.ThemePreference
import com.bisc.flauncher.util.AppIconLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val iconPackRepository: IconPackRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val appIconLoader: AppIconLoader,
    private val appOverrideRepository: AppOverrideRepository,
    private val shortcutRepository: ShortcutRepository,
    private val mapsRepository: MapsRepository,
    private val pageContentRepository: PageContentRepository,
    private val themeRepository: ThemeRepository,
    private val folderRepository: FolderRepository,
    private val websiteRepository: com.bisc.flauncher.data.repository.WebsiteRepository,
    private val actionButtonRepository: com.bisc.flauncher.data.repository.ActionButtonRepository,
    private val backupManager: com.bisc.flauncher.data.backup.BackupManager
) : ViewModel() {
    fun exportBackup(uri: android.net.Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(backupManager.export(uri)) }
    }

    fun importBackup(uri: android.net.Uri, onResult: (com.bisc.flauncher.data.backup.BackupManager.ImportResult) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.stageImport(uri)
            onResult(result)
            if (result == com.bisc.flauncher.data.backup.BackupManager.ImportResult.READY) {
                kotlinx.coroutines.delay(900)
                backupManager.restart()
            }
        }
    }

    fun loadIcon(componentName: ComponentName): ImageBitmap? = appIconLoader.load(componentName)

    val theme: StateFlow<ThemePreference> =
        repository.theme.stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.SYSTEM)

    val appOverviewHandedness: StateFlow<Handedness> =
        repository.appOverviewHandedness.stateIn(viewModelScope, SharingStarted.Eagerly, Handedness.RIGHT)

    val activeIconPack: StateFlow<String?> =
        repository.activeIconPack.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val fontPreference: StateFlow<FontPreference> =
        repository.fontPreference.stateIn(viewModelScope, SharingStarted.Eagerly, FontPreference.SYSTEM)

    val iconShape: StateFlow<IconShape> =
        repository.iconShape.stateIn(viewModelScope, SharingStarted.Eagerly, IconShape.ROUNDED_SQUARE)

    val iconSizePercent: StateFlow<Int> =
        repository.iconSizePercent.stateIn(viewModelScope, SharingStarted.Eagerly, 100)

    val defaultCornerRadiusPercent: StateFlow<Int> =
        repository.defaultCornerRadiusPercent.stateIn(viewModelScope, SharingStarted.Eagerly, com.bisc.flauncher.ui.theme.DEFAULT_CORNER_RADIUS_PERCENT)

    val appListRailAppearance: StateFlow<ElementAppearance?> =
        repository.appListRailAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setAppListRailAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setAppListRailAppearance(appearance) }
    }

    fun resetAppListRailAppearance() {
        viewModelScope.launch { repository.resetAppListRailAppearance() }
    }

    fun pageHideStatusBar(pageId: Long): Flow<Boolean> = repository.pageHideStatusBar(pageId)

    fun setPageHideStatusBar(pageId: Long, hide: Boolean) {
        viewModelScope.launch { repository.setPageHideStatusBar(pageId, hide) }
    }

    // Includes every page (not just the filtered `pages` above, which excludes the app overview
    // page), so "everywhere" actually means everywhere, including app overview's own flag. Text
    // search is the exception: it uses its own searchHideStatusBar, not its page's flag.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val statusBarHiddenEverywhere: StateFlow<Boolean> = pageContentRepository.allPages.flatMapLatest { allPages ->
        val perPageFlows = allPages.filter { it.kind != LauncherPageKind.SEARCH }.map { repository.pageHideStatusBar(it.id) }
        val sharedFlows = listOf(repository.searchHideStatusBar, repository.folderHideStatusBar)
        combine(perPageFlows + sharedFlows) { values -> values.all { it } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setStatusBarHiddenEverywhere(hidden: Boolean) {
        viewModelScope.launch {
            val pageIds = pageContentRepository.allPages.first().map { it.id }
            repository.setStatusBarHiddenEverywhere(pageIds, hidden)
        }
    }

    val folderHideStatusBar: StateFlow<Boolean> =
        repository.folderHideStatusBar.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setFolderHideStatusBar(hide: Boolean) {
        viewModelScope.launch { repository.setFolderHideStatusBar(hide) }
    }

    val folderHideDock: StateFlow<Boolean> =
        repository.folderHideDock.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setFolderHideDock(hide: Boolean) {
        viewModelScope.launch { repository.setFolderHideDock(hide) }
    }

    val folderShowLabels: StateFlow<Boolean> =
        repository.folderShowLabels.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setFolderShowLabels(show: Boolean) {
        viewModelScope.launch { repository.setFolderShowLabels(show) }
    }

    val accentColor: StateFlow<Int> =
        repository.accentColor.stateIn(viewModelScope, SharingStarted.Eagerly, com.bisc.flauncher.ui.theme.DEFAULT_ACCENT_COLOR)

    val folderCardAppearance: StateFlow<ElementAppearance> =
        repository.folderCardAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    val folders: StateFlow<List<com.bisc.flauncher.data.model.Folder>> =
        folderRepository.folders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val folderItems: StateFlow<List<com.bisc.flauncher.data.model.FolderItem>> =
        folderRepository.folderItems.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pinnedWebsites: StateFlow<List<com.bisc.flauncher.data.model.PinnedWebsite>> =
        websiteRepository.websites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun createFolder(name: String) {
        viewModelScope.launch { folderRepository.createFolder(name) }
    }

    /** Names a new, empty-or-not folder in one step, matching the home screen's own folder creator. */
    fun createFolderWithApps(name: String, apps: List<AppInfo>) {
        viewModelScope.launch {
            val folderId = folderRepository.createFolder(name)
            apps.forEach { folderRepository.addItemToFolder(folderId, com.bisc.flauncher.data.model.GridItemType.APP, it.componentName.flattenToString()) }
        }
    }

    // ---- Folder contents: apps, sub-folders, shortcuts, places, websites ----

    fun addAppToFolder(folderId: Long, app: AppInfo) {
        viewModelScope.launch { folderRepository.addItemToFolder(folderId, com.bisc.flauncher.data.model.GridItemType.APP, app.componentName.flattenToString()) }
    }

    fun addExistingFolderToFolder(folderId: Long, childFolderId: Long) {
        viewModelScope.launch { folderRepository.addItemToFolder(folderId, com.bisc.flauncher.data.model.GridItemType.FOLDER, childFolderId.toString()) }
    }

    fun createFolderInFolder(folderId: Long, name: String) {
        viewModelScope.launch { folderRepository.addNestedFolder(folderId, name) }
    }

    private val _folderShortcutPickerFolderId = MutableStateFlow<Long?>(null)

    fun openFolderShortcutAppPicker(folderId: Long) {
        _folderShortcutPickerFolderId.value = folderId
        openShortcutAppPicker()
    }

    fun addShortcutToFolder(info: ShortcutInfo) {
        val folderId = _folderShortcutPickerFolderId.value ?: return
        viewModelScope.launch {
            val id = shortcutRepository.pin(info)
            folderRepository.addShortcutToFolder(folderId, info.`package`, info.id, (info.shortLabel ?: info.longLabel ?: info.id).toString())
            dismissShortcutPicker()
            _folderShortcutPickerFolderId.value = null
        }
    }

    fun addMapTargetToFolder(folderId: Long, targetId: Long) {
        viewModelScope.launch { folderRepository.addMapTargetToFolder(folderId, targetId) }
    }

    fun addWidgetToFolder(folderId: Long, kind: String, spanY: Int) {
        viewModelScope.launch { folderRepository.addWidgetToFolder(folderId, kind, spanY) }
    }

    fun addNewMapTargetToFolder(folderId: Long, label: String, lat: Double, lon: Double, address: String = "") {
        viewModelScope.launch {
            val targetId = mapsRepository.add(label, lat, lon, address)
            folderRepository.addMapTargetToFolder(folderId, targetId)
        }
    }

    fun addWebsiteToFolder(folderId: Long, websiteId: Long) {
        viewModelScope.launch { folderRepository.addWebsiteToFolder(folderId, websiteId) }
    }

    fun addNewWebsiteToFolder(folderId: Long, label: String, url: String) {
        viewModelScope.launch {
            val websiteId = websiteRepository.add(label, url)
            folderRepository.addWebsiteToFolder(folderId, websiteId)
        }
    }

    // Standalone Elements management (Settings -> Elements -> Addresses/Links) - independent of
    // any particular row/folder placement, unlike the *ToFolder methods above.
    fun addMapTarget(label: String, lat: Double, lon: Double, address: String = "") {
        viewModelScope.launch { mapsRepository.add(label, lat, lon, address) }
    }

    fun updateMapTarget(target: com.bisc.flauncher.data.model.SavedMapTarget) {
        viewModelScope.launch { mapsRepository.update(target) }
    }

    fun removeMapTarget(target: com.bisc.flauncher.data.model.SavedMapTarget) {
        viewModelScope.launch { mapsRepository.remove(target) }
    }

    fun addWebsite(label: String, url: String) {
        viewModelScope.launch { websiteRepository.add(label, url) }
    }

    fun updateWebsite(website: com.bisc.flauncher.data.model.PinnedWebsite) {
        viewModelScope.launch { websiteRepository.update(website) }
    }

    fun removeWebsite(website: com.bisc.flauncher.data.model.PinnedWebsite) {
        viewModelScope.launch { websiteRepository.remove(website) }
    }

    val actionButtons: StateFlow<List<com.bisc.flauncher.data.model.ActionButton>> =
        actionButtonRepository.actionButtons.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addActionButton(label: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(actionButtonRepository.add(com.bisc.flauncher.data.model.ActionButton(label = label))) }
    }

    fun updateActionButton(button: com.bisc.flauncher.data.model.ActionButton) {
        viewModelScope.launch { actionButtonRepository.update(button) }
    }

    fun removeActionButton(button: com.bisc.flauncher.data.model.ActionButton) {
        viewModelScope.launch { actionButtonRepository.remove(button) }
    }

    fun setFolderItemSpan(item: com.bisc.flauncher.data.model.FolderItem, spanY: Int) {
        viewModelScope.launch { folderRepository.setFolderItemSpan(item, spanY) }
    }

    fun removeFolderItem(item: com.bisc.flauncher.data.model.FolderItem) {
        viewModelScope.launch { folderRepository.removeFolderItem(item) }
    }

    fun renameFolder(folderId: Long, newName: String) {
        viewModelScope.launch(kotlinx.coroutines.NonCancellable) { folderRepository.renameFolderById(folderId, newName) }
    }

    fun setFolderIcon(
        folderId: Long,
        mode: com.bisc.flauncher.data.model.FolderIconMode,
        color: String? = null,
        packPackage: String? = null,
        packDrawable: String? = null
    ) {
        viewModelScope.launch(kotlinx.coroutines.NonCancellable) {
            folderRepository.setFolderIconById(folderId, mode, color, packPackage, packDrawable)
        }
    }

    fun deleteFolder(folder: com.bisc.flauncher.data.model.Folder) {
        viewModelScope.launch { folderRepository.deleteFolder(folder) }
    }

    fun setFolderAppearanceOverrideEnabled(folderId: Long, enabled: Boolean) {
        viewModelScope.launch { folderRepository.setFolderAppearanceOverrideEnabledById(folderId, enabled) }
    }

    fun setFolderAppearance(folderId: Long, appearance: ElementAppearance) {
        viewModelScope.launch { folderRepository.setFolderAppearanceById(folderId, appearance) }
    }

    fun setFolderBorderOverrideEnabled(folderId: Long, enabled: Boolean) {
        viewModelScope.launch { folderRepository.setFolderBorderOverrideEnabledById(folderId, enabled) }
    }

    fun setFolderBorderAppearance(folderId: Long, appearance: ElementAppearance) {
        viewModelScope.launch { folderRepository.setFolderBorderAppearanceById(folderId, appearance) }
    }

    fun setFolderRowColumns(folderId: Long, rows: List<Int>) {
        viewModelScope.launch { folderRepository.setFolderRowColumnsById(folderId, rows) }
    }

    fun setFolderItemsShowLabels(folderId: Long, show: Boolean) {
        viewModelScope.launch { folderRepository.setFolderShowLabelsById(folderId, show) }
    }

    val iconLoader: com.bisc.flauncher.util.AppIconLoader get() = appIconLoader

    fun loadPackIcon(iconPackPackage: String, drawableName: String): ImageBitmap? =
        appIconLoader.loadPackIcon(iconPackPackage, drawableName)

    suspend fun availableIconPackIcons(iconPackPackage: String): List<com.bisc.flauncher.data.model.IconPackIcon> =
        iconPackRepository.availableIcons(iconPackPackage)

    val searchAppearance: StateFlow<ElementAppearance> =
        repository.searchAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance())

    val searchKeyboardAutoOpen: StateFlow<Boolean> =
        repository.searchKeyboardAutoOpen.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showSettingsTips: StateFlow<Boolean> =
        repository.showSettingsTips.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setShowSettingsTips(show: Boolean) {
        viewModelScope.launch { repository.setShowSettingsTips(show) }
    }

    val searchBarAppearance: StateFlow<ElementAppearance?> =
        repository.searchBarAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val searchBarCornerRadiusPercent: StateFlow<Int?> =
        repository.searchBarCornerRadiusPercent.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val searchBackgroundOverride: StateFlow<BackgroundConfig?> =
        repository.searchBackgroundOverride.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val searchGlobalShade: StateFlow<Float?> =
        repository.searchGlobalShade.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setSearchBackgroundConfig(config: BackgroundConfig) {
        viewModelScope.launch { repository.setSearchBackgroundConfig(config) }
    }

    fun clearSearchBackgroundConfig() {
        viewModelScope.launch { repository.clearSearchBackgroundConfig() }
    }

    fun setSearchGlobalShade(shade: Float) {
        viewModelScope.launch { repository.setSearchGlobalShade(shade) }
    }

    val searchResultsAppearance: StateFlow<ElementAppearance?> =
        repository.searchResultsAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val searchResultsCornerRadiusPercent: StateFlow<Int?> =
        repository.searchResultsCornerRadiusPercent.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val searchKeyboardCapEnabled: StateFlow<Boolean> =
        repository.searchKeyboardCapEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val searchKeyboardCapColor: StateFlow<Int> =
        repository.searchKeyboardCapColor.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsRepository.DEFAULT_KEYBOARD_CAP_COLOR)

    val searchKeyboardCapCornerRadiusPercent: StateFlow<Int> =
        repository.searchKeyboardCapCornerRadiusPercent.stateIn(viewModelScope, SharingStarted.Eagerly, 100)

    val searchKeyboardCapArrow: StateFlow<Boolean> =
        repository.searchKeyboardCapArrow.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val globalBackground: StateFlow<BackgroundConfig> =
        repository.globalBackground.stateIn(viewModelScope, SharingStarted.Eagerly, BackgroundConfig())

    val appListShowAzIndex: StateFlow<Boolean> =
        repository.appListShowAzIndex.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val searchHideStatusBar: StateFlow<Boolean> =
        repository.searchHideStatusBar.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val searchIncluded: StateFlow<Set<SearchCategory>> =
        repository.searchIncluded.stateIn(viewModelScope, SharingStarted.Eagerly, SearchCategory.entries.toSet())
    val searchCategoryOrder: StateFlow<List<SearchCategory>> =
        repository.searchCategoryOrder.stateIn(viewModelScope, SharingStarted.Eagerly, SearchCategory.entries)
    val categoryColors: StateFlow<Map<SearchCategory, Int?>> =
        combine(SearchCategory.entries.map { c -> repository.categoryColor(c).map { color -> c to color } }) { pairs -> pairs.toMap() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val searchAiApp: StateFlow<String?> =
        repository.searchAiApp.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val searchBrowserApp: StateFlow<String?> =
        repository.searchBrowserApp.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val searchMapsApp: StateFlow<String?> =
        repository.searchMapsApp.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val searchEngine: StateFlow<SearchEngine> =
        repository.searchEngine.stateIn(viewModelScope, SharingStarted.Eagerly, SearchEngine.DUCKDUCKGO)
    val searchEngineCustomUrl: StateFlow<String> =
        repository.searchEngineCustomUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val appListHeaderLetterAppearance: StateFlow<ElementAppearance> =
        repository.appListHeaderLetterAppearance.stateIn(
            viewModelScope, SharingStarted.Eagerly, ElementAppearance(fillMode = ElementFillMode.NONE, borderEnabled = true)
        )

    val appListSearchButtonAppearance: StateFlow<ElementAppearance?> =
        repository.appListSearchButtonAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setAppListSearchButtonAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setAppListSearchButtonAppearance(appearance) }
    }

    val appListShowSearchButton: StateFlow<Boolean> =
        repository.appListShowSearchButton.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setAppListShowSearchButton(show: Boolean) {
        viewModelScope.launch { repository.setAppListShowSearchButton(show) }
    }

    val appListOwnSearch: StateFlow<Boolean> = repository.appListOwnSearch.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    fun setAppListOwnSearch(own: Boolean) { viewModelScope.launch { repository.setAppListOwnSearch(own) } }

    // ---- App grid page ----

    val appGridColumns: StateFlow<Int> = repository.appGridColumns.stateIn(viewModelScope, SharingStarted.Eagerly, 4)
    fun setAppGridColumns(columns: Int) { viewModelScope.launch { repository.setAppGridColumns(columns) } }

    val appGridIconSizePercent: StateFlow<Int> = repository.appGridIconSizePercent.stateIn(viewModelScope, SharingStarted.Eagerly, 100)
    fun setAppGridIconSizePercent(percent: Int) { viewModelScope.launch { repository.setAppGridIconSizePercent(percent) } }

    val appGridShowLabels: StateFlow<Boolean> = repository.appGridShowLabels.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    fun setAppGridShowLabels(show: Boolean) { viewModelScope.launch { repository.setAppGridShowLabels(show) } }

    val appGridBarPosition: StateFlow<AppGridBarPosition> =
        repository.appGridBarPosition.stateIn(viewModelScope, SharingStarted.Eagerly, AppGridBarPosition.BOTTOM)
    fun setAppGridBarPosition(position: AppGridBarPosition) { viewModelScope.launch { repository.setAppGridBarPosition(position) } }

    val appGridBarOpensTextSearch: StateFlow<Boolean> =
        repository.appGridBarOpensTextSearch.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    fun setAppGridBarOpensTextSearch(open: Boolean) { viewModelScope.launch { repository.setAppGridBarOpensTextSearch(open) } }

    val appGridKeyboardAutoOpen: StateFlow<Boolean> =
        repository.appGridKeyboardAutoOpen.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    fun setAppGridKeyboardAutoOpen(open: Boolean) { viewModelScope.launch { repository.setAppGridKeyboardAutoOpen(open) } }

    val appGridBarAppearance: StateFlow<ElementAppearance?> =
        repository.appGridBarAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    fun setAppGridBarAppearance(appearance: ElementAppearance) { viewModelScope.launch { repository.setAppGridBarAppearance(appearance) } }
    fun resetAppGridBarAppearance() { viewModelScope.launch { repository.resetAppGridBarAppearance() } }

    val appGridFolderIds: StateFlow<List<Long>> = repository.appGridFolderIds.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun setAppGridFolderIds(ids: List<Long>) { viewModelScope.launch { repository.setAppGridFolderIds(ids) } }

    val appGridFoldersAlphabetical: StateFlow<Boolean> =
        repository.appGridFoldersAlphabetical.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    fun setAppGridFoldersAlphabetical(alphabetical: Boolean) { viewModelScope.launch { repository.setAppGridFoldersAlphabetical(alphabetical) } }

    val appGridHideFolderApps: StateFlow<Boolean> =
        repository.appGridHideFolderApps.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    fun setAppGridHideFolderApps(hide: Boolean) { viewModelScope.launch { repository.setAppGridHideFolderApps(hide) } }

    /** Own colors of the two accented surfaces; null = follow [accentColor]. */
    val chargingAnimationColor: StateFlow<Int?> =
        repository.chargingAnimationColor.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setChargingAnimationColor(color: Int?) {
        viewModelScope.launch { repository.setChargingAnimationColor(color) }
    }

    val chargingAnimationEnabled: StateFlow<Boolean> =
        repository.chargingAnimationEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setChargingAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setChargingAnimationEnabled(enabled) }
    }

    val pages: StateFlow<List<LauncherPage>> =
        pageContentRepository.pages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allPages: StateFlow<List<LauncherPage>> =
        pageContentRepository.allPages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val gestureActions: StateFlow<Map<HomeGesture, GestureAction>> =
        combine(HomeGesture.entries.map { repository.gestureAction(it) }) { actions ->
            HomeGesture.entries.zip(actions.toList()).toMap()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // Home's own pinned Dock/Panel rows - surfaced here only so SettingsGesturesScreen can show
    // SWIPE_UP/SWIPE_DOWN locked (with a reason) while the Dock/Panel have claimed that gesture for
    // themselves (see HomeScreen's detectHomeGestures wiring, the actual place that reservation is
    // enforced). Not used for editing either row; that still goes through the Home layout screens.
    val homeDockRow: StateFlow<PageRow?> = combine(pages, pageContentRepository.rows) { pages, rows ->
        val homeId = pages.find { it.isHome }?.id
        rows.find { it.pageId == homeId && it.type == PageRowType.SEARCH_BAR }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val homePanelRow: StateFlow<PageRow?> = combine(pages, pageContentRepository.rows) { pages, rows ->
        val homeId = pages.find { it.isHome }?.id
        rows.find { it.pageId == homeId && it.type == PageRowType.PANEL }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    val appOverrides: StateFlow<Map<String, AppOverride>> =
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
            apps.filter { overrides[it.componentName.flattenToString()]?.hiddenFromAppList != true }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun resolveLabel(componentName: ComponentName): String {
        val custom = appOverrides.value[componentName.flattenToString()]?.customLabel
        return if (!custom.isNullOrBlank()) custom else installedAppsRepository.rawLabelFor(componentName)
    }

    fun saveAppOverride(
        componentName: ComponentName,
        customLabel: String?,
        hiddenFromAppList: Boolean,
        hiddenFromSearch: Boolean,
        revealMode: RevealMode,
        revealCode: String?,
        iconShape: IconShape?,
        iconSizePercent: Int?
    ) {
        viewModelScope.launch {
            val effectiveMode = if (!hiddenFromSearch) revealMode else RevealMode.NONE
            val existing = appOverrides.value[componentName.flattenToString()]
            appOverrideRepository.save(
                AppOverride(
                    componentName = componentName.flattenToString(),
                    iconPackPackage = existing?.iconPackPackage,
                    iconPackDrawable = existing?.iconPackDrawable,
                    iconShape = iconShape,
                    iconSizePercent = iconSizePercent?.coerceIn(0, 200),
                    customLabel = customLabel?.trim()?.takeIf { it.isNotEmpty() },
                    hiddenFromAppList = hiddenFromAppList,
                    hiddenFromSearch = hiddenFromSearch,
                    revealMode = effectiveMode,
                    revealCode = if (effectiveMode == RevealMode.SEARCH_CODE) revealCode?.takeIf { it.isNotEmpty() } else null
                )
            )
        }
    }

    fun setAppIcon(componentName: ComponentName, packPackage: String?, packDrawable: String?) {
        viewModelScope.launch {
            val key = componentName.flattenToString()
            val current = appOverrides.value[key] ?: AppOverride(key)
            appOverrideRepository.save(current.copy(iconPackPackage = packPackage, iconPackDrawable = packDrawable))
        }
    }



    private val pinnedShortcuts: StateFlow<List<com.bisc.flauncher.data.model.PinnedShortcut>> =
        shortcutRepository.pinnedShortcuts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val savedMapTargets: StateFlow<List<com.bisc.flauncher.data.model.SavedMapTarget>> =
        mapsRepository.targets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _iconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val iconPacks: StateFlow<List<IconPackInfo>> = _iconPacks.asStateFlow()

    private val _shortcutAppPickerOpen = MutableStateFlow(false)
    val shortcutAppPickerOpen: StateFlow<Boolean> = _shortcutAppPickerOpen.asStateFlow()

    private val _shortcutPickerForApp = MutableStateFlow<AppInfo?>(null)
    val shortcutPickerForApp: StateFlow<AppInfo?> = _shortcutPickerForApp.asStateFlow()

    private val _availableShortcuts = MutableStateFlow<List<ShortcutInfo>>(emptyList())
    val availableShortcuts: StateFlow<List<ShortcutInfo>> = _availableShortcuts.asStateFlow()

    fun hasShortcutHostPermission(): Boolean = shortcutRepository.hasPermission()

    fun loadShortcutIcon(info: ShortcutInfo) = shortcutRepository.loadIcon(info)

    init {
        viewModelScope.launch { _iconPacks.value = iconPackRepository.installedIconPacks() }
        viewModelScope.launch {
            _installedApps.value = installedAppsRepository.getLaunchableApps()
            installedAppsRepository.packageChanges().collectLatest {
                _installedApps.value = installedAppsRepository.getLaunchableApps()
            }
        }
    }

    /** Persists the chosen Light/Dark/System and immediately re-stamps the background and every
     *  box/card/widget/dock/panel's default fill to match it (see ThemeRepository.reapplyTheme) - 
     *  Light/Dark/System is FLauncher's only appearance control, there's no separate preset layered
     *  on top of it. Switching it clears per-surface customizations back to automatic, hence the
     *  confirm dialog. */
    fun applyTheme(theme: ThemePreference) {
        viewModelScope.launch {
            repository.setTheme(theme)
            themeRepository.reapplyTheme()
        }
    }

    fun setAppOverviewHandedness(handedness: Handedness) {
        viewModelScope.launch { repository.setAppOverviewHandedness(handedness) }
    }

    fun selectIconPack(packageName: String?) {
        viewModelScope.launch { iconPackRepository.selectPack(packageName) }
    }

    fun setFontPreference(font: FontPreference) {
        viewModelScope.launch { repository.setFontPreference(font) }
    }

    fun setIconShape(shape: IconShape) {
        viewModelScope.launch { repository.setIconShape(shape) }
    }

    fun setIconSizePercent(percent: Int) {
        viewModelScope.launch { repository.setIconSizePercent(percent) }
    }

    fun setDefaultCornerRadiusPercent(percent: Int) {
        viewModelScope.launch {
            repository.setDefaultCornerRadiusPercent(percent)
            pageContentRepository.applyCornerRadiusToAllRows(percent)
            folderRepository.applyCornerRadiusToAllItems(percent)
        }
    }

    fun setFolderCardAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setFolderCardAppearance(appearance) }
    }

    fun setGlobalBackground(config: BackgroundConfig) {
        viewModelScope.launch { repository.setGlobalBackground(config) }
    }

    fun setSearchAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setSearchAppearance(appearance) }
    }

    fun setSearchKeyboardAutoOpen(enabled: Boolean) {
        viewModelScope.launch { repository.setSearchKeyboardAutoOpen(enabled) }
    }

    fun setSearchBarAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setSearchBarAppearance(appearance) }
    }

    fun setSearchBarCornerRadiusPercent(percent: Int) {
        viewModelScope.launch { repository.setSearchBarCornerRadiusPercent(percent) }
    }

    fun setSearchResultsAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setSearchResultsAppearance(appearance) }
    }

    val searchResultsEdgeToEdge: StateFlow<Boolean> =
        repository.searchResultsEdgeToEdge.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val searchBarTopLine: StateFlow<Boolean> =
        repository.searchBarTopLine.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setSearchBarTopLine(show: Boolean) {
        viewModelScope.launch { repository.setSearchBarTopLine(show) }
    }

    val searchBarMiddleLine: StateFlow<Boolean> =
        repository.searchBarMiddleLine.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setSearchBarMiddleLine(show: Boolean) {
        viewModelScope.launch { repository.setSearchBarMiddleLine(show) }
    }

    fun setSearchResultsEdgeToEdge(edgeToEdge: Boolean) {
        viewModelScope.launch { repository.setSearchResultsEdgeToEdge(edgeToEdge) }
    }

    fun setSearchResultsCornerRadiusPercent(percent: Int) {
        viewModelScope.launch { repository.setSearchResultsCornerRadiusPercent(percent) }
    }

    fun setSearchKeyboardCapEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSearchKeyboardCapEnabled(enabled) }
    }

    fun setSearchKeyboardCapColor(color: Int) {
        viewModelScope.launch { repository.setSearchKeyboardCapColor(color) }
    }

    fun setSearchKeyboardCapCornerRadiusPercent(percent: Int) {
        viewModelScope.launch { repository.setSearchKeyboardCapCornerRadiusPercent(percent) }
    }

    fun setSearchKeyboardCapArrow(enabled: Boolean) {
        viewModelScope.launch { repository.setSearchKeyboardCapArrow(enabled) }
    }

    fun setAccentColor(argb: Int) {
        viewModelScope.launch { repository.setAccentColor(argb) }
    }

    fun setAppListShowAzIndex(show: Boolean) {
        viewModelScope.launch { repository.setAppListShowAzIndex(show) }
    }

    fun setSearchHideStatusBar(hide: Boolean) {
        viewModelScope.launch { repository.setSearchHideStatusBar(hide) }
    }

    fun setSearchIncluded(category: SearchCategory, include: Boolean) {
        viewModelScope.launch { repository.setSearchIncluded(category, include) }
    }

    fun setSearchCategoryOrder(order: List<SearchCategory>) {
        viewModelScope.launch { repository.setSearchCategoryOrder(order) }
    }

    fun setCategoryColor(category: SearchCategory, color: Int) {
        viewModelScope.launch { repository.setCategoryColor(category, color) }
    }

    fun setSearchAiApp(packageName: String?) {
        viewModelScope.launch { repository.setSearchAiApp(packageName) }
    }

    fun setSearchBrowserApp(packageName: String?) {
        viewModelScope.launch { repository.setSearchBrowserApp(packageName) }
    }

    fun setSearchMapsApp(packageName: String?) {
        viewModelScope.launch { repository.setSearchMapsApp(packageName) }
    }

    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch { repository.setSearchEngine(engine) }
    }

    fun setSearchEngineCustomUrl(url: String) {
        viewModelScope.launch { repository.setSearchEngineCustomUrl(url) }
    }

    fun setAppListHeaderLetterAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setAppListHeaderLetterAppearance(appearance) }
    }

    fun setGestureAction(gesture: HomeGesture, action: GestureAction) {
        viewModelScope.launch { repository.setGestureAction(gesture, action) }
    }

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

}
