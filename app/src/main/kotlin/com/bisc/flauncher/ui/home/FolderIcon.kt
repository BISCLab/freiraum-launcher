package com.bisc.flauncher.ui.home

import android.content.ComponentName
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.Folder as FolderModel
import com.bisc.flauncher.data.model.FolderIconMode
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.GridItemType

private val DefaultFolderColor = Color(0xFF4F378B)
private val AutoStackColor = Color(0xFF8E8E93)

/** Folder symbol: preview grid of the first apps, a flat color, or an icon-pack icon. */
@Composable
fun FolderIcon(
    folder: FolderModel?,
    previewItems: List<FolderItem>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    modifier: Modifier = Modifier
) {
    val isCustomColor = folder?.iconMode == FolderIconMode.CUSTOM_COLOR
    val packIconBitmap = if (folder?.iconMode == FolderIconMode.ICON_PACK) {
        val pack = folder.iconPackPackage
        val drawable = folder.iconPackDrawable
        if (pack != null && drawable != null) loadPackIcon(pack, drawable) else null
    } else null

    val backgroundColor = if (isCustomColor) {
        folder?.iconColor?.let { runCatching { Color(AndroidColor.parseColor(it)) }.getOrNull() } ?: DefaultFolderColor
    } else {
        AutoStackColor
    }

    val appItems = if (isCustomColor || packIconBitmap != null) {
        emptyList()
    } else {
        previewItems.filter { it.type == GridItemType.APP }.take(4)
    }

    Box(
        modifier = if (packIconBitmap != null) modifier else modifier.background(backgroundColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (packIconBitmap != null) {
            Image(bitmap = packIconBitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else if (appItems.isEmpty()) {
            Icon(imageVector = Icons.Filled.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        } else {
            Column(modifier = Modifier.padding(4.dp)) {
                appItems.chunked(2).forEach { rowItems ->
                    Row {
                        rowItems.forEach { folderItem ->
                            val componentName = ComponentName.unflattenFromString(folderItem.payload)
                            val bitmap = rememberAppIcon(componentName, loadIcon)
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp).padding(1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
