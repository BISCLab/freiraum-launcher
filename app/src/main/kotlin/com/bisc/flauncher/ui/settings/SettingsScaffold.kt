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
import android.annotation.SuppressLint
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.pageLabel
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.theme.noRippleClickable

// currentBackStack is the only way to read the whole stack (there's no public equivalent), and the
// breadcrumbs need every settings entry on it, not just the current/previous one.
@SuppressLint("RestrictedApi")
@Composable
internal fun SettingsBreadcrumbBar(
    navController: NavController,
    /** Lets an editor with unsaved changes ask before a breadcrumb takes the user away. */
    guardLeave: (() -> Unit) -> Unit = { it() }
) {
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
                        guardLeave { navController.popBackStack(entry.destination.id, inclusive = false) }
                    }
                }
            )
            if (!isLast) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp).size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun settingsCrumbLabel(entry: NavBackStackEntry): String {
    return when (entry.destination.route) {
        "settings" -> stringResource(R.string.settings_title)
        "settings/appearance" -> stringResource(R.string.settings_design_title)
        "settings/extensions" -> stringResource(R.string.settings_group_extensions)
        "settings/appearance/menus" -> stringResource(R.string.settings_menus_title)
        "settings/extensions/widgets" -> stringResource(R.string.extensions_widgets_title)
        "settings/about" -> stringResource(R.string.about_title)
        "settings/appearance/background" -> stringResource(R.string.settings_section_global_background)
        "settings/search" -> stringResource(R.string.settings_section_search_full)
        "settings/search/results" -> stringResource(R.string.settings_search_results_label)
        "settings/search/field" -> stringResource(R.string.settings_app_grid_search_field)
        "settings/search/rail" -> stringResource(R.string.settings_search_rail_label)
        "settings/search/bubble" -> stringResource(R.string.settings_search_bubble_label)
        "settings/search/appearance" -> stringResource(R.string.settings_home_appearance_title)
        "settings/search/page" -> stringResource(R.string.settings_search_page_label)
        "settings/search/keyboard" -> stringResource(R.string.settings_search_keyboard_label)
        "settings/search/behavior" -> stringResource(R.string.settings_search_behavior_label)
        "settings/search/web" -> stringResource(R.string.settings_search_web_engines)
        "settings/search/ai" -> stringResource(R.string.settings_search_web_ai)
        "settings/search/favorites" -> stringResource(R.string.search_section_favorites)
        "settings/pages" -> stringResource(R.string.settings_section_pages)
        "settings/pages/{pageId}" -> {
            val viewModel: HomeViewModel = hiltViewModel(entry)
            val pages by viewModel.pages.collectAsState()
            val pageId = entry.arguments?.getLong("pageId")
            val page = pages.find { it.id == pageId }
            page?.let { pageLabel(it, pages) } ?: stringResource(R.string.settings_rows_title)
        }
        "settings/pages/{pageId}/home" -> stringResource(R.string.settings_pages_home)
        "settings/pages/{pageId}/appearance" -> stringResource(R.string.settings_home_appearance_title)
        "settings/pages/{pageId}/home/appearance" -> stringResource(R.string.settings_home_appearance_title)
        "settings/pages/{pageId}/home/layout" -> stringResource(R.string.settings_home_layout_title)
        "settings/pages/{pageId}/home/panel" -> stringResource(R.string.settings_home_panel_label)
        "settings/pages/{pageId}/home/panel/elements" -> stringResource(R.string.settings_panel_content_title)
        "settings/pages/{pageId}/home/panel/look" -> stringResource(R.string.settings_home_appearance_title)
        "settings/pages/{pageId}/home/layout/dock/look" -> stringResource(R.string.settings_home_appearance_title)
        "settings/pages/{pageId}/home/layout/dock" -> stringResource(R.string.settings_home_dock_label)
        "settings/pages/{pageId}/home/layout/dock/visibility" -> stringResource(R.string.settings_dock_visibility_label)
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
        "settings/pages/{pageId}/rows/{rowId}/elementbox" -> stringResource(R.string.settings_element_box_edit_title)
        "settings/app_overview" -> stringResource(R.string.settings_section_app_overview)
        "settings/app_overview/appearance" -> stringResource(R.string.settings_home_appearance_title)
        "settings/app_overview/page" -> stringResource(R.string.settings_search_page_label)
        "settings/app_overview/search_field" -> stringResource(R.string.settings_app_grid_search_field)
        "settings/app_grid" -> stringResource(R.string.settings_pages_app_grid)
        "settings/app_grid/appearance" -> stringResource(R.string.settings_home_appearance_title)
        "settings/app_grid/page" -> stringResource(R.string.settings_search_page_label)
        "settings/app_grid/grid" -> stringResource(R.string.settings_app_grid_grid_label)
        "settings/app_grid/search_field" -> stringResource(R.string.settings_app_grid_search_field)
        "settings/app_grid/behavior" -> stringResource(R.string.settings_search_behavior_label)
        "settings/appearance/designs" -> stringResource(R.string.designs_title)
        "settings/appearance/widget_paging" -> stringResource(R.string.settings_widget_paging_title)
        "settings/folder/{folderId}/widget/{itemId}" -> stringResource(R.string.settings_layer_surface_widget)
        "settings/app_overview/search_button" -> stringResource(R.string.settings_app_list_search_button)
        "settings/app_overview/behavior" -> stringResource(R.string.settings_search_behavior_label)
        "settings/app_overview/section_letters" -> stringResource(R.string.settings_layer_surface_section_letter)
        "settings/app_overview/az_index" -> stringResource(R.string.settings_app_overview_az_index_label)
        "settings/permissions" -> stringResource(R.string.settings_section_permissions)
        "settings/gestures" -> stringResource(R.string.settings_section_gestures)
        "settings/apps" -> stringResource(R.string.settings_section_manage_apps)
        "settings/apps/edit/{component}" -> stringResource(R.string.settings_manage_apps_edit_title)
        "settings/appearance/icons" -> stringResource(R.string.settings_icons_label)
        "settings/system" -> stringResource(R.string.settings_system_title)
        "settings/appearance/statusbar" -> stringResource(R.string.settings_statusbar_label)
        "settings/wallpaper_packs" -> stringResource(R.string.packs_title)
        "settings/appearance/corners" -> stringResource(R.string.settings_corners_all)
        "settings/appearance/colors" -> stringResource(R.string.settings_colors_all)
        "settings/appearance/animations" -> stringResource(R.string.settings_motion_label)
        "settings/folder" -> stringResource(R.string.settings_section_folder)
        "settings/folder/global" -> stringResource(R.string.settings_folder_global_label)
        "settings/folder/global/visibility" -> stringResource(R.string.settings_folder_visibility_label)
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
        "settings/elements/actionbuttons" -> stringResource(R.string.settings_elements_actionbuttons_title)
        "settings/elements/actionbuttons/{buttonId}" -> {
            val viewModel: SettingsViewModel = hiltViewModel(entry)
            val buttons by viewModel.actionButtons.collectAsState()
            val buttonId = entry.arguments?.getLong("buttonId")
            buttons.find { it.id == buttonId }?.label ?: stringResource(R.string.settings_elements_actionbuttons_title)
        }
        // Never the raw route: a page without a name of its own shows as an ellipsis.
        else -> "…"
    }
}
