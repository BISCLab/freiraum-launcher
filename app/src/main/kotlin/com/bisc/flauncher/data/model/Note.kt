package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NoteType { FREITEXT, CHECKLISTE }

data class ChecklistItem(
    val text: String = "",
    val checked: Boolean = false
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val type: NoteType = NoteType.FREITEXT,
    val items: List<ChecklistItem> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0,
    // Only affects the Notes widget's own grid; the note stays visible everywhere else (quick
    // overview, search, …) — this is a display filter, not a delete.
    val hiddenInWidget: Boolean = false
)

/** Splits note text into checklist items, one per non-blank line. */
fun noteContentToChecklistItems(content: String): List<ChecklistItem> =
    content.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { ChecklistItem(text = it) }
        .ifEmpty { listOf(ChecklistItem()) }

fun checklistItemsToNoteContent(items: List<ChecklistItem>): String = items.joinToString("\n") { it.text }
