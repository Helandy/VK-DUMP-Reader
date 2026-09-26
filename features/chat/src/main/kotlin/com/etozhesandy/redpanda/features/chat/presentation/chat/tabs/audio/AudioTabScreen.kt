package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.audio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.components.EmptyState
import com.etozhesandy.redpanda.core.designsystem.components.MEDIA_SORT_OPTIONS
import com.etozhesandy.redpanda.core.designsystem.components.ScrollToTopOnChange
import com.etozhesandy.redpanda.core.designsystem.components.SortMenu
import com.etozhesandy.redpanda.features.chat.R
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.AudioListItem
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.TabActionsHeight
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.TabActionsRow
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.rememberAudioPlayback

/** Owns its own player, which stops when the tab leaves. */
@Composable
fun AudioTabScreen(
    state: AudioTabState.State,
    onEvent: (AudioTabState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    ScrollToTopOnChange(state.sort to state.sortAscending) { listState.scrollToItem(0) }
    val playback = rememberAudioPlayback()

    Box(modifier = modifier.fillMaxSize()) {
        if (state.attachments.isEmpty()) {
            EmptyState(text = stringResource(R.string.chat_empty_audio))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = TabActionsHeight),
            ) {
                items(state.attachments, key = { it.id }) { attachment ->
                    LaunchedEffect(attachment.id) { onEvent(AudioTabState.Event.AudioShown(attachment)) }
                    val prepared = state.preparedAudio[attachment.id]
                    AudioListItem(
                        attachment = attachment,
                        isPlaying = playback.isPlaying(attachment),
                        onClick = { playback.toggle(attachment, prepared?.localPath) },
                        durationMs = prepared?.durationMs,
                        progress = playback.progressOf(attachment, prepared?.durationMs),
                        onSeek = { playback.seekTo(attachment, it) },
                    )
                }
            }
        }
        TabActionsRow(modifier = Modifier.align(Alignment.TopEnd)) {
            SortMenu(
                options = MEDIA_SORT_OPTIONS,
                selected = state.sort,
                ascending = state.sortAscending,
                onSelect = { onEvent(AudioTabState.Event.SortSelected(it)) },
            )
        }
    }
}
