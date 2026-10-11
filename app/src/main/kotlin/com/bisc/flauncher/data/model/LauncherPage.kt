package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** What a [LauncherPage] renders as its own content. */
enum class LauncherPageKind { ROWS, APP_LIST }

/** A home-screen page. */
@Entity(tableName = "launcher_pages")
data class LauncherPage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderIndex: Int,
    val isHome: Boolean = false,
    val isAppOverview: Boolean = false,
    val kind: LauncherPageKind = LauncherPageKind.ROWS,
    val name: String? = null
)
