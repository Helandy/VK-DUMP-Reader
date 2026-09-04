package com.etozhesandy.redpanda.features.home.presentation

import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object HomeState {

    data class State(
        val profiles: List<Profile> = emptyList(),
        val isLoading: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data object ImportClicked : Event
        data object SettingsClicked : Event
        data class ProfileClicked(val profileId: String) : Event
        data class DeleteProfileClicked(val profileId: String) : Event
    }

    /** No one-off effects: everything this screen does outwards is navigation, and that goes
     * through INavigationManager. */
    sealed interface Effect : UiEffect
}
