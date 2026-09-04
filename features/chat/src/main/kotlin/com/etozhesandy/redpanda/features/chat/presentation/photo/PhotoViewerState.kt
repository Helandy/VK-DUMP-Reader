package com.etozhesandy.redpanda.features.chat.presentation.photo

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object PhotoViewerState {

    data class State(
        val attachments: List<Attachment> = emptyList(),
        val startIndex: Int = 0,
    ) : UiState

    sealed interface Event : UiEvent {
        data class JumpToMessageClicked(val attachment: Attachment) : Event
        data class DownloadClicked(val source: String) : Event
        data object BackClicked : Event
    }

    sealed interface Effect : UiEffect {
        /** [savedTo] is the folder under Download/ the file landed in; null means it did not. */
        data class DownloadFinished(val savedTo: String?) : Effect
    }
}
