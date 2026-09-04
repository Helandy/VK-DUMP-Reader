package com.etozhesandy.redpanda.features.chat.presentation.chat

import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState
import com.etozhesandy.redpanda.core.model.ChatDialog

/**
 * MVI-контракт шапки чата.
 *
 * Шапка — единственная часть экрана, общая для всех табов, поэтому у неё своё крошечное состояние:
 * так табам не нужен общий владелец состояния, а шапке — знание о том, какой таб открыт.
 */
object ChatTopBarState {

    data class State(val dialog: ChatDialog? = null) : UiState

    sealed interface Event : UiEvent {
        data object BackClicked : Event
        data object SearchClicked : Event
    }

    sealed interface Effect : UiEffect
}
