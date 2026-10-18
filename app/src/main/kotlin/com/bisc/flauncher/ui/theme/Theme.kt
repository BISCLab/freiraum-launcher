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

/** The fill color every box/card/widget/dock/panel defaults to: the opposite tone from the page
 *  background, so it reads as a distinct layer floating over the page. */
fun defaultBoxFillColorFor(isDark: Boolean): Int = if (isDark) android.graphics.Color.WHITE else MATTE_DARK_COLOR

/** The shared default look for every box/card/widget/dock/panel when it has no customization of
 *  its own — [defaultBoxFillColorFor] at 80% opacity with a border, a frosted layer that reads as
 *  a distinct surface floating over the page. The same treatment the dock always had bespoke, now
 *  the one shared default everywhere. */
fun defaultBoxAppearanceFor(isDark: Boolean): ElementAppearance = ElementAppearance(
    fill = ColorFill(color = defaultBoxFillColorFor(isDark)),
    opacity = 0.8f,
    borderEnabled = true
)

@Composable
fun FLauncherTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    fontPreference: FontPreference = FontPreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> systemDark
    }

    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Dynamic color requires Android 12; older versions use the static scheme.
        if (isDark) darkColorScheme() else lightColorScheme()
    }

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
