package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.ui.home.HomeViewModel

/** Where a part of the Categories page settings leads: its own sub page, or the categories themselves. */
internal fun categoriesPageRoute(sub: String): String =
    if (sub == "manage") "settings/elements/categories" else "settings/categories/$sub"

/**
 * The Categories page on one page: the page in small (a tap on the chips opens the categories, on
 * the backdrop the page itself), then how the categories are laid out, what is shown on top and
 * with each name, and the ways to the page and to the categories.
 */
@Composable
fun SettingsCategoriesPageScreen(navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel(), settings: SettingsViewModel = hiltViewModel()) {
    val view by viewModel.categoriesView.collectAsState()
    val showSearch by viewModel.categoriesShowSearch.collectAsState()
    val cardRows by viewModel.categoriesCardRows.collectAsState()
    // The layout is on this page already; only the page and the categories have pages of their own.
    val onNavigate: (String) -> Unit = { sub -> if (sub != "layout") navController.navigate(categoriesPageRoute(sub)) }

    SettingsSubScreen(title = stringResource(R.string.settings_categories_page), navController = navController, onBack = onBack) {
        CategoriesPreview(onNavigate = onNavigate)
        Text(
            stringResource(R.string.settings_page_categories_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        SettingsPreferenceGroup(title = stringResource(R.string.settings_categories_view), info = stringResource(R.string.settings_categories_view_info)) {
            RadioPreference(stringResource(R.string.settings_categories_view_cards), "CARDS", view, viewModel::setCategoriesView, summary = stringResource(R.string.settings_categories_view_cards_summary))
            RadioPreference(stringResource(R.string.settings_categories_view_tabs), "TABS", view, viewModel::setCategoriesView, summary = stringResource(R.string.settings_categories_view_tabs_summary))
            if (view != "TABS") {
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_categories_card_rows),
                    options = listOf(1 to "1", 2 to "2", 3 to "3", 0 to stringResource(R.string.settings_categories_card_rows_all)),
                    selected = cardRows,
                    onSelect = viewModel::setCategoriesCardRows,
                    info = stringResource(R.string.settings_categories_card_rows_info)
                )
            }
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_categories_shown_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_categories_search_field),
                summary = stringResource(R.string.settings_categories_search_field_summary),
                onClick = { viewModel.setCategoriesShowSearch(!showSearch) },
                trailing = { Switch(checked = showSearch, onCheckedChange = viewModel::setCategoriesShowSearch) }
            )
            CategorySymbolsSwitch("page", stringResource(R.string.settings_category_symbols_page_summary), settings)
            CategoryCountsSwitch("page", settings)
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Wallpaper,
                title = stringResource(R.string.settings_search_page_label),
                summary = stringResource(R.string.settings_categories_page_part_summary),
                onClick = { onNavigate("page") }
            )
            SettingsPreference(
                icon = Icons.AutoMirrored.Filled.Label,
                title = stringResource(R.string.settings_categories_manage),
                summary = stringResource(R.string.settings_categories_manage_summary),
                onClick = { onNavigate("manage") }
            )
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
