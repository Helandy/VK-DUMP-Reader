package com.etozhesandy.redpanda.features.profile.presentation.mediafolder

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.LoadableContent
import com.etozhesandy.redpanda.core.designsystem.media.MediaGrid
import com.etozhesandy.redpanda.features.profile.R


@Composable
fun ProfileMediaFolderScreen(
    state: ProfileMediaFolderState.State,
    onEvent: (ProfileMediaFolderState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(title = state.folderName, modifier = modifier, onBack = { onEvent(ProfileMediaFolderState.Event.BackClicked) }) {
        val gridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
        LoadableContent(
            isLoading = state.isLoading,
            isEmpty = state.attachments.isEmpty(),
            emptyText = stringResource(R.string.profile_empty_folder),
        ) {
            MediaGrid(
                attachments = state.attachments,
                imageWidthDp = state.imageWidthDp,
                onAttachmentClick = { onEvent(ProfileMediaFolderState.Event.AttachmentClicked(it)) },
                state = gridState,
            )
        }
    }
}
