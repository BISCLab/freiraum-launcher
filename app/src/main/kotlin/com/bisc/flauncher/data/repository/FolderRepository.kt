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
    /** Every folder but the one that holds the Panel's widgets. */
    val folders: Flow<List<Folder>> = kotlinx.coroutines.flow.combine(folderDao.observeAll(), settingsRepository.panelWidgetFolderId) { all, panelId ->
        if (panelId == null) all else all.filter { it.id != panelId }
    }

    /** The folder holding the Panel's widgets, made on first use. */
    suspend fun panelWidgetFolderId(): Long {
        settingsRepository.panelWidgetFolderId.first()?.let { id -> if (folderDao.get(id) != null) return id }
        val id = folderDao.insert(Folder(name = "Panel"))
        settingsRepository.setPanelWidgetFolderId(id)
        return id
    }
    val folderItems: Flow<List<FolderItem>> = folderItemDao.observeAll()

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

    /** The place for a new app or element: the one reserved by tapping an empty cell, or the first free one. */
    private suspend fun nextOrderIndex(folderId: Long): Int {
        val used = folderItemDao.itemsForFolder(folderId).filter { !it.isWidgetItem() }.map { it.orderIndex }.toSet()
        reservedSlots.remove(folderId)?.takeIf { it >= 0 && it !in used }?.let { return it }
        return generateSequence(0) { it + 1 }.first { it !in used }
    }

    private suspend fun nextWidgetIndex(folderId: Long): Int =
        (folderItemDao.itemsForFolder(folderId).filter { it.isWidgetItem() }.maxOfOrNull { it.orderIndex } ?: -1) + 1

    private fun FolderItem.isWidgetItem() = type == GridItemType.WIDGET || type == GridItemType.SYSTEM_WIDGET

    /** The next app or element added to [folderId] goes to [slot]; null = the first free place. */
    fun reserveSlot(folderId: Long, slot: Int?) {
        if (slot == null) reservedSlots.remove(folderId) else reservedSlots[folderId] = slot
    }

    /** Moves an app or element to [slot]; whatever sits there takes its old place. */
    suspend fun moveToSlot(item: FolderItem, slot: Int) {
        if (slot < 0 || slot == item.orderIndex) return
        folderItemDao.itemsForFolder(item.folderId)
            .firstOrNull { !it.isWidgetItem() && it.orderIndex == slot && it.id != item.id }
            ?.let { folderItemDao.update(it.copy(orderIndex = item.orderIndex)) }
        folderItemDao.update(item.copy(orderIndex = slot))
    }

    suspend fun addItemToFolder(folderId: Long, type: GridItemType, payload: String) {
        folderItemDao.insert(FolderItem(folderId = folderId, type = type, payload = payload, orderIndex = nextOrderIndex(folderId)))
    }

    suspend fun addWidgetToFolder(folderId: Long, kind: String, spanY: Int = WidgetKind.defaultSpan(kind)): Long {
        val isDark = settingsRepository.theme.first().isDark(context)
        var item = FolderItem(
            folderId = folderId, type = GridItemType.WIDGET, payload = kind, spanY = spanY, orderIndex = nextWidgetIndex(folderId),
            // Widgets start only slightly rounded, same as on a page.
            cornerRadiusPercent = settingsRepository.newPartCorner(widget = true) ?: com.bisc.flauncher.ui.theme.DEFAULT_WIDGET_CORNER_RADIUS_PERCENT
        )
        item = item.withAppearance(settingsRepository.newPartLook(widget = true, isDark = isDark))
        return folderItemDao.insert(item)
    }

    /** An already bound (and configured) app widget, [appWidgetId], as a folder's widget. */
    suspend fun addSystemWidgetToFolder(folderId: Long, appWidgetId: Int, spanY: Int): Long {
        val isDark = settingsRepository.theme.first().isDark(context)
        var item = FolderItem(
            folderId = folderId, type = GridItemType.SYSTEM_WIDGET, payload = appWidgetId.toString(), spanY = spanY,
            orderIndex = nextWidgetIndex(folderId),
            cornerRadiusPercent = settingsRepository.newPartCorner(widget = true) ?: com.bisc.flauncher.ui.theme.DEFAULT_WIDGET_CORNER_RADIUS_PERCENT
        )
        item = item.withAppearance(settingsRepository.newPartLook(widget = true, isDark = isDark))
        return folderItemDao.insert(item)
    }

    suspend fun updateFolderItem(item: FolderItem) {
        folderItemDao.update(item)
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

    /** Puts [orderedItems] (all of one folder) in this order: widgets one after another, apps and
     *  elements into the places they had between them, so empty cells stay where they are. */
    /** Writes widgets moved to other places (see moveWidgetStep). */
    suspend fun saveItems(items: List<FolderItem>) {
        if (items.isNotEmpty()) folderItemDao.updateAll(items)
    }

    suspend fun reorderItems(orderedItems: List<FolderItem>) {
        val (widgets, icons) = orderedItems.partition { it.isWidgetItem() }
        // Each widget keeps the row it sits above; only the order among those there changes.
        widgets.groupBy { it.orderIndex / com.bisc.flauncher.ui.home.WIDGET_ROW_STEP }.forEach { (row, group) ->
            group.forEachIndexed { index, item ->
                val place = row * com.bisc.flauncher.ui.home.WIDGET_ROW_STEP + index
                if (item.orderIndex != place) folderItemDao.update(item.copy(orderIndex = place))
            }
        }
        val slots = icons.map { it.orderIndex }.sorted()
        icons.forEachIndexed { index, item ->
            if (item.orderIndex != slots[index]) folderItemDao.update(item.copy(orderIndex = slots[index]))
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

    companion object {
        private val reservedSlots = java.util.concurrent.ConcurrentHashMap<Long, Int>()
    }
}
