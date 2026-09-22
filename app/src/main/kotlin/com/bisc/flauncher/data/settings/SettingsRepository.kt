package com.bisc.flauncher.data.settings

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bisc.flauncher.data.model.AppListDisplayMode
import com.bisc.flauncher.data.model.AppListLayout
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.BackgroundSource
import com.bisc.flauncher.data.model.ColorFill
import com.bisc.flauncher.data.model.DesignPreset
import com.bisc.flauncher.data.model.DesignPresetValues
import com.bisc.flauncher.data.model.DockTapAction
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.FillMode
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.KeyboardLayout
import com.bisc.flauncher.data.model.GradientDirection
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.data.model.SearchEngine
import com.bisc.flauncher.data.model.SearchKeyboardType
import com.bisc.flauncher.data.model.SidebarCategoryViewMode
import com.bisc.flauncher.ui.theme.FontPreference
import com.bisc.flauncher.ui.theme.ThemePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

enum class Handedness { LEFT, RIGHT }

/** Shape applied to the dock's quick-launch icons and search button. */
enum class IconShape { CIRCLE, ROUNDED_SQUARE }

/** Persistent settings in DataStore; each setting is exposed as a flow with its default value. */
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    // ---- General ----

    val theme: Flow<ThemePreference> = dataStore.data.map { prefs ->
        prefs[THEME_KEY]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
            ?: ThemePreference.SYSTEM
    }

    val handedness: Flow<Handedness> = dataStore.data.map { prefs ->
        prefs[HANDEDNESS_KEY]?.let { runCatching { Handedness.valueOf(it) }.getOrNull() }
            ?: Handedness.RIGHT
    }

    val activeIconPack: Flow<String?> = dataStore.data.map { prefs -> prefs[ICON_PACK_KEY] }

    val fontPreference: Flow<FontPreference> = dataStore.data.map { prefs ->
        prefs[FONT_KEY]?.let { runCatching { FontPreference.valueOf(it) }.getOrNull() }
            ?: FontPreference.SYSTEM
    }

    val iconShape: Flow<IconShape> = dataStore.data.map { prefs ->
        prefs[ICON_SHAPE_KEY]?.let { runCatching { IconShape.valueOf(it) }.getOrNull() }
            ?: IconShape.ROUNDED_SQUARE
    }

    val sidebarPanelAppearance: Flow<ElementAppearance> = elementAppearanceFor("sidebar_panel", ElementAppearance(borderEnabled = true))

    val folderCardAppearance: Flow<ElementAppearance> = elementAppearanceFor("folder_card", ElementAppearance(borderEnabled = true))

    val searchOverlayBackground: Flow<BackgroundConfig> =
        backgroundConfigFor("search_overlay", BackgroundConfig(opacity = 0f))

    val quickOverviewBackground: Flow<BackgroundConfig> =
        backgroundConfigFor("quick_overview", BackgroundConfig(opacity = 0f))

    val keyboardAppearance: Flow<ElementAppearance> = elementAppearanceFor("keyboard_background", ElementAppearance(borderEnabled = true))

    val searchResultCardAppearance: Flow<ElementAppearance> = elementAppearanceFor("search_result_card", ElementAppearance(borderEnabled = true))

    val accentColor: Flow<Int> = dataStore.data.map { prefs ->
        prefs[ACCENT_COLOR_KEY] ?: DEFAULT_ACCENT_COLOR
    }

    val dockSingleTapAction: Flow<DockTapAction> = dataStore.data.map { prefs ->
        prefs[DOCK_SINGLE_TAP_ACTION_KEY]?.let { runCatching { DockTapAction.valueOf(it) }.getOrNull() }
            ?: DockTapAction.QUICK_OVERVIEW
    }

    val dockDoubleTapAction: Flow<DockTapAction> = dataStore.data.map { prefs ->
        prefs[DOCK_DOUBLE_TAP_ACTION_KEY]?.let { runCatching { DockTapAction.valueOf(it) }.getOrNull() }
            ?: DockTapAction.FULL_SEARCH
    }

    fun folderShowAddRow(folderId: Long): Flow<Boolean> = dataStore.data.map { it[folderShowAddRowKey(folderId)] ?: true }

    private fun folderShowAddRowKey(folderId: Long) = booleanPreferencesKey("folder_show_add_row_$folderId")

    val chargingAnimationColor: Flow<Int> = dataStore.data.map { it[CHARGING_ANIMATION_COLOR_KEY] ?: 0xFF34D058.toInt() }

    suspend fun setChargingAnimationColor(color: Int) {
        dataStore.edit { it[CHARGING_ANIMATION_COLOR_KEY] = color }
    }

    val chargingAnimationEnabled: Flow<Boolean> = dataStore.data.map { it[CHARGING_ANIMATION_ENABLED_KEY] ?: true }

    val dockMatchWidgetCorners: Flow<Boolean> = dataStore.data.map { it[DOCK_MATCH_WIDGET_CORNERS_KEY] ?: false }

    suspend fun setDockMatchWidgetCorners(enabled: Boolean) {
        dataStore.edit { it[DOCK_MATCH_WIDGET_CORNERS_KEY] = enabled }
    }

    /** Opt-in: quick overview opens/closes instantly by default, this switches on the slide/fade animation. */
    val quickOverviewAnimationEnabled: Flow<Boolean> = dataStore.data.map { it[QUICK_OVERVIEW_ANIMATION_ENABLED_KEY] ?: false }

    suspend fun setQuickOverviewAnimationEnabled(enabled: Boolean) {
        dataStore.edit { it[QUICK_OVERVIEW_ANIMATION_ENABLED_KEY] = enabled }
    }

    // ---- Text search ----

    val searchShowAzIndex: Flow<Boolean> = dataStore.data.map { it[SEARCH_SHOW_AZ_INDEX_KEY] ?: true }

    val searchHideStatusBar: Flow<Boolean> = dataStore.data.map { it[SEARCH_HIDE_STATUS_BAR_KEY] ?: true }

    val searchKeyboardShowNumberRow: Flow<Boolean> = dataStore.data.map { it[SEARCH_KEYBOARD_SHOW_NUMBER_ROW_KEY] ?: true }

    val searchKeyboardLayout: Flow<KeyboardLayout> = dataStore.data.map { prefs ->
        prefs[SEARCH_KEYBOARD_LAYOUT_KEY]?.let { runCatching { KeyboardLayout.valueOf(it) }.getOrNull() } ?: KeyboardLayout.QWERTY
    }

    suspend fun setSearchKeyboardLayout(layout: KeyboardLayout) {
        dataStore.edit { it[SEARCH_KEYBOARD_LAYOUT_KEY] = layout.name }
    }

    val searchKeyboardSparks: Flow<Boolean> = dataStore.data.map { it[SEARCH_KEYBOARD_SPARKS_KEY] ?: true }

    suspend fun setSearchKeyboardSparks(enabled: Boolean) {
        dataStore.edit { it[SEARCH_KEYBOARD_SPARKS_KEY] = enabled }
    }

    val searchKeyboardSparkColor: Flow<Int> = dataStore.data.map { it[SEARCH_KEYBOARD_SPARK_COLOR_KEY] ?: 0xFF34D058.toInt() }

    suspend fun setSearchKeyboardSparkColor(color: Int) {
        dataStore.edit { it[SEARCH_KEYBOARD_SPARK_COLOR_KEY] = color }
    }

    val searchKeyboardHaptics: Flow<Boolean> = dataStore.data.map { it[SEARCH_KEYBOARD_HAPTICS_KEY] ?: true }

    val searchEngine: Flow<SearchEngine> = dataStore.data.map { prefs ->
        prefs[SEARCH_ENGINE_KEY]?.let { runCatching { SearchEngine.valueOf(it) }.getOrNull() }
            ?: SearchEngine.DUCKDUCKGO
    }

    val searchEngineCustomUrl: Flow<String> = dataStore.data.map { it[SEARCH_ENGINE_CUSTOM_URL_KEY] ?: "" }

    val searchIncluded: Flow<Set<SearchCategory>> = dataStore.data.map { prefs ->
        SearchCategory.entries.filter { c ->
            prefs[searchIncludeKey(c.storageKey)] ?: c.legacyKey?.let { prefs[searchIncludeKey(it)] } ?: true
        }.toSet()
    }

    suspend fun setSearchIncluded(category: SearchCategory, include: Boolean) {
        dataStore.edit { it[searchIncludeKey(category.storageKey)] = include }
    }

    val searchCategoryOrder: Flow<List<SearchCategory>> = dataStore.data.map { prefs ->
        // Categories that were split later replace their old entry in place.
        val saved = prefs[SEARCH_CATEGORY_ORDER_KEY].orEmpty().split(',').flatMap { name ->
            when (name) {
                "DOCUMENTS" -> listOf(SearchCategory.NOTES, SearchCategory.WEBSITES)
                "APPS" -> listOf(SearchCategory.APPS, SearchCategory.FOLDERS)
                "WEB" -> listOf(SearchCategory.EMAIL, SearchCategory.WEB)
                else -> listOfNotNull(SearchCategory.entries.firstOrNull { it.name == name })
            }
        }
        (saved + SearchCategory.entries).distinct()
    }

    suspend fun setSearchCategoryOrder(order: List<SearchCategory>) {
        dataStore.edit { it[SEARCH_CATEGORY_ORDER_KEY] = order.joinToString(",") { c -> c.name } }
    }

    val searchAiApp: Flow<String?> = dataStore.data.map { it[SEARCH_AI_APP_KEY] }

    suspend fun setSearchAiApp(packageName: String?) {
        dataStore.edit { if (packageName == null) it.remove(SEARCH_AI_APP_KEY) else it[SEARCH_AI_APP_KEY] = packageName }
    }

    val searchBrowserApp: Flow<String?> = dataStore.data.map { it[SEARCH_BROWSER_APP_KEY] }

    suspend fun setSearchBrowserApp(packageName: String?) {
        dataStore.edit { if (packageName == null) it.remove(SEARCH_BROWSER_APP_KEY) else it[SEARCH_BROWSER_APP_KEY] = packageName }
    }

    val searchMapsApp: Flow<String?> = dataStore.data.map { it[SEARCH_MAPS_APP_KEY] }

    suspend fun setSearchMapsApp(packageName: String?) {
        dataStore.edit { if (packageName == null) it.remove(SEARCH_MAPS_APP_KEY) else it[SEARCH_MAPS_APP_KEY] = packageName }
    }

    val searchKeyboardType: Flow<SearchKeyboardType> = dataStore.data.map { prefs ->
        prefs[SEARCH_KEYBOARD_TYPE_KEY]?.let { runCatching { SearchKeyboardType.valueOf(it) }.getOrNull() } ?: SearchKeyboardType.CUSTOM
    }

    suspend fun setSearchKeyboardType(type: SearchKeyboardType) {
        dataStore.edit { it[SEARCH_KEYBOARD_TYPE_KEY] = type.name }
    }


    val searchIndexShowRailBar: Flow<Boolean> = dataStore.data.map { it[SEARCH_INDEX_SHOW_RAIL_BAR_KEY] ?: true }

    val searchHeaderLetterAppearance: Flow<ElementAppearance> =
        elementAppearanceFor("search_header_letter", ElementAppearance(fillMode = ElementFillMode.NONE, borderEnabled = true))

    val searchAlphabetLetterAppearance: Flow<ElementAppearance> =
        elementAppearanceFor("search_alphabet_letter", ElementAppearance(fillMode = ElementFillMode.NONE))

    // ---- App overview ----

    val appListLayout: Flow<AppListLayout> = dataStore.data.map { prefs ->
        prefs[APP_LIST_LAYOUT_KEY]?.let { runCatching { AppListLayout.valueOf(it) }.getOrNull() } ?: AppListLayout.LIST
    }

    val appListShowSearchButton: Flow<Boolean> = dataStore.data.map { it[APP_LIST_SHOW_SEARCH_BUTTON_KEY] ?: true }

    val appListDisplayMode: Flow<AppListDisplayMode> = dataStore.data.map { prefs ->
        prefs[APP_LIST_DISPLAY_MODE_KEY]?.let { runCatching { AppListDisplayMode.valueOf(it) }.getOrNull() }
            ?: AppListDisplayMode.FULLSCREEN
    }

    val appListContentAppearance: Flow<ElementAppearance> = elementAppearanceFor("app_list_content", ElementAppearance(borderEnabled = true))

    // ---- Presets, page backgrounds, sidebar ----

    val activeDesignPreset: Flow<DesignPreset?> = dataStore.data.map { prefs ->
        prefs[ACTIVE_DESIGN_PRESET_KEY]?.let { runCatching { DesignPreset.valueOf(it) }.getOrNull() }
    }

    fun pageBackgroundConfig(pageId: Long): Flow<BackgroundConfig> =
        backgroundConfigFor(pageKey(pageId), BackgroundConfig())

    fun sidebarCategoryViewMode(categoryKey: String): Flow<SidebarCategoryViewMode> = dataStore.data.map { prefs ->
        prefs[sidebarViewModeKey(categoryKey)]?.let { runCatching { SidebarCategoryViewMode.valueOf(it) }.getOrNull() }
            ?: SidebarCategoryViewMode.LIST
    }

    suspend fun setSidebarCategoryViewMode(categoryKey: String, mode: SidebarCategoryViewMode) {
        dataStore.edit { it[sidebarViewModeKey(categoryKey)] = mode.name }
    }

    fun sidebarCategoryShowAddRow(categoryKey: String): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[sidebarShowAddRowKey(categoryKey)] ?: true
    }

    suspend fun setSidebarCategoryShowAddRow(categoryKey: String, show: Boolean) {
        dataStore.edit { it[sidebarShowAddRowKey(categoryKey)] = show }
    }

    private fun sidebarViewModeKey(categoryKey: String) = stringPreferencesKey("sidebar_${categoryKey}_view_mode")
    private fun sidebarShowAddRowKey(categoryKey: String) = booleanPreferencesKey("sidebar_${categoryKey}_show_add_row")

    private fun backgroundConfigFor(key: String, default: BackgroundConfig): Flow<BackgroundConfig> = dataStore.data.map { prefs ->
        BackgroundConfig(
            source = prefs[backgroundSourceKey(key)]?.let { runCatching { BackgroundSource.valueOf(it) }.getOrNull() }
                ?: default.source,
            fill = ColorFill(
                mode = prefs[backgroundFillModeKey(key)]?.let { runCatching { FillMode.valueOf(it) }.getOrNull() }
                    ?: default.fill.mode,
                color = prefs[backgroundColorKey(key)] ?: default.fill.color,
                secondaryColor = prefs[backgroundSecondaryColorKey(key)] ?: default.fill.secondaryColor,
                direction = prefs[backgroundDirectionKey(key)]?.let { runCatching { GradientDirection.valueOf(it) }.getOrNull() }
                    ?: default.fill.direction
            ),
            imagePath = prefs[backgroundImagePathKey(key)],
            opacity = (prefs[backgroundOpacityKey(key)] ?: default.opacity).coerceIn(0f, 1f),
            darken = (prefs[backgroundDarkenKey(key)] ?: default.darken).coerceIn(0f, 1f)
        )
    }

    private fun pageKey(pageId: Long) = "page_$pageId"

    // ---- Gestures ----

    fun gestureAction(gesture: HomeGesture): Flow<GestureAction> = dataStore.data.map { prefs ->
        val type = prefs[gestureTypeKey(gesture)]?.let { runCatching { GestureActionType.valueOf(it) }.getOrNull() }
            ?: if (gesture == HomeGesture.SWIPE_UP) GestureActionType.APP_LIST else GestureActionType.NONE
        GestureAction(type, prefs[gesturePayloadKey(gesture)])
    }

    // ---- Setters: general ----

    suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[THEME_KEY] = theme.name }
    }

    suspend fun setHandedness(handedness: Handedness) {
        dataStore.edit { it[HANDEDNESS_KEY] = handedness.name }
    }

    suspend fun setActiveIconPack(packageName: String?) {
        dataStore.edit { prefs ->
            if (packageName == null) prefs.remove(ICON_PACK_KEY) else prefs[ICON_PACK_KEY] = packageName
        }
    }

    suspend fun setFontPreference(font: FontPreference) {
        dataStore.edit { it[FONT_KEY] = font.name }
    }

    suspend fun setIconShape(shape: IconShape) {
        dataStore.edit { it[ICON_SHAPE_KEY] = shape.name }
    }

    // ---- Status bar ----

    fun pageHideStatusBar(pageId: Long): Flow<Boolean> = dataStore.data.map { it[pageHideStatusBarKey(pageId)] ?: false }

    suspend fun setPageHideStatusBar(pageId: Long, hide: Boolean) {
        dataStore.edit { it[pageHideStatusBarKey(pageId)] = hide }
    }

    private fun pageHideStatusBarKey(pageId: Long) = booleanPreferencesKey("page_hide_status_bar_$pageId")

    val quickOverviewHideStatusBar: Flow<Boolean> = dataStore.data.map { it[QUICK_OVERVIEW_HIDE_STATUS_BAR_KEY] ?: true }

    suspend fun setQuickOverviewHideStatusBar(hide: Boolean) {
        dataStore.edit { it[QUICK_OVERVIEW_HIDE_STATUS_BAR_KEY] = hide }
    }

    val folderHideStatusBar: Flow<Boolean> = dataStore.data.map { it[FOLDER_HIDE_STATUS_BAR_KEY] ?: false }

    suspend fun setFolderHideStatusBar(hide: Boolean) {
        dataStore.edit { it[FOLDER_HIDE_STATUS_BAR_KEY] = hide }
    }

    /** One-shot command: hides the status bar on every screen (quick overview, search, folders, every page). */
    suspend fun hideStatusBarEverywhere(pageIds: List<Long>) {
        dataStore.edit { prefs ->
            prefs[QUICK_OVERVIEW_HIDE_STATUS_BAR_KEY] = true
            prefs[SEARCH_HIDE_STATUS_BAR_KEY] = true
            prefs[FOLDER_HIDE_STATUS_BAR_KEY] = true
            pageIds.forEach { prefs[pageHideStatusBarKey(it)] = true }
        }
    }

    // ---- Element appearance (layers 2 and 3) ----

    private fun customElementAppearanceFor(prefix: String, default: ElementAppearance): Flow<ElementAppearance?> =
        dataStore.data.map { prefs ->
            if (prefs[elementFillModeKeyEnum(prefix)] == null) null else readElementAppearance(prefs, prefix, default, null)
        }

    private suspend fun resetElementAppearanceFor(prefix: String) {
        dataStore.edit { prefs ->
            prefs.remove(elementFillModeKeyEnum(prefix))
            prefs.remove(elementFillColorKey(prefix))
            prefs.remove(elementFillSecondaryColorKey(prefix))
            prefs.remove(elementFillModeKey(prefix))
            prefs.remove(elementFillDirectionKey(prefix))
            prefs.remove(elementOpacityKey(prefix))
            prefs.remove(elementBorderKey(prefix))
            prefs.remove(elementBorderColorKey(prefix))
            prefs.remove(elementBorderWidthKey(prefix))
            prefs.remove(elementContentColorKey(prefix))
        }
    }

    val searchAlphabetRailAppearance: Flow<ElementAppearance?> = customElementAppearanceFor(RAIL_PREFIX, ElementAppearance(borderEnabled = true))

    suspend fun setSearchAlphabetRailAppearance(appearance: ElementAppearance) = setElementAppearanceFor(RAIL_PREFIX, appearance)

    suspend fun resetSearchAlphabetRailAppearance() = resetElementAppearanceFor(RAIL_PREFIX)

    val appListSearchButtonAppearance: Flow<ElementAppearance?> =
        customElementAppearanceFor("app_list_search_button", ElementAppearance(borderEnabled = true))

    suspend fun resetAppListSearchButtonAppearance() = resetElementAppearanceFor("app_list_search_button")

    suspend fun setSidebarPanelAppearance(appearance: ElementAppearance) = setElementAppearanceFor("sidebar_panel", appearance)

    suspend fun setFolderCardAppearance(appearance: ElementAppearance) = setElementAppearanceFor("folder_card", appearance)

    suspend fun setSearchOverlayBackground(config: BackgroundConfig) = setBackgroundConfigFor("search_overlay", config)

    suspend fun setQuickOverviewBackground(config: BackgroundConfig) = setBackgroundConfigFor("quick_overview", config)

    suspend fun setKeyboardAppearance(appearance: ElementAppearance) = setElementAppearanceFor("keyboard_background", appearance)

    suspend fun setSearchResultCardAppearance(appearance: ElementAppearance) = setElementAppearanceFor("search_result_card", appearance)

    private fun elementAppearanceFor(prefix: String, default: ElementAppearance, defaultFillColor: Int? = null): Flow<ElementAppearance> =
        dataStore.data.map { prefs -> readElementAppearance(prefs, prefix, default, defaultFillColor) }

    private fun readElementAppearance(
        prefs: androidx.datastore.preferences.core.Preferences,
        prefix: String,
        default: ElementAppearance,
        defaultFillColor: Int?
    ): ElementAppearance =
        ElementAppearance(
            fillMode = prefs[elementFillModeKeyEnum(prefix)]?.let { runCatching { ElementFillMode.valueOf(it) }.getOrNull() }
                ?: default.fillMode,
            fill = ColorFill(
                mode = prefs[elementFillModeKey(prefix)]?.let { runCatching { FillMode.valueOf(it) }.getOrNull() } ?: default.fill.mode,
                color = prefs[elementFillColorKey(prefix)] ?: defaultFillColor ?: resolveDefaultSurfaceColor(prefs),
                secondaryColor = prefs[elementFillSecondaryColorKey(prefix)] ?: default.fill.secondaryColor,
                direction = prefs[elementFillDirectionKey(prefix)]?.let { runCatching { GradientDirection.valueOf(it) }.getOrNull() }
                    ?: default.fill.direction
            ),
            opacity = (prefs[elementOpacityKey(prefix)] ?: default.opacity).coerceIn(0f, 1f),
            borderEnabled = prefs[elementBorderKey(prefix)] ?: default.borderEnabled,
            borderColor = prefs[elementBorderColorKey(prefix)] ?: default.borderColor,
            borderWidth = (prefs[elementBorderWidthKey(prefix)] ?: default.borderWidth).coerceIn(0.5f, 6f),
            contentColor = prefs[elementContentColorKey(prefix)] ?: default.contentColor
        )

    private fun resolveDefaultSurfaceColor(prefs: Preferences): Int {
        val theme = prefs[THEME_KEY]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() } ?: ThemePreference.SYSTEM
        val isDark = when (theme) {
            ThemePreference.DARK -> true
            ThemePreference.LIGHT -> false
            ThemePreference.SYSTEM -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        return if (isDark) DesignPresetValues.DARK_SURFACE else DesignPresetValues.LIGHT_SURFACE
    }

    private suspend fun setElementAppearanceFor(prefix: String, appearance: ElementAppearance) {
        dataStore.edit { prefs ->
            prefs[elementFillModeKeyEnum(prefix)] = appearance.fillMode.name
            prefs[elementFillColorKey(prefix)] = appearance.fill.color
            prefs[elementFillSecondaryColorKey(prefix)] = appearance.fill.secondaryColor
            prefs[elementFillModeKey(prefix)] = appearance.fill.mode.name
            prefs[elementFillDirectionKey(prefix)] = appearance.fill.direction.name
            prefs[elementOpacityKey(prefix)] = appearance.opacity.coerceIn(0f, 1f)
            prefs[elementBorderKey(prefix)] = appearance.borderEnabled
            appearance.borderColor?.let { prefs[elementBorderColorKey(prefix)] = it } ?: prefs.remove(elementBorderColorKey(prefix))
            prefs[elementBorderWidthKey(prefix)] = appearance.borderWidth.coerceIn(0.5f, 6f)
            appearance.contentColor?.let { prefs[elementContentColorKey(prefix)] = it } ?: prefs.remove(elementContentColorKey(prefix))
        }
    }

    private fun elementFillModeKeyEnum(prefix: String) = stringPreferencesKey("${prefix}_style")
    private fun elementFillColorKey(prefix: String) = intPreferencesKey("${prefix}_fill_color")
    private fun elementFillSecondaryColorKey(prefix: String) = intPreferencesKey("${prefix}_fill_color2")
    private fun elementFillModeKey(prefix: String) = stringPreferencesKey("${prefix}_fill_mode")
    private fun elementFillDirectionKey(prefix: String) = stringPreferencesKey("${prefix}_fill_direction")
    private fun elementOpacityKey(prefix: String) = floatPreferencesKey("${prefix}_opacity")
    private fun elementBorderKey(prefix: String) = booleanPreferencesKey("${prefix}_border")
    private fun elementBorderColorKey(prefix: String) = intPreferencesKey("${prefix}_border_color")
    private fun elementBorderWidthKey(prefix: String) = floatPreferencesKey("${prefix}_border_width")
    private fun elementContentColorKey(prefix: String) = intPreferencesKey("${prefix}_content_color")

    // ---- Setters: display and text search ----

    suspend fun setAccentColor(argb: Int) {
        dataStore.edit { it[ACCENT_COLOR_KEY] = argb }
    }

    suspend fun setDockSingleTapAction(action: DockTapAction) {
        dataStore.edit { it[DOCK_SINGLE_TAP_ACTION_KEY] = action.name }
    }

    suspend fun setDockDoubleTapAction(action: DockTapAction) {
        dataStore.edit { it[DOCK_DOUBLE_TAP_ACTION_KEY] = action.name }
    }

    suspend fun setFolderShowAddRow(folderId: Long, show: Boolean) {
        dataStore.edit { it[folderShowAddRowKey(folderId)] = show }
    }

    /** One-shot command: turns the "Add…" row off for every folder that exists right now. */
    suspend fun hideFolderAddRowEverywhere(folderIds: List<Long>) {
        dataStore.edit { prefs -> folderIds.forEach { prefs[folderShowAddRowKey(it)] = false } }
    }

    suspend fun setChargingAnimationEnabled(enabled: Boolean) {
        dataStore.edit { it[CHARGING_ANIMATION_ENABLED_KEY] = enabled }
    }

    suspend fun setSearchShowAzIndex(show: Boolean) {
        dataStore.edit { it[SEARCH_SHOW_AZ_INDEX_KEY] = show }
    }

    suspend fun setSearchHideStatusBar(hide: Boolean) {
        dataStore.edit { it[SEARCH_HIDE_STATUS_BAR_KEY] = hide }
    }

    suspend fun setSearchKeyboardHaptics(enabled: Boolean) {
        dataStore.edit { it[SEARCH_KEYBOARD_HAPTICS_KEY] = enabled }
    }

    suspend fun setSearchKeyboardShowNumberRow(show: Boolean) {
        dataStore.edit { it[SEARCH_KEYBOARD_SHOW_NUMBER_ROW_KEY] = show }
    }

    suspend fun setSearchEngine(engine: SearchEngine) {
        dataStore.edit { it[SEARCH_ENGINE_KEY] = engine.name }
    }

    suspend fun setSearchEngineCustomUrl(url: String) {
        dataStore.edit { it[SEARCH_ENGINE_CUSTOM_URL_KEY] = url }
    }

    suspend fun setSearchIndexShowRailBar(show: Boolean) {
        dataStore.edit { it[SEARCH_INDEX_SHOW_RAIL_BAR_KEY] = show }
    }

    suspend fun setSearchHeaderLetterAppearance(appearance: ElementAppearance) = setElementAppearanceFor("search_header_letter", appearance)

    suspend fun setSearchAlphabetLetterAppearance(appearance: ElementAppearance) = setElementAppearanceFor("search_alphabet_letter", appearance)

    suspend fun setAppListSearchButtonAppearance(appearance: ElementAppearance) =
        setElementAppearanceFor("app_list_search_button", appearance)

    suspend fun setAppListShowSearchButton(show: Boolean) {
        dataStore.edit { it[APP_LIST_SHOW_SEARCH_BUTTON_KEY] = show }
    }

    suspend fun setAppListLayout(layout: AppListLayout) {
        dataStore.edit { it[APP_LIST_LAYOUT_KEY] = layout.name }
    }

    suspend fun setAppListDisplayMode(mode: AppListDisplayMode) {
        dataStore.edit { it[APP_LIST_DISPLAY_MODE_KEY] = mode.name }
    }

    suspend fun setAppListContentAppearance(appearance: ElementAppearance) = setElementAppearanceFor("app_list_content", appearance)

    // ---- Setters: presets, backgrounds, gestures ----

    suspend fun setActiveDesignPreset(preset: DesignPreset?) {
        dataStore.edit { prefs ->
            if (preset == null) prefs.remove(ACTIVE_DESIGN_PRESET_KEY) else prefs[ACTIVE_DESIGN_PRESET_KEY] = preset.name
        }
    }

    suspend fun setPageBackgroundConfig(pageId: Long, config: BackgroundConfig) =
        setBackgroundConfigFor(pageKey(pageId), config)

    suspend fun clearPageBackgroundConfig(pageId: Long) {
        val key = pageKey(pageId)
        dataStore.edit { prefs ->
            prefs.remove(backgroundSourceKey(key))
            prefs.remove(backgroundColorKey(key))
            prefs.remove(backgroundSecondaryColorKey(key))
            prefs.remove(backgroundFillModeKey(key))
            prefs.remove(backgroundDirectionKey(key))
            prefs.remove(backgroundImagePathKey(key))
            prefs.remove(backgroundOpacityKey(key))
            prefs.remove(backgroundDarkenKey(key))
        }
    }

    private suspend fun setBackgroundConfigFor(key: String, config: BackgroundConfig) {
        dataStore.edit { prefs ->
            prefs[backgroundSourceKey(key)] = config.source.name
            prefs[backgroundColorKey(key)] = config.fill.color
            prefs[backgroundSecondaryColorKey(key)] = config.fill.secondaryColor
            prefs[backgroundFillModeKey(key)] = config.fill.mode.name
            prefs[backgroundDirectionKey(key)] = config.fill.direction.name
            if (config.imagePath != null) prefs[backgroundImagePathKey(key)] = config.imagePath
            else prefs.remove(backgroundImagePathKey(key))
            prefs[backgroundOpacityKey(key)] = config.opacity.coerceIn(0f, 1f)
            prefs[backgroundDarkenKey(key)] = config.darken.coerceIn(0f, 1f)
        }
    }

    private fun backgroundSourceKey(key: String) = stringPreferencesKey("bg_${key}_source")
    private fun backgroundColorKey(key: String) = intPreferencesKey("bg_${key}_color")
    private fun backgroundSecondaryColorKey(key: String) = intPreferencesKey("bg_${key}_color2")
    private fun backgroundFillModeKey(key: String) = stringPreferencesKey("bg_${key}_fill_mode")
    private fun backgroundDirectionKey(key: String) = stringPreferencesKey("bg_${key}_direction")
    private fun backgroundImagePathKey(key: String) = stringPreferencesKey("bg_${key}_image_path")
    private fun backgroundOpacityKey(key: String) = floatPreferencesKey("bg_${key}_opacity")
    private fun backgroundDarkenKey(key: String) = floatPreferencesKey("bg_${key}_darken")

    suspend fun setGestureAction(gesture: HomeGesture, action: GestureAction) {
        dataStore.edit { prefs ->
            prefs[gestureTypeKey(gesture)] = action.type.name
            if (action.payload != null) prefs[gesturePayloadKey(gesture)] = action.payload
            else prefs.remove(gesturePayloadKey(gesture))
        }
    }

    private fun gestureTypeKey(gesture: HomeGesture) = stringPreferencesKey("gesture_${gesture.name}_type")
    private fun gesturePayloadKey(gesture: HomeGesture) = stringPreferencesKey("gesture_${gesture.name}_payload")

    companion object {
        private val DEFAULT_ACCENT_COLOR = 0xFF3B82F6.toInt()

        private val THEME_KEY = stringPreferencesKey("theme")
        private val HANDEDNESS_KEY = stringPreferencesKey("handedness")
        private val ICON_PACK_KEY = stringPreferencesKey("active_icon_pack")
        private val FONT_KEY = stringPreferencesKey("font_preference")
        private val ICON_SHAPE_KEY = stringPreferencesKey("icon_shape")
        private val QUICK_OVERVIEW_HIDE_STATUS_BAR_KEY = booleanPreferencesKey("quick_overview_hide_status_bar")
        private val FOLDER_HIDE_STATUS_BAR_KEY = booleanPreferencesKey("folder_hide_status_bar")
        private val ACCENT_COLOR_KEY = intPreferencesKey("accent_color")
        private val DOCK_SINGLE_TAP_ACTION_KEY = stringPreferencesKey("dock_single_tap_action")
        private val DOCK_DOUBLE_TAP_ACTION_KEY = stringPreferencesKey("dock_double_tap_action")
        private val CHARGING_ANIMATION_COLOR_KEY = intPreferencesKey("charging_animation_color")
        private val DOCK_MATCH_WIDGET_CORNERS_KEY = booleanPreferencesKey("dock_match_widget_corners")
        private val QUICK_OVERVIEW_ANIMATION_ENABLED_KEY = booleanPreferencesKey("quick_overview_animation_enabled_v2")
        private val CHARGING_ANIMATION_ENABLED_KEY = booleanPreferencesKey("charging_animation_enabled")
        private val SEARCH_SHOW_AZ_INDEX_KEY = booleanPreferencesKey("search_show_az_index")
        private val SEARCH_HIDE_STATUS_BAR_KEY = booleanPreferencesKey("search_hide_status_bar")
        private val SEARCH_ENGINE_KEY = stringPreferencesKey("search_engine")
        private val SEARCH_ENGINE_CUSTOM_URL_KEY = stringPreferencesKey("search_engine_custom_url")
        private val SEARCH_KEYBOARD_SPARKS_KEY = booleanPreferencesKey("search_keyboard_sparks")
        private val SEARCH_KEYBOARD_SPARK_COLOR_KEY = intPreferencesKey("search_keyboard_spark_color")
        private val SEARCH_KEYBOARD_LAYOUT_KEY = stringPreferencesKey("search_keyboard_layout")
        private val SEARCH_KEYBOARD_TYPE_KEY = stringPreferencesKey("search_keyboard_type")
        private val SEARCH_CATEGORY_ORDER_KEY = stringPreferencesKey("search_category_order")
        private val SEARCH_AI_APP_KEY = stringPreferencesKey("search_ai_app")
        private val SEARCH_BROWSER_APP_KEY = stringPreferencesKey("search_browser_app")
        private val SEARCH_MAPS_APP_KEY = stringPreferencesKey("search_maps_app")
        private fun searchIncludeKey(storageKey: String) = booleanPreferencesKey("search_include_$storageKey")
        private val SEARCH_KEYBOARD_SHOW_NUMBER_ROW_KEY = booleanPreferencesKey("search_keyboard_show_number_row")
        private val SEARCH_KEYBOARD_HAPTICS_KEY = booleanPreferencesKey("search_keyboard_haptics")
        private val SEARCH_INDEX_SHOW_RAIL_BAR_KEY = booleanPreferencesKey("search_index_show_rail_bar")
        private const val RAIL_PREFIX = "search_alphabet_rail"
        private val APP_LIST_DISPLAY_MODE_KEY = stringPreferencesKey("app_list_display_mode")
        private val APP_LIST_LAYOUT_KEY = stringPreferencesKey("app_list_layout")
        private val APP_LIST_SHOW_SEARCH_BUTTON_KEY = booleanPreferencesKey("app_list_show_search_button")
        private val ACTIVE_DESIGN_PRESET_KEY = stringPreferencesKey("active_design_preset")
    }
}
