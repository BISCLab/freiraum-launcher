package com.bisc.flauncher.ui.home

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** Below this speed (px/s) a release settles on the nearer page, above it on the one swiped toward. */
private const val FLING_THRESHOLD = 400f

/**
 * For a page whose widgets swipe sideways themselves (tabs, notes): at their last tab the rest of
 * the drag moves the home pages, but on release the pages were left standing between two. Here the
 * pages are brought to rest on one, the nearer or the one swiped toward.
 */
fun Modifier.settleHomePager(pagerState: PagerState): Modifier = composed {
    val connection = remember(pagerState) {
        object : NestedScrollConnection {
            // A drag that started in a widget and spilled over to the pages.
            var moved = false

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && pagerState.currentPageOffsetFraction != 0f) moved = true
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!moved) return Velocity.Zero
                moved = false
                if (pagerState.currentPageOffsetFraction == 0f) return Velocity.Zero
                val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
                val target = when {
                    available.x < -FLING_THRESHOLD -> ceil(position).toInt()
                    available.x > FLING_THRESHOLD -> floor(position).toInt()
                    else -> position.roundToInt()
                }.coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
                pagerState.animateScrollToPage(target)
                return available
            }
        }
    }
    nestedScroll(connection)
}
