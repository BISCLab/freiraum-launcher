package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** How a [AppOverride.hidden] app can still be reached from inside the launcher. */
enum class RevealMode {
    NONE,
    SEARCH_CODE
}

/** Per-app overrides layered on top of the real, installed app. */
@Entity(tableName = "app_overrides")
data class AppOverride(
    @PrimaryKey val componentName: String,
    val customLabel: String? = null,
    val hidden: Boolean = false,
    val revealMode: RevealMode = RevealMode.NONE,
    val revealCode: String? = null,
    val iconPackPackage: String? = null,
    val iconPackDrawable: String? = null
)
