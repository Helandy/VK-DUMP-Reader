package com.etozhesandy.redpanda.features.profile.presentation.attachments

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object ProfileAttachmentsState {

    data class State(
        val attachments: List<Attachment> = emptyList(),
        val isLoading: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data class AttachmentClicked(val attachment: Attachment) : Event
        data object BackClicked : Event
    }

    /** No one-off effects: the only outward action is navigation, and that goes through
     * INavigationManager. */
    sealed interface Effect : UiEffect
}
