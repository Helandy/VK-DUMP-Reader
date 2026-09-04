package com.etozhesandy.redpanda.features.favorites.presentation

import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object FavoritesState {

    data class State(
        val messages: List<Message> = emptyList(),
        val isLoading: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data class MessageClicked(val message: Message) : Event
        data object BackClicked : Event
    }

    /** No one-off effects: the only outward action is navigation, and that goes through
     * INavigationManager. */
    sealed interface Effect : UiEffect
}
