package com.bisc.flauncher.data.repository

import com.bisc.flauncher.data.db.NoteDao
import com.bisc.flauncher.data.model.Note
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    val notes: Flow<List<Note>> = noteDao.observeAll()

    suspend fun save(note: Note): Long =
        if (note.id == 0L) {
            val next = (noteDao.maxOrderIndex() ?: -1) + 1
            noteDao.insert(note.copy(orderIndex = next))
        } else {
            noteDao.update(note)
            note.id
        }

    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }

    suspend fun reorder(ordered: List<Note>) {
        val changed = ordered.mapIndexedNotNull { index, note -> if (note.orderIndex != index) note.copy(orderIndex = index) else null }
        if (changed.isNotEmpty()) noteDao.updateAll(changed)
    }
}
