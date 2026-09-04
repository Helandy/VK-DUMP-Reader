package com.etozhesandy.redpanda.features.profile.presentation.savedphotos

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import com.etozhesandy.redpanda.features.profile.model.ProfileSavedPhotosArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ProfileSavedPhotosViewModel @Inject constructor(
    private val nav: INavigationManager,
    args: ProfileSavedPhotosArgs,
    repository: ProfileInfoRepository,
) : BaseViewModel<ProfileSavedPhotosState.State, ProfileSavedPhotosState.Event, ProfileSavedPhotosState.Effect>() {

    override fun createInitialState() = ProfileSavedPhotosState.State()

    private val profileId = args.profileId

    init {
        repository.observeSavedPhotos(profileId)
            .onEach { photos -> setState { copy(photos = photos, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ProfileSavedPhotosState.Event) {
        when (event) {
            ProfileSavedPhotosState.Event.BackClicked -> nav.back()
            is ProfileSavedPhotosState.Event.PhotoClicked ->
                nav.navigate(Routes.ProfileSavedPhotoViewer(profileId, event.photo.id))
        }
    }
}
