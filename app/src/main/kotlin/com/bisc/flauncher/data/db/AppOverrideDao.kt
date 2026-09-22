package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bisc.flauncher.data.model.AppOverride
import kotlinx.coroutines.flow.Flow

@Dao
interface AppOverrideDao {
    @Query("SELECT * FROM app_overrides")
    fun observeAll(): Flow<List<AppOverride>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: AppOverride)

    @Query("DELETE FROM app_overrides WHERE componentName = :componentName")
    suspend fun delete(componentName: String)
}
