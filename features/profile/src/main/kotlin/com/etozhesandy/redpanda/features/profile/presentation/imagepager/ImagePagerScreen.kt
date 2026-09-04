package com.etozhesandy.redpanda.features.profile.presentation.imagepager

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.etozhesandy.redpanda.core.designsystem.R
import com.etozhesandy.redpanda.core.designsystem.components.DownloadIconButton
import com.etozhesandy.redpanda.core.designsystem.components.ImmersiveScreen
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
    // A download lands in a folder the user cannot see from here, so the outcome is a Toast
    // rather than something in the layout — there is nothing on this screen for it to change.
    val context = LocalContext.current
    val savedTo = stringResource(R.string.download_saved_to, "%s")
    val failed = stringResource(R.string.download_failed)
    LaunchedEffect(Unit) {
        effect.collectLatest { current ->
            when (current) {
                is ImagePagerState.Effect.DownloadFinished -> Toast.makeText(
                    context,
                    current.savedTo?.let { savedTo.format(it) } ?: failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val pagerState = rememberPagerState(initialPage = state.startIndex) { state.urls.size }

    // `startIndex` resolves asynchronously after the photo list loads, but `rememberPagerState`
    // only reads `initialPage` on first composition (page 0, before that load completes) — re-key
    // the jump on the loaded list so the pager actually lands on the tapped photo.
    LaunchedEffect(state.urls, state.startIndex) {
        if (state.urls.isNotEmpty()) pagerState.scrollToPage(state.startIndex)
    }

    ImmersiveScreen(
        onBack = { onEvent(ImagePagerState.Event.BackClicked) },
        modifier = modifier,
        actions = {
            DownloadIconButton(source = state.urls.getOrNull(pagerState.currentPage)) { url ->
                onEvent(ImagePagerState.Event.DownloadClicked(url))
            }
        },
    ) {
        if (state.urls.isEmpty()) return@ImmersiveScreen
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = state.urls[page],
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
