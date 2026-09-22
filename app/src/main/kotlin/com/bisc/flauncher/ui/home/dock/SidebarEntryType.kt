package com.bisc.flauncher.ui.home.dock

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import com.bisc.flauncher.R

/** Sidebar categories in their fixed display order. */
enum class SidebarEntryType(val icon: ImageVector, val labelRes: Int) {
    APPS(Icons.Filled.Apps, R.string.sidebar_apps),
    FOLDERS(Icons.Filled.Folder, R.string.sidebar_folders),
    PAGES(Icons.Filled.Widgets, R.string.sidebar_pages),
    NOTES(Icons.Filled.EditNote, R.string.sidebar_notes),
    WEBSITES(Icons.Filled.Language, R.string.sidebar_websites),
    MAPS(Icons.Filled.Map, R.string.sidebar_maps),
    SHORTCUTS(Icons.AutoMirrored.Filled.Shortcut, R.string.sidebar_shortcuts),
    CONTACTS(Icons.Filled.Person, R.string.sidebar_contacts)
}
