package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.PageIconGlyph
import com.bisc.flauncher.ui.home.dock.PageIconGlyphImage
import com.bisc.flauncher.ui.home.dock.defaultColor
import com.bisc.flauncher.ui.pageLabel

/** Every page — including the singleton App overview/Search pages, no longer hidden/implicit —
 *  drag-reorderable (reordering only really applies to the swipeable ROWS pages; the two
 *  singletons just sort wherever their seed order put them). Tapping a page opens the settings
 *  that match its kind: a ROWS page's own rows editor, or the dedicated App overview/Search
 *  screens for those two. */
@Composable
fun SettingsPagesScreen(
    navController: NavController,
    onBack: () -> Unit,
    onOpenPage: (LauncherPage) -> Unit,
    onOpenIcon: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val pages by viewModel.allPages.collectAsState()
    val context = LocalContext.current
    var renamingPage by remember { mutableStateOf<LauncherPage?>(null) }

    SettingsSubScreen(title = stringResource(R.string.settings_section_pages), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            if (pages.isNotEmpty()) {
                ReorderableList(
                    items = pages,
                    key = { it.id },
                    onReorder = { reordered -> viewModel.reorderPages(reordered) }
                ) { page ->
                    val label = pageLabel(page, pages)
                    val glyph = page.iconGlyph?.let { runCatching { PageIconGlyph.valueOf(it) }.getOrNull() } ?: PageIconGlyph.forPageKind(page.kind)
                    val bg = page.iconBackgroundColor?.let { Color(it) } ?: glyph.defaultColor()
                    val tint = page.iconColor?.let { Color(it) } ?: Color.White
                    val removable = !page.isHome && !page.isAppOverview && page.kind != LauncherPageKind.SEARCH && page.kind != LauncherPageKind.SIDEBAR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPage(page) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .clickable { onOpenIcon(page.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            PageIconGlyphImage(glyph = glyph, tint = tint, modifier = Modifier.size(18.dp))
                        }
                        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { renamingPage = page }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.settings_pages_rename))
                        }
                        if (removable) {
                            IconButton(onClick = { viewModel.removePage(context, page) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.settings_pages_remove))
                            }
                        }
                    }
                }
            }
        }
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.settings_pages_add),
                onClick = viewModel::addPage,
                trailing = null
            )
        }
    }

    renamingPage?.let { page ->
        var name by remember(page.id) { mutableStateOf(page.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { renamingPage = null },
            title = { Text(stringResource(R.string.settings_pages_rename)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_pages_rename_hint)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renamePage(page.id, name)
                    renamingPage = null
                }) { Text(stringResource(R.string.settings_rows_save)) }
            },
            dismissButton = {
                TextButton(onClick = { renamingPage = null }) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }
}
