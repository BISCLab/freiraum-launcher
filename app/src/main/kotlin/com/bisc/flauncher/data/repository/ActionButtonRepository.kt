package com.bisc.flauncher.data.repository

import com.bisc.flauncher.data.db.ActionButtonDao
import com.bisc.flauncher.data.model.ActionButton
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ActionButtonRepository @Inject constructor(
    private val dao: ActionButtonDao
) {
    val actionButtons: Flow<List<ActionButton>> = dao.observeAll()

    suspend fun add(button: ActionButton): Long = dao.insert(button)

    suspend fun update(button: ActionButton) {
        dao.update(button)
    }

    suspend fun remove(button: ActionButton) {
        dao.delete(button)
    }
}
