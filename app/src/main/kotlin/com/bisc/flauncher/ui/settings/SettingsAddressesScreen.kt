package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Place
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
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.model.face
import com.bisc.flauncher.ui.home.ConfirmRemoveDialog
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.AddMapTargetDialog
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

private fun placeKey(id: Long) = "place:$id"

/**
 * Every saved place in one card stack: ⓪ all of them with a way to add one, then each place with
 * its options in two tabs, the place and its symbol. Several can be changed and saved at once.
 */
@Composable
fun SettingsAddressesScreen(navController: NavController, onBack: () -> Unit, startElement: String? = null, viewModel: SettingsViewModel = hiltViewModel()) {
    val home: HomeViewModel = hiltViewModel()
    val stack: StackDraft = viewModel()
    val targets by viewModel.savedMapTargets.collectAsState()
    var adding by remember { mutableStateOf(false) }
    val sorted = targets.sortedBy { it.label.lowercase() }
    // What is drawn: the draft where there is one.
    fun shown(t: SavedMapTarget): SavedMapTarget {
        val item = DraftField(stack, placeKey(t.id) + ":item", t.copy(tileFace = null)).value
        return item.copy(tileFace = DraftField(stack, placeKey(t.id) + ":face", t.tileFace).value)
    }

    val elements = buildList {
        add(StackElement(
            id = "all", number = 0,
            title = stringResource(R.string.settings_elements_addresses_title),
            summary = androidx.compose.ui.res.pluralStringResource(R.plurals.hub_content_count, targets.size, targets.size)
        ) {
            PartIntro(stringResource(R.string.stack_places_intro))
            StackAddRow(stringResource(R.string.settings_elements_add_address)) { adding = true }
        })
        sorted.forEachIndexed { i, t ->
            val s = shown(t)
            add(StackElement(
                id = placeKey(t.id), number = i + 1,
                title = s.label,
                summary = s.address.takeIf { it.isNotBlank() },
                draftKeys = listOf(placeKey(t.id) + ":item", placeKey(t.id) + ":face"),
                anchor = androidx.compose.ui.geometry.Offset(1f, 0f),
                leading = { ElementTile(s.face(), s.label, 32.dp, androidx.compose.material3.MaterialTheme.colorScheme.onSurface, Icons.Filled.Place) }
            ) { PlaceEditor(t, stack, navController, home, viewModel) })
        }
    }

    ElementCollectionScreen(
        title = stringResource(R.string.settings_elements_addresses_title),
        navController = navController,
        onBack = onBack,
        startElement = startElement,
        draft = stack,
        home = home,
        elements = elements,
        items = sorted.map { t -> val s = shown(t); CollectionItem(placeKey(t.id), s.label) { size, ink -> ElementTile(s.face(), s.label, size, ink, Icons.Filled.Place) } },
        listHeader = { StackAddRow(stringResource(R.string.settings_elements_add_address)) { adding = true } }
    )

    if (adding) {
        AddMapTargetDialog(
            onSave = { label, lat, lon, address -> viewModel.addMapTarget(label, lat, lon, address); adding = false },
            onDismiss = { adding = false }
        )
    }
}

/** One place, the way in from outside: the stack, opened on it. */
@Composable
fun SettingsAddressDetailScreen(targetId: Long, navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    SettingsAddressesScreen(navController, onBack, startElement = placeKey(targetId), viewModel = viewModel)
}

/** A place's options: name, address and the app it opens in, where it is used; and its symbol. */
@Composable
private fun ColumnScope.PlaceEditor(saved: SavedMapTarget, stack: StackDraft, navController: NavController, home: HomeViewModel, viewModel: SettingsViewModel) {
    val key = placeKey(saved.id)
    val item = stack.field("$key:item", saved.copy(tileFace = null)) {}
    val face = stack.field("$key:face", saved.tileFace) {}
    val target = item.value.copy(tileFace = face.value)
    // Stored as one place.
    stack.saveTogether(key) { if (target != saved) viewModel.updateMapTarget(target) }
    val browsableApps by viewModel.labeledApps.collectAsState()
    var showAppPicker by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val usages = rememberUsages(com.bisc.flauncher.data.model.GridItemType.MAP_TARGET, saved.id.toString(), home)

    StackTabs(listOf(
        stringResource(R.string.stack_el_address) to {
            SettingsPreferenceGroup {
                OutlinedTextField(
                    value = target.label,
                    onValueChange = { item.value = item.value.copy(label = it) },
                    label = { Text(stringResource(R.string.sidebar_maps_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OutlinedTextField(
                    value = target.address,
                    onValueChange = { item.value = item.value.copy(address = it) },
                    label = { Text(stringResource(R.string.settings_elements_address_hint)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_elements_navigation_app_label)) {
                val appLabel = target.navigationApp?.let { pkg -> browsableApps.find { it.componentName.packageName == pkg }?.label }
                SettingsPreference(
                    icon = null,
                    title = appLabel ?: stringResource(R.string.settings_elements_app_default),
                    onClick = { showAppPicker = true },
                    trailing = if (target.navigationApp != null) {
                        { TextButton(onClick = { item.value = item.value.copy(navigationApp = null) }) { Text(stringResource(R.string.settings_elements_use_default)) } }
                    } else null
                )
            }
            UsagesGroup(usages, navController)
            SettingsPreferenceGroup {
                SettingsPreference(icon = Icons.Filled.DeleteOutline, title = stringResource(R.string.settings_elements_delete_address), onClick = { confirmingDelete = true }, trailing = null)
            }
        },
        stringResource(R.string.stack_el_symbol) to {
            PartIntro(stringResource(R.string.element_tile_info))
            TileFaceEditor(
                face = target.face(),
                onChange = { face.value = it.toJson() },
                autoSurface = com.bisc.flauncher.ui.home.dock.UNSET_APP_SLOT_COLOR,
                letter = target.label,
                content = emptyList(),
                contentSummary = "",
                viewModel = viewModel,
                kinds = listOf(com.bisc.flauncher.data.model.TileKind.SYMBOL, com.bisc.flauncher.data.model.TileKind.LETTER),
                defaultSymbol = Icons.Filled.Place
            )
        }
    ))

    if (showAppPicker) {
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app -> item.value = item.value.copy(navigationApp = app.componentName.packageName); showAppPicker = false },
            onDismiss = { showAppPicker = false }
        )
    }
    if (confirmingDelete) {
        ConfirmRemoveDialog(
            label = target.label,
            onConfirm = { viewModel.removeMapTarget(saved); confirmingDelete = false },
            onDismiss = { confirmingDelete = false }
        )
    }
}
