package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.ui.compose.state.PlayPauseButtonState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import com.etozhesandy.redpanda.core.designsystem.R

/** How often the position under the scrubber is refreshed while the video plays. */
private const val PROGRESS_TICK_MS = 200L

/**
 * Playback controls over a [VideoPlayer]: play/pause in the middle, the clock and the scrubber
 * along the bottom.
 *
 * Only the button and the scrubber take a gesture — the dimmed area around them stays transparent
 * to touch, so the pager keeps its swipe to the next attachment even while the controls are up.
 */
@Composable
fun VideoPlayerControls(
    player: Player,
    playPause: PlayPauseButtonState,
    modifier: Modifier = Modifier,
) {
    val progress = rememberProgressStateWithTickInterval(player, tickIntervalMs = PROGRESS_TICK_MS)
    // A live video has no end to scrub to, and neither does one whose duration hasn't arrived yet.
    val durationMs = progress.durationMs.takeIf { it != C.TIME_UNSET && it > 0L }
    // While the viewer drags the scrubber it shows their position, not the one still playing.
    var scrubbedToMs: Long? by remember { mutableStateOf(null) }
    val positionMs = (scrubbedToMs ?: progress.currentPositionMs).coerceIn(0L, durationMs ?: 0L)

    Box(modifier = modifier.background(Color.Black.copy(alpha = 0.35f))) {
        IconButton(
            onClick = playPause::onClick,
            enabled = playPause.isEnabled,
            modifier = Modifier.align(Alignment.Center).size(72.dp),
        ) {
            Icon(
                imageVector = if (playPause.showPlay) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = stringResource(
                    if (playPause.showPlay) R.string.action_play else R.string.action_pause,
                ),
                tint = Color.White,
                modifier = Modifier.size(48.dp),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlaybackTime(positionMs)
            Slider(
                value = positionMs.toFloat(),
                onValueChange = { scrubbedToMs = it.toLong() },
                onValueChangeFinished = {
                    scrubbedToMs?.let { player.seekTo(it) }
                    scrubbedToMs = null
                },
                // An unknown duration leaves a valid but disabled range: an empty one would make
                // the slider divide by its own span.
                valueRange = 0f..(durationMs ?: 1L).toFloat(),
                enabled = durationMs != null,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
                modifier = Modifier.weight(1f),
            )
            PlaybackTime(durationMs ?: 0L)
        }
    }
}

@Composable
private fun PlaybackTime(timeMs: Long) {
    Text(
        text = Util.getStringForTime(timeMs),
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
    )
}
