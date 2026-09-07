package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    actions: @Composable RowScope.(current: T?) -> Unit = {},
    page: @Composable (item: T, isCurrentPage: Boolean, zoom: ZoomState) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = startIndex) { items.size }

    // `startIndex` resolves asynchronously after the media loads, but `rememberPagerState` only
    // reads `initialPage` on first composition (page 0, before that load completes) — re-key the
    // jump on the loaded list so the pager actually lands on the tapped item.
    LaunchedEffect(items, startIndex) {
        if (items.isNotEmpty()) pagerState.scrollToPage(startIndex)
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
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                page(items[index], index == pagerState.currentPage, zoom)
            }
        }
    }
}
