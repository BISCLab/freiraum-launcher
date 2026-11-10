package com.bisc.flauncher.util

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.PinnedWebsite

// Addresses that point into this phone or run code instead of opening a page or an app.
private val BLOCKED_SCHEMES = setOf("file", "content", "javascript", "intent", "data", "android-app")

/** The address a link opens: [raw] with https in front if it names no scheme. Null if it is not an
 *  address at all (empty, with spaces) or one a link must not open. */
fun linkAddress(raw: String): String? {
    val text = raw.trim()
    if (text.isEmpty() || text.any { it.isWhitespace() }) return null
    // "mailto:..." or "spotify:..." name a scheme; "example.com:8080" is a host with a port.
    val hasScheme = text.contains("://") || Regex("^[a-zA-Z][a-zA-Z0-9+-]*:(?![0-9]{1,5}(/|$))").containsMatchIn(text)
    val url = if (hasScheme) text else "https://$text"
    val scheme = runCatching { Uri.parse(url).scheme }.getOrNull()?.lowercase() ?: return null
    return url.takeIf { scheme !in BLOCKED_SCHEMES }
}

/** Opens a pinned website in the chosen browser, or the device default; says so if nothing opens it. */
fun openWebsite(context: Context, website: PinnedWebsite) {
    val opened = linkAddress(website.url)?.let { startView(context, Uri.parse(it), website.browserApp ?: ExternalApps.browser) } ?: false
    if (!opened) Toast.makeText(context, R.string.link_open_failed, Toast.LENGTH_SHORT).show()
}
