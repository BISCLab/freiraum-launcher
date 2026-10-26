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
import com.bisc.flauncher.util.DefaultLauncherUtil

/** System: how the launcher fits into the phone — permissions and default launcher, which apps
 *  links and places open in, the charging animation, settings tips and backups. */
@Composable
fun SettingsSystemScreen(
    navController: NavController,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val chargingAnimation by viewModel.chargingAnimationEnabled.collectAsState()
    val chargingColor by viewModel.chargingAnimationColor.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val showSettingsTips by viewModel.showSettingsTips.collectAsState()
    val browserPackage by viewModel.searchBrowserApp.collectAsState()
    val mapsPackage by viewModel.searchMapsApp.collectAsState()
    var browserPickerOpen by remember { mutableStateOf(false) }
    var mapsPickerOpen by remember { mutableStateOf(false) }
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

    SettingsSubScreen(title = stringResource(R.string.settings_system_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_section_permissions),
                summary = stringResource(
                    if (DefaultLauncherUtil.isDefaultLauncher(context)) R.string.settings_default_launcher_already_set
                    else R.string.settings_default_launcher_description
                ),
                onClick = { onNavigate("permissions") },
                trailing = null
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
                onClick = { browserPickerOpen = true },
                trailing = null
            )
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_search_maps_title),
                summary = appLabel(mapsPackage),
                onClick = { mapsPickerOpen = true },
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
                AccentOverrideRows(
                    title = stringResource(R.string.settings_charging_color),
                    own = chargingColor,
                    global = accentColor,
                    onChange = viewModel::setChargingAnimationColor
                )
            }
        }
        // Turns every "?" tip button across the settings on or off (see LocalShowSettingsTips).
        SettingsPreferenceGroup(title = stringResource(R.string.settings_tips_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_tips_show),
                onClick = { viewModel.setShowSettingsTips(!showSettingsTips) },
                trailing = { Switch(checked = showSettingsTips, onCheckedChange = viewModel::setShowSettingsTips) }
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
}
