package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bisc.flauncher.data.model.PowerButton
import kotlinx.coroutines.flow.Flow

@Dao
interface PowerButtonDao {
    @Query("SELECT * FROM power_buttons ORDER BY label ASC")
    fun observeAll(): Flow<List<PowerButton>>

    @Insert
    suspend fun insert(button: PowerButton): Long

    @Update
    suspend fun update(button: PowerButton)

    @Delete
    suspend fun delete(button: PowerButton)
}
