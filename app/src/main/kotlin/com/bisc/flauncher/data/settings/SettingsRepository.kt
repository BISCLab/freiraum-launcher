package com.bisc.flauncher.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.bisc.flauncher.data.model.AppGridBarPosition
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.BackgroundSource
import com.bisc.flauncher.data.model.ColorFill
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.FillMode
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.GradientDirection
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.data.model.SearchEngine
import com.bisc.flauncher.ui.theme.FontPreference
import com.bisc.flauncher.ui.theme.ThemePreference
import com.bisc.flauncher.ui.theme.defaultBoxFillColorFor
import com.bisc.flauncher.ui.theme.isDark
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Which edge a left/right-dependent layout favors - currently only the app overview's own
 *  alphabet rail and floating search button (see [SettingsRepository.appOverviewHandedness]). */
enum class Handedness { LEFT, RIGHT }

/** Shape every app icon is masked to - both the dock/search-button containers and the actual
 *  icon bitmaps rendered everywhere else (see [com.bisc.flauncher.util.AppIconLoader]). */
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

    val activeIconPack: Flow<String?> = dataStore.data.map { prefs -> prefs[ICON_PACK_KEY] }

    val fontPreference: Flow<FontPreference> = dataStore.data.map { prefs ->
        prefs[FONT_KEY]?.let { runCatching { FontPreference.valueOf(it) }.getOrNull() }
            ?: FontPreference.SYSTEM
    }

    val iconShape: Flow<IconShape> = dataStore.data.map { prefs ->
        prefs[ICON_SHAPE_KEY]?.let { runCatching { IconShape.valueOf(it) }.getOrNull() }
            ?: IconShape.ROUNDED_SQUARE
    }

    // 0..200, the icon's actual size as a percent of its normal size - 100 is neutral/default,
    // below shrinks, above grows; each app's own override screen can still adjust its own icon
    // individually on the same scale (see AppOverride.iconSizePercent).
    val iconSizePercent: Flow<Int> = dataStore.data.map { prefs -> prefs[ICON_SIZE_PERCENT_KEY] ?: 100 }

    // New elements (rows, folders) are created with this value; changing it also immediately
    // re-stamps every existing row/folder item to match (see PageContentRepository/FolderRepository
    // .applyCornerRadiusToAll, called from SettingsViewModel.setDefaultCornerRadiusPercent) - each
    // element's own Appearance screen can still override it individually afterwards.
    val defaultCornerRadiusPercent: Flow<Int> = dataStore.data.map { prefs -> prefs[DEFAULT_CORNER_RADIUS_PERCENT_KEY] ?: com.bisc.flauncher.ui.theme.DEFAULT_CORNER_RADIUS_PERCENT }

    val folderCardAppearance: Flow<ElementAppearance> = elementAppearanceFor("folder_card", ElementAppearance(borderEnabled = true))

    /** The text search screen's single, full-bleed appearance - covers both the backdrop and the
     *  results, which now float directly on it instead of sitting in a separate card (see the search
     *  redesign: the screen has no more layered boxes, just one themed fill behind everything). */
    val searchAppearance: Flow<ElementAppearance> = elementAppearanceFor("search_appearance", ElementAppearance())

    /** The global accent color (Design). The app list's scroll bubble and the charging animation
     *  follow it unless they have their own color set (null below = follow this one). */
    val accentColor: Flow<Int> = dataStore.data.map { prefs ->
        prefs[ACCENT_COLOR_KEY] ?: DEFAULT_ACCENT_COLOR
    }

    val chargingAnimationColor: Flow<Int?> = dataStore.data.map { it[CHARGING_ANIMATION_COLOR_KEY] }

    suspend fun setChargingAnimationColor(color: Int?) {
        dataStore.edit { if (color == null) it.remove(CHARGING_ANIMATION_COLOR_KEY) else it[CHARGING_ANIMATION_COLOR_KEY] = color }
    }

    val chargingAnimationEnabled: Flow<Boolean> = dataStore.data.map { it[CHARGING_ANIMATION_ENABLED_KEY] ?: true }


    // ---- Text search ----

    val appListShowAzIndex: Flow<Boolean> = dataStore.data.map { it[SEARCH_SHOW_AZ_INDEX_KEY] ?: true }

    val searchHideStatusBar: Flow<Boolean> = dataStore.data.map { it[SEARCH_HIDE_STATUS_BAR_KEY] ?: true }

    /** Whether settings show their "?" tip buttons. Off by default. */
    val showSettingsTips: Flow<Boolean> = dataStore.data.map { it[SHOW_SETTINGS_TIPS_KEY] ?: false }

    suspend fun setShowSettingsTips(show: Boolean) {
        dataStore.edit { it[SHOW_SETTINGS_TIPS_KEY] = show }
    }

    /** Apps whose playback the music widget shows (package names); empty = every app. */
    val musicSources: Flow<Set<String>> = dataStore.data.map { it[MUSIC_SOURCES_KEY] ?: emptySet() }

    suspend fun setMusicSources(packages: Set<String>) {
        dataStore.edit { it[MUSIC_SOURCES_KEY] = packages }
    }

    /** Whether the music widget's large cover gets rounded corners (square by default). */
    val musicCoverRounded: Flow<Boolean> = dataStore.data.map { it[MUSIC_COVER_ROUNDED_KEY] ?: false }

    suspend fun setMusicCoverRounded(rounded: Boolean) {
        dataStore.edit { it[MUSIC_COVER_ROUNDED_KEY] = rounded }
    }

    /** Whether the page carousel wraps around: swiping past the last page lands on the first. */
    val carouselEndless: Flow<Boolean> = dataStore.data.map { it[CAROUSEL_ENDLESS_KEY] ?: false }

    suspend fun setCarouselEndless(endless: Boolean) {
        dataStore.edit { it[CAROUSEL_ENDLESS_KEY] = endless }
    }

    /** Whether the system keyboard shows itself immediately when search opens, vs. staying
     *  unfocused until the user taps the input field. */
    val searchKeyboardAutoOpen: Flow<Boolean> = dataStore.data.map { it[SEARCH_KEYBOARD_AUTO_OPEN_KEY] ?: true }

    suspend fun setSearchKeyboardAutoOpen(enabled: Boolean) {
        dataStore.edit { it[SEARCH_KEYBOARD_AUTO_OPEN_KEY] = enabled }
    }

    /** The search screen's top input bar (field + category row) - its own fill/border, separate
     *  from [searchAppearance]; null until customized, so the bar keeps following the screen's look. */
    val searchBarAppearance: Flow<ElementAppearance?> = customElementAppearanceFor(SEARCH_BAR_PREFIX, ElementAppearance())

    suspend fun setSearchBarAppearance(appearance: ElementAppearance) = setElementAppearanceFor(SEARCH_BAR_PREFIX, appearance)

    suspend fun resetSearchBarAppearance() = resetElementAppearanceFor(SEARCH_BAR_PREFIX)

    /** Null = follow the global default corner roundness. */
    val searchBarCornerRadiusPercent: Flow<Int?> = dataStore.data.map { it[SEARCH_BAR_CORNER_RADIUS_KEY] }

    suspend fun setSearchBarCornerRadiusPercent(percent: Int) {
        dataStore.edit { it[SEARCH_BAR_CORNER_RADIUS_KEY] = percent }
    }

    /** The search result cards - same split as [searchBarAppearance]: null until customized. */
    val searchResultsAppearance: Flow<ElementAppearance?> = customElementAppearanceFor(SEARCH_RESULTS_PREFIX, ElementAppearance())

    suspend fun setSearchResultsAppearance(appearance: ElementAppearance) = setElementAppearanceFor(SEARCH_RESULTS_PREFIX, appearance)

    suspend fun resetSearchResultsAppearance() = resetElementAppearanceFor(SEARCH_RESULTS_PREFIX)

    /** Off by default: the cards keep the same fine side margin as every other element. */
    /** Whether the search bar shows its thin line above the input row. */
    val searchBarTopLine: Flow<Boolean> = dataStore.data.map { it[SEARCH_BAR_TOP_LINE_KEY] ?: true }

    suspend fun setSearchBarTopLine(show: Boolean) {
        dataStore.edit { it[SEARCH_BAR_TOP_LINE_KEY] = show }
    }

    /** Whether the search bar shows its thin line between the input row and the categories. */
    val searchBarMiddleLine: Flow<Boolean> = dataStore.data.map { it[SEARCH_BAR_MIDDLE_LINE_KEY] ?: true }

    suspend fun setSearchBarMiddleLine(show: Boolean) {
        dataStore.edit { it[SEARCH_BAR_MIDDLE_LINE_KEY] = show }
    }

    val searchResultsEdgeToEdge: Flow<Boolean> = dataStore.data.map { it[SEARCH_RESULTS_EDGE_TO_EDGE_KEY] ?: false }

    suspend fun setSearchResultsEdgeToEdge(edgeToEdge: Boolean) {
        dataStore.edit { it[SEARCH_RESULTS_EDGE_TO_EDGE_KEY] = edgeToEdge }
    }

    /** Null = fully rounded, the cards' original look. */
    val searchResultsCornerRadiusPercent: Flow<Int?> = dataStore.data.map { it[SEARCH_RESULTS_CORNER_RADIUS_KEY] }

    suspend fun setSearchResultsCornerRadiusPercent(percent: Int) {
        dataStore.edit { it[SEARCH_RESULTS_CORNER_RADIUS_KEY] = percent }
    }

    /** Keyboard extension: a slim rounded strip drawn right above the system keyboard, colored to
     *  match it, so the keyboard appears to have rounded top corners. Off by default. */
    val searchKeyboardCapEnabled: Flow<Boolean> = dataStore.data.map { it[SEARCH_KEYBOARD_CAP_ENABLED_KEY] ?: false }

    val searchKeyboardCapColor: Flow<Int> = dataStore.data.map { it[SEARCH_KEYBOARD_CAP_COLOR_KEY] ?: DEFAULT_KEYBOARD_CAP_COLOR }

    val searchKeyboardCapCornerRadiusPercent: Flow<Int> = dataStore.data.map { it[SEARCH_KEYBOARD_CAP_CORNER_RADIUS_KEY] ?: 100 }

    suspend fun setSearchKeyboardCapEnabled(enabled: Boolean) {
        dataStore.edit { it[SEARCH_KEYBOARD_CAP_ENABLED_KEY] = enabled }
    }

    suspend fun setSearchKeyboardCapColor(color: Int) {
        dataStore.edit { it[SEARCH_KEYBOARD_CAP_COLOR_KEY] = color }
    }

    suspend fun setSearchKeyboardCapCornerRadiusPercent(percent: Int) {
        dataStore.edit { it[SEARCH_KEYBOARD_CAP_CORNER_RADIUS_KEY] = percent }
    }

    /** A small arrow in the keyboard extension strip; tapping the strip then closes the keyboard. */
    val searchKeyboardCapArrow: Flow<Boolean> = dataStore.data.map { it[SEARCH_KEYBOARD_CAP_ARROW_KEY] ?: false }

    suspend fun setSearchKeyboardCapArrow(enabled: Boolean) {
        dataStore.edit { it[SEARCH_KEYBOARD_CAP_ARROW_KEY] = enabled }
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

    /** Categories removed entirely: not searched and not shown in the search bar. */
    val searchRemoved: Flow<Set<SearchCategory>> = dataStore.data.map { prefs ->
        prefs[SEARCH_REMOVED_KEY].orEmpty().split(',').mapNotNull { name -> SearchCategory.entries.firstOrNull { it.name == name } }.toSet()
    }

    suspend fun setSearchRemoved(category: SearchCategory, removed: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[SEARCH_REMOVED_KEY].orEmpty().split(',').filter { it.isNotBlank() }.toSet()
            prefs[SEARCH_REMOVED_KEY] = (if (removed) current + category.name else current - category.name).joinToString(",")
        }
    }

    val searchCategoryOrder: Flow<List<SearchCategory>> = dataStore.data.map { prefs ->
        // Categories that were split later replace their old entry in place.
        val names = prefs[SEARCH_CATEGORY_ORDER_KEY].orEmpty().split(',')
        val saved = names.flatMap { name ->
            when (name) {
                "DOCUMENTS" -> listOf(SearchCategory.WEBSITES)
                // Only for orders saved before folders had their own entry.
                "APPS" -> if ("FOLDERS" in names) listOf(SearchCategory.APPS) else listOf(SearchCategory.APPS, SearchCategory.FOLDERS)
                // The e-mail command's slot went to the launcher's own settings.
                "EMAIL" -> listOf(SearchCategory.LAUNCHER_SETTINGS)
                else -> listOfNotNull(SearchCategory.entries.firstOrNull { it.name == name })
            }
        }
        (saved + SearchCategory.entries).distinct()
    }

    suspend fun setSearchCategoryOrder(order: List<SearchCategory>) {
        dataStore.edit { it[SEARCH_CATEGORY_ORDER_KEY] = order.joinToString(",") { c -> c.name } }
    }

    /** Per-category icon color override, used everywhere a [SearchCategory]'s icon appears (the
     *  search filter row, each result card's header, the category reorder list in settings) - null
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

    /** Which edge the app-overview's own alphabet rail and floating search button sit on - the
     *  App overview page's own setting (see SettingsAppOverviewScreen), not a global one: nothing
     *  else in the launcher has a left/right-dependent layout. */
    val appOverviewHandedness: Flow<Handedness> = dataStore.data.map { prefs ->
        prefs[APP_OVERVIEW_HANDEDNESS_KEY]?.let { runCatching { Handedness.valueOf(it) }.getOrNull() } ?: Handedness.RIGHT
    }

    val appListHeaderLetterAppearance: Flow<ElementAppearance> =
        elementAppearanceFor("search_header_letter", ElementAppearance(fillMode = ElementFillMode.NONE, borderEnabled = true))

    // ---- App overview ----

    val appListShowSearchButton: Flow<Boolean> = dataStore.data.map { it[APP_LIST_SHOW_SEARCH_BUTTON_KEY] ?: true }

    // ---- Page backgrounds ----

    fun pageBackgroundOverride(pageId: Long): Flow<BackgroundConfig?> = backgroundOverrideFor(pageKey(pageId))

    /** A page's own darken/lighten applied to the *global* background while it has no override of its
     *  own; null = the global background's own shade. Kept apart from the override so switching back
     *  to "use global" doesn't lose it. */
    fun pageGlobalShade(pageId: Long): Flow<Float?> = globalShadeFor(pageKey(pageId))

    suspend fun setPageGlobalShade(pageId: Long, shade: Float) = setGlobalShadeFor(pageKey(pageId), shade)

    suspend fun clearPageGlobalShade(pageId: Long) {
        dataStore.edit { it.remove(globalShadeKey(pageKey(pageId))) }
    }

    /** The text search screen's own backdrop, same override/global-shade pair as a page's. */
    val searchBackgroundOverride: Flow<BackgroundConfig?> = backgroundOverrideFor(SEARCH_BACKGROUND_KEY)

    val searchGlobalShade: Flow<Float?> = globalShadeFor(SEARCH_BACKGROUND_KEY)

    suspend fun setSearchBackgroundConfig(config: BackgroundConfig) = setBackgroundConfigFor(SEARCH_BACKGROUND_KEY, config)

    suspend fun clearSearchBackgroundConfig() = clearBackgroundConfigFor(SEARCH_BACKGROUND_KEY)

    suspend fun setSearchGlobalShade(shade: Float) = setGlobalShadeFor(SEARCH_BACKGROUND_KEY, shade)

    private fun globalShadeKey(key: String) = floatPreferencesKey("bg_${key}_global_shade")

    private fun globalShadeFor(key: String): Flow<Float?> = dataStore.data.map { it[globalShadeKey(key)] }

    private suspend fun setGlobalShadeFor(key: String, shade: Float) {
        dataStore.edit { it[globalShadeKey(key)] = shade.coerceIn(-1f, 1f) }
    }

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
        imagePath = prefs[backgroundImagePathKey(key)]?.let { com.bisc.flauncher.util.BackgroundImageStore.resolve(context, it) },
        shade = (prefs[backgroundShadeKey(key)] ?: default.shade).coerceIn(-1f, 1f)
    )

    private fun backgroundConfigFor(key: String, default: BackgroundConfig): Flow<BackgroundConfig> = dataStore.data.map { prefs ->
        readBackgroundConfig(prefs, key, default)
    }

    // Null exactly when this surface has never been given its own explicit background - the surface
    // then falls back to the single global background instead. Same "null means untouched, fall back to
    // the shared default" shape already used for dockSearchButtonAppearance etc., just applied to layer 1.
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
            prefs.remove(backgroundShadeKey(key))
        }
    }

    private fun pageKey(pageId: Long) = "page_$pageId"

    // ---- Layer 1: one global background shared by every surface (pages, app overview, text search),
    // with an optional per-surface override (own color or own image) or, without one, a per-surface
    // darken/lighten of the global one. The image-to-image crossfade flicker that once got per-surface
    // images removed is handled at draw time now (see BackgroundCrossfade).

    val globalBackground: Flow<BackgroundConfig> = backgroundConfigFor(GLOBAL_BACKGROUND_KEY, BackgroundConfig())

    suspend fun setGlobalBackground(config: BackgroundConfig) = setBackgroundConfigFor(GLOBAL_BACKGROUND_KEY, config)

    suspend fun hasGlobalBackground(): Boolean = dataStore.data.map { it[backgroundSourceKey(GLOBAL_BACKGROUND_KEY)] != null }.first()

    // ---- Gestures ----

    fun gestureAction(gesture: HomeGesture): Flow<GestureAction> = dataStore.data.map { prefs ->
        // No more hardcoded per-gesture fallback here - this repository has no page-id knowledge,
        // so the real SWIPE_UP default (pointing at the App overview page) is
        // seeded once, with real ids, by PageContentRepository.ensureSeeded() instead.
        val type = prefs[gestureTypeKey(gesture)]?.let { runCatching { GestureActionType.valueOf(it) }.getOrNull() }
            ?: GestureActionType.NONE
        GestureAction(type, prefs[gesturePayloadKey(gesture)])
    }

    /** One-shot check, not a [Flow] - used only by seeding code to decide whether a default binding
     *  would still be safe to write (i.e. the user hasn't touched this gesture yet). */
    suspend fun isGestureConfigured(gesture: HomeGesture): Boolean =
        dataStore.data.first().contains(gestureTypeKey(gesture))

    // ---- Setters: general ----

    suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[THEME_KEY] = theme.name }
    }

    suspend fun setAppOverviewHandedness(handedness: Handedness) {
        dataStore.edit { it[APP_OVERVIEW_HANDEDNESS_KEY] = handedness.name }
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

    suspend fun setIconSizePercent(percent: Int) {
        dataStore.edit { it[ICON_SIZE_PERCENT_KEY] = percent.coerceIn(0, 200) }
    }

    suspend fun setDefaultCornerRadiusPercent(percent: Int) {
        dataStore.edit { it[DEFAULT_CORNER_RADIUS_PERCENT_KEY] = percent.coerceIn(0, 100) }
    }

    // ---- Status bar ----

    fun pageHideStatusBar(pageId: Long): Flow<Boolean> = dataStore.data.map { it[pageHideStatusBarKey(pageId)] ?: false }

    suspend fun setPageHideStatusBar(pageId: Long, hide: Boolean) {
        dataStore.edit { it[pageHideStatusBarKey(pageId)] = hide }
    }

    private fun pageHideStatusBarKey(pageId: Long) = booleanPreferencesKey("page_hide_status_bar_$pageId")

    val folderHideStatusBar: Flow<Boolean> = dataStore.data.map { it[FOLDER_HIDE_STATUS_BAR_KEY] ?: false }

    suspend fun setFolderHideStatusBar(hide: Boolean) {
        dataStore.edit { it[FOLDER_HIDE_STATUS_BAR_KEY] = hide }
    }

    /** Whether opening a folder also hides the floating dock, like it can hide the status bar. Off by default. */
    val folderHideDock: Flow<Boolean> = dataStore.data.map { it[FOLDER_HIDE_DOCK_KEY] ?: false }

    suspend fun setFolderHideDock(hide: Boolean) {
        dataStore.edit { it[FOLDER_HIDE_DOCK_KEY] = hide }
    }

    /** Default for whether folder items show a text label under their icon - used by any folder
     *  that doesn't set its own [com.bisc.flauncher.data.model.Folder.showLabelsOverrideEnabled]. */
    val folderShowLabels: Flow<Boolean> = dataStore.data.map { it[FOLDER_SHOW_LABELS_KEY] ?: false }

    suspend fun setFolderShowLabels(show: Boolean) {
        dataStore.edit { it[FOLDER_SHOW_LABELS_KEY] = show }
    }

    /** One-shot command: hides the status bar on every screen (search, folders, every page - the
     *  Panel is part of the home page now, so its own pageHideStatusBar entry already covers it). */
    suspend fun setStatusBarHiddenEverywhere(pageIds: List<Long>, hidden: Boolean) {
        dataStore.edit { prefs ->
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

    val appListRailAppearance: Flow<ElementAppearance?> = customElementAppearanceFor(RAIL_PREFIX, ElementAppearance(borderEnabled = true))

    suspend fun setAppListRailAppearance(appearance: ElementAppearance) = setElementAppearanceFor(RAIL_PREFIX, appearance)

    suspend fun resetAppListRailAppearance() = resetElementAppearanceFor(RAIL_PREFIX)

    val appListSearchButtonAppearance: Flow<ElementAppearance?> =
        customElementAppearanceFor("app_list_search_button", ElementAppearance(borderEnabled = true))

    suspend fun resetAppListSearchButtonAppearance() = resetElementAppearanceFor("app_list_search_button")

    suspend fun setFolderCardAppearance(appearance: ElementAppearance) = setElementAppearanceFor("folder_card", appearance)

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
        return defaultBoxFillColorFor(theme.isDark(context))
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


    suspend fun setChargingAnimationEnabled(enabled: Boolean) {
        dataStore.edit { it[CHARGING_ANIMATION_ENABLED_KEY] = enabled }
    }

    suspend fun setAppListShowAzIndex(show: Boolean) {
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

    suspend fun setAppListHeaderLetterAppearance(appearance: ElementAppearance) = setElementAppearanceFor("search_header_letter", appearance)


    suspend fun setAppListSearchButtonAppearance(appearance: ElementAppearance) =
        setElementAppearanceFor("app_list_search_button", appearance)

    /** Whether the app list's search button opens its own app-only search instead of Text search. */
    val appListOwnSearch: Flow<Boolean> = dataStore.data.map { it[APP_LIST_OWN_SEARCH_KEY] ?: false }

    suspend fun setAppListOwnSearch(own: Boolean) {
        dataStore.edit { it[APP_LIST_OWN_SEARCH_KEY] = own }
    }

    // ---- App grid page ----

    val appGridColumns: Flow<Int> = dataStore.data.map { (it[APP_GRID_COLUMNS_KEY] ?: 4).coerceIn(3, 6) }

    suspend fun setAppGridColumns(columns: Int) {
        dataStore.edit { it[APP_GRID_COLUMNS_KEY] = columns.coerceIn(3, 6) }
    }

    /** Icon size on top of the global one, in percent. */
    val appGridIconSizePercent: Flow<Int> = dataStore.data.map { (it[APP_GRID_ICON_SIZE_KEY] ?: 100).coerceIn(60, 140) }

    suspend fun setAppGridIconSizePercent(percent: Int) {
        dataStore.edit { it[APP_GRID_ICON_SIZE_KEY] = percent.coerceIn(60, 140) }
    }

    val appGridShowLabels: Flow<Boolean> = dataStore.data.map { it[APP_GRID_SHOW_LABELS_KEY] ?: true }

    suspend fun setAppGridShowLabels(show: Boolean) {
        dataStore.edit { it[APP_GRID_SHOW_LABELS_KEY] = show }
    }

    val appGridBarPosition: Flow<AppGridBarPosition> = dataStore.data.map { prefs ->
        prefs[APP_GRID_BAR_POSITION_KEY]?.let { runCatching { AppGridBarPosition.valueOf(it) }.getOrNull() } ?: AppGridBarPosition.BOTTOM
    }

    suspend fun setAppGridBarPosition(position: AppGridBarPosition) {
        dataStore.edit { it[APP_GRID_BAR_POSITION_KEY] = position.name }
    }

    /** Tapping the grid's search field opens Text search instead of filtering the grid. */
    val appGridBarOpensTextSearch: Flow<Boolean> = dataStore.data.map { it[APP_GRID_BAR_TEXT_SEARCH_KEY] ?: false }

    suspend fun setAppGridBarOpensTextSearch(open: Boolean) {
        dataStore.edit { it[APP_GRID_BAR_TEXT_SEARCH_KEY] = open }
    }

    val appGridKeyboardAutoOpen: Flow<Boolean> = dataStore.data.map { it[APP_GRID_KEYBOARD_AUTO_OPEN_KEY] ?: false }

    suspend fun setAppGridKeyboardAutoOpen(open: Boolean) {
        dataStore.edit { it[APP_GRID_KEYBOARD_AUTO_OPEN_KEY] = open }
    }

    val appGridBarAppearance: Flow<ElementAppearance?> =
        customElementAppearanceFor("app_grid_search_bar", ElementAppearance(borderEnabled = true))

    suspend fun setAppGridBarAppearance(appearance: ElementAppearance) = setElementAppearanceFor("app_grid_search_bar", appearance)

    suspend fun resetAppGridBarAppearance() = resetElementAppearanceFor("app_grid_search_bar")

    /** The folders shown on the App grid page, in this order. */
    val appGridFolderIds: Flow<List<Long>> = dataStore.data.map { prefs ->
        prefs[APP_GRID_FOLDERS_KEY].orEmpty().split(',').mapNotNull { it.toLongOrNull() }
    }

    suspend fun setAppGridFolderIds(ids: List<Long>) {
        dataStore.edit { it[APP_GRID_FOLDERS_KEY] = ids.joinToString(",") }
    }

    /** Folders sorted in among the apps by name, instead of together at the top. */
    val appGridFoldersAlphabetical: Flow<Boolean> = dataStore.data.map { it[APP_GRID_FOLDERS_ALPHABETICAL_KEY] ?: false }

    suspend fun setAppGridFoldersAlphabetical(alphabetical: Boolean) {
        dataStore.edit { it[APP_GRID_FOLDERS_ALPHABETICAL_KEY] = alphabetical }
    }

    /** Apps inside a shown folder are left out of the grid itself. */
    val appGridHideFolderApps: Flow<Boolean> = dataStore.data.map { it[APP_GRID_HIDE_FOLDER_APPS_KEY] ?: false }

    suspend fun setAppGridHideFolderApps(hide: Boolean) {
        dataStore.edit { it[APP_GRID_HIDE_FOLDER_APPS_KEY] = hide }
    }

    suspend fun setAppListShowSearchButton(show: Boolean) {
        dataStore.edit { it[APP_LIST_SHOW_SEARCH_BUTTON_KEY] = show }
    }

    // ---- Setters: backgrounds, gestures ----

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
            prefs[backgroundShadeKey(key)] = config.shade.coerceIn(-1f, 1f)
        }
    }

    private fun backgroundSourceKey(key: String) = stringPreferencesKey("bg_${key}_source")
    private fun backgroundColorKey(key: String) = intPreferencesKey("bg_${key}_color")
    private fun backgroundSecondaryColorKey(key: String) = intPreferencesKey("bg_${key}_color2")
    private fun backgroundFillModeKey(key: String) = stringPreferencesKey("bg_${key}_fill_mode")
    private fun backgroundDirectionKey(key: String) = stringPreferencesKey("bg_${key}_direction")
    private fun backgroundImagePathKey(key: String) = stringPreferencesKey("bg_${key}_image_path")
    private fun backgroundShadeKey(key: String) = floatPreferencesKey("bg_${key}_shade")

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
        const val SEARCH_BACKGROUND_KEY = "search_page"
        private val DEFAULT_ACCENT_COLOR = com.bisc.flauncher.ui.theme.DEFAULT_ACCENT_COLOR

        private val THEME_KEY = stringPreferencesKey("theme")
        private val APP_OVERVIEW_HANDEDNESS_KEY = stringPreferencesKey("app_overview_handedness")
        private val ICON_PACK_KEY = stringPreferencesKey("active_icon_pack")
        private val FONT_KEY = stringPreferencesKey("font_preference")
        private val ICON_SHAPE_KEY = stringPreferencesKey("icon_shape")
        // Renamed from the old "icon_size_percent" key when the scale changed from a -50..50
        // offset to a direct 0..200 percent, so a pre-existing stored offset is never misread as
        // the new absolute percent - it just reads back as unset (100, the new default) instead.
        private val ICON_SIZE_PERCENT_KEY = intPreferencesKey("icon_size_scale_percent")
        private val DEFAULT_CORNER_RADIUS_PERCENT_KEY = intPreferencesKey("default_corner_radius_percent")
        private val FOLDER_HIDE_STATUS_BAR_KEY = booleanPreferencesKey("folder_hide_status_bar")
        private val FOLDER_HIDE_DOCK_KEY = booleanPreferencesKey("folder_hide_dock")
        private val FOLDER_SHOW_LABELS_KEY = booleanPreferencesKey("folder_show_labels")
        private val ACCENT_COLOR_KEY = intPreferencesKey("accent_color")
        private val CHARGING_ANIMATION_COLOR_KEY = intPreferencesKey("charging_animation_color")
        private val CHARGING_ANIMATION_ENABLED_KEY = booleanPreferencesKey("charging_animation_enabled")
        private val SEARCH_SHOW_AZ_INDEX_KEY = booleanPreferencesKey("search_show_az_index")
        private val SEARCH_HIDE_STATUS_BAR_KEY = booleanPreferencesKey("search_hide_status_bar")
        private val SEARCH_ENGINE_KEY = stringPreferencesKey("search_engine")
        private val SEARCH_ENGINE_CUSTOM_URL_KEY = stringPreferencesKey("search_engine_custom_url")
        private val SEARCH_CATEGORY_ORDER_KEY = stringPreferencesKey("search_category_order")
        private val SEARCH_REMOVED_KEY = stringPreferencesKey("search_removed_categories")
        private val SEARCH_AI_APP_KEY = stringPreferencesKey("search_ai_app")
        private val SEARCH_BROWSER_APP_KEY = stringPreferencesKey("search_browser_app")
        private val SEARCH_MAPS_APP_KEY = stringPreferencesKey("search_maps_app")
        private fun searchIncludeKey(storageKey: String) = booleanPreferencesKey("search_include_$storageKey")
        private val SEARCH_KEYBOARD_AUTO_OPEN_KEY = booleanPreferencesKey("search_keyboard_auto_open")
        private val SHOW_SETTINGS_TIPS_KEY = booleanPreferencesKey("show_settings_tips")
        private val CAROUSEL_ENDLESS_KEY = booleanPreferencesKey("carousel_endless")
        private val MUSIC_SOURCES_KEY = stringSetPreferencesKey("music_sources")
        private val MUSIC_COVER_ROUNDED_KEY = booleanPreferencesKey("music_cover_rounded")
        private const val SEARCH_BAR_PREFIX = "search_bar"
        private val SEARCH_BAR_CORNER_RADIUS_KEY = intPreferencesKey("search_bar_corner_radius_percent")
        private const val SEARCH_RESULTS_PREFIX = "search_results"
        private val SEARCH_RESULTS_CORNER_RADIUS_KEY = intPreferencesKey("search_results_corner_radius_percent")
        private val SEARCH_RESULTS_EDGE_TO_EDGE_KEY = booleanPreferencesKey("search_results_edge_to_edge")
        private val SEARCH_BAR_TOP_LINE_KEY = booleanPreferencesKey("search_bar_top_line")
        private val SEARCH_BAR_MIDDLE_LINE_KEY = booleanPreferencesKey("search_bar_middle_line")
        private val SEARCH_KEYBOARD_CAP_ENABLED_KEY = booleanPreferencesKey("search_keyboard_cap_enabled")
        private val SEARCH_KEYBOARD_CAP_COLOR_KEY = intPreferencesKey("search_keyboard_cap_color")
        private val SEARCH_KEYBOARD_CAP_CORNER_RADIUS_KEY = intPreferencesKey("search_keyboard_cap_corner_radius_percent")
        private val SEARCH_KEYBOARD_CAP_ARROW_KEY = booleanPreferencesKey("search_keyboard_cap_arrow")
        const val DEFAULT_KEYBOARD_CAP_COLOR = 0xFFECEEF4.toInt()
        private const val RAIL_PREFIX = "search_alphabet_rail"
        private val APP_LIST_SHOW_SEARCH_BUTTON_KEY = booleanPreferencesKey("app_list_show_search_button")
        private val APP_LIST_OWN_SEARCH_KEY = booleanPreferencesKey("app_list_own_search")
        private val APP_GRID_COLUMNS_KEY = intPreferencesKey("app_grid_columns")
        private val APP_GRID_ICON_SIZE_KEY = intPreferencesKey("app_grid_icon_size")
        private val APP_GRID_SHOW_LABELS_KEY = booleanPreferencesKey("app_grid_show_labels")
        private val APP_GRID_BAR_POSITION_KEY = stringPreferencesKey("app_grid_bar_position")
        private val APP_GRID_BAR_TEXT_SEARCH_KEY = booleanPreferencesKey("app_grid_bar_text_search")
        private val APP_GRID_KEYBOARD_AUTO_OPEN_KEY = booleanPreferencesKey("app_grid_keyboard_auto_open")
        private val APP_GRID_FOLDERS_KEY = stringPreferencesKey("app_grid_folders")
        private val APP_GRID_FOLDERS_ALPHABETICAL_KEY = booleanPreferencesKey("app_grid_folders_alphabetical")
        private val APP_GRID_HIDE_FOLDER_APPS_KEY = booleanPreferencesKey("app_grid_hide_folder_apps")
    }
}
