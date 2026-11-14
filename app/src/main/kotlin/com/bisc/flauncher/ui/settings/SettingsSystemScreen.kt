package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** System: how the launcher fits into the phone - permissions and default launcher, language, which
 *  apps links and places open in, the tips in the settings, and backups. */
@Composable
fun SettingsSystemScreen(
    navController: NavController,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val browserPackage by viewModel.searchBrowserApp.collectAsState()
    val mapsPackage by viewModel.searchMapsApp.collectAsState()
    var browserPickerOpen by remember { mutableStateOf(false) }
    var mapsPickerOpen by remember { mutableStateOf(false) }
    val defaultAppLabel = stringResource(R.string.settings_search_open_with_default)
    fun appLabel(pkg: String?): String = pkg?.let {
        runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(it, 0)).toString() }.getOrNull()
    } ?: defaultAppLabel
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }
    // Whether the picked file is locked with a password; null while that is being checked.
    var importLocked by remember { mutableStateOf<Boolean?>(null) }
    var confirmingExport by remember { mutableStateOf(false) }
    // Held only from the password dialog until the file is chosen.
    var exportPassword by remember { mutableStateOf<CharArray?>(null) }
    val showSettingsTips by viewModel.showSettingsTips.collectAsState()
    val accessibilityMode by viewModel.accessibilityMode.collectAsState()
    var language by remember { mutableStateOf(com.bisc.flauncher.util.AppLanguage.current(context)) }
    // Only the chosen language shows; the full list opens on tap, so it stays short as more come.
    val languages = listOf("" to stringResource(R.string.settings_language_system), "en" to "English", "de" to "Deutsch")
    var languagePickerOpen by remember { mutableStateOf(false) }
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val password = exportPassword
        exportPassword = null
        if (uri != null && password != null) viewModel.exportBackup(uri, password) { ok ->
            android.widget.Toast.makeText(context, if (ok) R.string.settings_backup_export_done else R.string.settings_backup_export_failed, android.widget.Toast.LENGTH_LONG).show()
        } else password?.fill(' ')
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importLocked = null
            pendingImport = uri
            viewModel.isBackupLocked(uri) { importLocked = it }
        }
    }

    SettingsSubScreen(title = stringResource(R.string.settings_system_title), navController = navController, onBack = onBack) {
        SettingsTileGrid(
            tiles = listOf(
                SettingsTile(
                    androidx.compose.material.icons.Icons.Filled.Security,
                    stringResource(R.string.settings_section_permissions),
                    stringResource(
                        if (DefaultLauncherUtil.isDefaultLauncher(context)) R.string.settings_default_launcher_already_set
                        else R.string.settings_default_launcher_description
                    )
                ) { onNavigate("permissions") },
                SettingsTile(
                    androidx.compose.material.icons.Icons.Filled.Translate,
                    stringResource(R.string.settings_language_label),
                    badge = languages.firstOrNull { it.first == language }?.second ?: languages.first().second
                ) { languagePickerOpen = true }
            )
        )

        // Changes how everything looks, so it stands near the top where it is found.
        SettingsPreferenceGroup(title = stringResource(R.string.settings_accessibility_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_accessibility_mode),
                summary = stringResource(R.string.settings_accessibility_mode_summary),
                onClick = { viewModel.setAccessibilityMode(!accessibilityMode) },
                trailing = { Switch(checked = accessibilityMode, onCheckedChange = viewModel::setAccessibilityMode) }
            )
        }
        SettingsTileGrid(
            title = stringResource(R.string.settings_standard_apps_label),
            info = stringResource(R.string.settings_search_open_with_info),
            tiles = listOf(
                SettingsTile(androidx.compose.material.icons.Icons.Filled.Public, stringResource(R.string.settings_search_browser_title),
                    badge = appLabel(browserPackage)) { browserPickerOpen = true },
                SettingsTile(androidx.compose.material.icons.Icons.Filled.Map, stringResource(R.string.settings_search_maps_title),
                    badge = appLabel(mapsPackage)) { mapsPickerOpen = true }
            )
        )
        // Turns every "?" tip button across the settings on or off (see LocalShowSettingsTips).
        SettingsPreferenceGroup(title = stringResource(R.string.settings_tips_label)) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_tips_show),
                onClick = { viewModel.setShowSettingsTips(!showSettingsTips) },
                trailing = { Switch(checked = showSettingsTips, onCheckedChange = viewModel::setShowSettingsTips) }
            )
        }
        SettingsTileGrid(
            title = stringResource(R.string.settings_backup_label),
            info = stringResource(R.string.settings_backup_info),
            tiles = listOf(
                SettingsTile(androidx.compose.material.icons.Icons.Filled.Download, stringResource(R.string.settings_backup_export)) { confirmingExport = true },
                SettingsTile(androidx.compose.material.icons.Icons.Filled.Restore, stringResource(R.string.settings_backup_import)) {
                    importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                }
            )
        )
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

    if (languagePickerOpen) {
        ChoiceDialog(
            title = stringResource(R.string.settings_language_label),
            options = languages,
            selected = language,
            onPick = { tag ->
                languagePickerOpen = false
                language = tag
                (context as? android.app.Activity)?.let { com.bisc.flauncher.util.AppLanguage.set(it, tag) }
            },
            onDismiss = { languagePickerOpen = false }
        )
    }
    if (confirmingExport) {
        var password by remember { mutableStateOf("") }
        var repeat by remember { mutableStateOf("") }
        val valid = password.length >= MIN_BACKUP_PASSWORD && password == repeat
        AlertDialog(
            onDismissRequest = { confirmingExport = false },
            title = { Text(stringResource(R.string.settings_backup_export)) },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_backup_export_locked_info))
                    BackupPasswordField(password, { password = it }, stringResource(R.string.settings_backup_password))
                    BackupPasswordField(
                        repeat, { repeat = it }, stringResource(R.string.settings_backup_password_repeat),
                        error = when {
                            password.isNotEmpty() && password.length < MIN_BACKUP_PASSWORD -> stringResource(R.string.settings_backup_password_short, MIN_BACKUP_PASSWORD)
                            repeat.isNotEmpty() && repeat != password -> stringResource(R.string.settings_backup_password_mismatch)
                            else -> null
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    confirmingExport = false
                    exportPassword = password.toCharArray()
                    val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    exportLauncher.launch("freiraum-backup-$stamp.freiraum-backup")
                }) { Text(stringResource(R.string.settings_backup_export_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmingExport = false }) { Text(stringResource(R.string.widget_notes_cancel)) } }
        )
    }

    pendingImport?.let { uri ->
        var password by remember(uri) { mutableStateOf("") }
        var wrong by remember(uri) { mutableStateOf(false) }
        val locked = importLocked
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.settings_backup_import_confirm_title)) },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_backup_import_confirm_message))
                    if (locked == true) {
                        BackupPasswordField(
                            password, { password = it; wrong = false }, stringResource(R.string.settings_backup_password),
                            error = if (wrong) stringResource(R.string.settings_backup_password_wrong) else null
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = locked == false || (locked == true && password.isNotEmpty()), onClick = {
                    viewModel.importBackup(uri, if (locked == true) password.toCharArray() else null) { result ->
                        if (result == com.bisc.flauncher.data.backup.BackupManager.ImportResult.WRONG_PASSWORD) {
                            wrong = true
                        } else {
                            pendingImport = null
                            val message = when (result) {
                                com.bisc.flauncher.data.backup.BackupManager.ImportResult.READY -> R.string.settings_backup_import_restarting
                                com.bisc.flauncher.data.backup.BackupManager.ImportResult.NOT_A_BACKUP -> R.string.settings_backup_import_invalid
                                com.bisc.flauncher.data.backup.BackupManager.ImportResult.TOO_NEW -> R.string.settings_backup_import_too_new
                                else -> R.string.settings_backup_import_failed
                            }
                            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text(stringResource(R.string.settings_backup_import)) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.widget_notes_cancel)) } }
        )
    }
}

private const val MIN_BACKUP_PASSWORD = 8

/** A password field with a button to show what was typed; never learned by the keyboard. */
@Composable
private fun BackupPasswordField(value: String, onChange: (String) -> Unit, label: String, error: String? = null) {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
            autoCorrectEnabled = false
        ),
        trailingIcon = {
            androidx.compose.material3.IconButton(onClick = { visible = !visible }) {
                androidx.compose.material3.Icon(
                    if (visible) androidx.compose.material.icons.Icons.Filled.VisibilityOff else androidx.compose.material.icons.Icons.Filled.Visibility,
                    contentDescription = stringResource(R.string.settings_backup_password_show)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/** A short list of radio choices that closes on pick. */
@Composable
internal fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
    /** Shows each option in a typeface of its own, for picking a font. */
    fontOf: ((T) -> androidx.compose.ui.text.font.FontFamily?)? = null
) {
    ChoicePanel(
        title = title,
        options = options.map { (value, label) ->
            ChoiceOption(label = label, selected = value == selected, onClick = { onPick(value) }, fontFamily = fontOf?.invoke(value))
        },
        onDismiss = onDismiss
    )
}
