package com.bisc.flauncher.ui.home

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.bisc.flauncher.data.model.TileFace
import com.bisc.flauncher.data.model.TileKind
import com.bisc.flauncher.data.model.TileSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads the pictures tiles show: app icons for what a tile holds, pictures from icon packs. */
object TileIcons {
    var loadAppIcon: (ComponentName) -> ImageBitmap? = { null }
    var loadPackIcon: (String, String) -> ImageBitmap? = { _, _ -> null }
}

/** The color a tile's surface really has, or null when it has none (an outline or nothing). */
fun TileFace.surfaceFill(auto: Color): Color? = when (surface) {
    TileSurface.AUTO -> auto
    TileSurface.COLOR -> surfaceColor?.let { Color(it) } ?: auto
    TileSurface.OUTLINE, TileSurface.NONE -> null
}

/** The symbol's color: its own, else black or white on the surface, else the text around it. */
fun TileFace.inkOn(auto: Color, around: Color): Color =
    symbolColor?.let { Color(it) } ?: surfaceFill(auto)?.let { inkOn(it) } ?: around

/**
 * A folder or a category as a tile, the one way both are drawn: what it holds in small
 * ([content], two by two), its symbol (a pictogram or a picture from an icon pack) or its first
 * letter, on its surface. Sizes follow the tile, so it works from a settings badge to the Dock.
 */
@Composable
fun TileFaceView(
    face: TileFace,
    autoSurface: Color,
    letter: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    /** The color around the tile, for the outline and a symbol without a surface. */
    around: Color = Color.White,
    /** What the tile holds, at most four; empty shows the letter instead. */
    content: List<TileContent> = emptyList(),
    contentDescription: String? = null,
    /** The symbol when none is set, e.g. a globe for a link. */
    defaultSymbol: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val fill = face.surfaceFill(autoSurface)
    val tint = face.inkOn(autoSurface, around)
    val tileShape = face.corners?.let { cornerShape(it) } ?: shape
    BoxWithConstraints(
        modifier = modifier
            .clip(tileShape)
            .then(if (fill != null) Modifier.background(fill) else Modifier)
            .then(if (face.hasBorder) Modifier.border((face.borderWidth ?: 1.5f).dp, face.borderColor?.let { Color(it) } ?: around.copy(alpha = 0.6f), tileShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val side = min(maxWidth, maxHeight)
        val pack = face.packIcon
        when {
            face.kind == TileKind.SYMBOL && pack != null ->
                // A picture from an icon pack brings its own look; alone it fills the tile like an app icon.
                PackPicture(pack, if (fill == null && !face.hasBorder) side else side * 0.7f, contentDescription)
            face.kind == TileKind.SYMBOL ->
                Icon(PictogramCatalog.iconFor(face.symbol) ?: defaultSymbol ?: Icons.AutoMirrored.Filled.Label, contentDescription, tint = tint, modifier = Modifier.size(side * 0.54f))
            face.kind == TileKind.CONTENT && content.isNotEmpty() ->
                TileContentGrid(content.take(4), cell = side * 0.36f, gap = side * 0.06f, tint = tint)
            else -> Text(
                letter.trim().take(1).uppercase(),
                color = tint,
                fontSize = (side.value * 0.42f).sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** The shape of a tile's own corners. */
fun cornerShape(corners: com.bisc.flauncher.data.model.TileCorners): Shape = when (corners) {
    com.bisc.flauncher.data.model.TileCorners.ROUND -> RoundedCornerShape(50)
    com.bisc.flauncher.data.model.TileCorners.SOFT -> RoundedCornerShape(28)
    com.bisc.flauncher.data.model.TileCorners.SQUARE -> RoundedCornerShape(8)
}

/** One thing a tile holds: an app (its icon) or another element (a small symbol). */
sealed interface TileContent {
    data class App(val component: ComponentName?) : TileContent
    data class Element(val icon: androidx.compose.ui.graphics.vector.ImageVector) : TileContent
}

@Composable
private fun TileContentGrid(items: List<TileContent>, cell: Dp, gap: Dp, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        items.chunked(2).forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                line.forEach { item ->
                    Box(modifier = Modifier.size(cell), contentAlignment = Alignment.Center) {
                        when (item) {
                            is TileContent.App -> rememberAppIcon(item.component, TileIcons.loadAppIcon)?.let {
                                Image(it, contentDescription = null, modifier = Modifier.size(cell))
                            }
                            is TileContent.Element -> Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(cell * 0.8f))
                        }
                    }
                }
                if (line.size < 2) Spacer(Modifier.size(cell))
            }
        }
    }
}

/** A picture from an icon pack, loaded off the main thread. */
@Composable
fun PackPicture(pack: Pair<String, String>, size: Dp, contentDescription: String? = null) {
    val bitmap by produceState<ImageBitmap?>(null, pack) {
        value = withContext(Dispatchers.Default) { runCatching { TileIcons.loadPackIcon(pack.first, pack.second) }.getOrNull() }
    }
    val image = bitmap
    if (image != null) Image(image, contentDescription = contentDescription, modifier = Modifier.size(size))
    else Spacer(Modifier.size(size))
}
