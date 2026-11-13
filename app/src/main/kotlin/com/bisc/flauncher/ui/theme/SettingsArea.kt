package com.bisc.flauncher.ui.theme

import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

/**
 * The settings' five areas, each with a color of its own for orientation: it tints the circles
 * behind symbols, the group titles, and everything that is picked or switched on in that area.
 * Symbols themselves stay black or white for contrast. The colors are chosen so text in them reads
 * well on the settings' light and dark background (contrast of at least 4.5 to 1), and an area is
 * never told by its color alone: it always has its name and symbol too.
 */
enum class SettingsArea(private val light: Long, private val dark: Long, val icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    // Spread wide over the color wheel, so no two are mistaken for each other; each with its symbol
    // too, for those who tell colors apart badly.
    APPEARANCE(0xFFC2185B, 0xFFF48FB1, androidx.compose.material.icons.Icons.Filled.Palette),
    PAGES(0xFF1565C0, 0xFF90CAF9, androidx.compose.material.icons.Icons.Filled.ViewCarousel),
    ELEMENTS(0xFF2E7D32, 0xFFA5D6A7, androidx.compose.material.icons.Icons.Filled.Apps),
    SYSTEM(0xFF546E7A, 0xFFB0BEC5, androidx.compose.material.icons.Icons.Filled.PhoneAndroid),
    EXTENSIONS(0xFFB84A00, 0xFFFFB74D, androidx.compose.material.icons.Icons.Filled.Extension),
    /** The start page and the search: no area of their own. */
    NEUTRAL(0xFF3C4A57, 0xFFC4CED8, null);

    fun color(dark: Boolean): Color = Color(if (dark) this.dark else light)
}

val LocalSettingsArea = staticCompositionLocalOf { SettingsArea.NEUTRAL }

/** The area a settings route belongs to. */
fun settingsAreaFor(route: String?): SettingsArea {
    val r = route?.removePrefix("settings")?.removePrefix("/") ?: return SettingsArea.NEUTRAL
    return when {
        r.isEmpty() || r == "find" -> SettingsArea.NEUTRAL
        r.startsWith("appearance/designs") || r.startsWith("wallpaper_packs") || r.startsWith("extensions") -> SettingsArea.EXTENSIONS
        r.startsWith("appearance") -> SettingsArea.APPEARANCE
        r.startsWith("pages") || r.startsWith("search") || r.startsWith("app_overview") || r.startsWith("app_grid") || r.startsWith("gestures") -> SettingsArea.PAGES
        r.startsWith("elements") || r.startsWith("apps") || r.startsWith("folder") -> SettingsArea.ELEMENTS
        r.startsWith("system") || r.startsWith("permissions") || r.startsWith("about") -> SettingsArea.SYSTEM
        else -> SettingsArea.NEUTRAL
    }
}

/** The area color for the current light or dark settings. */
@Composable
fun SettingsArea.current(): Color = color(MaterialTheme.colorScheme.background.luminance() < 0.5f)

/** Gives [content] its area's color as the settings' own color: picked things, switches, titles. */
@Composable
fun SettingsAreaTheme(area: SettingsArea, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val c = area.color(dark)
    val on = if (dark) Color(0xFF111316) else Color.White
    val container = c.copy(alpha = if (dark) 0.28f else 0.16f).compositeOver(scheme.surface)
    MaterialTheme(
        colorScheme = scheme.copy(
            primary = c, onPrimary = on,
            secondary = c, onSecondary = on,
            tertiary = c, onTertiary = on,
            primaryContainer = container, onPrimaryContainer = scheme.onSurface,
            secondaryContainer = container, onSecondaryContainer = scheme.onSurface,
            // Hints and second lines a little stronger than the system's grey, so small text reads
            // well for everyone, also in bright light.
            onSurfaceVariant = androidx.compose.ui.graphics.lerp(scheme.onSurfaceVariant, scheme.onSurface, 0.35f),
            surfaceTint = c
        ),
        typography = MaterialTheme.typography
    ) {
        CompositionLocalProvider(LocalSettingsArea provides area, content = content)
    }
}
