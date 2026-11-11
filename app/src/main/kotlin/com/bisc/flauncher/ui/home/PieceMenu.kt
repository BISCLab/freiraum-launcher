package com.bisc.flauncher.ui.home

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.theme.designGlow
import kotlin.math.roundToInt

private val PIECE_MENU_GAP = 3.dp
private val PIECE_MENU_MARGIN = 12.dp

/** The look of the launcher's menus (Appearance → Menus, or a design); null = the automatic one. */
val LocalMenuAppearance = compositionLocalOf<ElementAppearance?> { null }

/** Corners of the menus, on the one scale every part uses. */
const val DEFAULT_MENU_CORNER = 85
val LocalMenuCornerPercent = compositionLocalOf { DEFAULT_MENU_CORNER }

/**
 * The launcher's popup menu: the screen behind it dims and blurs, and the entries are separate
 * pieces with the same fine gap between each, the first and last rounded off, like a group in the
 * settings. It opens below the part it belongs to, or above when there is no room below.
 */
@Composable
fun PieceMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    // Where the part it belongs to sits on the screen.
    var anchor by remember { mutableStateOf(Rect.Zero) }
    Layout(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            coordinates.parentLayoutCoordinates?.let { anchor = it.boundsInWindow() }
        },
        measurePolicy = { _, _ -> layout(0, 0) {} }
    )
    if (!expanded) return
    val menuShape = com.bisc.flauncher.ui.theme.PartCornerShape(LocalMenuCornerPercent.current)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                window.setDimAmount(0.45f)
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.apply { blurBehindRadius = 60 }
                }
                window.setWindowAnimations(0)
            }
        }
        val appear = remember { Animatable(0f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, tween(160)) }
        val margin = with(LocalDensity.current) { PIECE_MENU_MARGIN.roundToPx() }
        Layout(
            content = {
                Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismissRequest))
                Column(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = appear.value
                            val scale = 0.92f + 0.08f * appear.value
                            scaleX = scale
                            scaleY = scale
                        }
                        .designGlow(menuShape, "menus")
                        .width(IntrinsicSize.Max)
                        .widthIn(min = 220.dp)
                        .clip(menuShape),
                    verticalArrangement = Arrangement.spacedBy(PIECE_MENU_GAP),
                    content = content
                )
            }
        ) { measurables, constraints ->
            val scrim = measurables[0].measure(constraints)
            val menu = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = constraints.maxWidth - margin * 2))
            layout(constraints.maxWidth, constraints.maxHeight) {
                scrim.place(0, 0)
                val below = anchor.bottom.roundToInt() + margin / 2
                val y = if (below + menu.height <= constraints.maxHeight - margin) below
                    else (anchor.top.roundToInt() - margin / 2 - menu.height).coerceAtLeast(margin)
                val x = anchor.left.roundToInt().coerceIn(margin, (constraints.maxWidth - margin - menu.width).coerceAtLeast(margin))
                menu.place(x, y)
            }
        }
    }
}

/** One entry of a [PieceMenu]. */
@Composable
fun PieceMenuItem(text: @Composable () -> Unit, onClick: () -> Unit, leadingIcon: (@Composable () -> Unit)? = null) {
    val own = LocalMenuAppearance.current
    val colors = own?.let { resolveElementColors(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (colors != null) Modifier.background(colors.background, alpha = colors.backgroundAlpha.coerceAtLeast(0.85f))
                else Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
            .clickable(onClick = onClick)
            .heightIn(min = 54.dp)
            .padding(start = 18.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val content = colors?.contentColor ?: MaterialTheme.colorScheme.onSurface
        if (leadingIcon != null) {
            Box(modifier = Modifier.padding(end = 16.dp).size(24.dp), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalContentColor provides (colors?.contentColor?.copy(alpha = 0.8f) ?: MaterialTheme.colorScheme.onSurfaceVariant), content = leadingIcon)
            }
        } else {
            Spacer(Modifier.width(0.dp))
        }
        CompositionLocalProvider(LocalContentColor provides content) {
            ProvideTextStyle(MaterialTheme.typography.bodyLarge, content = text)
        }
    }
}
