package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bisc.flauncher.data.model.DockSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface DockSlotDao {
    @Query("SELECT * FROM dock_slots ORDER BY slotIndex ASC")
    fun observeAll(): Flow<List<DockSlot>>

    @Query("SELECT COUNT(*) FROM dock_slots")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(slot: DockSlot)

    @Query("DELETE FROM dock_slots WHERE slotIndex = :slotIndex")
    suspend fun clear(slotIndex: Int)

    @Query("DELETE FROM dock_slots")
    suspend fun clearAll()

    /** Replaces the whole slot list atomically — used for add/remove/reorder, which all renumber indices. */
    @Transaction
    suspend fun replaceAll(slots: List<DockSlot>) {
        clearAll()
        slots.forEach { upsert(it) }
    }
}
