package com.bisc.flauncher.data.design

import com.bisc.flauncher.data.model.ColorFill
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.FillMode
import com.bisc.flauncher.data.model.GradientDirection
import com.bisc.flauncher.ui.theme.FontPreference
import com.bisc.flauncher.ui.theme.ThemePreference
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** How the glowing border of a design moves: not at all, one turn when it appears, or slowly round. */
enum class EffectMotion { STILL, ONCE, SLOW }

/** The visual effects a design can switch on. The drawing code lives in the launcher (see
 *  ui/theme/DesignEffects.kt); a design only chooses and tunes them. */
data class DesignEffects(
    val glowBorder: Boolean = false,
    val glowColors: List<Int> = emptyList(),
    val baseColor: Int = 0x12FFFFFF,
    val speedSeconds: Float = 4.5f,
    val motion: EffectMotion = EffectMotion.ONCE,
    val glowStrength: Float = 0.55f,
    /** Where the glowing border appears: dock, panel, search, widgets, folders, cards, buttons. */
    val targets: Set<String> = emptySet(),
    val grid: Boolean = false,
    val gridColor: Int = 0x06FFFFFF,
    val gridSpacingDp: Int = 44,
    val blobs: Boolean = false,
    val blobColors: List<Int> = emptyList(),
    val blobStrength: Float = 1f
)

/** One page's background in a design: an image (file inside the design) or a plain color. */
data class DesignBackground(val image: File? = null, val color: Int? = null, val shade: Float = 0f)

/** A design: only looks, never content. Applied on top of the user's pages, apps and folders. */
data class Design(
    val id: String,
    val name: String,
    val nameDe: String? = null,
    val author: String = "",
    val version: String = "",
    val theme: ThemePreference? = null,
    val accent: Int? = null,
    val font: FontPreference? = null,
    val cornerRadiusPercent: Int? = null,
    /** Widgets, element boxes and folder items. */
    val box: ElementAppearance? = null,
    val dock: ElementAppearance? = null,
    val panel: ElementAppearance? = null,
    /** Search bar, field, results, app list parts, … by their settings name, see [DESIGN_ELEMENTS]. */
    val elements: Map<String, ElementAppearance> = emptyMap(),
    val keyboardCapColor: Int? = null,
    /** By role: global, home, appList, search, appGrid. */
    val backgrounds: Map<String, DesignBackground> = emptyMap(),
    val lockWallpaper: File? = null,
    val effects: DesignEffects? = null,
    /** Only in the snapshot of the user's own look: every row's and folder item's exact look. */
    val rowAppearances: Map<Long, ElementAppearance>? = null,
    val folderItemAppearances: Map<Long, ElementAppearance>? = null
) {
    fun displayName(german: Boolean): String = if (german && !nameDe.isNullOrBlank()) nameDe else name
}

/** The parts of the launcher a design can style, by the name they're stored under. */
val DESIGN_ELEMENTS = listOf(
    "search_appearance", "search_bar", "search_field", "search_results", "folder_card",
    "app_list_search_button", "app_list_search_field", "search_alphabet_rail", "search_header_letter",
    "app_grid_search_bar", "widget_arrow"
)

val BACKGROUND_ROLES = listOf("global", "home", "appList", "search", "appGrid")

/** Reads and writes `design.json`. Image paths in the file are relative to the design's folder. */
object DesignJson {
    const val FORMAT = 1

    fun parse(json: JSONObject, baseDir: File): Design {
        require(json.optInt("format", -1) == FORMAT) { "Unsupported design format" }
        fun file(path: String?): File? = path?.takeIf { it.isNotBlank() }?.let { File(baseDir, it) }
        return Design(
            id = json.getString("id"),
            name = json.optString("name", json.getString("id")),
            nameDe = json.optString("name_de").ifBlank { null },
            author = json.optString("author"),
            version = json.optString("version"),
            theme = json.optString("theme").let { v -> ThemePreference.entries.firstOrNull { it.name == v } },
            accent = color(json.optString("accent")),
            font = json.optString("font").let { v -> FontPreference.entries.firstOrNull { it.name == v } },
            cornerRadiusPercent = if (json.has("cornerRadiusPercent")) json.getInt("cornerRadiusPercent").coerceIn(0, 100) else null,
            box = json.optJSONObject("box")?.let(::appearance),
            dock = json.optJSONObject("dock")?.let(::appearance),
            panel = json.optJSONObject("panel")?.let(::appearance),
            elements = json.optJSONObject("elements")?.let { obj ->
                obj.keys().asSequence().filter { it in DESIGN_ELEMENTS }.associateWith { appearance(obj.getJSONObject(it)) }
            }.orEmpty(),
            keyboardCapColor = color(json.optString("keyboardCapColor")),
            backgrounds = json.optJSONObject("backgrounds")?.let { obj ->
                obj.keys().asSequence().filter { it in BACKGROUND_ROLES }.associateWith { role ->
                    val b = obj.getJSONObject(role)
                    DesignBackground(file(b.optString("image")), color(b.optString("color")), b.optDouble("shade", 0.0).toFloat())
                }
            }.orEmpty(),
            lockWallpaper = file(json.optString("lockWallpaper")),
            effects = json.optJSONObject("effects")?.let(::effects),
            rowAppearances = json.optJSONObject("rows")?.let { obj -> obj.keys().asSequence().associate { it.toLong() to appearance(obj.getJSONObject(it)) } },
            folderItemAppearances = json.optJSONObject("folderItems")?.let { obj -> obj.keys().asSequence().associate { it.toLong() to appearance(obj.getJSONObject(it)) } }
        )
    }

    /** [imageName] maps a background/lock image to the path it gets inside the design. */
    fun write(design: Design, imageName: (File) -> String): JSONObject = JSONObject().apply {
        put("format", FORMAT)
        put("id", design.id)
        put("name", design.name)
        design.nameDe?.let { put("name_de", it) }
        put("author", design.author)
        put("version", design.version)
        design.theme?.let { put("theme", it.name) }
        design.accent?.let { put("accent", hex(it)) }
        design.font?.let { put("font", it.name) }
        design.cornerRadiusPercent?.let { put("cornerRadiusPercent", it) }
        design.box?.let { put("box", appearanceJson(it)) }
        design.dock?.let { put("dock", appearanceJson(it)) }
        design.panel?.let { put("panel", appearanceJson(it)) }
        put("elements", JSONObject().apply { design.elements.forEach { (k, v) -> put(k, appearanceJson(v)) } })
        design.keyboardCapColor?.let { put("keyboardCapColor", hex(it)) }
        put("backgrounds", JSONObject().apply {
            design.backgrounds.forEach { (role, b) ->
                put(role, JSONObject().apply {
                    b.image?.let { put("image", imageName(it)) }
                    b.color?.let { put("color", hex(it)) }
                    put("shade", b.shade.toDouble())
                })
            }
        })
        design.lockWallpaper?.let { put("lockWallpaper", imageName(it)) }
        design.effects?.let { put("effects", effectsJson(it)) }
        design.rowAppearances?.let { rows -> put("rows", JSONObject().apply { rows.forEach { (k, v) -> put(k.toString(), appearanceJson(v)) } }) }
        design.folderItemAppearances?.let { items -> put("folderItems", JSONObject().apply { items.forEach { (k, v) -> put(k.toString(), appearanceJson(v)) } }) }
    }

    fun effects(json: JSONObject) = DesignEffects(
        glowBorder = json.optBoolean("glowBorder"),
        glowColors = colors(json.optJSONArray("glowColors")),
        baseColor = color(json.optString("baseColor")) ?: 0x12FFFFFF,
        speedSeconds = json.optDouble("speedSeconds", 4.5).toFloat().coerceIn(1f, 30f),
        motion = json.optString("motion").let { v -> EffectMotion.entries.firstOrNull { it.name == v } } ?: EffectMotion.ONCE,
        glowStrength = json.optDouble("glowStrength", 0.55).toFloat().coerceIn(0f, 1f),
        targets = json.optJSONArray("targets")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }.orEmpty(),
        grid = json.optBoolean("grid"),
        gridColor = color(json.optString("gridColor")) ?: 0x06FFFFFF,
        gridSpacingDp = json.optInt("gridSpacing", 44).coerceIn(12, 200),
        blobs = json.optBoolean("blobs"),
        blobColors = colors(json.optJSONArray("blobColors")),
        blobStrength = json.optDouble("blobStrength", 1.0).toFloat().coerceIn(0f, 1f)
    )

    fun effectsJson(e: DesignEffects) = JSONObject().apply {
        put("glowBorder", e.glowBorder)
        put("glowColors", JSONArray(e.glowColors.map(::hex)))
        put("baseColor", hex(e.baseColor))
        put("speedSeconds", e.speedSeconds.toDouble())
        put("motion", e.motion.name)
        put("glowStrength", e.glowStrength.toDouble())
        put("targets", JSONArray(e.targets.toList()))
        put("grid", e.grid)
        put("gridColor", hex(e.gridColor))
        put("gridSpacing", e.gridSpacingDp)
        put("blobs", e.blobs)
        put("blobColors", JSONArray(e.blobColors.map(::hex)))
        put("blobStrength", e.blobStrength.toDouble())
    }

    private fun appearance(json: JSONObject) = ElementAppearance(
        fillMode = if (json.optString("style") == "NONE") ElementFillMode.NONE else ElementFillMode.COLOR,
        fill = ColorFill(
            mode = if (json.optString("fillMode") == "GRADIENT") FillMode.GRADIENT else FillMode.SOLID,
            color = color(json.optString("fill")) ?: android.graphics.Color.WHITE,
            secondaryColor = color(json.optString("fill2")) ?: android.graphics.Color.BLACK,
            direction = if (json.optString("direction") == "LEFT_TO_RIGHT") GradientDirection.LEFT_TO_RIGHT else GradientDirection.TOP_TO_BOTTOM
        ),
        opacity = json.optDouble("opacity", 1.0).toFloat().coerceIn(0f, 1f),
        borderEnabled = json.optBoolean("border"),
        borderColor = color(json.optString("borderColor")),
        borderWidth = json.optDouble("borderWidth", 1.0).toFloat().coerceIn(0.5f, 6f),
        contentColor = color(json.optString("contentColor"))
    )

    private fun appearanceJson(a: ElementAppearance) = JSONObject().apply {
        put("style", a.fillMode.name)
        put("fill", hex(a.fill.color))
        put("fill2", hex(a.fill.secondaryColor))
        put("fillMode", a.fill.mode.name)
        put("direction", a.fill.direction.name)
        put("opacity", a.opacity.toDouble())
        put("border", a.borderEnabled)
        a.borderColor?.let { put("borderColor", hex(it)) }
        put("borderWidth", a.borderWidth.toDouble())
        a.contentColor?.let { put("contentColor", hex(it)) }
    }

    private fun colors(array: JSONArray?): List<Int> =
        array?.let { a -> (0 until a.length()).mapNotNull { color(a.optString(it)) } }.orEmpty()

    /** `#RRGGBB` or `#AARRGGBB`. */
    fun color(value: String?): Int? = value?.takeIf { it.startsWith("#") }?.let { runCatching { android.graphics.Color.parseColor(it) }.getOrNull() }

    fun hex(color: Int): String = String.format("#%08X", color)
}
