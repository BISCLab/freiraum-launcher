package com.bisc.flauncher.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.bisc.flauncher.data.model.ColorFill
import com.bisc.flauncher.data.model.ElementAppearance

enum class ThemePreference { LIGHT, DARK, SYSTEM }

/** Resolves SYSTEM against the live device setting - the single place this check happens, reused
 *  by every background/box color resolution so they all agree with each other and with
 *  [FLauncherTheme]'s own [isSystemInDarkTheme] check. */
fun ThemePreference.isDark(context: Context): Boolean = when (this) {
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
    ThemePreference.SYSTEM -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}

/** A matte near-black, not pure black - FLauncher's only default background in dark mode. No
 *  bundled wallpaper is shipped; this plus plain white in light mode are the only two defaults. */
const val MATTE_DARK_COLOR = 0xFF121212.toInt()

/** The page background for the given effective mode. */
fun backgroundColorFor(isDark: Boolean): Int = if (isDark) MATTE_DARK_COLOR else android.graphics.Color.WHITE

/** FLauncher's standard blue - the default accent, and the settings UI's own primary color. */
const val DEFAULT_ACCENT_COLOR = 0xFF0758F5.toInt()

/** Every element's default corner roundness: fully rounded. */
const val DEFAULT_CORNER_RADIUS_PERCENT = 100

/** Widgets' own default: only slightly rounded - fully round corners cut into a widget's content. */
const val DEFAULT_WIDGET_CORNER_RADIUS_PERCENT = 10

/** The one transparency every default element fill uses. */
const val DEFAULT_ELEMENT_OPACITY = 0.8f

/** The fill color every box/card/widget/dock/panel defaults to: black in dark mode, white in light
 *  mode - the same tone as the page, set apart from it by [DEFAULT_ELEMENT_OPACITY] and a border. */
fun defaultBoxFillColorFor(isDark: Boolean): Int = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE

/** The shared default look for every box/card/widget/dock/panel when it has no customization of
 *  its own - [defaultBoxFillColorFor] at [DEFAULT_ELEMENT_OPACITY] with a border. The one shared
 *  default everywhere, for dark and light mode alike. */
fun defaultBoxAppearanceFor(isDark: Boolean): ElementAppearance = ElementAppearance(
    fill = ColorFill(color = defaultBoxFillColorFor(isDark)),
    opacity = DEFAULT_ELEMENT_OPACITY,
    borderEnabled = true
)

@Composable
fun FLauncherTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    fontPreference: FontPreference = FontPreference.SYSTEM,
    accentColor: Int = DEFAULT_ACCENT_COLOR,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> systemDark
    }

    MaterialTheme(
        colorScheme = freiraumColorScheme(context, isDark, accentColor),
        typography = themedTypography(fontPreference.fontFamily),
        content = content
    )
}

/** Neutral surfaces from the system (dynamic on Android 12+), every accent the launcher's own. */
fun freiraumColorScheme(context: Context, isDark: Boolean, accentColor: Int): ColorScheme {
    val baseScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (isDark) darkColorScheme() else lightColorScheme()
    }
    val accent = Color(accentColor)
    return baseScheme.copy(
        primary = accent,
        onPrimary = Color.White,
        secondary = accent,
        onSecondary = Color.White,
        tertiary = accent,
        surfaceTint = accent
    )
}

/** Color mode of the settings and the welcome tour only; LAUNCHER follows the launcher's own. */
enum class SettingsThemeMode { LAUNCHER, LIGHT, DARK, SYSTEM }

val LocalSettingsThemeMode = staticCompositionLocalOf { SettingsThemeMode.LAUNCHER }

/** Whether settings show dark, given the launcher's own [launcherDark]. */
@Composable
fun settingsDark(mode: SettingsThemeMode, launcherDark: Boolean): Boolean = when (mode) {
    SettingsThemeMode.LAUNCHER -> launcherDark
    SettingsThemeMode.LIGHT -> false
    SettingsThemeMode.DARK -> true
    SettingsThemeMode.SYSTEM -> isSystemInDarkTheme()
}

/** Re-themes [content] in the settings color mode, keeping the launcher's accent and font. */
@Composable
fun SettingsThemed(content: @Composable () -> Unit) {
    val mode = LocalSettingsThemeMode.current
    if (mode == SettingsThemeMode.LAUNCHER) {
        content()
        return
    }
    val context = LocalContext.current
    val isDark = settingsDark(mode, launcherDark = false)
    val accent = MaterialTheme.colorScheme.primary.toArgb()
    MaterialTheme(colorScheme = freiraumColorScheme(context, isDark, accent), typography = MaterialTheme.typography) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}

/** A plain tap target with no press indication. */
@Composable
fun Modifier.noRippleClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
}

private fun themedTypography(fontFamily: FontFamily?): Typography {
    val base = Typography()
    if (fontFamily == null) return base
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily)
    )
}
