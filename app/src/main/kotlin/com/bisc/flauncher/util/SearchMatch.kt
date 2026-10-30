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

/** Like [matchesWordStart] for queries of several words, in any order: every word of [query] has to
 *  start a word somewhere in [text] — so "klee 23" finds "Kleeberger Straße 23, 35510 Butzbach". */
fun matchesAllWordStarts(text: String, query: String): Boolean {
    val words = query.split(Regex("[\\s,]+")).filter { it.isNotEmpty() }
    return words.isNotEmpty() && words.all { matchesWordStart(text, it) }
}
