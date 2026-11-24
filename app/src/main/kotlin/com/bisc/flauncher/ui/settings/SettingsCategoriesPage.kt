package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.ui.home.HomeViewModel

/** Where a part of the Categories page settings leads: its own sub page, or the categories themselves. */
internal fun categoriesPageRoute(sub: String): String =
    if (sub == "manage") "settings/elements/categories" else "settings/categories/$sub"

/** The Categories page: like the App grid, the page in small, then Appearance and Behavior. */
@Composable
fun SettingsCategoriesPageScreen(navController: NavController, onBack: () -> Unit) {
    val onNavigate: (String) -> Unit = { navController.navigate(categoriesPageRoute(it)) }
    AppearanceBehaviorHub(
        title = stringResource(R.string.settings_pages_categories),
        intro = stringResource(R.string.settings_page_categories_description),
        appearanceContents = stringResource(R.string.settings_hub_categories_appearance),
        behaviorContents = stringResource(R.string.settings_hub_categories_behavior),
        navController = navController,
        onBack = onBack,
        onNavigate = onNavigate,
        preview = { CategoriesPreview(onNavigate = onNavigate) }
    )
}

/** How the page looks: the page itself, and the colors and symbols of the categories. */
@Composable
fun SettingsCategoriesAppearanceScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val onNavigate: (String) -> Unit = { navController.navigate(categoriesPageRoute(it)) }
    SettingsSubScreen(title = stringResource(R.string.settings_home_appearance_title), navController = navController, onBack = onBack) {
        // Tap a part of the miniature to edit it; the tiles below are the second way in.
        CategoriesPreview(onNavigate = onNavigate)
        SettingsTileGrid(
            title = stringResource(R.string.settings_parts_label),
            tiles = listOf(
                SettingsTile(Icons.Filled.Wallpaper, stringResource(R.string.settings_search_page_label)) { onNavigate("page") },
                SettingsTile(Icons.Filled.Palette, stringResource(R.string.settings_categories_colors_tile)) { onNavigate("manage") }
            )
        )
        SettingsPreferenceGroup {
            CategorySymbolsSwitch("page", stringResource(R.string.settings_category_symbols_page_summary), viewModel)
        }
    }
}

/** The page itself: name and icon, background and status bar, like the App grid's page screen. */
@Composable
fun SettingsCategoriesPagePartScreen(navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val pageId by viewModel.categoriesPageId.collectAsState()
    val id = pageId ?: return
    val hideStatusBar by remember(id) { viewModel.pageHideStatusBar(id) }.collectAsState(initial = false)
    val override by remember(id) { viewModel.pageBackgroundOverride(id) }.collectAsState(initial = null)
    val globalShade by remember(id) { viewModel.pageGlobalShade(id) }.collectAsState(initial = null)
    val globalBackground by viewModel.globalBackground.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_search_page_label), navController = navController, onBack = onBack) {
        UtilityPageIdentityGroup(kind = LauncherPageKind.CATEGORIES, navController = navController, viewModel = viewModel)
        BackgroundOverrideSection(
            override = override,
            global = globalBackground,
            onUseGlobal = { viewModel.clearPageBackgroundConfig(id) },
            onCustomColor = { viewModel.setPageBackgroundConfig(id, it) },
            imageKey = "page_$id",
            globalShade = globalShade,
            onGlobalShadeChange = { viewModel.setPageGlobalShade(id, it) },
            groupTitle = stringResource(R.string.settings_section_global_background),
            onEditGlobal = { navController.navigate("settings/appearance/background") }
        )
        SettingsPreferenceGroup(title = stringResource(R.string.settings_statusbar_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_statusbar_hide_page),
                onClick = { viewModel.setPageHideStatusBar(id, !hideStatusBar) },
                trailing = { Switch(checked = hideStatusBar, onCheckedChange = { viewModel.setPageHideStatusBar(id, it) }) }
            )
        }
    }
}

/** What the page shows and how: cards, tabs or a list, how much a card shows, the search field,
 *  and which categories there are. */
@Composable
fun SettingsCategoriesBehaviorScreen(navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val view by viewModel.categoriesView.collectAsState()
    val showSearch by viewModel.categoriesShowSearch.collectAsState()
    val cardRows by viewModel.categoriesCardRows.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_search_behavior_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_categories_view), info = stringResource(R.string.settings_categories_view_info)) {
            RadioPreference(stringResource(R.string.settings_categories_view_cards), "CARDS", view, viewModel::setCategoriesView, summary = stringResource(R.string.settings_categories_view_cards_summary))
            RadioPreference(stringResource(R.string.settings_categories_view_tabs), "TABS", view, viewModel::setCategoriesView, summary = stringResource(R.string.settings_categories_view_tabs_summary))
            RadioPreference(stringResource(R.string.settings_categories_view_list), "LIST", view, viewModel::setCategoriesView, summary = stringResource(R.string.settings_categories_view_list_summary))
        }
        if (view == "CARDS") {
            SettingsPreferenceGroup {
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_categories_card_rows),
                    options = listOf(1 to "1", 2 to "2", 3 to "3"),
                    selected = cardRows,
                    onSelect = viewModel::setCategoriesCardRows,
                    info = stringResource(R.string.settings_categories_card_rows_info)
                )
            }
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_categories_search_field),
                summary = stringResource(R.string.settings_categories_search_field_summary),
                onClick = { viewModel.setCategoriesShowSearch(!showSearch) },
                trailing = { Switch(checked = showSearch, onCheckedChange = viewModel::setCategoriesShowSearch) }
            )
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.AutoMirrored.Filled.Label,
                title = stringResource(R.string.settings_categories_manage),
                summary = stringResource(R.string.settings_categories_manage_summary),
                onClick = { navController.navigate("settings/elements/categories") }
            )
        }
    }
}
