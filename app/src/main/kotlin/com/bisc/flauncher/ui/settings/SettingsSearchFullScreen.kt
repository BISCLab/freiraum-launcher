package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.data.model.SearchEngine
import com.bisc.flauncher.data.model.SearchKeyboardType
import com.bisc.flauncher.ui.home.search.icon
import com.bisc.flauncher.ui.home.search.settingsLabelRes

private enum class SearchElementSheet { SEARCH_BACKGROUND, KEYBOARD, RESULT_CARDS }

/** Settings for the text search: backgrounds, keyboard, categories and search engine. */
@Composable
fun SettingsSearchFullScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hideStatusBar by viewModel.searchHideStatusBar.collectAsState()
    val showNumberRow by viewModel.searchKeyboardShowNumberRow.collectAsState()
    val keyboardHaptics by viewModel.searchKeyboardHaptics.collectAsState()
    val keyboardLayout by viewModel.searchKeyboardLayout.collectAsState()
    val keyboardSparks by viewModel.searchKeyboardSparks.collectAsState()
    val sparkColor by viewModel.searchKeyboardSparkColor.collectAsState()
    val included by viewModel.searchIncluded.collectAsState()
    val categoryOrder by viewModel.searchCategoryOrder.collectAsState()
    val aiAppPackage by viewModel.searchAiApp.collectAsState()
    val keyboardType by viewModel.searchKeyboardType.collectAsState()
    val installedApps by viewModel.browsableApps.collectAsState()
    var aiPickerOpen by remember { mutableStateOf(false) }
    var browserPickerOpen by remember { mutableStateOf(false) }
    var mapsPickerOpen by remember { mutableStateOf(false) }
    val browserPackage by viewModel.searchBrowserApp.collectAsState()
    val mapsPackage by viewModel.searchMapsApp.collectAsState()
    val defaultLabel = stringResource(R.string.settings_search_open_with_default)
    fun appLabel(pkg: String?): String = pkg?.let {
        runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(it, 0)).toString() }.getOrNull()
    } ?: defaultLabel
    val custom = keyboardType == SearchKeyboardType.CUSTOM
    val aiAppLabel = remember(aiAppPackage) {
        aiAppPackage?.let { pkg ->
            runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrNull()
        }
    }
    val searchOverlayBackground by viewModel.searchOverlayBackground.collectAsState()
    val keyboardAppearance by viewModel.keyboardAppearance.collectAsState()
    val resultCardAppearance by viewModel.searchResultCardAppearance.collectAsState()
    val engine by viewModel.searchEngine.collectAsState()
    val savedCustomUrl by viewModel.searchEngineCustomUrl.collectAsState()
    // Typing goes through local state; the stored value arrives too late and would swallow characters.
    var customUrl by remember { mutableStateOf(savedCustomUrl) }
    var customUrlEdited by remember { mutableStateOf(false) }
    LaunchedEffect(savedCustomUrl) { if (!customUrlEdited) customUrl = savedCustomUrl }
    var openSheet by remember { mutableStateOf<SearchElementSheet?>(null) }
    var customUrlSheetOpen by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_section_search_full), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_search_appearance_label)) {
            SettingsPreference(
                icon = null,
                title = layerBackgroundLabel(1, R.string.settings_layer_surface_search),
                info = stringResource(R.string.settings_search_overlay_summary),
                onClick = { openSheet = SearchElementSheet.SEARCH_BACKGROUND },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = layerBackgroundLabel(2, R.string.settings_layer_surface_keyboard),
                onClick = { openSheet = SearchElementSheet.KEYBOARD },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = layerBackgroundLabel(2, R.string.settings_layer_surface_result_card),
                onClick = { openSheet = SearchElementSheet.RESULT_CARDS },
                trailing = null
            )
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_search_keyboard_type_label)) {
            RadioPreference(stringResource(R.string.settings_search_keyboard_type_custom), SearchKeyboardType.CUSTOM, keyboardType, viewModel::setSearchKeyboardType)
            RadioPreference(stringResource(R.string.settings_search_keyboard_type_system), SearchKeyboardType.SYSTEM, keyboardType, viewModel::setSearchKeyboardType)
            if (!custom) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_search_keyboard_type_system_howto),
                    info = stringResource(R.string.settings_search_keyboard_type_system_info),
                    onClick = {},
                    trailing = null
                )
            }
        }

        if (custom) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_search_keyboard_layout_label)) {
                RadioPreference(stringResource(R.string.settings_search_keyboard_layout_qwerty), com.bisc.flauncher.data.model.KeyboardLayout.QWERTY, keyboardLayout, viewModel::setSearchKeyboardLayout)
                RadioPreference(stringResource(R.string.settings_search_keyboard_layout_qwertz), com.bisc.flauncher.data.model.KeyboardLayout.QWERTZ, keyboardLayout, viewModel::setSearchKeyboardLayout)
            }
        }

        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_hide_status_bar),
                info = stringResource(R.string.settings_search_hide_status_bar_summary),
                onClick = { viewModel.setSearchHideStatusBar(!hideStatusBar) },
                trailing = { Switch(checked = hideStatusBar, onCheckedChange = viewModel::setSearchHideStatusBar) }
            )
            if (custom) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_search_keyboard_haptics),
                    info = stringResource(R.string.settings_search_keyboard_haptics_summary),
                    infoAction = stringResource(R.string.touch_feedback_hint_open) to { openSoundSettings(context) },
                    onClick = { viewModel.setSearchKeyboardHaptics(!keyboardHaptics) },
                    trailing = { Switch(checked = keyboardHaptics, onCheckedChange = viewModel::setSearchKeyboardHaptics) }
                )
            }
            if (custom) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_search_keyboard_sparks),
                    info = stringResource(R.string.settings_search_keyboard_sparks_summary),
                    onClick = { viewModel.setSearchKeyboardSparks(!keyboardSparks) },
                    trailing = { Switch(checked = keyboardSparks, onCheckedChange = viewModel::setSearchKeyboardSparks) }
                )
            }
            if (custom) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_search_keyboard_show_number_row),
                    info = stringResource(R.string.settings_search_keyboard_show_number_row_summary),
                    onClick = { viewModel.setSearchKeyboardShowNumberRow(!showNumberRow) },
                    trailing = { Switch(checked = showNumberRow, onCheckedChange = viewModel::setSearchKeyboardShowNumberRow) }
                )
            }
        }
        if (custom && keyboardSparks) {
            ColorPickerSection(
                title = stringResource(R.string.settings_search_keyboard_spark_color),
                color = sparkColor,
                onColorChange = viewModel::setSearchKeyboardSparkColor
            )
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_search_categories_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_categories_order_title),
                info = stringResource(R.string.settings_search_categories_info),
                onClick = {},
                trailing = null
            )
            ReorderableList(
                items = categoryOrder,
                key = { it },
                onReorder = viewModel::setSearchCategoryOrder
            ) { category ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(category.settingsLabelRes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(checked = category in included, onCheckedChange = { viewModel.setSearchIncluded(category, it) })
                }
            }
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_search_engine_label)) {
            SearchEngine.entries.forEach { e ->
                RadioPreference(
                    label = searchEngineLabel(e),
                    value = e,
                    selected = engine,
                    onSelect = viewModel::setSearchEngine
                )
            }
            if (engine == SearchEngine.CUSTOM) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_search_engine_custom_url_row),
                    summary = customUrl.ifBlank { stringResource(R.string.settings_search_engine_custom_url_unset) },
                    onClick = { customUrlSheetOpen = true }
                )
            }
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_search_open_with_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_browser_title),
                summary = appLabel(browserPackage),
                info = stringResource(R.string.settings_search_open_with_info),
                onClick = { browserPickerOpen = true }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_maps_title),
                summary = appLabel(mapsPackage),
                onClick = { mapsPickerOpen = true }
            )
            SettingsPreference(
                icon = SearchCategory.AI.icon,
                title = stringResource(R.string.settings_search_ai_title),
                summary = aiAppLabel ?: stringResource(R.string.settings_search_ai_none),
                info = stringResource(R.string.settings_search_ai_info),
                onClick = { aiPickerOpen = true }
            )
        }
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
                        label = { Text(stringResource(R.string.settings_search_engine_custom_hint)) },
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
    if (browserPickerOpen) {
        AppPickerDialog(
            title = stringResource(R.string.settings_search_browser_pick),
            clearLabel = stringResource(R.string.settings_search_open_with_clear),
            apps = remember { appsHandling(context, "https://example.com", browsable = true) },
            selectedPackage = browserPackage,
            onPick = { viewModel.setSearchBrowserApp(it); browserPickerOpen = false },
            onClear = { viewModel.setSearchBrowserApp(null); browserPickerOpen = false },
            onDismiss = { browserPickerOpen = false }
        )
    }
    if (mapsPickerOpen) {
        AppPickerDialog(
            title = stringResource(R.string.settings_search_maps_pick),
            clearLabel = stringResource(R.string.settings_search_open_with_clear),
            apps = remember { appsHandling(context, "geo:0,0?q=test", browsable = false) },
            selectedPackage = mapsPackage,
            onPick = { viewModel.setSearchMapsApp(it); mapsPickerOpen = false },
            onClear = { viewModel.setSearchMapsApp(null); mapsPickerOpen = false },
            onDismiss = { mapsPickerOpen = false }
        )
    }

    when (openSheet) {
        SearchElementSheet.SEARCH_BACKGROUND -> SettingsEditPage(
            title = layerBackgroundLabel(1, R.string.settings_layer_surface_search),
            onDismiss = { openSheet = null }
        ) {
            Text(
                stringResource(R.string.settings_overlay_backdrop_explainer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BackgroundConfigSection(
                imageKey = "search_overlay",
                config = searchOverlayBackground,
                onConfigChange = viewModel::setSearchOverlayBackground
            )
        }
        SearchElementSheet.KEYBOARD -> SettingsEditPage(
            title = layerBackgroundLabel(2, R.string.settings_layer_surface_keyboard),
            onDismiss = { openSheet = null }
        ) {
            ElementAppearanceSection(
                appearance = keyboardAppearance,
                onAppearanceChange = viewModel::setKeyboardAppearance,
                styleGroupTitle = layerBackgroundLabel(2, R.string.settings_layer_surface_keyboard)
            )
        }
        SearchElementSheet.RESULT_CARDS -> SettingsEditPage(
            title = layerBackgroundLabel(2, R.string.settings_layer_surface_result_card),
            onDismiss = { openSheet = null }
        ) {
            ElementAppearanceSection(
                appearance = resultCardAppearance,
                onAppearanceChange = viewModel::setSearchResultCardAppearance,
                styleGroupTitle = layerBackgroundLabel(2, R.string.settings_layer_surface_result_card)
            )
        }
        null -> Unit
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

private data class PickableApp(val packageName: String, val label: String)

@Composable
private fun AppPickerDialog(
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
private fun appsHandling(context: android.content.Context, uri: String, browsable: Boolean): List<PickableApp> {
    val pm = context.packageManager
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
    if (browsable) intent.addCategory(android.content.Intent.CATEGORY_BROWSABLE)
    return pm.queryIntentActivities(intent, 0)
        .map { PickableApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}
