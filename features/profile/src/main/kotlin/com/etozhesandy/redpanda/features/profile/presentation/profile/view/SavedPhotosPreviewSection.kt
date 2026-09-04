package com.etozhesandy.redpanda.features.profile.presentation.profile.view

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.etozhesandy.redpanda.core.model.SavedPhoto
import com.etozhesandy.redpanda.features.profile.R

@Composable
fun SavedPhotosPreviewSection(
    photos: List<SavedPhoto>,
    count: Int,
    onAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfilePreviewSection(
        title = stringResource(R.string.profile_saved_photos),
        count = count,
        onAllClick = onAllClick,
        modifier = modifier,
    ) {
        items(photos, key = { it.id }) { photo ->
            AsyncImage(
                model = photo.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(80.dp),
            )
        }
    }
}
