package com.bisc.flauncher.data.model

/** A group of text-search results; [storageKey] names its "searched by default" preference. */
enum class SearchCategory(val storageKey: String, val legacyKey: String? = null) {
    APPS("apps"),
    FOLDERS("folders", legacyKey = "apps"),
    CONTACTS("contacts"),
    SETTINGS("settings"),
    NOTES("notes", legacyKey = "documents"),
    WEBSITES("websites", legacyKey = "documents"),
    PLACES("maps"),
    SHORTCUTS("shortcuts"),
    EMAIL("email", legacyKey = "web"),
    WEB("web"),
    AI("ai")
}
