package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.model.Attachment

/** The square tile every media grid uses: quarter-gutter padding, full width, 1:1. */
val MediaGridTileModifier: Modifier = Modifier.padding(4.dp).fillMaxWidth().aspectRatio(1f)

/**
 * The square-tile gallery shared by the chat's media tabs and every media screen in a profile.
 *
 * The column count follows [imageWidthDp] rather than a fixed number: it is a user setting, so the
 * same grid is a wall of thumbnails or a handful of large tiles depending on what they picked.
 * [tile] receives [MediaGridTileModifier] — grids differ in what they draw per cell, never in the
 * shape of the cell itself.
 */
@Composable
fun <T> MediaGrid(
    items: List<T>,
    key: (T) -> Any,
    imageWidthDp: Int,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    tile: @Composable LazyGridItemScope.(T, Modifier) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = imageWidthDp.dp),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp),
    ) {
        items(items, key = key) { item -> tile(item, MediaGridTileModifier) }
    }
}

/** [MediaGrid] over attachments, drawn with the shared [AttachmentThumbnail]. */
@Composable
fun MediaGrid(
    attachments: List<Attachment>,
    imageWidthDp: Int,
    onAttachmentClick: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
) {
    MediaGrid(
        items = attachments,
        key = { it.id },
        imageWidthDp = imageWidthDp,
        modifier = modifier,
        state = state,
    ) { attachment, tileModifier ->
        AttachmentThumbnail(
            attachment = attachment,
            onClick = { onAttachmentClick(attachment) },
            modifier = tileModifier,
        )
    }
}
