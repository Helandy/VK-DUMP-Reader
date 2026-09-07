package com.etozhesandy.redpanda.features.profile.presentation.mediaviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.etozhesandy.redpanda.core.designsystem.components.DownloadIconButton
import com.etozhesandy.redpanda.core.designsystem.components.rememberDownloadToast
import com.etozhesandy.redpanda.core.designsystem.media.AttachmentPage
import com.etozhesandy.redpanda.core.designsystem.media.MediaPagerScreen
import com.etozhesandy.redpanda.core.designsystem.media.VideoPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest


/** Full-screen swipeable photo/video viewer for a profile's media grid. */
@Composable
fun ProfileMediaViewerScreen(
    state: ProfileMediaViewerState.State,
    effect: Flow<ProfileMediaViewerState.Effect>,
    onEvent: (ProfileMediaViewerState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showDownloadOutcome = rememberDownloadToast()
    LaunchedEffect(Unit) {
        effect.collectLatest { current ->
            when (current) {
                is ProfileMediaViewerState.Effect.DownloadFinished -> showDownloadOutcome(current.savedTo)
            }
        }
    }

    MediaPagerScreen(
        items = state.attachments,
        startIndex = state.startIndex,
        onBack = { onEvent(ProfileMediaViewerState.Event.BackClicked) },
        modifier = modifier,
        actions = { current ->
            DownloadIconButton(source = current?.path?.takeIf { it.isNotBlank() }) { source ->
                onEvent(ProfileMediaViewerState.Event.DownloadClicked(source))
            }
        },
    ) { attachment, isCurrentPage, zoom ->
        AttachmentPage(
            attachment = attachment,
            isCurrentPage = isCurrentPage,
            videoPlayer = { uri, autoPlay -> VideoPlayer(uri = uri, autoPlay = autoPlay) },
            zoom = zoom,
        )
    }
}
