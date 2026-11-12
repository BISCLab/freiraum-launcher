package com.bisc.flauncher.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.bisc.flauncher.MainActivity
import com.bisc.flauncher.data.db.FLauncherDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Exports and imports everything needed to move the launcher or set it up again: settings,
 *  database, background images, installed designs (with the saved own look) and wallpaper packs,
 *  as a single zip archive. */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: FLauncherDatabase
) {
    enum class ImportResult { READY, NOT_A_BACKUP, TOO_NEW, FAILED, WRONG_PASSWORD }

    /** Whether the file at [uri] is a backup locked with a password. */
    suspend fun isLocked(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(BackupCrypto.HEADER_PEEK)
                var read = 0
                while (read < header.size) { val n = input.read(header, read, header.size - read); if (n < 0) break; read += n }
                BackupCrypto.isLocked(header.copyOf(read))
            } ?: false
        }.getOrDefault(false)
    }

    /** Writes the backup to [uri], locked with [password]. */
    suspend fun export(uri: Uri, password: CharArray): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val db = database.openHelper.writableDatabase
            db.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            val manifest = JSONObject()
                .put("format", FORMAT)
                .put("dbVersion", db.version)
                .put("createdAt", System.currentTimeMillis())
                .put("app", context.packageName)
                .toString()

            BackupCrypto.lock(context.contentResolver.openOutputStream(uri, "w")!!.buffered(), password).buffered().use { out ->
                ZipOutputStream(out).use { zip ->
                    zip.put(MANIFEST, manifest.toByteArray())
                    zip.putFile(DB_ENTRY, databaseFile(context))
                    settingsFile(context).takeIf { it.exists() }?.let { zip.putFile(SETTINGS_ENTRY, it) }
                    backgroundsDir(context).listFiles()?.filter { it.isFile }?.forEach { zip.putFile("$BACKGROUNDS/${it.name}", it) }
                    // Designs and packs: one folder each, files directly inside.
                    FOLDER_TREES.forEach { tree ->
                        File(context.filesDir, tree).listFiles()?.filter { it.isDirectory }?.forEach { folder ->
                            folder.listFiles()?.filter { it.isFile }?.forEach { zip.putFile("$tree/${folder.name}/${it.name}", it) }
                        }
                    }
                }
            }
        }.isSuccess
    }

    /** [password] opens a locked backup; older unlocked ones open without. */
    suspend fun stageImport(uri: Uri, password: CharArray? = null): ImportResult = withContext(Dispatchers.IO) {
        val staging = stagingDir(context)
        staging.deleteRecursively()
        staging.mkdirs()
        try {
            var manifest: JSONObject? = null
            context.contentResolver.openInputStream(uri)?.buffered()?.use { raw ->
                raw.mark(BackupCrypto.HEADER_PEEK)
                val header = ByteArray(BackupCrypto.HEADER_PEEK).also { raw.read(it) }
                raw.reset()
                val locked = BackupCrypto.isLocked(header)
                if (locked && password == null) return@withContext ImportResult.WRONG_PASSWORD
                val input = if (locked) BackupCrypto.unlock(raw, password!!).buffered() else raw
                ZipInputStream(input).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        val target = when {
                            entry.isDirectory -> null
                            entry.name == MANIFEST -> null
                            entry.name == DB_ENTRY -> File(staging, DB_ENTRY)
                            entry.name == SETTINGS_ENTRY -> File(staging, SETTINGS_ENTRY)
                            entry.name.startsWith("$BACKGROUNDS/") && SAFE_NAME.matches(entry.name.removePrefix("$BACKGROUNDS/")) ->
                                File(staging, entry.name).also { it.parentFile?.mkdirs() }
                            isTreeEntry(entry.name) -> File(staging, entry.name).also { it.parentFile?.mkdirs() }
                            else -> null
                        }
                        if (entry.name == MANIFEST) {
                            manifest = runCatching { JSONObject(zip.readBytes().decodeToString()) }.getOrNull()
                        } else if (target != null) {
                            target.outputStream().use { zip.copyTo(it) }
                        }
                        entry = zip.nextEntry
                    }
                    // A locked file must run to its last chunk; a cut-off one fails here.
                    if (locked) { val sink = ByteArray(8192); while (input.read(sink) >= 0) Unit }
                }
            } ?: return@withContext ImportResult.FAILED

            val info = manifest
            val stagedDb = File(staging, DB_ENTRY)
            if (info == null || info.optInt("format", -1) != FORMAT || !isOwnAppId(info.optString("app")) ||
                !stagedDb.exists() || !looksLikeSqlite(stagedDb)
            ) {
                staging.deleteRecursively()
                return@withContext ImportResult.NOT_A_BACKUP
            }
            if (info.optInt("dbVersion", Int.MAX_VALUE) > database.openHelper.readableDatabase.version) {
                staging.deleteRecursively()
                return@withContext ImportResult.TOO_NEW
            }
            File(staging, READY).writeText("1")
            ImportResult.READY
        } catch (e: BackupCrypto.WrongPasswordException) {
            staging.deleteRecursively()
            ImportResult.WRONG_PASSWORD
        } catch (e: Exception) {
            staging.deleteRecursively()
            ImportResult.FAILED
        }
    }

    fun restart() {
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        Runtime.getRuntime().exit(0)
    }

    companion object {
        private const val FORMAT = 1
        // The app's id before it was renamed to Freiraum Launcher; its backups import unchanged.
        private const val PREVIOUS_APP_ID = "com.bisc.flauncher"
        private const val APP_ID = "com.bisc.freiraum"

        // Release and debug builds read each other's backups.
        private fun isOwnAppId(id: String) =
            id == PREVIOUS_APP_ID || id == APP_ID || id.startsWith("$APP_ID.")

        private const val MANIFEST = "manifest.json"
        private const val DB_ENTRY = "flauncher.db"
        private const val SETTINGS_ENTRY = "settings.preferences_pb"
        private const val BACKGROUNDS = "backgrounds"
        private const val READY = "READY"
        private const val DB_NAME = "flauncher.db"
        private const val SETTINGS_NAME = "datastore/flauncher_settings.preferences_pb"
        private val SAFE_NAME = Regex("[A-Za-z0-9_.-]+")
        private val FOLDER_TREES = listOf("designs", "wallpaper_packs", "search_icons")

        /** `designs/<folder>/<file>` or `wallpaper_packs/<folder>/<file>`, nothing deeper or odd. */
        private fun isTreeEntry(name: String): Boolean {
            val parts = name.split('/')
            return parts.size == 3 && parts[0] in FOLDER_TREES && parts.drop(1).all { SAFE_NAME.matches(it) && it != "." && it != ".." }
        }

        private fun databaseFile(context: Context) = context.getDatabasePath(DB_NAME)
        private fun settingsFile(context: Context) = File(context.filesDir, SETTINGS_NAME)
        private fun backgroundsDir(context: Context) = File(context.filesDir, BACKGROUNDS)
        private fun stagingDir(context: Context) = File(context.filesDir, "restore_staging")

        private fun ZipOutputStream.put(name: String, bytes: ByteArray) {
            putNextEntry(ZipEntry(name))
            write(bytes)
            closeEntry()
        }

        private fun ZipOutputStream.putFile(name: String, file: File) {
            putNextEntry(ZipEntry(name))
            file.inputStream().use { it.copyTo(this) }
            closeEntry()
        }

        private fun looksLikeSqlite(file: File): Boolean = runCatching {
            file.inputStream().use { input ->
                val header = ByteArray(15)
                input.read(header) == 15 && String(header, Charsets.US_ASCII) == "SQLite format 3"
            }
        }.getOrDefault(false)

        fun applyStagedRestore(context: Context) {
            val staging = stagingDir(context)
            if (!File(staging, READY).exists()) return
            runCatching {
                val db = databaseFile(context)
                db.parentFile?.mkdirs()
                listOf("", "-wal", "-shm", "-journal").forEach { File(db.path + it).delete() }
                File(staging, DB_ENTRY).copyTo(db, overwrite = true)

                File(staging, SETTINGS_ENTRY).takeIf { it.exists() }?.let {
                    val target = settingsFile(context)
                    target.parentFile?.mkdirs()
                    it.copyTo(target, overwrite = true)
                }

                val backgrounds = backgroundsDir(context)
                backgrounds.listFiles()?.forEach { it.delete() }
                File(staging, BACKGROUNDS).listFiles()?.forEach {
                    backgrounds.mkdirs()
                    it.copyTo(File(backgrounds, it.name), overwrite = true)
                }

                // Older backups have no designs or packs; then the installed ones stay.
                FOLDER_TREES.forEach { tree ->
                    val staged = File(staging, tree)
                    if (staged.isDirectory) {
                        val target = File(context.filesDir, tree)
                        target.deleteRecursively()
                        staged.copyRecursively(target, overwrite = true)
                    }
                }
            }
            staging.deleteRecursively()
        }
    }
}
