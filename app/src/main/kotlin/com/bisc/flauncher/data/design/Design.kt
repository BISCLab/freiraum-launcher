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
    /** Soft light from the glowing border into the surface itself, 0 (off) to 1. */
    val surfaceGlow: Float = 0f,
    /** Where the glowing border appears: dock, panel, search, widgets, folders, cards, buttons, rail. */
    val targets: Set<String> = emptySet(),
    val grid: Boolean = false,
    val gridColor: Int = 0x06FFFFFF,
    val gridSpacingDp: Int = 44,
    val blobs: Boolean = false,
    val blobColors: List<Int> = emptyList(),
    val blobStrength: Float = 1f,
    /** Relief: surfaces stand out from the page with a soft shadow below, a light edge on top and
     *  a slight sheen, 0 (off) to 1. Still, so it costs nothing while nothing moves. */
    val depth: Float = 0f,
    /** Where the relief appears; the same names as [targets]. */
    val depthTargets: Set<String> = emptySet()
)

/** A design: only looks, never content and never pictures (those come with wallpaper packs).
 *  Applied on top of the user's pages, apps and folders. */
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
    /** Widgets' own roundness (they're only slightly rounded by default, see DEFAULT_WIDGET_CORNER_RADIUS_PERCENT). */
    val widgetCornerRadiusPercent: Int? = null,
    /** Element boxes and folder items; widgets too unless [widget] is set. */
    val box: ElementAppearance? = null,
    /** Widgets on pages and in folders; null = like [box]. */
    val widget: ElementAppearance? = null,
    /** Corners of single parts, see [DESIGN_CORNERS]; parts left out keep the general ones. */
    val corners: Map<String, Int> = emptyMap(),
    val dock: ElementAppearance? = null,
    val panel: ElementAppearance? = null,
    /** Search bar, field, results, app list parts, … by their settings name, see [DESIGN_ELEMENTS]. */
    val elements: Map<String, ElementAppearance> = emptyMap(),
    val keyboardCapColor: Int? = null,
    val effects: DesignEffects? = null,
    /** Only in the snapshot of the user's own look: every row's and folder item's exact look. */
    val rowAppearances: Map<Long, ElementAppearance>? = null,
    val folderItemAppearances: Map<Long, ElementAppearance>? = null,
    val rowCorners: Map<Long, Int>? = null,
    val folderItemCorners: Map<Long, Int>? = null,
    /** Comes with the app; can't be removed. */
    val bundled: Boolean = false
) {
    fun displayName(german: Boolean): String = if (german && !nameDe.isNullOrBlank()) nameDe else name
}

/** The parts whose corners a design can set one by one. */
val DESIGN_CORNERS = listOf(
    "dock", "panel", "element_boxes", "folders", "widgets", "search_button", "alphabet_rail",
    "category_rail", "search_field", "search_results"
)

/** The parts of the launcher a design can style, by the name they're stored under. */
val DESIGN_ELEMENTS = listOf(
    "search_appearance", "search_field", "search_category_rail", "search_results", "folder_card",
    "app_list_search_button", "app_list_search_field", "search_alphabet_rail", "search_header_letter",
    "app_grid_search_bar", "widget_arrow", "search_rail_bubble"
)

/** Reads and writes `design.json`. Older files may still name backgrounds and pictures; those are
 *  ignored. */
object DesignJson {
    const val FORMAT = 1

    fun parse(json: JSONObject, bundled: Boolean = false): Design {
        require(json.optInt("format", -1) == FORMAT) { "Unsupported design format" }
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
            widgetCornerRadiusPercent = if (json.has("widgetCornerRadiusPercent")) json.getInt("widgetCornerRadiusPercent").coerceIn(0, 100) else null,
            box = json.optJSONObject("box")?.let(::appearance),
            widget = json.optJSONObject("widget")?.let(::appearance),
            corners = json.optJSONObject("corners")?.let { obj ->
                obj.keys().asSequence().filter { it in DESIGN_CORNERS }.associateWith { obj.getInt(it).coerceIn(0, 100) }
            }.orEmpty(),
            dock = json.optJSONObject("dock")?.let(::appearance),
            panel = json.optJSONObject("panel")?.let(::appearance),
            elements = json.optJSONObject("elements")?.let { obj ->
                obj.keys().asSequence().filter { it in DESIGN_ELEMENTS }.associateWith { appearance(obj.getJSONObject(it)) }
            }.orEmpty(),
            keyboardCapColor = color(json.optString("keyboardCapColor")),
            effects = json.optJSONObject("effects")?.let(::effects),
            rowAppearances = json.optJSONObject("rows")?.let { obj -> obj.keys().asSequence().associate { it.toLong() to appearance(obj.getJSONObject(it)) } },
            folderItemAppearances = json.optJSONObject("folderItems")?.let { obj -> obj.keys().asSequence().associate { it.toLong() to appearance(obj.getJSONObject(it)) } },
            rowCorners = json.optJSONObject("rowCorners")?.let { obj -> obj.keys().asSequence().associate { it.toLong() to obj.getInt(it) } },
            folderItemCorners = json.optJSONObject("folderItemCorners")?.let { obj -> obj.keys().asSequence().associate { it.toLong() to obj.getInt(it) } },
            bundled = bundled
        )
    }

    fun write(design: Design): JSONObject = JSONObject().apply {
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
        design.widgetCornerRadiusPercent?.let { put("widgetCornerRadiusPercent", it) }
        design.box?.let { put("box", appearanceJson(it)) }
        design.widget?.let { put("widget", appearanceJson(it)) }
        if (design.corners.isNotEmpty()) put("corners", JSONObject().apply { design.corners.forEach { (k, v) -> put(k, v) } })
        design.dock?.let { put("dock", appearanceJson(it)) }
        design.panel?.let { put("panel", appearanceJson(it)) }
        put("elements", JSONObject().apply { design.elements.forEach { (k, v) -> put(k, appearanceJson(v)) } })
        design.keyboardCapColor?.let { put("keyboardCapColor", hex(it)) }
        design.effects?.let { put("effects", effectsJson(it)) }
        design.rowAppearances?.let { rows -> put("rows", JSONObject().apply { rows.forEach { (k, v) -> put(k.toString(), appearanceJson(v)) } }) }
        design.folderItemAppearances?.let { items -> put("folderItems", JSONObject().apply { items.forEach { (k, v) -> put(k.toString(), appearanceJson(v)) } }) }
        design.rowCorners?.let { rows -> put("rowCorners", JSONObject().apply { rows.forEach { (k, v) -> put(k.toString(), v) } }) }
        design.folderItemCorners?.let { items -> put("folderItemCorners", JSONObject().apply { items.forEach { (k, v) -> put(k.toString(), v) } }) }
    }

    fun effects(json: JSONObject) = DesignEffects(
        glowBorder = json.optBoolean("glowBorder"),
        glowColors = colors(json.optJSONArray("glowColors")),
        baseColor = color(json.optString("baseColor")) ?: 0x12FFFFFF,
        speedSeconds = json.optDouble("speedSeconds", 4.5).toFloat().coerceIn(1f, 30f),
        motion = json.optString("motion").let { v -> EffectMotion.entries.firstOrNull { it.name == v } } ?: EffectMotion.ONCE,
        glowStrength = json.optDouble("glowStrength", 0.55).toFloat().coerceIn(0f, 1f),
        surfaceGlow = json.optDouble("surfaceGlow", 0.0).toFloat().coerceIn(0f, 1f),
        targets = json.optJSONArray("targets")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }.orEmpty(),
        grid = json.optBoolean("grid"),
        gridColor = color(json.optString("gridColor")) ?: 0x06FFFFFF,
        gridSpacingDp = json.optInt("gridSpacing", 44).coerceIn(12, 200),
        blobs = json.optBoolean("blobs"),
        blobColors = colors(json.optJSONArray("blobColors")),
        blobStrength = json.optDouble("blobStrength", 1.0).toFloat().coerceIn(0f, 1f),
        depth = json.optDouble("depth", 0.0).toFloat().coerceIn(0f, 1f),
        depthTargets = json.optJSONArray("depthTargets")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }.orEmpty()
    )

    fun effectsJson(e: DesignEffects) = JSONObject().apply {
        put("glowBorder", e.glowBorder)
        put("glowColors", JSONArray(e.glowColors.map(::hex)))
        put("baseColor", hex(e.baseColor))
        put("speedSeconds", e.speedSeconds.toDouble())
        put("motion", e.motion.name)
        put("glowStrength", e.glowStrength.toDouble())
        put("surfaceGlow", e.surfaceGlow.toDouble())
        put("targets", JSONArray(e.targets.toList()))
        put("grid", e.grid)
        put("gridColor", hex(e.gridColor))
        put("gridSpacing", e.gridSpacingDp)
        put("blobs", e.blobs)
        put("blobColors", JSONArray(e.blobColors.map(::hex)))
        put("blobStrength", e.blobStrength.toDouble())
        put("depth", e.depth.toDouble())
        put("depthTargets", JSONArray(e.depthTargets.toList()))
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
