package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One entry inside a [Folder]. */
@Entity(
    tableName = "folder_items",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folderId")]
)
data class FolderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val type: GridItemType,
    val payload: String,
    val orderIndex: Int = 0,
    val spanY: Int = 1,
    // Same flattened ElementAppearance payload PageRow carries.
    val elementFillMode: ElementFillMode = ElementFillMode.COLOR,
    val fillColor: Int = android.graphics.Color.WHITE,
    val fillSecondaryColor: Int = android.graphics.Color.BLACK,
    val fillMode: FillMode = FillMode.SOLID,
    val fillDirection: GradientDirection = GradientDirection.TOP_TO_BOTTOM,
    val elementOpacity: Float = 1f,
    val borderEnabled: Boolean = false,
    // 0 = sharp 90° corners, 100 = fully rounded - see PageRow.cornerRadiusPercent.
    val cornerRadiusPercent: Int = com.bisc.flauncher.ui.theme.DEFAULT_CORNER_RADIUS_PERCENT,
    /** See PageRow.pagingButtons. */
    val pagingButtons: Boolean = false,
    val elementBorderColor: Int? = null,
    val elementBorderWidth: Float = 1f,
    val elementContentColor: Int? = null,
    // The clock widget's own settings, the same as PageRow's.
    val clockStyle: String = ClockStyle.DIGITAL,
    val clockDateActionType: String = GestureActionType.NONE.name,
    val clockDateActionPayload: String? = null,
    val clockTimeActionType: String = GestureActionType.NONE.name,
    val clockTimeActionPayload: String? = null,
    val clockTimeFormat: String = ClockTimeFormat.SYSTEM,
    val clockDateFormat: String = "",
    val clockShowNextAlarm: Boolean = false,
    val clockAlignment: String = ClockAlignment.CENTER
)

/** A folder widget as a page row, so the one widget editor and widget renderer serve both. */
fun FolderItem.asWidgetRow(): PageRow = PageRow(
    id = id,
    pageId = -1L,
    orderIndex = 0,
    type = if (type == GridItemType.SYSTEM_WIDGET) PageRowType.SYSTEM_WIDGET else PageRowType.WIDGET,
    payload = payload,
    spanY = spanY,
    cornerRadiusPercent = cornerRadiusPercent,
    pagingButtons = pagingButtons,
    clockStyle = clockStyle,
    clockDateActionType = clockDateActionType,
    clockDateActionPayload = clockDateActionPayload,
    clockTimeActionType = clockTimeActionType,
    clockTimeActionPayload = clockTimeActionPayload,
    clockTimeFormat = clockTimeFormat,
    clockDateFormat = clockDateFormat,
    clockShowNextAlarm = clockShowNextAlarm,
    clockAlignment = clockAlignment
).withAppearance(appearance())

/** Takes back what was edited on [row] (see [asWidgetRow]). */
fun FolderItem.withWidgetRow(row: PageRow): FolderItem = copy(
    spanY = row.spanY,
    cornerRadiusPercent = row.cornerRadiusPercent,
    pagingButtons = row.pagingButtons,
    clockStyle = row.clockStyle,
    clockDateActionType = row.clockDateActionType,
    clockDateActionPayload = row.clockDateActionPayload,
    clockTimeActionType = row.clockTimeActionType,
    clockTimeActionPayload = row.clockTimeActionPayload,
    clockTimeFormat = row.clockTimeFormat,
    clockDateFormat = row.clockDateFormat,
    clockShowNextAlarm = row.clockShowNextAlarm,
    clockAlignment = row.clockAlignment
).withAppearance(row.appearance())

/** Assembles the flattened style columns into an [ElementAppearance]. */
fun FolderItem.appearance(): ElementAppearance = ElementAppearance(
    fillMode = elementFillMode,
    fill = ColorFill(mode = fillMode, color = fillColor, secondaryColor = fillSecondaryColor, direction = fillDirection),
    opacity = elementOpacity,
    borderEnabled = borderEnabled,
    borderColor = elementBorderColor,
    borderWidth = elementBorderWidth,
    contentColor = elementContentColor
)

/** Writes an [ElementAppearance] back into the flattened style columns. */
fun FolderItem.withAppearance(appearance: ElementAppearance): FolderItem = copy(
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
