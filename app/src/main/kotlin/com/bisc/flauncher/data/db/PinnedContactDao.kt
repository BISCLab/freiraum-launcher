package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.bisc.flauncher.data.model.PinnedContact
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedContactDao {
    @Query("SELECT * FROM pinned_contacts ORDER BY displayName ASC")
    fun observeAll(): Flow<List<PinnedContact>>

    @Query("SELECT COUNT(*) FROM pinned_contacts WHERE contactId = :contactId")
    suspend fun countByContactId(contactId: Long): Int

    @Insert
    suspend fun insert(contact: PinnedContact): Long

}
