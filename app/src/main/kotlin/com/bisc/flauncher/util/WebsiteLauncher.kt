package com.bisc.flauncher.util

import android.content.Context
import android.net.Uri
import com.bisc.flauncher.data.model.PinnedWebsite

/** Opens a pinned website in the chosen browser, or the device default. */
fun openWebsite(context: Context, website: PinnedWebsite) {
    val url = website.url.let { if (it.contains("://")) it else "https://$it" }
    startView(context, Uri.parse(url), ExternalApps.browser)
}
