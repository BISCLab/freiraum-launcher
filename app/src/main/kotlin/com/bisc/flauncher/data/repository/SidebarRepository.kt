package com.bisc.flauncher.data.repository

import com.bisc.flauncher.data.db.SidebarItemDao
import com.bisc.flauncher.data.model.SidebarItem
import com.bisc.flauncher.data.model.SidebarItemType
import com.bisc.flauncher.ui.home.dock.SidebarEntryType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SidebarRepository @Inject constructor(
    private val dao: SidebarItemDao
) {
    val items: Flow<List<SidebarItem>> = dao.observeAll()

    suspend fun ensureSeeded() {
        if (dao.count() > 0) return
        SidebarEntryType.entries.forEachIndexed { index, entry ->
            dao.insert(SidebarItem(orderIndex = index, type = SidebarItemType.CATEGORY, payload = entry.name))
        }
    }

    suspend fun addCategory(entry: SidebarEntryType, atIndex: Int) {
        dao.insert(SidebarItem(orderIndex = atIndex, type = SidebarItemType.CATEGORY, payload = entry.name))
    }

    suspend fun addApp(componentName: String, atIndex: Int) {
        dao.insert(SidebarItem(orderIndex = atIndex, type = SidebarItemType.APP, payload = componentName))
    }

    suspend fun addShortcut(pinnedShortcutId: Long, atIndex: Int) {
        dao.insert(SidebarItem(orderIndex = atIndex, type = SidebarItemType.SHORTCUT, payload = pinnedShortcutId.toString()))
    }

    suspend fun addMapTarget(savedMapTargetId: Long, atIndex: Int) {
        dao.insert(SidebarItem(orderIndex = atIndex, type = SidebarItemType.MAP_TARGET, payload = savedMapTargetId.toString()))
    }

    suspend fun setCustomIcon(item: SidebarItem, iconKey: String?) {
        dao.update(item.copy(customIconKey = iconKey))
    }

    suspend fun remove(item: SidebarItem) {
        dao.delete(item)
    }

    suspend fun reorder(orderedItems: List<SidebarItem>) {
        orderedItems.forEachIndexed { index, item ->
            if (item.orderIndex != index) {
                dao.update(item.copy(orderIndex = index))
            }
        }
    }
}
