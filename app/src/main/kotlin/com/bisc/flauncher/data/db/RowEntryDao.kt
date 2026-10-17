package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bisc.flauncher.data.model.RowEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface RowEntryDao {
    @Query("SELECT * FROM row_entries")
    fun observeAll(): Flow<List<RowEntry>>

    @Query("SELECT MAX(orderIndex) FROM row_entries WHERE pageRowId = :pageRowId")
    suspend fun maxOrderIndex(pageRowId: Long): Int?

    @Query("SELECT COUNT(*) FROM row_entries WHERE pageRowId = :pageRowId")
    suspend fun countForRow(pageRowId: Long): Int

    @Insert
    suspend fun insert(entry: RowEntry): Long

    @Update
    suspend fun update(entry: RowEntry)

    @Delete
    suspend fun delete(entry: RowEntry)
}
