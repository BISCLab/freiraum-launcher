package com.bisc.flauncher.ui.home.dock

import com.bisc.flauncher.ui.home.NoteReadContent
import com.bisc.flauncher.ui.home.rememberAppIcon
import android.Manifest
import android.content.ComponentName
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.home.ConfirmRemoveDialog
import com.bisc.flauncher.ui.home.NoteEditIconButton
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.Note
import com.bisc.flauncher.data.model.NoteType
import com.bisc.flauncher.data.model.checklistItemsToNoteContent
import com.bisc.flauncher.data.model.noteContentToChecklistItems
import com.bisc.flauncher.data.model.PinnedContact
import com.bisc.flauncher.data.model.PinnedShortcut
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.model.SidebarCategoryViewMode
import com.bisc.flauncher.data.model.SidebarPinnedApp
import com.bisc.flauncher.ui.pageLabel
import com.bisc.flauncher.ui.home.FolderIcon
import com.bisc.flauncher.ui.home.NoteContentEditor
import com.bisc.flauncher.ui.home.NoteTypeToggle
import com.bisc.flauncher.ui.theme.noRippleClickable
import kotlinx.coroutines.delay

private val OVERVIEW_LIST_CONTENT_PADDING = PaddingValues(vertical = 8.dp)
private val OVERVIEW_ROW_HEIGHT_MIN = 44.dp
private const val OVERVIEW_GRID_COLUMNS = 3
private val OVERVIEW_LIST_ICON_SIZE = 28.dp
private val OVERVIEW_GRID_ICON_SIZE = 52.dp

/** Grid mode has room for a bigger icon than a list row does. */
private fun overviewIconSize(viewMode: SidebarCategoryViewMode) =
    if (viewMode == SidebarCategoryViewMode.GRID) OVERVIEW_GRID_ICON_SIZE else OVERVIEW_LIST_ICON_SIZE

internal fun overviewRowHeight(availableHeight: Dp, appCount: Int): Dp {
    if (appCount <= 0) return OVERVIEW_ROW_HEIGHT_MIN
    val verticalPadding = OVERVIEW_LIST_CONTENT_PADDING.calculateTopPadding() + OVERVIEW_LIST_CONTENT_PADDING.calculateBottomPadding()
    return ((availableHeight - verticalPadding) / appCount).coerceAtLeast(OVERVIEW_ROW_HEIGHT_MIN)
}

/** The category content shown on the content side of [SidebarOverviewPanel]. */
@Composable
fun OverviewContent(
    entry: SidebarEntryType,
    pages: List<LauncherPage>,
    installedApps: List<AppInfo>,
    sidebarPinnedApps: List<SidebarPinnedApp>,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    notes: List<Note>,
    savedMapTargets: List<SavedMapTarget>,
    pinnedWebsites: List<PinnedWebsite>,
    pinnedShortcuts: List<PinnedShortcut>,
    hasShortcutPermission: Boolean,
    rowHeight: Dp,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    onAppClick: (AppInfo) -> Unit,
    onPageClick: (LauncherPage) -> Unit,
    onFolderClick: (Folder) -> Unit,
    onAddFolderClick: () -> Unit,
    onSaveNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onCreateNote: ((Long) -> Unit) -> Unit,
    onMapTargetClick: (SavedMapTarget) -> Unit,
    onRemoveMapTarget: (SavedMapTarget) -> Unit,
    onAddMapTargetClick: () -> Unit,
    onWebsiteClick: (PinnedWebsite) -> Unit,
    onRemoveWebsite: (PinnedWebsite) -> Unit,
    onAddWebsiteClick: () -> Unit,
    onShortcutClick: (PinnedShortcut) -> Unit,
    onRemoveShortcut: (PinnedShortcut) -> Unit,
    onAddShortcutClick: () -> Unit,
    onAddSidebarAppsClick: () -> Unit,
    onRemoveSidebarApp: (SidebarPinnedApp) -> Unit,
    pinnedContacts: List<PinnedContact>,
    hasContactsPermission: Boolean,
    onContactClick: (PinnedContact) -> Unit,
    onRemoveContact: (PinnedContact) -> Unit,
    onAddContactClick: () -> Unit,
    onContactsPermissionResult: (Boolean) -> Unit,
    loadContactPhoto: suspend (Long, String?) -> ImageBitmap?
) {
    when (entry) {
        SidebarEntryType.APPS -> SidebarAppsOverviewContent(
            installedApps, sidebarPinnedApps, loadIcon, onAppClick, onAddSidebarAppsClick, onRemoveSidebarApp, viewMode, showAddRow, rowHeight
        )
        SidebarEntryType.PAGES -> PagesOverviewContent(pages, onPageClick, rowHeight)
        SidebarEntryType.FOLDERS -> FoldersOverviewContent(
            folders, folderItemsByFolder, loadIcon, loadPackIcon, onFolderClick, onAddFolderClick, viewMode, showAddRow, rowHeight
        )
        SidebarEntryType.NOTES -> NotesOverviewContent(notes, onSaveNote, onDeleteNote, onCreateNote, rowHeight)
        SidebarEntryType.WEBSITES -> WebsitesOverviewContent(
            pinnedWebsites, onWebsiteClick, onRemoveWebsite, onAddWebsiteClick, viewMode, showAddRow, rowHeight
        )
        SidebarEntryType.MAPS -> MapsOverviewContent(
            savedMapTargets, onMapTargetClick, onRemoveMapTarget, onAddMapTargetClick, viewMode, showAddRow, rowHeight
        )
        SidebarEntryType.SHORTCUTS -> ShortcutsOverviewContent(
            pinnedShortcuts, hasShortcutPermission, onShortcutClick, onRemoveShortcut, onAddShortcutClick, viewMode, showAddRow, rowHeight
        )
        SidebarEntryType.CONTACTS -> ContactsOverviewContent(
            pinnedContacts, hasContactsPermission, onContactClick, onRemoveContact, onAddContactClick, onContactsPermissionResult,
            loadContactPhoto, viewMode, showAddRow, rowHeight
        )
    }
}

@Composable
private fun WholeRowsLazyColumn(
    rowHeight: Dp,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = OVERVIEW_LIST_CONTENT_PADDING,
    content: LazyListScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val verticalPadding = contentPadding.calculateTopPadding() + contentPadding.calculateBottomPadding()
        val wholeRows = ((maxHeight - verticalPadding) / rowHeight).toInt().coerceAtLeast(0)
        val cappedHeight = (verticalPadding + rowHeight * wholeRows).coerceAtMost(maxHeight)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(cappedHeight),
            contentPadding = contentPadding,
            content = content
        )
    }
}

@Composable
private fun SidebarAppsOverviewContent(
    installedApps: List<AppInfo>,
    pinnedApps: List<SidebarPinnedApp>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    onAddAppsClick: () -> Unit,
    onRemove: (SidebarPinnedApp) -> Unit,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    rowHeight: Dp
) {
    val appsByComponent = remember(installedApps) { installedApps.associateBy { it.componentName } }
    val onAppClickLatest by rememberUpdatedState(onAppClick)
    val onRemoveLatest by rememberUpdatedState(onRemove)
    val loadIconLatest by rememberUpdatedState(loadIcon)
    val iconSize = overviewIconSize(viewMode)
    val entries = remember(pinnedApps, appsByComponent, iconSize) {
        pinnedApps.map { pinned ->
            val componentName = ComponentName.unflattenFromString(pinned.componentName)
            val app = componentName?.let { appsByComponent[it] }
            CategoryEntry(
                key = pinned.id,
                label = app?.label ?: pinned.componentName,
                onClick = { app?.let(onAppClickLatest) },
                onRemove = { onRemoveLatest(pinned) },
                icon = {
                    val bitmap = rememberAppIcon(componentName, loadIconLatest)
                    if (bitmap != null) {
                        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(iconSize))
                    }
                }
            )
        }
    }
    CategoryOverviewList(
        entries = entries,
        rowHeight = rowHeight,
        viewMode = viewMode,
        emptyLabelRes = R.string.sidebar_apps_empty,
        addLabelRes = R.string.sidebar_apps_add.takeIf { showAddRow },
        onAddClick = onAddAppsClick.takeIf { showAddRow }
    )
}

@Composable
private fun PagesOverviewContent(pages: List<LauncherPage>, onPageClick: (LauncherPage) -> Unit, rowHeight: Dp) {
    WholeRowsLazyColumn(rowHeight = rowHeight) {
        items(pages, key = { it.id }) { page ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .noRippleClickable { onPageClick(page) }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = pageLabel(page, pages), color = LocalContentColor.current.copy(alpha = 0.87f))
            }
        }
    }
}

@Composable
private fun FoldersOverviewContent(
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap?,
    onFolderClick: (Folder) -> Unit,
    onAddFolderClick: () -> Unit,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    rowHeight: Dp
) {
    val onFolderClickLatest by rememberUpdatedState(onFolderClick)
    val loadIconLatest by rememberUpdatedState(loadIcon)
    val loadPackIconLatest by rememberUpdatedState(loadPackIcon)
    val iconSize = overviewIconSize(viewMode)
    val entries = remember(folders, folderItemsByFolder, iconSize) {
        folders.map { folder ->
            CategoryEntry(
                key = folder.id,
                label = folder.name,
                onClick = { onFolderClickLatest(folder) },
                icon = {
                    FolderIcon(
                        folder = folder,
                        previewItems = folderItemsByFolder[folder.id].orEmpty(),
                        loadIcon = loadIconLatest,
                        loadPackIcon = loadPackIconLatest,
                        modifier = Modifier.size(iconSize)
                    )
                }
            )
        }
    }
    CategoryOverviewList(
        entries = entries,
        rowHeight = rowHeight,
        viewMode = viewMode,
        emptyLabelRes = R.string.folder_overview_empty,
        addLabelRes = R.string.folder_add.takeIf { showAddRow },
        onAddClick = onAddFolderClick.takeIf { showAddRow }
    )
}

@Composable
private fun NotesOverviewContent(
    notes: List<Note>,
    onSaveNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onCreateNote: ((Long) -> Unit) -> Unit,
    rowHeight: Dp
) {
    var expandedNoteId by remember { mutableStateOf<Long?>(null) }
    var editingNoteId by remember { mutableStateOf<Long?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        contentPadding = OVERVIEW_LIST_CONTENT_PADDING
    ) {
        if (notes.isEmpty()) {
            item(key = "empty") {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.sidebar_notes_empty), color = LocalContentColor.current.copy(alpha = 0.5f))
                }
            }
        }
        items(notes, key = { "note_${it.id}" }) { note ->
            NoteOverviewItem(
                note = note,
                expanded = expandedNoteId == note.id,
                editing = editingNoteId == note.id,
                onToggleExpand = {
                    editingNoteId = null
                    expandedNoteId = if (expandedNoteId == note.id) null else note.id
                },
                onEdit = { editingNoteId = note.id },
                onDoneEditing = { editingNoteId = null },
                onSave = onSaveNote,
                onDelete = {
                    onDeleteNote(note)
                    if (expandedNoteId == note.id) expandedNoteId = null
                    if (editingNoteId == note.id) editingNoteId = null
                },
                rowHeight = rowHeight
            )
        }
        item(key = "add_note") {
            AddRow(
                labelRes = R.string.sidebar_notes_add,
                onClick = { onCreateNote { id -> expandedNoteId = id; editingNoteId = id } },
                rowHeight = rowHeight
            )
        }
    }
}

@Composable
private fun NoteOverviewItem(
    note: Note,
    expanded: Boolean,
    editing: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDoneEditing: () -> Unit,
    onSave: (Note) -> Unit,
    onDelete: () -> Unit,
    rowHeight: Dp
) {
    var confirmingDelete by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .combinedClickable(onClick = onToggleExpand, onLongClick = { confirmingDelete = true })
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (note.type == NoteType.CHECKLISTE) Icons.Filled.Checklist else Icons.Filled.EditNote,
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
            Text(
                note.title.ifBlank { stringResource(R.string.widget_notes_untitled) },
                color = LocalContentColor.current.copy(alpha = 0.87f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp).weight(1f)
            )
            if (note.type == NoteType.CHECKLISTE && note.items.isNotEmpty()) {
                Text(
                    stringResource(R.string.widget_notes_checklist_progress, note.items.count { it.checked }, note.items.size),
                    color = LocalContentColor.current.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (expanded && !editing) NoteEditIconButton(onEdit)
        }
        if (expanded && editing) {
            NoteInlineEditor(note = note, onSave = onSave)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDoneEditing) { Text(stringResource(R.string.widget_notes_done)) }
            }
        } else if (expanded) {
            NoteReadContent(
                note = note,
                onToggleItem = { index ->
                    onSave(note.copy(items = note.items.toMutableList().also { it[index] = it[index].copy(checked = !it[index].checked) }))
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                showTitle = false
            )
        }
    }
    if (confirmingDelete) {
        ConfirmRemoveDialog(
            label = note.title.ifBlank { stringResource(R.string.widget_notes_untitled) },
            onConfirm = { confirmingDelete = false; onDelete() },
            onDismiss = { confirmingDelete = false }
        )
    }
}

@Composable
private fun NoteInlineEditor(note: Note, onSave: (Note) -> Unit) {
    var dirty by remember(note.id) { mutableStateOf(false) }
    var title by remember(note.id) { mutableStateOf(note.title) }
    var type by remember(note.id) { mutableStateOf(note.type) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    var items by remember(note.id) { mutableStateOf(note.items) }

    LaunchedEffect(title, type, content, items) {
        if (!dirty) return@LaunchedEffect
        delay(400)
        onSave(note.copy(title = title, type = type, content = content, items = items, updatedAt = System.currentTimeMillis()))
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; dirty = true },
            label = { Text(stringResource(R.string.widget_notes_title_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        NoteTypeToggle(
            type = type,
            onTypeSelected = { selected ->
                if (selected != type) {
                    if (selected == NoteType.CHECKLISTE) items = noteContentToChecklistItems(content)
                    else content = checklistItemsToNoteContent(items)
                    type = selected
                    dirty = true
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        )
        NoteContentEditor(
            type = type,
            content = content,
            items = items,
            onContentChange = { content = it; dirty = true },
            onItemsChange = { items = it; dirty = true },
            contentMinHeight = 72.dp
        )
    }
}

@Composable
private fun WebsitesOverviewContent(
    websites: List<PinnedWebsite>,
    onWebsiteClick: (PinnedWebsite) -> Unit,
    onRemoveWebsite: (PinnedWebsite) -> Unit,
    onAddWebsiteClick: () -> Unit,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    rowHeight: Dp
) {
    val onClickLatest by rememberUpdatedState(onWebsiteClick)
    val onRemoveLatest by rememberUpdatedState(onRemoveWebsite)
    val iconSize = overviewIconSize(viewMode)
    val entries = remember(websites, iconSize) {
        websites.map { website ->
            CategoryEntry(
                key = "site_${website.id}",
                label = website.label,
                onClick = { onClickLatest(website) },
                onRemove = { onRemoveLatest(website) },
                icon = { Icon(Icons.Filled.Language, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f), modifier = Modifier.size(iconSize)) }
            )
        }
    }
    CategoryOverviewList(
        entries = entries,
        rowHeight = rowHeight,
        viewMode = viewMode,
        emptyLabelRes = R.string.sidebar_websites_empty,
        addLabelRes = R.string.sidebar_website_add.takeIf { showAddRow },
        onAddClick = onAddWebsiteClick.takeIf { showAddRow }
    )
}

@Composable
private fun MapsOverviewContent(
    targets: List<SavedMapTarget>,
    onTargetClick: (SavedMapTarget) -> Unit,
    onRemoveTarget: (SavedMapTarget) -> Unit,
    onAddClick: () -> Unit,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    rowHeight: Dp
) {
    val onClickLatest by rememberUpdatedState(onTargetClick)
    val onRemoveLatest by rememberUpdatedState(onRemoveTarget)
    val iconSize = overviewIconSize(viewMode)
    val entries = remember(targets, iconSize) {
        targets.map { target ->
            CategoryEntry(
                key = target.id,
                label = target.label,
                onClick = { onClickLatest(target) },
                onRemove = { onRemoveLatest(target) },
                icon = { Icon(Icons.Filled.Place, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f), modifier = Modifier.size(iconSize)) }
            )
        }
    }
    CategoryOverviewList(
        entries = entries,
        rowHeight = rowHeight,
        viewMode = viewMode,
        emptyLabelRes = R.string.sidebar_maps_empty,
        addLabelRes = R.string.sidebar_maps_add.takeIf { showAddRow },
        onAddClick = onAddClick.takeIf { showAddRow }
    )
}

@Composable
private fun ShortcutsOverviewContent(
    shortcuts: List<PinnedShortcut>,
    hasPermission: Boolean,
    onShortcutClick: (PinnedShortcut) -> Unit,
    onRemoveShortcut: (PinnedShortcut) -> Unit,
    onAddClick: () -> Unit,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    rowHeight: Dp
) {
    if (!hasPermission) {
        WholeRowsLazyColumn(rowHeight = rowHeight) {
            item(key = "no_permission") {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.sidebar_shortcuts_needs_default),
                        color = LocalContentColor.current.copy(alpha = 0.5f)
                    )
                }
            }
        }
        return
    }
    val onClickLatest by rememberUpdatedState(onShortcutClick)
    val onRemoveLatest by rememberUpdatedState(onRemoveShortcut)
    val iconSize = overviewIconSize(viewMode)
    val entries = remember(shortcuts, iconSize) {
        shortcuts.map { shortcut ->
            CategoryEntry(
                key = shortcut.id,
                label = shortcut.label,
                onClick = { onClickLatest(shortcut) },
                onRemove = { onRemoveLatest(shortcut) },
                icon = { Icon(Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f), modifier = Modifier.size(iconSize)) }
            )
        }
    }
    CategoryOverviewList(
        entries = entries,
        rowHeight = rowHeight,
        viewMode = viewMode,
        emptyLabelRes = R.string.sidebar_shortcuts_empty,
        addLabelRes = R.string.sidebar_shortcuts_add.takeIf { showAddRow },
        onAddClick = onAddClick.takeIf { showAddRow }
    )
}

@Composable
private fun ContactsOverviewContent(
    contacts: List<PinnedContact>,
    hasPermission: Boolean,
    onContactClick: (PinnedContact) -> Unit,
    onRemoveContact: (PinnedContact) -> Unit,
    onAddContactClick: () -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    loadContactPhoto: suspend (Long, String?) -> ImageBitmap?,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    rowHeight: Dp
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onPermissionResult(granted) }

    if (!hasPermission) {
        WholeRowsLazyColumn(rowHeight = rowHeight) {
            item(key = "no_permission") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.sidebar_contacts_needs_permission),
                        color = LocalContentColor.current.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                        Text(stringResource(R.string.sidebar_contacts_allow_access))
                    }
                }
            }
        }
        return
    }
    val onClickLatest by rememberUpdatedState(onContactClick)
    val onRemoveLatest by rememberUpdatedState(onRemoveContact)
    val loadPhotoLatest by rememberUpdatedState(loadContactPhoto)
    val iconSize = overviewIconSize(viewMode)
    val entries = remember(contacts, iconSize) {
        contacts.map { contact ->
            CategoryEntry(
                key = contact.id,
                label = contact.displayName,
                onClick = { onClickLatest(contact) },
                onRemove = { onRemoveLatest(contact) },
                icon = {
                    ContactPhotoIcon(
                        contactId = contact.contactId,
                        photoUri = contact.photoUri,
                        loadPhoto = { id, uri -> loadPhotoLatest(id, uri) },
                        size = iconSize
                    )
                }
            )
        }
    }
    CategoryOverviewList(
        entries = entries,
        rowHeight = rowHeight,
        viewMode = viewMode,
        emptyLabelRes = R.string.sidebar_contacts_empty,
        addLabelRes = R.string.sidebar_contacts_add.takeIf { showAddRow },
        onAddClick = onAddContactClick.takeIf { showAddRow }
    )
}

/** Contact photo, loaded lazily. */
@Composable
fun ContactPhotoIcon(contactId: Long, photoUri: String?, loadPhoto: suspend (Long, String?) -> ImageBitmap?, size: Dp = 28.dp) {
    var photo by remember(contactId) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(contactId, photoUri) { photo = loadPhoto(contactId, photoUri) }
    val loaded = photo
    if (loaded != null) {
        Image(bitmap = loaded, contentDescription = null, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        Icon(Icons.Filled.Person, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f), modifier = Modifier.size(size))
    }
}

private data class CategoryEntry(
    val key: Any,
    val label: String,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
    val onRemove: (() -> Unit)? = null
)

@Composable
private fun CategoryOverviewList(
    entries: List<CategoryEntry>,
    rowHeight: Dp,
    viewMode: SidebarCategoryViewMode,
    emptyLabelRes: Int?,
    addLabelRes: Int?,
    onAddClick: (() -> Unit)?
) {
    // Long-press removal asks first, instead of the item just disappearing.
    var pendingRemoval by remember { mutableStateOf<CategoryEntry?>(null) }
    when (viewMode) {
        SidebarCategoryViewMode.LIST -> WholeRowsLazyColumn(rowHeight = rowHeight) {
            if (entries.isEmpty() && emptyLabelRes != null) {
                item(key = "empty") {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(emptyLabelRes), color = LocalContentColor.current.copy(alpha = 0.5f))
                    }
                }
            }
            items(entries, key = { it.key }) { entry ->
                CategoryListRow(entry, rowHeight, onRequestRemove = { pendingRemoval = entry })
            }
            if (addLabelRes != null && onAddClick != null) {
                item(key = "add") { AddRow(labelRes = addLabelRes, onClick = onAddClick, rowHeight = rowHeight) }
            }
        }
        SidebarCategoryViewMode.GRID -> CategoryGrid(entries, emptyLabelRes, addLabelRes, onAddClick) { entry -> pendingRemoval = entry }
    }
    pendingRemoval?.let { entry ->
        ConfirmRemoveDialog(
            label = entry.label,
            onConfirm = { entry.onRemove?.invoke(); pendingRemoval = null },
            onDismiss = { pendingRemoval = null }
        )
    }
}

@Composable
private fun CategoryListRow(entry: CategoryEntry, rowHeight: Dp, onRequestRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .combinedClickable(onClick = entry.onClick, onLongClick = entry.onRemove?.let { { onRequestRemove() } })
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        entry.icon()
        Text(
            entry.label,
            color = LocalContentColor.current.copy(alpha = 0.87f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp).weight(1f)
        )
    }
}

@Composable
private fun CategoryGrid(
    entries: List<CategoryEntry>,
    emptyLabelRes: Int?,
    addLabelRes: Int?,
    onAddClick: (() -> Unit)?,
    onRequestRemove: (CategoryEntry) -> Unit
) {
    val rows = entries.chunked(OVERVIEW_GRID_COLUMNS)
    LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = OVERVIEW_LIST_CONTENT_PADDING) {
        if (entries.isEmpty() && emptyLabelRes != null) {
            item(key = "empty") {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(emptyLabelRes), color = LocalContentColor.current.copy(alpha = 0.5f))
                }
            }
        }
        items(rows, key = { row -> row.first().key }) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(OVERVIEW_GRID_COLUMNS) { slot ->
                    Box(modifier = Modifier.weight(1f)) {
                        row.getOrNull(slot)?.let { entry -> CategoryGridCell(entry, onRequestRemove = { onRequestRemove(entry) }) }
                    }
                }
            }
        }
        if (addLabelRes != null && onAddClick != null) {
            item(key = "add") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleClickable(onClick = onAddClick)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f))
                    Text(stringResource(addLabelRes), color = LocalContentColor.current.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun CategoryGridCell(entry: CategoryEntry, onRequestRemove: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = entry.onClick, onLongClick = entry.onRemove?.let { { onRequestRemove() } })
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        entry.icon()
        Text(
            entry.label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalContentColor.current.copy(alpha = 0.87f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun AddRow(labelRes: Int, onClick: () -> Unit, rowHeight: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f))
        Text(stringResource(labelRes), color = LocalContentColor.current.copy(alpha = 0.7f), modifier = Modifier.padding(start = 8.dp))
    }
}
