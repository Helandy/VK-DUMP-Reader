package com.etozhesandy.redpanda.features.chat.presentation.chat.view

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.chat.R
import com.etozhesandy.redpanda.features.chat.model.AudioProgress
import kotlinx.coroutines.delay

/** How often the position of a playing track is read back for its progress bar. */
private const val POSITION_POLL_MS = 200L

/**
 * One player for a whole screen of audio rows, so starting one track stops whichever was playing.
 *
 * Playback is purely UI: it lives exactly as long as the screen that remembered it and never
 * reaches a ViewModel.
 */
@Stable
class AudioPlayback internal constructor(private val player: ExoPlayer) {

    /** The attachment loaded into the player, playing or paused. */
    var playingId: String? by mutableStateOf(null)
        internal set

    var isPlaying: Boolean by mutableStateOf(false)
        internal set

    private var positionMs: Long by mutableLongStateOf(0L)

    /** The player's own reading once it has one; until then the caller's pre-read length stands in. */
    private var playerDurationMs: Long by mutableLongStateOf(C.TIME_UNSET)

    fun isPlaying(attachment: Attachment): Boolean = isPlaying && playingId == attachment.id

    /** Progress of [attachment] if it is the loaded track, else null. */
    fun progressOf(attachment: Attachment, knownDurationMs: Long?): AudioProgress? {
        if (playingId != attachment.id) return null
        val duration = playerDurationMs.takeIf { it != C.TIME_UNSET && it > 0 } ?: knownDurationMs ?: return null
        return AudioProgress(positionMs.coerceAtMost(duration), duration)
    }

    /**
     * The same track pauses or resumes; another one replaces it. A path-less track is ignored.
     * [localPath] is played instead of the attachment's own link once there is a copy on the device.
     */
    fun toggle(attachment: Attachment, localPath: String? = null) {
        if (attachment.path.isBlank()) return
        if (playingId == attachment.id) {
            when {
                player.isPlaying -> player.pause()
                // A finished track stays at its end, where `play()` alone does nothing.
                player.playbackState == Player.STATE_ENDED -> {
                    player.seekTo(0)
                    player.play()
                }
                else -> player.play()
            }
        } else {
            player.setMediaItem(MediaItem.fromUri(localPath ?: attachment.path))
            player.prepare()
            player.play()
            playingId = attachment.id
            positionMs = 0L
            playerDurationMs = C.TIME_UNSET
        }
    }

    fun seekTo(attachment: Attachment, positionMs: Long) {
        if (playingId != attachment.id) return
        player.seekTo(positionMs)
        this.positionMs = positionMs
    }

    internal fun syncPosition() {
        positionMs = player.currentPosition
        playerDurationMs = player.duration
    }
}

@Composable
fun rememberAudioPlayback(): AudioPlayback {
    val context = LocalContext.current
    val player = remember {
        // VK's voice messages are constant-bitrate MP3s without a Xing/Info header; without
        // constant-bitrate seeking the player knows neither their length nor how to seek in them.
        val extractors = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        ExoPlayer.Builder(context, DefaultMediaSourceFactory(context, extractors)).build()
    }
    val playback = remember(player) { AudioPlayback(player) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                playback.isPlaying = playing
                playback.syncPosition()
            }

            // Voice messages are CDN links, and an old export's links can be dead; without this the
            // row would just sit there with nothing playing and no word why.
            override fun onPlayerError(error: PlaybackException) {
                playback.playingId = null
                Toast.makeText(context, R.string.chat_audio_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // The player reports no position changes of its own, so a playing track is read back on a timer.
    LaunchedEffect(playback.isPlaying) {
        while (playback.isPlaying) {
            playback.syncPosition()
            delay(POSITION_POLL_MS)
        }
    }
    return playback
}
