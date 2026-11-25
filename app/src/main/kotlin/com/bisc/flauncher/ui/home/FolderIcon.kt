package com.bisc.flauncher.ui.home

import android.content.ComponentName
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.TileFace
import com.bisc.flauncher.data.model.face
import com.bisc.flauncher.data.model.Folder as FolderModel

/** A folder's automatic surface: the accent, like a category without its own color. */
@Composable
fun folderAutoSurface(): Color = androidx.compose.material3.MaterialTheme.colorScheme.primary

/** A folder as a tile, as it is set under "As a tile": its first apps, a symbol or its letter. */
@Composable
fun FolderIcon(
    folder: FolderModel?,
    previewItems: List<FolderItem>,
    modifier: Modifier = Modifier,
    /** A face being edited and not saved yet; null shows the folder's own. */
    face: TileFace? = null,
    around: Color = Color.White
) {
    TileFaceView(
        face = face ?: folder?.face() ?: TileFace(),
        autoSurface = folderAutoSurface(),
        letter = folder?.name.orEmpty(),
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        around = around,
        content = previewItems.filter { it.type == GridItemType.APP }.take(4).map { TileContent.App(ComponentName.unflattenFromString(it.payload)) }
    )
}
