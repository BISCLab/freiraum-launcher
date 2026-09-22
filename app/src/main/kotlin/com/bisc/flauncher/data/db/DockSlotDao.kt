package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bisc.flauncher.data.model.DockSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface DockSlotDao {
    @Query("SELECT * FROM dock_slots ORDER BY slotIndex ASC")
    fun observeAll(): Flow<List<DockSlot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(slot: DockSlot)

    @Query("DELETE FROM dock_slots WHERE slotIndex = :slotIndex")
    suspend fun clear(slotIndex: Int)
}
