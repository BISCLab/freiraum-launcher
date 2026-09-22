package com.bisc.flauncher.data.repository

import com.bisc.flauncher.data.db.AppOverrideDao
import com.bisc.flauncher.data.model.AppOverride
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AppOverrideRepository @Inject constructor(
    private val dao: AppOverrideDao
) {
    val overrides: Flow<List<AppOverride>> = dao.observeAll()

    suspend fun save(override: AppOverride) {
        if (override == AppOverride(override.componentName)) {
            dao.delete(override.componentName)
        } else {
            dao.upsert(override)
        }
    }
}
