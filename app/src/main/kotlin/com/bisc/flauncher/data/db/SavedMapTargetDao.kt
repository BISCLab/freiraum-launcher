package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.bisc.flauncher.data.model.SavedMapTarget
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMapTargetDao {
    @Query("SELECT * FROM saved_map_targets ORDER BY label ASC")
    fun observeAll(): Flow<List<SavedMapTarget>>

    @Insert
    suspend fun insert(target: SavedMapTarget): Long

    @Delete
    suspend fun delete(target: SavedMapTarget)
}
