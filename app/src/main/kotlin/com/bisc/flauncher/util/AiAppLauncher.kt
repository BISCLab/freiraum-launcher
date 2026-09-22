package com.bisc.flauncher.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/** Hands [query] to the chosen AI app like a share; opens the app plainly if it does not take shared text. */
fun launchAiApp(context: Context, packageName: String, query: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, query)
        .setPackage(packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(send)
    } catch (_: ActivityNotFoundException) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}
