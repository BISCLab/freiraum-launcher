package com.bisc.flauncher.data.model

import android.net.Uri

/** Where the web-search command result sends the query. */
enum class SearchEngine(private val template: String) {
    DUCKDUCKGO("https://duckduckgo.com/?q=%s"),
    STARTPAGE("https://www.startpage.com/sp/search?query=%s"),
    GOOGLE("https://www.google.com/search?q=%s"),
    BRAVE("https://search.brave.com/search?q=%s"),
    CUSTOM("");

    fun urlFor(query: String, customTemplate: String? = null): String {
        val encoded = Uri.encode(query)
        val raw = if (this == CUSTOM) customTemplate?.trim()?.takeIf { it.isNotEmpty() } ?: DUCKDUCKGO.template else template
        val url = if (raw.contains("://")) raw else "https://$raw"
        return when {
            url.contains("%s") -> url.replace("%s", encoded)
            url.endsWith("=") -> url + encoded
            else -> bareDomainSearch(url, encoded) ?: (url.trimEnd('/') + "/?q=" + encoded)
        }
    }

    /** A bare start-page address of a well-known engine, turned into that engine's real search URL. */
    private fun bareDomainSearch(url: String, encoded: String): String? {
        val uri = Uri.parse(url)
        if (!uri.encodedPath.isNullOrEmpty() && uri.encodedPath != "/" || uri.encodedQuery != null) return null
        val host = uri.host?.lowercase() ?: return null
        val path = when {
            "google." in host || "bing." in host || "ecosia." in host || "brave." in host -> "/search?q="
            "startpage." in host -> "/sp/search?query="
            "yahoo." in host -> "/search?p="
            else -> return null
        }
        return "${uri.scheme}://$host$path$encoded"
    }
}
