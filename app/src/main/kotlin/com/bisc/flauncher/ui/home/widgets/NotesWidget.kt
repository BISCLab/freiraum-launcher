package com.bisc.flauncher.ui.home.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.Note
import com.bisc.flauncher.data.model.NoteType
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.resolveElementColors

private const val COLUMNS = 2

/**
 * A grid of note titles, [spanY] rows by two columns; more notes than fit one page are reached by
 * swiping sideways.
 */
@Composable
fun NotesWidget(
    modifier: Modifier = Modifier,
    appearance: ElementAppearance = ElementAppearance(),
    cornerRadiusPercent: Int = com.bisc.flauncher.ui.theme.DEFAULT_CORNER_RADIUS_PERCENT,
    horizontalMargin: Dp = 0.dp,
    spanY: Int = 1,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsState()
    // Hiding only affects this widget's own grid; the note stays visible everywhere else, so
    // reordering (moveBy below) still operates on the full, unfiltered list.
    val visibleNotes = remember(notes) { notes.filter { !it.hiddenInWidget } }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var menuForNoteId by remember { mutableStateOf<Long?>(null) }
    var movingNoteId by remember { mutableStateOf<Long?>(null) }

    val rows = spanY.coerceAtLeast(1)
    val perPage = rows * COLUMNS
    val pageCount = ((visibleNotes.size - 1) / perPage + 1).coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    // Same automatic black/white the rest of the app uses so the grid lines read against any background.
    val gridLineColor = resolveElementColors(appearance).borderColor

    // Moves past the next *visible* neighbor — stepping through the full list instead would swap
    // with a hidden note next to it, a move nobody can see.
    fun moveBy(delta: Int) {
        val visibleIndex = visibleNotes.indexOfFirst { it.id == movingNoteId }
        val neighbor = visibleNotes.getOrNull(visibleIndex + delta) ?: return
        val from = notes.indexOfFirst { it.id == movingNoteId }
        val to = notes.indexOfFirst { it.id == neighbor.id }
        if (from < 0 || to < 0) return
        val reordered = notes.toMutableList().apply { add(to, removeAt(from)) }
        viewModel.reorderNotes(reordered)
    }
    val hiddenNotes = remember(notes) { notes.filter { it.hiddenInWidget } }
    var hiddenMenuOpen by remember { mutableStateOf(false) }

    WidgetCard(modifier = modifier, appearance = appearance, cornerRadiusPercent = cornerRadiusPercent, horizontalMargin = horizontalMargin, contentPadding = 0.dp) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (movingNoteId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { moveBy(-1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.folder_move_left), modifier = Modifier.size(18.dp))
                    }
                    Text(
                        stringResource(R.string.folder_move_hint),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )
                    TextButton(onClick = { movingNoteId = null }) { Text(stringResource(R.string.folder_move_done)) }
                    IconButton(onClick = { moveBy(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.folder_move_right), modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.EditNote, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.8f), modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.widget_notes_title), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    // Notes hidden from this widget, to show again — otherwise hiding would be a dead end.
                    if (hiddenNotes.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { hiddenMenuOpen = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.VisibilityOff, contentDescription = stringResource(R.string.widget_notes_hidden), modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = hiddenMenuOpen, onDismissRequest = { hiddenMenuOpen = false }) {
                                hiddenNotes.forEach { note ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.widget_notes_show_again, note.title.ifBlank { stringResource(R.string.widget_notes_untitled) })) },
                                        onClick = {
                                            viewModel.saveNote(note.copy(hiddenInWidget = false))
                                            if (hiddenNotes.size == 1) hiddenMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = { editingNote = null; showEditor = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.widget_notes_add), modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (visibleNotes.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxSize().combinedClickable(onClick = { editingNote = null; showEditor = true }).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.widget_notes_empty), color = LocalContentColor.current.copy(alpha = 0.7f))
                }
            } else {
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                    val pageNotes = visibleNotes.drop(page * perPage).take(perPage)
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(rows) { r ->
                            Row(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(COLUMNS) { c ->
                                    val cellNote = pageNotes.getOrNull(r * COLUMNS + c)
                                    NoteCell(
                                        note = cellNote,
                                        borderColor = gridLineColor,
                                        selected = cellNote?.id != null && cellNote.id == movingNoteId,
                                        enabled = movingNoteId == null,
                                        menuExpanded = cellNote?.id != null && cellNote.id == menuForNoteId,
                                        onClick = { note -> editingNote = note; showEditor = true },
                                        onLongClick = { note -> menuForNoteId = note.id },
                                        onDismissMenu = { menuForNoteId = null },
                                        onRequestMove = { note -> menuForNoteId = null; movingNoteId = note.id },
                                        onRequestHide = { note -> menuForNoteId = null; viewModel.saveNote(note.copy(hiddenInWidget = true)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        NoteEditorDialog(
            note = editingNote,
            onSave = { note, onSaved -> viewModel.saveNote(note, onSaved) },
            onDelete = { note ->
                viewModel.deleteNote(note)
                showEditor = false
            },
            onDismiss = { showEditor = false }
        )
    }
}

@Composable
private fun NoteCell(
    note: Note?,
    borderColor: Color,
    selected: Boolean,
    enabled: Boolean,
    menuExpanded: Boolean,
    onClick: (Note?) -> Unit,
    onLongClick: (Note) -> Unit,
    onDismissMenu: () -> Unit,
    onRequestMove: (Note) -> Unit,
    onRequestHide: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else LocalContentColor.current.copy(alpha = 0.05f),
                shape
            )
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else borderColor, shape)
            .combinedClickable(
                enabled = note != null && enabled,
                onClick = { onClick(note) },
                onLongClick = note?.let { { onLongClick(it) } }
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (note != null) {
            DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(text = { Text(stringResource(R.string.folder_item_move)) }, onClick = { onRequestMove(note) })
                DropdownMenuItem(text = { Text(stringResource(R.string.widget_notes_hide)) }, onClick = { onRequestHide(note) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.type == NoteType.CHECKLISTE) {
                    Icon(
                        Icons.Filled.Checklist,
                        contentDescription = null,
                        tint = LocalContentColor.current.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                }
                Text(
                    note.title.ifBlank { stringResource(R.string.widget_notes_untitled) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
