package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.AttachmentType
import java.io.File

/**
 * One full-screen attachment inside a media viewer's pager.
 *
 * Both viewers used to branch on "is it a video, otherwise draw an image", which was only ever
 * correct while every attachment was media with a URL behind it. A document, a call or a removed
 * video would render as a broken image, so the kinds are handled explicitly and anything with
 * nothing to show falls back to [AttachmentPlaceholder].
 */
@Composable
fun AttachmentPage(
    attachment: Attachment,
    isCurrentPage: Boolean,
    videoPlayer: @Composable (uri: String, autoPlay: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    zoom: ZoomState? = null,
) {
    when {
        attachment.path.isBlank() ->
            AttachmentPlaceholder(attachment.type, attachment.caption, modifier)

        attachment.type == AttachmentType.VIDEO ->
            // Only the page on screen plays, so swiping away stops the previous video.
            videoPlayer(attachment.path, isCurrentPage)

        attachment.type.isVisualMedia -> MediaImage(
            model = if (attachment.path.startsWith("http")) attachment.path else File(attachment.path),
            // Only a still image zooms — a video is played rather than examined, and a
            // placeholder has no detail to magnify.
            zoom = zoom,
            modifier = modifier,
            error = { AttachmentPlaceholder(attachment.type, attachment.caption) },
        )

        else -> AttachmentPlaceholder(attachment.type, attachment.caption, modifier)
    }
}
