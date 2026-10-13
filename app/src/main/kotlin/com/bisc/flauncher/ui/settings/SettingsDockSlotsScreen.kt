package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.repository.DockRepository
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.DockSlotUi
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

/**
 * The dock's slots — max [DockRepository.MAX_SLOTS]. "Add app"/"Add page" are pure pickers: no
 * icon or label editing happens here, that's exclusively on the app itself (hidden/rename, see
 * Apps settings) or the page itself (its own Icon editor, see Pages settings) — one global place
 * per content type, never duplicated per picker.
 */
@Composable
fun SettingsDockSlotsScreen(
    navController: NavController,
    onBack: () -> Unit,
    initialEditSlot: Int? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val dockSlots by viewModel.dockSlots.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val allPages by viewModel.allPages.collectAsState()

    var pickingAppForSlot by remember { mutableStateOf<Int?>(null) }
    var pickingPageForSlot by remember { mutableStateOf<Int?>(null) }
    var addingPage by remember { mutableStateOf(false) }

    // Deep link from the home-screen dock's "+" — jump straight into this slot's picker. Guarded
    // so it only fires once (the slot list keeps changing after the pick, the whole point).
    var consumedInitialEditSlot by remember { mutableStateOf(false) }
    LaunchedEffect(dockSlots) {
        if (!consumedInitialEditSlot && initialEditSlot != null) {
            dockSlots.find { it.index == initialEditSlot }?.let { slot ->
                consumedInitialEditSlot = true
                when (slot) {
                    is DockSlotUi.App -> pickingAppForSlot = slot.index
                    is DockSlotUi.PageLink -> pickingPageForSlot = slot.index
                }
            }
        }
    }

    val atCap = dockSlots.size >= DockRepository.MAX_SLOTS

    SettingsSubScreen(title = stringResource(R.string.settings_dock_slots_label), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            dockSlots.forEach { slot ->
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_dock_slot_title, slot.index + 1),
                    summary = dockSlotSummary(slot, allPages),
                    onClick = {
                        when (slot) {
                            is DockSlotUi.App -> pickingAppForSlot = slot.index
                            is DockSlotUi.PageLink -> pickingPageForSlot = slot.index
                        }
                    },
                    trailing = {
                        Row {
                            IconButton(onClick = { viewModel.moveDockSlot(slot.index, -1) }) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.settings_dock_slot_move_up))
                            }
                            IconButton(onClick = { viewModel.moveDockSlot(slot.index, 1) }) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.settings_dock_slot_move_down))
                            }
                            IconButton(onClick = { viewModel.removeDockSlot(slot.index) }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.settings_dock_slot_remove))
                            }
                        }
                    }
                )
            }
        }

        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_dock_add_app_slot),
                info = if (atCap) stringResource(R.string.settings_dock_slots_at_cap, DockRepository.MAX_SLOTS) else null,
                onClick = { if (!atCap) viewModel.addDockAppSlot() },
                trailing = null
            )
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_dock_add_page_slot),
                info = if (atCap) stringResource(R.string.settings_dock_slots_at_cap, DockRepository.MAX_SLOTS) else null,
                onClick = { if (!atCap) addingPage = true },
                trailing = null
            )
        }
    }

    pickingAppForSlot?.let { index ->
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app ->
                viewModel.setDockAppSlot(index, app.componentName)
                pickingAppForSlot = null
            },
            onDismiss = { pickingAppForSlot = null }
        )
    }

    pickingPageForSlot?.let { index ->
        PagePickerSheet(
            pages = allPages,
            onPageSelected = { page ->
                viewModel.setDockPageSlot(index, page.id)
                pickingPageForSlot = null
            },
            onDismiss = { pickingPageForSlot = null }
        )
    }

    if (addingPage) {
        PagePickerSheet(
            pages = allPages,
            onPageSelected = { page ->
                viewModel.addDockPageSlot(page.id)
                addingPage = false
            },
            onDismiss = { addingPage = false }
        )
    }
}

@Composable
private fun dockSlotSummary(slot: DockSlotUi, pages: List<com.bisc.flauncher.data.model.LauncherPage>): String = when (slot) {
    is DockSlotUi.App -> slot.label ?: stringResource(R.string.settings_dock_slot_not_set)
    is DockSlotUi.PageLink -> slot.pageId?.let { id -> pages.find { it.id == id } }
        ?.let { page -> com.bisc.flauncher.ui.pageLabel(page, pages) }
        ?: stringResource(R.string.settings_dock_slot_not_set)
}
