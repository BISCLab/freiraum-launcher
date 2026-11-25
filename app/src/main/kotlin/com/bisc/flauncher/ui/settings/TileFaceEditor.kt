package com.bisc.flauncher.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.IconPackIcon
import com.bisc.flauncher.data.model.TileFace
import com.bisc.flauncher.data.model.TileKind
import com.bisc.flauncher.data.model.TileSurface
import com.bisc.flauncher.ui.EqualGrid
import com.bisc.flauncher.ui.home.PackPicture
import com.bisc.flauncher.ui.home.PictogramCatalog
import com.bisc.flauncher.ui.home.TileContent
import com.bisc.flauncher.ui.home.TileFaceView
import com.bisc.flauncher.ui.home.inkOn

/**
 * The tile as it will look, on light or on dark (it can lie in many places, so both are checked),
 * with its name below it like in the Dock. It lies above the page like a card and stays in sight; while the page
 * below is scrolled it steps back to a smaller size. [extra] can sit beside the tile, e.g. how a
 * category's name looks.
 */
@Composable
internal fun TilePreviewHeader(
    name: String,
    tile: @Composable (around: Color) -> Unit,
    extra: (@Composable (around: Color) -> Unit)? = null,
    /** Small words over the tile and over [extra], the same as the sections below that set them. */
    tileCaption: String? = null,
    extraCaption: String? = null
) {
    // Starts like the settings themselves are shown, light or dark.
    val settingsDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var dark by remember { mutableStateOf(settingsDark) }
    val around = if (dark) Color.White else Color.Black
    val compact = LocalHeaderCompact.current
    val tileSize by animateDpAsState(if (compact) 48.dp else 68.dp, label = "previewTile")
    val shape = RoundedCornerShape(28.dp)
    val twoParts = extra != null
    // In three lines, each the same for both halves: the words, the parts, the light/dark switch.
    // Scrolled, only the parts stay, small.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape)
            .clip(shape)
            .background(if (dark) Color(0xFF1C1C1E) else Color(0xFFF2F2F4))
            .animateContentSize()
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!compact && (tileCaption != null || extraCaption != null)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { tileCaption?.let { PreviewCaption(it, around) } }
                if (twoParts) {
                    Spacer(Modifier.width(1.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { extraCaption?.let { PreviewCaption(it, around) } }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(tileSize)) { tile(around) }
                    if (!compact) {
                        Spacer(Modifier.height(6.dp))
                        Text(name, color = around, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (extra != null) {
                // A fine line between the halves, as tall as the tile.
                Box(modifier = Modifier.width(1.dp).height(tileSize).background(around.copy(alpha = 0.2f)))
                // The name level with the middle of the tile.
                Box(modifier = Modifier.weight(1f).height(tileSize), contentAlignment = Alignment.Center) { extra(around) }
            }
        }
        // Which background to check the tile on.
        if (!compact) Row(
            modifier = Modifier.clip(RoundedCornerShape(50)).background(around.copy(alpha = 0.12f)).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(false to R.string.tile_preview_light, true to R.string.tile_preview_dark).forEach { (option, label) ->
                val picked = option == dark
                Text(
                    stringResource(label),
                    color = if (picked) (if (dark) Color.Black else Color.White) else around.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (picked) around else Color.Transparent)
                        .clickable { dark = option }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/** A small word over a part of the preview, naming the section that sets it. */
@Composable
private fun PreviewCaption(text: String, around: Color) {
    Text(
        text.uppercase(),
        color = around.copy(alpha = 0.75f),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(around.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** The start of one part of a page: a symbol, its name, and where what it sets is seen. */
@Composable
internal fun SectionBanner(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, where: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AreaSymbol(icon, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(where, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Sets how a folder or a category looks as a tile, one decision after the other: what it shows,
 * the symbol (chosen on a page of its own), the surface behind it, the colors of the symbol and of
 * the name below. Only what applies to the choice so far is shown.
 */
@Composable
internal fun TileFaceEditor(
    face: TileFace,
    onChange: (TileFace) -> Unit,
    autoSurface: Color,
    letter: String,
    content: List<TileContent>,
    contentSummary: String,
    viewModel: SettingsViewModel
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    var choosingSymbol by remember { mutableStateOf(false) }

    // 1. What the tile shows.
    SettingsPreferenceGroup(title = stringResource(R.string.tile_kind_title)) {
        val kinds = listOf(TileKind.CONTENT to R.string.tile_kind_content, TileKind.SYMBOL to R.string.tile_kind_symbol, TileKind.LETTER to R.string.tile_kind_letter)
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp)) {
            EqualGrid(count = kinds.size, columns = 3) { index ->
                val (kind, label) = kinds[index]
                val picked = face.kind == kind
                val sample = face.copy(kind = kind, symbol = if (kind == TileKind.SYMBOL) face.symbol ?: DEFAULT_SYMBOL else face.symbol)
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(if (picked) 2.dp else 0.dp, if (picked) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(18.dp))
                        .clickable {
                            onChange(sample)
                            // A symbol is the next decision: go straight to choosing it the first time.
                            if (kind == TileKind.SYMBOL && face.kind != TileKind.SYMBOL && face.symbol == null) choosingSymbol = true
                        }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TileFaceView(sample, autoSurface, letter, modifier = Modifier.size(44.dp), around = onSurface, content = content)
                    Text(stringResource(label), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
            }
        }
        when (face.kind) {
            TileKind.CONTENT -> SettingsPreference(icon = null, title = contentSummary, onClick = null, trailing = null)
            // 2. Which symbol: one line here, the choosing on a page of its own.
            TileKind.SYMBOL -> {
                val packs by viewModel.iconPacks.collectAsState()
                val pack = face.packIcon
                SettingsPreference(
                    icon = null,
                    leading = {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                            if (pack != null) PackPicture(pack, 30.dp)
                            else Icon(PictogramCatalog.iconFor(face.symbol) ?: Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(22.dp))
                        }
                    },
                    title = stringResource(R.string.tile_symbol_choose),
                    summary = if (pack != null) stringResource(R.string.tile_symbol_from_pack, packs.find { it.packageName == pack.first }?.label ?: pack.first)
                    else stringResource(R.string.tile_symbol_from_catalog),
                    onClick = { choosingSymbol = true }
                )
            }
            TileKind.LETTER -> Unit
        }
    }

    // 3. Fill, border and corners, each on its own; their colors only show once switched on.
    val fromPack = face.kind == TileKind.SYMBOL && face.packIcon != null
    SettingsPreferenceGroup(title = stringResource(R.string.tile_surface_title), info = if (fromPack) stringResource(R.string.tile_pack_own) else null) {
        // Every change writes the border too, so tiles from before keep theirs.
        val setFill = { on: Boolean -> onChange(face.copy(surface = if (on) (if (face.surfaceColor != null) TileSurface.COLOR else TileSurface.AUTO) else TileSurface.NONE, border = face.hasBorder)) }
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.tile_fill),
            onClick = { setFill(!face.hasFill) },
            trailing = { Switch(checked = face.hasFill, onCheckedChange = { setFill(it) }) }
        )
        if (face.hasFill) {
            AutomaticColorPicker(
                title = stringResource(R.string.tile_surface_color_pick),
                custom = face.surfaceColor.takeIf { face.surface == TileSurface.COLOR },
                automaticSeed = autoSurface.toArgb(),
                onChange = { color -> onChange(face.copy(surface = if (color == null) TileSurface.AUTO else TileSurface.COLOR, surfaceColor = color, border = face.hasBorder)) },
                ownGroup = false
            )
        }
        val setBorder = { on: Boolean ->
            onChange(face.copy(border = on, surface = if (face.surface == TileSurface.OUTLINE) TileSurface.NONE else face.surface))
        }
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.tile_border),
            onClick = { setBorder(!face.hasBorder) },
            trailing = { Switch(checked = face.hasBorder, onCheckedChange = { setBorder(it) }) }
        )
        if (face.hasBorder) {
            AutomaticColorPicker(
                title = stringResource(R.string.tile_border_color),
                custom = face.borderColor,
                automaticSeed = onSurface.copy(alpha = 0.6f).toArgb(),
                onChange = { onChange(face.copy(borderColor = it, border = true)) },
                ownGroup = false
            )
        }
        CornerChoice(face, onChange, autoSurface, letter, content)
    }

    // 4. The color of a pictogram or the letter: black or white unless chosen.
    if ((face.kind == TileKind.SYMBOL && !fromPack) || face.kind == TileKind.LETTER) {
        SettingsPreferenceGroup {
            AutomaticColorPicker(
                title = stringResource(if (face.kind == TileKind.LETTER) R.string.tile_letter_color else R.string.tile_symbol_color),
                custom = face.symbolColor,
                automaticSeed = face.copy(symbolColor = null).inkOn(autoSurface, onSurface).toArgb(),
                onChange = { onChange(face.copy(symbolColor = it)) },
                ownGroup = false
            )
        }
    }

    if (choosingSymbol) {
        SymbolPickerDialog(face = face, onPick = { onChange(it); choosingSymbol = false }, onDismiss = { choosingSymbol = false }, viewModel = viewModel)
    }
}

private const val DEFAULT_SYMBOL = "star"

/** The tile's corners as four small tiles, the first one "like the others around". */
@Composable
private fun CornerChoice(face: TileFace, onChange: (TileFace) -> Unit, autoSurface: Color, letter: String, content: List<TileContent>) {
    val options = listOf(
        null to R.string.tile_corners_default,
        com.bisc.flauncher.data.model.TileCorners.ROUND to R.string.welcome_shape_round,
        com.bisc.flauncher.data.model.TileCorners.SOFT to R.string.welcome_shape_soft,
        com.bisc.flauncher.data.model.TileCorners.SQUARE to R.string.welcome_shape_square
    )
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.tile_corners), style = MaterialTheme.typography.bodyLarge)
        EqualGrid(count = options.size, columns = 4, gap = 8.dp) { index ->
            val (corners, label) = options[index]
            val picked = face.corners == corners
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(if (picked) 2.dp else 0.dp, if (picked) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable { onChange(face.copy(corners = corners, border = face.hasBorder)) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TileFaceView(face.copy(corners = corners), autoSurface, letter, modifier = Modifier.size(32.dp), around = MaterialTheme.colorScheme.onSurface, content = content, shape = RoundedCornerShape(10.dp))
                Text(stringResource(label), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

/** Chooses the symbol on a page of its own: the catalog or an installed icon pack, a search, and
 *  every symbol in a grid that fills the page. */
@Composable
private fun SymbolPickerDialog(face: TileFace, onPick: (TileFace) -> Unit, onDismiss: () -> Unit, viewModel: SettingsViewModel) {
    val packs by viewModel.iconPacks.collectAsState()
    var fromPack by remember { mutableStateOf(face.packIcon != null) }
    var pack by remember { mutableStateOf(face.packIcon?.first ?: packs.firstOrNull()?.packageName) }
    var query by remember { mutableStateOf("") }
    var packIcons by remember(pack) { mutableStateOf<List<IconPackIcon>?>(null) }
    LaunchedEffect(pack, fromPack) { if (fromPack) packIcons = pack?.let { viewModel.availableIconPackIcons(it) } }
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    SettingsFixedDialog(title = stringResource(R.string.tile_symbol_choose), onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !fromPack, onClick = { fromPack = false }, label = { Text(stringResource(R.string.tile_source_catalog)) })
                FilterChip(selected = fromPack, onClick = { fromPack = true }, enabled = packs.isNotEmpty(), label = { Text(stringResource(R.string.tile_source_pack)) })
            }
            if (packs.isEmpty()) {
                Text(stringResource(R.string.tile_no_pack), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (fromPack && packs.size > 1) {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    packs.forEach { info -> FilterChip(selected = info.packageName == pack, onClick = { pack = info.packageName }, label = { Text(info.label) }) }
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.tile_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = SettingsTextFieldShape,
                modifier = Modifier.fillMaxWidth()
            )
        }
        val q = query.trim().lowercase()
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) {
            if (!fromPack) {
                val icons = PictogramCatalog.icons.entries.filter { q.isEmpty() || q in it.key }
                if (icons.isEmpty()) Text(stringResource(R.string.tile_nothing_found), style = MaterialTheme.typography.bodyMedium)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(60.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(icons, key = { it.key }) { (key, icon) ->
                        val picked = face.symbol == key
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (picked) accent else MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable {
                                    // Back from a picture with its own background: a surface again.
                                    onPick(face.copy(kind = TileKind.SYMBOL, symbol = key, surface = if (face.packIcon != null && face.surface == TileSurface.NONE) TileSurface.AUTO else face.surface))
                                },
                            contentAlignment = Alignment.Center
                        ) { Icon(icon, contentDescription = key, tint = if (picked) MaterialTheme.colorScheme.onPrimary else onSurface, modifier = Modifier.size(26.dp)) }
                    }
                }
            } else {
                val all = packIcons
                val currentPack = pack
                when {
                    all == null || currentPack == null -> Text(stringResource(R.string.settings_folder_icon_pack_loading), style = MaterialTheme.typography.bodyMedium)
                    else -> {
                        val matches = if (q.isEmpty()) all else all.filter { q in it.searchText }
                        if (matches.isEmpty()) Text(stringResource(R.string.tile_nothing_found), style = MaterialTheme.typography.bodyMedium)
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(64.dp),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(matches, key = { it.drawable }) { icon ->
                                val picked = face.packIcon == (currentPack to icon.drawable)
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(if (picked) 2.dp else 0.dp, if (picked) accent else Color.Transparent, RoundedCornerShape(14.dp))
                                        // A picture brings its own background: none behind it unless asked for.
                                        .clickable { onPick(face.copy(kind = TileKind.SYMBOL, symbol = TileFace.packSymbol(currentPack, icon.drawable), surface = TileSurface.NONE, border = false)) },
                                    contentAlignment = Alignment.Center
                                ) { PackPicture(currentPack to icon.drawable, 48.dp, icon.drawable) }
                            }
                        }
                    }
                }
            }
        }
    }
}
