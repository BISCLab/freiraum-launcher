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
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.FillMode
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.GradientDirection
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.data.model.SearchEngine
import com.bisc.flauncher.data.model.SidebarCategoryViewMode
import com.bisc.flauncher.ui.theme.FontPreference
import com.bisc.flauncher.ui.theme.ThemePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    val quickOverviewBackgroundOverride: Flow<BackgroundConfig?> = backgroundOverrideFor("quick_overview")

    val keyboardAppearance: Flow<ElementAppearance> = elementAppearanceFor("keyboard_background", ElementAppearance(borderEnabled = true))

    /** The text search screen's single, full-bleed appearance — covers both the backdrop and the
     *  results, which now float directly on it instead of sitting in a separate card (see the search
     *  redesign: the screen has no more layered boxes, just one themed fill behind everything). */
    val searchAppearance: Flow<ElementAppearance> = elementAppearanceFor("search_appearance", ElementAppearance())

    val accentColor: Flow<Int> = dataStore.data.map { prefs ->
        prefs[ACCENT_COLOR_KEY] ?: DEFAULT_ACCENT_COLOR
    }

    // Off by default — the Apps panel has no border anywhere by design (flush/seamless on the top/left/
    // right edges that are always square); this is the one optional exception, a thin line along just the
    // bottom edge where the panel meets the dimmed home screen.
    val favoritesBottomBorderEnabled: Flow<Boolean> = dataStore.data.map { it[FAVORITES_BOTTOM_BORDER_ENABLED_KEY] ?: false }

    suspend fun setFavoritesBottomBorderEnabled(enabled: Boolean) {
        dataStore.edit { it[FAVORITES_BOTTOM_BORDER_ENABLED_KEY] = enabled }
    }

    // Off by default — a thin horizontal divider between the apps/folders/shortcuts groups in the Apps
    // panel's grid, only drawn between groups that actually have at least one entry.
    val favoritesGroupDividersEnabled: Flow<Boolean> = dataStore.data.map { it[FAVORITES_GROUP_DIVIDERS_ENABLED_KEY] ?: false }

    suspend fun setFavoritesGroupDividersEnabled(enabled: Boolean) {
        dataStore.edit { it[FAVORITES_GROUP_DIVIDERS_ENABLED_KEY] = enabled }
    }

    /** How rounded the dock bar's own corners are — replaces the old orb-era "match widget corners" toggle. */
    val dockCornerStyle: Flow<com.bisc.flauncher.ui.home.dock.DockCornerStyle> = dataStore.data.map { prefs ->
        prefs[DOCK_CORNER_STYLE_KEY]?.let { runCatching { com.bisc.flauncher.ui.home.dock.DockCornerStyle.valueOf(it) }.getOrNull() }
            ?: com.bisc.flauncher.ui.home.dock.DockCornerStyle.ROUNDED
    }

    suspend fun setDockCornerStyle(style: com.bisc.flauncher.ui.home.dock.DockCornerStyle) {
        dataStore.edit { it[DOCK_CORNER_STYLE_KEY] = style.name }
    }

    /** Whether the dock bar is pinned to a fixed 5-slot width or grows with however many slots are filled. */
    val dockWidthMode: Flow<com.bisc.flauncher.ui.home.dock.DockWidthMode> = dataStore.data.map { prefs ->
        prefs[DOCK_WIDTH_MODE_KEY]?.let { runCatching { com.bisc.flauncher.ui.home.dock.DockWidthMode.valueOf(it) }.getOrNull() }
            ?: com.bisc.flauncher.ui.home.dock.DockWidthMode.FIXED
    }

    suspend fun setDockWidthMode(mode: com.bisc.flauncher.ui.home.dock.DockWidthMode) {
        dataStore.edit { it[DOCK_WIDTH_MODE_KEY] = mode.name }
    }

    fun folderShowAddRow(folderId: Long): Flow<Boolean> = dataStore.data.map { it[folderShowAddRowKey(folderId)] ?: true }

    private fun folderShowAddRowKey(folderId: Long) = booleanPreferencesKey("folder_show_add_row_$folderId")

    // Off by default — the home screen starts empty (just the dock) and stays that way until the user
    // deliberately opts into widgets there, matching Kvaesitso's "enable widgets on home screen" switch.
    // Other pages are unaffected; this only gates what renders on the page with isHome == true.
    val homeScreenWidgetsEnabled: Flow<Boolean> = dataStore.data.map { it[HOME_SCREEN_WIDGETS_ENABLED_KEY] ?: false }

    suspend fun setHomeScreenWidgetsEnabled(enabled: Boolean) {
        dataStore.edit { it[HOME_SCREEN_WIDGETS_ENABLED_KEY] = enabled }
    }

    val chargingAnimationColor: Flow<Int> = dataStore.data.map { it[CHARGING_ANIMATION_COLOR_KEY] ?: 0xFF34D058.toInt() }

    suspend fun setChargingAnimationColor(color: Int) {
        dataStore.edit { it[CHARGING_ANIMATION_COLOR_KEY] = color }
    }

    val chargingAnimationEnabled: Flow<Boolean> = dataStore.data.map { it[CHARGING_ANIMATION_ENABLED_KEY] ?: true }


    // ---- Text search ----

    val searchShowAzIndex: Flow<Boolean> = dataStore.data.map { it[SEARCH_SHOW_AZ_INDEX_KEY] ?: true }

    val searchHideStatusBar: Flow<Boolean> = dataStore.data.map { it[SEARCH_HIDE_STATUS_BAR_KEY] ?: true }

    /** Whether the system keyboard shows itself immediately when search opens, vs. staying
     *  unfocused until the user taps the input field. */
    val searchKeyboardAutoOpen: Flow<Boolean> = dataStore.data.map { it[SEARCH_KEYBOARD_AUTO_OPEN_KEY] ?: true }

    suspend fun setSearchKeyboardAutoOpen(enabled: Boolean) {
        dataStore.edit { it[SEARCH_KEYBOARD_AUTO_OPEN_KEY] = enabled }
    }

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

    /** Per-category icon color override, used everywhere a [SearchCategory]'s icon appears (the
     *  search filter row, each result card's header, the category reorder list in settings) — null
     *  means "use [com.bisc.flauncher.ui.home.search.defaultColor]". */
    fun categoryColor(category: SearchCategory): Flow<Int?> = dataStore.data.map { it[categoryColorKey(category)] }

    suspend fun setCategoryColor(category: SearchCategory, color: Int) {
        dataStore.edit { it[categoryColorKey(category)] = color }
    }

    private fun categoryColorKey(category: SearchCategory) = intPreferencesKey("category_color_${category.storageKey}")

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

    // Unlike the other Layer-2 elements above, this one paints as a full-screen fill (not a bounded
    // card) whenever the app list is in fullscreen mode — fullscreen mode is itself the app's own
    // default, so an opaque default here silently hid every Layer-1 background/image behind it.
    val appListContentAppearance: Flow<ElementAppearance> =
        elementAppearanceFor("app_list_content", ElementAppearance(borderEnabled = true, opacity = 0f))

    // ---- Presets, page backgrounds, sidebar ----

    val activeDesignPreset: Flow<DesignPreset?> = dataStore.data.map { prefs ->
        prefs[ACTIVE_DESIGN_PRESET_KEY]?.let { runCatching { DesignPreset.valueOf(it) }.getOrNull() }
    }

    fun pageBackgroundOverride(pageId: Long): Flow<BackgroundConfig?> = backgroundOverrideFor(pageKey(pageId))

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

    private fun readBackgroundConfig(prefs: Preferences, key: String, default: BackgroundConfig): BackgroundConfig = BackgroundConfig(
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

    private fun backgroundConfigFor(key: String, default: BackgroundConfig): Flow<BackgroundConfig> = dataStore.data.map { prefs ->
        readBackgroundConfig(prefs, key, default)
    }

    // Null exactly when this surface has never been given its own explicit background — the surface
    // then falls back to the single global background instead. Same "null means untouched, fall back to
    // the shared default" shape already used for dockSearchButtonAppearance etc., just applied to layer 1
    // now that per-surface *images* went away in favor of one global image with only a per-surface color
    // override remaining (a color-to-color or color-to-(cached, already GPU-resident) global-image
    // crossfade never showed the flicker a custom-image-to-custom-image one did).
    private fun backgroundOverrideFor(key: String): Flow<BackgroundConfig?> = dataStore.data.map { prefs ->
        if (prefs[backgroundSourceKey(key)] == null) null else readBackgroundConfig(prefs, key, BackgroundConfig())
    }

    private suspend fun clearBackgroundConfigFor(key: String) {
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

    private fun pageKey(pageId: Long) = "page_$pageId"

    // ---- Layer 1: one global background shared by every surface (pages, app overview, both search
    // surfaces), with an optional per-surface color-only override. See backgroundOverrideFor above for
    // why images are global-only now.

    val globalBackground: Flow<BackgroundConfig> = backgroundConfigFor(GLOBAL_BACKGROUND_KEY, BackgroundConfig())

    suspend fun setGlobalBackground(config: BackgroundConfig) = setBackgroundConfigFor(GLOBAL_BACKGROUND_KEY, config)

    suspend fun hasGlobalBackground(): Boolean = dataStore.data.map { it[backgroundSourceKey(GLOBAL_BACKGROUND_KEY)] != null }.first()

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

    /** Whether opening a folder also hides the floating dock, like it can hide the status bar. Off by default. */
    val folderHideDock: Flow<Boolean> = dataStore.data.map { it[FOLDER_HIDE_DOCK_KEY] ?: false }

    suspend fun setFolderHideDock(hide: Boolean) {
        dataStore.edit { it[FOLDER_HIDE_DOCK_KEY] = hide }
    }

    /** One-shot command: hides the status bar on every screen (quick overview, search, folders, every page). */
    suspend fun setStatusBarHiddenEverywhere(pageIds: List<Long>, hidden: Boolean) {
        dataStore.edit { prefs ->
            prefs[QUICK_OVERVIEW_HIDE_STATUS_BAR_KEY] = hidden
            prefs[SEARCH_HIDE_STATUS_BAR_KEY] = hidden
            prefs[FOLDER_HIDE_STATUS_BAR_KEY] = hidden
            pageIds.forEach { prefs[pageHideStatusBarKey(it)] = hidden }
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

    suspend fun setQuickOverviewBackground(config: BackgroundConfig) = setBackgroundConfigFor("quick_overview", config)

    suspend fun clearQuickOverviewBackground() = clearBackgroundConfigFor("quick_overview")

    suspend fun setKeyboardAppearance(appearance: ElementAppearance) = setElementAppearanceFor("keyboard_background", appearance)

    suspend fun setSearchAppearance(appearance: ElementAppearance) = setElementAppearanceFor("search_appearance", appearance)

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

    suspend fun clearPageBackgroundConfig(pageId: Long) = clearBackgroundConfigFor(pageKey(pageId))

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
        private const val GLOBAL_BACKGROUND_KEY = "global"
        private val DEFAULT_ACCENT_COLOR = 0xFF3B82F6.toInt()

        private val THEME_KEY = stringPreferencesKey("theme")
        private val HANDEDNESS_KEY = stringPreferencesKey("handedness")
        private val ICON_PACK_KEY = stringPreferencesKey("active_icon_pack")
        private val FONT_KEY = stringPreferencesKey("font_preference")
        private val ICON_SHAPE_KEY = stringPreferencesKey("icon_shape")
        private val QUICK_OVERVIEW_HIDE_STATUS_BAR_KEY = booleanPreferencesKey("quick_overview_hide_status_bar")
        private val FOLDER_HIDE_STATUS_BAR_KEY = booleanPreferencesKey("folder_hide_status_bar")
        private val FOLDER_HIDE_DOCK_KEY = booleanPreferencesKey("folder_hide_dock")
        private val ACCENT_COLOR_KEY = intPreferencesKey("accent_color")
        private val CHARGING_ANIMATION_COLOR_KEY = intPreferencesKey("charging_animation_color")
        private val DOCK_CORNER_STYLE_KEY = stringPreferencesKey("dock_corner_style")
        private val DOCK_WIDTH_MODE_KEY = stringPreferencesKey("dock_width_mode")
        private val CHARGING_ANIMATION_ENABLED_KEY = booleanPreferencesKey("charging_animation_enabled")
        private val SEARCH_SHOW_AZ_INDEX_KEY = booleanPreferencesKey("search_show_az_index")
        private val HOME_SCREEN_WIDGETS_ENABLED_KEY = booleanPreferencesKey("home_screen_widgets_enabled")
        private val FAVORITES_BOTTOM_BORDER_ENABLED_KEY = booleanPreferencesKey("favorites_bottom_border_enabled")
        private val FAVORITES_GROUP_DIVIDERS_ENABLED_KEY = booleanPreferencesKey("favorites_group_dividers_enabled")
        private val SEARCH_HIDE_STATUS_BAR_KEY = booleanPreferencesKey("search_hide_status_bar")
        private val SEARCH_ENGINE_KEY = stringPreferencesKey("search_engine")
        private val SEARCH_ENGINE_CUSTOM_URL_KEY = stringPreferencesKey("search_engine_custom_url")
        private val SEARCH_CATEGORY_ORDER_KEY = stringPreferencesKey("search_category_order")
        private val SEARCH_AI_APP_KEY = stringPreferencesKey("search_ai_app")
        private val SEARCH_BROWSER_APP_KEY = stringPreferencesKey("search_browser_app")
        private val SEARCH_MAPS_APP_KEY = stringPreferencesKey("search_maps_app")
        private fun searchIncludeKey(storageKey: String) = booleanPreferencesKey("search_include_$storageKey")
        private val SEARCH_KEYBOARD_AUTO_OPEN_KEY = booleanPreferencesKey("search_keyboard_auto_open")
        private val SEARCH_INDEX_SHOW_RAIL_BAR_KEY = booleanPreferencesKey("search_index_show_rail_bar")
        private const val RAIL_PREFIX = "search_alphabet_rail"
        private val APP_LIST_DISPLAY_MODE_KEY = stringPreferencesKey("app_list_display_mode")
        private val APP_LIST_LAYOUT_KEY = stringPreferencesKey("app_list_layout")
        private val APP_LIST_SHOW_SEARCH_BUTTON_KEY = booleanPreferencesKey("app_list_show_search_button")
        private val ACTIVE_DESIGN_PRESET_KEY = stringPreferencesKey("active_design_preset")
    }
}
