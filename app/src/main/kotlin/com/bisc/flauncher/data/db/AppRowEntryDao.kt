package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bisc.flauncher.data.model.AppRowEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRowEntryDao {
    @Query("SELECT * FROM app_row_entries")
    fun observeAll(): Flow<List<AppRowEntry>>

    @Query("SELECT MAX(orderIndex) FROM app_row_entries WHERE pageRowId = :pageRowId")
    suspend fun maxOrderIndex(pageRowId: Long): Int?

    @Query("SELECT COUNT(*) FROM app_row_entries WHERE pageRowId = :pageRowId")
    suspend fun countForRow(pageRowId: Long): Int

    @Insert
    suspend fun insert(entry: AppRowEntry): Long

    @Update
    suspend fun update(entry: AppRowEntry)

    @Delete
    suspend fun delete(entry: AppRowEntry)
}
