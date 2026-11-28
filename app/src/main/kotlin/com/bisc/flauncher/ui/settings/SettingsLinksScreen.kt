package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.face
import com.bisc.flauncher.ui.home.ConfirmRemoveDialog
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.AddWebsiteDialog
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

private fun linkKey(id: Long) = "link:$id"

/**
 * Every link in one card stack: ⓪ all of them with a way to add one, then each link with its
 * options in two tabs, the link and its symbol. Several can be changed and saved at once.
 */
@Composable
fun SettingsLinksScreen(navController: NavController, onBack: () -> Unit, startElement: String? = null, viewModel: SettingsViewModel = hiltViewModel()) {
    val home: HomeViewModel = hiltViewModel()
    val stack: StackDraft = viewModel()
    val websites by viewModel.pinnedWebsites.collectAsState()
    var adding by remember { mutableStateOf(false) }
    val sorted = websites.sortedBy { it.label.lowercase() }
    fun shown(w: PinnedWebsite): PinnedWebsite {
        val item = DraftField(stack, linkKey(w.id) + ":item", w.copy(tileFace = null)).value
        return item.copy(tileFace = DraftField(stack, linkKey(w.id) + ":face", w.tileFace).value)
    }

    val elements = buildList {
        add(StackElement(
            id = "all", number = 0,
            title = stringResource(R.string.settings_elements_links_title),
            summary = androidx.compose.ui.res.pluralStringResource(R.plurals.hub_content_count, websites.size, websites.size)
        ) {
            PartIntro(stringResource(R.string.stack_links_intro))
            StackAddRow(stringResource(R.string.settings_elements_add_link)) { adding = true }
        })
        sorted.forEachIndexed { i, w ->
            val s = shown(w)
            add(StackElement(
                id = linkKey(w.id), number = i + 1,
                title = s.label,
                summary = s.url.takeIf { it.isNotBlank() },
                draftKeys = listOf(linkKey(w.id) + ":item", linkKey(w.id) + ":face"),
                anchor = androidx.compose.ui.geometry.Offset(1f, 0f),
                leading = { ElementTile(s.face(), s.label, 32.dp, androidx.compose.material3.MaterialTheme.colorScheme.onSurface, Icons.Filled.Language) }
            ) { LinkEditor(w, stack, navController, home, viewModel) })
        }
    }

    ElementCollectionScreen(
        title = stringResource(R.string.settings_elements_links_title),
        navController = navController,
        onBack = onBack,
        startElement = startElement,
        draft = stack,
        home = home,
        elements = elements,
        items = sorted.map { w -> val s = shown(w); CollectionItem(linkKey(w.id), s.label) { size, ink -> ElementTile(s.face(), s.label, size, ink, Icons.Filled.Language) } },
        listHeader = { StackAddRow(stringResource(R.string.settings_elements_add_link)) { adding = true } }
    )

    if (adding) {
        AddWebsiteDialog(
            onSave = { label, url -> viewModel.addWebsite(label, url); adding = false },
            onDismiss = { adding = false }
        )
    }
}

/** One link, the way in from outside: the stack, opened on it. */
@Composable
fun SettingsLinkDetailScreen(websiteId: Long, navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    SettingsLinksScreen(navController, onBack, startElement = linkKey(websiteId), viewModel = viewModel)
}

/** A link's options: name, address and the browser it opens in, where it is used; and its symbol. */
@Composable
private fun ColumnScope.LinkEditor(saved: PinnedWebsite, stack: StackDraft, navController: NavController, home: HomeViewModel, viewModel: SettingsViewModel) {
    val key = linkKey(saved.id)
    val item = stack.field("$key:item", saved.copy(tileFace = null)) {}
    val face = stack.field("$key:face", saved.tileFace) {}
    val website = item.value.copy(tileFace = face.value)
    val urlValid = com.bisc.flauncher.util.linkAddress(website.url) != null
    // A wrong address is not written; the field says what is wrong.
    stack.saveTogether(key) { if (website != saved && urlValid) viewModel.updateWebsite(website) }
    val browsableApps by viewModel.labeledApps.collectAsState()
    var showAppPicker by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val usages = rememberUsages(com.bisc.flauncher.data.model.GridItemType.WEBSITE, saved.id.toString(), home)

    StackTabs(listOf(
        stringResource(R.string.stack_el_link) to {
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
        stringResource(R.string.stack_el_symbol) to {
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
    ))

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
            onConfirm = { viewModel.removeWebsite(saved); confirmingDelete = false },
            onDismiss = { confirmingDelete = false }
        )
    }
}
