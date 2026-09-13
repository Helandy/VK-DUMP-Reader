package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.designsystem.components.EmptyState
import com.etozhesandy.redpanda.core.designsystem.components.MEDIA_SORT_OPTIONS
import com.etozhesandy.redpanda.core.designsystem.components.SortMenu
import com.etozhesandy.redpanda.core.designsystem.media.MediaGrid
import com.etozhesandy.redpanda.features.chat.presentation.chat.MediaScrollSlot
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.TabActionsRow
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.TabActionsHeight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** The grid shared by «Фото» and «Видео» — [emptyText] is all that differs between them. */
@Composable
fun MediaTabScreen(
    state: MediaTabState.State,
    effect: Flow<MediaTabState.Effect>,
    scrollSlot: MediaScrollSlot,
    emptyText: String,
    onEvent: (MediaTabState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberCachedGridState(scrollSlot)
    LaunchedEffect(Unit) {
        effect.collectLatest { current ->
            when (current) {
                MediaTabState.Effect.ScrollToTop -> gridState.scrollToItem(0)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.attachments.isEmpty()) {
            EmptyState(text = emptyText)
        } else {
            MediaGrid(
                attachments = state.attachments,
                imageWidthDp = state.imageWidthDp,
                onAttachmentClick = { onEvent(MediaTabState.Event.AttachmentClicked(it.id)) },
                state = gridState,
                contentPadding = PaddingValues(
                    start = 4.dp,
                    top = TabActionsHeight + 4.dp,
                    end = 4.dp,
                    bottom = 4.dp,
                ),
            )
        }
        TabActionsRow(modifier = Modifier.align(Alignment.TopEnd)) {
            SortMenu(
                options = MEDIA_SORT_OPTIONS,
                selected = state.sort,
                ascending = state.sortAscending,
                onSelect = { onEvent(MediaTabState.Event.SortSelected(it)) },
            )
        }
    }
}
