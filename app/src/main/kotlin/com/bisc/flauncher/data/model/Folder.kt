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
    // When false, this folder's card uses the shared global folder appearance (see
    // SettingsRepository.folderCardAppearance) and the fields below are unused. Same flattened
    // ElementAppearance payload PageRow/FolderItem carry, used only once this is turned on.
    val appearanceOverrideEnabled: Boolean = false,
    val elementFillMode: ElementFillMode = ElementFillMode.COLOR,
    val fillColor: Int = android.graphics.Color.WHITE,
    val fillSecondaryColor: Int = android.graphics.Color.BLACK,
    val fillMode: FillMode = FillMode.SOLID,
    val fillDirection: GradientDirection = GradientDirection.TOP_TO_BOTTOM,
    val elementOpacity: Float = 1f,
    val borderEnabled: Boolean = false,
    val elementBorderColor: Int? = null,
    val elementBorderWidth: Float = 1f,
    val elementContentColor: Int? = null
)

/** Assembles this folder's flattened override columns into an [ElementAppearance], regardless of
 *  whether the override is actually enabled — callers check [Folder.appearanceOverrideEnabled]
 *  themselves to decide whether to use this or the global folder appearance. */
fun Folder.appearanceOverride(): ElementAppearance = ElementAppearance(
    fillMode = elementFillMode,
    fill = ColorFill(mode = fillMode, color = fillColor, secondaryColor = fillSecondaryColor, direction = fillDirection),
    opacity = elementOpacity,
    borderEnabled = borderEnabled,
    borderColor = elementBorderColor,
    borderWidth = elementBorderWidth,
    contentColor = elementContentColor
)

/** Writes an [ElementAppearance] back into the flattened override columns. */
fun Folder.withAppearanceOverride(appearance: ElementAppearance): Folder = copy(
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
