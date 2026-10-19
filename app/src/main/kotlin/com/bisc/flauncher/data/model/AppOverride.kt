package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bisc.flauncher.data.settings.IconShape

/** Whether a Text-search-visible app ([AppOverride.hiddenFromSearch] false) matches normally by
 *  name, or [SEARCH_CODE] instead — typing the exact [AppOverride.revealCode] is then the *only*
 *  way to find it there, replacing name matching rather than adding to it. Meaningless once
 *  [AppOverride.hiddenFromSearch] is true: an app not in Text search at all has nothing a code
 *  could bypass. See HomeViewModel.searchResults. */
enum class RevealMode {
    NONE,
    SEARCH_CODE
}

/** Per-app overrides layered on top of the real, installed app. [iconShape] and [iconSizePercent]
 *  null means "inherit the global default" — see SettingsRepository.iconShape/iconSizePercent.
 *  [hiddenFromAppList] and [hiddenFromSearch] are independent — an app can be hidden from one
 *  surface while staying visible on the other (placements on pages, in the dock and in folders
 *  are unaffected by either). */
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
    // 0..200, the icon's actual size as a percent of its normal size (100 = neutral); null means
    // "inherit the global default" (see SettingsRepository.iconSizePercent).
    val iconSizePercent: Int? = null
)
