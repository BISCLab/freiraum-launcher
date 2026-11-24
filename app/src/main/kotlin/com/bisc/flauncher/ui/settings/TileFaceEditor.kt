package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.IconPackIcon
import com.bisc.flauncher.data.model.TileFace
import com.bisc.flauncher.data.model.TileKind
import com.bisc.flauncher.data.model.TileSurface
import com.bisc.flauncher.ui.EqualGrid
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.PackPicture
import com.bisc.flauncher.ui.home.PictogramCatalog
import com.bisc.flauncher.ui.home.TileContent
import com.bisc.flauncher.ui.home.TileFaceView
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.ui.home.inkOn
import com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark

/** What a tile is shown on in the preview: the real page, or plain light or dark to check both. */
enum class TileBackdrop { PAGE, LIGHT, DARK }

/**
 * The tile as it will look, on the real page background (or plain light or dark), with the name
 * below it like in the Dock. [extra] can sit beside it, e.g. how a category's name looks.
 */
@Composable
internal fun TilePreviewHeader(
    name: String,
    tile: @Composable (around: Color) -> Unit,
    extra: (@Composable (around: Color) -> Unit)? = null,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val global by homeViewModel.globalBackground.collectAsState()
    var backdrop by remember { mutableStateOf(TileBackdrop.PAGE) }
    val pageDark = rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), global)
    val dark = when (backdrop) {
        TileBackdrop.PAGE -> pageDark
        TileBackdrop.LIGHT -> false
        TileBackdrop.DARK -> true
    }
    val around = if (dark) Color.White else Color.Black
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(24.dp))
            .then(
                when (backdrop) {
                    TileBackdrop.PAGE -> Modifier
                    TileBackdrop.LIGHT -> Modifier.background(Color(0xFFF2F2F4))
                    TileBackdrop.DARK -> Modifier.background(Color(0xFF1C1C1E))
                }
            )
    ) {
        if (backdrop == TileBackdrop.PAGE) BackgroundFill(global, Modifier.matchParentSize())
        Row(
            modifier = Modifier.align(Alignment.Center).padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(64.dp)) { tile(around) }
                Spacer(Modifier.height(6.dp))
                Text(name, color = around, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            extra?.invoke(around)
        }
        // Which background to check the tile on.
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.35f)).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(
                TileBackdrop.PAGE to R.string.tile_preview_page,
                TileBackdrop.LIGHT to R.string.tile_preview_light,
                TileBackdrop.DARK to R.string.tile_preview_dark
            ).forEach { (option, label) ->
                val picked = option == backdrop
                Text(
                    stringResource(label),
                    color = if (picked) Color.Black else Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (picked) Color.White else Color.Transparent)
                        .clickable { backdrop = option }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Sets how a folder or a category looks as a tile, one decision after the other: what it shows,
 * where the symbol comes from and which, the surface behind it, the symbol's color. Only what
 * applies to the choice so far is shown.
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
                        .clickable { onChange(sample) }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TileFaceView(sample, autoSurface, letter, modifier = Modifier.size(44.dp), around = onSurface, content = content)
                    Text(stringResource(label), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
            }
        }
        if (face.kind == TileKind.CONTENT) {
            SettingsPreference(icon = null, title = contentSummary, onClick = null, trailing = null)
        }
    }

    // 2. Which symbol, from the catalog or an icon pack.
    if (face.kind == TileKind.SYMBOL) SymbolChooser(face, onChange, viewModel)

    // 3. The surface behind it. A picture from an icon pack brings its own.
    val fromPack = face.kind == TileKind.SYMBOL && face.packIcon != null
    SettingsPreferenceGroup(title = stringResource(R.string.tile_surface_title)) {
        if (fromPack) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.tile_pack_surface),
                summary = stringResource(R.string.tile_pack_own),
                onClick = { onChange(face.copy(surface = if (face.surface == TileSurface.NONE) TileSurface.AUTO else TileSurface.NONE)) },
                trailing = { Switch(checked = face.surface != TileSurface.NONE, onCheckedChange = { on -> onChange(face.copy(surface = if (on) TileSurface.AUTO else TileSurface.NONE)) }) }
            )
        } else {
            val colored = face.surface == TileSurface.AUTO || face.surface == TileSurface.COLOR
            RadioPreference(stringResource(R.string.tile_surface_color), true, colored, { onChange(face.copy(surface = if (face.surfaceColor != null) TileSurface.COLOR else TileSurface.AUTO)) })
            RadioPreference(stringResource(R.string.tile_surface_outline), TileSurface.OUTLINE, face.surface, { onChange(face.copy(surface = it)) })
            RadioPreference(stringResource(R.string.tile_surface_none), TileSurface.NONE, face.surface, { onChange(face.copy(surface = it)) })
        }
        if (face.surface == TileSurface.AUTO || face.surface == TileSurface.COLOR) {
            AutomaticColorPicker(
                title = stringResource(R.string.tile_surface_color_pick),
                custom = face.surfaceColor.takeIf { face.surface == TileSurface.COLOR },
                automaticSeed = autoSurface.toArgb(),
                onChange = { color -> onChange(face.copy(surface = if (color == null) TileSurface.AUTO else TileSurface.COLOR, surfaceColor = color)) },
                ownGroup = false
            )
        }
    }

    // 4. The color of a pictogram or the letter; black or white unless chosen.
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
}

private const val DEFAULT_SYMBOL = "star"
private const val PACK_PAGE = 60

/** The symbol: two sources side by side, a search, and the symbols right below. */
@Composable
private fun SymbolChooser(face: TileFace, onChange: (TileFace) -> Unit, viewModel: SettingsViewModel) {
    val packs by viewModel.iconPacks.collectAsState()
    var fromPack by remember { mutableStateOf(face.packIcon != null) }
    var query by remember { mutableStateOf("") }
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    SettingsPreferenceGroup(title = stringResource(R.string.tile_symbol_title)) {
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !fromPack, onClick = { fromPack = false }, label = { Text(stringResource(R.string.tile_source_catalog)) })
                FilterChip(selected = fromPack, onClick = { fromPack = true }, enabled = packs.isNotEmpty(), label = { Text(stringResource(R.string.tile_source_pack)) })
            }
            if (packs.isEmpty()) {
                Text(stringResource(R.string.tile_no_pack), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            if (!fromPack) {
                val q = query.trim().lowercase()
                val icons = PictogramCatalog.icons.entries.filter { q.isEmpty() || q in it.key }
                if (icons.isEmpty()) Text(stringResource(R.string.tile_nothing_found), style = MaterialTheme.typography.bodySmall)
                EqualGrid(count = icons.size, columns = 6, gap = 6.dp) { index ->
                    val (key, icon) = icons[index]
                    val picked = face.symbol == key
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (picked) accent else MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable { onChange(face.copy(symbol = key, surface = if (face.surface == TileSurface.NONE && face.packIcon != null) TileSurface.AUTO else face.surface)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = key, tint = if (picked) MaterialTheme.colorScheme.onPrimary else onSurface, modifier = Modifier.size(22.dp))
                    }
                }
            } else {
                PackSymbols(face, onChange, query, packs.map { it.packageName to it.label }, viewModel)
            }
        }
    }
}

/** The pictures of one installed icon pack, searchable, a page at a time. */
@Composable
private fun PackSymbols(face: TileFace, onChange: (TileFace) -> Unit, query: String, packs: List<Pair<String, String>>, viewModel: SettingsViewModel) {
    var pack by remember { mutableStateOf(face.packIcon?.first ?: packs.firstOrNull()?.first) }
    var icons by remember(pack) { mutableStateOf<List<IconPackIcon>?>(null) }
    var shown by remember(pack, query) { mutableIntStateOf(PACK_PAGE) }
    LaunchedEffect(pack) { icons = pack?.let { viewModel.availableIconPackIcons(it) } }
    val accent = MaterialTheme.colorScheme.primary

    if (packs.size > 1) {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            packs.forEach { (pkg, label) -> FilterChip(selected = pkg == pack, onClick = { pack = pkg }, label = { Text(label) }) }
        }
    }
    val all = icons
    val currentPack = pack
    when {
        all == null || currentPack == null -> Text(stringResource(R.string.settings_folder_icon_pack_loading), style = MaterialTheme.typography.bodySmall)
        else -> {
            val q = query.trim().lowercase()
            val matches = if (q.isEmpty()) all else all.filter { q in it.searchText }
            if (matches.isEmpty()) Text(stringResource(R.string.tile_nothing_found), style = MaterialTheme.typography.bodySmall)
            val page = matches.take(shown)
            EqualGrid(count = page.size, columns = 5, gap = 6.dp) { index ->
                val icon = page[index]
                val picked = face.packIcon == (currentPack to icon.drawable)
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(if (picked) 2.dp else 0.dp, if (picked) accent else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable {
                            // A picture brings its own background: none behind it unless asked for.
                            onChange(face.copy(symbol = TileFace.packSymbol(currentPack, icon.drawable), surface = TileSurface.NONE))
                        },
                    contentAlignment = Alignment.Center
                ) { PackPicture(currentPack to icon.drawable, 44.dp, icon.drawable) }
            }
            if (matches.size > shown) {
                TextButton(onClick = { shown += PACK_PAGE }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.tile_more, matches.size - shown))
                }
            }
        }
    }
}
