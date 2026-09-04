package com.etozhesandy.redpanda.features.chat.presentation.search

import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.core.model.MessageSort

/** MVI-контракт экрана поиска по одному диалогу. */
object ChatSearchState {

    data class State(
        val query: String = "",
        val results: List<Message> = emptyList(),
        val sort: MessageSort = MessageSort.DATE,
        val sortAscending: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class QueryChanged(val query: String) : Event
        data class ResultClicked(val message: Message) : Event
        data object BackClicked : Event

        /** Picking the sort that's already active flips the direction instead of changing the key. */
        data class SortSelected(val sort: MessageSort) : Event
    }

    /** The screen has nothing one-off to say — every outcome is either state or navigation. */
    sealed interface Effect : UiEffect
}
