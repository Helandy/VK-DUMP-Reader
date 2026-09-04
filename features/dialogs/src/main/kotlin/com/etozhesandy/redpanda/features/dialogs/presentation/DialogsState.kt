package com.etozhesandy.redpanda.features.dialogs.presentation

import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.ImportProgress
import com.etozhesandy.redpanda.core.model.ProfileStatus
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState
import com.etozhesandy.redpanda.core.model.DialogSort

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object DialogsState {

    data class State(
        val profileId: String = "",
        val profileStatus: ProfileStatus? = null,
        val dialogs: List<ChatDialog> = emptyList(),
        val categories: List<String> = emptyList(),
        val selectedCategory: String? = null,
        val query: String = "",
        val sort: DialogSort = DialogSort.DATE,
        val sortAscending: Boolean = false,
        val isLoading: Boolean = true,
        /** Live counters of the import running in the background, null when nothing is running. */
        val importProgress: ImportProgress? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class QueryChanged(val value: String) : Event
        data class CategorySelected(val category: String?) : Event
        data class DialogClicked(val dialog: ChatDialog) : Event
        data object FavoritesClicked : Event
        data object BackClicked : Event

        /** Picking the sort that's already active flips the direction instead of changing the key. */
        data class SortSelected(val sort: DialogSort) : Event
    }

    sealed interface Effect : UiEffect {
        data class DialogAdded(val peerName: String) : Effect
    }
}
