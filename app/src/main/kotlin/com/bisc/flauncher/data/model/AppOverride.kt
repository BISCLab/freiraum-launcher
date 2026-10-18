package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bisc.flauncher.data.settings.IconShape

/** How a [AppOverride.hidden] app can still be reached from inside the launcher. */
enum class RevealMode {
    NONE,
    SEARCH_CODE
}

/** Per-app overrides layered on top of the real, installed app. [iconShape] and [iconSizePercent]
 *  null means "inherit the global default" — see SettingsRepository.iconShape/iconSizePercent. */
@Entity(tableName = "app_overrides")
data class AppOverride(
    @PrimaryKey val componentName: String,
    val customLabel: String? = null,
    val hidden: Boolean = false,
    val revealMode: RevealMode = RevealMode.NONE,
    val revealCode: String? = null,
    val iconPackPackage: String? = null,
    val iconPackDrawable: String? = null,
    val iconShape: IconShape? = null,
    val iconSizePercent: Int? = null
)
