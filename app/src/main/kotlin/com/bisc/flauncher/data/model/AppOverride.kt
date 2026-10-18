package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bisc.flauncher.data.settings.IconShape

/** How a search-hidden app ([AppOverride.hiddenFromSearch]) can still be reached from inside the
 *  launcher. */
enum class RevealMode {
    NONE,
    SEARCH_CODE
}

/** Per-app overrides layered on top of the real, installed app. [iconShape] and [iconSizePercent]
 *  null means "inherit the global default" — see SettingsRepository.iconShape/iconSizePercent.
 *  [hiddenFromAppList] and [hiddenFromSearch] are independent — an app can be hidden from one
 *  surface while staying visible on the other (placements on pages, in the dock and in folders
 *  are unaffected by either). [revealMode]/[revealCode] only matter while [hiddenFromSearch] is
 *  true — they're the search-only bypass that lets a search-hidden app reappear by typing a code,
 *  see HomeViewModel.searchResults. */
@Entity(tableName = "app_overrides")
data class AppOverride(
    @PrimaryKey val componentName: String,
    val customLabel: String? = null,
    val hiddenFromAppList: Boolean = false,
    val hiddenFromSearch: Boolean = false,
    val revealMode: RevealMode = RevealMode.NONE,
    val revealCode: String? = null,
    val iconPackPackage: String? = null,
    val iconPackDrawable: String? = null,
    val iconShape: IconShape? = null,
    val iconSizePercent: Int? = null
)
