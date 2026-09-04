package com.etozhesandy.redpanda.features.profile.presentation.mediaviewer

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.files.ImageDownloader
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import com.etozhesandy.redpanda.features.profile.model.ProfileMediaViewerArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ProfileMediaViewerViewModel @Inject constructor(
    private val nav: INavigationManager,
    private val args: ProfileMediaViewerArgs,
    private val repository: ProfileInfoRepository,
    private val imageDownloader: ImageDownloader,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : BaseViewModel<ProfileMediaViewerState.State, ProfileMediaViewerState.Event, ProfileMediaViewerState.Effect>() {

    override fun createInitialState() = ProfileMediaViewerState.State()

    /** Folder name for saved files; the id is only a fallback until the profile loads. */
    private var profileName: String = args.profileId

    init {
        repository.observeProfile(args.profileId)
            .onEach { profile -> profile?.displayName?.takeIf { it.isNotBlank() }?.let { profileName = it } }
            .launchIn(viewModelScope)

        repository.observeArchiveFiles(args.profileId)
            .map { files -> files.filter { it.sourceFolder == args.folder } }
            .flowOn(defaultDispatcher)
            .onEach { attachments ->
                val startIndex = attachments.indexOfFirst { it.id == args.startAttachmentId }.coerceAtLeast(0)
                setState { copy(attachments = attachments, startIndex = startIndex, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ProfileMediaViewerState.Event) {
        when (event) {
            ProfileMediaViewerState.Event.BackClicked -> nav.back()

            is ProfileMediaViewerState.Event.DownloadClicked -> launchSafe {
                // `args.folder` is the archive-relative folder path, so it nests as several levels.
                val savedTo = imageDownloader.download(event.source, profileName, args.folder).getOrNull()
                setEffect { ProfileMediaViewerState.Effect.DownloadFinished(savedTo) }
            }
        }
    }
}
