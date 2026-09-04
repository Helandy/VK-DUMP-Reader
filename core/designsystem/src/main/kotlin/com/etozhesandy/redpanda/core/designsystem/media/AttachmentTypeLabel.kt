package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.R
import com.etozhesandy.redpanda.core.model.AttachmentType

/** Human-readable name of an attachment kind, for tiles and viewers that show no thumbnail. */
@Composable
fun AttachmentType.label(): String = stringResource(
    when (this) {
        AttachmentType.PHOTO -> R.string.attachment_type_photo
        AttachmentType.VIDEO -> R.string.attachment_type_video
        AttachmentType.FILE -> R.string.attachment_type_file
        AttachmentType.AUDIO -> R.string.attachment_type_audio
        AttachmentType.STICKER -> R.string.attachment_type_sticker
        AttachmentType.GRAFFITI -> R.string.attachment_type_graffiti
        AttachmentType.LINK -> R.string.attachment_type_link
        AttachmentType.WALL -> R.string.attachment_type_wall
        AttachmentType.CALL -> R.string.attachment_type_call
        AttachmentType.OTHER -> R.string.attachment_type_other
    },
)

/** Icon standing in for an attachment kind when there is no image to show. */
val AttachmentType.icon: ImageVector
    get() = when (this) {
        AttachmentType.PHOTO -> Icons.Default.Image
        AttachmentType.GRAFFITI -> Icons.Default.Brush
        AttachmentType.VIDEO -> Icons.Default.PlayCircle
        AttachmentType.FILE -> Icons.Default.Description
        AttachmentType.AUDIO -> Icons.Default.MusicNote
        AttachmentType.STICKER -> Icons.Default.EmojiEmotions
        AttachmentType.LINK -> Icons.Default.Link
        AttachmentType.WALL -> Icons.Default.Newspaper
        AttachmentType.CALL -> Icons.Default.Call
        AttachmentType.OTHER -> Icons.Default.AttachFile
    }

/** Whether this kind is something [AttachmentThumbnail] can render as an image. */
val AttachmentType.isVisualMedia: Boolean
    get() = this == AttachmentType.PHOTO ||
        this == AttachmentType.VIDEO ||
        this == AttachmentType.STICKER ||
        this == AttachmentType.GRAFFITI
