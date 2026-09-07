package com.etozhesandy.redpanda.features.profile.presentation.imagepager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.etozhesandy.redpanda.core.designsystem.components.DownloadIconButton
import com.etozhesandy.redpanda.core.designsystem.components.GoToMessageIconButton
import com.etozhesandy.redpanda.core.designsystem.components.rememberDownloadToast
import com.etozhesandy.redpanda.core.designsystem.media.MediaPagerScreen
import com.etozhesandy.redpanda.core.designsystem.media.zoomable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest


/** Full-screen swipeable image viewer shared by the saved-photos and attachments grids. */
@Composable
fun ImagePagerScreen(
    state: ImagePagerState.State,
    effect: Flow<ImagePagerState.Effect>,
    onEvent: (ImagePagerState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showDownloadOutcome = rememberDownloadToast()
    LaunchedEffect(Unit) {
        effect.collectLatest { current ->
            when (current) {
                is ImagePagerState.Effect.DownloadFinished -> showDownloadOutcome(current.savedTo)
            }
        }
    }

    MediaPagerScreen(
        items = state.pages,
        startIndex = state.startIndex,
        onBack = { onEvent(ImagePagerState.Event.BackClicked) },
        modifier = modifier,
        actions = { current ->
            val anchor = current?.anchor
            GoToMessageIconButton(enabled = anchor != null) {
                onEvent(ImagePagerState.Event.JumpToMessageClicked(anchor!!))
            }
            DownloadIconButton(source = current?.url?.takeIf { it.isNotBlank() }) { url ->
                onEvent(ImagePagerState.Event.DownloadClicked(url))
            }
        },
    ) { page, _, zoom ->
        AsyncImage(
            model = page.url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().zoomable(zoom),
        )
    }
}
