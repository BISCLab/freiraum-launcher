package com.bisc.flauncher.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** The browser and maps app picked in the settings; null means the system default. */
object ExternalApps {
    @Volatile var browser: String? = null
    @Volatile var maps: String? = null
}

/** Opens [uri] in [preferredPackage] if that app takes it, otherwise in the system default; false if nothing could open it. */
fun startView(context: Context, uri: Uri, preferredPackage: String?): Boolean {
    val view = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (preferredPackage != null) {
        // Anything the chosen app refuses (not installed, not allowed, an address it must not get)
        // falls through to the device default below.
        if (runCatching { context.startActivity(Intent(view).setPackage(preferredPackage)) }.isSuccess) return true
    }
    return runCatching { context.startActivity(view) }.isSuccess
}
