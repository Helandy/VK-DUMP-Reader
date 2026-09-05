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
        /** An import is already in flight; starting a second one breaks both. */
        val isImportRunning: Boolean = false,
        /**
         * Profiles whose erase is still running. Their rows leave the database early on, but the
         * on-disk data does not, so opening one of them would show a profile that is coming apart.
         */
        val deletingProfileIds: Set<String> = emptySet(),
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
