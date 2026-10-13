package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.LauncherPageKind
import kotlinx.coroutines.flow.Flow

@Dao
interface LauncherPageDao {
    @Query("SELECT * FROM launcher_pages ORDER BY orderIndex")
    fun observeAll(): Flow<List<LauncherPage>>

    @Query("SELECT * FROM launcher_pages ORDER BY orderIndex")
    suspend fun getAllOnce(): List<LauncherPage>

    @Query("SELECT * FROM launcher_pages WHERE id = :id")
    suspend fun getById(id: Long): LauncherPage?

    @Query("SELECT * FROM launcher_pages WHERE isHome = 1 LIMIT 1")
    suspend fun getHomePage(): LauncherPage?

    @Query("SELECT * FROM launcher_pages WHERE isAppOverview = 1 LIMIT 1")
    suspend fun getAppOverviewPage(): LauncherPage?

    @Query("SELECT * FROM launcher_pages WHERE kind = :kind LIMIT 1")
    suspend fun getByKind(kind: LauncherPageKind): LauncherPage?

    @Query("SELECT MAX(orderIndex) FROM launcher_pages")
    suspend fun maxOrderIndex(): Int?

    @Insert
    suspend fun insert(page: LauncherPage): Long

    @Update
    suspend fun update(page: LauncherPage)

    @Delete
    suspend fun delete(page: LauncherPage)
}
