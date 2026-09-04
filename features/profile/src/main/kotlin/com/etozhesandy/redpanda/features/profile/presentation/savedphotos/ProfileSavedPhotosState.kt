package com.etozhesandy.redpanda.features.profile.presentation.savedphotos

import com.etozhesandy.redpanda.core.model.SavedPhoto
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object ProfileSavedPhotosState {

    data class State(
        val photos: List<SavedPhoto> = emptyList(),
        val isLoading: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data class PhotoClicked(val photo: SavedPhoto) : Event
        data object BackClicked : Event
    }

    /** No one-off effects: the only outward action is navigation, and that goes through
     * INavigationManager. */
    sealed interface Effect : UiEffect
}
