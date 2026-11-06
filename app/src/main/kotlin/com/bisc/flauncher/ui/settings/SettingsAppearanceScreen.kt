package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.ui.theme.noRippleClickable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.ui.pageLabel
import com.bisc.flauncher.ui.theme.FontPreference
import com.bisc.flauncher.ui.theme.ThemePreference

/** Design: everything that looks the same across the whole launcher - colors, language and font,
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
            allPages.filter { it.kind == LauncherPageKind.SEARCH } +
            allPages.filter { it.kind == LauncherPageKind.APP_GRID }
    }
    var confirmingTheme by remember { mutableStateOf<ThemePreference?>(null) }
    var accentPickerOpen by remember { mutableStateOf(false) }
    var confirmingHideStatusBarEverywhere by remember { mutableStateOf<Boolean?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    SettingsSubScreen(title = stringResource(R.string.settings_design_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.designs_title),
                summary = stringResource(R.string.designs_entry_summary),
                onClick = { onNavigate("appearance/designs") },
                trailing = null
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_theme_label)) {
            RadioPreference(stringResource(R.string.settings_theme_light), ThemePreference.LIGHT, theme, onSelect = { confirmingTheme = it })
            RadioPreference(stringResource(R.string.settings_theme_dark), ThemePreference.DARK, theme, onSelect = { confirmingTheme = it })
            RadioPreference(stringResource(R.string.settings_theme_system), ThemePreference.SYSTEM, theme, onSelect = { confirmingTheme = it })
        }
        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_accent_label),
            info = stringResource(R.string.settings_accent_global_info)
        ) {
            // Tapping a swatch recolors this very screen at once (the settings UI uses the accent
            // as its own primary color) - that's the preview.
            AccentPalette(selected = accentColor, onSelect = viewModel::setAccentColor, onCustom = { accentPickerOpen = true })
        }
        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_motion_label),
            info = stringResource(R.string.settings_motion_info)
        ) {
            val motionLevel by viewModel.motionLevel.collectAsState()
            // Android's own switch overrides everything here - say so, with a way there.
            var systemOff by remember { mutableStateOf(com.bisc.flauncher.ui.systemAnimationsOff(context)) }
            androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
                systemOff = com.bisc.flauncher.ui.systemAnimationsOff(context)
                onPauseOrDispose { }
            }
            if (systemOff) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_motion_system_off),
                    summary = stringResource(R.string.settings_motion_system_off_summary),
                    onClick = {
                        runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    },
                    trailing = null
                )
            }
            RadioPreference(stringResource(R.string.settings_motion_full), com.bisc.flauncher.ui.MotionLevel.FULL, motionLevel, viewModel::setMotionLevel, enabled = !systemOff)
            RadioPreference(stringResource(R.string.settings_motion_subtle), com.bisc.flauncher.ui.MotionLevel.SUBTLE, motionLevel, viewModel::setMotionLevel, enabled = !systemOff)
            RadioPreference(stringResource(R.string.settings_motion_off), com.bisc.flauncher.ui.MotionLevel.OFF, motionLevel, viewModel::setMotionLevel, enabled = !systemOff)
        }
        val motionLevelNow by viewModel.motionLevel.collectAsState()
        val motionPrefs by viewModel.motionPrefs.collectAsState()
        if (motionLevelNow != com.bisc.flauncher.ui.MotionLevel.OFF) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_motion_each_label)) {
                @Composable
                fun MotionSwitch(titleRes: Int, checked: Boolean, set: (Boolean) -> com.bisc.flauncher.ui.MotionPrefs) {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(titleRes),
                        onClick = { viewModel.setMotionPrefs(set(!checked)) },
                        trailing = { Switch(checked = checked, onCheckedChange = { viewModel.setMotionPrefs(set(it)) }) }
                    )
                }
                MotionSwitch(R.string.settings_motion_app_zoom, motionPrefs.appZoom) { motionPrefs.copy(appZoom = it) }
                MotionSwitch(R.string.settings_motion_overlay_zoom, motionPrefs.overlayZoom) { motionPrefs.copy(overlayZoom = it) }
                MotionSwitch(R.string.settings_motion_folder_zoom, motionPrefs.folderZoom) { motionPrefs.copy(folderZoom = it) }
                MotionSwitch(R.string.settings_motion_page_depth, motionPrefs.pageDepth) { motionPrefs.copy(pageDepth = it) }
                MotionSwitch(R.string.settings_motion_back_gesture, motionPrefs.backGesture) { motionPrefs.copy(backGesture = it) }
                MotionSwitch(R.string.settings_motion_settings_slide, motionPrefs.settingsSlide) { motionPrefs.copy(settingsSlide = it) }
                MotionSwitch(R.string.settings_motion_ripples, motionPrefs.gestureRipples) { motionPrefs.copy(gestureRipples = it) }
            }
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_language_label)) {
            var language by remember { mutableStateOf(com.bisc.flauncher.util.AppLanguage.current(context)) }
            val pick = { tag: String ->
                language = tag
                (context as? android.app.Activity)?.let { com.bisc.flauncher.util.AppLanguage.set(it, tag) }
                Unit
            }
            // Only the chosen language shows; the full list opens on tap, so it stays short as more come.
            val languages = listOf("" to stringResource(R.string.settings_language_system), "en" to "English", "de" to "Deutsch")
            var languagePickerOpen by remember { mutableStateOf(false) }
            SettingsPreference(
                icon = null,
                title = languages.firstOrNull { it.first == language }?.second ?: languages.first().second,
                onClick = { languagePickerOpen = true },
                trailing = null
            )
            if (languagePickerOpen) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { languagePickerOpen = false },
                    title = { Text(stringResource(R.string.settings_language_label)) },
                    text = {
                        androidx.compose.foundation.layout.Column {
                            languages.forEach { (tag, label) ->
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { languagePickerOpen = false; pick(tag) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.RadioButton(selected = tag == language, onClick = { languagePickerOpen = false; pick(tag) })
                                    Text(label, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_font_label)) {
            RadioPreference(stringResource(R.string.settings_font_system), FontPreference.SYSTEM, fontPreference, viewModel::setFontPreference, font = FontPreference.SYSTEM.fontFamily)
            RadioPreference(stringResource(R.string.settings_font_serif), FontPreference.SERIF, fontPreference, viewModel::setFontPreference, font = FontPreference.SERIF.fontFamily)
            RadioPreference(stringResource(R.string.settings_font_monospace), FontPreference.MONOSPACE, fontPreference, viewModel::setFontPreference, font = FontPreference.MONOSPACE.fontFamily)
            RadioPreference(stringResource(R.string.settings_font_rounded), FontPreference.ROUNDED, fontPreference, viewModel::setFontPreference, font = FontPreference.ROUNDED.fontFamily)
        }
        // Changing this immediately re-stamps every existing row/folder item to match (see
        // SettingsViewModel.setDefaultCornerRadiusPercent) - each element's own Appearance screen
        // can still override it individually afterwards.
        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_corner_style_label),
            info = stringResource(R.string.settings_corner_style_info)
        ) {
            CornerShapeTiles(percent = defaultCornerRadiusPercent, onSelect = viewModel::setDefaultCornerRadiusPercent)
            // Fine-tuning between the presets.
            ExpandablePercentSlider(
                percent = defaultCornerRadiusPercent,
                onSettled = viewModel::setDefaultCornerRadiusPercent
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_home_appearance_title)) {
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
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_widget_paging_title),
                summary = stringResource(R.string.settings_widget_paging_summary),
                onClick = { onNavigate("appearance/widget_paging") },
                trailing = null
            )
        }
        // Home, Pages, App list, Text search and folders each have their own status-bar switch on
        // their own screen. This one is a bulk action over all of them - checked reflects whether
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
            // Right under it, in the same card: every place with its own status-bar switch (the
            // same switches as on each page's own screen), as slimmer rows so they read as the
            // detail of the switch above.
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

    if (accentPickerOpen) {
        var draft by rememberDraft(accentColor)
        SettingsEditPage(title = stringResource(R.string.settings_accent_custom), onDismiss = { accentPickerOpen = false }, onSave = { viewModel.setAccentColor(draft) }, dirty = draft != accentColor) {
            ColorPickerControls(
                color = androidx.compose.ui.graphics.Color(draft),
                onColorChange = { draft = it.toArgb() },
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .noRippleClickable { onChange(!hidden) }
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = hidden, onCheckedChange = onChange, modifier = Modifier.scale(0.75f))
    }
}
