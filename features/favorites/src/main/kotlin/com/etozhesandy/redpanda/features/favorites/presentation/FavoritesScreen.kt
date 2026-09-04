package com.etozhesandy.redpanda.features.favorites.presentation

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.LoadableContent
import com.etozhesandy.redpanda.features.favorites.R
import com.etozhesandy.redpanda.features.favorites.presentation.FavoritesState
import com.etozhesandy.redpanda.features.favorites.presentation.view.FavoriteMessageItem

@Composable
fun FavoritesScreen(
    state: FavoritesState.State,
    onEvent: (FavoritesState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(
        title = stringResource(R.string.favorites_title),
        modifier = modifier,
        onBack = { onEvent(FavoritesState.Event.BackClicked) },
    ) {
        LoadableContent(
            isLoading = state.isLoading,
            isEmpty = state.messages.isEmpty(),
            emptyText = stringResource(R.string.favorites_empty),
        ) {
            LazyColumn {
                items(state.messages, key = { it.id }) { message ->
                    FavoriteMessageItem(
                        message = message,
                        onClick = { onEvent(FavoritesState.Event.MessageClicked(message)) },
                    )
                }
            }
        }
    }
}
