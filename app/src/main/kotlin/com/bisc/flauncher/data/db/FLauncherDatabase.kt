package com.bisc.flauncher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bisc.flauncher.data.model.AppOverride
import com.bisc.flauncher.data.model.AppRowEntry
import com.bisc.flauncher.data.model.DockSlot
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.IconPackMapping
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.Note
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PinnedContact
import com.bisc.flauncher.data.model.PinnedShortcut
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.model.SidebarItem
import com.bisc.flauncher.data.model.SidebarPinnedApp

@Database(
    entities = [
        LauncherPage::class, PageRow::class, AppRowEntry::class, Folder::class, FolderItem::class,
        Note::class, IconPackMapping::class, SavedMapTarget::class, PinnedWebsite::class,
        PinnedShortcut::class, SidebarItem::class, AppOverride::class, SidebarPinnedApp::class,
        PinnedContact::class, DockSlot::class
    ],
    version = 39,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FLauncherDatabase : RoomDatabase() {
    abstract fun launcherPageDao(): LauncherPageDao
    abstract fun pageRowDao(): PageRowDao
    abstract fun appRowEntryDao(): AppRowEntryDao
    abstract fun folderDao(): FolderDao
    abstract fun folderItemDao(): FolderItemDao
    abstract fun noteDao(): NoteDao
    abstract fun iconPackMappingDao(): IconPackMappingDao
    abstract fun savedMapTargetDao(): SavedMapTargetDao
    abstract fun pinnedWebsiteDao(): PinnedWebsiteDao
    abstract fun pinnedShortcutDao(): PinnedShortcutDao
    abstract fun sidebarItemDao(): SidebarItemDao
    abstract fun appOverrideDao(): AppOverrideDao
    abstract fun sidebarPinnedAppDao(): SidebarPinnedAppDao
    abstract fun pinnedContactDao(): PinnedContactDao
    abstract fun dockSlotDao(): DockSlotDao
}
