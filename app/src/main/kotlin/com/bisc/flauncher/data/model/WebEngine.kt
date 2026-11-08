package com.bisc.flauncher.data.model

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** A search engine the web result can send the query to; [template] holds %s where the query goes. */
data class WebEngine(val id: String, val name: String, val template: String) {
    fun urlFor(query: String): String = webSearchUrl(template, query)

    val isPreset: Boolean get() = presets.any { it.id == id }

    /** The address without scheme and path, to show under the name. */
    val host: String get() = Uri.parse(if (template.contains("://")) template else "https://$template").host?.removePrefix("www.") ?: template

    companion object {
        val presets: List<WebEngine> = listOf(
            WebEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
            WebEngine("startpage", "Startpage", "https://www.startpage.com/sp/search?query=%s"),
            WebEngine("google", "Google", "https://www.google.com/search?q=%s"),
            WebEngine("brave", "Brave", "https://search.brave.com/search?q=%s"),
            WebEngine("ecosia", "Ecosia", "https://www.ecosia.org/search?q=%s"),
            WebEngine("bing", "Bing", "https://www.bing.com/search?q=%s"),
            WebEngine("qwant", "Qwant", "https://www.qwant.com/?q=%s"),
            WebEngine("wikipedia", "Wikipedia", "https://${Locale.getDefault().language.ifBlank { "en" }}.wikipedia.org/w/index.php?search=%s")
        )

        /** Brand-like colors for the letter symbol of the presets. */
        fun colorOf(engine: WebEngine): Long = when (engine.id) {
            "duckduckgo" -> 0xFFDE5833
            "startpage" -> 0xFF6573FF
            "google" -> 0xFF4285F4
            "brave" -> 0xFFFB542B
            "ecosia" -> 0xFF36A35B
            "bing" -> 0xFF008373
            "qwant" -> 0xFF5C97FF
            "wikipedia" -> 0xFF636466
            else -> listOf(0xFF7E57C2, 0xFF26A69A, 0xFFEF6C00, 0xFF5C6BC0, 0xFFAD1457, 0xFF00897B)[(engine.name.hashCode() and 0x7fffffff) % 6]
        }

        fun toJson(engines: List<WebEngine>): String = JSONArray().apply {
            engines.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("url", it.template)) }
        }.toString()

        fun fromJson(json: String): List<WebEngine> = runCatching {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                WebEngine(id, o.optString("name").ifBlank { id }, o.optString("url"))
            }.distinctBy { it.id }
        }.getOrDefault(emptyList())

        /** The single engine an older version kept, as the list this one keeps. */
        fun fromLegacy(engine: String?, customUrl: String?): List<WebEngine> {
            if (engine == "CUSTOM" && !customUrl.isNullOrBlank()) {
                val custom = WebEngine("custom", "", customUrl.trim())
                return listOf(custom.copy(name = custom.host))
            }
            val id = when (engine) {
                "STARTPAGE" -> "startpage"
                "GOOGLE" -> "google"
                "BRAVE" -> "brave"
                else -> "duckduckgo"
            }
            return listOf(presets.first { it.id == id })
        }
    }
}

/** The search address for [query]: %s replaced, or the query appended the way the address suggests. */
fun webSearchUrl(template: String, query: String): String {
    val encoded = Uri.encode(query)
    val raw = template.trim().ifEmpty { WebEngine.presets.first().template }
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
