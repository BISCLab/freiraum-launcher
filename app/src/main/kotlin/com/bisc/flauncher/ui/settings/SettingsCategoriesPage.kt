package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
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

/** The Categories page: its name and icon, how it shows the categories, its background and status bar. */
@Composable
fun SettingsCategoriesPageScreen(navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val pageId by viewModel.categoriesPageId.collectAsState()
    val id = pageId ?: return
    val view by viewModel.categoriesView.collectAsState()
    val showSearch by viewModel.categoriesShowSearch.collectAsState()
    val cardRows by viewModel.categoriesCardRows.collectAsState()
    val hideStatusBar by remember(id) { viewModel.pageHideStatusBar(id) }.collectAsState(initial = false)
    val override by remember(id) { viewModel.pageBackgroundOverride(id) }.collectAsState(initial = null)
    val globalShade by remember(id) { viewModel.pageGlobalShade(id) }.collectAsState(initial = null)
    val globalBackground by viewModel.globalBackground.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_pages_categories), navController = navController, onBack = onBack) {
        UtilityPageIdentityGroup(kind = LauncherPageKind.CATEGORIES, navController = navController, viewModel = viewModel)
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.AutoMirrored.Filled.Label,
                title = stringResource(R.string.settings_categories_manage),
                summary = stringResource(R.string.settings_categories_manage_summary),
                onClick = { navController.navigate("settings/elements/categories") }
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_categories_view), info = stringResource(R.string.settings_categories_view_info)) {
            RadioPreference(stringResource(R.string.settings_categories_view_cards), "CARDS", view, viewModel::setCategoriesView, summary = stringResource(R.string.settings_categories_view_cards_summary))
            RadioPreference(stringResource(R.string.settings_categories_view_tabs), "TABS", view, viewModel::setCategoriesView, summary = stringResource(R.string.settings_categories_view_tabs_summary))
            RadioPreference(stringResource(R.string.settings_categories_view_list), "LIST", view, viewModel::setCategoriesView, summary = stringResource(R.string.settings_categories_view_list_summary))
            if (view == "CARDS") {
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_categories_card_rows),
                    options = listOf(1 to "1", 2 to "2", 3 to "3"),
                    selected = cardRows,
                    onSelect = viewModel::setCategoriesCardRows,
                    info = stringResource(R.string.settings_categories_card_rows_info)
                )
            }
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_categories_search_field),
                summary = stringResource(R.string.settings_categories_search_field_summary),
                onClick = { viewModel.setCategoriesShowSearch(!showSearch) },
                trailing = { Switch(checked = showSearch, onCheckedChange = viewModel::setCategoriesShowSearch) }
            )
        }
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
