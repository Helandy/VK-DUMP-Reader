package com.etozhesandy.redpanda.features.profile.presentation.profile

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.importprogress.ImportProgressStore
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.common.net.UrlGuard
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import com.etozhesandy.redpanda.features.profile.model.ProfileArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val PREVIEW_LIMIT = 6

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val nav: INavigationManager,
    args: ProfileArgs,
    private val repository: ProfileInfoRepository,
    importProgressStore: ImportProgressStore,
) : BaseViewModel<ProfileState.State, ProfileState.Event, ProfileState.Effect>() {

    override fun createInitialState() = ProfileState.State()

    private val profileId = args.profileId

    init {
        val profileFriendsGroups = combine(
            repository.observeProfile(profileId),
            repository.observeFriends(profileId),
            repository.observeGroups(profileId),
        ) { profile, friends, groups -> Triple(profile, friends, groups) }
        val savedPhotosAttachmentsMedia = combine(
            repository.observeSavedPhotos(profileId),
            repository.observeAttachments(profileId),
            repository.observeArchiveFiles(profileId),
        ) { savedPhotos, attachments, media -> Triple(savedPhotos, attachments, media) }

        profileFriendsGroups.combine(savedPhotosAttachmentsMedia) { first, second ->
            val (profile, friends, groups) = first
            val (savedPhotos, attachments, media) = second
            ProfileState.State(
                profile = profile,
                friendsPreview = friends.take(PREVIEW_LIMIT),
                friendsCount = friends.size,
                groupsPreview = groups.take(PREVIEW_LIMIT),
                groupsCount = groups.size,
                savedPhotosPreview = savedPhotos.take(PREVIEW_LIMIT),
                savedPhotosCount = savedPhotos.size,
                attachmentsPreview = attachments.take(PREVIEW_LIMIT),
                attachmentsCount = attachments.size,
                mediaPreview = media.take(PREVIEW_LIMIT),
                mediaCount = media.size,
                isLoading = false,
            )
        }
            // The database snapshot replaces the whole state, so the progress carried by the
            // current one is copied over rather than reset to null on every emission.
            .onEach { snapshot -> setState { snapshot.copy(importProgress = importProgress) } }
            .launchIn(viewModelScope)

        // Import lands here now, so the screen shows the same live counters the dialog list does.
        // Kept out of the combine above: the store is a separate, in-process source whose updates
        // must not wait on a database emission to reach the screen.
        importProgressStore.observe(profileId)
            .onEach { progress -> setState { copy(importProgress = progress) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ProfileState.Event) {
        when (event) {
            ProfileState.Event.BackClicked -> nav.back()
            ProfileState.Event.DialogsClicked -> nav.navigate(Routes.Dialogs(profileId))
            ProfileState.Event.LinkClicked -> {
                // The screen name comes from the imported archive, so it is validated rather than
                // concatenated: "@evil.com/x" would make vk.com the userinfo of another host.
                val url = UrlGuard.vkProfileUrl(currentState.profile?.screenName) ?: return
                setEffect { ProfileState.Effect.OpenLink(url) }
            }
            ProfileState.Event.FriendsAllClicked -> nav.navigate(Routes.ProfileFriends(profileId))
            ProfileState.Event.GroupsAllClicked -> nav.navigate(Routes.ProfileGroups(profileId))
            ProfileState.Event.SavedPhotosAllClicked -> nav.navigate(Routes.ProfileSavedPhotos(profileId))
            ProfileState.Event.AttachmentsAllClicked -> nav.navigate(Routes.ProfileAttachments(profileId))
            ProfileState.Event.MediaAllClicked -> nav.navigate(Routes.ProfileMedia(profileId))
        }
    }
}
