package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bisc.flauncher.data.model.IconPackMapping

@Dao
interface IconPackMappingDao {
    @Query("SELECT * FROM icon_pack_mappings WHERE iconPackPackage = :pkg")
    suspend fun getAllForPack(pkg: String): List<IconPackMapping>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mappings: List<IconPackMapping>)

    @Query("DELETE FROM icon_pack_mappings WHERE iconPackPackage = :pkg")
    suspend fun clearForPack(pkg: String)
}
