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
import com.bisc.flauncher.data.model.DesignPreset
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.ui.theme.FontPreference
import com.bisc.flauncher.ui.theme.ThemePreference

@Composable
fun SettingsAppearanceScreen(
    navController: NavController,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val theme by viewModel.theme.collectAsState()
    val fontPreference by viewModel.fontPreference.collectAsState()
    val handedness by viewModel.handedness.collectAsState()
    val chargingAnimation by viewModel.chargingAnimationEnabled.collectAsState()
    val chargingColor by viewModel.chargingAnimationColor.collectAsState()
    val statusBarHiddenEverywhere by viewModel.statusBarHiddenEverywhere.collectAsState()
    val homeScreenWidgetsEnabled by viewModel.homeScreenWidgetsEnabled.collectAsState()
    val favoritesBottomBorderEnabled by viewModel.favoritesBottomBorderEnabled.collectAsState()
    val favoritesGroupDividersEnabled by viewModel.favoritesGroupDividersEnabled.collectAsState()
    var confirmingPreset by remember { mutableStateOf<DesignPreset?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri) { ok ->
            android.widget.Toast.makeText(context, if (ok) R.string.settings_backup_export_done else R.string.settings_backup_export_failed, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingImport = uri }

    SettingsSubScreen(title = stringResource(R.string.settings_section_appearance), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_homescreen_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_homescreen_widgets_enabled),
                info = stringResource(R.string.settings_homescreen_widgets_enabled_summary),
                onClick = { viewModel.setHomeScreenWidgetsEnabled(!homeScreenWidgetsEnabled) },
                trailing = {
                    Switch(checked = homeScreenWidgetsEnabled, onCheckedChange = { viewModel.setHomeScreenWidgetsEnabled(it) })
                }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_favorites_bottom_border),
                info = stringResource(R.string.settings_favorites_bottom_border_summary),
                onClick = { viewModel.setFavoritesBottomBorderEnabled(!favoritesBottomBorderEnabled) },
                trailing = {
                    Switch(checked = favoritesBottomBorderEnabled, onCheckedChange = { viewModel.setFavoritesBottomBorderEnabled(it) })
                }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_favorites_group_dividers),
                info = stringResource(R.string.settings_favorites_group_dividers_summary),
                onClick = { viewModel.setFavoritesGroupDividersEnabled(!favoritesGroupDividersEnabled) },
                trailing = {
                    Switch(checked = favoritesGroupDividersEnabled, onCheckedChange = { viewModel.setFavoritesGroupDividersEnabled(it) })
                }
            )
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_section_global_background),
                summary = stringResource(R.string.settings_card_global_background_subtitle),
                onClick = { onNavigate("appearance/background") },
                trailing = null
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_design_presets_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_design_preset_light),
                info = stringResource(R.string.settings_design_preset_summary),
                onClick = { confirmingPreset = DesignPreset.LIGHT },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_design_preset_dark),
                info = stringResource(R.string.settings_design_preset_summary),
                onClick = { confirmingPreset = DesignPreset.DARK },
                trailing = null
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
        SettingsPreferenceGroup(title = stringResource(R.string.settings_theme_label)) {
            RadioPreference(stringResource(R.string.settings_theme_light), ThemePreference.LIGHT, theme, viewModel::setTheme)
            RadioPreference(stringResource(R.string.settings_theme_dark), ThemePreference.DARK, theme, viewModel::setTheme)
            RadioPreference(stringResource(R.string.settings_theme_system), ThemePreference.SYSTEM, theme, viewModel::setTheme)
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_font_label)) {
            RadioPreference(stringResource(R.string.settings_font_system), FontPreference.SYSTEM, fontPreference, viewModel::setFontPreference, font = FontPreference.SYSTEM.fontFamily)
            RadioPreference(stringResource(R.string.settings_font_serif), FontPreference.SERIF, fontPreference, viewModel::setFontPreference, font = FontPreference.SERIF.fontFamily)
            RadioPreference(stringResource(R.string.settings_font_monospace), FontPreference.MONOSPACE, fontPreference, viewModel::setFontPreference, font = FontPreference.MONOSPACE.fontFamily)
            RadioPreference(stringResource(R.string.settings_font_rounded), FontPreference.ROUNDED, fontPreference, viewModel::setFontPreference, font = FontPreference.ROUNDED.fontFamily)
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_handedness_label)) {
            RadioPreference(stringResource(R.string.settings_handedness_left), Handedness.LEFT, handedness, viewModel::setHandedness)
            RadioPreference(stringResource(R.string.settings_handedness_right), Handedness.RIGHT, handedness, viewModel::setHandedness)
        }
        // Home, Pages, Quick overview and Text search each have their own status-bar switch on their own screen.
        // This one is a bulk action over all of them — checked reflects whether every one of them is
        // currently hidden, and toggling it off sets every one of them back to shown.
        SettingsPreferenceGroup(title = stringResource(R.string.settings_statusbar_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_statusbar_hide_all),
                info = stringResource(R.string.settings_statusbar_hide_all_summary),
                onClick = { viewModel.setStatusBarHiddenEverywhere(!statusBarHiddenEverywhere) },
                trailing = {
                    Switch(checked = statusBarHiddenEverywhere, onCheckedChange = { viewModel.setStatusBarHiddenEverywhere(it) })
                }
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_backup_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_backup_export),
                info = stringResource(R.string.settings_backup_export_summary),
                onClick = {
                    val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    exportLauncher.launch("flauncher-backup-$stamp.zip")
                },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_backup_import),
                info = stringResource(R.string.settings_backup_import_summary),
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                trailing = null
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_charging_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_charging_animation),
                info = stringResource(R.string.settings_charging_animation_summary),
                onClick = { viewModel.setChargingAnimationEnabled(!chargingAnimation) },
                trailing = { Switch(checked = chargingAnimation, onCheckedChange = viewModel::setChargingAnimationEnabled) }
            )
        }
        if (chargingAnimation) {
            ColorPickerSection(
                title = stringResource(R.string.settings_charging_color),
                color = chargingColor,
                onColorChange = viewModel::setChargingAnimationColor
            )
        }
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.settings_backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.settings_backup_import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    viewModel.importBackup(uri) { result ->
                        val message = when (result) {
                            com.bisc.flauncher.data.backup.BackupManager.ImportResult.READY -> R.string.settings_backup_import_restarting
                            com.bisc.flauncher.data.backup.BackupManager.ImportResult.NOT_A_BACKUP -> R.string.settings_backup_import_invalid
                            com.bisc.flauncher.data.backup.BackupManager.ImportResult.TOO_NEW -> R.string.settings_backup_import_too_new
                            com.bisc.flauncher.data.backup.BackupManager.ImportResult.FAILED -> R.string.settings_backup_import_failed
                        }
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    }
                }) { Text(stringResource(R.string.settings_backup_import)) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.widget_notes_cancel)) } }
        )
    }

    confirmingPreset?.let { preset ->
        val label = stringResource(
            if (preset == DesignPreset.LIGHT) R.string.settings_design_preset_name_light else R.string.settings_design_preset_name_dark
        )
        AlertDialog(
            onDismissRequest = { confirmingPreset = null },
            title = { Text(stringResource(R.string.settings_design_preset_confirm_title, label)) },
            text = { Text(stringResource(R.string.settings_design_preset_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.applyDesignPreset(preset)
                    confirmingPreset = null
                }) {
                    Text(stringResource(R.string.settings_design_preset_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingPreset = null }) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }
}
