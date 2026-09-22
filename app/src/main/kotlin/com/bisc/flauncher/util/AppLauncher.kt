package com.bisc.flauncher.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent

fun launchApp(context: Context, componentName: ComponentName) {
    runCatching {
        context.startActivity(
            Intent().apply {
                component = componentName
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
