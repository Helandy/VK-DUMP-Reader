package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.delay

/** How long the controls stay up over a video that keeps playing. */
private const val CONTROLS_TIMEOUT_MS = 4_000L

/** How far a double tap on either half of the video seeks. */
private const val SEEK_STEP_MS = 5_000L

/** How long the double-tap seek confirmation stays up after the last tap. */
private const val SEEK_INDICATOR_TIMEOUT_MS = 600L

/**
 * [autoPlay] starts playback as soon as the player is ready; in a pager it must be true only for
 * the page currently on screen, so off-screen videos don't play in the background.
 *
 * The frame is Media3's Compose surface rather than a `PlayerView`: an interop View that carries
 * its own controls is clickable, and the interop layer treats the whole gesture as handled the
 * moment that View takes the touch down — which left the pager around this player (see
 * [MediaPagerScreen]) without its swipe to the next attachment. A surface claims nothing, so the
 * controls are Compose too ([VideoPlayerControls]) and the only gestures handled here are taps —
 * one shows the controls, a double tap seeks [SEEK_STEP_MS] back or forward depending on the half
 * tapped; a drag passes through to the pager.
 *
 * Until the first frame is decoded the surface shows the video's cached still (the same one its
 * grid tile shows) rather than black, so the viewer sees what they opened straight away.
 */
@Composable
fun VideoPlayer(uri: String, modifier: Modifier = Modifier, autoPlay: Boolean = false) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(SEEK_STEP_MS)
            .setSeekForwardIncrementMs(SEEK_STEP_MS)
            .build()
            .apply {
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
    // Which way the last double tap seeked; each tap bumps the counter so a repeated one in the
    // same direction keeps the confirmation up instead of letting the first one's timer end it.
    var seekedForward: Boolean? by remember { mutableStateOf(null) }
    var seekCount by remember { mutableIntStateOf(0) }

    // Controls left up over a playing video would cover the rest of the clip; over a paused one
    // they stay until the viewer taps again, since that is when they are being used.
    LaunchedEffect(areControlsVisible, playPause.showPlay) {
        if (areControlsVisible && !playPause.showPlay) {
            delay(CONTROLS_TIMEOUT_MS)
            areControlsVisible = false
        }
    }

    LaunchedEffect(seekCount) {
        if (seekedForward != null) {
            delay(SEEK_INDICATOR_TIMEOUT_MS)
            seekedForward = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(player) {
                detectTapGestures(
                    onTap = { areControlsVisible = !areControlsVisible },
                    onDoubleTap = { offset ->
                        val forward = offset.x >= size.width / 2
                        if (forward) player.seekForward() else player.seekBack()
                        seekedForward = forward
                        seekCount++
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        ContentFrame(
            player = player,
            shutter = {
                AsyncImage(
                    model = if (uri.startsWith("http")) uri else File(uri),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                )
            },
        )
        seekedForward?.let { forward ->
            VideoSeekIndicator(
                isForward = forward,
                stepSeconds = (SEEK_STEP_MS / 1_000).toInt(),
                modifier = Modifier.matchParentSize(),
            )
        }
        if (areControlsVisible) {
            VideoPlayerControls(
                player = player,
                playPause = playPause,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
