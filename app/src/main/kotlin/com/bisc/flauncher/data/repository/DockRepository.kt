package com.bisc.flauncher.data.repository

import com.bisc.flauncher.data.db.DockSlotDao
import com.bisc.flauncher.data.model.DockSlot
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Thin repository over [DockSlotDao]. */
class DockRepository @Inject constructor(private val dao: DockSlotDao) {
    val slots: Flow<List<DockSlot>> = dao.observeAll()

    suspend fun setSlot(index: Int, componentName: String?) {
        if (componentName == null) dao.clear(index) else dao.upsert(DockSlot(index, componentName))
    }
}
