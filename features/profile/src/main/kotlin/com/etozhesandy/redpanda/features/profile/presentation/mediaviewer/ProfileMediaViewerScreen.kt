package com.etozhesandy.redpanda.features.profile.presentation.mediaviewer

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.components.DownloadIconButton
import com.etozhesandy.redpanda.core.designsystem.components.ImmersiveScreen
import com.etozhesandy.redpanda.core.designsystem.media.AttachmentPage
import com.etozhesandy.redpanda.core.designsystem.media.VideoPlayer
import com.etozhesandy.redpanda.core.designsystem.R
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
    // A download lands in a folder the user cannot see from here, so the outcome is a Toast
    // rather than something in the layout — there is nothing on this screen for it to change.
    val context = LocalContext.current
    val savedTo = stringResource(R.string.download_saved_to, "%s")
    val failed = stringResource(R.string.download_failed)
    LaunchedEffect(Unit) {
        effect.collectLatest { current ->
            when (current) {
                is ProfileMediaViewerState.Effect.DownloadFinished -> Toast.makeText(
                    context,
                    current.savedTo?.let { savedTo.format(it) } ?: failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val pagerState = rememberPagerState(initialPage = state.startIndex) { state.attachments.size }

    // `startIndex` resolves asynchronously after the media list loads, but `rememberPagerState`
    // only reads `initialPage` on first composition (page 0, before that load completes) — re-key
    // the jump on the loaded attachment list so the pager actually lands on the tapped item.
    LaunchedEffect(state.attachments, state.startIndex) {
        if (state.attachments.isNotEmpty()) pagerState.scrollToPage(state.startIndex)
    }

    ImmersiveScreen(
        onBack = { onEvent(ProfileMediaViewerState.Event.BackClicked) },
        modifier = modifier,
        actions = {
            DownloadIconButton(
                source = state.attachments.getOrNull(pagerState.currentPage)?.path?.takeIf { it.isNotBlank() },
            ) { source ->
                onEvent(ProfileMediaViewerState.Event.DownloadClicked(source))
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
