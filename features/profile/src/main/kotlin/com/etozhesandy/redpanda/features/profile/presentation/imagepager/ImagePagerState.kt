package com.etozhesandy.redpanda.features.profile.presentation.imagepager

import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** Shared state for a full-screen swipeable image viewer, backed by either saved photos or attachments. */

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object ImagePagerState {

    data class State(
        val urls: List<String> = emptyList(),
        val startIndex: Int = 0,
        val isLoading: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackClicked : Event
        data class DownloadClicked(val url: String) : Event
    }

    sealed interface Effect : UiEffect {
        /** [savedTo] is the folder under Download/ the file landed in; null means it did not. */
        data class DownloadFinished(val savedTo: String?) : Effect
    }
}
