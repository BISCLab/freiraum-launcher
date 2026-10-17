package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One icon inside a [PageRowType.ELEMENT_BOX] row, in [orderIndex] order. */
@Entity(
    tableName = "row_entries",
    foreignKeys = [
        ForeignKey(
            entity = PageRow::class,
            parentColumns = ["id"],
            childColumns = ["pageRowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pageRowId")]
)
data class RowEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageRowId: Long,
    val orderIndex: Int,
    val type: GridItemType,
    val payload: String
)
