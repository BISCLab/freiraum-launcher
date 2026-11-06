package com.bisc.flauncher.data.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.BackgroundSource
import com.bisc.flauncher.data.model.ColorFill
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.repository.PageContentRepository
import com.bisc.flauncher.data.settings.SettingsRepository
import com.bisc.flauncher.ui.theme.backgroundColorFor
import com.bisc.flauncher.ui.theme.isDark
import com.bisc.flauncher.util.BackgroundImageStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/** A set of backgrounds, one per page, from a `.freiraum-wallpapers` file. */
data class WallpaperPack(
    val id: String,
    val name: String,
    val nameDe: String? = null,
    val author: String = "",
    val version: String = "",
    /** Made for dark mode (true), light mode (false) or either (null). */
    val dark: Boolean? = null,
    /** By role: home, appList, search, appGrid, lock. */
    val images: Map<String, File> = emptyMap(),
    /** For further pages, in their order. */
    val extras: List<File> = emptyList()
) {
    fun displayName(german: Boolean): String = if (german && !nameDe.isNullOrBlank()) nameDe else name
    val preview: File? get() = images["home"] ?: images.values.firstOrNull()
    val lock: File? get() = images["lock"]
}

/**
 * Installed wallpaper packs (`files/wallpaper_packs/<id>/`), applying one to the pages, and the
 * plain matte backgrounds without pictures. Packs are downloaded in the browser; Freiraum has no
 * internet access.
 *
 * pack.json (format 1): id, name, name_de, author, version, dark, wallpapers { home, appList,
 * search, appGrid, lock }, extras [ ... ]. Every picture sits next to pack.json.
 */
@Singleton
class WallpaperPackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val pageContentRepository: PageContentRepository
) {
    private val _packs = MutableStateFlow<List<WallpaperPack>>(emptyList())
    val packs: StateFlow<List<WallpaperPack>> = _packs.asStateFlow()

    private val root get() = File(context.filesDir, PACKS_DIR)

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _packs.value = root.listFiles().orEmpty()
            .filter { it.isDirectory }
            .mapNotNull { dir -> runCatching { load(dir) }.getOrNull() }
            .sortedBy { it.name.lowercase() }
    }

    private fun load(dir: File): WallpaperPack {
        val json = JSONObject(File(dir, PACK_JSON).readText())
        require(json.optInt("format", -1) == FORMAT) { "Unsupported pack format" }
        fun file(name: String?): File? = name?.takeIf { it.isNotBlank() && SAFE_IMAGE.matches(it) }?.let { File(dir, it) }?.takeIf { it.isFile }
        val wallpapers = json.optJSONObject("wallpapers")
        return WallpaperPack(
            id = json.getString("id"),
            name = json.optString("name", json.getString("id")),
            nameDe = json.optString("name_de").ifBlank { null },
            author = json.optString("author"),
            version = json.optString("version"),
            dark = if (json.has("dark")) json.getBoolean("dark") else null,
            images = ROLES.mapNotNull { role -> file(wallpapers?.optString(role))?.let { role to it } }.toMap(),
            extras = json.optJSONArray("extras")?.let { a -> (0 until a.length()).mapNotNull { file(a.optString(it)) } }.orEmpty()
        )
    }

    /** Installs a pack file. Returns its name, or null if it isn't a valid pack. */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "pack_import").apply { deleteRecursively(); mkdirs() }
        try {
            var total = 0L
            var count = 0
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) continue
                        val name = entry.name
                        if (name.contains("..") || (name != PACK_JSON && !SAFE_IMAGE.matches(name))) return@withContext null
                        if (++count > MAX_ENTRIES) return@withContext null
                        File(staging, name).outputStream().use { os ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val n = zip.read(buffer)
                                if (n < 0) break
                                total += n
                                if (total > MAX_TOTAL_BYTES) return@withContext null
                                os.write(buffer, 0, n)
                            }
                        }
                    }
                }
            } ?: return@withContext null
            val pack = runCatching { load(staging) }.getOrNull() ?: return@withContext null
            if (pack.images.isEmpty() && pack.extras.isEmpty()) return@withContext null
            val id = pack.id.lowercase().replace(Regex("[^a-z0-9-]"), "-").take(40).ifBlank { return@withContext null }
            val target = File(root, id)
            target.deleteRecursively()
            root.mkdirs()
            staging.copyRecursively(target, overwrite = true)
            refresh()
            pack.name
        } finally {
            staging.deleteRecursively()
        }
    }

    suspend fun delete(pack: WallpaperPack) = withContext(Dispatchers.IO) {
        // The pictures in use are copies, so the pages keep them.
        File(root, pack.id).deleteRecursively()
        if (settingsRepository.activeWallpaperPackId.first() == pack.id) settingsRepository.setActiveWallpaperPackId(null)
        refresh()
    }

    /** Puts the pack's pictures on the pages: Home (and every page without one of its own), App
     *  list, Text search, App grid, and the extras on the other pages in their order. */
    suspend fun apply(pack: WallpaperPack, lockScreen: Boolean) = withContext(Dispatchers.IO) {
        val pages = pageContentRepository.allPages.first()
        val home = pages.firstOrNull { it.isHome }
        fun image(file: File?, key: String): BackgroundConfig? = file?.let { BackgroundImageStore.save(context, Uri.fromFile(it), key) }
            ?.let { BackgroundConfig(source = BackgroundSource.IMAGE, imagePath = it) }

        image(pack.images["home"] ?: pack.preview, "global")?.let { settingsRepository.setGlobalBackground(it) }
        home?.let { page -> setPage(page.id, image(pack.images["home"], "page_${page.id}")) }
        pages.firstOrNull { it.kind == LauncherPageKind.APP_LIST }?.let { page -> setPage(page.id, image(pack.images["appList"], "page_${page.id}")) }
        pages.firstOrNull { it.kind == LauncherPageKind.APP_GRID }?.let { page -> setPage(page.id, image(pack.images["appGrid"], "page_${page.id}")) }
        val search = image(pack.images["search"], SettingsRepository.SEARCH_BACKGROUND_KEY)
        if (search != null) settingsRepository.setSearchBackgroundConfig(search) else settingsRepository.clearSearchBackgroundConfig()
        pages.filter { it.kind == LauncherPageKind.ROWS && !it.isHome }.sortedBy { it.orderIndex }.forEachIndexed { i, page ->
            setPage(page.id, image(pack.extras.getOrNull(i), "page_${page.id}"))
        }
        if (lockScreen) pack.lock?.let { file ->
            runCatching { file.inputStream().use { WallpaperManager.getInstance(context).setStream(it, null, true, WallpaperManager.FLAG_LOCK) } }
        }
        settingsRepository.setActiveWallpaperPackId(pack.id)
    }

    /** No pictures: matte black in dark mode, chalk white in light mode, on every page. */
    suspend fun applyPlain() = withContext(Dispatchers.IO) {
        val isDark = settingsRepository.theme.first().isDark(context)
        settingsRepository.setGlobalBackground(BackgroundConfig(source = BackgroundSource.COLOR, fill = ColorFill(color = backgroundColorFor(isDark))))
        pageContentRepository.allPages.first().forEach { page -> setPage(page.id, null) }
        settingsRepository.clearSearchBackgroundConfig()
        settingsRepository.setActiveWallpaperPackId(PLAIN)
    }

    private suspend fun setPage(pageId: Long, config: BackgroundConfig?) {
        if (config != null) settingsRepository.setPageBackgroundConfig(pageId, config)
        else {
            BackgroundImageStore.delete(context, "page_$pageId")
            settingsRepository.clearPageBackgroundConfig(pageId)
        }
    }

    companion object {
        const val PACKS_DIR = "wallpaper_packs"
        /** Stands for "no pictures" as the active pack. */
        const val PLAIN = ""
        private const val PACK_JSON = "pack.json"
        private const val FORMAT = 1
        private const val MAX_ENTRIES = 32
        private const val MAX_TOTAL_BYTES = 80L * 1024 * 1024
        private val ROLES = listOf("home", "appList", "search", "appGrid", "lock")
        private val SAFE_IMAGE = Regex("^[A-Za-z0-9._-]+\\.(png|jpg|jpeg|webp)$")
    }
}
