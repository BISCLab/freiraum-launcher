package com.bisc.flauncher.data.model

/** A group of text-search results; [storageKey] names its "searched by default" preference. */
enum class SearchCategory(val storageKey: String, val legacyKey: String? = null) {
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
