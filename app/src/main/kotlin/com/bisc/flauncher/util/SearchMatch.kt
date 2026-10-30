package com.bisc.flauncher.util

import java.text.Normalizer

private val COMBINING_MARKS = Regex("\\p{Mn}+")
private val WORD_SEPARATORS = Regex("[\\s,]+")

/** Lowercase, without accents and with ß as ss — so "uber" finds "Über" and "strasse" finds "Straße". */
fun foldForSearch(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(COMBINING_MARKS, "").replace("ß", "ss")

/** Search matching shared by every text-search category: [query] starts a word of [text]. */
fun matchesWordStart(text: String, query: String): Boolean {
    val q = foldForSearch(query.trim())
    if (q.isEmpty()) return false
    val t = foldForSearch(text)
    for (i in t.indices) {
        if ((i == 0 || !t[i - 1].isLetterOrDigit()) && t.startsWith(q, i)) return true
    }
    return false
}

/** Like [matchesWordStart] for queries of several words, in any order: every word of [query] has to
 *  start a word somewhere in [text] — so "klee 23" finds "Kleeberger Straße 23, 35510 Butzbach". */
fun matchesAllWordStarts(text: String, query: String): Boolean {
    val words = query.split(WORD_SEPARATORS).filter { it.isNotEmpty() }
    return words.isNotEmpty() && words.all { matchesWordStart(text, it) }
}

/** Whether [query] (one or several words) matches [label] or any of [keywords]: every word has to
 *  start a word of the label, or of one single keyword. */
fun matchesLabelOrKeywords(label: String, keywords: List<String>, query: String): Boolean =
    matchesAllWordStarts(label, query) || keywords.any { matchesAllWordStarts(it, query) }
