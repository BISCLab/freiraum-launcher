package com.bisc.flauncher.util

/** Search matching shared by every text-search category. */
fun matchesWordStart(text: String, query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return false
    for (i in text.indices) {
        if ((i == 0 || !text[i - 1].isLetterOrDigit()) && text.startsWith(q, i, ignoreCase = true)) return true
    }
    return false
}
