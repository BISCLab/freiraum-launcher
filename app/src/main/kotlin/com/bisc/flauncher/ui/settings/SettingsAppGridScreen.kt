package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppGridBarPosition
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.search.automaticRailAppearance
import kotlin.math.roundToInt

/** The App grid page's settings: same structure as the app list's (Appearance, Behavior). */
@Composable
fun SettingsAppGridScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    SettingsSubScreen(title = stringResource(R.string.settings_pages_app_grid), navController = navController, onBack = onBack) {
        Text(
            stringResource(R.string.settings_page_app_grid_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
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

@Composable
fun SettingsAppGridAppearanceHubScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    SettingsSubScreen(title = stringResource(R.string.settings_home_appearance_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(icon = null, title = stringResource(R.string.settings_search_page_label), onClick = { onNavigate("page") }, trailing = null)
            SettingsPreference(icon = null, title = stringResource(R.string.settings_app_grid_grid_label), onClick = { onNavigate("grid") }, trailing = null)
            SettingsPreference(icon = null, title = stringResource(R.string.settings_app_grid_search_field), onClick = { onNavigate("search_field") }, trailing = null)
        }
    }
}

/** The page itself: identity, background and status bar, like the app list's page screen. */
@Composable
fun SettingsAppGridPageScreen(navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val pageId by viewModel.appGridPageId.collectAsState()
    val id = pageId ?: return
    val hideStatusBar by remember(id) { viewModel.pageHideStatusBar(id) }.collectAsState(initial = false)
    val override by remember(id) { viewModel.pageBackgroundOverride(id) }.collectAsState(initial = null)
    val globalShade by remember(id) { viewModel.pageGlobalShade(id) }.collectAsState(initial = null)
    val globalBackground by viewModel.globalBackground.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_search_page_label), navController = navController, onBack = onBack) {
        UtilityPageIdentityGroup(kind = LauncherPageKind.APP_GRID, navController = navController, viewModel = viewModel)
        BackgroundOverrideSection(
            override = override,
            global = globalBackground,
            onUseGlobal = { viewModel.clearPageBackgroundConfig(id) },
            onCustomColor = { viewModel.setPageBackgroundConfig(id, it) },
            imageKey = "page_$id",
            globalShade = globalShade,
            onGlobalShadeChange = { viewModel.setPageGlobalShade(id, it) },
            groupTitle = stringResource(R.string.settings_section_global_background)
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

/** Columns, icon size and app names. */
@Composable
fun SettingsAppGridGridScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val columns by viewModel.appGridColumns.collectAsState()
    val iconSize by viewModel.appGridIconSizePercent.collectAsState()
    val showLabels by viewModel.appGridShowLabels.collectAsState()
    SettingsSubScreen(title = stringResource(R.string.settings_app_grid_grid_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_app_grid_columns)) {
            SettingsPreference(
                icon = null,
                title = columns.toString(),
                onClick = {},
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { viewModel.setAppGridColumns(columns - 1) }, enabled = columns > 3) {
                            Icon(Icons.Filled.Remove, contentDescription = null)
                        }
                        IconButton(onClick = { viewModel.setAppGridColumns(columns + 1) }, enabled = columns < 6) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                        }
                    }
                }
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_app_grid_icon_size)) {
            ExpandableSlider(
                value = iconSize.toFloat(),
                onSettled = { viewModel.setAppGridIconSizePercent(it.roundToInt()) },
                valueRange = 60f..140f,
                valueLabel = { stringResource(R.string.settings_icon_size_value, it.roundToInt()) }
            )
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_app_grid_labels),
                onClick = { viewModel.setAppGridShowLabels(!showLabels) },
                trailing = { Switch(checked = showLabels, onCheckedChange = viewModel::setAppGridShowLabels) }
            )
        }
    }
}

/** The search field's own look - follows the app list's A-Z index look until customized here. */
@Composable
fun SettingsAppGridSearchFieldScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val own by viewModel.appGridBarAppearance.collectAsState()
    val pageId by homeViewModel.appGridPageId.collectAsState()
    val pageBackground by remember(pageId) { homeViewModel.pageBackgroundConfig(pageId ?: -1L) }.collectAsState(initial = BackgroundConfig())
    val surfaceIsDark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), pageBackground)

    val saved = remember(own) { own ?: automaticRailAppearance(surfaceIsDark) }
    var draft by rememberDraft(saved)
    SettingsSubScreen(title = stringResource(R.string.settings_app_grid_search_field), navController = navController, onBack = onBack, onSave = { viewModel.setAppGridBarAppearance(draft) }, dirty = draft != saved) {
        ElementAppearanceSection(appearance = draft, onAppearanceChange = { draft = it }, styleGroupTitle = null)
        if (own != null) {
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_element_color_reset),
                    onClick = viewModel::resetAppGridBarAppearance,
                    trailing = null
                )
            }
        }
    }
}

/** Search field position and behavior, and which folders the grid shows. */
@Composable
fun SettingsAppGridBehaviorScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val position by viewModel.appGridBarPosition.collectAsState()
    val opensTextSearch by viewModel.appGridBarOpensTextSearch.collectAsState()
    val keyboardAutoOpen by viewModel.appGridKeyboardAutoOpen.collectAsState()
    val folderIds by viewModel.appGridFolderIds.collectAsState()
    val alphabetical by viewModel.appGridFoldersAlphabetical.collectAsState()
    val hideFolderApps by viewModel.appGridHideFolderApps.collectAsState()
    val folders by homeViewModel.folders.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_search_behavior_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_app_grid_search_field)) {
            RadioPreference(stringResource(R.string.settings_app_grid_bar_bottom), AppGridBarPosition.BOTTOM, position, viewModel::setAppGridBarPosition)
            RadioPreference(stringResource(R.string.settings_app_grid_bar_top), AppGridBarPosition.TOP, position, viewModel::setAppGridBarPosition)
            RadioPreference(stringResource(R.string.settings_app_grid_bar_none), AppGridBarPosition.NONE, position, viewModel::setAppGridBarPosition)
        }
        if (position != AppGridBarPosition.NONE) {
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_app_grid_bar_text_search),
                    info = stringResource(R.string.settings_app_grid_bar_text_search_info),
                    onClick = { viewModel.setAppGridBarOpensTextSearch(!opensTextSearch) },
                    trailing = { Switch(checked = opensTextSearch, onCheckedChange = viewModel::setAppGridBarOpensTextSearch) }
                )
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_app_grid_keyboard),
                    info = stringResource(R.string.settings_app_grid_keyboard_info),
                    enabled = !opensTextSearch,
                    onClick = { viewModel.setAppGridKeyboardAutoOpen(!keyboardAutoOpen) },
                    trailing = {
                        Switch(checked = keyboardAutoOpen && !opensTextSearch, enabled = !opensTextSearch, onCheckedChange = viewModel::setAppGridKeyboardAutoOpen)
                    }
                )
            }
        }

        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_app_grid_folders),
            info = stringResource(R.string.settings_app_grid_folders_info)
        ) {
            if (folders.isEmpty()) {
                SettingsPreference(icon = null, title = stringResource(R.string.settings_app_grid_folders_empty), onClick = {}, trailing = null, enabled = false)
            }
            folders.forEach { folder ->
                val shown = folder.id in folderIds
                val toggle = { on: Boolean ->
                    val ids = if (on) folderIds + folder.id else folderIds - folder.id
                    // Kept in the order of the folders list.
                    viewModel.setAppGridFolderIds(folders.map { it.id }.filter { it in ids })
                }
                SettingsPreference(
                    icon = null,
                    title = folder.name,
                    onClick = { toggle(!shown) },
                    trailing = { Switch(checked = shown, onCheckedChange = { toggle(it) }) }
                )
            }
        }
        if (folderIds.isNotEmpty()) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_app_grid_folders_placement)) {
                RadioPreference(stringResource(R.string.settings_app_grid_folders_top), false, alphabetical, viewModel::setAppGridFoldersAlphabetical)
                RadioPreference(stringResource(R.string.settings_app_grid_folders_alphabetical), true, alphabetical, viewModel::setAppGridFoldersAlphabetical)
            }
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_app_grid_hide_folder_apps),
                    onClick = { viewModel.setAppGridHideFolderApps(!hideFolderApps) },
                    trailing = { Switch(checked = hideFolderApps, onCheckedChange = viewModel::setAppGridHideFolderApps) }
                )
            }
        }
    }
}
