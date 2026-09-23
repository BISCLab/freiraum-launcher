package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bisc.flauncher.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY orderIndex ASC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT MAX(orderIndex) FROM notes")
    suspend fun maxOrderIndex(): Int?

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Update
    suspend fun updateAll(notes: List<Note>)

    @Delete
    suspend fun delete(note: Note)
}
