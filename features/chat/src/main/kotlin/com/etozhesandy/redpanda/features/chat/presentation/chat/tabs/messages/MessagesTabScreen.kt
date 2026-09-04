package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.messages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.etozhesandy.redpanda.core.designsystem.media.isVisualMedia
import com.etozhesandy.redpanda.features.chat.R
import com.etozhesandy.redpanda.features.chat.model.MessageUi
import com.etozhesandy.redpanda.features.chat.presentation.chat.utils.formatMessageDate
import com.etozhesandy.redpanda.features.chat.presentation.chat.utils.isSameDay
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.DateSeparator
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.MessageBubble
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.TabActionsRow

@Composable
fun MessagesTabScreen(
    pagingItems: LazyPagingItems<MessageUi>,
    listState: LazyListState,
    onEvent: (MessagesTabState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TabActionsRow {
            // Flipping the order re-anchors paging instead of reordering a loaded list, so this
            // is a toggle rather than one of the sort menus the other tabs draw here.
            IconButton(onClick = { onEvent(MessagesTabState.Event.ToggleOrderReversed) }) {
                Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.chat_action_reverse_order))
            }
        }
        MessagesList(pagingItems = pagingItems, listState = listState, onEvent = onEvent)
    }
}

@Composable
private fun MessagesList(
    pagingItems: LazyPagingItems<MessageUi>,
    listState: LazyListState,
    onEvent: (MessagesTabState.Event) -> Unit,
) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.message.id },
        ) { index ->
            val item = pagingItems[index] ?: return@items
            val message = item.message
            val previous = if (index > 0) pagingItems.peek(index - 1)?.message else null
            if (previous == null || !isSameDay(previous.timestampEpoch, message.timestampEpoch)) {
                DateSeparator(date = formatMessageDate(message.timestampEpoch))
            }
            MessageBubble(
                message = message,
                attachments = item.attachments,
                onFavoriteToggle = {
                    onEvent(MessagesTabState.Event.FavoriteToggled(message.id, !message.isFavorite))
                },
                // Only media belongs in the photo viewer; a document or a wall post opens
                // where it actually lives, and metadata-only kinds have nowhere to go at all.
                onAttachmentClick = { attachment ->
                    when {
                        attachment.type.isVisualMedia ->
                            onEvent(MessagesTabState.Event.AttachmentClicked(attachment.id))

                        attachment.path.startsWith("http") ->
                            onEvent(MessagesTabState.Event.FileClicked(attachment.path))
                    }
                },
            )
        }
    }
}
