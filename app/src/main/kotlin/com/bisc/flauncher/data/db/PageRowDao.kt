package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bisc.flauncher.data.model.PageRow
import kotlinx.coroutines.flow.Flow

@Dao
interface PageRowDao {
    @Query("SELECT * FROM page_rows")
    fun observeAll(): Flow<List<PageRow>>

    @Query("SELECT * FROM page_rows WHERE pageId = :pageId ORDER BY orderIndex")
    suspend fun getForPage(pageId: Long): List<PageRow>

    @Query("SELECT * FROM page_rows WHERE id = :id")
    suspend fun getById(id: Long): PageRow?

    @Query("SELECT COUNT(*) FROM page_rows WHERE pageId = :pageId")
    suspend fun countForPage(pageId: Long): Int

    @Query("SELECT MAX(orderIndex) FROM page_rows WHERE pageId = :pageId")
    suspend fun maxOrderIndex(pageId: Long): Int?

    @Insert
    suspend fun insert(row: PageRow): Long

    @Update
    suspend fun update(row: PageRow)

    @Update
    suspend fun updateAll(rows: List<PageRow>)

    @Delete
    suspend fun delete(row: PageRow)
}
