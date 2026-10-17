package com.bisc.flauncher.data.repository

import com.bisc.flauncher.data.db.PowerButtonDao
import com.bisc.flauncher.data.model.PowerButton
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PowerButtonRepository @Inject constructor(
    private val dao: PowerButtonDao
) {
    val powerButtons: Flow<List<PowerButton>> = dao.observeAll()

    suspend fun add(button: PowerButton): Long = dao.insert(button)

    suspend fun update(button: PowerButton) {
        dao.update(button)
    }

    suspend fun remove(button: PowerButton) {
        dao.delete(button)
    }
}
