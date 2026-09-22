package com.bisc.flauncher.data.db

import androidx.room.TypeConverter
import com.bisc.flauncher.data.model.ChecklistItem
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.FillMode
import com.bisc.flauncher.data.model.FolderIconMode
import com.bisc.flauncher.data.model.GradientDirection
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.NoteType
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromGridItemType(type: GridItemType): String = type.name

    @TypeConverter
    fun toGridItemType(value: String): GridItemType = GridItemType.valueOf(value)

    @TypeConverter
    fun fromElementFillMode(mode: ElementFillMode): String = mode.name

    @TypeConverter
    fun toElementFillMode(value: String): ElementFillMode =
        runCatching { ElementFillMode.valueOf(value) }.getOrDefault(ElementFillMode.COLOR)

    @TypeConverter
    fun fromFolderIconMode(mode: FolderIconMode): String = mode.name

    @TypeConverter
    fun toFolderIconMode(value: String): FolderIconMode = FolderIconMode.valueOf(value)

    @TypeConverter
    fun fromFillMode(mode: FillMode): String = mode.name

    @TypeConverter
    fun toFillMode(value: String): FillMode = runCatching { FillMode.valueOf(value) }.getOrDefault(FillMode.SOLID)

    @TypeConverter
    fun fromGradientDirection(direction: GradientDirection): String = direction.name

    @TypeConverter
    fun toGradientDirection(value: String): GradientDirection =
        runCatching { GradientDirection.valueOf(value) }.getOrDefault(GradientDirection.TOP_TO_BOTTOM)

    @TypeConverter
    fun fromLauncherPageKind(kind: LauncherPageKind): String = kind.name

    @TypeConverter
    fun toLauncherPageKind(value: String): LauncherPageKind =
        runCatching { LauncherPageKind.valueOf(value) }.getOrDefault(LauncherPageKind.ROWS)

    @TypeConverter
    fun fromNoteType(type: NoteType): String = type.name

    @TypeConverter
    fun toNoteType(value: String): NoteType = runCatching { NoteType.valueOf(value) }.getOrDefault(NoteType.FREITEXT)

    @TypeConverter
    fun fromChecklistItems(items: List<ChecklistItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply { put("text", item.text); put("checked", item.checked) })
        }
        return array.toString()
    }

    @TypeConverter
    fun toChecklistItems(value: String): List<ChecklistItem> {
        if (value.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ChecklistItem(text = obj.optString("text"), checked = obj.optBoolean("checked"))
            }
        }.getOrDefault(emptyList())
    }
}
