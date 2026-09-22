package com.bisc.flauncher.ui.home.search

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.SearchCategory

val SearchCategory.icon: ImageVector
    get() = when (this) {
        SearchCategory.APPS -> Icons.Filled.Apps
        SearchCategory.CONTACTS -> Icons.Filled.Person
        SearchCategory.SETTINGS -> Icons.Filled.Settings
        SearchCategory.NOTES -> Icons.Filled.EditNote
        SearchCategory.WEBSITES -> Icons.Filled.Bookmark
        SearchCategory.FOLDERS -> Icons.Filled.Folder
        SearchCategory.EMAIL -> Icons.Filled.Mail
        SearchCategory.PLACES -> Icons.Filled.Place
        SearchCategory.SHORTCUTS -> Icons.AutoMirrored.Filled.Shortcut
        SearchCategory.WEB -> Icons.Filled.Language
        SearchCategory.AI -> Icons.Filled.AutoAwesome
    }

/** Short name, used for the filter icons' descriptions. */
@get:StringRes
val SearchCategory.filterLabelRes: Int
    get() = when (this) {
        SearchCategory.APPS -> R.string.search_filter_apps
        SearchCategory.CONTACTS -> R.string.search_filter_contacts
        SearchCategory.SETTINGS -> R.string.search_filter_settings
        SearchCategory.NOTES -> R.string.search_filter_notes
        SearchCategory.WEBSITES -> R.string.search_filter_websites
        SearchCategory.FOLDERS -> R.string.search_filter_folders
        SearchCategory.EMAIL -> R.string.search_filter_email
        SearchCategory.PLACES -> R.string.search_filter_maps
        SearchCategory.SHORTCUTS -> R.string.search_filter_shortcuts
        SearchCategory.WEB -> R.string.search_filter_web
        SearchCategory.AI -> R.string.search_filter_ai
    }

/** Name in the settings list. */
@get:StringRes
val SearchCategory.settingsLabelRes: Int
    get() = when (this) {
        SearchCategory.APPS -> R.string.settings_search_include_apps
        SearchCategory.CONTACTS -> R.string.settings_search_include_contacts
        SearchCategory.SETTINGS -> R.string.settings_search_include_settings
        SearchCategory.NOTES -> R.string.settings_search_include_notes
        SearchCategory.WEBSITES -> R.string.settings_search_include_websites
        SearchCategory.FOLDERS -> R.string.settings_search_include_folders
        SearchCategory.EMAIL -> R.string.settings_search_include_email
        SearchCategory.PLACES -> R.string.settings_search_include_maps
        SearchCategory.SHORTCUTS -> R.string.settings_search_include_shortcuts
        SearchCategory.WEB -> R.string.settings_search_include_web
        SearchCategory.AI -> R.string.settings_search_include_ai
    }
