package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bisc.flauncher.data.model.FolderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderItemDao {
    @Query("SELECT * FROM folder_items ORDER BY orderIndex")
    fun observeAll(): Flow<List<FolderItem>>

    @Query("SELECT COUNT(*) FROM folder_items WHERE folderId = :folderId")
    suspend fun countForFolder(folderId: Long): Int

    @Query("SELECT MAX(orderIndex) FROM folder_items WHERE folderId = :folderId")
    suspend fun maxOrderIndex(folderId: Long): Int?

    @Insert
    suspend fun insert(item: FolderItem): Long

    @Update
    suspend fun update(item: FolderItem)

    @Update
    suspend fun updateAll(items: List<FolderItem>)

    @Delete
    suspend fun delete(item: FolderItem)
}
