package com.bisc.flauncher.ui.home.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.bisc.flauncher.data.model.WidgetCornerStyle
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
    cornerStyle: WidgetCornerStyle = WidgetCornerStyle.ROUNDED,
    horizontalMargin: Dp = 0.dp,
    spanY: Int = 1,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsState()
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val rows = spanY.coerceAtLeast(1)
    val perPage = rows * COLUMNS
    val pageCount = ((notes.size - 1) / perPage + 1).coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    // Same automatic black/white the rest of the app uses so the grid lines read against any background.
    val gridLineColor = resolveElementColors(appearance).borderColor

    WidgetCard(modifier = modifier, appearance = appearance, cornerStyle = cornerStyle, horizontalMargin = horizontalMargin, contentPadding = 0.dp) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.EditNote, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.8f), modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.widget_notes_title), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { editingNote = null; showEditor = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.widget_notes_add), modifier = Modifier.size(18.dp))
                }
            }
            if (notes.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxSize().clickable { editingNote = null; showEditor = true }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.widget_notes_empty), color = LocalContentColor.current.copy(alpha = 0.7f))
                }
            } else {
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                    val pageNotes = notes.drop(page * perPage).take(perPage)
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
                                    NoteCell(
                                        note = pageNotes.getOrNull(r * COLUMNS + c),
                                        borderColor = gridLineColor,
                                        onClick = { note -> editingNote = note; showEditor = true },
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
private fun NoteCell(note: Note?, borderColor: Color, onClick: (Note?) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(LocalContentColor.current.copy(alpha = 0.05f), shape)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = note != null) { onClick(note) }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (note != null) {
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
