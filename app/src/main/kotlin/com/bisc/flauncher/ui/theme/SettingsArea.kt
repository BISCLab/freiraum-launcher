package com.bisc.flauncher.ui.theme

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
enum class SettingsArea(private val light: Long, private val dark: Long) {
    APPEARANCE(0xFF7B3FE4, 0xFFC3A6FF),
    PAGES(0xFF1A66D9, 0xFF8AB4F8),
    ELEMENTS(0xFF1B7F3B, 0xFF81C995),
    SYSTEM(0xFF546474, 0xFFAEBBC9),
    EXTENSIONS(0xFFB45309, 0xFFF4A261),
    /** The start page and the search: no area of their own. */
    NEUTRAL(0xFF3C4A57, 0xFFC4CED8);

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
