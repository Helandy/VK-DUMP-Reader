package com.etozhesandy.redpanda.features.profile.presentation.attachments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.LoadableContent
import com.etozhesandy.redpanda.core.designsystem.media.MediaGrid
import com.etozhesandy.redpanda.features.profile.R


/** These grids predate the media-width setting and stay at the size they shipped with. */
private const val GRID_IMAGE_WIDTH_DP = 100

@Composable
fun ProfileAttachmentsScreen(
    state: ProfileAttachmentsState.State,
    onEvent: (ProfileAttachmentsState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(
        title = stringResource(R.string.profile_attachments),
        modifier = modifier,
        onBack = { onEvent(ProfileAttachmentsState.Event.BackClicked) },
    ) {
        val gridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
        LoadableContent(
            isLoading = state.isLoading,
            isEmpty = state.attachments.isEmpty(),
            emptyText = stringResource(R.string.profile_empty_attachments),
        ) {
            MediaGrid(
                items = state.attachments,
                key = { it.id },
                imageWidthDp = GRID_IMAGE_WIDTH_DP,
                state = gridState,
            ) { attachment, tileModifier ->
                AsyncImage(
                    model = attachment.path,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = tileModifier.clickable { onEvent(ProfileAttachmentsState.Event.AttachmentClicked(attachment)) },
                )
            }
        }
    }
}
