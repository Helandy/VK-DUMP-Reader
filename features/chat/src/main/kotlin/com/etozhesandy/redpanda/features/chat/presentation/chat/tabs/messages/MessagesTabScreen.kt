package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.messages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.etozhesandy.redpanda.core.designsystem.media.isVisualMedia
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.AttachmentType
import com.etozhesandy.redpanda.features.chat.R
import com.etozhesandy.redpanda.features.chat.model.MessageUi
import com.etozhesandy.redpanda.features.chat.presentation.chat.utils.formatMessageDate
import com.etozhesandy.redpanda.features.chat.presentation.chat.utils.isSameDay
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.AudioListItem
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.DateSeparator
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.MessageBubble
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.TabActionsHeight
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.TabActionsRow
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.rememberAudioPlayback
import kotlinx.coroutines.flow.first

@Composable
fun MessagesTabScreen(
    state: MessagesTabState.State,
    pagingItems: LazyPagingItems<MessageUi>,
    listState: LazyListState,
    onEvent: (MessagesTabState.Event) -> Unit,
    modifier: Modifier = Modifier,
    targetMessageId: String? = null,
) {
    // A database paging anchor is not a LazyColumn index: refresh may load earlier rows,
    // and prepends can shift the target. Locate the actual message in the loaded snapshot.
    var targetReached by rememberSaveable(targetMessageId) { mutableStateOf(false) }
    LaunchedEffect(targetMessageId, pagingItems, listState) {
        if (targetMessageId == null || targetReached) return@LaunchedEffect
        val index = snapshotFlow {
            pagingItems.itemSnapshotList.items.indexOfFirst { it.message.id == targetMessageId }
        }.first { it >= 0 }
        listState.scrollToItem(index)
        targetReached = true
    }
    // One player for the whole chat, so starting a voice message stops the previous one.
    val playback = rememberAudioPlayback()
    Box(modifier = modifier.fillMaxSize()) {
        MessagesList(
            audioContent = { attachment ->
                LaunchedEffect(attachment.id) { onEvent(MessagesTabState.Event.AudioShown(attachment)) }
                val prepared = state.preparedAudio[attachment.id]
                AudioListItem(
                    attachment = attachment,
                    isPlaying = playback.isPlaying(attachment),
                    onClick = { playback.toggle(attachment, prepared?.localPath) },
                    durationMs = prepared?.durationMs,
                    progress = playback.progressOf(attachment, prepared?.durationMs),
                    onSeek = { playback.seekTo(attachment, it) },
                )
            },
            favoriteIds = state.favoriteIds,
            pagingItems = pagingItems,
            listState = listState,
            onEvent = onEvent,
            contentPadding = PaddingValues(top = TabActionsHeight),
        )
        TabActionsRow(modifier = Modifier.align(Alignment.TopEnd)) {
            // Flipping the order re-anchors paging instead of reordering a loaded list, so this
            // is a toggle rather than one of the sort menus the other tabs draw here.
            IconButton(onClick = { onEvent(MessagesTabState.Event.ToggleOrderReversed) }) {
                Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.chat_action_reverse_order))
            }
        }
    }
}

@Composable
private fun MessagesList(
    audioContent: @Composable (Attachment) -> Unit,
    favoriteIds: Set<String>,
    pagingItems: LazyPagingItems<MessageUi>,
    listState: LazyListState,
    onEvent: (MessagesTabState.Event) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.message.id },
            // Plain text and a bubble full of media measure nothing alike; keeping them apart lets
            // a scrolled-off row be reused by one of its own kind.
            contentType = pagingItems.itemContentType { item ->
                if (item.attachments.isEmpty()) MESSAGE_TEXT else MESSAGE_WITH_ATTACHMENTS
            },
        ) { index ->
            val item = pagingItems[index] ?: return@items
            val message = item.message
            val previous = if (index > 0) pagingItems.peek(index - 1)?.message else null
            if (previous == null || !isSameDay(previous.timestampEpoch, message.timestampEpoch)) {
                DateSeparator(date = formatMessageDate(message.timestampEpoch))
            }
            val isFavorite = message.id in favoriteIds
            MessageBubble(
                message = message,
                attachments = item.attachments,
                isFavorite = isFavorite,
                onFavoriteToggle = {
                    onEvent(MessagesTabState.Event.FavoriteToggled(message.id, !isFavorite))
                },
                // Only media belongs in the photo viewer; a document or a wall post opens
                // where it actually lives, and metadata-only kinds have nowhere to go at all.
                onAttachmentClick = { attachment ->
                    when {
                        attachment.type.isVisualMedia ->
                            onEvent(MessagesTabState.Event.AttachmentClicked(attachment.id))

                        attachment.type != AttachmentType.AUDIO && attachment.path.startsWith("http") ->
                            onEvent(MessagesTabState.Event.FileClicked(attachment.path))
                    }
                },
                audioContent = audioContent,
            )
        }
    }
}

private const val MESSAGE_TEXT = "text"
private const val MESSAGE_WITH_ATTACHMENTS = "attachments"
