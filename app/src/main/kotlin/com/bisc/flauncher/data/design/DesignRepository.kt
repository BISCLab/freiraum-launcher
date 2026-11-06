package com.bisc.flauncher.data.design

import android.content.Context
import android.net.Uri
import com.bisc.flauncher.data.db.FolderItemDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.data.repository.FolderRepository
import com.bisc.flauncher.data.repository.PageContentRepository
import com.bisc.flauncher.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Designs (bundled ones from the app's assets, installed ones in `files/designs/<id>/design.json`),
 * applying them, and going back to the user's own look. Designs are plain data files
 * (`.freiraum-design`, a ZIP); the app never downloads them itself - it has no internet access.
 * Backgrounds are not part of a design; they come from wallpaper packs.
 */
@Singleton
class DesignRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val pageContentRepository: PageContentRepository,
    private val folderRepository: FolderRepository,
    private val pageRowDao: PageRowDao,
    private val folderItemDao: FolderItemDao
) {
    private val _designs = MutableStateFlow<List<Design>>(emptyList())
    val designs: StateFlow<List<Design>> = _designs.asStateFlow()

    private val root get() = File(context.filesDir, DESIGNS_DIR)
    private val mineDir get() = File(root, MINE)

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val bundled = context.assets.list(BUNDLED_DIR).orEmpty().sorted().mapNotNull { file ->
            runCatching {
                DesignJson.parse(JSONObject(context.assets.open("$BUNDLED_DIR/$file").bufferedReader().use { it.readText() }), bundled = true)
            }.getOrNull()
        }
        val installed = root.listFiles().orEmpty()
            .filter { it.isDirectory && it.name != MINE && bundled.none { b -> b.id == it.name } }
            .mapNotNull { dir -> runCatching { load(dir) }.getOrNull() }
            .sortedBy { it.name.lowercase() }
        _designs.value = bundled + installed
    }

    private fun load(dir: File): Design = DesignJson.parse(JSONObject(File(dir, DESIGN_JSON).readText()))

    /** Installs a design file. Returns its name, or null if it isn't a valid design. */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "design_import").apply { deleteRecursively(); mkdirs() }
        try {
            var total = 0L
            var count = 0
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) continue
                        val name = entry.name
                        // Only the description, never paths leaving the design's folder. Pictures
                        // in older files are skipped.
                        if (name.contains("..")) return@withContext null
                        if (name != DESIGN_JSON) continue
                        if (++count > MAX_ENTRIES) return@withContext null
                        val out = File(staging, name).apply { parentFile?.mkdirs() }
                        out.outputStream().use { os ->
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
            val design = runCatching { load(staging) }.getOrNull() ?: return@withContext null
            val id = design.id.lowercase().replace(Regex("[^a-z0-9-]"), "-").take(40).ifBlank { return@withContext null }
            if (id == MINE || _designs.value.any { it.bundled && it.id == id }) return@withContext null
            val target = File(root, id)
            target.deleteRecursively()
            root.mkdirs()
            staging.copyRecursively(target, overwrite = true)
            refresh()
            design.name
        } finally {
            staging.deleteRecursively()
        }
    }

    suspend fun delete(design: Design) = withContext(Dispatchers.IO) {
        if (design.bundled) return@withContext
        if (settingsRepository.activeDesignId.first() == design.id) restoreMine()
        File(root, design.id).deleteRecursively()
        refresh()
    }

    /** Applies [design] on top of the launcher; the user's own look is saved first, once. */
    suspend fun apply(design: Design) = withContext(Dispatchers.IO) {
        if (settingsRepository.activeDesignId.first() == null) {
            mineDir.deleteRecursively()
            writeDesign(captureCurrent(id = MINE, name = MINE, withRows = true), mineDir)
        }
        write(design)
        settingsRepository.setActiveDesignId(design.id)
    }

    /** Back to the look the user had before the first design. */
    suspend fun restoreMine() = withContext(Dispatchers.IO) {
        val mine = runCatching { load(mineDir) }.getOrNull()
        if (mine != null) write(mine)
        settingsRepository.setActiveDesignId(null)
    }

    /** Saves the current look as a design file, e.g. to share it or as the start of a new design. */
    suspend fun exportCurrent(target: Uri, name: String) = withContext(Dispatchers.IO) {
        val id = name.lowercase().replace(Regex("[^a-z0-9-]"), "-").trim('-').ifBlank { "design" }
        val design = captureCurrent(id = id, name = name, withRows = false)
        val json = DesignJson.write(design).toString(2)
        context.contentResolver.openOutputStream(target)?.use { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry(DESIGN_JSON))
                zip.write(json.toByteArray())
                zip.closeEntry()
            }
        }
    }

    // ---- inside ----

    private suspend fun pageIdFor(role: String): Long? {
        val pages = pageContentRepository.allPages.first()
        return when (role) {
            "home" -> pages.firstOrNull { it.isHome }?.id
            "appList" -> pages.firstOrNull { it.kind == LauncherPageKind.APP_LIST }?.id
            "appGrid" -> pages.firstOrNull { it.kind == LauncherPageKind.APP_GRID }?.id
            else -> null
        }
    }

    private suspend fun write(design: Design) {
        design.theme?.let { settingsRepository.setTheme(it) }
        design.accent?.let { settingsRepository.setAccentColor(it) }
        design.font?.let { settingsRepository.setFontPreference(it) }
        design.cornerRadiusPercent?.let { settingsRepository.setDefaultCornerRadiusPercent(it) }
        DESIGN_ELEMENTS.forEach { settingsRepository.setElementAppearanceByName(it, design.elements[it]) }
        design.keyboardCapColor?.let { settingsRepository.setSearchKeyboardCapColor(it) }

        val rows = pageRowDao.observeAll().first()
        val items = folderItemDao.observeAll().first()
        val ownRows = design.rowAppearances
        val ownItems = design.folderItemAppearances
        // Corners follow Freiraum's own rule: widgets only slightly rounded, everything else by the
        // general roundness - never one value for both.
        // Snapshots from before corners were saved per widget fall back to Freiraum's widget default.
        fun cornerFor(isWidget: Boolean): Int? = if (isWidget) {
            design.widgetCornerRadiusPercent ?: if (design.rowAppearances != null) com.bisc.flauncher.ui.theme.DEFAULT_WIDGET_CORNER_RADIUS_PERCENT else null
        } else design.cornerRadiusPercent
        pageRowDao.updateAll(rows.map { row ->
            val look = when {
                ownRows != null -> ownRows[row.id]
                row.type == PageRowType.SEARCH_BAR -> design.dock ?: design.box
                row.type == PageRowType.PANEL -> design.panel ?: design.box
                else -> design.box
            }
            val isWidget = row.type == PageRowType.WIDGET || row.type == PageRowType.SYSTEM_WIDGET
            val corner = design.rowCorners?.get(row.id) ?: cornerFor(isWidget)
            (if (look != null) row.withAppearance(look) else row).let { if (corner != null) it.copy(cornerRadiusPercent = corner) else it }
        })
        folderItemDao.updateAll(items.map { item ->
            val look = if (ownItems != null) ownItems[item.id] else design.box
            val isWidget = item.type == com.bisc.flauncher.data.model.GridItemType.WIDGET || item.type == com.bisc.flauncher.data.model.GridItemType.SYSTEM_WIDGET
            val corner = design.folderItemCorners?.get(item.id) ?: cornerFor(isWidget)
            (if (look != null) item.withAppearance(look) else item).let { if (corner != null) it.copy(cornerRadiusPercent = corner) else it }
        })

        settingsRepository.setDesignEffects(design.effects)
    }

    private suspend fun captureCurrent(id: String, name: String, withRows: Boolean): Design {
        val rows = pageRowDao.observeAll().first()
        val homeId = pageIdFor("home")
        val homeRows = rows.filter { it.pageId == homeId }
        val firstBox = rows.firstOrNull { it.type == PageRowType.WIDGET || it.type == PageRowType.SYSTEM_WIDGET || it.type == PageRowType.ELEMENT_BOX }
        return Design(
            id = id,
            name = name,
            theme = settingsRepository.theme.first(),
            accent = settingsRepository.accentColor.first(),
            font = settingsRepository.fontPreference.first(),
            cornerRadiusPercent = settingsRepository.defaultCornerRadiusPercent.first(),
            box = firstBox?.appearance(),
            dock = homeRows.firstOrNull { it.type == PageRowType.SEARCH_BAR }?.appearance(),
            panel = homeRows.firstOrNull { it.type == PageRowType.PANEL }?.appearance(),
            elements = DESIGN_ELEMENTS.mapNotNull { key -> settingsRepository.elementAppearanceOrNull(key)?.let { key to it } }.toMap(),
            keyboardCapColor = settingsRepository.searchKeyboardCapColor.first(),
            effects = settingsRepository.designEffects.first(),
            widgetCornerRadiusPercent = rows.firstOrNull { it.type == PageRowType.WIDGET || it.type == PageRowType.SYSTEM_WIDGET }?.cornerRadiusPercent,
            rowAppearances = if (withRows) rows.associate { it.id to it.appearance() } else null,
            folderItemAppearances = if (withRows) folderItemDao.observeAll().first().associate { it.id to it.appearance() } else null,
            rowCorners = if (withRows) rows.associate { it.id to it.cornerRadiusPercent } else null,
            folderItemCorners = if (withRows) folderItemDao.observeAll().first().associate { it.id to it.cornerRadiusPercent } else null
        )
    }

    private fun writeDesign(design: Design, dir: File) {
        dir.mkdirs()
        File(dir, DESIGN_JSON).writeText(DesignJson.write(design).toString())
    }

    companion object {
        const val DESIGNS_DIR = "designs"
        private const val DESIGN_JSON = "design.json"
        private const val MINE = "_mine"
        private const val MAX_ENTRIES = 64
        private const val MAX_TOTAL_BYTES = 60L * 1024 * 1024
        private const val BUNDLED_DIR = "designs"
    }
}
