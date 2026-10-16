package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bisc.flauncher.data.model.PinnedWebsite
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedWebsiteDao {
    @Query("SELECT * FROM pinned_websites ORDER BY label ASC")
    fun observeAll(): Flow<List<PinnedWebsite>>

    @Insert
    suspend fun insert(website: PinnedWebsite): Long

    @Update
    suspend fun update(website: PinnedWebsite)

    @Delete
    suspend fun delete(website: PinnedWebsite)
}
