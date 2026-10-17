package com.bisc.flauncher.util

import android.content.Context
import android.content.Intent

/** Opens a system settings screen by its public Intent action string (e.g. [SettingsShortcut.action]
 *  or an [com.bisc.flauncher.data.model.ActionButton.androidSettingAction]) — no special permission
 *  needed, every action here is a plain public AOSP intent. */
fun launchSettingsShortcut(context: Context, action: String) {
    runCatching {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
