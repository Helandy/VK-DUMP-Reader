package com.etozhesandy.redpanda.features.profile.presentation.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.LoadableContent
import com.etozhesandy.redpanda.core.designsystem.media.MediaThumbnail
import com.etozhesandy.redpanda.features.profile.R
import com.etozhesandy.redpanda.features.profile.model.ProfileMediaFolderSummary


@Composable
fun ProfileMediaScreen(
    state: ProfileMediaState.State,
    onEvent: (ProfileMediaState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(
        title = stringResource(R.string.profile_archive_files),
        modifier = modifier,
        onBack = { onEvent(ProfileMediaState.Event.BackClicked) },
    ) {
        LoadableContent(
            isLoading = state.isLoading,
            isEmpty = state.folders.isEmpty(),
            emptyText = stringResource(R.string.profile_empty_archive_files),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.folders, key = { it.path }) { folder ->
                    FolderRow(
                        folder = folder,
                        onClick = { onEvent(ProfileMediaState.Event.FolderClicked(folder.path)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderRow(folder: ProfileMediaFolderSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val previewPath = folder.previewPath
        val previewType = folder.previewType
        if (previewPath != null && previewType != null) {
            MediaThumbnail(
                path = previewPath,
                type = previewType,
                caption = null,
                onClick = onClick,
                modifier = Modifier.size(56.dp),
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(folder.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                pluralStringResource(R.plurals.profile_file_count, folder.count, folder.count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
