package com.bisc.flauncher.util

import androidx.core.net.toUri
import android.content.Context
import android.content.Intent
import com.bisc.flauncher.data.model.WebEngine

/** Opens a web search for [query] in the chosen browser, or the device default. */
fun launchWebSearch(context: Context, query: String, engine: WebEngine): Boolean =
    startView(context, engine.urlFor(query.trim()).toUri(), ExternalApps.browser)

/** Opens the default mail app composing to [address]. */
fun launchEmailTo(context: Context, address: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO, "mailto:${address.trim()}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
