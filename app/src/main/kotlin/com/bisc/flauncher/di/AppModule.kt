package com.bisc.flauncher.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bisc.flauncher.data.db.AppOverrideDao
import com.bisc.flauncher.data.db.AppRowEntryDao
import com.bisc.flauncher.data.db.FLauncherDatabase
import com.bisc.flauncher.data.db.FolderDao
import com.bisc.flauncher.data.db.FolderItemDao
import com.bisc.flauncher.data.db.IconPackMappingDao
import com.bisc.flauncher.data.db.LauncherPageDao
import com.bisc.flauncher.data.db.NoteDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.db.DockSlotDao
import com.bisc.flauncher.data.db.PinnedContactDao
import com.bisc.flauncher.data.db.PinnedShortcutDao
import com.bisc.flauncher.data.db.PinnedWebsiteDao
import com.bisc.flauncher.data.db.SavedMapTargetDao
import com.bisc.flauncher.data.db.SidebarItemDao
import com.bisc.flauncher.data.db.SidebarPinnedAppDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE saved_map_targets ADD COLUMN address TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE page_rows ADD COLUMN clockShowAmPm INTEGER NOT NULL DEFAULT 1")
    }
}

private val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `page_rows_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`pageId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `type` TEXT NOT NULL, `payload` TEXT NOT NULL, " +
                "`spanY` INTEGER NOT NULL, `elementFillMode` TEXT NOT NULL, `fillColor` INTEGER NOT NULL, " +
                "`fillSecondaryColor` INTEGER NOT NULL, `fillMode` TEXT NOT NULL, `fillDirection` TEXT NOT NULL, " +
                "`elementOpacity` REAL NOT NULL, `borderEnabled` INTEGER NOT NULL, `cornerStyle` TEXT NOT NULL, " +
                "`edgeToEdge` INTEGER NOT NULL, `columns` INTEGER NOT NULL, `clockStyle` TEXT NOT NULL, " +
                "`clockDateActionType` TEXT NOT NULL, `clockDateActionPayload` TEXT, `clockTimeActionType` TEXT NOT NULL, " +
                "`clockTimeActionPayload` TEXT, `clockTimeFormat` TEXT NOT NULL, `clockDateFormat` TEXT NOT NULL, " +
                "`clockShowNextAlarm` INTEGER NOT NULL, `clockAlignment` TEXT NOT NULL, " +
                "FOREIGN KEY(`pageId`) REFERENCES `launcher_pages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "INSERT INTO page_rows_new (id, pageId, orderIndex, type, payload, spanY, elementFillMode, fillColor, fillSecondaryColor, fillMode, fillDirection, elementOpacity, borderEnabled, cornerStyle, edgeToEdge, columns, clockStyle, clockDateActionType, clockDateActionPayload, clockTimeActionType, clockTimeActionPayload, clockTimeFormat, clockDateFormat, clockShowNextAlarm, clockAlignment) SELECT id, pageId, orderIndex, type, payload, spanY, elementFillMode, fillColor, fillSecondaryColor, fillMode, fillDirection, elementOpacity, borderEnabled, cornerStyle, edgeToEdge, columns, clockStyle, clockDateActionType, clockDateActionPayload, clockTimeActionType, clockTimeActionPayload, clockTimeFormat, clockDateFormat, clockShowNextAlarm, 'center' FROM page_rows"
        )
        db.execSQL("DROP TABLE page_rows")
        db.execSQL("ALTER TABLE page_rows_new RENAME TO page_rows")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_page_rows_pageId` ON `page_rows` (`pageId`)")
    }
}

private val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE app_overrides ADD COLUMN iconPackPackage TEXT")
        db.execSQL("ALTER TABLE app_overrides ADD COLUMN iconPackDrawable TEXT")
    }
}

private val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for (table in listOf("page_rows", "folder_items")) {
            db.execSQL("ALTER TABLE $table ADD COLUMN elementBorderColor INTEGER")
            db.execSQL("ALTER TABLE $table ADD COLUMN elementBorderWidth REAL NOT NULL DEFAULT 1.0")
            db.execSQL("ALTER TABLE $table ADD COLUMN elementContentColor INTEGER")
        }
    }
}

private val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM page_rows WHERE type = 'WIDGET' AND payload = 'calendar'")
        db.execSQL("DELETE FROM folder_items WHERE type = 'WIDGET' AND payload = 'calendar'")
    }
}

private val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE page_rows ADD COLUMN heightPx INTEGER")
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flauncher_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): FLauncherDatabase =
        Room.databaseBuilder(ctx, FLauncherDatabase::class.java, "flauncher.db")
            .addMigrations(MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideLauncherPageDao(db: FLauncherDatabase): LauncherPageDao = db.launcherPageDao()

    @Provides
    fun providePageRowDao(db: FLauncherDatabase): PageRowDao = db.pageRowDao()

    @Provides
    fun provideAppRowEntryDao(db: FLauncherDatabase): AppRowEntryDao = db.appRowEntryDao()

    @Provides
    fun provideFolderDao(db: FLauncherDatabase): FolderDao = db.folderDao()

    @Provides
    fun provideFolderItemDao(db: FLauncherDatabase): FolderItemDao = db.folderItemDao()

    @Provides
    fun provideNoteDao(db: FLauncherDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideIconPackMappingDao(db: FLauncherDatabase): IconPackMappingDao = db.iconPackMappingDao()

    @Provides
    fun provideSavedMapTargetDao(db: FLauncherDatabase): SavedMapTargetDao = db.savedMapTargetDao()

    @Provides
    fun providePinnedWebsiteDao(db: FLauncherDatabase): PinnedWebsiteDao = db.pinnedWebsiteDao()

    @Provides
    fun providePinnedShortcutDao(db: FLauncherDatabase): PinnedShortcutDao = db.pinnedShortcutDao()

    @Provides
    fun provideSidebarItemDao(db: FLauncherDatabase): SidebarItemDao = db.sidebarItemDao()

    @Provides
    fun provideAppOverrideDao(db: FLauncherDatabase): AppOverrideDao = db.appOverrideDao()

    @Provides
    fun provideSidebarPinnedAppDao(db: FLauncherDatabase): SidebarPinnedAppDao = db.sidebarPinnedAppDao()

    @Provides
    fun providePinnedContactDao(db: FLauncherDatabase): PinnedContactDao = db.pinnedContactDao()

    @Provides
    fun provideDockSlotDao(db: FLauncherDatabase): DockSlotDao = db.dockSlotDao()
}
