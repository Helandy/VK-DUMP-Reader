package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.common.net.openExternally
import com.etozhesandy.redpanda.core.designsystem.components.EmptyState
import com.etozhesandy.redpanda.core.designsystem.components.MEDIA_SORT_OPTIONS
import com.etozhesandy.redpanda.core.designsystem.components.ScrollToTopOnChange
import com.etozhesandy.redpanda.core.designsystem.components.SortMenu
import com.etozhesandy.redpanda.features.chat.R
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.FileListItem
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.TabActionsRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Documents are never downloaded by any export — they stay behind a `vk.com/doc…` link — so a row
 * hands the URL to whatever app handles it. Rows without one are still listed rather than hidden:
 * the dialog did contain that document, and saying so is the point.
 */
@Composable
fun FilesTabScreen(
    state: FilesTabState.State,
    effect: Flow<FilesTabState.Effect>,
    onEvent: (FilesTabState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        effect.collectLatest { current ->
            when (current) {
                is FilesTabState.Effect.OpenExternally -> context.openExternally(current.url)
            }
        }
    }

    val listState = rememberLazyListState()
    ScrollToTopOnChange(state.sort to state.sortAscending) { listState.scrollToItem(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabActionsRow {
            SortMenu(
                options = MEDIA_SORT_OPTIONS,
                selected = state.sort,
                ascending = state.sortAscending,
                onSelect = { onEvent(FilesTabState.Event.SortSelected(it)) },
            )
        }
        if (state.attachments.isEmpty()) {
            EmptyState(text = stringResource(R.string.chat_empty_files))
            return@Column
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(state.attachments, key = { it.id }) { attachment ->
                FileListItem(
                    attachment = attachment,
                    onClick = { onEvent(FilesTabState.Event.FileClicked(attachment.path)) },
                )
            }
        }
    }
}
