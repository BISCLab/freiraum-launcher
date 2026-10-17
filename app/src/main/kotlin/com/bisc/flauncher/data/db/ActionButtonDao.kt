package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bisc.flauncher.data.model.ActionButton
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionButtonDao {
    @Query("SELECT * FROM action_buttons ORDER BY label ASC")
    fun observeAll(): Flow<List<ActionButton>>

    @Insert
    suspend fun insert(button: ActionButton): Long

    @Update
    suspend fun update(button: ActionButton)

    @Delete
    suspend fun delete(button: ActionButton)
}
