package com.bisc.flauncher.data.repository

import android.content.Context
import com.bisc.flauncher.R
import com.bisc.flauncher.data.db.AppRowEntryDao
import com.bisc.flauncher.data.db.LauncherPageDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.model.AppRowEntry
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.BackgroundSource
import com.bisc.flauncher.data.model.BundledWallpaper
import com.bisc.flauncher.data.model.DesignPresetValues
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.GridConstants
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.WidgetCornerStyle
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.util.BackgroundImageStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.roundToInt

/** Pages, page rows and apps-row entries, including seeding of the default pages. */
class PageContentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pageDao: LauncherPageDao,
    private val rowDao: PageRowDao,
    private val entryDao: AppRowEntryDao,
    private val settingsRepository: SettingsRepository
) {
    val allPages: Flow<List<LauncherPage>> = pageDao.observeAll()

    val pages: Flow<List<LauncherPage>> = allPages.map { list -> list.filter { it.kind == LauncherPageKind.ROWS } }

    val appOverviewPageId: Flow<Long?> = allPages.map { list -> list.firstOrNull { it.kind == LauncherPageKind.APP_LIST }?.id }
    val rows: Flow<List<PageRow>> = rowDao.observeAll()
    val appRowEntries: Flow<List<AppRowEntry>> = entryDao.observeAll()

    suspend fun ensureSeeded() {
        val existingHome = pageDao.getHomePage()
        val homeId = existingHome?.id ?: pageDao.insert(LauncherPage(orderIndex = 0, isHome = true)).also { newHomeId ->
            seedDefaultHomeBackground(newHomeId)
        }
        if (rowDao.countForPage(homeId) == 0) {
            rowDao.insert(
                PageRow(
                    pageId = homeId,
                    orderIndex = 0,
                    type = PageRowType.WIDGET,
                    payload = WidgetKind.CLOCK,
                    spanY = WidgetKind.defaultSpan(WidgetKind.CLOCK)
                )
            )
            rowDao.insert(PageRow(pageId = homeId, orderIndex = 1, type = PageRowType.SEARCH_BAR, borderEnabled = true))
        }

        if (pageDao.getAppOverviewPage() == null) {
            pageDao.insert(
                LauncherPage(orderIndex = (pageDao.maxOrderIndex() ?: -1) + 1, isAppOverview = true, kind = LauncherPageKind.APP_LIST)
            )
        }
    }

    private suspend fun seedDefaultHomeBackground(homeId: Long) {
        settingsRepository.setPageBackgroundConfig(homeId, BackgroundConfig(source = BackgroundSource.IMAGE, imagePath = BundledWallpaper.LIGHT.path))
    }

    suspend fun addPage(): Long {
        val next = (pageDao.maxOrderIndex() ?: -1) + 1
        val newId = pageDao.insert(LauncherPage(orderIndex = next))
        // A brand new page's own background otherwise defaults to plain white until the user picks
        // one, which is jarring (e.g. behind a widget added right away) — start it looking like home.
        pageDao.getHomePage()?.let { home ->
            settingsRepository.setPageBackgroundConfig(newId, settingsRepository.pageBackgroundConfig(home.id).first())
        }
        return newId
    }

    suspend fun removePageIfEmpty(page: LauncherPage): Boolean {
        if (page.isHome || page.isAppOverview) return false
        if (rowDao.countForPage(page.id) > 0) return false
        pageDao.delete(page)
        return true
    }

    suspend fun reorderPages(ordered: List<LauncherPage>) {
        ordered.forEachIndexed { index, page ->
            if (page.orderIndex != index) pageDao.update(page.copy(orderIndex = index))
        }
    }

    private suspend fun nextRowOrderIndex(pageId: Long): Int {
        val rows = rowDao.getForPage(pageId)
        val searchBarRow = rows.find { it.type == PageRowType.SEARCH_BAR }
        if (searchBarRow != null) {
            rows.filter { it.orderIndex >= searchBarRow.orderIndex }
                .forEach { row -> rowDao.update(row.copy(orderIndex = row.orderIndex + 1)) }
            return searchBarRow.orderIndex
        }
        return (rowDao.maxOrderIndex(pageId) ?: -1) + 1
    }

    private suspend fun presetRowAppearance(): ElementAppearance? =
        settingsRepository.activeDesignPreset.first()?.let { DesignPresetValues.of(it).rowAppearance }

    private suspend fun presetSearchBarAppearance(): ElementAppearance? =
        settingsRepository.activeDesignPreset.first()?.let { DesignPresetValues.of(it).searchBarAppearance }

    suspend fun addWidgetRow(pageId: Long, kind: String, spanY: Int = WidgetKind.defaultSpan(kind)): Long {
        var row = PageRow(pageId = pageId, orderIndex = nextRowOrderIndex(pageId), type = PageRowType.WIDGET, payload = kind, spanY = spanY)
        presetRowAppearance()?.let { row = row.withAppearance(it) }
        return rowDao.insert(row)
    }

    suspend fun addSystemWidgetRow(pageId: Long, appWidgetId: Int, spanY: Int): Long {
        var row = PageRow(pageId = pageId, orderIndex = nextRowOrderIndex(pageId), type = PageRowType.SYSTEM_WIDGET, payload = appWidgetId.toString(), spanY = spanY)
        presetRowAppearance()?.let { row = row.withAppearance(it) }
        return rowDao.insert(row)
    }

    suspend fun addAppsRow(pageId: Long): Long {
        var row = PageRow(pageId = pageId, orderIndex = nextRowOrderIndex(pageId), type = PageRowType.APPS, columns = GridConstants.DEFAULT_APPS_ROW_COLUMNS)
        presetRowAppearance()?.let { row = row.withAppearance(it) }
        return rowDao.insert(row)
    }

    /** Returns how many SYSTEM_WIDGET rows on [pageId] had to be relocated to a new page as a result — see [relocateOversizedWidgetsForDock]. */
    suspend fun addSearchBarRow(pageId: Long): Int {
        val next = (rowDao.maxOrderIndex(pageId) ?: -1) + 1
        var row = PageRow(pageId = pageId, orderIndex = next, type = PageRowType.SEARCH_BAR, borderEnabled = true)
        presetSearchBarAppearance()?.let { row = row.withAppearance(it) }
        rowDao.insert(row)
        return relocateOversizedWidgetsForDock(pageId).size
    }

    /**
     * A dock reserves noticeably more bottom space than an empty page does, so a SYSTEM_WIDGET row sized
     * up to the (larger) no-dock max before the dock existed can now overlap it — exactly the scenario
     * that has caused real widget-process crashes when [ui.home.PagedRows] then has to squeeze it abruptly
     * at render time. Rather than let that happen, any such row is moved to a fresh page outright.
     */
    private suspend fun relocateOversizedWidgetsForDock(pageId: Long): List<PageRow> {
        val rows = rowDao.getForPage(pageId)
        if (rows.none { it.type == PageRowType.SEARCH_BAR }) return emptyList()
        val dm = context.resources.displayMetrics
        val density = androidx.compose.ui.unit.Density(dm.density)
        val maxPx = com.bisc.flauncher.ui.home.safeSystemWidgetMaxHeightPx(
            pageHasDock = true,
            screenWidthPx = dm.widthPixels,
            screenHeightPx = dm.heightPixels,
            densityValue = dm.density,
            statusBarPx = systemInsetPx("status_bar_height"),
            navigationBarPx = systemInsetPx("navigation_bar_height")
        )
        val offending = rows.filter { row ->
            if (row.type != PageRowType.SYSTEM_WIDGET) return@filter false
            val heightPx = row.heightPx ?: with(density) {
                (com.bisc.flauncher.ui.home.PageRowDimens.rowUnitHeight * row.spanY).toPx().roundToInt()
            }
            heightPx > maxPx
        }
        if (offending.isEmpty()) return emptyList()
        val newPageId = addPage()
        var nextOrder = (rowDao.maxOrderIndex(newPageId) ?: -1) + 1
        offending.forEach { row ->
            rowDao.update(row.copy(pageId = newPageId, orderIndex = nextOrder))
            nextOrder++
        }
        return offending
    }

    private fun systemInsetPx(name: String): Int {
        val resId = context.resources.getIdentifier(name, "dimen", "android")
        return if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
    }

    suspend fun setRowSpanY(row: PageRow, spanY: Int) {
        rowDao.update(row.copy(spanY = spanY))
    }

    /** Exact-pixel sizing for a SYSTEM_WIDGET row; null reverts it to the ordinary spanY-based height. */
    suspend fun setRowHeightPx(row: PageRow, heightPx: Int?) {
        rowDao.update(row.copy(heightPx = heightPx))
    }

    suspend fun setRowAppearance(row: PageRow, appearance: ElementAppearance) {
        rowDao.update(row.withAppearance(appearance))
    }

    suspend fun setRowCornerStyle(row: PageRow, style: WidgetCornerStyle) {
        rowDao.update(row.copy(cornerStyle = style))
    }

    suspend fun setRowEdgeToEdge(row: PageRow, edgeToEdge: Boolean) {
        rowDao.update(row.copy(edgeToEdge = edgeToEdge))
    }

    suspend fun setRowColumns(row: PageRow, columns: Int) {
        rowDao.update(row.copy(columns = columns.coerceIn(GridConstants.MIN_APPS_ROW_COLUMNS, GridConstants.MAX_APPS_ROW_COLUMNS)))
    }

    suspend fun setRowClockStyle(row: PageRow, style: String) {
        rowDao.update(row.copy(clockStyle = style))
    }

    suspend fun setRowClockDateAction(row: PageRow, action: GestureAction) {
        rowDao.update(row.copy(clockDateActionType = action.type.name, clockDateActionPayload = action.payload))
    }

    suspend fun setRowClockTimeAction(row: PageRow, action: GestureAction) {
        rowDao.update(row.copy(clockTimeActionType = action.type.name, clockTimeActionPayload = action.payload))
    }

    suspend fun setRowClockTimeFormat(row: PageRow, format: String) {
        rowDao.update(row.copy(clockTimeFormat = format))
    }

    suspend fun setRowClockDateFormat(row: PageRow, format: String) {
        rowDao.update(row.copy(clockDateFormat = format))
    }

    suspend fun setRowClockShowNextAlarm(row: PageRow, show: Boolean) {
        rowDao.update(row.copy(clockShowNextAlarm = show))
    }

    suspend fun setRowClockAlignment(row: PageRow, alignment: String) {
        rowDao.update(row.copy(clockAlignment = alignment))
    }

    suspend fun removeRow(row: PageRow) {
        rowDao.delete(row)
    }

    suspend fun reorderRows(orderedRows: List<PageRow>) {
        orderedRows.forEachIndexed { index, row ->
            if (row.orderIndex != index) rowDao.update(row.copy(orderIndex = index))
        }
    }

    suspend fun addAppEntry(pageRowId: Long, componentName: String): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        return entryDao.insert(AppRowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.APP, payload = componentName))
    }

    suspend fun addFolderEntry(pageRowId: Long, folderId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        return entryDao.insert(AppRowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.FOLDER, payload = folderId.toString()))
    }

    suspend fun addShortcutEntry(pageRowId: Long, packageName: String, shortcutId: String, label: String): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        return entryDao.insert(
            AppRowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.SHORTCUT, payload = "$packageName|$shortcutId|$label")
        )
    }

    suspend fun addMapTargetEntry(pageRowId: Long, targetId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        return entryDao.insert(AppRowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.MAP_TARGET, payload = targetId.toString()))
    }

    suspend fun addWebsiteEntry(pageRowId: Long, websiteId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        return entryDao.insert(AppRowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.WEBSITE, payload = websiteId.toString()))
    }

    suspend fun removeEntry(entry: AppRowEntry) {
        entryDao.delete(entry)
    }

    suspend fun reorderEntries(orderedEntries: List<AppRowEntry>) {
        orderedEntries.forEachIndexed { index, entry ->
            if (entry.orderIndex != index) entryDao.update(entry.copy(orderIndex = index))
        }
    }
}
