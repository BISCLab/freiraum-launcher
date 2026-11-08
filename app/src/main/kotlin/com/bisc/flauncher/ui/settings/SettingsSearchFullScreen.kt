package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.ui.home.search.defaultColor
import com.bisc.flauncher.ui.home.search.icon
import com.bisc.flauncher.ui.home.search.shownIcon
import com.bisc.flauncher.ui.home.search.settingsLabelRes

/** Text search settings hub: everything about how it looks under Appearance, everything about
 *  what it searches and where results open under Behavior. The custom in-app keyboard was
 *  removed - search always uses the system keyboard now, see [com.bisc.flauncher.ui.home.search.SearchInputBar]. */
@Composable
fun SettingsSearchFullScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    AppearanceBehaviorHub(
        title = stringResource(R.string.settings_section_search_full),
        intro = stringResource(R.string.settings_page_search_description),
        appearanceContents = stringResource(R.string.settings_hub_search_appearance),
        behaviorContents = stringResource(R.string.settings_hub_search_behavior),
        navController = navController,
        onBack = onBack,
        onNavigate = onNavigate
    )
}

/** Appearance hub: the page itself, then each of its elements. */
@Composable
fun SettingsSearchAppearanceScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val railShown by viewModel.searchBarShowCategories.collectAsState()
    val railLeft by viewModel.searchCategoryRailLeft.collectAsState()
    SettingsSubScreen(title = stringResource(R.string.settings_home_appearance_title), navController = navController, onBack = onBack) {
        // Tap a part of the miniature to edit it; the list below stays as the second way in.
        SearchPreview(onNavigate = onNavigate)
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_page_label),
                onClick = { onNavigate("page") },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_app_grid_search_field),
                onClick = { onNavigate(SearchElement.FIELD.route) },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_rail_label),
                summary = stringResource(if (railShown) (if (railLeft) R.string.settings_search_rail_left else R.string.settings_search_rail_right) else R.string.settings_search_rail_hidden),
                onClick = { onNavigate(SearchElement.RAIL.route) },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_results_label),
                onClick = { onNavigate(SearchElement.RESULTS.route) },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_keyboard_label),
                onClick = { onNavigate("keyboard") },
                trailing = null
            )
        }
    }
}

/** The search page itself: its background (always opaque), the text color drawn on it, and its
 *  status bar. */
@Composable
fun SettingsSearchPageScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val savedAppearance by viewModel.searchAppearance.collectAsState()
    var searchAppearance by rememberDraft(savedAppearance)
    val backgroundOverride by viewModel.searchBackgroundOverride.collectAsState()
    val globalShade by viewModel.searchGlobalShade.collectAsState()
    val globalBackground by viewModel.globalBackground.collectAsState()
    val hideStatusBar by viewModel.searchHideStatusBar.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_search_page_label), navController = navController, onBack = onBack, onSave = { viewModel.setSearchAppearance(searchAppearance) }, dirty = searchAppearance != savedAppearance) {
        UtilityPageIdentityGroup(kind = com.bisc.flauncher.data.model.LauncherPageKind.SEARCH, navController = navController)
        BackgroundOverrideSection(
            override = backgroundOverride,
            global = globalBackground,
            onUseGlobal = viewModel::clearSearchBackgroundConfig,
            onCustomColor = viewModel::setSearchBackgroundConfig,
            imageKey = com.bisc.flauncher.data.settings.SettingsRepository.SEARCH_BACKGROUND_KEY,
            globalShade = globalShade,
            onGlobalShadeChange = viewModel::setSearchGlobalShade,
            groupTitle = stringResource(R.string.settings_section_global_background)
        )
        SettingsPreferenceGroup(title = stringResource(R.string.settings_element_text_color_label)) {
            ColorPickerRow(
                title = stringResource(R.string.settings_element_text_color_label),
                summary = if (searchAppearance.contentColor == null) stringResource(R.string.settings_element_color_auto_hint) else null,
                color = searchAppearance.contentColor ?: MaterialTheme.colorScheme.onSurface.toArgb(),
                onColorChange = { searchAppearance = searchAppearance.copy(contentColor = it) }
            )
            if (searchAppearance.contentColor != null) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_element_color_reset),
                    onClick = { searchAppearance = searchAppearance.copy(contentColor = null) },
                    trailing = null
                )
            }
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_statusbar_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_statusbar_hide_page),
                onClick = { viewModel.setSearchHideStatusBar(!hideStatusBar) },
                trailing = { Switch(checked = hideStatusBar, onCheckedChange = viewModel::setSearchHideStatusBar) }
            )
        }
    }
}

/** Keyboard: whether it opens by itself, and the optional extension strip on top of it. */
@Composable
fun SettingsSearchKeyboardScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val keyboardAutoOpen by viewModel.searchKeyboardAutoOpen.collectAsState()
    val capEnabled by viewModel.searchKeyboardCapEnabled.collectAsState()
    val capColor by viewModel.searchKeyboardCapColor.collectAsState()
    val capCornerPercent by viewModel.searchKeyboardCapCornerRadiusPercent.collectAsState()
    val capArrow by viewModel.searchKeyboardCapArrow.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_search_keyboard_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_keyboard_auto_open),
                info = stringResource(R.string.settings_search_keyboard_auto_open_summary),
                onClick = { viewModel.setSearchKeyboardAutoOpen(!keyboardAutoOpen) },
                trailing = { Switch(checked = keyboardAutoOpen, onCheckedChange = viewModel::setSearchKeyboardAutoOpen) }
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_search_keyboard_cap_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_keyboard_cap_label),
                info = stringResource(R.string.settings_search_keyboard_cap_info),
                onClick = { viewModel.setSearchKeyboardCapEnabled(!capEnabled) },
                trailing = { Switch(checked = capEnabled, onCheckedChange = viewModel::setSearchKeyboardCapEnabled) }
            )
            if (capEnabled) {
                ColorPickerRow(
                    title = stringResource(R.string.settings_search_keyboard_cap_color),
                    info = stringResource(R.string.settings_search_keyboard_cap_color_info),
                    color = capColor,
                    onColorChange = viewModel::setSearchKeyboardCapColor
                )
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_search_keyboard_cap_arrow),
                    info = stringResource(R.string.settings_search_keyboard_cap_arrow_info),
                    onClick = { viewModel.setSearchKeyboardCapArrow(!capArrow) },
                    trailing = { Switch(checked = capArrow, onCheckedChange = viewModel::setSearchKeyboardCapArrow) }
                )
            }
        }
        if (capEnabled) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
                ExpandablePercentSlider(percent = capCornerPercent, onSettled = viewModel::setSearchKeyboardCapCornerRadiusPercent)
            }
        }
    }
}

/** Behavior: where web queries go, which AI app gets them, and which categories are searched in
 *  which order (with per-category color). */
@Composable
fun SettingsSearchBehaviorScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val categoryOrder by viewModel.searchCategoryOrder.collectAsState()
    val modes by viewModel.searchCategoryModes.collectAsState()
    val railShown by viewModel.searchBarShowCategories.collectAsState()
    val categoryColors by viewModel.categoryColors.collectAsState()
    val engines by viewModel.searchEngines.collectAsState()
    val aiApps by viewModel.searchAiApps.collectAsState()
    val aiChoices = com.bisc.flauncher.ui.home.search.rememberAiChoices(aiApps, viewModel::loadIcon)
    var pickingColorFor by remember { mutableStateOf<SearchCategory?>(null) }
    val favorites by viewModel.searchFavorites.collectAsState()
    val favoritesMode by viewModel.searchFavoritesMode.collectAsState()
    val labeledApps by viewModel.labeledApps.collectAsState()
    var favoritesPickerOpen by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_search_behavior_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = androidx.compose.material.icons.Icons.Filled.Public,
                title = stringResource(R.string.settings_search_web_title),
                summary = (engines.map { it.name } + aiChoices.map { it.label }).joinToString(", "),
                onClick = { navController.navigate("settings/search/web") }
            )
        }

        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_search_categories_label),
            info = stringResource(R.string.settings_search_categories_info)
        ) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_rail_show),
                summary = stringResource(if (railShown) R.string.settings_search_categories_rail_on else R.string.settings_search_categories_rail_off),
                onClick = { viewModel.setSearchBarShowCategories(!railShown) },
                trailing = { Switch(checked = railShown, onCheckedChange = viewModel::setSearchBarShowCategories) }
            )
            CategoryPresets(modes, onPick = viewModel::setSearchCategoryModes)
            CategoryTableHeader(railShown)
            ReorderableList(
                items = categoryOrder,
                key = { it },
                onReorder = viewModel::setSearchCategoryOrder,
                itemHeight = 76.dp,
                shape = androidx.compose.ui.graphics.RectangleShape
            ) { category ->
                CategoryModeRow(
                    category = category,
                    color = categoryColors[category]?.let { Color(it) } ?: category.defaultColor,
                    mode = modes[category] ?: com.bisc.flauncher.data.model.CategoryMode(search = true, rail = true),
                    railShown = railShown,
                    onDesign = { pickingColorFor = category },
                    onChange = { viewModel.setSearchCategoryModes(mapOf(category to it)) }
                )
            }
        }

        SettingsPreferenceGroup(
            title = stringResource(R.string.search_section_favorites),
            info = stringResource(R.string.settings_search_favorites_info)
        ) {
            RadioPreference(stringResource(R.string.settings_search_favorites_mode_both), com.bisc.flauncher.data.model.SearchFavoritesMode.BOTH, favoritesMode, viewModel::setSearchFavoritesMode)
            RadioPreference(stringResource(R.string.settings_search_favorites_mode_manual), com.bisc.flauncher.data.model.SearchFavoritesMode.MANUAL, favoritesMode, viewModel::setSearchFavoritesMode)
            RadioPreference(stringResource(R.string.settings_search_favorites_mode_auto), com.bisc.flauncher.data.model.SearchFavoritesMode.AUTO, favoritesMode, viewModel::setSearchFavoritesMode)
            if (favoritesMode != com.bisc.flauncher.data.model.SearchFavoritesMode.AUTO) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_search_favorites_pick),
                    summary = androidx.compose.ui.res.pluralStringResource(R.plurals.settings_search_favorites_count, favorites.size, favorites.size),
                    onClick = { favoritesPickerOpen = true },
                    trailing = null
                )
            }
        }
    }

    if (favoritesPickerOpen) {
        var picked by remember { mutableStateOf(favorites) }
        AlertDialog(
            onDismissRequest = { favoritesPickerOpen = false },
            title = { Text(stringResource(R.string.settings_search_favorites_pick)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    val apps = labeledApps.sortedBy { it.label.lowercase() }
                    items(apps, key = { it.componentName.flattenToString() }) { app ->
                        val id = app.componentName.flattenToString()
                        val on = id in picked
                        val full = !on && picked.size >= com.bisc.flauncher.data.model.SEARCH_FAVORITES_MAX
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !full) { picked = if (on) picked - id else picked + id }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(checked = on, enabled = !full, onCheckedChange = { picked = if (on) picked - id else picked + id })
                            Text(app.label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setSearchFavorites(picked); favoritesPickerOpen = false }) { Text(stringResource(R.string.widget_notes_save)) }
            },
            dismissButton = { TextButton(onClick = { favoritesPickerOpen = false }) { Text(stringResource(R.string.widget_notes_cancel)) } }
        )
    }

    pickingColorFor?.let { category ->
        val categoryIcons by viewModel.categoryIcons.collectAsState()
        val savedColor = categoryColors[category]?.let { Color(it) } ?: category.defaultColor
        val savedIcon = categoryIcons[category]
        var color by rememberDraft(savedColor)
        var iconKey by rememberDraft(savedIcon)
        SettingsEditPage(
            title = stringResource(category.settingsLabelRes),
            onDismiss = { pickingColorFor = null },
            onSave = {
                if (color != savedColor) viewModel.setCategoryColor(category, color.toArgb())
                if (iconKey != savedIcon) viewModel.setCategoryIcon(category, iconKey)
            },
            dirty = color != savedColor || iconKey != savedIcon
        ) {
            CategoryDesigner(category, color, iconKey, onIcon = { iconKey = it }, onColor = { color = it })
        }
    }
}

/** Ready-made settings for all categories at once; the one matching right now is marked. */
private enum class CategoryPreset(val label: Int) {
    ALL(R.string.settings_search_preset_all),
    APPS_FIRST(R.string.settings_search_preset_apps),
    ON_DEMAND(R.string.settings_search_preset_demand);

    fun modes(): Map<SearchCategory, com.bisc.flauncher.data.model.CategoryMode> = SearchCategory.entries.associateWith { c ->
        val always = when (this) {
            ALL -> true
            APPS_FIRST -> c == SearchCategory.FAVORITES || c == SearchCategory.APPS || c == SearchCategory.FOLDERS
            ON_DEMAND -> c == SearchCategory.FAVORITES
        }
        com.bisc.flauncher.data.model.CategoryMode(search = always, rail = true)
    }
}

/** Three equal boxes side by side, so they always fit the width; a long label wraps instead of
 *  running off the edge. */
@Composable
private fun CategoryPresets(modes: Map<SearchCategory, com.bisc.flauncher.data.model.CategoryMode>, onPick: (Map<SearchCategory, com.bisc.flauncher.data.model.CategoryMode>) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryPreset.entries.forEach { preset ->
            val presetModes = preset.modes()
            androidx.compose.material3.FilterChip(
                selected = modes.isNotEmpty() && presetModes == modes,
                onClick = { onPick(presetModes) },
                label = { Text(stringResource(preset.label)) }
            )
        }
    }
}

private val MODE_COLUMN = 48.dp

/** The heads of the two columns, lined up with the toggles below (and the drag handle). */
@Composable
private fun CategoryTableHeader(railShown: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.settings_search_col_category),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        listOf(R.string.settings_search_col_search to true, R.string.settings_search_col_rail to railShown).forEach { (label, active) ->
            Text(
                stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (active) 1f else 0.4f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.width(MODE_COLUMN)
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.width(32.dp))
    }
}

/** One category: symbol (tap to design it), name with what it does right now, and the two toggles. */
@Composable
private fun CategoryModeRow(
    category: SearchCategory,
    color: Color,
    mode: com.bisc.flauncher.data.model.CategoryMode,
    railShown: Boolean,
    onDesign: () -> Unit,
    onChange: (com.bisc.flauncher.data.model.CategoryMode) -> Unit
) {
    val inRail = mode.rail && railShown
    val (status, statusColor) = when {
        mode.search && inRail -> R.string.settings_search_mode_always to color
        mode.search -> R.string.settings_search_mode_results to MaterialTheme.colorScheme.onSurfaceVariant
        inRail -> R.string.settings_search_mode_demand to MaterialTheme.colorScheme.onSurfaceVariant
        else -> R.string.settings_search_mode_off to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    val off = !mode.search && !inRail
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            category.shownIcon(),
            contentDescription = null,
            tint = color.copy(alpha = if (off) 0.4f else 1f),
            modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDesign).padding(4.dp)
        )
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp, end = 4.dp)) {
            // One line; a longer name slides sideways to be read in full.
            androidx.compose.foundation.layout.Box(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                Text(
                    stringResource(category.settingsLabelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (off) 0.5f else 1f),
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(stringResource(status), style = MaterialTheme.typography.labelSmall, color = statusColor, maxLines = 2)
        }
        ModeToggle(Icons.Filled.Search, on = mode.search, enabled = true, color = color) { onChange(mode.copy(search = it)) }
        ModeToggle(Icons.Filled.ViewSidebar, on = mode.rail, enabled = railShown, color = color) { onChange(mode.copy(rail = it)) }
    }
}

@Composable
private fun ModeToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, on: Boolean, enabled: Boolean, color: Color, onToggle: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = Modifier.width(MODE_COLUMN), contentAlignment = Alignment.Center) {
        val shown = on && enabled
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (shown) color.copy(alpha = 0.2f) else Color.Transparent)
                .then(if (shown) Modifier else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                .clickable(enabled = enabled) { onToggle(!on) }
                .alpha(if (enabled) 1f else 0.4f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (shown) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** A category's symbol and color: a large preview, the symbols to pick (its own first), the color. */
@Composable
private fun CategoryDesigner(category: SearchCategory, color: Color, iconKey: String?, onIcon: (String?) -> Unit, onColor: (Color) -> Unit) {
    val shown = iconKey?.let { key -> com.bisc.flauncher.ui.home.search.CategoryIconChoices.firstOrNull { it.first == key }?.second } ?: category.icon
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) { Icon(shown, contentDescription = null, tint = color, modifier = Modifier.size(36.dp)) }
    }
    SettingsPreferenceGroup(title = stringResource(R.string.settings_category_symbol)) {
        val choices: List<Pair<String?, androidx.compose.ui.graphics.vector.ImageVector>> =
            // Always 6 x 6: its own first, then the others without a twin of its own.
            listOf<Pair<String?, androidx.compose.ui.graphics.vector.ImageVector>>(null to category.icon) +
                com.bisc.flauncher.ui.home.search.CategoryIconChoices.filter { it.second != category.icon }.take(35)
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            choices.chunked(6).forEach { line ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    line.forEach { (key, vector) ->
                        val picked = key == iconKey
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                .background(if (picked) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .then(if (picked) Modifier.border(2.dp, color, androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) else Modifier)
                                .clickable { onIcon(key) },
                            contentAlignment = Alignment.Center
                        ) { Icon(vector, contentDescription = null, tint = if (picked) color else MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    repeat(6 - line.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    SettingsPreferenceGroup(title = stringResource(R.string.settings_category_color)) {
        ColorPickerControls(color = color, onColorChange = onColor, modifier = Modifier.padding(12.dp))
    }
}

/** The search screen's own styleable elements, each with the same Background/Border pages. */
enum class SearchElement(val route: String, val titleRes: Int) {
    FIELD("field", R.string.settings_app_grid_search_field),
    RAIL("rail", R.string.settings_search_rail_label),
    RESULTS("results", R.string.settings_search_results_label)
}

/** One search part: its look with a live preview, its corners, and what else belongs to it. */
@Composable
fun SettingsSearchElementScreen(
    element: SearchElement,
    navController: NavController,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val resultsEdgeToEdge by viewModel.searchResultsEdgeToEdge.collectAsState()
    val barAtBottom by viewModel.searchBarAtBottom.collectAsState()
    val saved = effectiveSearchElementAppearance(element, viewModel)
    // Unset defaults: the field follows the general roundness, the rail the field, the cards start fully rounded.
    val barCorner by viewModel.searchBarCornerRadiusPercent.collectAsState()
    val resultsCorner by viewModel.searchResultsCornerRadiusPercent.collectAsState()
    val fieldCorner by viewModel.searchFieldCornerRadiusPercent.collectAsState()
    val railCorner by viewModel.searchRailCornerRadiusPercent.collectAsState()
    val alphabetCorner by viewModel.alphabetRailCornerPercent.collectAsState()
    val defaultCornerPercent by viewModel.defaultCornerRadiusPercent.collectAsState()
    val savedCorner = when (element) {
        SearchElement.FIELD -> fieldCorner ?: barCorner ?: 100
        SearchElement.RAIL -> railCorner ?: alphabetCorner ?: defaultCornerPercent
        SearchElement.RESULTS -> resultsCorner ?: 100
    }
    ElementLookScreen(
        title = stringResource(element.titleRes),
        navController = navController,
        onBack = onBack,
        saved = saved,
        savedCorner = savedCorner,
        onSave = { look, corner ->
            if (look != saved) viewModel.setSearchElementAppearance(element, look)
            if (corner != null && corner != savedCorner) when (element) {
                SearchElement.FIELD -> viewModel.setSearchFieldCornerRadiusPercent(corner)
                SearchElement.RAIL -> viewModel.setSearchRailCornerRadiusPercent(corner)
                SearchElement.RESULTS -> viewModel.setSearchResultsCornerRadiusPercent(corner)
            }
        },
        kind = when (element) {
            SearchElement.FIELD -> LookSampleKind.FIELD
            SearchElement.RAIL -> LookSampleKind.RAIL
            SearchElement.RESULTS -> LookSampleKind.CARDS
        },
        links = {
            if (element == SearchElement.FIELD) {
                SettingsPreferenceGroup(
                    title = stringResource(R.string.settings_search_bar_position_label),
                    info = stringResource(R.string.settings_search_field_position_info)
                ) {
                    RadioPreference(stringResource(R.string.settings_app_grid_bar_top), false, barAtBottom, viewModel::setSearchBarAtBottom)
                    RadioPreference(stringResource(R.string.settings_app_grid_bar_bottom), true, barAtBottom, viewModel::setSearchBarAtBottom)
                }
            }
            if (element == SearchElement.RAIL) {
                val railShown by viewModel.searchBarShowCategories.collectAsState()
                val railLeft by viewModel.searchCategoryRailLeft.collectAsState()
                SettingsPreferenceGroup(info = stringResource(R.string.settings_search_rail_info)) {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.settings_search_rail_show),
                        onClick = { viewModel.setSearchBarShowCategories(!railShown) },
                        trailing = { Switch(checked = railShown, onCheckedChange = viewModel::setSearchBarShowCategories) }
                    )
                    if (railShown) {
                        RadioPreference(stringResource(R.string.settings_search_rail_left), true, railLeft, viewModel::setSearchCategoryRailLeft)
                        RadioPreference(stringResource(R.string.settings_search_rail_right), false, railLeft, viewModel::setSearchCategoryRailLeft)
                    }
                }
            }
            if (element == SearchElement.RESULTS) {
                SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.settings_widgets_edge_to_edge_label),
                        info = stringResource(R.string.settings_edge_to_edge_summary),
                        onClick = { viewModel.setSearchResultsEdgeToEdge(!resultsEdgeToEdge) },
                        trailing = { Switch(checked = resultsEdgeToEdge, onCheckedChange = viewModel::setSearchResultsEdgeToEdge) }
                    )
                }
            }
        },
        extras = {
            val fieldOwn by viewModel.searchFieldAppearance.collectAsState()
            val railOwn by viewModel.searchRailAppearance.collectAsState()
            val reset: (() -> Unit)? = when {
                element == SearchElement.FIELD && fieldOwn != null -> viewModel::resetSearchFieldAppearance
                element == SearchElement.RAIL && railOwn != null -> viewModel::resetSearchRailAppearance
                else -> null
            }
            if (reset != null) {
                SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(if (element == SearchElement.RAIL) R.string.settings_search_rail_like_field else R.string.settings_element_color_reset),
                        onClick = reset,
                        trailing = null
                    )
                }
            }
        }
    )
}

/** Whether the search page is dark, from its background - for the parts that follow it. */
@Composable
private fun searchPageDark(viewModel: SettingsViewModel): Boolean {
    val override by viewModel.searchBackgroundOverride.collectAsState()
    val global by viewModel.globalBackground.collectAsState()
    val shade by viewModel.searchGlobalShade.collectAsState()
    val background = override ?: global.copy(shade = shade ?: global.shade)
    return com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(
        com.bisc.flauncher.data.model.ElementAppearance(fillMode = com.bisc.flauncher.data.model.ElementFillMode.NONE), background
    )
}

/** Until customized here, an element looks like every other element: the theme's default box. */
@Composable
private fun effectiveSearchElementAppearance(element: SearchElement, viewModel: SettingsViewModel): com.bisc.flauncher.data.model.ElementAppearance {
    if (element == SearchElement.RAIL) {
        // Until changed, like the App list's A-Z rail.
        val railOwn by viewModel.searchRailAppearance.collectAsState()
        val alphabetRail by viewModel.appListRailAppearance.collectAsState()
        return railOwn ?: alphabetRail ?: com.bisc.flauncher.ui.home.search.automaticRailAppearance(searchPageDark(viewModel))
    }
    val own by when (element) {
        SearchElement.RAIL, SearchElement.FIELD -> viewModel.searchFieldAppearance
        SearchElement.RESULTS -> viewModel.searchResultsAppearance
    }.collectAsState()
    val defaultBox = com.bisc.flauncher.ui.theme.defaultBoxAppearanceFor(MaterialTheme.colorScheme.background.luminance() < 0.5f)
    // The field starts white and round, like the App grid's.
    if (element == SearchElement.FIELD && own == null) return com.bisc.flauncher.ui.home.search.automaticRailAppearance(false)
    return own ?: defaultBox
}

private fun SettingsViewModel.setSearchElementAppearance(element: SearchElement, appearance: com.bisc.flauncher.data.model.ElementAppearance) =
    when (element) {
        SearchElement.FIELD -> setSearchFieldAppearance(appearance)
        SearchElement.RAIL -> setSearchRailAppearance(appearance)
        SearchElement.RESULTS -> setSearchResultsAppearance(appearance)
    }

internal data class PickableApp(val packageName: String, val label: String)

@Composable
internal fun AppPickerDialog(
    title: String,
    clearLabel: String,
    apps: List<PickableApp>,
    selectedPackage: String?,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(app.packageName) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = app.packageName == selectedPackage, onClick = { onPick(app.packageName) })
                        Text(app.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            if (selectedPackage != null) TextButton(onClick = onClear) { Text(clearLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.widget_notes_cancel)) } }
    )
}

/** Apps that handle [uri] as a view intent, sorted by name. */
internal fun appsHandling(context: android.content.Context, uri: String, browsable: Boolean): List<PickableApp> {
    val pm = context.packageManager
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
    if (browsable) intent.addCategory(android.content.Intent.CATEGORY_BROWSABLE)
    return pm.queryIntentActivities(intent, 0)
        .map { PickableApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}
