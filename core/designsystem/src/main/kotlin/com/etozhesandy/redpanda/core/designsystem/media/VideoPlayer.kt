package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import kotlinx.coroutines.delay

/** How long the controls stay up over a video that keeps playing. */
private const val CONTROLS_TIMEOUT_MS = 4_000L

/**
 * [autoPlay] starts playback as soon as the player is ready; in a pager it must be true only for
 * the page currently on screen, so off-screen videos don't play in the background.
 *
 * The frame is Media3's Compose surface rather than a `PlayerView`: an interop View that carries
 * its own controls is clickable, and the interop layer treats the whole gesture as handled the
 * moment that View takes the touch down — which left the pager around this player (see
 * [MediaPagerScreen]) without its swipe to the next attachment. A surface claims nothing, so the
 * controls are Compose too ([VideoPlayerControls]) and the only gesture handled here is the tap
 * that shows them; a drag passes through to the pager.
 */
@Composable
fun VideoPlayer(uri: String, modifier: Modifier = Modifier, autoPlay: Boolean = false) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    LaunchedEffect(player, autoPlay) { player.playWhenReady = autoPlay }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    var areControlsVisible by remember { mutableStateOf(false) }
    val playPause = rememberPlayPauseButtonState(player)

    // Controls left up over a playing video would cover the rest of the clip; over a paused one
    // they stay until the viewer taps again, since that is when they are being used.
    LaunchedEffect(areControlsVisible, playPause.showPlay) {
        if (areControlsVisible && !playPause.showPlay) {
            delay(CONTROLS_TIMEOUT_MS)
            areControlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { areControlsVisible = !areControlsVisible }
            },
        contentAlignment = Alignment.Center,
    ) {
        ContentFrame(player = player)
        if (areControlsVisible) {
            VideoPlayerControls(
                player = player,
                playPause = playPause,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
