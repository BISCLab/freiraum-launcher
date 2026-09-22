package com.bisc.flauncher.data.repository

import com.bisc.flauncher.data.db.SavedMapTargetDao
import com.bisc.flauncher.data.model.SavedMapTarget
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MapsRepository @Inject constructor(
    private val dao: SavedMapTargetDao
) {
    val targets: Flow<List<SavedMapTarget>> = dao.observeAll()

    suspend fun add(label: String, lat: Double, lon: Double, address: String = ""): Long =
        dao.insert(SavedMapTarget(label = label, lat = lat, lon = lon, address = address))

    suspend fun remove(target: SavedMapTarget) {
        dao.delete(target)
    }
}
