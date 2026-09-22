package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bisc.flauncher.data.model.SidebarItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SidebarItemDao {
    @Query("SELECT * FROM sidebar_items ORDER BY orderIndex ASC")
    fun observeAll(): Flow<List<SidebarItem>>

    @Query("SELECT COUNT(*) FROM sidebar_items")
    suspend fun count(): Int

    @Insert
    suspend fun insert(item: SidebarItem): Long

    @Update
    suspend fun update(item: SidebarItem)

    @Delete
    suspend fun delete(item: SidebarItem)
}
