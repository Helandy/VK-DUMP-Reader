package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.audio

import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.MediaSort

/** MVI-контракт таба «Аудио». Воспроизведение остаётся в UI — плеер живёт ровно столько, сколько таб. */
object AudioTabState {

    data class State(
        val attachments: List<Attachment> = emptyList(),
        val sort: MediaSort = MediaSort.DATE,
        val sortAscending: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        /** Picking the sort that's already active flips the direction instead of changing the key. */
        data class SortSelected(val sort: MediaSort) : Event
    }

    sealed interface Effect : UiEffect
}
