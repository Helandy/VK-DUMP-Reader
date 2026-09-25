package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.etozhesandy.redpanda.core.designsystem.components.ImmersiveScreen

/**
 * The frame every full-screen media viewer shares: a black immersive scaffold over a swipeable
 * pager, with one zoom state following whichever page is on screen.
 *
 * [ZoomState] is handed to [page] rather than applied here: only a still image should zoom, while
 * a video player or a placeholder should not, and the frame knows nothing about [T].
 */
@Composable
fun <T> MediaPagerScreen(
    items: List<T>,
    startIndex: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    actions: @Composable RowScope.(current: T?) -> Unit = {},
    page: @Composable (item: T, isCurrentPage: Boolean, zoom: ZoomState) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = startIndex) { items.size }

    // `startIndex` resolves asynchronously after the media loads, but `rememberPagerState` only
    // reads `initialPage` on first composition (page 0, before that load completes) — so the pager
    // jumps to the tapped item once the list arrives. Only once: the list is a database stream
    // that re-emits, and re-running the jump then yanked the viewer back to the tapped photo in the
    // middle of a swipe.
    var hasJumpedToStart by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(items.isNotEmpty()) {
        if (items.isNotEmpty() && !hasJumpedToStart) {
            pagerState.scrollToPage(startIndex)
            hasJumpedToStart = true
        }
    }

    val zoom = rememberZoomState()
    // Leaving a page abandons its magnification: the next one opens fit-to-screen, and the pager
    // gets its horizontal drag back.
    LaunchedEffect(pagerState.currentPage) { zoom.reset() }

    ImmersiveScreen(
        onBack = onBack,
        modifier = modifier,
        actions = { actions(items.getOrNull(pagerState.currentPage)) },
    ) {
        if (items.isEmpty()) return@ImmersiveScreen
        // While the image is magnified a horizontal drag pans it instead of turning the page;
        // a double tap returns to 1x and hands swiping back.
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoom.isZoomed,
            // The neighbours load ahead, so a swipe lands on a ready image rather than on the
            // black frame while it is still being fetched.
            beyondViewportPageCount = 1,
            key = key?.let { itemKey -> { index -> itemKey(items[index]) } },
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                page(items[index], index == pagerState.currentPage, zoom)
            }
        }
    }
}
