package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.RowEntry
import com.bisc.flauncher.ui.home.dock.DockPanelSurface

/**
 * The stage of a live editor (Element-Box, Dock, Panel): the element drawn in its own look and
 * corners on a soft backdrop, like the live folder. [content] is the element's entries.
 */
@Composable
internal fun RowLiveStage(appearance: ElementAppearance, cornerPercent: Int, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHighest)))
            .padding(horizontal = 12.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        DockPanelSurface(
            appearance = appearance,
            shape = com.bisc.flauncher.ui.theme.PartCornerShape(cornerPercent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

/** Which entry of a live editor is picked; forgets it when the entry is gone. */
@Composable
internal fun rememberPickedEntry(entries: List<RowEntry>): Pair<RowEntry?, (RowEntry?) -> Unit> {
    var pickedId by remember { mutableStateOf<Long?>(null) }
    val picked = entries.firstOrNull { it.id == pickedId }
    LaunchedEffect(entries, pickedId) { if (pickedId != null && picked == null) pickedId = null }
    return picked to { entry: RowEntry? -> pickedId = if (entry?.id == pickedId) null else entry?.id }
}

/** What can be done with the picked entry, right under the stage: move it, open its own settings,
 *  remove it. Same bar as in the live folder. */
@Composable
internal fun RowEntrySelectionBar(
    title: String,
    canMoveBack: Boolean,
    canMoveOn: Boolean,
    onMove: (Int) -> Unit,
    onOpenSettings: (() -> Unit)?,
    onRemove: () -> Unit,
    onClose: () -> Unit
) {
    FolderSelectionBar(
        title = title,
        isWidget = false,
        span = 1,
        canGrow = false,
        canShrink = false,
        onSpan = {},
        canMoveBack = canMoveBack,
        canMoveOn = canMoveOn,
        onMove = onMove,
        onOpenSettings = onOpenSettings,
        onRemove = onRemove,
        onClose = onClose
    )
}
