package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** What a [LauncherPage] renders as its own content. [ROWS] is a normal swipeable pager page;
 *  [APP_LIST]/[SEARCH] are singleton pages (auto-seeded, not deletable) that live in the pages
 *  list for identity/icon/settings purposes but render as their own overlay instead of pager
 *  content — see [com.bisc.flauncher.ui.home.HomeViewModel.openAppList]/`openSearch`. */
enum class LauncherPageKind { ROWS, APP_LIST, SEARCH }

/** A home-screen page. [iconGlyph]/[iconBackgroundColor]/[iconColor] control how this page looks
 *  when shown as a dock slot (a dock slot can link to any page) — null means "default for this
 *  page's kind", see [com.bisc.flauncher.ui.home.dock.PageIconGlyph]. */
@Entity(tableName = "launcher_pages")
data class LauncherPage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderIndex: Int,
    val isHome: Boolean = false,
    val isAppOverview: Boolean = false,
    val kind: LauncherPageKind = LauncherPageKind.ROWS,
    val name: String? = null,
    val iconGlyph: String? = null,
    val iconBackgroundColor: Int? = null,
    val iconColor: Int? = null
)
