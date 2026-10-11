package com.bisc.flauncher.ui.home.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.Note
import com.bisc.flauncher.data.model.NoteType
import com.bisc.flauncher.data.model.checklistItemsToNoteContent
import com.bisc.flauncher.data.model.noteContentToChecklistItems
import com.bisc.flauncher.ui.home.NoteContentEditor
import com.bisc.flauncher.ui.home.NoteReadContent
import com.bisc.flauncher.ui.home.NoteTypeToggle

/**
 * A note in a dialog. An existing note opens in the read view, where checklist items can be ticked; title, type and
 * content change only in the edit view. A new note ([note] = null) opens in the edit view and switches to the read
 * view after saving. [onSave] reports the stored id through its callback.
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            val shown = current
            if (editing || shown == null) {
                NoteEditForm(
                    note = shown,
                    onSave = { edited ->
                        onSave(edited) { id -> current = edited.copy(id = id) }
                        current = edited
                        editing = false
                    },
                    onDelete = onDelete,
                    onCancel = { if (shown == null) onDismiss() else editing = false }
                )
            } else {
                Column(modifier = Modifier.padding(20.dp)) {
                    Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
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
private fun NoteEditForm(note: Note?, onSave: (Note) -> Unit, onDelete: (Note) -> Unit, onCancel: () -> Unit) {
    var confirmingDelete by remember { mutableStateOf(false) }
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var type by remember(note?.id) { mutableStateOf(note?.type ?: NoteType.FREITEXT) }
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var items by remember(note?.id) { mutableStateOf(note?.items.orEmpty()) }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
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
        Column(modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
            NoteContentEditor(
                type = type,
                content = content,
                items = items,
                onContentChange = { content = it },
                onItemsChange = { items = it },
                contentMinHeight = 160.dp
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (note != null && note.id != 0L) {
                TextButton(onClick = { confirmingDelete = true }) { Text(stringResource(R.string.widget_notes_delete)) }
            }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.widget_notes_cancel)) }
            TextButton(onClick = {
                onSave(
                    (note ?: Note()).copy(
                        title = title,
                        type = type,
                        content = content,
                        items = items,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }) { Text(stringResource(R.string.widget_notes_save)) }
        }
    }
    if (confirmingDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.sidebar_remove_confirm_title)) },
            text = { Text(stringResource(R.string.sidebar_remove_confirm_message, note?.title?.ifBlank { stringResource(R.string.widget_notes_untitled) } ?: stringResource(R.string.widget_notes_untitled))) },
            confirmButton = {
                TextButton(onClick = { confirmingDelete = false; note?.let(onDelete) }) { Text(stringResource(R.string.widget_notes_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }
}
