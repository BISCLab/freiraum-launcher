package com.bisc.flauncher.util

import android.content.Context
import android.content.Intent
import com.bisc.flauncher.data.model.SettingsShortcut

fun launchSettingsShortcut(context: Context, shortcut: SettingsShortcut) {
    runCatching {
        context.startActivity(Intent(shortcut.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
