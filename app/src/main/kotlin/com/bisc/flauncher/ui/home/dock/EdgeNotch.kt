package com.bisc.flauncher.ui.home.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.ui.home.LocalSurfaceIsDark

/** Which screen edge an [EdgeNotch] sits flush against. */
enum class NotchEdge { TOP, BOTTOM }

/**
 * The small, always-visible bump a collapsed dock/Favorites panel shows at its resting edge —
 * [NotchEdge.BOTTOM] for the dock, [NotchEdge.TOP] for the Favorites popup. This is the one and
 * only way to reopen a collapsed panel by hand, via either a direct tap or a short drag toward the
 * screen's center (up for a bottom notch, down for a top one). Deliberately simple threshold-based
 * drag detection (same pattern as [com.bisc.flauncher.ui.home.detectHomeGestures]), not a live
 * finger-following reveal — the panel's own expand/collapse animation still plays in full once the
 * threshold is crossed. Neither panel's own visibility is driven by the 4 rebindable HomeGestures
 * any more — this notch is the sole handle.
 *
 * Deliberately the *opposite* of the current page surface (light page -> dark notch, dark page ->
 * light notch) rather than reading the dock/Favorites row's own configured appearance — a panel can
 * legitimately be styled to match the page (Favorites' own default does exactly that), which would
 * leave a same-colored notch invisible against it. The notch's only job is to stay discoverable, so
 * it always contrasts with the page behind it instead.
 *
 * A full-width tail (only the central bump itself stays narrow) fills the system inset behind it —
 * the gesture nav bar for a bottom notch, the status bar/camera cutout for a top one — so the
 * collapsed panel reads as "the rest of this bar is just past the edge" instead of the bump
 * floating a visible gap above a sliver of the page's own background.
 */
@Composable
fun EdgeNotch(edge: NotchEdge, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val surfaceDark = LocalSurfaceIsDark.current ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)
    val background = (if (surfaceDark) Color.White else Color.Black).copy(alpha = 0.85f)
    val iconTint = if (surfaceDark) Color.Black else Color.White
    val bumpShape = when (edge) {
        NotchEdge.BOTTOM -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
        NotchEdge.TOP -> RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
    }
    val tailHeight: Dp = when (edge) {
        NotchEdge.BOTTOM -> WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        NotchEdge.TOP -> maxOf(
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
        )
    }
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 20.dp.toPx() } }
    val currentOnOpen = rememberUpdatedState(onOpen)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(edge, thresholdPx) {
                awaitEachGesture {
                    var accumulated = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        if (change.isConsumed) continue
                        accumulated += change.positionChange().y
                        val crossed = when (edge) {
                            NotchEdge.BOTTOM -> accumulated <= -thresholdPx
                            NotchEdge.TOP -> accumulated >= thresholdPx
                        }
                        if (crossed) {
                            currentOnOpen.value()
                            break
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { currentOnOpen.value() })
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (edge == NotchEdge.TOP && tailHeight > 0.dp) {
            Box(Modifier.fillMaxWidth().height(tailHeight).background(background))
        }
        Box(
            modifier = Modifier.size(width = 56.dp, height = 22.dp).clip(bumpShape).background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (edge == NotchEdge.BOTTOM) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = iconTint
            )
        }
        if (edge == NotchEdge.BOTTOM && tailHeight > 0.dp) {
            Box(Modifier.fillMaxWidth().height(tailHeight).background(background))
        }
    }
}
