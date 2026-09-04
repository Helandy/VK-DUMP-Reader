package com.etozhesandy.redpanda.features.profile.presentation.mediafolder

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import com.etozhesandy.redpanda.features.profile.utils.folderDisplayName
import com.etozhesandy.redpanda.features.profile.model.ProfileMediaFolderArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ProfileMediaFolderViewModel @Inject constructor(
    private val nav: INavigationManager,
    private val args: ProfileMediaFolderArgs,
    repository: ProfileInfoRepository,
    settingsRepository: SettingsRepository,
) : BaseViewModel<ProfileMediaFolderState.State, ProfileMediaFolderState.Event, ProfileMediaFolderState.Effect>() {

    override fun createInitialState() = ProfileMediaFolderState.State()

    init {
        setState { copy(folderName = folderDisplayName(args.folder)) }
        repository.observeArchiveFilesInFolder(args.profileId, args.folder)
            .onEach { attachments -> setState { copy(attachments = attachments, isLoading = false) } }
            .launchIn(viewModelScope)
        settingsRepository.settings
            .onEach { settings -> setState { copy(imageWidthDp = settings.mediaImageWidthDp) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ProfileMediaFolderState.Event) {
        when (event) {
            ProfileMediaFolderState.Event.BackClicked -> nav.back()
            is ProfileMediaFolderState.Event.AttachmentClicked ->
                nav.navigate(Routes.ProfileMediaViewer(args.profileId, args.folder, event.attachment.id))
        }
    }
}
