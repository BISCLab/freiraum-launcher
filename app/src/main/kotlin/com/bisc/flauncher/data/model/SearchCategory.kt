package com.bisc.flauncher.data.model

/** A group of text-search results; [storageKey] names its "searched by default" preference. */
enum class SearchCategory(val storageKey: String, val legacyKey: String? = null) {
    // Picked by hand and/or the most opened apps; shown while nothing is typed.
    FAVORITES("favorites"),
    APPS("apps"),
    FOLDERS("folders", legacyKey = "apps"),
    CONTACTS("contacts"),
    SETTINGS("settings"),
    // The launcher's own settings screens (see LauncherSettingsTargets).
    LAUNCHER_SETTINGS("launcher_settings"),
    WEBSITES("websites", legacyKey = "documents"),
    PLACES("maps"),
    SHORTCUTS("shortcuts"),
    WEB("web"),
    AI("ai")
}

/** How a category takes part in the Text search: searched with every query, and shown in the rail.
 *  Only the rail = on demand (results after a tap on it); neither = off. */
data class CategoryMode(val search: Boolean, val rail: Boolean) {
    val off: Boolean get() = !search && !rail
}
