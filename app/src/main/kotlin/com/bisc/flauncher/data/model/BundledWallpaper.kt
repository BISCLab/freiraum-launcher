package com.bisc.flauncher.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.bisc.flauncher.R

/** The two wallpapers that ship inside the app; stored in a background as `res:<name>`. */
enum class BundledWallpaper(@DrawableRes val drawable: Int, @StringRes val label: Int) {
    LIGHT(R.drawable.wallpaper_light, R.string.wallpaper_light),
    DARK(R.drawable.wallpaper_dark, R.string.wallpaper_dark);

    val path: String get() = PREFIX + name

    companion object {
        const val PREFIX = "res:"

        fun fromPath(path: String?): BundledWallpaper? =
            path?.takeIf { it.startsWith(PREFIX) }?.let { p -> entries.firstOrNull { it.name == p.removePrefix(PREFIX) } }
    }
}
