package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.media

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.etozhesandy.redpanda.core.designsystem.components.EmptyState
import com.etozhesandy.redpanda.core.designsystem.components.MEDIA_SORT_OPTIONS
import com.etozhesandy.redpanda.core.designsystem.components.ScrollToTopOnChange
import com.etozhesandy.redpanda.core.designsystem.components.SortMenu
import com.etozhesandy.redpanda.core.designsystem.media.MediaGrid
import com.etozhesandy.redpanda.features.chat.presentation.chat.MediaScrollSlot
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.TabActionsRow

/** The grid shared by «Фото» and «Видео» — [emptyText] is all that differs between them. */
@Composable
fun MediaTabScreen(
    state: MediaTabState.State,
    scrollSlot: MediaScrollSlot,
    emptyText: String,
    onEvent: (MediaTabState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberCachedGridState(scrollSlot)
    ScrollToTopOnChange(state.sort to state.sortAscending) { gridState.scrollToItem(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabActionsRow {
            SortMenu(
                options = MEDIA_SORT_OPTIONS,
                selected = state.sort,
                ascending = state.sortAscending,
                onSelect = { onEvent(MediaTabState.Event.SortSelected(it)) },
            )
        }
        if (state.attachments.isEmpty()) {
            EmptyState(text = emptyText)
        } else {
            MediaGrid(
                attachments = state.attachments,
                imageWidthDp = state.imageWidthDp,
                onAttachmentClick = { onEvent(MediaTabState.Event.AttachmentClicked(it.id)) },
                state = gridState,
            )
        }
    }
}
