package com.bisc.flauncher.ui

import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext

/**
 * A line of text too long for its place runs through now and then, so it can be read in full: a
 * short wait, once through, a pause, again. Text that fits stays still, and so does all of it while
 * the system asks for no animations.
 */
fun Modifier.marqueeWhenCut(): Modifier = composed {
    val context = LocalContext.current
    val still = remember {
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    if (still) Modifier else Modifier.basicMarquee(
        iterations = Int.MAX_VALUE,
        initialDelayMillis = 2000,
        repeatDelayMillis = 3500,
        velocity = androidx.compose.ui.unit.Dp(36f)
    )
}
