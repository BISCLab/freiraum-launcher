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
    val chargingAnimation by viewModel.chargingAnimationEnabled.collectAsState()
    val chargingColor by viewModel.chargingAnimationColor.collectAsState()
    val statusBarHiddenEverywhere by viewModel.statusBarHiddenEverywhere.collectAsState()
    val defaultCornerRadiusPercent by viewModel.defaultCornerRadiusPercent.collectAsState()
    val browserPackage by viewModel.searchBrowserApp.collectAsState()
    val mapsPackage by viewModel.searchMapsApp.collectAsState()
    var browserPickerOpen by remember { mutableStateOf(false) }
    var mapsPickerOpen by remember { mutableStateOf(false) }
    var confirmingTheme by remember { mutableStateOf<ThemePreference?>(null) }
    var confirmingHideStatusBarEverywhere by remember { mutableStateOf<Boolean?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val defaultAppLabel = stringResource(R.string.settings_search_open_with_default)
    fun appLabel(pkg: String?): String = pkg?.let {
        runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(it, 0)).toString() }.getOrNull()
    } ?: defaultAppLabel
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
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_section_global_background),
                summary = stringResource(R.string.settings_card_global_background_subtitle),
                onClick = { onNavigate("appearance/background") },
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
            RadioPreference(stringResource(R.string.settings_theme_light), ThemePreference.LIGHT, theme, onSelect = { confirmingTheme = it })
            RadioPreference(stringResource(R.string.settings_theme_dark), ThemePreference.DARK, theme, onSelect = { confirmingTheme = it })
            RadioPreference(stringResource(R.string.settings_theme_system), ThemePreference.SYSTEM, theme, onSelect = { confirmingTheme = it })
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
        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_standard_apps_label),
            info = stringResource(R.string.settings_search_open_with_info)
        ) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_browser_title),
                summary = appLabel(browserPackage),
                onClick = { browserPickerOpen = true }
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_maps_title),
                summary = appLabel(mapsPackage),
                onClick = { mapsPickerOpen = true }
            )
        }
        // Home, Pages, Quick overview and Text search each have their own status-bar switch on their own screen.
        // This one is a bulk action over all of them — checked reflects whether every one of them is
        // currently hidden, and toggling it off sets every one of them back to shown.
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
        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_backup_label),
            info = stringResource(R.string.settings_backup_info)
        ) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_backup_export),
                onClick = {
                    val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    exportLauncher.launch("flauncher-backup-$stamp.zip")
                },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_backup_import),
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                trailing = null
            )
        }
        SettingsPreferenceGroup(
            title = stringResource(R.string.settings_charging_label),
            info = stringResource(R.string.settings_charging_animation_summary)
        ) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_charging_animation),
                onClick = { viewModel.setChargingAnimationEnabled(!chargingAnimation) },
                trailing = { Switch(checked = chargingAnimation, onCheckedChange = viewModel::setChargingAnimationEnabled) }
            )
            if (chargingAnimation) {
                ColorPickerRow(
                    title = stringResource(R.string.settings_charging_color),
                    color = chargingColor,
                    onColorChange = viewModel::setChargingAnimationColor
                )
            }
        }
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
