package com.etozhesandy.redpanda.features.profile.presentation.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.common.net.openExternally
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.LoadingState
import com.etozhesandy.redpanda.features.profile.R
import com.etozhesandy.redpanda.features.profile.presentation.profile.view.AttachmentsPreviewSection
import com.etozhesandy.redpanda.features.profile.presentation.profile.view.FriendsPreviewSection
import com.etozhesandy.redpanda.features.profile.presentation.profile.view.GroupsPreviewSection
import com.etozhesandy.redpanda.features.profile.presentation.profile.view.MediaPreviewSection
import com.etozhesandy.redpanda.features.profile.presentation.profile.view.ProfileHeader
import com.etozhesandy.redpanda.features.profile.presentation.profile.view.SavedPhotosPreviewSection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest


@Composable
fun ProfileScreen(
    state: ProfileState.State,
    effect: Flow<ProfileState.Effect>,
    onEvent: (ProfileState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        effect.collectLatest { current ->
            when (current) {
                is ProfileState.Effect.OpenLink -> context.openExternally(current.url)
            }
        }
    }

    // Back goes through the contract rather than straight to [onBack]: leaving the profile is a
    // screen decision the ViewModel makes, and the effect above is what actually pops.
    BaseScreen(
        title = state.profile?.displayName.orEmpty(),
        modifier = modifier,
        onBack = { onEvent(ProfileState.Event.BackClicked) },
    ) {
        if (state.isLoading) {
            LoadingState()
            return@BaseScreen
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            state.profile?.let { profile ->
                item { ProfileHeader(profile) }
                item {
                    Button(
                        onClick = { onEvent(ProfileState.Event.DialogsClicked) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(stringResource(R.string.profile_dialogs))
                    }
                }
                if (profile.screenName != null) {
                    item {
                        TextButton(
                            onClick = { onEvent(ProfileState.Event.LinkClicked) },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            Text("vk.com/${profile.screenName}")
                        }
                    }
                }
            }

            item {
                FriendsPreviewSection(
                    friends = state.friendsPreview,
                    count = state.friendsCount,
                    onAllClick = { onEvent(ProfileState.Event.FriendsAllClicked) },
                )
            }
            item {
                GroupsPreviewSection(
                    groups = state.groupsPreview,
                    count = state.groupsCount,
                    onAllClick = { onEvent(ProfileState.Event.GroupsAllClicked) },
                )
            }
            item {
                SavedPhotosPreviewSection(
                    photos = state.savedPhotosPreview,
                    count = state.savedPhotosCount,
                    onAllClick = { onEvent(ProfileState.Event.SavedPhotosAllClicked) },
                )
            }
            item {
                AttachmentsPreviewSection(
                    attachments = state.attachmentsPreview,
                    count = state.attachmentsCount,
                    onAllClick = { onEvent(ProfileState.Event.AttachmentsAllClicked) },
                )
            }
            item {
                MediaPreviewSection(
                    media = state.mediaPreview,
                    count = state.mediaCount,
                    onAllClick = { onEvent(ProfileState.Event.MediaAllClicked) },
                )
            }
        }
    }
}
