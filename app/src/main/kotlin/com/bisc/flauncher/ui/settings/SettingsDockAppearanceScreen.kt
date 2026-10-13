package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.DockCornerStyle
import com.bisc.flauncher.ui.home.dock.DockWidthMode

/** The dock bar's own look: background, corner style, width mode — each its own sub-page. */
@Composable
fun SettingsDockAppearanceScreen(navController: NavController, onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val cornerStyle by viewModel.dockCornerStyle.collectAsState()
    val widthMode by viewModel.dockWidthMode.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val homePage = pages.find { it.isHome }
    val dockRow = homePage?.let { rowsByPage[it.id] }?.find { it.type == PageRowType.SEARCH_BAR }

    var showBackgroundSheet by remember { mutableStateOf(false) }
    var pickingCornerStyle by remember { mutableStateOf(false) }
    var pickingWidthMode by remember { mutableStateOf(false) }

    SettingsSubScreen(title = stringResource(R.string.settings_dock_appearance_label), navController = navController, onBack = onBack) {
        if (dockRow != null) {
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_dock_background_label),
                    onClick = { showBackgroundSheet = true },
                    trailing = null
                )
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_dock_corner_style_label),
                    summary = dockCornerStyleLabel(cornerStyle),
                    onClick = { pickingCornerStyle = true },
                    trailing = null
                )
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_dock_width_mode_label),
                    summary = dockWidthModeLabel(widthMode),
                    onClick = { pickingWidthMode = true },
                    trailing = null
                )
            }
        }
    }

    if (showBackgroundSheet && dockRow != null) {
        val backgroundLabel = stringResource(R.string.settings_dock_background_label)
        SettingsEditPage(title = backgroundLabel, onDismiss = { showBackgroundSheet = false }) {
            ElementAppearanceSection(
                appearance = dockRow.appearance(),
                onAppearanceChange = { appearance -> viewModel.setRowAppearance(dockRow, appearance) },
                styleGroupTitle = backgroundLabel
            )
        }
    }

    if (pickingCornerStyle) {
        DockCornerStyleSheet(
            current = cornerStyle,
            onSelected = { style -> viewModel.setDockCornerStyle(style); pickingCornerStyle = false },
            onDismiss = { pickingCornerStyle = false }
        )
    }

    if (pickingWidthMode) {
        DockWidthModeSheet(
            current = widthMode,
            onSelected = { mode -> viewModel.setDockWidthMode(mode); pickingWidthMode = false },
            onDismiss = { pickingWidthMode = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DockCornerStyleSheet(current: DockCornerStyle, onSelected: (DockCornerStyle) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            DockCornerStyle.entries.forEach { style ->
                RadioPreference(dockCornerStyleLabel(style), style, current, onSelected)
            }
        }
    }
}

@Composable
private fun dockCornerStyleLabel(style: DockCornerStyle): String = when (style) {
    DockCornerStyle.SHARP -> stringResource(R.string.settings_dock_corner_style_sharp)
    DockCornerStyle.ROUNDED -> stringResource(R.string.settings_dock_corner_style_rounded)
    DockCornerStyle.FULLY_ROUND -> stringResource(R.string.settings_dock_corner_style_fully_round)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DockWidthModeSheet(current: DockWidthMode, onSelected: (DockWidthMode) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            DockWidthMode.entries.forEach { mode ->
                RadioPreference(dockWidthModeLabel(mode), mode, current, onSelected)
            }
        }
    }
}

@Composable
private fun dockWidthModeLabel(mode: DockWidthMode): String = when (mode) {
    DockWidthMode.FIXED -> stringResource(R.string.settings_dock_width_mode_fixed)
    DockWidthMode.GROW -> stringResource(R.string.settings_dock_width_mode_grow)
}
