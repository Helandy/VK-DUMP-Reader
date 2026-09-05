package com.etozhesandy.redpanda.features.home.presentation

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.home.domain.usecase.DeleteProfileUseCase
import com.etozhesandy.redpanda.features.home.domain.usecase.ObserveImportRunningUseCase
import com.etozhesandy.redpanda.features.home.domain.usecase.ObserveProfilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val nav: INavigationManager,
    private val observeProfiles: ObserveProfilesUseCase,
    private val observeImportRunning: ObserveImportRunningUseCase,
    private val deleteProfile: DeleteProfileUseCase,
) : BaseViewModel<HomeState.State, HomeState.Event, HomeState.Effect>() {

    override fun createInitialState() = HomeState.State()

    init {
        observeProfiles()
            .onEach { profiles -> setState { copy(profiles = profiles, isLoading = false) } }
            .launchIn(viewModelScope)

        observeImportRunning()
            .onEach { running -> setState { copy(isImportRunning = running) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: HomeState.Event) {
        when (event) {
            // Guarded here as well as in the UI: the click can land on a state that has already
            // gone stale by the time it reaches the ViewModel.
            HomeState.Event.ImportClicked -> if (!currentState.isImportRunning) nav.navigate(Routes.Import)
            HomeState.Event.SettingsClicked -> nav.navigate(Routes.Settings)
            is HomeState.Event.ProfileClicked -> nav.navigate(Routes.Profile(event.profileId))
            is HomeState.Event.DeleteProfileClicked -> launchSafe { deleteProfile(event.profileId) }
        }
    }
}
