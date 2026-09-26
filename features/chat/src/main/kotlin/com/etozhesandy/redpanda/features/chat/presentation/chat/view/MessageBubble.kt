package com.etozhesandy.redpanda.features.chat.presentation.chat.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.designsystem.media.AttachmentThumbnail
import com.etozhesandy.redpanda.core.designsystem.theme.PandaBlack
import com.etozhesandy.redpanda.core.designsystem.theme.PandaBubbleIncomingDark
import com.etozhesandy.redpanda.core.designsystem.theme.PandaBubbleIncomingLight
import com.etozhesandy.redpanda.core.designsystem.theme.PandaWhite
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.AttachmentType
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.features.chat.R
import com.etozhesandy.redpanda.features.chat.presentation.chat.utils.formatMessageTime

@Composable
fun MessageBubble(
    message: Message,
    attachments: List<Attachment>,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onAttachmentClick: (Attachment) -> Unit,
    audioContent: @Composable (Attachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Voice messages are played in place, as a row with its own control, rather than shown as a
    // tile that could only hand the link to another app.
    val (audio, visual) = remember(attachments) { attachments.partition { it.type == AttachmentType.AUDIO } }
    val alignment = if (message.isOutgoing) Alignment.End else Alignment.Start
    val bubbleColor: Color
    val contentColor: Color
    if (message.isOutgoing) {
        bubbleColor = MaterialTheme.colorScheme.primaryContainer
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        bubbleColor = if (isSystemInDarkTheme()) PandaBubbleIncomingDark else PandaBubbleIncomingLight
        contentColor = if (isSystemInDarkTheme()) PandaWhite else PandaBlack
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = alignment,
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (visual.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(visual, key = { it.id }) { attachment ->
                            AttachmentThumbnail(
                                attachment = attachment,
                                onClick = { onAttachmentClick(attachment) },
                            )
                        }
                    }
                }
                audio.forEach { attachment -> audioContent(attachment) }
                if (message.text.isNotBlank()) {
                    Text(text = message.text, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatMessageTime(message.timestampEpoch),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = stringResource(R.string.chat_action_favorite),
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
