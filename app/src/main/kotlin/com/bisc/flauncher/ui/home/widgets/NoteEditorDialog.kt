package com.bisc.flauncher.ui.home.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.Note
import com.bisc.flauncher.data.model.NoteType
import com.bisc.flauncher.data.model.checklistItemsToNoteContent
import com.bisc.flauncher.data.model.noteContentToChecklistItems
import com.bisc.flauncher.ui.home.NoteContentEditor
import com.bisc.flauncher.ui.home.NoteReadContent
import com.bisc.flauncher.ui.home.NoteTypeToggle
import kotlinx.coroutines.delay

private const val AUTOSAVE_DELAY_MS = 500L

/**
 * A note in a near-fullscreen dialog (top row down to the bottom safe area, independent of the
 * underlying home screen's scroll position): an existing note opens in the read view, where
 * checklist items can be ticked; title, type and content change only in the edit view, which
 * autosaves as you type — there is no explicit Save button. A new note ([note] = null) opens
 * straight into the edit view. [onSave] reports the stored id through its callback.
 */
@Composable
fun NoteEditorDialog(
    note: Note?,
    onSave: (Note, (Long) -> Unit) -> Unit,
    onDelete: (Note) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember(note) { mutableStateOf(note) }
    var editing by remember(note) { mutableStateOf(note == null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        val lightBackground = MaterialTheme.colorScheme.surface.luminance() > 0.5f
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightBackground
                    isAppearanceLightNavigationBars = lightBackground
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            val shown = current
            if (editing || shown == null) {
                NoteEditForm(
                    note = shown,
                    onAutosave = { edited, onStored ->
                        onSave(edited) { id -> onStored(id) }
                    },
                    onChanged = { current = it },
                    onDone = { saved -> if (saved != null) { current = saved; editing = false } else onDismiss() },
                    onDelete = onDelete
                )
            } else {
                Column(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        NoteReadContent(
                            note = shown,
                            onEdit = { editing = true },
                            onToggleItem = { index ->
                                val toggled = shown.copy(
                                    items = shown.items.toMutableList().also { it[index] = it[index].copy(checked = !it[index].checked) }
                                )
                                current = toggled
                                onSave(toggled) {}
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.widget_notes_close)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditForm(
    note: Note?,
    onAutosave: (Note, onStored: (Long) -> Unit) -> Unit,
    onChanged: (Note) -> Unit,
    onDone: (Note?) -> Unit,
    onDelete: (Note) -> Unit
) {
    var confirmingDelete by remember { mutableStateOf(false) }
    var noteId by remember(note?.id) { mutableStateOf(note?.id ?: 0L) }
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var type by remember(note?.id) { mutableStateOf(note?.type ?: NoteType.FREITEXT) }
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var items by remember(note?.id) { mutableStateOf(note?.items.orEmpty()) }
    // Nothing has been persisted yet for a brand-new, still-blank note — tracked so "Done"/dismiss
    // can tell a genuinely empty draft (discard, nothing to show in the read view) from an edited one.
    var everStored by remember(note?.id) { mutableStateOf(note != null && note.id != 0L) }

    val latestOnChanged = rememberUpdatedState(onChanged)
    LaunchedEffect(title, type, content, items) {
        val hasContent = title.isNotBlank() || content.isNotBlank() || items.any { it.text.isNotBlank() }
        if (!hasContent) return@LaunchedEffect
        delay(AUTOSAVE_DELAY_MS)
        val edited = Note(
            id = noteId,
            title = title,
            type = type,
            content = content,
            items = items,
            updatedAt = System.currentTimeMillis()
        )
        onAutosave(edited) { id ->
            noteId = id
            everStored = true
            latestOnChanged.value(edited.copy(id = id))
        }
    }

    fun currentSnapshot(): Note? =
        if (!everStored) null
        else Note(id = noteId, title = title, type = type, content = content, items = items, updatedAt = System.currentTimeMillis())

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            androidx.compose.material3.OutlinedTextField(
                value = title,
                onValueChange = { title = it },
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
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            )
            NoteContentEditor(
                type = type,
                content = content,
                items = items,
                onContentChange = { content = it },
                onItemsChange = { items = it },
                contentMinHeight = 200.dp
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (everStored) {
                TextButton(onClick = { confirmingDelete = true }) { Text(stringResource(R.string.widget_notes_delete)) }
            }
            TextButton(onClick = { onDone(currentSnapshot()) }) { Text(stringResource(R.string.widget_notes_close)) }
        }
    }
    if (confirmingDelete) {
        DeleteNoteDialog(
            title = title.ifBlank { stringResource(R.string.widget_notes_untitled) },
            onDelete = {
                confirmingDelete = false
                currentSnapshot()?.let(onDelete)
            },
            onCancel = { confirmingDelete = false },
            onSave = {
                confirmingDelete = false
                onDone(currentSnapshot())
            }
        )
    }
}

/** Delete/Cancel/Save, Cancel centered — a mistaken "Delete" tap mid-edit still lets you either keep
 *  editing or explicitly finish (autosave has already captured everything either way). */
@Composable
private fun DeleteNoteDialog(title: String, onDelete: () -> Unit, onCancel: () -> Unit, onSave: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(R.string.sidebar_remove_confirm_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.sidebar_remove_confirm_message, title),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.widget_notes_delete)) }
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.widget_notes_cancel)) }
                    TextButton(onClick = onSave) { Text(stringResource(R.string.widget_notes_save)) }
                }
            }
        }
    }
}
