package com.etozhesandy.redpanda.features.chat.presentation.chat

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import com.etozhesandy.redpanda.features.chat.model.MediaScrollPosition

/** Creates a pager that resumes at the tab stored in [slot] and writes selection changes back. */
@Composable
fun rememberCachedPagerState(
    slot: ChatTabSlot,
    pageCount: () -> Int,
    initialPage: Int = slot.readTab(),
): PagerState {
    val pagerState = rememberSaveable(saver = pagerStateSaver(pageCount)) {
        PagerState(currentPage = initialPage.coerceIn(0, pageCount() - 1), pageCount = pageCount)
    }
    LaunchedEffect(pagerState, slot) {
        snapshotFlow { pagerState.currentPage }.collect(slot::writeTab)
    }
    return pagerState
}

private fun pagerStateSaver(pageCount: () -> Int): Saver<PagerState, Any> = listSaver(
    save = { listOf(it.currentPage, it.currentPageOffsetFraction) },
    restore = { saved ->
        PagerState(
            currentPage = saved[0] as Int,
            currentPageOffsetFraction = saved[1] as Float,
            pageCount = pageCount,
        )
    },
)

/** Creates a messages-list state that resumes at the position stored in [slot]. */
@Composable
fun rememberCachedMessagesListState(
    slot: ChatTabSlot,
    restoreCachedPosition: Boolean = true,
): LazyListState {
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        val restored = if (restoreCachedPosition) slot.readMessagesPosition() else MediaScrollPosition()
        LazyListState(restored.index, restored.offset)
    }
    LaunchedEffect(listState, slot) {
        snapshotFlow {
            MediaScrollPosition(
                index = listState.firstVisibleItemIndex,
                offset = listState.firstVisibleItemScrollOffset,
            )
        }.collect(slot::writeMessagesPosition)
    }
    return listState
}
