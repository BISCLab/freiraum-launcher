package com.bisc.flauncher.data.model

import com.bisc.flauncher.R

/** A screen of the launcher's own settings the text search can open. [route] is a settings
 *  navigation route; [HOME] in it stands for the home page's id. [keywords] (German and English)
 *  find it by what's on it, not only by its name. */
data class LauncherSettingsTarget(val labelRes: Int, val route: String, val keywords: List<String> = emptyList()) {
    companion object {
        const val HOME = "{home}"
    }
}

object LauncherSettingsTargets {
    val all = listOf(
        LauncherSettingsTarget(R.string.settings_design_title, "settings/appearance",
            listOf("farben", "akzentfarbe", "farbmodus", "hell", "dunkel", "ecken", "schriftart", "sprache", "statusleiste",
                "colors", "accent color", "light", "dark", "corners", "font", "language", "status bar")),
        LauncherSettingsTarget(R.string.settings_section_global_background, "settings/appearance/background",
            listOf("hintergrundbild", "abdunkeln", "wallpaper", "background")),
        LauncherSettingsTarget(R.string.settings_icons_label, "settings/appearance/icons",
            listOf("icons", "symbole", "icon pack", "symbolgröße", "icon size")),
        LauncherSettingsTarget(R.string.settings_section_pages, "settings/pages",
            listOf("karussell", "endlos", "startseite", "zusatzseite", "carousel", "endless", "home screen")),
        LauncherSettingsTarget(R.string.settings_home_layout_title, "settings/pages/${LauncherSettingsTarget.HOME}/home/layout",
            listOf("widgets", "element-box", "zeilen", "layout", "rows")),
        LauncherSettingsTarget(R.string.settings_home_dock_label, "settings/pages/${LauncherSettingsTarget.HOME}/home/layout/dock",
            listOf("dock")),
        LauncherSettingsTarget(R.string.settings_home_panel_label, "settings/pages/${LauncherSettingsTarget.HOME}/home/panel",
            listOf("panel", "favoriten", "favorites")),
        LauncherSettingsTarget(R.string.settings_section_gestures, "settings/gestures",
            listOf("wischen", "doppeltippen", "lange drücken", "gestures", "swipe", "double tap", "long press")),
        LauncherSettingsTarget(R.string.settings_section_app_overview, "settings/app_overview",
            listOf("app-liste", "a-z", "suchbutton", "app list", "search button")),
        LauncherSettingsTarget(R.string.settings_section_search_full, "settings/search",
            listOf("suche", "suchmaschine", "tastatur", "kategorien", "search", "search engine", "keyboard", "categories")),
        LauncherSettingsTarget(R.string.settings_section_folder, "settings/folder", listOf("ordner", "folders")),
        LauncherSettingsTarget(R.string.settings_section_elements, "settings/elements", listOf("elemente", "elements")),
        LauncherSettingsTarget(R.string.settings_section_manage_apps, "settings/apps",
            listOf("apps ausblenden", "umbenennen", "suchcode", "hide apps", "rename", "search code")),
        LauncherSettingsTarget(R.string.settings_elements_addresses_title, "settings/elements/addresses",
            listOf("adressen", "orte", "navigation", "addresses", "places")),
        LauncherSettingsTarget(R.string.settings_elements_links_title, "settings/elements/links",
            listOf("links", "webseiten", "lesezeichen", "websites", "bookmarks")),
        LauncherSettingsTarget(R.string.settings_elements_actionbuttons_title, "settings/elements/actionbuttons",
            listOf("aktionsbuttons", "action buttons")),
        LauncherSettingsTarget(R.string.settings_system_title, "settings/system",
            listOf("standard-launcher", "ladeanimation", "default launcher", "charging animation")),
        LauncherSettingsTarget(R.string.settings_backup_label, "settings/system",
            listOf("sicherung", "backup", "exportieren", "importieren", "export", "import")),
        LauncherSettingsTarget(R.string.settings_section_permissions, "settings/permissions",
            listOf("berechtigungen", "kontakte", "benachrichtigungszugriff", "permissions", "contacts", "notification access")),
        LauncherSettingsTarget(R.string.about_title, "settings/about",
            listOf("info", "über", "version", "lizenz", "quellcode", "about", "license", "source code"))
    )
}
