package com.bisc.flauncher.data.repository

import com.bisc.flauncher.data.db.SidebarPinnedAppDao
import com.bisc.flauncher.data.model.SidebarPinnedApp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The Sidebar's own curated Apps list. */
class SidebarAppsRepository @Inject constructor(
    private val dao: SidebarPinnedAppDao
) {
    val pinnedApps: Flow<List<SidebarPinnedApp>> = dao.observeAll()

    suspend fun add(componentName: String) {
        if (dao.countByComponent(componentName) == 0) {
            dao.insert(SidebarPinnedApp(componentName = componentName))
        }
    }

    suspend fun addAll(componentNames: List<String>) {
        componentNames.forEach { add(it) }
    }

    suspend fun remove(app: SidebarPinnedApp) {
        dao.delete(app)
    }
}
