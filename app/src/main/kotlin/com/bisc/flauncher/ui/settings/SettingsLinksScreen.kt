package com.bisc.flauncher.ui.settings

import com.bisc.flauncher.data.model.face
import androidx.compose.material.icons.filled.Apps
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
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

/** Global "Internetadressen" Element list (Settings -> Elements -> Links) - every [PinnedWebsite] in
 *  one place, independent of which rows/folders currently pin it. Mirrors [SettingsAddressesScreen]. */
@Composable
fun SettingsLinksScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val websites by viewModel.pinnedWebsites.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_elements_links_title), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_links_title)) {
            if (websites.isEmpty()) {
                SettingsPreference(icon = null, title = stringResource(R.string.settings_elements_links_empty), onClick = null, trailing = null)
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

/** Detail/edit screen for one [PinnedWebsite], reached by id - see [SettingsAddressDetailScreen]
 *  for the same pattern applied to addresses. */
@Composable
fun SettingsLinkDetailScreen(websiteId: Long, navController: NavController, onBack: () -> Unit, startElement: String? = null, viewModel: SettingsViewModel = hiltViewModel()) {
    val home: com.bisc.flauncher.ui.home.HomeViewModel = hiltViewModel()
    val stack: StackDraft = androidx.lifecycle.viewmodel.compose.viewModel()
    val websites by viewModel.pinnedWebsites.collectAsState()
    val saved = websites.find { it.id == websiteId } ?: return
    val item = stack.field("item", saved.copy(tileFace = null)) {}
    val face = stack.field("face", saved.tileFace) {}
    val website = item.value.copy(tileFace = face.value)
    val urlValid = com.bisc.flauncher.util.linkAddress(website.url) != null
    // A wrong address is not written; the field says what is wrong.
    stack.saveTogether { if (website != saved && urlValid) viewModel.updateWebsite(website) }
    val browsableApps by viewModel.labeledApps.collectAsState()
    var showAppPicker by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val usages = rememberUsages(com.bisc.flauncher.data.model.GridItemType.WEBSITE, websiteId.toString(), home)

    ElementStackScreen(
        title = website.label.ifBlank { saved.label },
        navController = navController,
        onBack = onBack,
        startElement = startElement,
        draft = stack,
        home = home,
        partTitle = stringResource(R.string.stack_el_link),
        partSummary = website.url.takeIf { it.isNotBlank() },
        partKeys = listOf("item"),
        symbolTitle = stringResource(R.string.stack_el_symbol),
        symbolKeys = listOf("face"),
        name = website.label,
        symbol = { size, ink -> ElementTile(website.face(), website.label, size, ink, Icons.Filled.Language) },
        partOptions = {
            SettingsPreferenceGroup {
                OutlinedTextField(
                    value = website.label,
                    onValueChange = { item.value = item.value.copy(label = it) },
                    label = { Text(stringResource(R.string.sidebar_website_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OutlinedTextField(
                    value = website.url,
                    onValueChange = { item.value = item.value.copy(url = it) },
                    label = { Text(stringResource(R.string.sidebar_website_url_hint)) },
                    isError = !urlValid,
                    supportingText = if (!urlValid) ({ Text(stringResource(R.string.link_address_invalid)) }) else null,
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
                        { TextButton(onClick = { item.value = item.value.copy(browserApp = null) }) { Text(stringResource(R.string.settings_elements_use_default)) } }
                    } else null
                )
            }
            UsagesGroup(usages, navController)
            SettingsPreferenceGroup {
                SettingsPreference(icon = Icons.Filled.DeleteOutline, title = stringResource(R.string.settings_elements_delete_link), onClick = { confirmingDelete = true }, trailing = null)
            }
        },
        symbolOptions = {
            PartIntro(stringResource(R.string.element_tile_info))
            TileFaceEditor(
                face = website.face(),
                onChange = { face.value = it.toJson() },
                autoSurface = com.bisc.flauncher.ui.home.dock.UNSET_APP_SLOT_COLOR,
                letter = website.label,
                content = emptyList(),
                contentSummary = "",
                viewModel = viewModel,
                kinds = listOf(com.bisc.flauncher.data.model.TileKind.SYMBOL, com.bisc.flauncher.data.model.TileKind.LETTER),
                defaultSymbol = Icons.Filled.Language
            )
        }
    )

    if (showAppPicker) {
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app -> item.value = item.value.copy(browserApp = app.componentName.packageName); showAppPicker = false },
            onDismiss = { showAppPicker = false }
        )
    }

    if (confirmingDelete) {
        ConfirmRemoveDialog(
            label = website.label,
            onConfirm = { viewModel.removeWebsite(saved); confirmingDelete = false; onBack() },
            onDismiss = { confirmingDelete = false }
        )
    }
}
