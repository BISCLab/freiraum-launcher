package com.bisc.flauncher.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent

fun launchApp(context: Context, componentName: ComponentName) {
    // ACTION_MAIN + CATEGORY_LAUNCHER, not just a bare component, matches what
    // PackageManager.getLaunchIntentForPackage() (what "Open" in App info uses) sends. Some apps'
    // launch activities check for this and silently do nothing for a component-only Intent.
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = componentName
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
