package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One full-width row on a [LauncherPage], stacked top-to-bottom by [orderIndex]. */
@Entity(
    tableName = "page_rows",
    foreignKeys = [
        ForeignKey(
            entity = LauncherPage::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pageId")]
)
data class PageRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageId: Long,
    val orderIndex: Int,
    val type: PageRowType,
    val payload: String = "",
    val spanY: Int = 1,
    // Exact pixel height, used only for SYSTEM_WIDGET rows once resized with the fine (per-digit) size
    // editor; null means "not set yet", falling back to spanY * the shared row unit height as usual.
    val heightPx: Int? = null,
    val elementFillMode: ElementFillMode = ElementFillMode.COLOR,
    val fillColor: Int = android.graphics.Color.WHITE,
    val fillSecondaryColor: Int = android.graphics.Color.BLACK,
    val fillMode: FillMode = FillMode.SOLID,
    val fillDirection: GradientDirection = GradientDirection.TOP_TO_BOTTOM,
    val elementOpacity: Float = 1f,
    val borderEnabled: Boolean = false,
    val cornerStyle: WidgetCornerStyle = WidgetCornerStyle.ROUNDED,
    val edgeToEdge: Boolean = false,
    val columns: Int = 5,
    val clockStyle: String = ClockStyle.DIGITAL,
    val clockDateActionType: String = GestureActionType.NONE.name,
    val clockDateActionPayload: String? = null,
    val clockTimeActionType: String = GestureActionType.NONE.name,
    val clockTimeActionPayload: String? = null,
    val clockTimeFormat: String = ClockTimeFormat.SYSTEM,
    val clockDateFormat: String = "",
    val clockShowNextAlarm: Boolean = false,
    val clockAlignment: String = ClockAlignment.CENTER,
    val elementBorderColor: Int? = null,
    val elementBorderWidth: Float = 1f,
    val elementContentColor: Int? = null,
    // Single-line/pill layout, opt-in on any row (not just the dock's own SEARCH_BAR row) — entries
    // laid out in one non-wrapping line instead of AppsRowGrid's usual column-wrapped grid, sized per
    // pillWidthMode (only meaningful while pillMode is true) and capped to pillMaxEntries.
    val pillMode: Boolean = false,
    val pillWidthMode: DockWidthMode = DockWidthMode.FIXED,
    val pillMaxEntries: Int = 5,
    // Only meaningful for the pinned dock row (type == SEARCH_BAR) — see HomeScreen's floating dock
    // Box for the expand/collapse rendering these drive.
    val dockVisibilityMode: DockVisibilityMode = DockVisibilityMode.ALWAYS_VISIBLE,
    val dockToggleGestureType: String? = null,
    val dockShowDragHandle: Boolean = false
)

/** Reads this row's stored dock-toggle gesture, if any — only meaningful with [PageRow.dockVisibilityMode] == [DockVisibilityMode.GESTURE_TOGGLE]. */
fun PageRow.dockToggleGesture(): HomeGesture? = dockToggleGestureType?.let { runCatching { HomeGesture.valueOf(it) }.getOrNull() }

/** Reads the stored date-tap action of a clock row as a [GestureAction]. */
fun PageRow.clockDateAction(): GestureAction =
    GestureAction(runCatching { GestureActionType.valueOf(clockDateActionType) }.getOrDefault(GestureActionType.NONE), clockDateActionPayload)

fun PageRow.clockTimeAction(): GestureAction =
    GestureAction(runCatching { GestureActionType.valueOf(clockTimeActionType) }.getOrDefault(GestureActionType.NONE), clockTimeActionPayload)

/** Assembles this row's flattened style columns back into one [ElementAppearance]. */
fun PageRow.appearance(): ElementAppearance = ElementAppearance(
    fillMode = elementFillMode,
    fill = ColorFill(mode = fillMode, color = fillColor, secondaryColor = fillSecondaryColor, direction = fillDirection),
    opacity = elementOpacity,
    borderEnabled = borderEnabled,
    borderColor = elementBorderColor,
    borderWidth = elementBorderWidth,
    contentColor = elementContentColor
)

/** Writes an [ElementAppearance] back into the flattened style columns. */
fun PageRow.withAppearance(appearance: ElementAppearance): PageRow = copy(
    elementFillMode = appearance.fillMode,
    fillColor = appearance.fill.color,
    fillSecondaryColor = appearance.fill.secondaryColor,
    fillMode = appearance.fill.mode,
    fillDirection = appearance.fill.direction,
    elementOpacity = appearance.opacity,
    borderEnabled = appearance.borderEnabled,
    elementBorderColor = appearance.borderColor,
    elementBorderWidth = appearance.borderWidth,
    elementContentColor = appearance.contentColor
)
