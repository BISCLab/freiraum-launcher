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
import kotlin.math.roundToInt

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

        // Home starts with just the dock — no clock, no widgets. Widgets (the clock included) only
        // appear once the user opts in via "Widgets auf dem Homescreen aktivieren" in Settings, matching
        // the Discreet Launcher "empty home screen" principle this app is built on. The dock row's own
        // entries are plain RowEntry rows, same table/machinery as any other row. The dock deliberately
        // takes the *opposite* tone from the page (see defaultBoxAppearanceFor) so it reads as its own
        // distinct surface against the page behind it; borderEnabled here now means "show the top edge
        // line" (the dock panel is flush with the screen's bottom edge, so only that one edge can have
        // a border at all — see the dock panel rendering in HomeScreen.kt).
        val dockRowId = rowDao.getForPage(homeId).find { it.type == PageRowType.SEARCH_BAR }?.id
            ?: rowDao.insert(
                PageRow(pageId = homeId, orderIndex = 0, type = PageRowType.SEARCH_BAR, borderEnabled = true)
                    .withAppearance(defaultBoxAppearanceFor(isDark))
            )

        // Same structural pattern as the dock row above — a pinned bookend on Home, rendered as its
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

        // Autonomous singleton, same pattern as the app overview above — exists and works whether or
        // not any dock slot links to it (unlike the old, now-deleted TEXT_SEARCH dock button, which
        // search *was*, predating today's ActionButton Element entirely).
        if (pageDao.getByKind(LauncherPageKind.SEARCH) == null) {
            val searchId = pageDao.insert(
                LauncherPage(orderIndex = (pageDao.maxOrderIndex() ?: -1) + 1, kind = LauncherPageKind.SEARCH)
            )
            settingsRepository.setPageHideStatusBar(searchId, true)
        }

        // Gesture defaults, seeded once with real page ids now that Search/App-overview are
        // all real pages — SettingsRepository has no page-id knowledge, so it can no longer carry a
        // hardcoded SWIPE_UP fallback (removed in the same step as the APP_LIST/SEARCH enum values).
        // Guarded by "nothing configured yet" so this never clobbers a user's own choice.
        if (!settingsRepository.isGestureConfigured(HomeGesture.SWIPE_UP)) {
            pageDao.getAppOverviewPage()?.let { appOverview ->
                settingsRepository.setGestureAction(HomeGesture.SWIPE_UP, GestureAction(GestureActionType.PAGE, appOverview.id.toString()))
            }
        }
        // No default SWIPE_DOWN binding — the Panel claims plain swipe-down on the home content
        // outright whenever it's enabled (see HomeScreen's detectHomeGestures wiring).

        // Settings needs a default path from the home screen itself — otherwise, on a fresh install,
        // the only way in would be the system "App info" entry or the static app shortcut (both
        // outside this app's own UI). Long-press is free by default and matches the Android norm.
        if (!settingsRepository.isGestureConfigured(HomeGesture.LONG_PRESS)) {
            settingsRepository.setGestureAction(HomeGesture.LONG_PRESS, GestureAction(GestureActionType.SETTINGS, null))
        }

        // Fresh-install default: the dock (ALWAYS_VISIBLE by default, see PageRow.dockVisibilityMode)
        // arrives with 5 ready-to-use entries so it doesn't look sparse — Android Settings, Camera,
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

        // Self-healing check on every start: pages used to scroll, so content sized against that
        // no longer fits now that a page is a hard, fixed screen. A no-op once everything's already
        // rebalanced (the common case), so safe to run unconditionally rather than only once ever.
        pageDao.getAllOnce().filter { it.kind == LauncherPageKind.ROWS }.forEach { rebalancePageOverflow(it.id) }
    }

    /** Finds [intent]'s current default handler, then resolves that same package's actual
     *  ACTION_MAIN/CATEGORY_LAUNCHER activity — not the (possibly different, non-launchable)
     *  activity that narrowly handles [intent] itself — so the result is always safe to hand to
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

    /** Resolves ThemePreference.SYSTEM against the live device setting — no bundled wallpaper is
     *  shipped, Light/Dark/System (see ui.theme.Theme.kt) is the only appearance control there is. */
    private suspend fun isDark(): Boolean = settingsRepository.theme.first().isDark(context)

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
     * — the carousel's own "+" tiles. Like [reorderRowsPages], this renumbers every page from
     * scratch: ROWS pages in their new relative order first (the new page
     * inserted at [direction]'s position relative to Home), then every non-ROWS singleton appended
     * after, each keeping its own prior relative order among themselves. Avoids a collision-prone
     * localized index shift — page counts are small, a full stable re-sort is cheap and simple.
     */
    // Ergänzungsseiten start as an independent copy of Home's current *structure* — same row types,
    // order, appearance and background — not of its actual content. Copying the user's pinned apps/
    // folders onto a brand-new page by default would be a confusing surprise, not a sensible
    // starting point; copying the empty scaffold (Dock/Panel/widget rows with their configured
    // look) matches "Default-Kopie" while staying consistent with this app's existing "home starts
    // empty" philosophy (see ensureSeeded's own comment on that). Once created, the new page is
    // fully independent — editing it never touches Home back.
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
            rowDao.getForPage(homePage.id).forEach { row ->
                rowDao.insert(row.copy(id = 0, pageId = newId))
            }
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
     *  the non-ROWS singletons in their prior relative order — same scheme as [addPageInDirection]. */
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
            cornerRadiusPercent = settingsRepository.defaultCornerRadiusPercent.first()
        )
        row = row.withAppearance(defaultBoxAppearanceFor(isDark()))
        val id = rowDao.insert(row)
        rebalancePageOverflow(pageId)
        return id
    }

    suspend fun addSystemWidgetRow(pageId: Long, appWidgetId: Int, spanY: Int): Long {
        var row = PageRow(
            pageId = pageId, orderIndex = nextRowOrderIndex(pageId), type = PageRowType.SYSTEM_WIDGET, payload = appWidgetId.toString(), spanY = spanY,
            cornerRadiusPercent = settingsRepository.defaultCornerRadiusPercent.first()
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

    /** Returns how many rows on [pageId] had to be relocated to a new page as a result — see [rebalancePageOverflow]. */
    suspend fun addSearchBarRow(pageId: Long): Int {
        val next = (rowDao.maxOrderIndex(pageId) ?: -1) + 1
        val row = PageRow(pageId = pageId, orderIndex = next, type = PageRowType.SEARCH_BAR, borderEnabled = true)
            .withAppearance(defaultBoxAppearanceFor(isDark()))
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
    private suspend fun heightPxOf(row: PageRow, density: androidx.compose.ui.unit.Density): Int = row.heightPx ?: with(density) {
        val heightDp = if (row.type == PageRowType.ELEMENT_BOX) {
            com.bisc.flauncher.ui.home.PageRowDimens.elementBoxHeight(entryDao.countForRow(row.id), row.columns.coerceAtLeast(1))
        } else {
            com.bisc.flauncher.ui.home.PageRowDimens.rowUnitHeight * row.spanY
        }
        heightDp.toPx().roundToInt()
    }

    private fun stackedRowsBudget(pageId: Long, hasDock: Boolean): Int {
        val dm = context.resources.displayMetrics
        val (topInsetPx, bottomInsetPx) = systemInsetsPx()
        return com.bisc.flauncher.ui.home.safeSystemWidgetMaxHeightPx(
            pageHasDock = hasDock,
            screenWidthPx = dm.widthPixels,
            screenHeightPx = dm.heightPixels,
            densityValue = dm.density,
            statusBarPx = topInsetPx,
            navigationBarPx = bottomInsetPx
        )
    }

    /**
     * Dry-run check for the "this page is full" prompt (see masterplan Phase 7: offer a new page
     * instead of silently relocating rows there): would a not-yet-inserted row shaped like
     * [candidateRow] still fit on [pageId] alongside what's already there? Mirrors
     * [rebalancePageOverflow]'s own budget math exactly, just without writing anything.
     */
    suspend fun wouldOverflow(pageId: Long, candidateRow: PageRow): Boolean {
        if (candidateRow.type == PageRowType.SEARCH_BAR) return false // floats separately, never part of this budget
        val allRows = rowDao.getForPage(pageId)
        val dockRow = allRows.lastOrNull { it.type == PageRowType.SEARCH_BAR }
        val stackedRows = allRows.filter { it.id != dockRow?.id && it.type != PageRowType.PANEL }
        val density = androidx.compose.ui.unit.Density(context.resources.displayMetrics.density)
        val budgetPx = stackedRowsBudget(pageId, dockRow != null)
        val spacingPx = with(density) { com.bisc.flauncher.ui.home.PageRowDimens.rowSpacing.toPx().roundToInt() }

        var used = 0
        for (row in stackedRows) {
            used += (if (used > 0) spacingPx else 0) + heightPxOf(row, density)
        }
        val needed = used + (if (used > 0) spacingPx else 0) + heightPxOf(candidateRow, density)
        return needed > budgetPx
    }

    private suspend fun rebalancePageOverflow(pageId: Long): List<PageRow> {
        val allRows = rowDao.getForPage(pageId)
        val dockRow = allRows.lastOrNull { it.type == PageRowType.SEARCH_BAR }
        // The Panel (like the dock) floats outside the pager rather than rendering inline — see
        // HomeScreen's top EdgeNotch Box — so it never counts toward this page's inline height budget.
        val stackedRows = allRows.filter { it.id != dockRow?.id && it.type != PageRowType.PANEL }
        if (stackedRows.isEmpty()) return emptyList()

        val dm = context.resources.displayMetrics
        val density = androidx.compose.ui.unit.Density(dm.density)
        val budgetPx = stackedRowsBudget(pageId, dockRow != null)
        val spacingPx = with(density) { com.bisc.flauncher.ui.home.PageRowDimens.rowSpacing.toPx().roundToInt() }

        suspend fun heightPxOf(row: PageRow): Int = heightPxOf(row, density)

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
        val freshPageBudgetPx = stackedRowsBudget(newPageId, hasDock = false)
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

    /** An element box grows taller as entries are added to it — this can push a page over budget too. */
    private suspend fun rebalanceForRow(pageRowId: Long) {
        rowDao.getById(pageRowId)?.let { rebalancePageOverflow(it.pageId) }
    }

    suspend fun addAppEntry(pageRowId: Long, componentName: String): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.APP, payload = componentName))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addFolderEntry(pageRowId: Long, folderId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.FOLDER, payload = folderId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addShortcutEntry(pageRowId: Long, packageName: String, shortcutId: String, label: String): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(
            RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.SHORTCUT, payload = "$packageName|$shortcutId|$label")
        )
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addMapTargetEntry(pageRowId: Long, targetId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.MAP_TARGET, payload = targetId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addWebsiteEntry(pageRowId: Long, websiteId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.WEBSITE, payload = websiteId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addPageLinkEntry(pageRowId: Long, pageId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.PAGE, payload = pageId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    /** [contactId] is a [com.bisc.flauncher.data.model.PinnedContact.contactId] — the caller (see
     *  HomeViewModel.addContactEntryToRow) ensures that pin exists first, same as this row just
     *  referencing an existing folder/website/map target by id. */
    suspend fun addContactEntry(pageRowId: Long, contactId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
        val id = entryDao.insert(RowEntry(pageRowId = pageRowId, orderIndex = next, type = GridItemType.CONTACT, payload = contactId.toString()))
        rebalanceForRow(pageRowId)
        return id
    }

    suspend fun addActionButtonEntry(pageRowId: Long, actionButtonId: Long): Long {
        val next = (entryDao.maxOrderIndex(pageRowId) ?: -1) + 1
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
