package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.pageLabel
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.theme.noRippleClickable

@Composable
internal fun SettingsBreadcrumbBar(navController: NavController) {
    val backStack by navController.currentBackStack.collectAsState()
    val crumbs = backStack.filter { entry -> entry.destination.route?.startsWith("settings") == true }
    if (crumbs.size <= 1) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        crumbs.forEachIndexed { index, entry ->
            val isLast = index == crumbs.lastIndex
            val label = settingsCrumbLabel(entry)
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                color = if (isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (isLast) {
                    Modifier
                } else {
                    Modifier.noRippleClickable {
                        navController.popBackStack(entry.destination.id, inclusive = false)
                    }
                }
            )
            if (!isLast) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp).padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun settingsCrumbLabel(entry: NavBackStackEntry): String {
    return when (entry.destination.route) {
        "settings" -> stringResource(R.string.settings_title)
        "settings/appearance" -> stringResource(R.string.settings_section_appearance)
        "settings/appearance/background" -> stringResource(R.string.settings_section_global_background)
        "settings/sidebar" -> stringResource(R.string.settings_section_search_overview)
        "settings/search" -> stringResource(R.string.settings_section_search_full)
        "settings/pages" -> stringResource(R.string.settings_section_pages)
        "settings/pages/{pageId}" -> {
            val viewModel: HomeViewModel = hiltViewModel(entry)
            val pages by viewModel.pages.collectAsState()
            val pageId = entry.arguments?.getLong("pageId")
            val page = pages.find { it.id == pageId }
            page?.let { pageLabel(it, pages) } ?: stringResource(R.string.settings_rows_title)
        }
        "settings/pages/{pageId}/home" -> stringResource(R.string.settings_pages_home)
        "settings/pages/{pageId}/home/appearance" -> stringResource(R.string.settings_home_appearance_title)
        "settings/pages/{pageId}/home/layout" -> stringResource(R.string.settings_home_layout_title)
        "settings/pages/{pageId}/home/panel" -> stringResource(R.string.settings_home_panel_label)
        "settings/pages/{pageId}/home/panel/background" -> stringResource(R.string.settings_section_global_background)
        "settings/pages/{pageId}/home/panel/border" -> stringResource(R.string.settings_element_border_label)
        "settings/pages/{pageId}/home/panel/elements" -> stringResource(R.string.settings_dock_elements_title)
        "settings/pages/{pageId}/home/layout/dock" -> stringResource(R.string.settings_home_dock_label)
        "settings/pages/{pageId}/home/layout/dock/visibility" -> stringResource(R.string.settings_dock_visibility_label)
        "settings/pages/{pageId}/home/layout/dock/background" -> stringResource(R.string.settings_section_global_background)
        "settings/pages/{pageId}/home/layout/dock/border" -> stringResource(R.string.settings_element_border_label)
        "settings/pages/{pageId}/home/layout/dock/elements" -> stringResource(R.string.settings_dock_elements_title)
        "settings/pages/{pageId}/icon" -> stringResource(R.string.settings_page_icon_title)
        "settings/pages/{pageId}/rows/{rowId}" -> {
            val viewModel: HomeViewModel = hiltViewModel(entry)
            val rowsByPage by viewModel.rowsByPage.collectAsState()
            val pageId = entry.arguments?.getLong("pageId")
            val rowId = entry.arguments?.getLong("rowId")
            val row = rowsByPage[pageId]?.find { it.id == rowId }
            row?.let { rowLabel(it) } ?: stringResource(R.string.settings_widgets_edit_title)
        }
        "settings/pages/{pageId}/rows/{rowId}/apps" -> stringResource(R.string.settings_apps_row_edit_title)
        "settings/app_overview" -> stringResource(R.string.settings_section_app_overview)
        "settings/permissions" -> stringResource(R.string.settings_section_permissions)
        "settings/gestures" -> stringResource(R.string.settings_section_gestures)
        "settings/apps" -> stringResource(R.string.settings_section_manage_apps)
        "settings/folder" -> stringResource(R.string.settings_section_folder)
        "settings/folder/{folderId}" -> {
            val viewModel: SettingsViewModel = hiltViewModel(entry)
            val folders by viewModel.folders.collectAsState()
            val folderId = entry.arguments?.getLong("folderId")
            folders.find { it.id == folderId }?.name ?: stringResource(R.string.settings_folder_edit_title)
        }
        "settings/folder/{folderId}/icon" -> stringResource(R.string.settings_folder_icon_pack_choose_icon)
        "settings/elements" -> stringResource(R.string.settings_section_elements)
        "settings/elements/addresses" -> stringResource(R.string.settings_elements_addresses_title)
        "settings/elements/addresses/{targetId}" -> {
            val viewModel: SettingsViewModel = hiltViewModel(entry)
            val targets by viewModel.savedMapTargets.collectAsState()
            val targetId = entry.arguments?.getLong("targetId")
            targets.find { it.id == targetId }?.label ?: stringResource(R.string.settings_elements_addresses_title)
        }
        "settings/elements/links" -> stringResource(R.string.settings_elements_links_title)
        "settings/elements/links/{websiteId}" -> {
            val viewModel: SettingsViewModel = hiltViewModel(entry)
            val websites by viewModel.pinnedWebsites.collectAsState()
            val websiteId = entry.arguments?.getLong("websiteId")
            websites.find { it.id == websiteId }?.label ?: stringResource(R.string.settings_elements_links_title)
        }
        "settings/elements/powerbuttons" -> stringResource(R.string.settings_elements_powerbuttons_title)
        "settings/elements/powerbuttons/{buttonId}" -> {
            val viewModel: SettingsViewModel = hiltViewModel(entry)
            val buttons by viewModel.powerButtons.collectAsState()
            val buttonId = entry.arguments?.getLong("buttonId")
            buttons.find { it.id == buttonId }?.label ?: stringResource(R.string.settings_elements_powerbuttons_title)
        }
        else -> entry.destination.route.orEmpty()
    }
}
