package com.etozhesandy.redpanda.features.chat.presentation.photo

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.R
import com.etozhesandy.redpanda.core.designsystem.components.DownloadIconButton
import com.etozhesandy.redpanda.core.designsystem.components.ImmersiveScreen
import com.etozhesandy.redpanda.core.designsystem.media.AttachmentPage
import com.etozhesandy.redpanda.core.designsystem.media.VideoPlayer
import com.etozhesandy.redpanda.features.chat.R as ChatR
import com.etozhesandy.redpanda.features.chat.presentation.photo.PhotoViewerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** Full-screen swipeable viewer over everything the dialog holds, photos and videos alike. */
@Composable
fun PhotoViewerScreen(
    state: PhotoViewerState.State,
    effect: Flow<PhotoViewerState.Effect>,
    onEvent: (PhotoViewerState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A download lands in a folder the user cannot see from here, so the outcome is a Toast
    // rather than something in the layout — there is nothing on this screen for it to change.
    val context = LocalContext.current
    val savedTo = stringResource(R.string.download_saved_to, "%s")
    val failed = stringResource(R.string.download_failed)
    LaunchedEffect(Unit) {
        effect.collectLatest { current ->
            when (current) {
                is PhotoViewerState.Effect.DownloadFinished -> Toast.makeText(
                    context,
                    current.savedTo?.let { savedTo.format(it) } ?: failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val pagerState = rememberPagerState(initialPage = state.startIndex) { state.attachments.size }

    // `startIndex` resolves asynchronously after the dialog's media loads, but `rememberPagerState`
    // only reads `initialPage` on first composition (page 0, before that load completes) — re-key
    // the jump on the loaded attachment list so the pager actually lands on the tapped photo.
    LaunchedEffect(state.attachments, state.startIndex) {
        if (state.attachments.isNotEmpty()) pagerState.scrollToPage(state.startIndex)
    }

    ImmersiveScreen(
        onBack = { onEvent(PhotoViewerState.Event.BackClicked) },
        modifier = modifier,
        actions = {
            val current = state.attachments.getOrNull(pagerState.currentPage)
            // Gallery-sourced attachments carry no message of their own, and neither does one
            // whose twin was never sent inline — there would be nowhere for the jump to land.
            val canJumpToMessage = current != null &&
                (current.messageId != null || state.attachments.any { it.path == current.path && it.messageId != null })
            if (current != null && canJumpToMessage) {
                IconButton(onClick = { onEvent(PhotoViewerState.Event.JumpToMessageClicked(current)) }) {
                    Icon(
                        imageVector = Icons.Default.SubdirectoryArrowLeft,
                        contentDescription = stringResource(ChatR.string.chat_action_go_to_message),
                        tint = Color.White,
                    )
                }
            }
            DownloadIconButton(source = current?.path?.takeIf { it.isNotBlank() }) { source ->
                onEvent(PhotoViewerState.Event.DownloadClicked(source))
            }
        },
    ) {
        if (state.attachments.isEmpty()) return@ImmersiveScreen
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val attachment = state.attachments[page]
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AttachmentPage(
                    attachment = attachment,
                    isCurrentPage = page == pagerState.currentPage,
                    videoPlayer = { uri, autoPlay -> VideoPlayer(uri = uri, autoPlay = autoPlay) },
                )
            }
        }
    }
}
