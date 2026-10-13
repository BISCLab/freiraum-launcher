package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.home.HomeViewModel

/**
 * Dock hub: just two cards, each its own full-screen destination (Slots, Appearance) — every
 * setting gets its own sub-page per the breadcrumb pattern, instead of everything living in one
 * long scroll. The dock's slots are either pinned apps or links to a page (see
 * [SettingsDockSlotsScreen]) — text search/app overview/any other page live under **Pages** now,
 * not here.
 */
@Composable
fun SettingsDockScreen(navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val dockSlots by viewModel.dockSlots.collectAsState()

    SettingsSubScreen(title = stringResource(R.string.settings_section_dock), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.ViewModule,
                title = stringResource(R.string.settings_dock_slots_label),
                summary = stringResource(R.string.settings_dock_slots_summary, dockSlots.size, com.bisc.flauncher.data.repository.DockRepository.MAX_SLOTS),
                onClick = { navController.navigate("settings/dock/slots") },
                trailing = null
            )
            SettingsPreference(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.settings_dock_appearance_label),
                onClick = { navController.navigate("settings/dock/appearance") },
                trailing = null
            )
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.AutoMirrored.Filled.ViewList,
                title = stringResource(R.string.settings_section_search_overview),
                summary = stringResource(R.string.settings_card_search_overview_subtitle),
                onClick = { navController.navigate("settings/dock/overview") },
                trailing = null
            )
        }
    }
}
