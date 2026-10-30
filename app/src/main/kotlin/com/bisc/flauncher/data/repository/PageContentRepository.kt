package com.bisc.flauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import com.bisc.flauncher.R
import com.bisc.flauncher.data.db.RowEntryDao
import com.bisc.flauncher.data.db.LauncherPageDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.model.ActionButton
import com.bisc.flauncher.data.model.ActionButtonActionType
import com.bisc.flauncher.data.model.RowEntry
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.BackgroundSource
import com.bisc.flauncher.data.model.ColorFill
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
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.data.model.withDockRows
import com.bisc.flauncher.data.model.withPanelRows
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.theme.backgroundColorFor
import com.bisc.flauncher.ui.theme.defaultBoxAppearanceFor
import com.bisc.flauncher.ui.theme.isDark
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import androidx.compose.ui.unit.dp

/** Pages, page rows and row entries, including seeding of the default pages. */
class PageContentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pageDao: LauncherPageDao,
    private val rowDao: PageRowDao,
    private val entryDao: RowEntryDao,
    private val settingsRepository: SettingsRepository,
    private val actionButtonRepository: ActionButtonRepository
) {
    val allPages: Flow<List<LauncherPage>> = pageDao.observeAll()

    val pages: Flow<List<LauncherPage>> = allPages.map { list -> list.filter { it.kind == LauncherPageKind.ROWS } }

    val appOverviewPageId: Flow<Long?> = allPages.map { list -> list.firstOrNull { it.kind == LauncherPageKind.APP_LIST }?.id }
    val rows: Flow<List<PageRow>> = rowDao.observeAll()
    val rowEntries: Flow<List<RowEntry>> = entryDao.observeAll()

    suspend fun ensureSeeded() {
        ensureGlobalBackgroundSeeded()
        val existingHome = pageDao.getHomePage()
        val homeId = existingHome?.id ?: pageDao.insert(LauncherPage(orderIndex = 0, isHome = true)).also { newHomeId ->
            settingsRepository.setPageHideStatusBar(newHomeId, true)
        }
        val isDark = isDark()

        // Home starts with just the dock - no clock, no widgets. Widgets (the clock included) only
        // appear once the user opts in via "Widgets auf dem Homescreen aktivieren" in Settings, matching
        // the Discreet Launcher "empty home screen" principle this app is built on. The dock row's own
        // entries are plain RowEntry rows, same table/machinery as any other row. The dock deliberately
        // takes the *opposite* tone from the page (see defaultBoxAppearanceFor) so it reads as its own
        // distinct surface against the page behind it; borderEnabled here now means "show the top edge
        // line" (the dock panel is flush with the screen's bottom edge, so only that one edge can have
        // a border at all - see the dock panel rendering in HomeScreen.kt).
        val dockRowId = rowDao.getForPage(homeId).find { it.type == PageRowType.SEARCH_BAR }?.id
            ?: rowDao.insert(
                PageRow(pageId = homeId, orderIndex = 0, type = PageRowType.SEARCH_BAR, borderEnabled = true)
                    .withAppearance(defaultBoxAppearanceFor(isDark))
            )

        // Same structural pattern as the dock row above - a pinned bookend on Home, rendered as its
        // own floating overlay outside the pager (see HomeScreen's Panel AnimatedVisibility block)
        // rather than inline.
        if (rowDao.getForPage(homeId).none { it.type == PageRowType.PANEL }) {
            rowDao.insert(
                PageRow(pageId = homeId, orderIndex = 0, type = PageRowType.PANEL, borderEnabled = true)
                    .withAppearance(defaultBoxAppearanceFor(isDark))
            )
        }

        if (pageDao.getAppOverviewPage() == null) {
            val appOverviewId = pageDao.insert(
                LauncherPage(orderIndex = (pageDao.maxOrderIndex() ?: -1) + 1, isAppOverview = true, kind = LauncherPageKind.APP_LIST)
            )
            settingsRepository.setPageHideStatusBar(appOverviewId, true)
        }

        // Autonomous singleton, same pattern as the app overview above - exists and works whether or
        // not any dock slot links to it (unlike the old, now-deleted TEXT_SEARCH dock button, which
        // search *was*, predating today's ActionButton Element entirely).
        if (pageDao.getByKind(LauncherPageKind.SEARCH) == null) {
            val searchId = pageDao.insert(
                LauncherPage(orderIndex = (pageDao.maxOrderIndex() ?: -1) + 1, kind = LauncherPageKind.SEARCH)
            )
            settingsRepository.setPageHideStatusBar(searchId, true)
        }

        // Gesture defaults, seeded with real page ids (SettingsRepository knows no page ids).
        // Guarded by "nothing configured yet" so this never clobbers a user's own choice.
        if (!settingsRepository.isGestureConfigured(HomeGesture.SWIPE_UP)) {
            pageDao.getAppOverviewPage()?.let { appOverview ->
                settingsRepository.setGestureAction(HomeGesture.SWIPE_UP, GestureAction(GestureActionType.PAGE, appOverview.id.toString()))
            }
        }
        // No default SWIPE_DOWN binding - the Panel claims plain swipe-down on the home content
        // outright whenever it's enabled (see HomeScreen's detectHomeGestures wiring).

        // Settings needs a default path from the home screen itself - otherwise, on a fresh install,
        // the only way in would be the system "App info" entry or the static app shortcut (both
        // outside this app's own UI). Long-press is free by default and matches the Android norm.
        if (!settingsRepository.isGestureConfigured(HomeGesture.LONG_PRESS)) {
            settingsRepository.setGestureAction(HomeGesture.LONG_PRESS, GestureAction(GestureActionType.SETTINGS, null))
        }

        // Fresh-install default: the dock (ALWAYS_VISIBLE by default, see PageRow.dockVisibilityMode)
        // arrives with 5 ready-to-use entries so it doesn't look sparse - Android Settings, Camera,
        // Browser and Phone (each resolved to this device's actual default handler via
        // resolveLauncherComponentFor, not a hardcoded package name, so it works the same across
        // OEM skins/emulators), plus a button back into FLauncher's own Settings. Only runs once
        // (dockRowId already had zero entries at that point), never re-applied once the user has
        // touched the row themselves.
        if (entryDao.countForRow(dockRowId) == 0) {
            var order = 0
            resolveLauncherComponentFor(Intent(android.provider.Settings.ACTION_SETTINGS))?.let { component ->
                entryDao.insert(RowEntry(pageRowId = dockRowId, orderIndex = order++, type = GridItemType.APP, payload = component.flattenToString()))
            }
            resolveLauncherComponentFor(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))?.let { component ->
                entryDao.insert(RowEntry(pageRowId = dockRowId, orderIndex = order++, type = GridItemType.APP, payload = component.flattenToString()))
            }
            resolveLauncherComponentFor(Intent(Intent.ACTION_VIEW, Uri.parse("https://")).addCategory(Intent.CATEGORY_BROWSABLE))?.let { component ->
                entryDao.insert(RowEntry(pageRowId = dockRowId, orderIndex = order++, type = GridItemType.APP, payload = component.flattenToString()))
            }
            resolveLauncherComponentFor(Intent(Intent.ACTION_DIAL))?.let { component ->
                entryDao.insert(RowEntry(pageRowId = dockRowId, orderIndex = order++, type = GridItemType.APP, payload = component.flattenToString()))
            }
            val settingsButtonId = actionButtonRepository.add(
                ActionButton(label = context.getString(R.string.settings_title), actionType = ActionButtonActionType.LAUNCHER_SETTINGS)
            )
            entryDao.insert(RowEntry(pageRowId = dockRowId, orderIndex = order++, type = GridItemType.ACTION_BUTTON, payload = settingsButtonId.toString()))
        }

        // The Dock and the Panel belong to Home and show on every page; other pages never carry
        // their own (older versions copied them onto new pages, and a backup may still contain them).
        pageDao.getAllOnce().filter { !it.isHome }.forEach { page ->
            rowDao.getForPage(page.id)
                .filter { it.type == PageRowType.SEARCH_BAR || it.type == PageRowType.PANEL }
                .forEach { rowDao.delete(it) }
        }

        // Self-healing check on every start: a page is one fixed screen, so anything that doesn't fit
        // moves on. A no-op once everything fits (the common case), so safe to run every time.
        pageDao.getAllOnce().filter { it.kind == LauncherPageKind.ROWS }.forEach { rebalancePageOverflow(it.id) }
    }

    /** Finds [intent]'s current default handler, then resolves that same package's actual
     *  ACTION_MAIN/CATEGORY_LAUNCHER activity - not the (possibly different, non-launchable)
     *  activity that narrowly handles [intent] itself - so the result is always safe to hand to
     *  [com.bisc.flauncher.util.launchApp], which requires a real launcher activity. Returns null
     *  if the device has no default handler for it (e.g. no camera app on some emulator images). */
    private fun resolveLauncherComponentFor(intent: Intent): ComponentName? {
        val packageManager = context.packageManager
        val targetPackage = runCatching {
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        }.getOrNull() ?: return null
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(targetPackage)
        val launcherActivity = runCatching { packageManager.resolveActivity(launcherIntent, 0)?.activityInfo }.getOrNull() ?: return null
        return ComponentName(launcherActivity.packageName, launcherActivity.name)
    }

    /** Every page/surface falls back to this when it has no color override of its own. */
    private suspend fun ensureGlobalBackgroundSeeded() {
        if (!settingsRepository.hasGlobalBackground()) {
            settingsRepository.setGlobalBackground(BackgroundConfig(source = BackgroundSource.COLOR, fill = ColorFill(color = backgroundColorFor(isDark()))))
        }
    }

    /** Resolves ThemePreference.SYSTEM against the live device setting - no bundled wallpaper is
     *  shipped, Light/Dark/System (see ui.theme.Theme.kt) is the only appearance control there is. */
    private suspend fun isDark(): Boolean = settingsRepository.theme.first().isDark(context)

    suspend fun addPage(): Long {
        val next = (pageDao.maxOrderIndex() ?: -1) + 1
        val newId = pageDao.insert(LauncherPage(orderIndex = next))
        // No background to seed: a page without its own override uses the global background.
        // Hidden status bar is the app-wide default now; a page that silently defaulted to *shown*
        // is exactly the kind of per-page mismatch that made switching to it visibly jump/settle.
        settingsRepository.setPageHideStatusBar(newId, true)
        return newId
    }

    /**
     * Adds a new ROWS page immediately to the left or right of Home **within the ROWS subset only**
     * - the carousel's own "+" tiles. Like [reorderRowsPages], this renumbers every page from
     * scratch: ROWS pages in their new relative order first (the new page
     * inserted at [direction]'s position relative to Home), then every non-ROWS singleton appended
     * after, each keeping its own prior relative order among themselves. Avoids a collision-prone
     * localized index shift - page counts are small, a full stable re-sort is cheap and simple.
     */
    // New pages start as an independent copy of Home's current *structure* - same row types,
    // order, appearance and background - not of its actual content (pinned apps/folders stay on
    // Home). The Dock and Panel aren't copied: they're Home's and show on every page anyway. Once
    // created, the new page is fully independent - editing it never touches Home back.
    suspend fun addPageInDirection(direction: PageDirection): Long {
        val all = pageDao.getAllOnce()
        val rowsOnly = all.filter { it.kind == LauncherPageKind.ROWS }
        val nonRows = all.filter { it.kind != LauncherPageKind.ROWS }
        val homePage = rowsOnly.find { it.isHome }
        val homePos = rowsOnly.indexOfFirst { it.isHome }.coerceAtLeast(0)
        val insertPos = if (direction == PageDirection.LEFT) homePos else homePos + 1

        val newId = pageDao.insert(LauncherPage(orderIndex = 0))
        settingsRepository.setPageHideStatusBar(newId, true)
        val newPage = pageDao.getById(newId) ?: return newId

        if (homePage != null) {
            rowDao.getForPage(homePage.id)
                .filter { it.type != PageRowType.SEARCH_BAR && it.type != PageRowType.PANEL }
                .forEach { row -> rowDao.insert(row.copy(id = 0, pageId = newId)) }
            settingsRepository.pageBackgroundOverride(homePage.id).first()?.let { override ->
                settingsRepository.setPageBackgroundConfig(newId, override)
            }
        }

        val newRowsOrder = rowsOnly.toMutableList().apply { add(insertPos, newPage) }
        (newRowsOrder + nonRows).forEachIndexed { index, page ->
            if (page.orderIndex != index) pageDao.update(page.copy(orderIndex = index))
        }
        return newId
    }

    /** Deletes a removable page together with everything on it (its rows and their entries cascade
     *  with it). Returns the deleted rows so the caller can free their system-widget ids, or null
     *  if this page can't be removed (Home and the utility pages). */
    suspend fun removePage(page: LauncherPage): List<PageRow>? {
        if (page.isHome || page.isAppOverview || page.kind == LauncherPageKind.SEARCH) return null
        val rows = rowDao.getForPage(page.id)
        pageDao.delete(page)
        return rows
    }

    suspend fun setPageIcon(pageId: Long, glyph: String?, backgroundColor: Int?, iconColor: Int?) {
        val page = pageDao.getById(pageId) ?: return
        pageDao.update(page.copy(iconGlyph = glyph, iconBackgroundColor = backgroundColor, iconColor = iconColor))
    }

    /** The carousel's drag-to-reorder: [orderedRowsPageIds] is the new order of the swipeable ROWS
     *  pages (Home included); every page is renumbered from scratch, ROWS first in that order, then
     *  the non-ROWS singletons in their prior relative order - same scheme as [addPageInDirection]. */
    suspend fun reorderRowsPages(orderedRowsPageIds: List<Long>) {
        val all = pageDao.getAllOnce()
        val byId = all.associateBy { it.id }
        val rows = orderedRowsPageIds.mapNotNull { byId[it] }.filter { it.kind == LauncherPageKind.ROWS }
        // Any ROWS page missing from the list (e.g. added meanwhile) keeps its place at the end.
        val missingRows = all.filter { it.kind == LauncherPageKind.ROWS && it.id !in orderedRowsPageIds }.sortedBy { it.orderIndex }
        val nonRows = all.filter { it.kind != LauncherPageKind.ROWS }.sortedBy { it.orderIndex }
        (rows + missingRows + nonRows).forEachIndexed { index, page ->
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

    suspend fun addWidgetRow(pageId: Long, kind: String, spanY: Int = WidgetKind.defaultSpan(kind)): Long {
        var row = PageRow(
            pageId = pageId, orderIndex = nextRowOrderIndex(pageId), type = PageRowType.WIDGET, payload = kind, spanY = spanY,
            // Widgets start only slightly rounded, whatever the global corner style; the global
            // corner setting still restamps them when it's changed (see its hint in Design).
            cornerRadiusPercent = com.bisc.flauncher.ui.theme.DEFAULT_WIDGET_CORNER_RADIUS_PERCENT
        )
        row = row.withAppearance(defaultBoxAppearanceFor(isDark()))
        val id = rowDao.insert(row)
        rebalancePageOverflow(pageId)
        return id
    }

    suspend fun addSystemWidgetRow(pageId: Long, appWidgetId: Int, spanY: Int): Long {
        var row = PageRow(
            pageId = pageId, orderIndex = nextRowOrderIndex(pageId), type = PageRowType.SYSTEM_WIDGET, payload = appWidgetId.toString(), spanY = spanY,
            // Widgets start only slightly rounded, whatever the global corner style; the global
            // corner setting still restamps them when it's changed (see its hint in Design).
            cornerRadiusPercent = com.bisc.flauncher.ui.theme.DEFAULT_WIDGET_CORNER_RADIUS_PERCENT
        )
        row = row.withAppearance(defaultBoxAppearanceFor(isDark()))
        val id = rowDao.insert(row)
        rebalancePageOverflow(pageId)
        return id
    }

    suspend fun addElementBox(pageId: Long): Long {
        var row = PageRow(
            pageId = pageId, orderIndex = nextRowOrderIndex(pageId), type = PageRowType.ELEMENT_BOX, columns = GridConstants.DEFAULT_ELEMENT_BOX_COLUMNS,
            cornerRadiusPercent = settingsRepository.defaultCornerRadiusPercent.first()
        )
        row = row.withAppearance(defaultBoxAppearanceFor(isDark()))
        val id = rowDao.insert(row)
        rebalancePageOverflow(pageId)
        return id
    }

    /** Home's Dock when it's switched on - it shows on every page, so every page reserves room for it. */
    private suspend fun shownDockRow(): PageRow? =
        pageDao.getHomePage()?.let { home -> rowDao.getForPage(home.id).find { it.type == PageRowType.SEARCH_BAR } }
            ?.takeIf { it.enabled }

    /**
     * [pageId]'s row grid, computed exactly like HomeScreen lays the page out: how many row units fit,
     * and the unit height stretched so they fill the page (see [com.bisc.flauncher.ui.home.PageRowDimens.stretchedUnit]).
     */
    private suspend fun pageGrid(pageId: Long): com.bisc.flauncher.ui.home.PageGrid {
        val density = context.resources.displayMetrics.density
        val insets = systemInsetsPx()
        val hidesStatusBar = settingsRepository.pageHideStatusBar(pageId).first()
        val topInsetPx = if (hidesStatusBar) insets.cutoutTop else maxOf(insets.statusBarTop, insets.cutoutTop)
        return com.bisc.flauncher.ui.home.PageGrid.of(
            windowHeight = (windowHeightPx() / density).dp,
            topInset = (topInsetPx / density).dp,
            navigationBar = (insets.navigationBottom / density).dp,
            dock = shownDockRow()
        )
    }

    /** How many grid units [row] takes on a page with [unit]-tall rows: its span, or for rows whose
     *  height comes from content or a free pixel value, that height rounded up to whole units. */
    private suspend fun unitsOf(row: PageRow, unit: androidx.compose.ui.unit.Dp): Int {
        val dims = com.bisc.flauncher.ui.home.PageRowDimens
        val density = context.resources.displayMetrics.density
        val exactPx = row.heightPx
        return when {
            row.type == PageRowType.ELEMENT_BOX ->
                dims.unitsFor(dims.elementBoxHeight(entryDao.countForRow(row.id), row.columns.coerceAtLeast(1)), unit)
            // 1dp of slack: a height set to exactly N rows in the editor stays N rows despite rounding.
            exactPx != null && exactPx > 0 -> dims.unitsFor((exactPx / density).dp - 1.dp, unit)
            else -> row.spanY.coerceAtLeast(1)
        }
    }

    /**
     * Dry-run check for the "this page is full" prompt (offer a new page instead of silently
     * relocating rows there): would a not-yet-inserted row shaped like
     * [candidateRow] still fit on [pageId] alongside what's already there? Mirrors
     * [rebalancePageOverflow]'s own math exactly, just without writing anything.
     */
    suspend fun wouldOverflow(pageId: Long, candidateRow: PageRow): Boolean {
        if (candidateRow.type == PageRowType.SEARCH_BAR) return false // floats separately, never part of this budget
        val grid = pageGrid(pageId)
        val used = rowDao.getForPage(pageId)
            .filter { it.type != PageRowType.SEARCH_BAR && it.type != PageRowType.PANEL }
            .sumOf { unitsOf(it, grid.unit) }
        return used + unitsOf(candidateRow, grid.unit) > grid.unitCount
    }

    /**
     * A page is exactly one screen, so whatever doesn't fit needs a home somewhere else instead of
     * silently overflowing off-screen. Walks [pageId]'s rows in order, counting row units (see
     * [pageGrid]); the moment they'd exceed the page's unit count, that row and everything after it
     * move to a fresh page. A free-height widget taller than a whole page is clamped to one, so moving
     * it always terminates. Recurses in case the fresh page overflows too.
     */
    private suspend fun rebalancePageOverflow(pageId: Long): List<PageRow> {
        // The Dock and the Panel float outside the pager rather than rendering inline, so they never
        // count toward a page's rows (the Dock's room is reserved by the grid itself).
        val stackedRows = rowDao.getForPage(pageId).filter { it.type != PageRowType.SEARCH_BAR && it.type != PageRowType.PANEL }
        if (stackedRows.isEmpty()) return emptyList()

        val grid = pageGrid(pageId)
        var used = 0
        val overflow = mutableListOf<PageRow>()
        for (row in stackedRows) {
            val units = unitsOf(row, grid.unit)
            if (used + units > grid.unitCount) overflow.add(row) else used += units
        }
        if (overflow.isEmpty()) return emptyList()

        val newPageId = addPage()
        val freshGrid = pageGrid(newPageId)
        val fullPagePx = (com.bisc.flauncher.ui.home.PageRowDimens.spanHeight(freshGrid.unit, freshGrid.unitCount).value *
            context.resources.displayMetrics.density).toInt()
        var nextOrder = (rowDao.maxOrderIndex(newPageId) ?: -1) + 1
        overflow.forEach { row ->
            var moved = row.copy(pageId = newPageId, orderIndex = nextOrder)
            val exactPx = moved.heightPx
            if (moved.type == PageRowType.SYSTEM_WIDGET && exactPx != null && exactPx > fullPagePx) {
                moved = moved.copy(heightPx = fullPagePx)
            }
            rowDao.update(moved)
            nextOrder++
        }
        return overflow + rebalancePageOverflow(newPageId)
    }

    private data class SystemInsets(val statusBarTop: Int, val cutoutTop: Int, val navigationBottom: Int)

    /**
     * Real system-bar insets in px - this runs outside any Compose tree, so it can't just read
     * WindowInsets. The status bar's height counts even while it's hidden (a page decides that on its
     * own, see [pageGrid]). Falls back to the internal status_bar_height/navigation_bar_height dimens
     * pre-API 30.
     */
    private fun systemInsetsPx(): SystemInsets {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            runCatching {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                val insets = wm.currentWindowMetrics.windowInsets
                return SystemInsets(
                    statusBarTop = insets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.statusBars()).top,
                    cutoutTop = insets.displayCutout?.safeInsetTop ?: 0,
                    navigationBottom = insets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.navigationBars()).bottom
                )
            }
        }
        return SystemInsets(systemInsetPx("status_bar_height"), 0, systemInsetPx("navigation_bar_height"))
    }

    /** The full window height (the launcher draws edge to edge, under both system bars). */
    private fun windowHeightPx(): Int {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            runCatching {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                return wm.currentWindowMetrics.bounds.height()
            }
        }
        return context.resources.displayMetrics.heightPixels
    }

    /** How many row units fit on each carousel page - HomeScreen and the widget editor stretch the
     *  unit with exactly this count, so a full page ends exactly one row gap above the Dock. */
    suspend fun rowUnitCounts(): Map<Long, Int> =
        pageDao.getAllOnce().filter { it.kind == LauncherPageKind.ROWS }.associate { it.id to pageGrid(it.id).unitCount }

    private fun systemInsetPx(name: String): Int {
        val resId = context.resources.getIdentifier(name, "dimen", "android")
        return if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
    }

    /** Writes every field of an edited row at once (an editor screen's Save), then re-fits the
     *  page since size, height or column changes can push it over its budget. */
    suspend fun saveRow(row: PageRow) {
        rowDao.update(row.copy(columns = row.columns.coerceIn(GridConstants.MIN_ELEMENT_BOX_COLUMNS, GridConstants.MAX_ELEMENT_BOX_COLUMNS)))
        rebalancePageOverflow(row.pageId)
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

    suspend fun setRowCornerRadiusPercent(row: PageRow, percent: Int) {
        rowDao.update(row.copy(cornerRadiusPercent = percent))
    }

    suspend fun applyCornerRadiusToAllRows(percent: Int) {
        val rows = rowDao.observeAll().first().map { it.copy(cornerRadiusPercent = percent) }
        rowDao.updateAll(rows)
    }

    suspend fun setRowEdgeToEdge(row: PageRow, edgeToEdge: Boolean) {
        rowDao.update(row.copy(edgeToEdge = edgeToEdge))
    }

    suspend fun setRowColumns(row: PageRow, columns: Int) {
        rowDao.update(row.copy(columns = columns.coerceIn(GridConstants.MIN_ELEMENT_BOX_COLUMNS, GridConstants.MAX_ELEMENT_BOX_COLUMNS)))
        // Fewer columns means more icon rows, i.e. taller - can push the page over budget too.
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

    /** An element box grows taller as entries are added to it - this can push a page over budget too. */
    private suspend fun rebalanceForRow(pageRowId: Long) {
        rowDao.getById(pageRowId)?.let { rebalancePageOverflow(it.pageId) }
    }

    suspend fun addAppEntry(pageRowId: Long, componentName: String, orderIndex: Int? = null): Long {
        val next = orderIndex ?: ((entryDao.maxOrderIndex(pageRowId) ?: -1) + 1)
        val id = entryDao.insert(RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.APP, payload = componentName))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addFolderEntry(pageRowId: Long, folderId: Long, orderIndex: Int? = null): Long {
        val next = orderIndex ?: ((entryDao.maxOrderIndex(pageRowId) ?: -1) + 1)
        val id = entryDao.insert(RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.FOLDER, payload = folderId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addShortcutEntry(pageRowId: Long, packageName: String, shortcutId: String, label: String, orderIndex: Int? = null): Long {
        val next = orderIndex ?: ((entryDao.maxOrderIndex(pageRowId) ?: -1) + 1)
        val id = entryDao.insert(
            RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.SHORTCUT, payload = "$packageName|$shortcutId|$label")
        )
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addMapTargetEntry(pageRowId: Long, targetId: Long, orderIndex: Int? = null): Long {
        val next = orderIndex ?: ((entryDao.maxOrderIndex(pageRowId) ?: -1) + 1)
        val id = entryDao.insert(RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.MAP_TARGET, payload = targetId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addWebsiteEntry(pageRowId: Long, websiteId: Long, orderIndex: Int? = null): Long {
        val next = orderIndex ?: ((entryDao.maxOrderIndex(pageRowId) ?: -1) + 1)
        val id = entryDao.insert(RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.WEBSITE, payload = websiteId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addPageLinkEntry(pageRowId: Long, pageId: Long, orderIndex: Int? = null): Long {
        val next = orderIndex ?: ((entryDao.maxOrderIndex(pageRowId) ?: -1) + 1)
        val id = entryDao.insert(RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.PAGE, payload = pageId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    /** [contactId] is a [com.bisc.flauncher.data.model.PinnedContact.contactId] - the caller (see
     *  HomeViewModel.addContactEntryToRow) ensures that pin exists first, same as this row just
     *  referencing an existing folder/website/map target by id. */
    suspend fun addContactEntry(pageRowId: Long, contactId: Long, orderIndex: Int? = null): Long {
        val next = orderIndex ?: ((entryDao.maxOrderIndex(pageRowId) ?: -1) + 1)
        val id = entryDao.insert(RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.CONTACT, payload = contactId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addActionButtonEntry(pageRowId: Long, actionButtonId: Long, orderIndex: Int? = null): Long {
        val next = orderIndex ?: ((entryDao.maxOrderIndex(pageRowId) ?: -1) + 1)
        val id = entryDao.insert(RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.ACTION_BUTTON, payload = actionButtonId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun setRowPillMode(row: PageRow, pillMode: Boolean) {
        rowDao.update(row.copy(pillMode = pillMode))
    }

    suspend fun setPanelRowColumns(row: PageRow, rows: List<Int>) {
        rowDao.update(row.withPanelRows(rows))
    }

    suspend fun setDockRowColumns(row: PageRow, rows: List<Int>) {
        rowDao.update(row.withDockRows(rows))
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

    suspend fun removeEntry(entry: RowEntry) {
        entryDao.delete(entry)
    }

    suspend fun reorderEntries(orderedEntries: List<RowEntry>) {
        orderedEntries.forEachIndexed { index, entry ->
            if (entry.orderIndex != index) entryDao.update(entry.copy(orderIndex = index))
        }
    }
}
