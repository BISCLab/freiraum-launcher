package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.bisc.flauncher.data.model.PinnedShortcut
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedShortcutDao {
    @Query("SELECT * FROM pinned_shortcuts ORDER BY label ASC")
    fun observeAll(): Flow<List<PinnedShortcut>>

    @Insert
    suspend fun insert(shortcut: PinnedShortcut): Long

}
