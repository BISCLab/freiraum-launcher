package com.bisc.flauncher.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.ui.pageLabel
import com.bisc.flauncher.ui.theme.FontPreference
import com.bisc.flauncher.ui.theme.ThemePreference

/** Design: everything that looks the same across the whole launcher — colors, language and font,
 *  corner shape, icons, background and status bar. Pages and elements can override parts of it on
 *  their own screens (a page's own background, the app list's own accent, ...). */
@Composable
fun SettingsAppearanceScreen(
    navController: NavController,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val theme by viewModel.theme.collectAsState()
    val fontPreference by viewModel.fontPreference.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val statusBarHiddenEverywhere by viewModel.statusBarHiddenEverywhere.collectAsState()
    val defaultCornerRadiusPercent by viewModel.defaultCornerRadiusPercent.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val searchHideStatusBar by viewModel.searchHideStatusBar.collectAsState()
    val folderHideStatusBar by viewModel.folderHideStatusBar.collectAsState()
    // Carousel pages in their order (Home among them), then App list and Text search.
    val orderedPages = remember(allPages) {
        allPages.filter { it.kind == LauncherPageKind.ROWS }.sortedBy { it.orderIndex } +
            allPages.filter { it.kind == LauncherPageKind.APP_LIST } +
            allPages.filter { it.kind == LauncherPageKind.SEARCH }
    }
    var confirmingTheme by remember { mutableStateOf<ThemePreference?>(null) }
    var confirmingHideStatusBarEverywhere by remember { mutableStateOf<Boolean?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    SettingsSubScreen(title = stringResource(R.string.settings_design_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_theme_label)) {
            RadioPreference(stringResource(R.string.settings_theme_light), ThemePreference.LIGHT, theme, onSelect = { confirmingTheme = it })
            RadioPreference(stringResource(R.string.settings_theme_dark), ThemePreference.DARK, theme, onSelect = { confirmingTheme = it })
            RadioPreference(stringResource(R.string.settings_theme_system), ThemePreference.SYSTEM, theme, onSelect = { confirmingTheme = it })
        }
        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_accent_label),
            info = stringResource(R.string.settings_accent_global_info)
        ) {
            ColorPickerRow(
                title = stringResource(R.string.settings_accent_label),
                color = accentColor,
                onColorChange = viewModel::setAccentColor
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_language_label)) {
            var language by remember { mutableStateOf(com.bisc.flauncher.util.AppLanguage.current(context)) }
            val pick = { tag: String ->
                language = tag
                (context as? android.app.Activity)?.let { com.bisc.flauncher.util.AppLanguage.set(it, tag) }
                Unit
            }
            RadioPreference(stringResource(R.string.settings_language_system), "", language, pick)
            RadioPreference("English", "en", language, pick)
            RadioPreference("Deutsch", "de", language, pick)
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_font_label)) {
            RadioPreference(stringResource(R.string.settings_font_system), FontPreference.SYSTEM, fontPreference, viewModel::setFontPreference, font = FontPreference.SYSTEM.fontFamily)
            RadioPreference(stringResource(R.string.settings_font_serif), FontPreference.SERIF, fontPreference, viewModel::setFontPreference, font = FontPreference.SERIF.fontFamily)
            RadioPreference(stringResource(R.string.settings_font_monospace), FontPreference.MONOSPACE, fontPreference, viewModel::setFontPreference, font = FontPreference.MONOSPACE.fontFamily)
            RadioPreference(stringResource(R.string.settings_font_rounded), FontPreference.ROUNDED, fontPreference, viewModel::setFontPreference, font = FontPreference.ROUNDED.fontFamily)
        }
        // Changing this immediately re-stamps every existing row/folder item to match (see
        // SettingsViewModel.setDefaultCornerRadiusPercent) — each element's own Appearance screen
        // can still override it individually afterwards.
        SettingsPreferenceGroup(title = stringResource(R.string.settings_corner_style_label)) {
            ExpandablePercentSlider(
                percent = defaultCornerRadiusPercent,
                onSettled = viewModel::setDefaultCornerRadiusPercent
            )
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_icons_label),
                summary = stringResource(R.string.settings_icons_summary),
                onClick = { onNavigate("appearance/icons") },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_section_global_background),
                summary = stringResource(R.string.settings_card_global_background_subtitle),
                onClick = { onNavigate("appearance/background") },
                trailing = null
            )
        }
        // Home, Pages, App list, Text search and folders each have their own status-bar switch on
        // their own screen. This one is a bulk action over all of them — checked reflects whether
        // every one of them is currently hidden, and toggling it off sets every one back to shown.
        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_statusbar_label),
            info = stringResource(R.string.settings_statusbar_hide_all_summary)
        ) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_statusbar_hide_all),
                onClick = { confirmingHideStatusBarEverywhere = !statusBarHiddenEverywhere },
                trailing = {
                    Switch(checked = statusBarHiddenEverywhere, onCheckedChange = { confirmingHideStatusBarEverywhere = it })
                }
            )
        }
        // Overview of every place with its own status-bar switch — the same switches as on each
        // page's own screen, just all in one list.
        SettingsPreferenceGroup(title = stringResource(R.string.settings_statusbar_per_page_label)) {
            orderedPages.forEach { page ->
                if (page.kind == LauncherPageKind.SEARCH) {
                    StatusBarRow(pageLabel(page, allPages), searchHideStatusBar, viewModel::setSearchHideStatusBar)
                } else {
                    val hidden by remember(page.id) { viewModel.pageHideStatusBar(page.id) }.collectAsState(initial = false)
                    StatusBarRow(pageLabel(page, allPages), hidden) { viewModel.setPageHideStatusBar(page.id, it) }
                }
            }
            StatusBarRow(stringResource(R.string.settings_section_folder), folderHideStatusBar, viewModel::setFolderHideStatusBar)
        }
    }

    confirmingHideStatusBarEverywhere?.let { targetHidden ->
        AlertDialog(
            onDismissRequest = { confirmingHideStatusBarEverywhere = null },
            title = { Text(stringResource(R.string.settings_statusbar_hide_all)) },
            text = {
                Text(
                    stringResource(
                        if (targetHidden) R.string.settings_statusbar_confirm_hide_message
                        else R.string.settings_statusbar_confirm_show_message
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setStatusBarHiddenEverywhere(targetHidden)
                    confirmingHideStatusBarEverywhere = null
                }) { Text(stringResource(R.string.widget_notes_save)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingHideStatusBarEverywhere = null }) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }

    confirmingTheme?.let { pref ->
        val label = stringResource(
            when (pref) {
                ThemePreference.LIGHT -> R.string.settings_theme_light
                ThemePreference.DARK -> R.string.settings_theme_dark
                ThemePreference.SYSTEM -> R.string.settings_theme_system
            }
        )
        AlertDialog(
            onDismissRequest = { confirmingTheme = null },
            title = { Text(stringResource(R.string.settings_theme_confirm_title, label)) },
            text = { Text(stringResource(R.string.settings_theme_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.applyTheme(pref)
                    confirmingTheme = null
                }) {
                    Text(stringResource(R.string.settings_theme_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingTheme = null }) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }
}

@Composable
private fun StatusBarRow(label: String, hidden: Boolean, onChange: (Boolean) -> Unit) {
    SettingsPreference(
        icon = null,
        title = label,
        onClick = { onChange(!hidden) },
        trailing = { Switch(checked = hidden, onCheckedChange = onChange) }
    )
}
