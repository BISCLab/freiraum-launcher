package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconMode: FolderIconMode = FolderIconMode.AUTO_STACK,
    val iconColor: String? = null,
    val iconPackPackage: String? = null,
    val iconPackDrawable: String? = null,
    // When false, this folder's card content (fill/opacity/text color) uses the shared global folder
    // appearance (see SettingsRepository.folderCardAppearance) and the content fields below are
    // unused. Independent of [borderOverrideEnabled] - content and border can each be reset to the
    // global default on their own.
    val appearanceOverrideEnabled: Boolean = false,
    val elementFillMode: ElementFillMode = ElementFillMode.COLOR,
    val fillColor: Int = android.graphics.Color.WHITE,
    val fillSecondaryColor: Int = android.graphics.Color.BLACK,
    val fillMode: FillMode = FillMode.SOLID,
    val fillDirection: GradientDirection = GradientDirection.TOP_TO_BOTTOM,
    val elementOpacity: Float = 1f,
    val elementContentColor: Int? = null,
    // Independent override flag for just the border columns below - same override-vs-global idea as
    // [appearanceOverrideEnabled], but covers only borderEnabled/elementBorderColor/elementBorderWidth
    // so Appearance and Border can each be reset to the shared global default on their own.
    val borderOverrideEnabled: Boolean = false,
    val borderEnabled: Boolean = false,
    val elementBorderColor: Int? = null,
    val elementBorderWidth: Float = 1f,
    // This folder's own grid shape - one column count per sub-row, comma-separated (e.g. "4,3"),
    // same idea as [PageRow.dockRowColumns]/[PageRow.panelRowColumns]. See [Folder.rows]/[Folder.withRows].
    val rowColumns: String = "4",
    // Same override-vs-global pattern as [appearanceOverrideEnabled] - when false, this folder uses
    // the shared global default (SettingsRepository.folderShowLabels) instead of [showLabels].
    val showLabelsOverrideEnabled: Boolean = false,
    val showLabels: Boolean = false
)

/** Most elements one folder sub-row holds - as many as fit across the folder view. */
const val MAX_FOLDER_ROW_COLUMNS = 5

/** Parses [Folder.rowColumns] into one column count per sub-row; always at least one row. */
fun Folder.rows(): List<Int> =
    rowColumns.split(",").mapNotNull { it.trim().toIntOrNull()?.coerceIn(1, MAX_FOLDER_ROW_COLUMNS) }.ifEmpty { listOf(4) }

fun Folder.withRows(rows: List<Int>): Folder =
    copy(rowColumns = rows.ifEmpty { listOf(4) }.joinToString(",") { it.coerceIn(1, MAX_FOLDER_ROW_COLUMNS).toString() })

/** Content half (fill/opacity/text color) of this folder's appearance override, independent of
 *  [borderAppearance]. Assembled regardless of whether [Folder.appearanceOverrideEnabled] is
 *  actually on - callers check that themselves. The returned border fields are unused placeholders. */
fun Folder.contentAppearance(): ElementAppearance = ElementAppearance(
    fillMode = elementFillMode,
    fill = ColorFill(mode = fillMode, color = fillColor, secondaryColor = fillSecondaryColor, direction = fillDirection),
    opacity = elementOpacity,
    contentColor = elementContentColor
)

/** Writes an [ElementAppearance]'s content fields back into this folder's override columns; its
 *  border fields are ignored - see [withBorderAppearance] for those. */
fun Folder.withContentAppearance(appearance: ElementAppearance): Folder = copy(
    elementFillMode = appearance.fillMode,
    fillColor = appearance.fill.color,
    fillSecondaryColor = appearance.fill.secondaryColor,
    fillMode = appearance.fill.mode,
    fillDirection = appearance.fill.direction,
    elementOpacity = appearance.opacity,
    elementContentColor = appearance.contentColor
)

/** Border half (on/off, width, color) of this folder's appearance override, independent of
 *  [contentAppearance]. Assembled regardless of whether [Folder.borderOverrideEnabled] is actually
 *  on. The returned fill/opacity/contentColor fields are unused placeholders. */
fun Folder.borderAppearance(): ElementAppearance = ElementAppearance(
    borderEnabled = borderEnabled,
    borderColor = elementBorderColor,
    borderWidth = elementBorderWidth
)

/** Writes an [ElementAppearance]'s border fields back into this folder's override columns; its
 *  content fields are ignored - see [withContentAppearance] for those. */
fun Folder.withBorderAppearance(appearance: ElementAppearance): Folder = copy(
    borderEnabled = appearance.borderEnabled,
    elementBorderColor = appearance.borderColor,
    elementBorderWidth = appearance.borderWidth
)

/** This folder's live-rendering appearance: content and border resolved independently against the
 *  shared global default, since [Folder.appearanceOverrideEnabled] and [Folder.borderOverrideEnabled]
 *  can each be on independently. */
fun Folder.effectiveAppearance(globalAppearance: ElementAppearance): ElementAppearance {
    val content = if (appearanceOverrideEnabled) contentAppearance() else globalAppearance
    val border = if (borderOverrideEnabled) borderAppearance() else globalAppearance
    return content.copy(borderEnabled = border.borderEnabled, borderColor = border.borderColor, borderWidth = border.borderWidth)
}
