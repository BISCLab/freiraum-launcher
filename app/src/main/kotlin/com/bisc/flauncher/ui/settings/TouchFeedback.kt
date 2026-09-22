package com.bisc.flauncher.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Whether the system-wide touch feedback (haptic feedback) setting is enabled. */
@Suppress("DEPRECATION")
fun isTouchFeedbackOn(context: Context): Boolean =
    runCatching { Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0 }
        .getOrDefault(true)

/** Opens the system sound settings. */
fun openSoundSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
