package com.bisc.flauncher.ui.settings

import android.content.ComponentName
import android.content.pm.ShortcutInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.AppListDisplayMode
import com.bisc.flauncher.data.model.AppListLayout
import com.bisc.flauncher.data.model.AppOverride
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.data.model.IconPackInfo
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.RevealMode
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.data.model.SearchEngine
import com.bisc.flauncher.data.model.SearchKeyboardType
import com.bisc.flauncher.data.model.SidebarCategoryViewMode
import com.bisc.flauncher.data.model.DesignPreset
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.repository.AppOverrideRepository
import com.bisc.flauncher.data.repository.FolderRepository
import com.bisc.flauncher.data.repository.IconPackRepository
import com.bisc.flauncher.data.repository.InstalledAppsRepository
import com.bisc.flauncher.data.repository.MapsRepository
import com.bisc.flauncher.data.repository.PageContentRepository
import com.bisc.flauncher.data.repository.PresetRepository
import com.bisc.flauncher.data.repository.ShortcutRepository
import com.bisc.flauncher.data.repository.SidebarRepository
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.home.dock.ResolvedSidebarItem
import com.bisc.flauncher.ui.home.dock.SidebarEntryType
import com.bisc.flauncher.ui.home.dock.resolveSidebarItems
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
    private val sidebarRepository: SidebarRepository,
    private val appOverrideRepository: AppOverrideRepository,
    private val shortcutRepository: ShortcutRepository,
    private val mapsRepository: MapsRepository,
    private val pageContentRepository: PageContentRepository,
    private val presetRepository: PresetRepository,
    private val folderRepository: FolderRepository,
    private val websiteRepository: com.bisc.flauncher.data.repository.WebsiteRepository,
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

    val handedness: StateFlow<Handedness> =
        repository.handedness.stateIn(viewModelScope, SharingStarted.Eagerly, Handedness.RIGHT)

    val activeIconPack: StateFlow<String?> =
        repository.activeIconPack.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val fontPreference: StateFlow<FontPreference> =
        repository.fontPreference.stateIn(viewModelScope, SharingStarted.Eagerly, FontPreference.SYSTEM)

    val iconShape: StateFlow<IconShape> =
        repository.iconShape.stateIn(viewModelScope, SharingStarted.Eagerly, IconShape.ROUNDED_SQUARE)

    val searchAlphabetRailAppearance: StateFlow<ElementAppearance?> =
        repository.searchAlphabetRailAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setSearchAlphabetRailAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setSearchAlphabetRailAppearance(appearance) }
    }

    fun resetSearchAlphabetRailAppearance() {
        viewModelScope.launch { repository.resetSearchAlphabetRailAppearance() }
    }

    fun pageHideStatusBar(pageId: Long): Flow<Boolean> = repository.pageHideStatusBar(pageId)

    fun setPageHideStatusBar(pageId: Long, hide: Boolean) {
        viewModelScope.launch { repository.setPageHideStatusBar(pageId, hide) }
    }

    // Includes every page (not just the filtered `pages` above, which excludes the app overview
    // page), so "everywhere" actually means everywhere, including app overview's own flag.
    val statusBarHiddenEverywhere: StateFlow<Boolean> = pageContentRepository.allPages.flatMapLatest { allPages ->
        val perPageFlows = allPages.map { repository.pageHideStatusBar(it.id) }
        val sharedFlows = listOf(repository.quickOverviewHideStatusBar, repository.searchHideStatusBar, repository.folderHideStatusBar)
        combine(perPageFlows + sharedFlows) { values -> values.all { it } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setStatusBarHiddenEverywhere(hidden: Boolean) {
        viewModelScope.launch {
            val pageIds = pageContentRepository.allPages.first().map { it.id }
            repository.setStatusBarHiddenEverywhere(pageIds, hidden)
        }
    }

    val quickOverviewHideStatusBar: StateFlow<Boolean> =
        repository.quickOverviewHideStatusBar.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setQuickOverviewHideStatusBar(hide: Boolean) {
        viewModelScope.launch { repository.setQuickOverviewHideStatusBar(hide) }
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

    val accentColor: StateFlow<Int> =
        repository.accentColor.stateIn(viewModelScope, SharingStarted.Eagerly, 0xFF3B82F6.toInt())

    val sidebarPanelAppearance: StateFlow<ElementAppearance> =
        repository.sidebarPanelAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    val folderCardAppearance: StateFlow<ElementAppearance> =
        repository.folderCardAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    fun folderShowAddRow(folderId: Long): Flow<Boolean> = repository.folderShowAddRow(folderId)

    fun setFolderShowAddRow(folderId: Long, show: Boolean) {
        viewModelScope.launch { repository.setFolderShowAddRow(folderId, show) }
    }

    fun hideFolderAddRowEverywhere() {
        viewModelScope.launch { repository.hideFolderAddRowEverywhere(folders.value.map { it.id }) }
    }

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

    val iconLoader: com.bisc.flauncher.util.AppIconLoader get() = appIconLoader

    fun loadPackIcon(iconPackPackage: String, drawableName: String): ImageBitmap? =
        appIconLoader.loadPackIcon(iconPackPackage, drawableName)

    suspend fun availableIconPackIcons(iconPackPackage: String): List<com.bisc.flauncher.data.model.IconPackIcon> =
        iconPackRepository.availableIcons(iconPackPackage)

    val keyboardAppearance: StateFlow<ElementAppearance> =
        repository.keyboardAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    val searchResultCardAppearance: StateFlow<ElementAppearance> =
        repository.searchResultCardAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    val globalBackground: StateFlow<BackgroundConfig> =
        repository.globalBackground.stateIn(viewModelScope, SharingStarted.Eagerly, BackgroundConfig())

    val searchOverlayBackgroundOverride: StateFlow<BackgroundConfig?> =
        repository.searchOverlayBackgroundOverride.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val quickOverviewBackgroundOverride: StateFlow<BackgroundConfig?> =
        repository.quickOverviewBackgroundOverride.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val searchShowAzIndex: StateFlow<Boolean> =
        repository.searchShowAzIndex.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val searchHideStatusBar: StateFlow<Boolean> =
        repository.searchHideStatusBar.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val searchKeyboardSparks: StateFlow<Boolean> =
        repository.searchKeyboardSparks.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setSearchKeyboardSparks(enabled: Boolean) {
        viewModelScope.launch { repository.setSearchKeyboardSparks(enabled) }
    }

    val searchKeyboardSparkColor: StateFlow<Int> =
        repository.searchKeyboardSparkColor.stateIn(viewModelScope, SharingStarted.Eagerly, 0xFF34D058.toInt())

    fun setSearchKeyboardSparkColor(color: Int) {
        viewModelScope.launch { repository.setSearchKeyboardSparkColor(color) }
    }

    val searchKeyboardLayout: StateFlow<com.bisc.flauncher.data.model.KeyboardLayout> =
        repository.searchKeyboardLayout.stateIn(viewModelScope, SharingStarted.Eagerly, com.bisc.flauncher.data.model.KeyboardLayout.QWERTY)

    fun setSearchKeyboardLayout(layout: com.bisc.flauncher.data.model.KeyboardLayout) {
        viewModelScope.launch { repository.setSearchKeyboardLayout(layout) }
    }

    val searchKeyboardShowNumberRow: StateFlow<Boolean> =
        repository.searchKeyboardShowNumberRow.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val searchIncluded: StateFlow<Set<SearchCategory>> =
        repository.searchIncluded.stateIn(viewModelScope, SharingStarted.Eagerly, SearchCategory.entries.toSet())
    val searchCategoryOrder: StateFlow<List<SearchCategory>> =
        repository.searchCategoryOrder.stateIn(viewModelScope, SharingStarted.Eagerly, SearchCategory.entries)
    val searchAiApp: StateFlow<String?> =
        repository.searchAiApp.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val searchBrowserApp: StateFlow<String?> =
        repository.searchBrowserApp.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val searchMapsApp: StateFlow<String?> =
        repository.searchMapsApp.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val searchKeyboardType: StateFlow<SearchKeyboardType> =
        repository.searchKeyboardType.stateIn(viewModelScope, SharingStarted.Eagerly, SearchKeyboardType.CUSTOM)
    val searchEngine: StateFlow<SearchEngine> =
        repository.searchEngine.stateIn(viewModelScope, SharingStarted.Eagerly, SearchEngine.DUCKDUCKGO)
    val searchEngineCustomUrl: StateFlow<String> =
        repository.searchEngineCustomUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val searchIndexShowRailBar: StateFlow<Boolean> =
        repository.searchIndexShowRailBar.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val searchHeaderLetterAppearance: StateFlow<ElementAppearance> =
        repository.searchHeaderLetterAppearance.stateIn(
            viewModelScope, SharingStarted.Eagerly, ElementAppearance(fillMode = ElementFillMode.NONE, borderEnabled = true)
        )
    val searchAlphabetLetterAppearance: StateFlow<ElementAppearance> =
        repository.searchAlphabetLetterAppearance.stateIn(
            viewModelScope, SharingStarted.Eagerly, ElementAppearance(fillMode = ElementFillMode.NONE)
        )
    val appListLayout: StateFlow<AppListLayout> =
        repository.appListLayout.stateIn(viewModelScope, SharingStarted.Eagerly, AppListLayout.LIST)

    fun setAppListLayout(layout: AppListLayout) {
        viewModelScope.launch { repository.setAppListLayout(layout) }
    }

    val appListDisplayMode: StateFlow<AppListDisplayMode> =
        repository.appListDisplayMode.stateIn(viewModelScope, SharingStarted.Eagerly, AppListDisplayMode.FULLSCREEN)
    val appListSearchButtonAppearance: StateFlow<ElementAppearance?> =
        repository.appListSearchButtonAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun resetAppListSearchButtonAppearance() {
        viewModelScope.launch { repository.resetAppListSearchButtonAppearance() }
    }

    fun setAppListSearchButtonAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setAppListSearchButtonAppearance(appearance) }
    }

    val appListShowSearchButton: StateFlow<Boolean> =
        repository.appListShowSearchButton.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setAppListShowSearchButton(show: Boolean) {
        viewModelScope.launch { repository.setAppListShowSearchButton(show) }
    }

    val appListContentAppearance: StateFlow<ElementAppearance> =
        repository.appListContentAppearance.stateIn(viewModelScope, SharingStarted.Eagerly, ElementAppearance(borderEnabled = true))

    val dockMatchWidgetCorners: StateFlow<Boolean> =
        repository.dockMatchWidgetCorners.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val chargingAnimationColor: StateFlow<Int> =
        repository.chargingAnimationColor.stateIn(viewModelScope, SharingStarted.Eagerly, 0xFF34D058.toInt())

    fun setChargingAnimationColor(color: Int) {
        viewModelScope.launch { repository.setChargingAnimationColor(color) }
    }

    val chargingAnimationEnabled: StateFlow<Boolean> =
        repository.chargingAnimationEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setChargingAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setChargingAnimationEnabled(enabled) }
    }

    val homeScreenWidgetsEnabled: StateFlow<Boolean> =
        repository.homeScreenWidgetsEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setHomeScreenWidgetsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setHomeScreenWidgetsEnabled(enabled) }
    }

    val activeDesignPreset: StateFlow<DesignPreset?> =
        repository.activeDesignPreset.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pages: StateFlow<List<LauncherPage>> =
        pageContentRepository.pages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val gestureActions: StateFlow<Map<HomeGesture, GestureAction>> =
        combine(HomeGesture.entries.map { repository.gestureAction(it) }) { actions ->
            HomeGesture.entries.zip(actions.toList()).toMap()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

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
            apps.filter { overrides[it.componentName.flattenToString()]?.hidden != true }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun resolveLabel(componentName: ComponentName): String {
        val custom = appOverrides.value[componentName.flattenToString()]?.customLabel
        return if (!custom.isNullOrBlank()) custom else installedAppsRepository.rawLabelFor(componentName)
    }

    fun overrideFor(componentName: ComponentName): AppOverride =
        appOverrides.value[componentName.flattenToString()] ?: AppOverride(componentName.flattenToString())

    fun saveAppOverride(componentName: ComponentName, customLabel: String?, hidden: Boolean, revealMode: RevealMode, revealCode: String?) {
        viewModelScope.launch {
            val effectiveMode = if (hidden) revealMode else RevealMode.NONE
            val existing = appOverrides.value[componentName.flattenToString()]
            appOverrideRepository.save(
                AppOverride(
                    componentName = componentName.flattenToString(),
                    iconPackPackage = existing?.iconPackPackage,
                    iconPackDrawable = existing?.iconPackDrawable,
                    customLabel = customLabel?.trim()?.takeIf { it.isNotEmpty() },
                    hidden = hidden,
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

    val sidebarItems: StateFlow<List<ResolvedSidebarItem>> =
        combine(sidebarRepository.items, labeledApps, pinnedShortcuts, savedMapTargets) { items, apps, shortcuts, targets ->
            resolveSidebarItems(items, apps, shortcuts, targets)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { repository.setTheme(theme) }
    }

    fun setHandedness(handedness: Handedness) {
        viewModelScope.launch { repository.setHandedness(handedness) }
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

    fun setSidebarPanelAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setSidebarPanelAppearance(appearance) }
    }

    fun setFolderCardAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setFolderCardAppearance(appearance) }
    }

    fun setGlobalBackground(config: BackgroundConfig) {
        viewModelScope.launch { repository.setGlobalBackground(config) }
    }

    fun setSearchOverlayBackground(config: BackgroundConfig) {
        viewModelScope.launch { repository.setSearchOverlayBackground(config) }
    }

    fun clearSearchOverlayBackground() {
        viewModelScope.launch { repository.clearSearchOverlayBackground() }
    }

    fun setQuickOverviewBackground(config: BackgroundConfig) {
        viewModelScope.launch { repository.setQuickOverviewBackground(config) }
    }

    fun clearQuickOverviewBackground() {
        viewModelScope.launch { repository.clearQuickOverviewBackground() }
    }

    fun setKeyboardAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setKeyboardAppearance(appearance) }
    }

    fun setSearchResultCardAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setSearchResultCardAppearance(appearance) }
    }

    fun setAccentColor(argb: Int) {
        viewModelScope.launch { repository.setAccentColor(argb) }
    }

    fun setSearchShowAzIndex(show: Boolean) {
        viewModelScope.launch { repository.setSearchShowAzIndex(show) }
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

    fun setSearchAiApp(packageName: String?) {
        viewModelScope.launch { repository.setSearchAiApp(packageName) }
    }

    fun setSearchBrowserApp(packageName: String?) {
        viewModelScope.launch { repository.setSearchBrowserApp(packageName) }
    }

    fun setSearchMapsApp(packageName: String?) {
        viewModelScope.launch { repository.setSearchMapsApp(packageName) }
    }

    fun setSearchKeyboardType(type: SearchKeyboardType) {
        viewModelScope.launch { repository.setSearchKeyboardType(type) }
    }

    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch { repository.setSearchEngine(engine) }
    }

    fun setSearchEngineCustomUrl(url: String) {
        viewModelScope.launch { repository.setSearchEngineCustomUrl(url) }
    }

    val searchKeyboardHaptics: StateFlow<Boolean> =
        repository.searchKeyboardHaptics.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setSearchKeyboardHaptics(enabled: Boolean) {
        viewModelScope.launch { repository.setSearchKeyboardHaptics(enabled) }
    }

    fun setSearchKeyboardShowNumberRow(show: Boolean) {
        viewModelScope.launch { repository.setSearchKeyboardShowNumberRow(show) }
    }

    fun setSearchIndexShowRailBar(show: Boolean) {
        viewModelScope.launch { repository.setSearchIndexShowRailBar(show) }
    }

    fun setSearchHeaderLetterAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setSearchHeaderLetterAppearance(appearance) }
    }

    fun setSearchAlphabetLetterAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setSearchAlphabetLetterAppearance(appearance) }
    }

    fun setAppListDisplayMode(mode: AppListDisplayMode) {
        // Layer 2's own background otherwise still renders (now covering the whole screen, since
        // fullscreen mode paints it as a full-bleed fill rather than a bounded card) and can make it
        // look like a background image change did nothing, when it's actually just hidden behind it.
        // Unconditional on every select of FULLSCREEN (not just an actual mode change) so this also
        // recovers a page that's *already* stuck with an explicit non-zero opacity from before — e.g.
        // fullscreen is the app's own default, so most people never see an actual mode-switch event.
        viewModelScope.launch {
            repository.setAppListDisplayMode(mode)
            if (mode == AppListDisplayMode.FULLSCREEN) {
                repository.setAppListContentAppearance(appListContentAppearance.value.copy(opacity = 0f))
            }
        }
    }

    fun setAppListContentAppearance(appearance: ElementAppearance) {
        viewModelScope.launch { repository.setAppListContentAppearance(appearance) }
    }

    fun applyDesignPreset(preset: DesignPreset) {
        viewModelScope.launch { presetRepository.applyPreset(preset) }
    }

    fun sidebarCategoryViewMode(entry: SidebarEntryType): Flow<SidebarCategoryViewMode> =
        repository.sidebarCategoryViewMode(entry.name)

    fun setSidebarCategoryViewMode(entry: SidebarEntryType, mode: SidebarCategoryViewMode) {
        viewModelScope.launch { repository.setSidebarCategoryViewMode(entry.name, mode) }
    }

    fun sidebarCategoryShowAddRow(entry: SidebarEntryType): Flow<Boolean> =
        repository.sidebarCategoryShowAddRow(entry.name)

    fun setSidebarCategoryShowAddRow(entry: SidebarEntryType, show: Boolean) {
        viewModelScope.launch { repository.setSidebarCategoryShowAddRow(entry.name, show) }
    }

    fun setGestureAction(gesture: HomeGesture, action: GestureAction) {
        viewModelScope.launch { repository.setGestureAction(gesture, action) }
    }

    fun addSidebarCategory(entry: SidebarEntryType) {
        viewModelScope.launch { sidebarRepository.addCategory(entry, sidebarItems.value.size) }
    }

    fun addSidebarApp(app: AppInfo) {
        viewModelScope.launch { sidebarRepository.addApp(app.componentName.flattenToString(), sidebarItems.value.size) }
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

    fun addSidebarShortcut(info: ShortcutInfo) {
        viewModelScope.launch {
            val id = shortcutRepository.pin(info)
            sidebarRepository.addShortcut(id, sidebarItems.value.size)
            dismissShortcutPicker()
        }
    }

    fun addSidebarMapTarget(label: String, lat: Double, lon: Double, address: String = "") {
        viewModelScope.launch {
            val id = mapsRepository.add(label, lat, lon, address)
            sidebarRepository.addMapTarget(id, sidebarItems.value.size)
        }
    }

    fun removeSidebarItem(item: ResolvedSidebarItem) {
        viewModelScope.launch { sidebarRepository.remove(item.raw) }
    }

    fun setSidebarItemIcon(item: ResolvedSidebarItem, iconKey: String?) {
        viewModelScope.launch { sidebarRepository.setCustomIcon(item.raw, iconKey) }
    }

    fun reorderSidebarItems(reordered: List<ResolvedSidebarItem>) {
        viewModelScope.launch { sidebarRepository.reorder(reordered.map { it.raw }) }
    }
}
