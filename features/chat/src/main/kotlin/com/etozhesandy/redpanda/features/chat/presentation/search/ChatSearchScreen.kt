package com.etozhesandy.redpanda.features.chat.presentation.search

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.EmptyState
import com.etozhesandy.redpanda.core.designsystem.components.MESSAGE_SORT_OPTIONS
import com.etozhesandy.redpanda.core.designsystem.components.ScrollToTopOnChange
import com.etozhesandy.redpanda.core.designsystem.components.SearchField
import com.etozhesandy.redpanda.core.designsystem.components.SortMenu
import com.etozhesandy.redpanda.features.chat.R
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ChatSearchScreen(
    state: ChatSearchState.State,
    onEvent: (ChatSearchState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(
        topBar = {
            TopAppBar(
                title = {
                    SearchField(
                        value = state.query,
                        onValueChange = { onEvent(ChatSearchState.Event.QueryChanged(it)) },
                        placeholder = stringResource(R.string.chat_search_placeholder),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ChatSearchState.Event.BackClicked) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.chat_search_close))
                    }
                },
                actions = {
                    SortMenu(
                        options = MESSAGE_SORT_OPTIONS,
                        selected = state.sort,
                        ascending = state.sortAscending,
                        onSelect = { onEvent(ChatSearchState.Event.SortSelected(it)) },
                    )
                },
            )
        },
        modifier = modifier,
    ) {
        Column {
            if (state.isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (state.results.isEmpty() && state.query.trim().length >= 3 && !state.isSearching) {
                EmptyState(text = stringResource(R.string.chat_search_empty))
                return@Column
            }
            val listState = rememberLazyListState()
            ScrollToTopOnChange(state.sort to state.sortAscending) { listState.scrollToItem(0) }
            LaunchedEffect(listState, state.query) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .distinctUntilChanged()
                    .collect { lastVisibleIndex ->
                        if (lastVisibleIndex != null && lastVisibleIndex >= state.results.lastIndex - 5) {
                            onEvent(ChatSearchState.Event.LoadMore)
                        }
                    }
            }
            LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                items(state.results, key = { it.id }) { message ->
                    SearchResultItem(
                        message = message,
                        onClick = { onEvent(ChatSearchState.Event.ResultClicked(message)) },
                    )
                }
            }
            if (state.isLoadingMore) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
