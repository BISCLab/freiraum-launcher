package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One of the dock's icon positions (0..N-1, in the bar's own left-to-right order) — a flexible-length
 * list, capped to [com.bisc.flauncher.ui.home.dock.DockDimens.REFERENCE_SLOT_COUNT] slots. Each slot
 * is either a pinned app ([type] "APP", [componentName] set) or a linked page ([type] "PAGE",
 * [linkedPageId] set to a [LauncherPage.id]) — tapping it opens that page (jumps the pager to it for a
 * [LauncherPageKind.ROWS] page, opens the matching overlay for [LauncherPageKind.APP_LIST]/[LauncherPageKind.SEARCH]).
 * Pages are autonomous: they exist and work whether or not any dock slot links to them.
 * [backgroundColor]/[iconColor] are ARGB overrides; null means "use that page's/app's own default colors".
 */
@Entity(tableName = "dock_slots")
data class DockSlot(
    @PrimaryKey val slotIndex: Int,
    val type: String = "APP",
    val componentName: String? = null,
    val linkedPageId: Long? = null,
    val backgroundColor: Int? = null,
    val iconColor: Int? = null
)
