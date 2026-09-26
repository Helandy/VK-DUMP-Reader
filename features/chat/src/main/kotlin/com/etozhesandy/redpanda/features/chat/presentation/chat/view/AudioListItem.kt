package com.etozhesandy.redpanda.features.chat.presentation.chat.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.designsystem.media.label
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.chat.R
import com.etozhesandy.redpanda.features.chat.model.AudioProgress
import com.etozhesandy.redpanda.features.chat.presentation.chat.utils.formatDuration

/**
 * Not every audio attachment is playable: the classic HTML export names voice messages without
 * linking them, and a plain `audio` attachment is metadata only. Those rows still exist — losing
 * them would understate what the dialog held — but they carry no play control and no click target.
 *
 * [durationMs] is the length read ahead of playback, shown so a row says how long it is before
 * anyone presses play. [progress] is set only on the row the player is on, which then gets a bar
 * to scrub through it.
 */
@Composable
fun AudioListItem(
    attachment: Attachment,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    durationMs: Long? = null,
    progress: AudioProgress? = null,
    onSeek: (positionMs: Long) -> Unit = {},
) {
    val isPlayable = attachment.path.isNotBlank()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isPlayable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPlayable) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.chat_action_pause else R.string.chat_action_play,
                    ),
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.caption?.takeIf { it.isNotBlank() }
                    ?: attachment.path.substringAfterLast('/').ifBlank { attachment.type.label() },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (progress != null) {
                AudioSeekBar(progress = progress, onSeek = onSeek)
            } else if (durationMs != null) {
                Text(
                    text = formatDuration(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The thumb follows the finger while dragging and only seeks on release, so the player isn't
 * asked to jump on every pixel of the drag.
 */
@Composable
private fun AudioSeekBar(
    progress: AudioProgress,
    onSeek: (positionMs: Long) -> Unit,
) {
    var dragPositionMs by remember { mutableStateOf<Float?>(null) }
    val shownPositionMs = dragPositionMs ?: progress.positionMs.toFloat()
    Slider(
        value = shownPositionMs,
        onValueChange = { dragPositionMs = it },
        onValueChangeFinished = {
            dragPositionMs?.let { onSeek(it.toLong()) }
            dragPositionMs = null
        },
        valueRange = 0f..progress.durationMs.toFloat(),
    )
    Text(
        text = "${formatDuration(shownPositionMs.toLong())} / ${formatDuration(progress.durationMs)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
