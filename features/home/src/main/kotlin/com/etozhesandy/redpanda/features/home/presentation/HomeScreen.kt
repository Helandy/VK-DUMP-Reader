package com.etozhesandy.redpanda.features.home.presentation

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.LoadableContent
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.features.home.R
import com.etozhesandy.redpanda.features.home.presentation.HomeState
import com.etozhesandy.redpanda.features.home.presentation.view.DeleteProfileDialog
import com.etozhesandy.redpanda.features.home.presentation.view.ProfileListItem

@Composable
fun HomeScreen(
    state: HomeState.State,
    onEvent: (HomeState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The profile awaiting confirmation is view state only: nothing outside this screen, and
    // nothing that survives it, depends on which row's delete button was tapped.
    var profileToDelete by remember { mutableStateOf<Profile?>(null) }

    BaseScreen(
        title = stringResource(R.string.home_title),
        modifier = modifier,
        actions = {
            IconButton(onClick = { onEvent(HomeState.Event.SettingsClicked) }) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.home_action_settings))
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEvent(HomeState.Event.ImportClicked) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_action_import))
            }
        },
    ) {
        LoadableContent(
            isLoading = state.isLoading,
            isEmpty = state.profiles.isEmpty(),
            emptyText = stringResource(R.string.home_empty),
        ) {
            LazyColumn {
                items(state.profiles, key = { it.id }) { profile ->
                    ProfileListItem(
                        profile = profile,
                        onClick = { onEvent(HomeState.Event.ProfileClicked(profile.id)) },
                        onDeleteClick = { profileToDelete = profile },
                    )
                }
            }
        }
    }

    profileToDelete?.let { profile ->
        DeleteProfileDialog(
            profile = profile,
            onConfirm = {
                onEvent(HomeState.Event.DeleteProfileClicked(profile.id))
                profileToDelete = null
            },
            onDismiss = { profileToDelete = null },
        )
    }
}
