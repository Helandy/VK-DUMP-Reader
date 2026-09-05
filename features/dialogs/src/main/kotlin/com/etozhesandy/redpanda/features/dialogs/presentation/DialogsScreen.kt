package com.etozhesandy.redpanda.features.dialogs.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.DIALOG_SORT_OPTIONS
import com.etozhesandy.redpanda.core.designsystem.components.LoadableContent
import com.etozhesandy.redpanda.core.designsystem.components.ScrollToTopOnChange
import com.etozhesandy.redpanda.core.designsystem.components.SearchField
import com.etozhesandy.redpanda.core.designsystem.components.SortMenu
import com.etozhesandy.redpanda.core.model.ProfileStatus
import com.etozhesandy.redpanda.features.dialogs.R
import com.etozhesandy.redpanda.features.dialogs.presentation.DialogsState
import com.etozhesandy.redpanda.features.dialogs.presentation.view.CategoryChips
import com.etozhesandy.redpanda.features.dialogs.presentation.view.DialogListItem
import com.etozhesandy.redpanda.features.dialogs.presentation.view.ImportBanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DialogsScreen(
    state: DialogsState.State,
    effect: Flow<DialogsState.Effect>,
    onEvent: (DialogsState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // A running import can add hundreds of dialogs — one self-replacing Toast instead of
    // a stacked queue that would keep showing for minutes after the import is done.
    val currentToast = remember { arrayOfNulls<Toast>(1) }

    LaunchedEffect(Unit) {
        effect.collectLatest { current ->
            when (current) {
                is DialogsState.Effect.DialogAdded -> {
                    currentToast[0]?.cancel()
                    currentToast[0] = Toast.makeText(
                        context,
                        context.getString(R.string.dialogs_dialog_added, current.peerName),
                        Toast.LENGTH_SHORT,
                    ).apply { show() }
                }
            }
        }
    }

    val listState = rememberLazyListState()
    ScrollToTopOnChange(state.sort to state.sortAscending) { listState.scrollToItem(0) }

    BaseScreen(
        title = stringResource(R.string.dialogs_title),
        modifier = modifier,
        onBack = { onEvent(DialogsState.Event.BackClicked) },
        actions = {
            IconButton(onClick = { onEvent(DialogsState.Event.SearchClicked) }) {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.dialogs_action_search))
            }
            SortMenu(
                options = DIALOG_SORT_OPTIONS,
                selected = state.sort,
                ascending = state.sortAscending,
                onSelect = { onEvent(DialogsState.Event.SortSelected(it)) },
            )
            IconButton(onClick = { onEvent(DialogsState.Event.FavoritesClicked) }) {
                Icon(Icons.Default.Star, contentDescription = stringResource(R.string.dialogs_action_favorites))
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val isImporting = state.profileStatus == ProfileStatus.IMPORTING
            if (isImporting) {
                ImportBanner(progress = state.importProgress)
            }
            SearchField(
                value = state.query,
                onValueChange = { onEvent(DialogsState.Event.QueryChanged(it)) },
                placeholder = stringResource(R.string.dialogs_search_placeholder),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            CategoryChips(
                categories = state.categories,
                selected = state.selectedCategory,
                onSelected = { onEvent(DialogsState.Event.CategorySelected(it)) },
            )
            Box(modifier = Modifier.fillMaxSize()) {
                LoadableContent(
                    isLoading = state.isLoading,
                    isEmpty = state.dialogs.isEmpty(),
                    // While the import is still running an empty list means "not there yet",
                    // not "no such dialog" — saying "nothing found" would be a lie in progress.
                    emptyText = stringResource(
                        if (isImporting) R.string.dialogs_empty_importing else R.string.dialogs_empty,
                    ),
                ) {
                    LazyColumn(state = listState) {
                        items(state.dialogs, key = { it.id }) { dialog ->
                            DialogListItem(
                                dialog = dialog,
                                onClick = { onEvent(DialogsState.Event.DialogClicked(dialog)) },
                            )
                        }
                    }
                }
            }
        }
    }
}
