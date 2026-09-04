package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.etozhesandy.redpanda.core.designsystem.R
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.AttachmentType
import java.io.File

/**
 * The size is caller-controlled via [modifier] (default 120dp square, used inline in message
 * bubbles). The media grid overrides it with a fillMaxWidth/aspectRatio modifier so thumbnails
 * fit the configured number of columns.
 */
@Composable
fun AttachmentThumbnail(
    attachment: Attachment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(120.dp),
) = MediaThumbnail(
    path = attachment.path,
    type = attachment.type,
    caption = attachment.caption,
    onClick = onClick,
    modifier = modifier,
)

/**
 * The same tile addressed by its parts, for callers holding only a path and a kind — folder
 * previews come from an aggregate query rather than a whole [Attachment].
 */
@Composable
fun MediaThumbnail(
    path: String,
    type: AttachmentType,
    caption: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(120.dp),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Deliberately no `else`: this is the one place every attachment tile goes through, so a
        // future AttachmentType constant should break the build here rather than render as blank.
        when (type) {
            AttachmentType.PHOTO,
            AttachmentType.VIDEO,
            AttachmentType.STICKER,
            AttachmentType.GRAFFITI,
            -> if (path.isBlank()) {
                // Media the source no longer has — a removed video still has a record but nothing
                // to load, and without this it would render as an empty tile.
                AttachmentPlaceholder(type, caption, Modifier.matchParentSize())
            } else {
                val model = if (path.startsWith("http")) path else File(path)
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    // Stickers are transparent artwork sized to their own aspect ratio; cropping
                    // them to a square tile cuts the drawing.
                    contentScale = if (type == AttachmentType.STICKER) {
                        ContentScale.Fit
                    } else {
                        ContentScale.Crop
                    },
                    modifier = Modifier.matchParentSize(),
                )
                if (type == AttachmentType.VIDEO) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = stringResource(R.string.attachment_type_video),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            AttachmentType.FILE,
            AttachmentType.AUDIO,
            AttachmentType.LINK,
            AttachmentType.WALL,
            AttachmentType.CALL,
            AttachmentType.OTHER,
            -> AttachmentPlaceholder(type, caption, Modifier.matchParentSize())
        }
    }
}
