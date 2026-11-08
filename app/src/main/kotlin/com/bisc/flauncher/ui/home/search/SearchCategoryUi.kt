package com.bisc.flauncher.ui.home.search

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Pets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.SearchCategory

/** Default icon color per category, used everywhere a category's icon appears unless the user
 *  overrides it (see [SettingsRepository.categoryColor]/[HomeViewModel.categoryColors]). */
val SearchCategory.defaultColor: Color
    get() = when (this) {
        SearchCategory.FAVORITES -> Color(0xFFFFB300)
        SearchCategory.APPS -> Color(0xFF4F86F7)
        SearchCategory.FOLDERS -> Color(0xFFFF8A65)
        SearchCategory.CONTACTS -> Color(0xFF34A853)
        SearchCategory.SETTINGS -> Color(0xFF9AA0A6)
        SearchCategory.WEBSITES -> Color(0xFFA142F4)
        SearchCategory.PLACES -> Color(0xFFEA4335)
        SearchCategory.SHORTCUTS -> Color(0xFF26A69A)
        SearchCategory.LAUNCHER_SETTINGS -> Color(0xFF0758F5)
        SearchCategory.WEB -> Color(0xFF00ACC1)
        SearchCategory.AI -> Color(0xFFEC407A)
    }

val SearchCategory.icon: ImageVector
    get() = when (this) {
        SearchCategory.FAVORITES -> Icons.Filled.Star
        SearchCategory.APPS -> Icons.Filled.Apps
        SearchCategory.CONTACTS -> Icons.Filled.Person
        SearchCategory.SETTINGS -> Icons.Filled.Settings
        SearchCategory.WEBSITES -> Icons.Filled.Bookmark
        SearchCategory.FOLDERS -> Icons.Filled.Folder
        SearchCategory.LAUNCHER_SETTINGS -> Icons.Filled.Tune
        SearchCategory.PLACES -> Icons.Filled.Place
        SearchCategory.SHORTCUTS -> Icons.AutoMirrored.Filled.Shortcut
        SearchCategory.WEB -> Icons.Filled.Language
        SearchCategory.AI -> Icons.Filled.AutoAwesome
    }

/** Short name, used for the filter icons' descriptions. */
@get:StringRes
val SearchCategory.filterLabelRes: Int
    get() = when (this) {
        SearchCategory.FAVORITES -> R.string.search_filter_favorites
        SearchCategory.APPS -> R.string.search_filter_apps
        SearchCategory.CONTACTS -> R.string.search_filter_contacts
        SearchCategory.SETTINGS -> R.string.search_filter_settings
        SearchCategory.WEBSITES -> R.string.search_filter_websites
        SearchCategory.FOLDERS -> R.string.search_filter_folders
        SearchCategory.LAUNCHER_SETTINGS -> R.string.search_filter_launcher_settings
        SearchCategory.PLACES -> R.string.search_filter_maps
        SearchCategory.SHORTCUTS -> R.string.search_filter_shortcuts
        SearchCategory.WEB -> R.string.search_filter_web
        SearchCategory.AI -> R.string.search_filter_ai
    }

/** Name in the settings list. */
@get:StringRes
val SearchCategory.settingsLabelRes: Int
    get() = when (this) {
        SearchCategory.FAVORITES -> R.string.settings_search_include_favorites
        SearchCategory.APPS -> R.string.settings_search_include_apps
        SearchCategory.CONTACTS -> R.string.settings_search_include_contacts
        SearchCategory.SETTINGS -> R.string.settings_search_include_settings
        SearchCategory.WEBSITES -> R.string.settings_search_include_websites
        SearchCategory.FOLDERS -> R.string.settings_search_include_folders
        SearchCategory.LAUNCHER_SETTINGS -> R.string.settings_search_include_launcher_settings
        SearchCategory.PLACES -> R.string.settings_search_include_maps
        SearchCategory.SHORTCUTS -> R.string.settings_search_include_shortcuts
        SearchCategory.WEB -> R.string.settings_search_include_web
        SearchCategory.AI -> R.string.settings_search_include_ai
    }


/** Symbols a category can get instead of its own, by the name they are stored under. */
val CategoryIconChoices: List<Pair<String, ImageVector>> = listOf(
    "apps" to Icons.Filled.Apps,
    "star" to Icons.Filled.Star,
    "favorite" to Icons.Filled.Favorite,
    "folder" to Icons.Filled.Folder,
    "person" to Icons.Filled.Person,
    "group" to Icons.Filled.Group,
    "phone" to Icons.Filled.Phone,
    "mail" to Icons.Filled.Mail,
    "chat" to Icons.AutoMirrored.Filled.Chat,
    "settings" to Icons.Filled.Settings,
    "tune" to Icons.Filled.Tune,
    "build" to Icons.Filled.Build,
    "bookmark" to Icons.Filled.Bookmark,
    "link" to Icons.Filled.Link,
    "language" to Icons.Filled.Language,
    "public" to Icons.Filled.Public,
    "place" to Icons.Filled.Place,
    "map" to Icons.Filled.Map,
    "home" to Icons.Filled.Home,
    "work" to Icons.Filled.Work,
    "school" to Icons.Filled.School,
    "shortcut" to Icons.AutoMirrored.Filled.Shortcut,
    "bolt" to Icons.Filled.Bolt,
    "auto_awesome" to Icons.Filled.AutoAwesome,
    "lightbulb" to Icons.Filled.Lightbulb,
    "search" to Icons.Filled.Search,
    "music" to Icons.Filled.MusicNote,
    "movie" to Icons.Filled.Movie,
    "photo" to Icons.Filled.Photo,
    "cart" to Icons.Filled.ShoppingCart,
    "restaurant" to Icons.Filled.Restaurant,
    "pets" to Icons.Filled.Pets
)

/** Each category's own chosen symbol, if any; provided once for the whole app. */
val LocalCategoryIcons = androidx.compose.runtime.compositionLocalOf<Map<SearchCategory, String?>> { emptyMap() }

/** The symbol a category shows: the one chosen for it, or its own. */
@androidx.compose.runtime.Composable
fun SearchCategory.shownIcon(): ImageVector =
    LocalCategoryIcons.current[this]?.let { key -> CategoryIconChoices.firstOrNull { it.first == key }?.second } ?: icon
