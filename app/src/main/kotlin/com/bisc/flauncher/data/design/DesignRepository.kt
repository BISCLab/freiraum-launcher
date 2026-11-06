package com.bisc.flauncher.data.design

import android.content.Context
import android.net.Uri
import com.bisc.flauncher.data.db.FolderItemDao
import com.bisc.flauncher.data.db.PageRowDao
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.BackgroundSource
import com.bisc.flauncher.data.model.ColorFill
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.withAppearance
import com.bisc.flauncher.data.repository.FolderRepository
import com.bisc.flauncher.data.repository.PageContentRepository
import com.bisc.flauncher.data.settings.SettingsRepository
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
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Installed designs (`files/designs/<id>/design.json` plus its images), applying them, and going
 * back to the user's own look. Designs are plain data files (`.freiraum-design`, a ZIP); the app
 * never downloads them itself - it has no internet access.
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
        _designs.value = root.listFiles().orEmpty()
            .filter { it.isDirectory && it.name != MINE }
            .mapNotNull { dir -> runCatching { load(dir) }.getOrNull() }
            .sortedBy { it.name.lowercase() }
    }

    private fun load(dir: File): Design = DesignJson.parse(JSONObject(File(dir, DESIGN_JSON).readText()), dir)

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
                        // Only the description and images, never paths leaving the design's folder.
                        if (!SAFE_NAME.matches(name) || name.contains("..")) return@withContext null
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
            if (id == MINE) return@withContext null
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
        val images = mutableMapOf<File, String>()
        fun imageName(file: File) = images.getOrPut(file) { "wallpapers/${images.size + 1}.${file.extension.ifBlank { "jpg" }}" }
        val json = DesignJson.write(design, ::imageName).toString(2)
        context.contentResolver.openOutputStream(target)?.use { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry(DESIGN_JSON))
                zip.write(json.toByteArray())
                zip.closeEntry()
                images.forEach { (file, entry) ->
                    if (file.isFile) {
                        zip.putNextEntry(ZipEntry(entry))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    suspend fun lockWallpaperOf(design: Design): File? = design.lockWallpaper?.takeIf { it.isFile }

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

    private suspend fun currentBackground(role: String): BackgroundConfig? = when (role) {
        "global" -> settingsRepository.globalBackground.first()
        "search" -> settingsRepository.searchBackgroundOverride.first()
        else -> pageIdFor(role)?.let { settingsRepository.pageBackgroundOverride(it).first() }
    }

    private suspend fun setBackground(role: String, background: DesignBackground?) {
        val key = when (role) {
            "global" -> "global"
            "search" -> SettingsRepository.SEARCH_BACKGROUND_KEY
            else -> pageIdFor(role)?.let { "page_$it" } ?: return
        }
        val config = background?.let { b ->
            val image = b.image?.takeIf { it.isFile }?.let { BackgroundImageStore.save(context, Uri.fromFile(it), key) }
            when {
                image != null -> BackgroundConfig(source = BackgroundSource.IMAGE, imagePath = image, shade = b.shade)
                b.color != null -> BackgroundConfig(source = BackgroundSource.COLOR, fill = ColorFill(color = b.color), shade = b.shade)
                else -> null
            }
        }
        when (role) {
            "global" -> if (config != null) settingsRepository.setGlobalBackground(config)
            "search" -> if (config != null) settingsRepository.setSearchBackgroundConfig(config) else settingsRepository.clearSearchBackgroundConfig()
            else -> pageIdFor(role)?.let { id ->
                if (config != null) settingsRepository.setPageBackgroundConfig(id, config) else settingsRepository.clearPageBackgroundConfig(id)
            }
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

        BACKGROUND_ROLES.forEach { role ->
            val background = design.backgrounds[role]
            // A design without a global background keeps the current one; every other page then
            // simply follows it.
            if (role != "global" || background != null) setBackground(role, background)
        }
        settingsRepository.setDesignEffects(design.effects)
    }

    private suspend fun captureCurrent(id: String, name: String, withRows: Boolean): Design {
        val rows = pageRowDao.observeAll().first()
        val homeId = pageIdFor("home")
        val homeRows = rows.filter { it.pageId == homeId }
        val firstBox = rows.firstOrNull { it.type == PageRowType.WIDGET || it.type == PageRowType.SYSTEM_WIDGET || it.type == PageRowType.ELEMENT_BOX }
        val backgrounds = BACKGROUND_ROLES.mapNotNull { role ->
            currentBackground(role)?.let { config ->
                role to when (config.source) {
                    BackgroundSource.IMAGE -> DesignBackground(image = config.imagePath?.let(::File), shade = config.shade)
                    BackgroundSource.COLOR -> DesignBackground(color = config.fill.color, shade = config.shade)
                }
            }
        }.toMap()
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
            backgrounds = backgrounds,
            effects = settingsRepository.designEffects.first(),
            widgetCornerRadiusPercent = rows.firstOrNull { it.type == PageRowType.WIDGET || it.type == PageRowType.SYSTEM_WIDGET }?.cornerRadiusPercent,
            rowAppearances = if (withRows) rows.associate { it.id to it.appearance() } else null,
            folderItemAppearances = if (withRows) folderItemDao.observeAll().first().associate { it.id to it.appearance() } else null,
            rowCorners = if (withRows) rows.associate { it.id to it.cornerRadiusPercent } else null,
            folderItemCorners = if (withRows) folderItemDao.observeAll().first().associate { it.id to it.cornerRadiusPercent } else null
        )
    }

    /** Writes [design] with copies of its images into [dir] (images move into the folder, since the
     *  live background files get replaced as soon as another design is applied). */
    private fun writeDesign(design: Design, dir: File) {
        val images = File(dir, "wallpapers").apply { mkdirs() }
        var n = 0
        fun copyImage(file: File): String {
            val name = "wallpapers/${++n}.${file.extension.ifBlank { "jpg" }}"
            if (file.isFile) file.copyTo(File(dir, name), overwrite = true)
            return name
        }
        val json = DesignJson.write(design, ::copyImage)
        File(dir, DESIGN_JSON).writeText(json.toString())
        if (images.listFiles().isNullOrEmpty()) images.delete()
    }

    companion object {
        const val DESIGNS_DIR = "designs"
        private const val DESIGN_JSON = "design.json"
        private const val MINE = "_mine"
        private const val MAX_ENTRIES = 64
        private const val MAX_TOTAL_BYTES = 60L * 1024 * 1024
        private val SAFE_NAME = Regex("^(design\\.json|preview\\.(png|jpg|jpeg|webp)|wallpapers/[A-Za-z0-9._-]+\\.(png|jpg|jpeg|webp))$")
    }
}
