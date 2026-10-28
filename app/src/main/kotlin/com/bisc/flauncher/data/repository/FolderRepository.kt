package com.bisc.flauncher.data.repository

import android.content.Context
import com.bisc.flauncher.data.db.FolderDao
import com.bisc.flauncher.data.db.FolderItemDao
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderIconMode
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.data.model.withBorderAppearance
import com.bisc.flauncher.data.model.withContentAppearance
import com.bisc.flauncher.data.model.withRows
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.theme.defaultBoxAppearanceFor
import com.bisc.flauncher.ui.theme.isDark
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class FolderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderDao: FolderDao,
    private val folderItemDao: FolderItemDao,
    private val settingsRepository: SettingsRepository,
    private val widgetHostManager: com.bisc.flauncher.util.SystemWidgetHostManager
) {
    val folders: Flow<List<Folder>> = folderDao.observeAll()
    val folderItems: Flow<List<FolderItem>> = folderItemDao.observeAll()

    suspend fun applyCornerRadiusToAllItems(percent: Int) {
        val items = folderItemDao.observeAll().first().map { it.copy(cornerRadiusPercent = percent) }
        folderItemDao.updateAll(items)
    }

    suspend fun createFolder(name: String): Long = folderDao.insert(Folder(name = name))

    suspend fun renameFolder(folder: Folder, newName: String) {
        folderDao.update(folder.copy(name = newName))
    }

    suspend fun renameFolderById(id: Long, newName: String) {
        folderDao.get(id)?.let { folderDao.update(it.copy(name = newName)) }
    }

    suspend fun setFolderIconById(
        id: Long,
        mode: FolderIconMode,
        color: String? = null,
        packPackage: String? = null,
        packDrawable: String? = null
    ) {
        folderDao.get(id)?.let { setFolderIcon(it, mode, color, packPackage, packDrawable) }
    }

    suspend fun setFolderIcon(
        folder: Folder,
        mode: FolderIconMode,
        color: String? = null,
        packPackage: String? = null,
        packDrawable: String? = null
    ) {
        folderDao.update(
            folder.copy(
                iconMode = mode,
                iconColor = if (mode == FolderIconMode.CUSTOM_COLOR) color else null,
                iconPackPackage = if (mode == FolderIconMode.ICON_PACK) packPackage else null,
                iconPackDrawable = if (mode == FolderIconMode.ICON_PACK) packDrawable else null
            )
        )
    }

    suspend fun setFolderAppearanceOverrideEnabledById(id: Long, enabled: Boolean) {
        folderDao.get(id)?.let { folderDao.update(it.copy(appearanceOverrideEnabled = enabled)) }
    }

    suspend fun setFolderAppearanceById(id: Long, appearance: ElementAppearance) {
        folderDao.get(id)?.let { folderDao.update(it.withContentAppearance(appearance).copy(appearanceOverrideEnabled = true)) }
    }

    suspend fun setFolderBorderOverrideEnabledById(id: Long, enabled: Boolean) {
        folderDao.get(id)?.let { folderDao.update(it.copy(borderOverrideEnabled = enabled)) }
    }

    suspend fun setFolderBorderAppearanceById(id: Long, appearance: ElementAppearance) {
        folderDao.get(id)?.let { folderDao.update(it.withBorderAppearance(appearance).copy(borderOverrideEnabled = true)) }
    }

    suspend fun setFolderRowColumnsById(id: Long, rows: List<Int>) {
        folderDao.get(id)?.let { folderDao.update(it.withRows(rows)) }
    }

    suspend fun setFolderShowLabelsById(id: Long, show: Boolean) {
        folderDao.get(id)?.let { folderDao.update(it.copy(showLabelsOverrideEnabled = true, showLabels = show)) }
    }

    private suspend fun nextOrderIndex(folderId: Long): Int = (folderItemDao.maxOrderIndex(folderId) ?: -1) + 1

    suspend fun addItemToFolder(folderId: Long, type: GridItemType, payload: String) {
        folderItemDao.insert(FolderItem(folderId = folderId, type = type, payload = payload, orderIndex = nextOrderIndex(folderId)))
    }

    suspend fun addWidgetToFolder(folderId: Long, kind: String, spanY: Int = WidgetKind.defaultSpan(kind)): Long {
        val isDark = settingsRepository.theme.first().isDark(context)
        var item = FolderItem(
            folderId = folderId, type = GridItemType.WIDGET, payload = kind, spanY = spanY, orderIndex = nextOrderIndex(folderId),
            // Widgets start only slightly rounded, same as on a page.
            cornerRadiusPercent = com.bisc.flauncher.ui.theme.DEFAULT_WIDGET_CORNER_RADIUS_PERCENT
        )
        item = item.withAppearance(defaultBoxAppearanceFor(isDark))
        return folderItemDao.insert(item)
    }

    /** An already bound (and configured) app widget, [appWidgetId], as a folder's widget. */
    suspend fun addSystemWidgetToFolder(folderId: Long, appWidgetId: Int, spanY: Int): Long {
        val isDark = settingsRepository.theme.first().isDark(context)
        var item = FolderItem(
            folderId = folderId, type = GridItemType.SYSTEM_WIDGET, payload = appWidgetId.toString(), spanY = spanY,
            orderIndex = nextOrderIndex(folderId),
            cornerRadiusPercent = com.bisc.flauncher.ui.theme.DEFAULT_WIDGET_CORNER_RADIUS_PERCENT
        )
        item = item.withAppearance(defaultBoxAppearanceFor(isDark))
        return folderItemDao.insert(item)
    }

    suspend fun setFolderItemSpan(item: FolderItem, spanY: Int) {
        folderItemDao.update(item.copy(spanY = spanY))
    }

    /** Frees a removed app widget's id with the system, so no orphaned widget stays bound. */
    private fun releaseWidget(item: FolderItem) {
        if (item.type == GridItemType.SYSTEM_WIDGET) item.payload.toIntOrNull()?.let(widgetHostManager::deleteId)
    }

    suspend fun addShortcutToFolder(folderId: Long, packageName: String, shortcutId: String, label: String): Long =
        folderItemDao.insert(
            FolderItem(folderId = folderId, type = GridItemType.SHORTCUT, payload = "$packageName|$shortcutId|$label", orderIndex = nextOrderIndex(folderId))
        )

    suspend fun addMapTargetToFolder(folderId: Long, targetId: Long): Long =
        folderItemDao.insert(FolderItem(folderId = folderId, type = GridItemType.MAP_TARGET, payload = targetId.toString(), orderIndex = nextOrderIndex(folderId)))

    suspend fun addWebsiteToFolder(folderId: Long, websiteId: Long): Long =
        folderItemDao.insert(FolderItem(folderId = folderId, type = GridItemType.WEBSITE, payload = websiteId.toString(), orderIndex = nextOrderIndex(folderId)))

    suspend fun addNestedFolder(parentFolderId: Long, name: String): Long {
        val childId = createFolder(name)
        addItemToFolder(parentFolderId, GridItemType.FOLDER, childId.toString())
        return childId
    }

    /** Rewrites [orderedItems]' positions to match the given order; they must all belong to the same folder. */
    suspend fun reorderItems(orderedItems: List<FolderItem>) {
        orderedItems.forEachIndexed { index, item ->
            if (item.orderIndex != index) folderItemDao.update(item.copy(orderIndex = index))
        }
    }

    suspend fun removeFolderItem(item: FolderItem): Boolean {
        folderItemDao.delete(item)
        releaseWidget(item)
        if (folderItemDao.countForFolder(item.folderId) == 0) {
            folderDao.get(item.folderId)?.let { folderDao.delete(it) }
            return true
        }
        return false
    }

    suspend fun deleteFolder(folder: Folder) {
        folderItemDao.observeAll().first().filter { it.folderId == folder.id }.forEach(::releaseWidget)
        folderDao.delete(folder)
    }

    suspend fun deleteFolderById(id: Long) {
        folderDao.get(id)?.let { deleteFolder(it) }
    }
}
