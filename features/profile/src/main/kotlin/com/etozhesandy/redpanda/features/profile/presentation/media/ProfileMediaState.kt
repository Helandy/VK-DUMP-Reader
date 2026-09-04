package com.etozhesandy.redpanda.features.profile.presentation.media

import com.etozhesandy.redpanda.features.profile.model.ProfileMediaFolderSummary
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object ProfileMediaState {

    data class State(
        val folders: List<ProfileMediaFolderSummary> = emptyList(),
        val isLoading: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data class FolderClicked(val folder: String) : Event
        data object BackClicked : Event
    }

    /** No one-off effects: the only outward action is navigation, and that goes through
     * INavigationManager. */
    sealed interface Effect : UiEffect
}
