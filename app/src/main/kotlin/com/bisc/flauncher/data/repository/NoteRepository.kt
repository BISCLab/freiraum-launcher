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
        if (note.id == 0L) noteDao.insert(note) else { noteDao.update(note); note.id }

    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }
}
