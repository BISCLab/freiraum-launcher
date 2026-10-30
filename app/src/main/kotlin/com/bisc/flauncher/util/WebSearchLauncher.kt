package com.bisc.flauncher.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.bisc.flauncher.data.model.SearchEngine

/** Opens a web search for [query] in the chosen browser, or the device default. */
fun launchWebSearch(context: Context, query: String, engine: SearchEngine, customTemplate: String?): Boolean =
    startView(context, Uri.parse(engine.urlFor(query.trim(), customTemplate)), ExternalApps.browser)

/** Opens the default mail app composing to [address]. */
fun launchEmailTo(context: Context, address: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${address.trim()}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
