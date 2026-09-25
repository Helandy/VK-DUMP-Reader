package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.compose.AsyncImage

/**
 * One full-screen image of a media viewer's pager, loading or not.
 *
 * A bare image on the viewer's black frame draws nothing until it has loaded — a CDN photo still
 * on its way, or one whose link has expired, looked exactly like a black picture sitting between
 * two real ones. A spinner stands in while loading, and [error] once the load has failed.
 */
@Composable
fun MediaImage(
    model: Any?,
    modifier: Modifier = Modifier,
    zoom: ZoomState? = null,
    error: @Composable () -> Unit = {},
) {
    var state by remember(model) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onState = { state = it },
            modifier = Modifier.fillMaxSize().let { if (zoom != null) it.zoomable(zoom) else it },
        )
        when (state) {
            is AsyncImagePainter.State.Loading -> CircularProgressIndicator(color = Color.White)
            is AsyncImagePainter.State.Error -> error()
            else -> Unit
        }
    }
}
