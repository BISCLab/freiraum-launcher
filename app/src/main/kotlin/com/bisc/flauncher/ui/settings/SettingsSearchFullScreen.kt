package com.bisc.flauncher.ui.settings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.data.model.SearchEngine
import com.bisc.flauncher.ui.home.search.defaultColor
import com.bisc.flauncher.ui.home.search.icon
import com.bisc.flauncher.ui.home.search.settingsLabelRes

/** Text search settings hub: everything about how it looks under Appearance, everything about
 *  what it searches and where results open under Behavior. The custom in-app keyboard was
 *  removed — search always uses the system keyboard now, see [com.bisc.flauncher.ui.home.search.SearchInputBar]. */
@Composable
fun SettingsSearchFullScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    SettingsSubScreen(title = stringResource(R.string.settings_section_search_full), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_home_appearance_title),
                onClick = { onNavigate("appearance") },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_behavior_label),
                onClick = { onNavigate("behavior") },
                trailing = null
            )
        }
    }
}

/** Appearance hub: the page itself, then each of its elements. */
@Composable
fun SettingsSearchAppearanceScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
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
                title = stringResource(R.string.settings_search_bar_label),
                onClick = { onNavigate(SearchElement.BAR.route) },
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
    val included by viewModel.searchIncluded.collectAsState()
    val categoryOrder by viewModel.searchCategoryOrder.collectAsState()
    val categoryColors by viewModel.categoryColors.collectAsState()
    val aiAppPackage by viewModel.searchAiApp.collectAsState()
    val installedApps by viewModel.browsableApps.collectAsState()
    var aiPickerOpen by remember { mutableStateOf(false) }
    var pickingColorFor by remember { mutableStateOf<SearchCategory?>(null) }
    val aiAppLabel = remember(aiAppPackage) {
        aiAppPackage?.let { pkg ->
            runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrNull()
        }
    }
    val engine by viewModel.searchEngine.collectAsState()
    val savedCustomUrl by viewModel.searchEngineCustomUrl.collectAsState()
    // Typing goes through local state; the stored value arrives too late and would swallow characters.
    var customUrl by remember { mutableStateOf(savedCustomUrl) }
    var customUrlEdited by remember { mutableStateOf(false) }
    LaunchedEffect(savedCustomUrl) { if (!customUrlEdited) customUrl = savedCustomUrl }
    var customUrlSheetOpen by remember { mutableStateOf(false) }
    var enginePickerOpen by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_search_behavior_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_engine_label),
                summary = searchEngineLabel(engine),
                onClick = { enginePickerOpen = true },
                trailing = null
            )
            if (engine == SearchEngine.CUSTOM) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_search_engine_custom_url_row),
                    summary = customUrl.ifBlank { stringResource(R.string.settings_search_engine_custom_url_unset) },
                    onClick = { customUrlSheetOpen = true },
                    trailing = null
                )
            }
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_ai_title),
                summary = aiAppLabel ?: stringResource(R.string.settings_search_ai_none),
                info = stringResource(R.string.settings_search_ai_info),
                onClick = { aiPickerOpen = true },
                trailing = null
            )
        }

        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_search_categories_label),
            info = stringResource(R.string.settings_search_categories_info)
        ) {
            ReorderableList(
                items = categoryOrder,
                key = { it },
                onReorder = viewModel::setSearchCategoryOrder
            ) { category ->
                val color = categoryColors[category]?.let { Color(it) } ?: category.defaultColor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        category.icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp).clip(CircleShape).clickable { pickingColorFor = category }.padding(2.dp)
                    )
                    Text(stringResource(category.settingsLabelRes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(checked = category in included, onCheckedChange = { viewModel.setSearchIncluded(category, it) })
                }
            }
        }
    }

    if (enginePickerOpen) {
        AlertDialog(
            onDismissRequest = { enginePickerOpen = false },
            title = { Text(stringResource(R.string.settings_search_engine_label)) },
            text = {
                Column {
                    SearchEngine.entries.forEach { e ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSearchEngine(e); enginePickerOpen = false }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = e == engine, onClick = { viewModel.setSearchEngine(e); enginePickerOpen = false })
                            Text(searchEngineLabel(e), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { enginePickerOpen = false }) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }

    if (customUrlSheetOpen) {
        AlertDialog(
            onDismissRequest = { customUrlSheetOpen = false },
            title = { Text(stringResource(R.string.settings_search_engine_custom_url_row)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_search_engine_custom_info), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = {
                            customUrlEdited = true
                            customUrl = it
                            viewModel.setSearchEngineCustomUrl(it)
                        },
                        singleLine = true,
                        shape = SettingsTextFieldShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { customUrlSheetOpen = false }) { Text(stringResource(R.string.widget_notes_done)) }
            }
        )
    }

    if (aiPickerOpen) {
        AppPickerDialog(
            title = stringResource(R.string.settings_search_ai_pick),
            clearLabel = stringResource(R.string.settings_search_ai_clear),
            apps = remember(installedApps) { installedApps.map { PickableApp(it.componentName.packageName, it.label) }.distinctBy { it.packageName } },
            selectedPackage = aiAppPackage,
            onPick = { viewModel.setSearchAiApp(it); aiPickerOpen = false },
            onClear = { viewModel.setSearchAiApp(null); aiPickerOpen = false },
            onDismiss = { aiPickerOpen = false }
        )
    }

    pickingColorFor?.let { category ->
        val current = categoryColors[category]?.let { Color(it) } ?: category.defaultColor
        var draft by rememberDraft(current)
        SettingsEditPage(
            title = stringResource(category.settingsLabelRes),
            onDismiss = { pickingColorFor = null },
            onSave = { viewModel.setCategoryColor(category, draft.toArgb()) },
            dirty = draft != current
        ) {
            ColorPickerControls(
                color = draft,
                onColorChange = { draft = it }
            )
        }
    }
}

/** The search screen's own styleable elements, each with the same Background/Border pages. */
enum class SearchElement(val route: String, val titleRes: Int) {
    BAR("bar", R.string.settings_search_bar_label),
    RESULTS("results", R.string.settings_search_results_label)
}

/** Background/Border hub for one [SearchElement], same split as the Panel's own. */
@Composable
fun SettingsSearchElementScreen(
    element: SearchElement,
    navController: NavController,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val resultsEdgeToEdge by viewModel.searchResultsEdgeToEdge.collectAsState()
    val barDividers by viewModel.searchBarDividers.collectAsState()
    SettingsSubScreen(title = stringResource(element.titleRes), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_element_fill_label),
                onClick = { onNavigate("background") },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_element_border_label),
                onClick = { onNavigate("border") },
                trailing = null
            )
        }
        if (element == SearchElement.BAR) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_search_bar_dividers_label)) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_search_bar_dividers),
                    onClick = { viewModel.setSearchBarDividers(!barDividers) },
                    trailing = { Switch(checked = barDividers, onCheckedChange = viewModel::setSearchBarDividers) }
                )
            }
        }
        // The bar always spans the full width (it's flush with the screen top like the Panel);
        // only the cards have a side margin to switch off.
        if (element == SearchElement.RESULTS) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_edge_label)) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_widgets_edge_to_edge_label),
                    info = stringResource(R.string.settings_edge_to_edge_summary),
                    onClick = { viewModel.setSearchResultsEdgeToEdge(!resultsEdgeToEdge) },
                    trailing = { Switch(checked = resultsEdgeToEdge, onCheckedChange = viewModel::setSearchResultsEdgeToEdge) }
                )
            }
        }
    }
}

/** Until customized here, an element looks like every other element: the theme's default box. */
@Composable
private fun effectiveSearchElementAppearance(element: SearchElement, viewModel: SettingsViewModel): com.bisc.flauncher.data.model.ElementAppearance {
    val own by when (element) {
        SearchElement.BAR -> viewModel.searchBarAppearance
        SearchElement.RESULTS -> viewModel.searchResultsAppearance
    }.collectAsState()
    return own ?: com.bisc.flauncher.ui.theme.defaultBoxAppearanceFor(MaterialTheme.colorScheme.background.luminance() < 0.5f)
}

private fun SettingsViewModel.setSearchElementAppearance(element: SearchElement, appearance: com.bisc.flauncher.data.model.ElementAppearance) =
    when (element) {
        SearchElement.BAR -> setSearchBarAppearance(appearance)
        SearchElement.RESULTS -> setSearchResultsAppearance(appearance)
    }

@Composable
fun SettingsSearchElementBackgroundScreen(element: SearchElement, navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val saved = effectiveSearchElementAppearance(element, viewModel)
    var appearance by rememberDraft(saved)
    SettingsSubScreen(title = stringResource(R.string.settings_element_fill_label), navController = navController, onBack = onBack, onSave = { viewModel.setSearchElementAppearance(element, appearance) }, dirty = appearance != saved) {
        ElementBackgroundSection(appearance = appearance, onAppearanceChange = { appearance = it })
    }
}

@Composable
fun SettingsSearchElementBorderScreen(element: SearchElement, navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val saved = effectiveSearchElementAppearance(element, viewModel)
    var appearance by rememberDraft(saved)
    // Unset defaults: the bar follows the global corner roundness, the cards start fully rounded.
    val barCorner by viewModel.searchBarCornerRadiusPercent.collectAsState()
    val resultsCorner by viewModel.searchResultsCornerRadiusPercent.collectAsState()
    val defaultCornerPercent by viewModel.defaultCornerRadiusPercent.collectAsState()
    val savedCornerPercent = when (element) {
        SearchElement.BAR -> barCorner ?: defaultCornerPercent
        SearchElement.RESULTS -> resultsCorner ?: 100
    }
    var cornerPercent by rememberDraft(savedCornerPercent)
    val save = {
        if (appearance != saved) viewModel.setSearchElementAppearance(element, appearance)
        if (cornerPercent != savedCornerPercent) when (element) {
            SearchElement.BAR -> viewModel.setSearchBarCornerRadiusPercent(cornerPercent)
            SearchElement.RESULTS -> viewModel.setSearchResultsCornerRadiusPercent(cornerPercent)
        }
    }
    SettingsSubScreen(title = stringResource(R.string.settings_element_border_label), navController = navController, onBack = onBack, onSave = save, dirty = appearance != saved || cornerPercent != savedCornerPercent) {
        ElementBorderSection(appearance = appearance, onAppearanceChange = { appearance = it })
        SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
            ExpandablePercentSlider(
                percent = cornerPercent,
                onSettled = { cornerPercent = it }
            )
        }
    }
}

@Composable
private fun searchEngineLabel(engine: SearchEngine): String = when (engine) {
    SearchEngine.DUCKDUCKGO -> "DuckDuckGo"
    SearchEngine.STARTPAGE -> "Startpage"
    SearchEngine.GOOGLE -> "Google"
    SearchEngine.BRAVE -> "Brave"
    SearchEngine.CUSTOM -> stringResource(R.string.settings_search_engine_custom)
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
