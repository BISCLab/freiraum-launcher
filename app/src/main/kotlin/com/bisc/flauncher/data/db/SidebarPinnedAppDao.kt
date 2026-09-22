package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.bisc.flauncher.data.model.SidebarPinnedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface SidebarPinnedAppDao {
    @Query("SELECT * FROM sidebar_pinned_apps ORDER BY id ASC")
    fun observeAll(): Flow<List<SidebarPinnedApp>>

    @Query("SELECT COUNT(*) FROM sidebar_pinned_apps WHERE componentName = :componentName")
    suspend fun countByComponent(componentName: String): Int

    @Insert
    suspend fun insert(app: SidebarPinnedApp): Long

    @Delete
    suspend fun delete(app: SidebarPinnedApp)
}
