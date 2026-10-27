package com.bisc.flauncher.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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

/** Resolves SYSTEM against the live device setting — the single place this check happens, reused
 *  by every background/box color resolution so they all agree with each other and with
 *  [FLauncherTheme]'s own [isSystemInDarkTheme] check. */
fun ThemePreference.isDark(context: Context): Boolean = when (this) {
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
    ThemePreference.SYSTEM -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}

/** A matte near-black, not pure black — FLauncher's only default background in dark mode. No
 *  bundled wallpaper is shipped; this plus plain white in light mode are the only two defaults. */
const val MATTE_DARK_COLOR = 0xFF121212.toInt()

/** The page background for the given effective mode. */
fun backgroundColorFor(isDark: Boolean): Int = if (isDark) MATTE_DARK_COLOR else android.graphics.Color.WHITE

/** FLauncher's standard blue — the default accent, and the settings UI's own primary color. */
const val DEFAULT_ACCENT_COLOR = 0xFF0758F5.toInt()

/** Every element's default corner roundness: fully rounded. */
const val DEFAULT_CORNER_RADIUS_PERCENT = 100

/** Widgets' own default: only slightly rounded — fully round corners cut into a widget's content. */
const val DEFAULT_WIDGET_CORNER_RADIUS_PERCENT = 10

/** The one transparency every default element fill uses. */
const val DEFAULT_ELEMENT_OPACITY = 0.8f

/** The fill color every box/card/widget/dock/panel defaults to: black in dark mode, white in light
 *  mode — the same tone as the page, set apart from it by [DEFAULT_ELEMENT_OPACITY] and a border. */
fun defaultBoxFillColorFor(isDark: Boolean): Int = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE

/** The shared default look for every box/card/widget/dock/panel when it has no customization of
 *  its own — [defaultBoxFillColorFor] at [DEFAULT_ELEMENT_OPACITY] with a border. The one shared
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

    val baseScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Dynamic color requires Android 12; older versions use the static scheme.
        if (isDark) darkColorScheme() else lightColorScheme()
    }
    // Neutral surfaces stay from the base scheme; every accent (switches, radio buttons, sliders,
    // group titles) is the launcher's own accent color instead of a wallpaper-derived one.
    val accent = Color(accentColor)
    val colorScheme = baseScheme.copy(
        primary = accent,
        onPrimary = Color.White,
        secondary = accent,
        onSecondary = Color.White,
        tertiary = accent,
        surfaceTint = accent
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = themedTypography(fontPreference.fontFamily),
        content = content
    )
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
