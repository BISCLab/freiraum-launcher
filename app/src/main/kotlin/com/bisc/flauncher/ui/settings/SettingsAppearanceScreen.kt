package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.theme.FontPreference
import com.bisc.flauncher.ui.theme.SettingsThemeMode
import com.bisc.flauncher.ui.theme.ThemePreference

/** Appearance: everything about how the launcher looks and moves - ready-made looks, color modes,
 *  accent, font, corners, background and icons, status bar and rotation, and animations. Pages
 *  and elements can override parts of it on their own screens. */
@Composable
fun SettingsAppearanceScreen(
    navController: NavController,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val theme by viewModel.theme.collectAsState()
    val settingsThemeMode by viewModel.settingsThemeMode.collectAsState()
    val fontPreference by viewModel.fontPreference.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val lockPortrait by viewModel.lockPortrait.collectAsState()
    var confirmingTheme by remember { mutableStateOf<ThemePreference?>(null) }
    var launcherThemePickerOpen by remember { mutableStateOf(false) }
    var settingsThemePickerOpen by remember { mutableStateOf(false) }
    var accentPickerOpen by remember { mutableStateOf(false) }
    var fontPickerOpen by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_design_title), navController = navController, onBack = onBack) {
        SettingsTileGrid(
            title = stringResource(R.string.settings_ready_look_label),
            tiles = listOf(
                SettingsTile(Icons.Filled.AutoAwesome, stringResource(R.string.designs_title), stringResource(R.string.designs_entry_summary)) { onNavigate("appearance/designs") },
                SettingsTile(Icons.Filled.Wallpaper, stringResource(R.string.packs_title), stringResource(R.string.packs_entry_summary)) { onNavigate("wallpaper_packs") }
            )
        )
        // Each choice opens a short list; the tile shows what is picked.
        SettingsTileGrid(
            title = stringResource(R.string.settings_color_modes_font_label),
            info = stringResource(R.string.settings_color_modes_info),
            columns = 3,
            tiles = listOf(
                SettingsTile(androidx.compose.material.icons.Icons.Filled.Contrast, stringResource(R.string.settings_launcher_theme_label),
                    badge = themeLabel(theme)) { launcherThemePickerOpen = true },
                SettingsTile(androidx.compose.material.icons.Icons.Filled.Settings, stringResource(R.string.settings_settings_theme_label),
                    badge = settingsThemeLabel(settingsThemeMode)) { settingsThemePickerOpen = true },
                SettingsTile(androidx.compose.material.icons.Icons.Filled.TextFields, stringResource(R.string.settings_font_label),
                    badge = fontLabel(fontPreference)) { fontPickerOpen = true }
            )
        )
        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_accent_label),
            info = stringResource(R.string.settings_accent_global_info)
        ) {
            // Tapping a swatch recolors this very screen at once (the settings UI uses the accent
            // as its own primary color) - that's the preview.
            AccentPalette(selected = accentColor, onSelect = viewModel::setAccentColor, onCustom = { accentPickerOpen = true })
        }
        SettingsTileGrid(
            title = stringResource(R.string.settings_parts_label),
            columns = 3,
            tiles = listOf(
                SettingsTile(Icons.Filled.FormatColorFill, stringResource(R.string.settings_colors_all)) { onNavigate("appearance/colors") },
                SettingsTile(Icons.Filled.RoundedCorner, stringResource(R.string.settings_corners_all)) { onNavigate("appearance/corners") },
                SettingsTile(Icons.Filled.MoreVert, stringResource(R.string.settings_menus_title)) { onNavigate("appearance/menus") }
            )
        )
        SettingsTileGrid(
            title = stringResource(R.string.settings_background_icons_label),
            tiles = listOf(
                SettingsTile(Icons.Filled.Image, stringResource(R.string.settings_section_global_background), stringResource(R.string.settings_card_global_background_subtitle)) { onNavigate("appearance/background") },
                SettingsTile(Icons.Filled.Interests, stringResource(R.string.settings_icons_label), stringResource(R.string.settings_icons_summary)) { onNavigate("appearance/icons") }
            )
        )
        SettingsTileGrid(
            title = stringResource(R.string.settings_display_motion_label),
            tiles = listOf(
                SettingsTile(androidx.compose.material.icons.Icons.Filled.StayCurrentPortrait, stringResource(R.string.settings_statusbar_label),
                    badge = statusBarSummary(viewModel)) { onNavigate("appearance/statusbar") },
                SettingsTile(androidx.compose.material.icons.Icons.Filled.Animation, stringResource(R.string.settings_motion_label),
                    badge = motionSummary(viewModel)) { onNavigate("appearance/animations") }
            )
        )
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_lock_portrait),
                summary = stringResource(R.string.settings_lock_portrait_summary),
                onClick = { viewModel.setLockPortrait(!lockPortrait) },
                trailing = { Switch(checked = lockPortrait, onCheckedChange = viewModel::setLockPortrait) }
            )
        }
    }

    if (launcherThemePickerOpen) {
        ChoiceDialog(
            title = stringResource(R.string.settings_launcher_theme_label),
            options = ThemePreference.entries.map { it to themeLabel(it) },
            selected = theme,
            onPick = { launcherThemePickerOpen = false; if (it != theme) confirmingTheme = it },
            onDismiss = { launcherThemePickerOpen = false }
        )
    }
    if (settingsThemePickerOpen) {
        ChoiceDialog(
            title = stringResource(R.string.settings_settings_theme_label),
            options = SettingsThemeMode.entries.map { it to settingsThemeLabel(it) },
            selected = settingsThemeMode,
            onPick = { settingsThemePickerOpen = false; viewModel.setSettingsThemeMode(it) },
            onDismiss = { settingsThemePickerOpen = false }
        )
    }

    if (fontPickerOpen) {
        ChoiceDialog(
            title = stringResource(R.string.settings_font_label),
            options = FontPreference.entries.map { it to fontLabel(it) },
            selected = fontPreference,
            onPick = { fontPickerOpen = false; viewModel.setFontPreference(it) },
            onDismiss = { fontPickerOpen = false },
            fontOf = { it.fontFamily ?: androidx.compose.ui.text.font.FontFamily.Default }
        )
    }

    if (accentPickerOpen) {
        var draft by rememberDraft(accentColor)
        SettingsEditPage(title = stringResource(R.string.settings_accent_custom), onDismiss = { accentPickerOpen = false }, onSave = { viewModel.setAccentColor(draft) }, dirty = draft != accentColor) {
            ColorPickerControls(
                color = Color(draft),
                onColorChange = { draft = it.toArgb() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    confirmingTheme?.let { pref ->
        AlertDialog(
            onDismissRequest = { confirmingTheme = null },
            title = { Text(stringResource(R.string.settings_theme_confirm_title, themeLabel(pref))) },
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
private fun themeLabel(theme: ThemePreference): String = stringResource(
    when (theme) {
        ThemePreference.LIGHT -> R.string.settings_theme_light
        ThemePreference.DARK -> R.string.settings_theme_dark
        ThemePreference.SYSTEM -> R.string.settings_theme_system
    }
)

@Composable
internal fun settingsThemeLabel(mode: SettingsThemeMode): String = stringResource(
    when (mode) {
        SettingsThemeMode.LAUNCHER -> R.string.settings_settings_theme_launcher
        SettingsThemeMode.LIGHT -> R.string.settings_theme_light
        SettingsThemeMode.DARK -> R.string.settings_theme_dark
        SettingsThemeMode.SYSTEM -> R.string.settings_theme_system
    }
)

@Composable
private fun fontLabel(font: FontPreference): String = stringResource(
    when (font) {
        FontPreference.SYSTEM -> R.string.settings_font_system
        FontPreference.SERIF -> R.string.settings_font_serif
        FontPreference.MONOSPACE -> R.string.settings_font_monospace
        FontPreference.ROUNDED -> R.string.settings_font_rounded
    }
)
