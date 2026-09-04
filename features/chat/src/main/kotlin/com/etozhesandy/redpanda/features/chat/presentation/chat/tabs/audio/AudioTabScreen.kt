package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.audio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.etozhesandy.redpanda.core.designsystem.components.EmptyState
import com.etozhesandy.redpanda.core.designsystem.components.MEDIA_SORT_OPTIONS
import com.etozhesandy.redpanda.core.designsystem.components.ScrollToTopOnChange
import com.etozhesandy.redpanda.core.designsystem.components.SortMenu
import com.etozhesandy.redpanda.features.chat.R
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.AudioListItem
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.TabActionsRow

/** Owns its own player: audio is the only tab that plays anything, and it stops when it leaves. */
@Composable
fun AudioTabScreen(
    state: AudioTabState.State,
    onEvent: (AudioTabState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    ScrollToTopOnChange(state.sort to state.sortAscending) { listState.scrollToItem(0) }
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    var playingId by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabActionsRow {
            SortMenu(
                options = MEDIA_SORT_OPTIONS,
                selected = state.sort,
                ascending = state.sortAscending,
                onSelect = { onEvent(AudioTabState.Event.SortSelected(it)) },
            )
        }
        if (state.attachments.isEmpty()) {
            EmptyState(text = stringResource(R.string.chat_empty_audio))
            return@Column
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(state.attachments, key = { it.id }) { attachment ->
                AudioListItem(
                    attachment = attachment,
                    isPlaying = playingId == attachment.id && isPlaying,
                    onClick = {
                        if (attachment.path.isBlank()) return@AudioListItem
                        if (playingId == attachment.id) {
                            if (player.isPlaying) player.pause() else player.play()
                        } else {
                            player.setMediaItem(MediaItem.fromUri(attachment.path))
                            player.prepare()
                            player.play()
                            playingId = attachment.id
                        }
                    },
                )
            }
        }
    }
}
