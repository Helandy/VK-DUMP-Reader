package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
) {
    when {
        attachment.path.isBlank() ->
            AttachmentPlaceholder(attachment.type, attachment.caption, modifier)

        attachment.type == AttachmentType.VIDEO ->
            // Only the page on screen plays, so swiping away stops the previous video.
            videoPlayer(attachment.path, isCurrentPage)

        attachment.type.isVisualMedia -> AsyncImage(
            model = if (attachment.path.startsWith("http")) attachment.path else File(attachment.path),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.fillMaxSize(),
        )

        else -> AttachmentPlaceholder(attachment.type, attachment.caption, modifier)
    }
}
