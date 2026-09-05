package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * [autoPlay] starts playback as soon as the player is ready; in a pager it must be true only for
 * the page currently on screen, so off-screen videos don't play in the background.
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

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                // Without this the controls pop up the moment playback starts (and on every
                // pause/buffer); the overlay should only appear when the viewer taps the video.
                controllerAutoShow = false
                hideController()
            }
        },
    )
}
