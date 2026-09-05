package com.etozhesandy.redpanda.features.chat.presentation.globalsearch

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
        // An empty query isn't "nothing found" — it's nothing asked for yet, so the list simply
        // stays blank until the user types.
        if (state.results.isEmpty() && state.query.isNotBlank()) {
            EmptyState(text = stringResource(R.string.global_search_empty))
            return@BaseScreen
        }
        val listState = rememberLazyListState()
        ScrollToTopOnChange(state.sort to state.sortAscending) { listState.scrollToItem(0) }
        LazyColumn(state = listState) {
            items(state.results, key = { it.message.id }) { result ->
                GlobalSearchResultItem(
                    result = result,
                    onClick = { onEvent(GlobalSearchState.Event.ResultClicked(result)) },
                )
            }
        }
    }
}
