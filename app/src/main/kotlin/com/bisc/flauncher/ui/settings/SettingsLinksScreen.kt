package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.home.ConfirmRemoveDialog
import com.bisc.flauncher.ui.home.dock.AddWebsiteDialog
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

/** Global "Internetadressen" Element list (Settings -> Elements -> Links) — every [PinnedWebsite] in
 *  one place, independent of which rows/folders currently pin it. Mirrors [SettingsAddressesScreen]. */
@Composable
fun SettingsLinksScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val websites by viewModel.pinnedWebsites.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_elements_links_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_links_title)) {
            if (websites.isEmpty()) {
                SettingsPreference(icon = null, title = stringResource(R.string.settings_elements_links_empty), onClick = {}, trailing = null)
            }
            websites.sortedBy { it.label }.forEach { website ->
                SettingsPreference(
                    icon = Icons.Filled.Language,
                    title = website.label,
                    summary = website.url,
                    onClick = { navController.navigate("settings/elements/links/${website.id}") }
                )
            }
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_elements_add_link),
                onClick = { showCreateDialog = true },
                trailing = null
            )
        }
    }

    if (showCreateDialog) {
        AddWebsiteDialog(
            onSave = { label, url -> viewModel.addWebsite(label, url); showCreateDialog = false },
            onDismiss = { showCreateDialog = false }
        )
    }
}

/** Detail/edit screen for one [PinnedWebsite], reached by id — see [SettingsAddressDetailScreen]
 *  for the same pattern applied to addresses. */
@Composable
fun SettingsLinkDetailScreen(websiteId: Long, navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val websites by viewModel.pinnedWebsites.collectAsState()
    val saved = websites.find { it.id == websiteId } ?: return
    var website by rememberDraft(saved)
    val browsableApps by viewModel.browsableApps.collectAsState()
    var showAppPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    SettingsSubScreen(title = saved.label, navController = navController, onBack = onBack, onSave = { viewModel.updateWebsite(website) }, dirty = website != saved) {
        SettingsPreferenceGroup {
            OutlinedTextField(
                value = website.label,
                onValueChange = { website = website.copy(label = it) },
                label = { Text(stringResource(R.string.sidebar_website_label_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            OutlinedTextField(
                value = website.url,
                onValueChange = { website = website.copy(url = it) },
                label = { Text(stringResource(R.string.sidebar_website_url_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_browser_app_label)) {
            val appLabel = website.browserApp?.let { pkg -> browsableApps.find { it.componentName.packageName == pkg }?.label }
            SettingsPreference(
                icon = null,
                title = appLabel ?: stringResource(R.string.settings_elements_app_default),
                onClick = { showAppPicker = true },
                trailing = if (website.browserApp != null) {
                    { TextButton(onClick = { website = website.copy(browserApp = null) }) { Text(stringResource(R.string.settings_elements_use_default)) } }
                } else null
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_colors_label)) {
            SettingsPreference(
                icon = com.bisc.flauncher.ui.home.PictogramCatalog.iconFor(website.iconKey) ?: Icons.Filled.Language,
                title = stringResource(R.string.settings_element_icon_label),
                onClick = { showIconPicker = true },
                trailing = null
            )
            AutomaticColorPicker(
                title = stringResource(R.string.settings_section_global_background),
                custom = website.backgroundColor,
                automaticSeed = android.graphics.Color.DKGRAY,
                onChange = { website = website.copy(backgroundColor = it) },
                ownGroup = false
            )
            AutomaticColorPicker(
                title = stringResource(R.string.settings_element_text_color_label),
                custom = website.iconColor,
                automaticSeed = android.graphics.Color.WHITE,
                onChange = { website = website.copy(iconColor = it) },
                ownGroup = false
            )
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.DeleteOutline,
                title = stringResource(R.string.settings_elements_delete_link),
                onClick = { confirmingDelete = true },
                trailing = null
            )
        }
    }

    if (showIconPicker) {
        PictogramPickerSheet(
            selected = website.iconKey,
            onSelect = { key -> website = website.copy(iconKey = key); showIconPicker = false },
            onDismiss = { showIconPicker = false }
        )
    }

    if (showAppPicker) {
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app -> website = website.copy(browserApp = app.componentName.packageName); showAppPicker = false },
            onDismiss = { showAppPicker = false }
        )
    }

    if (confirmingDelete) {
        ConfirmRemoveDialog(
            label = website.label,
            onConfirm = { viewModel.removeWebsite(website); confirmingDelete = false; onBack() },
            onDismiss = { confirmingDelete = false }
        )
    }
}
