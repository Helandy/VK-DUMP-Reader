package com.etozhesandy.redpanda.features.profile.presentation.attachments

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import com.etozhesandy.redpanda.features.profile.model.ProfileAttachmentsArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ProfileAttachmentsViewModel @Inject constructor(
    private val nav: INavigationManager,
    args: ProfileAttachmentsArgs,
    repository: ProfileInfoRepository,
) : BaseViewModel<ProfileAttachmentsState.State, ProfileAttachmentsState.Event, ProfileAttachmentsState.Effect>() {

    override fun createInitialState() = ProfileAttachmentsState.State()

    private val profileId = args.profileId

    init {
        repository.observeAttachments(profileId)
            .onEach { attachments -> setState { copy(attachments = attachments, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ProfileAttachmentsState.Event) {
        when (event) {
            ProfileAttachmentsState.Event.BackClicked -> nav.back()
            is ProfileAttachmentsState.Event.AttachmentClicked ->
                nav.navigate(Routes.ProfileAttachmentViewer(profileId, event.attachment.id))
        }
    }
}
