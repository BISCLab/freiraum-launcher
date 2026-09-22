package com.bisc.flauncher.ui.home

import com.bisc.flauncher.data.model.Note
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ChecklistItem
import com.bisc.flauncher.data.model.NoteType
import com.bisc.flauncher.ui.theme.noRippleClickable

/** Switch between free text and checklist. */
@Composable
fun NoteTypeToggle(type: NoteType, onTypeSelected: (NoteType) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        NoteTypeToggleOption(
            label = stringResource(R.string.note_type_text),
            selected = type == NoteType.FREITEXT,
            onClick = { onTypeSelected(NoteType.FREITEXT) },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        NoteTypeToggleOption(
            label = stringResource(R.string.note_type_checklist),
            selected = type == NoteType.CHECKLISTE,
            onClick = { onTypeSelected(NoteType.CHECKLISTE) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NoteTypeToggleOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) LocalContentColor.current.copy(alpha = 0.12f) else Color.Transparent)
            .noRippleClickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(label, color = LocalContentColor.current.copy(alpha = if (selected) 0.95f else 0.6f))
    }
}

/** Either a free-text field or an editable checklist, depending on [type]. */
@Composable
fun NoteContentEditor(
    type: NoteType,
    content: String,
    items: List<ChecklistItem>,
    onContentChange: (String) -> Unit,
    onItemsChange: (List<ChecklistItem>) -> Unit,
    contentMinHeight: Dp = 96.dp,
    modifier: Modifier = Modifier
) {
    if (type == NoteType.FREITEXT) {
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            label = { Text(stringResource(R.string.widget_notes_content_hint)) },
            modifier = modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = contentMinHeight)
        )
    } else {
        Column(modifier = modifier.fillMaxWidth().padding(top = 4.dp)) {
            items.forEachIndexed { index, item ->
                ChecklistItemRow(
                    item = item,
                    onToggle = { onItemsChange(items.toMutableList().also { it[index] = item.copy(checked = !item.checked) }) },
                    onTextChange = { text -> onItemsChange(items.toMutableList().also { it[index] = item.copy(text = text) }) },
                    onRemove = { onItemsChange(items.toMutableList().also { it.removeAt(index) }) }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .noRippleClickable { onItemsChange(items + ChecklistItem()) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = LocalContentColor.current.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp).padding(start = 4.dp)
                )
                Text(
                    stringResource(R.string.note_add_item),
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = item.checked, onCheckedChange = { onToggle() })
        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            textStyle = LocalTextStyle.current.copy(
                color = LocalContentColor.current.copy(alpha = if (item.checked) 0.4f else 0.87f),
                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
            ),
            cursorBrush = SolidColor(LocalContentColor.current),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.note_item_remove),
                tint = LocalContentColor.current.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Read view of a note: title, edit time, and the text or a checklist whose items can be ticked. */
@Composable
fun NoteReadContent(
    note: Note,
    onToggleItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    onEdit: (() -> Unit)? = null
) {
    val untitled = stringResource(R.string.widget_notes_untitled)
    val updated = remember(note.updatedAt) {
        java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(note.updatedAt))
    }
    Column(modifier = modifier.fillMaxWidth()) {
        if (showTitle) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    note.title.ifBlank { untitled },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalContentColor.current.copy(alpha = 0.95f),
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (onEdit != null) NoteEditIconButton(onEdit)
            }
        }
        Text(
            stringResource(R.string.widget_notes_updated, updated),
            style = MaterialTheme.typography.labelSmall,
            color = LocalContentColor.current.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp)
        )
        if (note.type == NoteType.CHECKLISTE) {
            val done = note.items.count { it.checked && it.text.isNotBlank() }
            val total = note.items.count { it.text.isNotBlank() }
            if (total > 0) {
                LinearProgressIndicator(
                    progress = { done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = LocalContentColor.current.copy(alpha = 0.85f),
                    trackColor = LocalContentColor.current.copy(alpha = 0.15f)
                )
                Text(
                    stringResource(R.string.widget_notes_checklist_progress, done, total),
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Column(modifier = Modifier.padding(top = 8.dp)) {
                note.items.forEachIndexed { index, item ->
                    if (item.text.isBlank()) return@forEachIndexed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .noRippleClickable { onToggleItem(index) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = item.checked, onCheckedChange = { onToggleItem(index) })
                        Text(
                            item.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = LocalContentColor.current.copy(alpha = if (item.checked) 0.4f else 0.9f),
                            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else {
            Text(
                note.content.ifBlank { stringResource(R.string.widget_notes_empty_note) },
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = LocalContentColor.current.copy(alpha = if (note.content.isBlank()) 0.4f else 0.9f),
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

/** Small, unobtrusive pencil button that switches a note from reading to editing. */
@Composable
fun NoteEditIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(36.dp)) {
        Icon(
            Icons.Filled.Edit,
            contentDescription = stringResource(R.string.widget_notes_edit),
            tint = LocalContentColor.current.copy(alpha = 0.55f),
            modifier = Modifier.size(18.dp)
        )
    }
}
