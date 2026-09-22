package com.bisc.flauncher.data.model

import com.bisc.flauncher.ui.theme.ThemePreference

enum class DesignPreset { LIGHT, DARK }

/** Fixed value sets written to all surfaces when a design preset is applied. */
data class DesignPresetValues(
    val theme: ThemePreference,
    val accentColor: Int,
    val wallpaper: BundledWallpaper,
    val cardAppearance: ElementAppearance,
    val rowAppearance: ElementAppearance,
    val searchBarAppearance: ElementAppearance
) {
    companion object {
        private const val BRAND_GREEN = 0xFF0A7220.toInt()

        const val LIGHT_SURFACE = 0xFFFFFFFF.toInt()
        const val DARK_SURFACE = 0xFF1C1C1E.toInt()

        fun of(preset: DesignPreset): DesignPresetValues = when (preset) {
            DesignPreset.LIGHT -> DesignPresetValues(
                theme = ThemePreference.LIGHT,
                accentColor = BRAND_GREEN,
                wallpaper = BundledWallpaper.LIGHT,
                cardAppearance = ElementAppearance(fill = ColorFill(color = LIGHT_SURFACE), borderEnabled = true),
                rowAppearance = ElementAppearance(fill = ColorFill(color = LIGHT_SURFACE), borderEnabled = false),
                searchBarAppearance = ElementAppearance(fill = ColorFill(color = LIGHT_SURFACE), borderEnabled = true)
            )
            DesignPreset.DARK -> DesignPresetValues(
                theme = ThemePreference.DARK,
                accentColor = BRAND_GREEN,
                wallpaper = BundledWallpaper.DARK,
                cardAppearance = ElementAppearance(fill = ColorFill(color = DARK_SURFACE), borderEnabled = true),
                rowAppearance = ElementAppearance(fill = ColorFill(color = DARK_SURFACE), borderEnabled = false),
                searchBarAppearance = ElementAppearance(fill = ColorFill(color = DARK_SURFACE), borderEnabled = true)
            )
        }
    }
}
