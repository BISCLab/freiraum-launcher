package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark

/** Corner radii of the miniature, scaled down from the real ones (search bar 28dp, cards 28dp). */
private val MINI_BAR_RADIUS = 14.dp
private val MINI_CARD_RADIUS = 10.dp
private val MINI_SIDE_MARGIN = 4.dp
private val MINI_CAP_HEIGHT = 8.dp

/** A miniature of the search screen drawn from the live settings: page background and opacity,
 *  search bar, two result cards and the keyboard (with its extension, if on). Each part is
 *  tappable and opens its own settings via [onNavigate] (the same routes as the list below it). */
@Composable
fun SearchPreview(onNavigate: (String) -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val pageAppearance by viewModel.searchAppearance.collectAsState()
    val override by viewModel.searchBackgroundOverride.collectAsState()
    val globalBackground by viewModel.globalBackground.collectAsState()
    val globalShade by viewModel.searchGlobalShade.collectAsState()
    val barOwn by viewModel.searchBarAppearance.collectAsState()
    val barCorner by viewModel.searchBarCornerRadiusPercent.collectAsState()
    val defaultCorner by viewModel.defaultCornerRadiusPercent.collectAsState()
    val resultsOwn by viewModel.searchResultsAppearance.collectAsState()
    val resultsCorner by viewModel.searchResultsCornerRadiusPercent.collectAsState()
    val resultsEdgeToEdge by viewModel.searchResultsEdgeToEdge.collectAsState()
    val capEnabled by viewModel.searchKeyboardCapEnabled.collectAsState()
    val capColor by viewModel.searchKeyboardCapColor.collectAsState()
    val capCorner by viewModel.searchKeyboardCapCornerRadiusPercent.collectAsState()
    val barAtBottom by viewModel.searchBarAtBottom.collectAsState()
    val barShowCategories by viewModel.searchBarShowCategories.collectAsState()
    val barFloating by viewModel.searchBarFloating.collectAsState()

    // Same resolution rules as the real screen (HomeViewModel.searchBackground, SearchOverlay).
    val background = override ?: globalBackground.copy(shade = globalShade ?: globalBackground.shade)
    val surfaceDark = rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), background)
    val defaultElement = com.bisc.flauncher.ui.theme.defaultBoxAppearanceFor(MaterialTheme.colorScheme.background.luminance() < 0.5f)
    val barAppearance = barOwn ?: defaultElement
    val cardAppearance = resultsOwn ?: defaultElement
    val barRadius = MINI_BAR_RADIUS * ((barCorner ?: defaultCorner).coerceIn(0, 100) / 100f)
    val cardRadius = MINI_CARD_RADIUS * ((resultsCorner ?: 100).coerceIn(0, 100) / 100f)
    val sideMargin = if (resultsEdgeToEdge) 0.dp else MINI_SIDE_MARGIN
    val phoneShape = RoundedCornerShape(24.dp)
    val appDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(0.5f)
                .clip(phoneShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(3.dp, MaterialTheme.colorScheme.outline, phoneShape)
        ) {
            // Page: its background; a tap anywhere no other part covers opens it.
            BackgroundFill(background, Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().clickable { onNavigate("page") })

            Column(modifier = Modifier.fillMaxSize()) {
                if (!barAtBottom) {
                    MiniSearchBar(barAppearance, surfaceDark, RoundedCornerShape(bottomStart = barRadius, bottomEnd = barRadius), barShowCategories, atBottom = false, floating = barFloating) {
                        onNavigate(SearchElement.BAR.route)
                    }
                }
                Column(
                    modifier = Modifier.padding(start = sideMargin, end = sideMargin, top = if (barAtBottom) 14.dp else 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    MiniResultCard(cardAppearance, surfaceDark, RoundedCornerShape(cardRadius), rows = 3) {
                        onNavigate(SearchElement.RESULTS.route)
                    }
                    MiniResultCard(cardAppearance, surfaceDark, RoundedCornerShape(cardRadius), rows = 2) {
                        onNavigate(SearchElement.RESULTS.route)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (barAtBottom) {
                    MiniSearchBar(barAppearance, surfaceDark, RoundedCornerShape(topStart = barRadius, topEnd = barRadius), barShowCategories, atBottom = true, floating = barFloating) {
                        onNavigate(SearchElement.BAR.route)
                    }
                }
                MiniKeyboard(
                    capColor = if (capEnabled) Color(capColor) else null,
                    capRadius = MINI_CAP_HEIGHT * (capCorner.coerceIn(0, 100) / 100f),
                    dark = appDark
                ) { onNavigate("keyboard") }
            }
        }
    }
}

@Composable
private fun MiniSearchBar(appearance: ElementAppearance, surfaceDark: Boolean, shape: Shape, showCategories: Boolean, atBottom: Boolean, floating: Boolean, onClick: () -> Unit) {
    // Floating: no box, the field sits on the page itself.
    val colors = resolveElementColors(if (floating) appearance.copy(fillMode = ElementFillMode.NONE) else appearance, surfaceDark)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.background, alpha = colors.backgroundAlpha)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 8.dp, top = if (atBottom) 8.dp else 14.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val field = @Composable {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(colors.contentColor.copy(alpha = 0.12f))
                    .border(0.5.dp, colors.contentColor.copy(alpha = 0.25f), CircleShape)
            )
        }
        val categories = @Composable {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                repeat(8) { Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(colors.contentColor.copy(alpha = 0.45f))) }
            }
        }
        if (atBottom) {
            if (showCategories) categories()
            field()
        } else {
            field()
            if (showCategories) categories()
        }
    }
}

@Composable
private fun MiniResultCard(appearance: ElementAppearance, surfaceDark: Boolean, shape: Shape, rows: Int, onClick: () -> Unit) {
    val colors = resolveElementColors(appearance, surfaceDark)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.background, alpha = colors.backgroundAlpha)
            .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth / 2, colors.borderColor, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(rows) { row ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(colors.contentColor.copy(alpha = 0.5f)))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (row % 2 == 0) 0.7f else 0.45f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.contentColor.copy(alpha = 0.3f))
                )
            }
        }
    }
}

/** The system keyboard: a neutral stand-in, since its real colors belong to the keyboard app,
 *  with the extension strip on top when [capColor] is set. */
@Composable
private fun MiniKeyboard(capColor: Color?, capRadius: Dp, dark: Boolean, onClick: () -> Unit) {
    val keyboardColor = if (dark) Color(0xFF2B2D31) else Color(0xFFDDE1E8)
    val keyColor = if (dark) Color(0xFF45484E) else Color.White
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        if (capColor != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MINI_CAP_HEIGHT)
                    .clip(RoundedCornerShape(topStart = capRadius, topEnd = capRadius))
                    .background(capColor)
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().background(keyboardColor).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(10, 9, 7).forEach { keys ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(keys) { Box(modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(2.dp)).background(keyColor)) }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(10.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(2.dp))
                    .background(keyColor)
            )
        }
    }
}
