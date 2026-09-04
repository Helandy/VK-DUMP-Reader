package com.etozhesandy.redpanda.features.profile.presentation.savedphotos

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

@Composable
fun ProfileSavedPhotosScreen(
    state: ProfileSavedPhotosState.State,
    onEvent: (ProfileSavedPhotosState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(
        title = stringResource(R.string.profile_saved_photos),
        modifier = modifier,
        onBack = { onEvent(ProfileSavedPhotosState.Event.BackClicked) },
    ) {
        val gridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
        LoadableContent(
            isLoading = state.isLoading,
            isEmpty = state.photos.isEmpty(),
            emptyText = stringResource(R.string.profile_empty_saved_photos),
        ) {
            MediaGrid(
                items = state.photos,
                key = { it.id },
                imageWidthDp = 100,
                state = gridState,
            ) { photo, tileModifier ->
                AsyncImage(
                    model = photo.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = tileModifier.clickable { onEvent(ProfileSavedPhotosState.Event.PhotoClicked(photo)) },
                )
            }
        }
    }
}
