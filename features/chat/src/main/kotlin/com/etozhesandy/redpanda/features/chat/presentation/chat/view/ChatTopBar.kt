package com.etozhesandy.redpanda.features.chat.presentation.chat.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.R as DesignSystemR
import com.etozhesandy.redpanda.features.chat.R
import com.etozhesandy.redpanda.features.chat.presentation.chat.ChatTopBarState

/**
 * The chat's bar: the dialog name, the way back and the way into search.
 *
 * Nothing here depends on which tab is showing — ordering controls live inside the tab they order,
 * so the bar stays the same across all five pages of the pager.
 */
@Composable
fun ChatTopBar(
    state: ChatTopBarState.State,
    onEvent: (ChatTopBarState.Event) -> Unit,
) {
    TopAppBar(
        title = { Text(state.dialog?.peerName ?: "") },
        navigationIcon = {
            IconButton(onClick = { onEvent(ChatTopBarState.Event.BackClicked) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(DesignSystemR.string.action_back))
            }
        },
        actions = {
            IconButton(onClick = { onEvent(ChatTopBarState.Event.SearchClicked) }) {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.chat_action_search))
            }
        },
    )
}
