package com.etozhesandy.redpanda.features.chat.presentation.chat.view

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.designsystem.media.label
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.chat.R

/**
 * Not every audio attachment is playable: the classic HTML export names voice messages without
 * linking them, and a plain `audio` attachment is metadata only. Those rows still exist — losing
 * them would understate what the dialog held — but they carry no play control and no click target.
 */
@Composable
fun AudioListItem(
    attachment: Attachment,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPlayable = attachment.path.isNotBlank()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isPlayable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
        }
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = attachment.caption?.takeIf { it.isNotBlank() }
                ?: attachment.path.substringAfterLast('/').ifBlank { attachment.type.label() },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
