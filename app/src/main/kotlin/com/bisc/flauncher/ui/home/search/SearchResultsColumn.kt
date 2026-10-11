package com.bisc.flauncher.ui.home.search

import com.bisc.flauncher.ui.home.rememberAppIcon
import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.ContactInfo
import com.bisc.flauncher.data.model.Note
import com.bisc.flauncher.data.model.PinnedShortcut
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.data.model.SettingsShortcut
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.ui.theme.noRippleClickable
import com.bisc.flauncher.util.looksLikeEmail
import androidx.compose.ui.res.stringResource

private const val APP_GRID_COLUMNS = 4
private const val APP_GRID_COLLAPSED_ROWS = 2
private val FILTER_CHIP_SIZE = 36.dp
private val FILTER_CHIP_GAP = 6.dp

/** Search results grouped by category, with filter chips. */
@Composable
fun SearchResultsColumn(
    query: String,
    appResults: List<AppInfo>,
    calculatorResult: String?,
    contactResults: List<ContactInfo>,
    settingsResults: List<SettingsShortcut>,
    noteResults: List<Note>,
    websiteResults: List<PinnedWebsite>,
    shortcutResults: List<PinnedShortcut>,
    folderResults: List<com.bisc.flauncher.data.model.Folder> = emptyList(),
    mapTargetResults: List<com.bisc.flauncher.data.model.SavedMapTarget> = emptyList(),
    contactsPermissionGranted: Boolean,
    order: List<SearchCategory>,
    active: Set<SearchCategory>,
    filter: SearchCategory?,
    aiAppLabel: String?,
    onToggleFilter: (SearchCategory) -> Unit,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    onContactClick: (ContactInfo) -> Unit,
    onSettingsShortcutClick: (SettingsShortcut) -> Unit,
    onNoteClick: (Note) -> Unit,
    onFolderClick: (com.bisc.flauncher.data.model.Folder) -> Unit = {},
    onMapTargetClick: (com.bisc.flauncher.data.model.SavedMapTarget) -> Unit = {},
    onWebsiteClick: (PinnedWebsite) -> Unit,
    onShortcutClick: (PinnedShortcut) -> Unit,
    onWebCommand: () -> Unit,
    onMapsCommand: () -> Unit,
    onEmailCommand: () -> Unit,
    onAiCommand: () -> Unit,
    onCalculatorResultClick: (String) -> Unit,
    cardAppearance: ElementAppearance,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    var appsExpanded by remember(query) { mutableStateOf(false) }

    val shownAppResults = if (SearchCategory.APPS in active) appResults else emptyList()
    val shownContactResults = if (SearchCategory.CONTACTS in active && contactsPermissionGranted) contactResults else emptyList()
    val shownSettingsResults = if (SearchCategory.SETTINGS in active) settingsResults else emptyList()
    val shownNoteResults = if (SearchCategory.NOTES in active) noteResults else emptyList()
    val shownWebsiteResults = if (SearchCategory.WEBSITES in active) websiteResults else emptyList()
    val shownShortcutResults = if (SearchCategory.SHORTCUTS in active) shortcutResults else emptyList()
    val shownFolderResults = if (SearchCategory.FOLDERS in active) folderResults else emptyList()
    val shownMapTargetResults = if (SearchCategory.PLACES in active) mapTargetResults else emptyList()
    val showWeb = SearchCategory.WEB in active && query.isNotBlank()
    val showMaps = SearchCategory.PLACES in active && query.isNotBlank()
    val showAi = SearchCategory.AI in active && aiAppLabel != null && query.isNotBlank()

    val hasDirectResults = shownAppResults.isNotEmpty() || shownContactResults.isNotEmpty() || shownSettingsResults.isNotEmpty() ||
        shownNoteResults.isNotEmpty() || shownWebsiteResults.isNotEmpty() || shownShortcutResults.isNotEmpty() ||
        shownFolderResults.isNotEmpty() || shownMapTargetResults.isNotEmpty()
    val showEmail = SearchCategory.EMAIL in active && looksLikeEmail(query)

    var needsDivider = false
    fun LazyListScope.divider() {
        if (needsDivider) item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }
        needsDivider = true
    }

    SearchResultsPanel(appearance = cardAppearance, shape = shape, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (calculatorResult != null) {
                    divider()
                    item {
                        CalculatorResultRow(
                            expression = query,
                            result = calculatorResult,
                            onClick = { onCalculatorResultClick(calculatorResult) }
                        )
                    }
                }

                order.forEach { category ->
                    when (category) {
                        SearchCategory.APPS -> {
                            if (shownAppResults.isNotEmpty()) {
                                divider()
                                item { SectionTitle(stringResource(R.string.search_section_apps)) }
                                item {
                                    AppTileGrid(
                                        apps = shownAppResults,
                                        expanded = appsExpanded,
                                        loadIcon = loadIcon,
                                        onAppClick = onAppClick,
                                        onToggleExpand = { appsExpanded = !appsExpanded }
                                    )
                                }
                            }
                        }
                        SearchCategory.FOLDERS -> {
                            if (shownFolderResults.isNotEmpty()) {
                                divider()
                                item { SectionTitle(stringResource(R.string.search_section_folders)) }
                                items(shownFolderResults) { folder ->
                                    ResultRow(
                                        leading = { Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(28.dp)) },
                                        title = folder.name,
                                        onClick = { onFolderClick(folder) }
                                    )
                                }
                            }

                        }
                        SearchCategory.CONTACTS -> {
                            if (shownContactResults.isNotEmpty()) {
                                divider()
                                item { SectionTitle(stringResource(R.string.search_section_contacts)) }
                                items(shownContactResults) { contact ->
                                    ResultRow(
                                        leading = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(28.dp)) },
                                        title = contact.displayName,
                                        onClick = { onContactClick(contact) }
                                    )
                                }
                            }

                        }
                        SearchCategory.SETTINGS -> {
                            if (shownSettingsResults.isNotEmpty()) {
                                divider()
                                item { SectionTitle(stringResource(R.string.search_section_settings)) }
                                items(shownSettingsResults) { shortcut ->
                                    ResultRow(
                                        leading = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(28.dp)) },
                                        title = stringResource(shortcut.labelRes),
                                        onClick = { onSettingsShortcutClick(shortcut) }
                                    )
                                }
                            }

                        }
                        SearchCategory.NOTES -> {
                            if (shownNoteResults.isNotEmpty()) {
                                divider()
                                item { SectionTitle(stringResource(R.string.search_section_notes)) }
                                items(shownNoteResults) { note ->
                                    ResultRow(
                                        leading = { Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(28.dp)) },
                                        title = note.title,
                                        onClick = { onNoteClick(note) }
                                    )
                                }
                            }
                        }
                        SearchCategory.WEBSITES -> {
                            if (shownWebsiteResults.isNotEmpty()) {
                                divider()
                                item { SectionTitle(stringResource(R.string.search_section_websites)) }
                                items(shownWebsiteResults) { website ->
                                    ResultRow(
                                        leading = { Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(28.dp)) },
                                        title = website.label,
                                        onClick = { onWebsiteClick(website) }
                                    )
                                }
                            }
                        }
                        SearchCategory.PLACES -> {
                            if (shownMapTargetResults.isNotEmpty()) {
                                divider()
                                item { SectionTitle(stringResource(R.string.search_section_places)) }
                                items(shownMapTargetResults) { target ->
                                    ResultRow(
                                        leading = { Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(28.dp)) },
                                        title = target.label,
                                        subtitle = target.address.takeIf { it.isNotBlank() },
                                        onClick = { onMapTargetClick(target) }
                                    )
                                }
                            }

                            if (showMaps) {
                                divider()
                                item {
                                    CommandRow(
                                        icon = Icons.Filled.Place,
                                        text = stringResource(R.string.search_command_maps, query),
                                        prominent = false,
                                        onClick = onMapsCommand
                                    )
                                }
                            }
                        }
                        SearchCategory.SHORTCUTS -> {
                            if (shownShortcutResults.isNotEmpty()) {
                                divider()
                                item { SectionTitle(stringResource(R.string.search_section_shortcuts)) }
                                items(shownShortcutResults) { shortcut ->
                                    ResultRow(
                                        leading = { Icon(Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, modifier = Modifier.size(28.dp)) },
                                        title = shortcut.label,
                                        onClick = { onShortcutClick(shortcut) }
                                    )
                                }
                            }

                        }
                        SearchCategory.EMAIL -> {
                            if (showEmail) {
                                divider()
                                item {
                                    CommandRow(
                                        icon = Icons.Filled.Mail,
                                        text = stringResource(R.string.search_command_email, query),
                                        prominent = !hasDirectResults,
                                        onClick = onEmailCommand
                                    )
                                }
                            }
                        }
                        SearchCategory.WEB -> {
                            if (showWeb) {
                                divider()
                                item {
                                    CommandRow(
                                        icon = Icons.Filled.Language,
                                        text = stringResource(R.string.search_command_web, query),
                                        prominent = !hasDirectResults && !showEmail,
                                        onClick = onWebCommand
                                    )
                                }
                            }
                        }
                        SearchCategory.AI -> {
                            if (showAi) {
                                divider()
                                item {
                                    CommandRow(
                                        icon = SearchCategory.AI.icon,
                                        text = stringResource(R.string.search_command_ai, aiAppLabel.orEmpty(), query),
                                        prominent = !hasDirectResults && !showEmail && !showWeb,
                                        onClick = onAiCommand
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
            SearchFilterRow(
                categories = order.filter { it != SearchCategory.AI || aiAppLabel != null },
                selected = filter,
                onSelect = onToggleFilter
            )
        }
    }
}

@Composable
private fun SearchFilterRow(categories: List<SearchCategory>, selected: SearchCategory?, onSelect: (SearchCategory) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
        val fits = maxWidth >= FILTER_CHIP_SIZE * categories.size + FILTER_CHIP_GAP * (categories.size - 1)
        Row(
            modifier = Modifier.fillMaxWidth().then(if (fits) Modifier else Modifier.horizontalScroll(rememberScrollState())),
            horizontalArrangement = if (fits) Arrangement.SpaceEvenly else Arrangement.spacedBy(FILTER_CHIP_GAP),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                FilterChip(
                    icon = category.icon,
                    contentDescription = stringResource(category.filterLabelRes),
                    selected = category == selected,
                    anySelected = selected != null,
                    onClick = { onSelect(category) }
                )
            }
        }
    }
}

@Composable
private fun FilterChip(icon: ImageVector, contentDescription: String, selected: Boolean, anySelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(FILTER_CHIP_SIZE)
            .clip(CircleShape)
            .background(LocalContentColor.current.copy(alpha = if (selected) 0.16f else 0f))
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = LocalContentColor.current.copy(alpha = if (selected) 0.95f else if (anySelected) 0.4f else 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = LocalContentColor.current.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun AppTileGrid(
    apps: List<AppInfo>,
    expanded: Boolean,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    onToggleExpand: () -> Unit
) {
    val collapsedLimit = APP_GRID_COLUMNS * APP_GRID_COLLAPSED_ROWS
    val shown = if (expanded || apps.size <= collapsedLimit) apps else apps.take(collapsedLimit)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        shown.chunked(APP_GRID_COLUMNS).forEach { rowApps ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowApps.forEach { app ->
                    val bitmap = rememberAppIcon(app.componentName, loadIcon)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .noRippleClickable { onAppClick(app) }
                            .padding(vertical = 6.dp, horizontal = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (bitmap != null) {
                            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(48.dp))
                        } else {
                            Spacer(Modifier.size(48.dp))
                        }
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                repeat(APP_GRID_COLUMNS - rowApps.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (apps.size > collapsedLimit) {
            TextButton(onClick = onToggleExpand) {
                Text(
                    stringResource(
                        if (expanded) R.string.search_show_less else R.string.search_show_all
                    )
                )
            }
        }
    }
}

@Composable
private fun ResultRow(leading: @Composable () -> Unit, title: String, onClick: () -> Unit, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(title)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CalculatorResultRow(expression: String, result: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = expression.trim(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "= $result",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun CommandRow(icon: ImageVector, text: String, prominent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (prominent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = text,
            style = if (prominent) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        )
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
