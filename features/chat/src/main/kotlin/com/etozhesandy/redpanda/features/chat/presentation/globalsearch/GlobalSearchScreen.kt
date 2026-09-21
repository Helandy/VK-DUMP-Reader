package com.etozhesandy.redpanda.features.chat.presentation.globalsearch

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
import kotlinx.coroutines.flow.distinctUntilChanged
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.EmptyState
import com.etozhesandy.redpanda.core.designsystem.components.MESSAGE_SORT_OPTIONS
import com.etozhesandy.redpanda.core.designsystem.components.ScrollToTopOnChange
import com.etozhesandy.redpanda.core.designsystem.components.SearchField
import com.etozhesandy.redpanda.core.designsystem.components.SortMenu
import com.etozhesandy.redpanda.features.chat.R
import com.etozhesandy.redpanda.features.chat.presentation.globalsearch.view.GlobalSearchResultItem

@Composable
fun GlobalSearchScreen(
    state: GlobalSearchState.State,
    onEvent: (GlobalSearchState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(
        topBar = {
            TopAppBar(
                title = {
                    SearchField(
                        value = state.query,
                        onValueChange = { onEvent(GlobalSearchState.Event.QueryChanged(it)) },
                        placeholder = stringResource(R.string.global_search_placeholder),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(GlobalSearchState.Event.BackClicked) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.global_search_close))
                    }
                },
                actions = {
                    SortMenu(
                        options = MESSAGE_SORT_OPTIONS,
                        selected = state.sort,
                        ascending = state.sortAscending,
                        onSelect = { onEvent(GlobalSearchState.Event.SortSelected(it)) },
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

            // Keep old results visible while a new query is running so the screen never looks
            // frozen or needlessly empty.
            if (state.results.isEmpty() && state.query.isNotBlank() && !state.isSearching) {
                EmptyState(text = stringResource(R.string.global_search_empty))
                return@Column
            }
            val listState = rememberLazyListState()
            ScrollToTopOnChange(state.sort to state.sortAscending) { listState.scrollToItem(0) }
            LaunchedEffect(listState, state.query) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .distinctUntilChanged()
                    .collect { lastVisibleIndex ->
                        if (lastVisibleIndex != null && lastVisibleIndex >= state.results.lastIndex - 5) {
                            onEvent(GlobalSearchState.Event.LoadMore)
                        }
                    }
            }
            LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                items(state.results, key = { it.message.id }) { result ->
                    GlobalSearchResultItem(
                        result = result,
                        onClick = { onEvent(GlobalSearchState.Event.ResultClicked(result)) },
                    )
                }
            }
            if (state.isLoadingMore) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
