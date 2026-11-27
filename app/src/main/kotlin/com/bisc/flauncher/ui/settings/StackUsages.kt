package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.ui.home.HomeViewModel

/** One place a folder or category is used, and the settings page where it sits. */
internal data class Usage(val label: String, val route: String)

/**
 * Where an element ([type] with [payload], e.g. a folder's id) is placed: in the Dock, the Panel, a
 * row of a page, the App grid or another folder. [extra] adds places that always show it.
 */
@Composable
internal fun rememberUsages(type: GridItemType, payload: String, home: HomeViewModel, extra: List<Usage> = emptyList()): List<Usage> {
    val rowsByPage by home.rowsByPage.collectAsState()
    val entriesByRow by home.rowEntriesByRow.collectAsState()
    val pages by home.allPages.collectAsState()
    val folders by home.folders.collectAsState()
    val folderItems by home.folderItemsByFolder.collectAsState()
    val gridFolders by home.appGridFolderIds.collectAsState()
    val dock = stringResource(R.string.settings_home_dock_label)
    val panel = stringResource(R.string.settings_home_panel_label)
    val rowWord = stringResource(R.string.stack_usage_row)
    val inFolder = stringResource(R.string.stack_usage_in_folder)
    val grid = stringResource(R.string.settings_pages_app_grid)
    val out = mutableListOf<Usage>()
    rowsByPage.forEach { (pageId, rows) ->
        val page = pages.find { it.id == pageId } ?: return@forEach
        val name = com.bisc.flauncher.ui.pageLabel(page, pages)
        val boxes = rows.filter { it.type == PageRowType.ELEMENT_BOX }
        rows.forEach { row ->
            if (entriesByRow[row.id].orEmpty().none { it.type == type && it.payload == payload }) return@forEach
            out += when (row.type) {
                PageRowType.SEARCH_BAR -> Usage(dock, "settings/pages/$pageId/home/layout/dock/elements")
                PageRowType.PANEL -> Usage(panel, "settings/pages/$pageId/home/panel/elements")
                else -> Usage("$name · $rowWord ${boxes.indexOf(row) + 1}", "settings/pages/$pageId/rows/${row.id}/elementbox")
            }
        }
    }
    if (type == GridItemType.FOLDER && payload.toLongOrNull() in gridFolders && pages.any { it.kind == LauncherPageKind.APP_GRID }) {
        out += Usage(grid, "settings/app_grid")
    }
    folderItems.forEach { (folderId, items) ->
        if (items.any { it.type == type && it.payload == payload }) {
            folders.find { it.id == folderId }?.let { out += Usage("$inFolder ${it.name}", "settings/folder/$folderId") }
        }
    }
    return extra + out.distinct()
}

/** The places as a list, each a way there; with none, a line that says so. */
@Composable
internal fun UsagesGroup(usages: List<Usage>, navController: NavController) {
    SettingsPreferenceGroup(title = stringResource(R.string.stack_usage_title)) {
        if (usages.isEmpty()) {
            SettingsPreference(icon = null, title = stringResource(R.string.stack_usage_none), onClick = null, trailing = null)
        }
        usages.forEach { u ->
            SettingsPreference(
                icon = null,
                title = u.label,
                onClick = { navController.navigate(u.route) },
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
            )
        }
    }
}
