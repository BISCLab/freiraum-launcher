package com.bisc.flauncher.data.repository

import android.content.Context
import com.bisc.flauncher.R
import com.bisc.flauncher.data.db.AppRowEntryDao
import com.bisc.flauncher.data.db.LauncherPageDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.model.AppRowEntry
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.BackgroundSource
import com.bisc.flauncher.data.model.ColorFill
import com.bisc.flauncher.data.model.DesignPresetValues
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.GridConstants
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.PageDirection
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.WidgetCornerStyle
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.theme.ThemePreference
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
    val searchPageId: Flow<Long?> = allPages.map { list -> list.firstOrNull { it.kind == LauncherPageKind.SEARCH }?.id }
    val rows: Flow<List<PageRow>> = rowDao.observeAll()
    val appRowEntries: Flow<List<AppRowEntry>> = entryDao.observeAll()

    suspend fun ensureSeeded() {
        ensureGlobalBackgroundSeeded()
        val existingHome = pageDao.getHomePage()
        val homeId = existingHome?.id ?: pageDao.insert(LauncherPage(orderIndex = 0, isHome = true)).also { newHomeId ->
            settingsRepository.setPageHideStatusBar(newHomeId, true)
        }
        // Home starts with just the dock — no clock, no widgets. Widgets (the clock included) only
        // appear once the user opts in via "Widgets auf dem Homescreen aktivieren" in Settings, matching
        // the Discreet Launcher "empty home screen" principle this app is built on. The dock row's own
        // entries are plain AppRowEntry rows, same table/machinery as any other apps row. Unlike the
        // Favorites popup (which matches the page's own light/dark mode), the dock deliberately
        // INVERTS it by default — black-on-light/white-on-dark — so it reads as its own distinct
        // surface against the page behind it; borderEnabled here now means "show the top edge line"
        // (the dock panel is flush with the screen's bottom edge, so only that one edge can have a
        // border at all — see the dock panel rendering in HomeScreen.kt).
        val dockRowId = rowDao.getForPage(homeId).find { it.type == PageRowType.SEARCH_BAR }?.id
            ?: rowDao.insert(
                PageRow(pageId = homeId, orderIndex = 0, type = PageRowType.SEARCH_BAR, borderEnabled = true)
                    .withAppearance(ElementAppearance(fill = ColorFill(color = invertedBackgroundColor()), opacity = 0.85f, borderEnabled = true))
            )

        // Same structural pattern as the dock row above — a pinned bookend on Home, rendered as its
        // own floating overlay outside the pager (see HomeScreen's top EdgeNotch) rather than inline.
        // A translucent neutral fill (not opaque white) so resolveElementColors' automatic White-on-
        // dark/Black-on-light contrast picks a sensible default without any bespoke color logic.
        if (rowDao.getForPage(homeId).none { it.type == PageRowType.PANEL }) {
            rowDao.insert(
                PageRow(pageId = homeId, orderIndex = 0, type = PageRowType.PANEL, borderEnabled = true, columns = 4, gridRows = 3)
                    .withAppearance(ElementAppearance(fill = ColorFill(color = invertedBackgroundColor()), opacity = 0.85f, borderEnabled = true))
            )
        }

        if (pageDao.getAppOverviewPage() == null) {
            val appOverviewId = pageDao.insert(
                LauncherPage(orderIndex = (pageDao.maxOrderIndex() ?: -1) + 1, isAppOverview = true, kind = LauncherPageKind.APP_LIST)
            )
            settingsRepository.setPageHideStatusBar(appOverviewId, true)
        }

        // Autonomous singleton, same pattern as the app overview above — exists and works whether or
        // not any dock slot links to it (unlike the old TEXT_SEARCH power button, which search *was*).
        if (pageDao.getByKind(LauncherPageKind.SEARCH) == null) {
            val searchId = pageDao.insert(
                LauncherPage(orderIndex = (pageDao.maxOrderIndex() ?: -1) + 1, kind = LauncherPageKind.SEARCH)
            )
            settingsRepository.setPageHideStatusBar(searchId, true)
        }

        // Same autonomous-singleton pattern again — the Favorites/Quick-overview panel used to be
        // reachable only via a hardcoded swipe-down gesture (still the case for now); as a real page
        // it's also placeable in a dock slot like Search/App overview, just not linked there by
        // default (the swipe-down path already reaches it without needing a dock slot too).
        if (pageDao.getByKind(LauncherPageKind.SIDEBAR) == null) {
            val sidebarId = pageDao.insert(
                LauncherPage(orderIndex = (pageDao.maxOrderIndex() ?: -1) + 1, kind = LauncherPageKind.SIDEBAR)
            )
            settingsRepository.setPageHideStatusBar(sidebarId, true)
        }

        // Gesture defaults, seeded once with real page ids now that Search/App-overview/Sidebar are
        // all real pages — SettingsRepository has no page-id knowledge, so it can no longer carry a
        // hardcoded SWIPE_UP fallback (removed in the same step as the APP_LIST/SEARCH enum values).
        // Guarded by "nothing configured yet" so this never clobbers a user's own choice.
        if (!settingsRepository.isGestureConfigured(HomeGesture.SWIPE_UP)) {
            pageDao.getAppOverviewPage()?.let { appOverview ->
                settingsRepository.setGestureAction(HomeGesture.SWIPE_UP, GestureAction(GestureActionType.PAGE, appOverview.id.toString()))
            }
        }
        // No more default SWIPE_DOWN -> Sidebar binding — the Favorites popup now opens exclusively
        // via its own EdgeNotch (see HomeScreen's top notch Box), same as the Dock. A user can still
        // bind a gesture to it manually (it's just another PAGE-type GestureAction target), but
        // nothing does that automatically any more.

        // Fresh-install default: the dock arrives already linking to the two singleton pages, so it
        // doesn't look sparse — mirrors the pre-merge DockRepository's own default seed. Only runs once
        // (dockRowId already had zero entries at that point), never re-applied once the user has
        // touched the row themselves.
        if (entryDao.countForRow(dockRowId) == 0) {
            val appOverviewId = pageDao.getAppOverviewPage()?.id
            val searchId = pageDao.getByKind(LauncherPageKind.SEARCH)?.id
            searchId?.let { entryDao.insert(AppRowEntry(pageRowId = dockRowId, orderIndex = 0, type = GridItemType.PAGE, payload = it.toString())) }
            appOverviewId?.let { entryDao.insert(AppRowEntry(pageRowId = dockRowId, orderIndex = 1, type = GridItemType.PAGE, payload = it.toString())) }
        }

        // Self-healing check on every start: pages used to scroll, so content sized against that
        // no longer fits now that a page is a hard, fixed screen. A no-op once everything's already
        // rebalanced (the common case), so safe to run unconditionally rather than only once ever.
        pageDao.getAllOnce().filter { it.kind == LauncherPageKind.ROWS }.forEach { rebalancePageOverflow(it.id) }
    }

    /** Every page/surface falls back to this when it has no color override of its own. */
    private suspend fun ensureGlobalBackgroundSeeded() {
        if (!settingsRepository.hasGlobalBackground()) {
            settingsRepository.setGlobalBackground(BackgroundConfig(source = BackgroundSource.COLOR, fill = ColorFill(color = defaultBackgroundColor())))
        }
    }

    /** Plain black in dark mode, plain white in light mode — no bundled wallpaper is shipped anymore. */
    private suspend fun defaultBackgroundColor(): Int {
        val isDark = when (settingsRepository.theme.first()) {
            ThemePreference.DARK -> true
            ThemePreference.LIGHT -> false
            ThemePreference.SYSTEM ->
                context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        return if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }

    /** The opposite of [defaultBackgroundColor] — for surfaces that want to read as a distinct layer
     *  against the page instead of blending into it (the dock panel's own default fill). */
    private suspend fun invertedBackgroundColor(): Int =
        if (defaultBackgroundColor() == android.graphics.Color.BLACK) android.graphics.Color.WHITE else android.graphics.Color.BLACK

    suspend fun addPage(): Long {
        val next = (pageDao.maxOrderIndex() ?: -1) + 1
        val newId = pageDao.insert(LauncherPage(orderIndex = next))
        // No per-page background to seed anymore — a page with no override of its own just falls back
        // to the global background automatically, which is exactly what every fresh page should look
        // like (matches home) until the user deliberately picks its own color.
        // Hidden status bar is the app-wide default now; a page that silently defaulted to *shown*
        // is exactly the kind of per-page mismatch that made switching to it visibly jump/settle.
        settingsRepository.setPageHideStatusBar(newId, true)
        return newId
    }

    /**
     * Adds a new ROWS page immediately to the left or right of Home **within the ROWS subset only**
     * — the carousel's own "+" tiles. Unlike [reorderPages] (a plain positional reindex over
     * whatever list it's handed, which is exactly what let the old flat Pages list cosmetically
     * "reorder" the singleton App-overview/Search/Sidebar tiles relative to swipeable pages without
     * that having any real effect on the pager, since the pager filters to ROWS before use), this
     * renumbers every page from scratch: ROWS pages in their new relative order first (the new page
     * inserted at [direction]'s position relative to Home), then every non-ROWS singleton appended
     * after, each keeping its own prior relative order among themselves. Avoids a collision-prone
     * localized index shift — page counts are small, a full stable re-sort is cheap and simple.
     */
    suspend fun addPageInDirection(direction: PageDirection): Long {
        val all = pageDao.getAllOnce()
        val rowsOnly = all.filter { it.kind == LauncherPageKind.ROWS }
        val nonRows = all.filter { it.kind != LauncherPageKind.ROWS }
        val homePos = rowsOnly.indexOfFirst { it.isHome }.coerceAtLeast(0)
        val insertPos = if (direction == PageDirection.LEFT) homePos else homePos + 1

        val newId = pageDao.insert(LauncherPage(orderIndex = 0))
        settingsRepository.setPageHideStatusBar(newId, true)
        val newPage = pageDao.getById(newId) ?: return newId

        val newRowsOrder = rowsOnly.toMutableList().apply { add(insertPos, newPage) }
        (newRowsOrder + nonRows).forEachIndexed { index, page ->
            if (page.orderIndex != index) pageDao.update(page.copy(orderIndex = index))
        }
        return newId
    }

    suspend fun removePageIfEmpty(page: LauncherPage): Boolean {
        if (page.isHome || page.isAppOverview || page.kind == LauncherPageKind.SEARCH || page.kind == LauncherPageKind.SIDEBAR) return false
        if (rowDao.countForPage(page.id) > 0) return false
        pageDao.delete(page)
        return true
    }

    suspend fun setPageIcon(pageId: Long, glyph: String?, backgroundColor: Int?, iconColor: Int?) {
        val page = pageDao.getById(pageId) ?: return
        pageDao.update(page.copy(iconGlyph = glyph, iconBackgroundColor = backgroundColor, iconColor = iconColor))
    }

    suspend fun reorderPages(ordered: List<LauncherPage>) {
        ordered.forEachIndexed { index, page ->
            if (page.orderIndex != index) pageDao.update(page.copy(orderIndex = index))
        }
    }

    suspend fun renamePage(pageId: Long, name: String?) {
        val page = pageDao.getById(pageId) ?: return
        pageDao.update(page.copy(name = name?.trim()?.takeIf { it.isNotEmpty() }))
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
        val id = rowDao.insert(row)
        rebalancePageOverflow(pageId)
        return id
    }

    suspend fun addSystemWidgetRow(pageId: Long, appWidgetId: Int, spanY: Int): Long {
        var row = PageRow(pageId = pageId, orderIndex = nextRowOrderIndex(pageId), type = PageRowType.SYSTEM_WIDGET, payload = appWidgetId.toString(), spanY = spanY)
        presetRowAppearance()?.let { row = row.withAppearance(it) }
        val id = rowDao.insert(row)
        rebalancePageOverflow(pageId)
        return id
    }

    suspend fun addAppsRow(pageId: Long): Long {
        var row = PageRow(pageId = pageId, orderIndex = nextRowOrderIndex(pageId), type = PageRowType.APPS, columns = GridConstants.DEFAULT_APPS_ROW_COLUMNS)
        presetRowAppearance()?.let { row = row.withAppearance(it) }
        val id = rowDao.insert(row)
        rebalancePageOverflow(pageId)
        return id
    }

    /** Returns how many rows on [pageId] had to be relocated to a new page as a result — see [rebalancePageOverflow]. */
    suspend fun addSearchBarRow(pageId: Long): Int {
        val next = (rowDao.maxOrderIndex(pageId) ?: -1) + 1
        var row = PageRow(pageId = pageId, orderIndex = next, type = PageRowType.SEARCH_BAR, borderEnabled = true)
        presetSearchBarAppearance()?.let { row = row.withAppearance(it) }
        rowDao.insert(row)
        return rebalancePageOverflow(pageId).size
    }

    /**
     * Pages no longer scroll — a page is exactly one screen, full stop, so whatever doesn't fit needs
     * a home somewhere else instead of silently overflowing off-screen. Walks [pageId]'s rows (the
     * dock/search bar row excluded — it floats separately, not part of this stack) in order, and the
     * moment the running total would exceed what's actually available (accounting for status bar, the
     * dock if present, and a safety margin — the exact same budget the height stepper enforces per
     * row), that row and everything after it move to a fresh page. A row that's still too tall even by
     * itself on that fresh (dock-less) page — the most room any row can ever get — is clamped down to
     * that maximum directly instead of being moved again and again, which would otherwise never
     * terminate. Recurses in case the fresh page overflows too (e.g. several oversized rows at once).
     */
    private suspend fun rebalancePageOverflow(pageId: Long): List<PageRow> {
        val allRows = rowDao.getForPage(pageId)
        val dockRow = allRows.lastOrNull { it.type == PageRowType.SEARCH_BAR }
        // The Panel (like the dock) floats outside the pager rather than rendering inline — see
        // HomeScreen's top EdgeNotch Box — so it never counts toward this page's inline height budget.
        val stackedRows = allRows.filter { it.id != dockRow?.id && it.type != PageRowType.PANEL }
        if (stackedRows.isEmpty()) return emptyList()

        val dm = context.resources.displayMetrics
        val density = androidx.compose.ui.unit.Density(dm.density)
        val (topInsetPx, bottomInsetPx) = systemInsetsPx()
        val budgetPx = com.bisc.flauncher.ui.home.safeSystemWidgetMaxHeightPx(
            pageHasDock = dockRow != null,
            screenWidthPx = dm.widthPixels,
            screenHeightPx = dm.heightPixels,
            densityValue = dm.density,
            statusBarPx = topInsetPx,
            navigationBarPx = bottomInsetPx
        )
        val spacingPx = with(density) { com.bisc.flauncher.ui.home.PageRowDimens.rowSpacing.toPx().roundToInt() }

        suspend fun heightPxOf(row: PageRow): Int = row.heightPx ?: with(density) {
            val heightDp = if (row.type == PageRowType.APPS) {
                com.bisc.flauncher.ui.home.PageRowDimens.appsRowHeight(entryDao.countForRow(row.id), row.columns.coerceAtLeast(1))
            } else {
                com.bisc.flauncher.ui.home.PageRowDimens.rowUnitHeight * row.spanY
            }
            heightDp.toPx().roundToInt()
        }

        var used = 0
        val overflow = mutableListOf<PageRow>()
        for (row in stackedRows) {
            val needed = used + (if (used > 0) spacingPx else 0) + heightPxOf(row)
            if (needed > budgetPx) {
                overflow.add(row)
            } else {
                used = needed
            }
        }
        if (overflow.isEmpty()) return emptyList()

        val newPageId = addPage()
        // A fresh page never has a dock yet, so this is the most room any of these rows could ever get.
        val freshPageBudgetPx = com.bisc.flauncher.ui.home.safeSystemWidgetMaxHeightPx(
            pageHasDock = false,
            screenWidthPx = dm.widthPixels,
            screenHeightPx = dm.heightPixels,
            densityValue = dm.density,
            statusBarPx = topInsetPx,
            navigationBarPx = bottomInsetPx
        )
        var nextOrder = (rowDao.maxOrderIndex(newPageId) ?: -1) + 1
        overflow.forEach { row ->
            var moved = row.copy(pageId = newPageId, orderIndex = nextOrder)
            if (moved.type == PageRowType.SYSTEM_WIDGET && heightPxOf(moved) > freshPageBudgetPx) {
                moved = moved.copy(heightPx = freshPageBudgetPx)
            }
            rowDao.update(moved)
            nextOrder++
        }
        return overflow + rebalancePageOverflow(newPageId)
    }

    /**
     * Real top/bottom system-bar insets in px, for [rebalancePageOverflow]'s budget — this runs outside
     * any Compose tree, so it can't just read WindowInsets. Falls back to the (less precise) internal
     * status_bar_height/navigation_bar_height dimens pre-API 30 — those alone can under-report the real
     * top inset on devices with a display cutout, which is exactly what let a widget be sized taller
     * than what actually fits.
     */
    private fun systemInsetsPx(): Pair<Int, Int> {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            runCatching {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                val insets = wm.currentWindowMetrics.windowInsets
                val statusBarTop = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top
                val cutoutTop = insets.displayCutout?.safeInsetTop ?: 0
                val navBottom = insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
                return maxOf(statusBarTop, cutoutTop) to navBottom
            }
        }
        return systemInsetPx("status_bar_height") to systemInsetPx("navigation_bar_height")
    }

    private fun systemInsetPx(name: String): Int {
        val resId = context.resources.getIdentifier(name, "dimen", "android")
        return if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
    }

    suspend fun setRowSpanY(row: PageRow, spanY: Int) {
        rowDao.update(row.copy(spanY = spanY))
        rebalancePageOverflow(row.pageId)
    }

    /** Exact-pixel sizing for a SYSTEM_WIDGET row; null reverts it to the ordinary spanY-based height. */
    suspend fun setRowHeightPx(row: PageRow, heightPx: Int?) {
        rowDao.update(row.copy(heightPx = heightPx))
        rebalancePageOverflow(row.pageId)
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
        // Fewer columns means more icon rows, i.e. taller — can push the page over budget too.
        rebalancePageOverflow(row.pageId)
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

    /** An apps row grows taller as entries are added to it — this can push a page over budget too. */
    private suspend fun rebalanceForRow(pageRowId: Long) {
        rowDao.getById(pageRowId)?.let { rebalancePageOverflow(it.pageId) }
    }

    suspend fun addAppEntry(pageRowId: Long, componentName: String): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(AppRowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.APP, payload = componentName))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addFolderEntry(pageRowId: Long, folderId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(AppRowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.FOLDER, payload = folderId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addShortcutEntry(pageRowId: Long, packageName: String, shortcutId: String, label: String): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(
            AppRowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.SHORTCUT, payload = "$packageName|$shortcutId|$label")
        )
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addMapTargetEntry(pageRowId: Long, targetId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(AppRowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.MAP_TARGET, payload = targetId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addWebsiteEntry(pageRowId: Long, websiteId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(AppRowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.WEBSITE, payload = websiteId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addPageLinkEntry(pageRowId: Long, pageId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(AppRowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.PAGE, payload = pageId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    /** [contactId] is a [com.bisc.flauncher.data.model.PinnedContact.contactId] — the caller (see
     *  HomeViewModel.addContactEntryToRow) ensures that pin exists first, same as this row just
     *  referencing an existing folder/website/map target by id. */
    suspend fun addContactEntry(pageRowId: Long, contactId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(AppRowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.CONTACT, payload = contactId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun setRowPillMode(row: PageRow, pillMode: Boolean) {
        rowDao.update(row.copy(pillMode = pillMode))
    }

    suspend fun setRowGridRows(row: PageRow, rows: Int) {
        rowDao.update(row.copy(gridRows = rows.coerceIn(1, 6)))
    }

    suspend fun setRowDockVisibilityMode(row: PageRow, mode: com.bisc.flauncher.data.model.DockVisibilityMode) {
        rowDao.update(row.copy(dockVisibilityMode = mode))
    }

    suspend fun setRowEnabled(row: PageRow, enabled: Boolean) {
        rowDao.update(row.copy(enabled = enabled))
    }

    suspend fun setRowShowLabels(row: PageRow, showLabels: Boolean) {
        rowDao.update(row.copy(showLabels = showLabels))
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
