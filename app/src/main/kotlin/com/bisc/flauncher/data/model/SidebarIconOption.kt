package com.bisc.flauncher.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

/** Selectable icons that replace the default icon of a sidebar item. */
enum class SidebarIconOption(val icon: ImageVector) {
    APPS(Icons.Filled.Apps),
    FOLDER(Icons.Filled.Folder),
    WIDGETS(Icons.Filled.Widgets),
    BOOKMARK(Icons.Filled.Bookmark),
    MAP(Icons.Filled.Map),
    SHORTCUT(Icons.AutoMirrored.Filled.Shortcut),
    STAR(Icons.Filled.Star),
    HOME(Icons.Filled.Home),
    PERSON(Icons.Filled.Person),
    SETTINGS(Icons.Filled.Settings),
    PHONE(Icons.Filled.Phone),
    EMAIL(Icons.Filled.Email),
    CAMERA(Icons.Filled.CameraAlt),
    MUSIC(Icons.Filled.MusicNote),
    CALENDAR(Icons.Filled.CalendarMonth),
    NOTE(Icons.Filled.EditNote),
    WEATHER(Icons.Filled.Cloud),
    PLACE(Icons.Filled.Place),
    LINK(Icons.Filled.Link),
    FAVORITE(Icons.Filled.Favorite)
}
